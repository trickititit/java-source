package l2p.gameserver.model.entity.events.impl;

import l2p.commons.collections.MultiValueSet;
import l2p.commons.dao.JdbcEntityState;
import l2p.commons.lang.reference.HardReference;
import l2p.gameserver.Config;
import l2p.gameserver.dao.SiegeClanDAO;
import l2p.gameserver.data.xml.holder.ResidenceHolder;
import l2p.gameserver.instancemanager.ReflectionManager;
import l2p.gameserver.listener.actor.OnDeathListener;
import l2p.gameserver.listener.actor.OnKillListener;
import l2p.gameserver.model.Creature;
import l2p.gameserver.model.GameObject;
import l2p.gameserver.model.Player;
import l2p.gameserver.model.Skill;
import l2p.gameserver.model.base.RestartType;
import l2p.gameserver.model.entity.events.GlobalEvent;
import l2p.gameserver.model.entity.events.objects.SiegeClanObject;
import l2p.gameserver.model.entity.events.objects.ZoneObject;
import l2p.gameserver.model.entity.residence.Residence;
import l2p.gameserver.model.instances.DoorInstance;
import l2p.gameserver.model.instances.SummonInstance;
import l2p.gameserver.model.pledge.Clan;
import l2p.gameserver.serverpackets.L2GameServerPacket;
import l2p.gameserver.serverpackets.RelationChanged;
import l2p.gameserver.serverpackets.components.IStaticPacket;
import l2p.gameserver.serverpackets.components.SystemMsg;
import l2p.gameserver.tables.ClanTable;
import l2p.gameserver.templates.DoorTemplate;
import l2p.gameserver.utils.Location;
import l2p.gameserver.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class SiegeEvent<R extends Residence, S extends SiegeClanObject> extends GlobalEvent {

    public class DoorDeathListener implements OnDeathListener {

        @Override
        public void onDeath(Creature actor, Creature killer) {
            if (!isInProgress()) {
                return;
            }

            DoorInstance door = (DoorInstance) actor;
            if (door.getDoorType() == DoorTemplate.DoorType.WALL) {
                return;
            }

            broadcastTo(SystemMsg.THE_CASTLE_GATE_HAS_BEEN_DESTROYED, SiegeEvent.ATTACKERS, SiegeEvent.DEFENDERS);
        }
    }

    public class KillListener implements OnKillListener {

        @Override
        public void onKill(Creature actor, Creature victim) {
            Player winner = actor.getPlayer();

            if (/*SiegeEvent.this instanceof FortressSiegeEvent ||*/winner == null || !victim.isPlayer() || winner.getLevel() < 40 || winner == victim || victim.getEvent(SiegeEvent.this.getClass()) != SiegeEvent.this || !checkIfInZone(actor) || !checkIfInZone(victim)) {
                return;
            }


        }

        @Override
        public boolean ignorePetOrSummon() {
            return true;
        }
    }
    public static final String OWNER = "owner";
    public static final String OLD_OWNER = "old_owner";
    public static final String ATTACKERS = "attackers";
    public static final String DEFENDERS = "defenders";
    public static final String SPECTATORS = "spectators";
    public static final String SIEGE_ZONES = "siege_zones";
    public static final String FLAG_ZONES = "flag_zones";
    public static final String DAY_OF_WEEK = "day_of_week";
    public static final String HOUR_OF_DAY = "hour_of_day";
    public static final String REGISTRATION = "registration";
    public static final String DOORS = "doors";
    protected R _residence;

    protected AtomicBoolean _isInProgress = new AtomicBoolean(false);
    private boolean _isRegistrationOver;
    protected int _dayOfWeek;
    protected int _hourOfDay;
    protected Clan _oldOwner;
    protected OnKillListener _killListener = new KillListener();
    protected OnDeathListener _doorDeathListener = new DoorDeathListener();
    protected List<HardReference<SummonInstance>> _siegeSummons = new ArrayList<>();

    public SiegeEvent(MultiValueSet<String> set) {
        super(set);
        _dayOfWeek = set.getInteger(DAY_OF_WEEK, 0);
        _hourOfDay = set.getInteger(HOUR_OF_DAY, 0);
    }

    //========================================================================================================================================================================
    //                                                                   Start / Stop Siege
    //========================================================================================================================================================================
    @Override
    public void startEvent() {
        setInProgress(true);

        super.startEvent();
    }

    @Override
    public final void stopEvent() {
        stopEvent(false);
    }

    public void stopEvent(boolean step) {
        despawnSiegeSummons();
        setInProgress(false);
        reCalcNextTime(false);

        super.stopEvent();
    }

    public void processStep(Clan clan) {
        //
    }

    @Override
    public void reCalcNextTime(boolean onInit) {
        clearActions();

        final Calendar startSiegeDate = getResidence().getSiegeDate();
        if (onInit) {
            // дата ниже текущей
            if (startSiegeDate.getTimeInMillis() <= System.currentTimeMillis()) {
                startSiegeDate.set(Calendar.DAY_OF_WEEK, _dayOfWeek);
                startSiegeDate.set(Calendar.HOUR_OF_DAY, _hourOfDay);

                validateSiegeDate(startSiegeDate, 2);
                getResidence().setJdbcState(JdbcEntityState.UPDATED);
            }
        } else {
            startSiegeDate.add(Calendar.WEEK_OF_YEAR, Config.CASTLE_WEEK);
            getResidence().setJdbcState(JdbcEntityState.UPDATED);
        }

        registerActions();

        getResidence().update();
    }

    protected void validateSiegeDate(Calendar calendar, int add) {
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        while (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, add);
        }
    }

    @Override
    protected long startTimeMillis() {
        return getResidence().getSiegeDate().getTimeInMillis();
    }
    //========================================================================================================================================================================
    //                                                                   Zones
    //========================================================================================================================================================================

    @Override
    public void teleportPlayers(String t) {
        List<Player> players = new ArrayList<>();
        Clan ownerClan = getResidence().getOwner();
        if (t.equalsIgnoreCase(OWNER)) {
            if (ownerClan != null) {
                for (Player player : getPlayersInZone()) {
                    if (player.getClan() == ownerClan) {
                        players.add(player);
                    }
                }
            }
        } else if (t.equalsIgnoreCase(ATTACKERS)) {
            for (Player player : getPlayersInZone()) {
                S siegeClan = getSiegeClan(ATTACKERS, player.getClan());
                if (siegeClan != null && siegeClan.isParticle(player)) {
                    players.add(player);
                }
            }
        } else if (t.equalsIgnoreCase(DEFENDERS)) {
            for (Player player : getPlayersInZone()) {
                if (ownerClan != null && player.getClan() != null && player.getClan() == ownerClan) {
                    continue;
                }

                S siegeClan = getSiegeClan(DEFENDERS, player.getClan());
                if (siegeClan != null && siegeClan.isParticle(player)) {
                    players.add(player);
                }
            }
        } else if (t.equalsIgnoreCase(SPECTATORS)) {
            for (Player player : getPlayersInZone()) {
                if (ownerClan != null && player.getClan() != null && player.getClan() == ownerClan) {
                    continue;
                }

                if (player.getClan() == null || getSiegeClan(ATTACKERS, player.getClan()) == null && getSiegeClan(DEFENDERS, player.getClan()) == null) {
                    players.add(player);
                }
            }
        } else {
            players = getPlayersInZone();
        }

        for (Player player : players) {
            Location loc;
            if (t.equalsIgnoreCase(OWNER) || t.equalsIgnoreCase(DEFENDERS)) {
                loc = getResidence().getOwnerRestartPoint();
            } else {
                loc = getResidence().getNotOwnerRestartPoint(player);
            }

            player.teleToLocation(loc, ReflectionManager.DEFAULT);
        }
    }

    public List<Player> getPlayersInZone() {
        List<ZoneObject> zones = getObjects(SIEGE_ZONES);
        List<Player> result = new ArrayList<>();
        for (ZoneObject zone : zones) {
            result.addAll(zone.getInsidePlayers());
        }
        return result;
    }

    public void broadcastInZone(L2GameServerPacket... packet) {
        for (Player player : getPlayersInZone()) {
            player.sendPacket(packet);
        }
    }

    public void broadcastInZone(IStaticPacket... packet) {
        for (Player player : getPlayersInZone()) {
            player.sendPacket(packet);
        }
    }

    public boolean checkIfInZone(Creature character) {
        List<ZoneObject> zones = getObjects(SIEGE_ZONES);
        for (ZoneObject zone : zones) {
            if (zone.checkIfInZone(character)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastInZone2(IStaticPacket... packet) {
        for (Player player : getResidence().getZone().getInsidePlayers()) {
            player.sendPacket(packet);
        }
    }

    public void broadcastInZone2(L2GameServerPacket... packet) {
        for (Player player : getResidence().getZone().getInsidePlayers()) {
            player.sendPacket(packet);
        }
    }
    //========================================================================================================================================================================
    //                                                                   Siege Clans
    //========================================================================================================================================================================

    public void loadSiegeClans() {
        addObjects(ATTACKERS, SiegeClanDAO.getInstance().load(getResidence(), ATTACKERS));
        addObjects(DEFENDERS, SiegeClanDAO.getInstance().load(getResidence(), DEFENDERS));
    }

    @SuppressWarnings("unchecked")
    public S newSiegeClan(String type, int clanId, long param, long date) {
        Clan clan = ClanTable.getInstance().getClan(clanId);
        return clan == null ? null : (S) new SiegeClanObject(type, clan, param, date);
    }

    public void updateParticles(boolean start, String... arg) {
        for (String a : arg) {
            List<SiegeClanObject> siegeClans = getObjects(a);
            for (SiegeClanObject s : siegeClans) {
                s.setEvent(start, this);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public S getSiegeClan(String name, Clan clan) {
        if (clan == null) {
            return null;
        }
        return getSiegeClan(name, clan.getClanId());
    }

    @SuppressWarnings("unchecked")
    public S getSiegeClan(String name, int objectId) {
        List<SiegeClanObject> siegeClanList = getObjects(name);
        if (siegeClanList.isEmpty()) {
            return null;
        }
        for (SiegeClanObject siegeClan : siegeClanList) {
            if (siegeClan.getObjectId() == objectId) {
                return (S) siegeClan;
            }
        }
        return null;
    }

    public void broadcastTo(IStaticPacket packet, String... types) {
        for (String type : types) {
            List<SiegeClanObject> siegeClans = getObjects(type);
            for (SiegeClanObject siegeClan : siegeClans) {
                siegeClan.broadcast(packet);
            }
        }
    }

    public void broadcastTo(L2GameServerPacket packet, String... types) {
        for (String type : types) {
            List<SiegeClanObject> siegeClans = getObjects(type);
            for (SiegeClanObject siegeClan : siegeClans) {
                siegeClan.broadcast(packet);
            }
        }
    }

    //========================================================================================================================================================================
    //                                                         Override GlobalEvent
    //========================================================================================================================================================================
    @Override
    @SuppressWarnings("unchecked")
    public void initEvent() {
        _residence = (R) ResidenceHolder.getInstance().getResidence(getId());

        loadSiegeClans();

        clearActions();

        super.initEvent();
    }

    @Override
    protected void printInfo() {
        final long startSiegeMillis = startTimeMillis();

        if (startSiegeMillis == 0) {
            info(getName() + " time - undefined");
        } else {
            info(getName() + " time - " + TimeUtils.toSimpleFormat(startSiegeMillis));
        }
    }

    @Override
    public boolean ifVar(String name) {
        if (name.equals(OWNER)) {
            return getResidence().getOwner() != null;
        }
        return name.equals(OLD_OWNER) && _oldOwner != null;

    }

    @Override
    public boolean isParticle(Player player) {
        return !(!isInProgress() || player.getClan() == null) && (getSiegeClan(ATTACKERS, player.getClan()) != null || getSiegeClan(DEFENDERS, player.getClan()) != null);
    }

    @Override
    public void checkRestartLocs(Player player, Map<RestartType, Boolean> r) {
        if (getObjects(FLAG_ZONES).isEmpty()) {
            return;
        }

        S clan = getSiegeClan(ATTACKERS, player.getClan());
        if (clan != null) {
            if (clan.getFlag() != null) {
                r.put(RestartType.TO_FLAG, Boolean.TRUE);
            }
        }
    }

    @Override
    public Location getRestartLoc(Player player, RestartType type) {
        S attackerClan = getSiegeClan(ATTACKERS, player.getClan());

        Location loc = null;
        switch (type) {
            case TO_FLAG:
                if (!getObjects(FLAG_ZONES).isEmpty() && attackerClan != null && attackerClan.getFlag() != null) {
                    loc = Location.findPointToStay(attackerClan.getFlag(), 50, 75);
                } else {
                    player.sendPacket(SystemMsg.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE);
                }
                break;
        }

        return loc;
    }

    @Override
    public int getRelation(Player thisPlayer, Player targetPlayer, int result) {
        Clan clan1 = thisPlayer.getClan();
        Clan clan2 = targetPlayer.getClan();
        if (clan2 == null) {
            return result;
        }

        SiegeEvent<?, ?> siegeEvent2 = targetPlayer.getEvent(SiegeEvent.class);
        result |= RelationChanged.RELATION_INSIEGE;
        SiegeClanObject siegeClan1 = getSiegeClan(SiegeEvent.ATTACKERS, clan1);
        SiegeClanObject siegeClan2 = getSiegeClan(SiegeEvent.ATTACKERS, clan2);

        if (siegeClan1 == null && siegeClan2 == null || siegeClan1 != null && siegeClan2 != null && isAttackersInAlly()) {
            result |= RelationChanged.RELATION_ALLY;
        } else {
            result |= RelationChanged.RELATION_ENEMY;
        }
        if (siegeClan1 != null) {
            result |= RelationChanged.RELATION_ATTACKER;
        }

        return result;
    }

    @Override
    public int getUserRelation(Player thisPlayer, int oldRelation) {
        SiegeClanObject siegeClan = getSiegeClan(SiegeEvent.ATTACKERS, thisPlayer.getClan());
        if (siegeClan != null) {
            oldRelation |= 0x180;
        } else {
            oldRelation |= 0x80;
        }
        return oldRelation;
    }

    @Override
    public boolean canAttack(Creature target, Creature attacker, Skill skill, boolean force) {
        return true;
    }

    @Override
    public SystemMsg checkForAttack(Creature target, Creature attacker, Skill skill, boolean force) {
        SiegeEvent<?, ?> siegeEvent = target.getEvent(SiegeEvent.class);

        // или вообще не учасник, или учасники разных осад
        if (this != siegeEvent) {
            return null;
        }
        if (!checkIfInZone(target) || !checkIfInZone(attacker)) {
            return null;
        }

        Player player = target.getPlayer();
        if (player == null) {
            return null;
        }

        SiegeClanObject siegeClan1 = getSiegeClan(SiegeEvent.ATTACKERS, player.getClan());
        if (siegeClan1 == null && attacker.isSiegeGuard()) {
            return SystemMsg.INVALID_TARGET;
        }
        Player playerAttacker = attacker.getPlayer();
        if (playerAttacker == null) {
            return SystemMsg.INVALID_TARGET;
        }

        SiegeClanObject siegeClan2 = getSiegeClan(SiegeEvent.ATTACKERS, playerAttacker.getClan());
        // если оба аттакеры, и в осаде, аттакеры в Алли, невозможно бить
        if (siegeClan1 != null && siegeClan2 != null && isAttackersInAlly()) {
            return SystemMsg.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE;
        }
        // если нету как Аттакры, это дефендеры, то невозможно бить
        if (siegeClan1 == null && siegeClan2 == null) {
            return SystemMsg.INVALID_TARGET;
        }

        return null;
    }

    @Override
    public boolean isInProgress() {
        return _isInProgress.get();
    }

    @Override
    public void action(String name, boolean start) {
        if (name.equalsIgnoreCase(REGISTRATION)) {
            setRegistrationOver(!start);
        } else {
            super.action(name, start);
        }
    }

    public boolean isAttackersInAlly() {
        return false;
    }

    @Override
    public void onAddEvent(GameObject object) {
        if (_killListener == null) {
            return;
        }

        if (object.isPlayer()) {
            ((Player) object).addListener(_killListener);
        }
    }

    @Override
    public void onRemoveEvent(GameObject object) {
        if (_killListener == null) {
            return;
        }

        if (object.isPlayer()) {
            ((Player) object).removeListener(_killListener);
        }
    }

    @Override
    public List<Player> broadcastPlayers(int range) {
        return itemObtainPlayers();
    }

    @Override
    public List<Player> itemObtainPlayers() {
        List<Player> playersInZone = getPlayersInZone();

        List<Player> list = new ArrayList<>(playersInZone.size());
        list.addAll(getPlayersInZone().stream().filter(player -> player.getEvent(getClass()) == this).collect(Collectors.toList()));
        return list;
    }

    public Location getEnterLoc(Player player) {
        S siegeClan = getSiegeClan(ATTACKERS, player.getClan());
        if (siegeClan != null) {
            if (siegeClan.getFlag() != null) {
                return Location.findAroundPosition(siegeClan.getFlag(), 50, 75);
            } else {
                return getResidence().getNotOwnerRestartPoint(player);
            }
        } else {
            return getResidence().getOwnerRestartPoint();
        }
    }

    //========================================================================================================================================================================
    // Getters & Setters
    //========================================================================================================================================================================
    public R getResidence() {
        return _residence;
    }

    public void setInProgress(boolean b) {
        _isInProgress.set(b);
    }

    public boolean isRegistrationOver() {
        return _isRegistrationOver;
    }

    public void setRegistrationOver(boolean b) {
        _isRegistrationOver = b;
    }
    //========================================================================================================================================================================

    public void addSiegeSummon(SummonInstance summon) {
        _siegeSummons.add(summon.getRef());
    }

    public boolean containsSiegeSummon(SummonInstance cha) {
        return _siegeSummons.contains(cha.getRef());
    }

    public void despawnSiegeSummons() {
        for (HardReference<SummonInstance> ref : _siegeSummons) {
            SummonInstance summon = ref.get();
            if (summon != null) {
                summon.unSummon();
            }
        }
        _siegeSummons.clear();
    }
}
