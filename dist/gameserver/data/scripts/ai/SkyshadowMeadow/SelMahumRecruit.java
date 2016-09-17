package ai.SkyshadowMeadow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import l2p.commons.util.Rnd;
import l2p.gameserver.ai.CtrlEvent;
import l2p.gameserver.ai.Fighter;
import l2p.gameserver.model.Creature;
import l2p.gameserver.model.instances.NpcInstance;
import l2p.gameserver.scripts.Functions;
import l2p.gameserver.serverpackets.SocialAction;
import l2p.gameserver.serverpackets.components.NpcString;

/**
 * @author PaInKiLlEr - AI для моба Sel Mahum Recruit (22780) и Sel Mahum
 * Recruit (22782) и Sel Mahum Soldier (22783) и Sel Mahum Recruit (22784) и Sel
 * Mahum Soldier (22785). - При атаке ругается в чат с шансом 20%, агрит
 * главного моба. - AI проверен и работает.
 */
public class SelMahumRecruit extends Fighter {

    private long _wait_timeout = System.currentTimeMillis() + 180000;
    private List<NpcInstance> _arm = new ArrayList<>();
    private boolean _firstTimeAttacked = true;
    public static final NpcString[] _text = {NpcString.SCHOOL1, NpcString.SCHOOL2};

    public SelMahumRecruit(NpcInstance actor) {
        super(actor);
    }

    @Override
    protected boolean thinkActive() {
        NpcInstance actor = getActor();
        if (actor == null) {
            return true;
        }

        if (_wait_timeout < System.currentTimeMillis()) {
            _wait_timeout = (System.currentTimeMillis() + Rnd.get(150, 200) * 1000);
            actor.broadcastPacket(new SocialAction(actor.getObjectId(), 1));
        }

        if (_arm == null || _arm.isEmpty()) {
            _arm.addAll(getActor().getAroundNpc(750, 750).stream().filter(npc -> npc != null && (npc.getNpcId() == 22775 || npc.getNpcId() == 22776 || npc.getNpcId() == 22778 || npc.getNpcId() == 22780 || npc.getNpcId() == 22782 || npc.getNpcId() == 22783 || npc.getNpcId() == 22784 || npc.getNpcId() == 22785)).collect(Collectors.toList()));
        }
        return true;
    }

    @Override
    protected void onEvtAttacked(Creature attacker, int damage) {
        NpcInstance actor = getActor();
        if (actor == null) {
            return;
        }

        for (NpcInstance npc : _arm) {
            npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, Rnd.get(1, 100));

            if (npc.isDead()) {
                if (Rnd.chance(20)) {
                    if (_firstTimeAttacked) {
                        _firstTimeAttacked = false;
                        Functions.npcSay(actor, _text[Rnd.get(_text.length)]);
                    }
                }
                actor.moveToLocation(actor.getSpawnedLoc(), 0, true);
            }
        }

        super.onEvtAttacked(attacker, damage);
    }

    @Override
    protected void onEvtDead(Creature killer) {
        _firstTimeAttacked = true;
        super.onEvtDead(killer);
    }
}