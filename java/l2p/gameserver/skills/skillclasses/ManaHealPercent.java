package l2p.gameserver.skills.skillclasses;

import java.util.List;

import l2p.gameserver.model.Creature;
import l2p.gameserver.model.Skill;
import l2p.gameserver.serverpackets.components.SystemMsg;
import l2p.gameserver.serverpackets.SystemMessage2;
import l2p.gameserver.stats.Stats;
import l2p.gameserver.templates.StatsSet;

public class ManaHealPercent extends Skill {

    private final boolean _ignoreMpEff;

    public ManaHealPercent(StatsSet set) {
        super(set);
        _ignoreMpEff = set.getBool("ignoreMpEff", true);
    }

    @Override
    public void useSkill(Creature activeChar, List<Creature> targets) {
        for (Creature target : targets) {
            if (target != null) {
                if (target.isDead() || target.isHealBlocked()) {
                    continue;
                }

                getEffects(activeChar, target, true, false);

                double mp = _power * target.getMaxMp() / 100.;
                double newMp = mp * (!_ignoreMpEff ? target.calcStat(Stats.MANAHEAL_EFFECTIVNESS, 100., activeChar, this) : 100.) / 100.;
                double addToMp = Math.max(0, Math.min(newMp, target.calcStat(Stats.MP_LIMIT, null, null) * target.getMaxMp() / 100. - target.getCurrentMp()));

                if (addToMp > 0) {
                    target.setCurrentMp(target.getCurrentMp() + addToMp);
                }
                if (target.isPlayer()) {
                    if (activeChar != target) {
                        target.sendPacket(new SystemMessage2(SystemMsg.S2_MP_HAS_BEEN_RESTORED_BY_C1).addString(activeChar.getName()).addInteger(Math.round(addToMp)));
                    } else {
                        activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_MP_HAS_BEEN_RESTORED).addInteger(Math.round(addToMp)));
                    }
                }
            }
        }

        if (isSSPossible()) {
            activeChar.unChargeShots(isMagic());
        }
    }
}