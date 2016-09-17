package l2p.gameserver.skills.skillclasses;

import java.util.List;

import l2p.gameserver.model.Creature;
import l2p.gameserver.model.Player;
import l2p.gameserver.model.Skill;
import l2p.gameserver.serverpackets.components.SystemMsg;
import l2p.gameserver.serverpackets.MagicSkillUse;
import l2p.gameserver.stats.Formulas;
import l2p.gameserver.stats.Formulas.AttackInfo;
import l2p.gameserver.templates.StatsSet;

public class Charge extends Skill {

    public static final int MAX_CHARGE = 8;
    private int _charges;
    private boolean _fullCharge;

    public Charge(StatsSet set) {
        super(set);
        _charges = set.getInteger("charges", getLevel());
        _fullCharge = set.getBool("fullCharge", false);
    }

    @Override
    public boolean checkCondition(final Creature activeChar, final Creature target, boolean forceUse, boolean dontMove, boolean first) {
        if (!activeChar.isPlayer()) {
            return false;
        }

        Player player = (Player) activeChar;

        //Камушки можно юзать даже если заряд > 7, остальное только если заряд < уровень скила
        if (getPower() <= 0 && getId() != 2165 && player.getIncreasedForce() >= _charges) {
            activeChar.sendPacket(SystemMsg.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
            return false;
        } else if (getId() == 2165) {
            player.sendPacket(new MagicSkillUse(player, player, 2165, 1, 0, 0));
        }

        return super.checkCondition(activeChar, target, forceUse, dontMove, first);
    }

    @Override
    public void useSkill(Creature activeChar, List<Creature> targets) {
        if (!activeChar.isPlayer()) {
            return;
        }

        boolean ss = activeChar.getChargedSoulShot() && isSSPossible();
        if (ss && getTargetType() != SkillTargetType.TARGET_SELF) {
            activeChar.unChargeShots(false);
        }

        for (Creature target : targets) {
            if (target.isDead() || target == activeChar) {
                continue;
            }

            if (getPower() > 0) // Если == 0 значит скилл "отключен"
            {
                AttackInfo info = Formulas.calcPhysDam(activeChar, target, this, false, false, ss, false);

                if (info.lethal_dmg > 0) {
                    target.reduceCurrentHp(info.lethal_dmg, activeChar, this, true, true, false, false, false, false, false);
                }

                target.reduceCurrentHp(info.damage, activeChar, this, true, true, false, true, false, false, true);
                target.doCounterAttack(this, activeChar, false);

            }

            getEffects(activeChar, target, true, false);
        }

        chargePlayer((Player) activeChar, getId());
    }

    public void chargePlayer(Player player, Integer skillId) {
        if (player.getIncreasedForce() >= _charges) {
            player.sendPacket(SystemMsg.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY_);
            return;
        }
        if (_fullCharge) {
            player.setIncreasedForce(_charges);
        } else {
            player.setIncreasedForce(player.getIncreasedForce() + 1);
        }
    }
}
