package l2p.gameserver.skills.effects;

import l2p.gameserver.model.Creature;
import l2p.gameserver.model.Effect;
import l2p.gameserver.model.Skill;
import l2p.gameserver.model.Skill.SkillType;
import l2p.gameserver.stats.Env;

public final class EffectInvulnerable extends Effect {

    public EffectInvulnerable(Env env, EffectTemplate template) {
        super(env, template);
    }

    @Override
    public boolean checkCondition() {

        if (_effected.isInvul()) {
            return false;
        }

        Skill skill = _effected.getCastingSkill();

        if (skill != null && (skill.getSkillType() == SkillType.TAKECASTLE || skill.getSkillType() == SkillType.TAKEFORTRESS || skill.getSkillType() == SkillType.TAKEFLAG)) {
            return false;
        }
        return super.checkCondition();
    }

    @Override
    public void onStart() {
        super.onStart();
        _effected.startHealBlocked();
        _effected.setIsInvul(true);
    }

    @Override
    public void onExit() {
        super.onExit();
        _effected.stopHealBlocked();
        _effected.setIsInvul(false);
    }

    @Override
    public boolean onActionTime() {
        return false;
    }


}