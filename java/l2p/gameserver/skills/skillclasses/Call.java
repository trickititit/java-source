package l2p.gameserver.skills.skillclasses;

import static l2p.gameserver.model.Zone.ZoneType.no_restart;
import static l2p.gameserver.model.Zone.ZoneType.no_summon;

import java.util.List;

import l2p.gameserver.instancemanager.ReflectionManager;
import l2p.gameserver.model.Creature;
import l2p.gameserver.model.Player;
import l2p.gameserver.model.Skill;
import l2p.gameserver.model.Zone;
import l2p.gameserver.serverpackets.components.SystemMsg;
import l2p.gameserver.serverpackets.SystemMessage2;
import l2p.gameserver.templates.StatsSet;
import l2p.gameserver.utils.Location;

public class Call extends Skill {

    final boolean _party;

    public Call(StatsSet set) {
        super(set);
        _party = set.getBool("party", false);
    }

    @Override
    public boolean checkCondition(Creature activeChar, Creature target, boolean forceUse, boolean dontMove, boolean first) {
        if (activeChar.isPlayer()) {
            if (_party && ((Player) activeChar).getParty() == null) {
                return false;
            }

            SystemMessage2 msg = canSummonHere((Player) activeChar);
            if (msg != null) {
                activeChar.sendPacket(msg);
                return false;
            }

            // Эта проверка только для одиночной цели
            if (!_party) {
                if (activeChar == target) {
                    return false;
                }

                msg = canBeSummoned(target);
                if (msg != null) {
                    activeChar.sendPacket(msg);
                    return false;
                }
            }
        }

        return super.checkCondition(activeChar, target, forceUse, dontMove, first);
    }

    @Override
    public void useSkill(Creature activeChar, List<Creature> targets) {
        if (!activeChar.isPlayer()) {
            return;
        }

        SystemMessage2 msg = canSummonHere((Player) activeChar);
        if (msg != null) {
            activeChar.sendPacket(msg);
            return;
        }

        if (_party) {
            if (((Player) activeChar).getParty() != null) {
                ((Player) activeChar).getParty().getPartyMembers().stream().filter(target -> !target.equals(activeChar) && canBeSummoned(target) == null && !target.isTerritoryFlagEquipped()).forEach(target -> {
                    target.stopMove();
                    target.teleToLocation(Location.findPointToStay(activeChar, 100, 150), activeChar.getGeoIndex());
                    getEffects(activeChar, target, getActivateRate() > 0, false);
                });
            }

            if (isSSPossible()) {
                activeChar.unChargeShots(isMagic());
            }
            return;
        }

        for (Creature target : targets) {
            if (target != null) {
                if (canBeSummoned(target) != null) {
                    continue;
                }

                ((Player) target).summonCharacterRequest(activeChar, Location.findAroundPosition(activeChar, 100, 150), getId() == 1403 || getId() == 1404 ? 1 : 0);

                getEffects(activeChar, target, true, false);
            }
        }

        if (isSSPossible()) {
            activeChar.unChargeShots(isMagic());
        }
    }

    /**
     * Может ли призывающий в данный момент использовать призыв
     */
    public static SystemMessage2 canSummonHere(Player activeChar) {
        if (activeChar.isAlikeDead() || activeChar.isInOlympiadMode() || activeChar.isOutOfControl() || activeChar.isFlying() || activeChar.isFestivalParticipant()) {
            return new SystemMessage2(SystemMsg.NOTHING_HAPPENED);
        }

        // "Нельзя вызывать персонажей в/из зоны свободного PvP"
        // "в зоны осад"
        // "на Олимпийский стадион"
        // "в зоны определенных рейд-боссов и эпик-боссов"
        if (activeChar.isInZoneBattle() || activeChar.isInZone(Zone.ZoneType.SIEGE) || activeChar.isInZone(no_restart) || activeChar.isInZone(no_summon) || activeChar.isInBoat() || activeChar.getReflection() != ReflectionManager.DEFAULT) {
            return new SystemMessage2(SystemMsg.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
        }

        //if(activeChar.isInCombat())
        //return Msg.YOU_CANNOT_SUMMON_DURING_COMBAT;
        if (activeChar.isInStoreMode() || activeChar.isProcessingRequest()) {
            return new SystemMessage2(SystemMsg.YOU_CANNOT_SUMMON_DURING_A_TRADE_OR_WHILE_USING_A_PRIVATE_STORE);
        }

        return null;
    }

    /**
     * Может ли цель ответить на призыв
     */
    public static SystemMessage2 canBeSummoned(Creature target) {
        if ((target == null) || (!target.isPlayer()) || (target.getPlayer().isTerritoryFlagEquipped()) || (target.isFlying()) || (target.isOutOfControl()) || (target.getPlayer().isFestivalParticipant()) || (target.getPlayer().isTeleportBlocked())) {
            return new SystemMessage2(SystemMsg.INVALID_TARGET);
        }

        if (((Player) target).isInOlympiadMode()) {
            return new SystemMessage2(SystemMsg.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_CURRENTLY_PARTICIPATING_IN_THE_GRAND_OLYMPIAD);
        }

        if (target.isInZoneBattle() || target.isInZone(Zone.ZoneType.SIEGE) || target.isInZone(no_restart) || target.isInZone(no_summon) || target.getReflection() != ReflectionManager.DEFAULT || target.isInBoat()) {
            return new SystemMessage2(SystemMsg.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
        }

        // Нельзя призывать мертвых персонажей
        if (target.isAlikeDead()) {
            return new SystemMessage2(SystemMsg.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addString(target.getName());
        }

        // Нельзя призывать персонажей, которые находятся в режиме PvP или Combat Mode
        if (target.getPvpFlag() != 0 || target.isInCombat()) {
            return new SystemMessage2(SystemMsg.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addString(target.getName());
        }

        Player pTarget = (Player) target;

        // Нельзя призывать торгующих персонажей
        if (pTarget.getPrivateStoreType() != Player.STORE_PRIVATE_NONE || pTarget.isProcessingRequest()) {
            return new SystemMessage2(SystemMsg.C1_IS_CURRENTLY_TRADING_OR_OPERATING_A_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addString(target.getName());
        }

        return null;
    }
}
