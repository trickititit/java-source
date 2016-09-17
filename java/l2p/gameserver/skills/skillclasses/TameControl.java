package l2p.gameserver.skills.skillclasses;

import java.util.List;

import l2p.gameserver.Config;
import l2p.gameserver.ai.CtrlIntention;
import l2p.gameserver.model.Creature;
import l2p.gameserver.model.Player;
import l2p.gameserver.model.Skill;
import l2p.gameserver.model.instances.TamedBeastInstance;
import l2p.gameserver.templates.StatsSet;

public class TameControl extends Skill {

    private final int _type;

    public TameControl(StatsSet set) {
        super(set);
        _type = set.getInteger("type", 0);
    }

    @Override
    public void useSkill(Creature activeChar, List<Creature> targets) {

        if (isSSPossible()) {
            activeChar.unChargeShots(isMagic());
        }

        if (!activeChar.isPlayer()) {
            return;
        }

        Player player = activeChar.getPlayer();
        if (player.getTrainedBeasts() == null) {
            return;
        }

        if (_type == 0) {
            targets.stream().filter(target -> target != null && target instanceof TamedBeastInstance).filter(target -> player.getTrainedBeasts().get(target.getObjectId()) != null).forEach(target -> {
                ((TamedBeastInstance) target).despawnWithDelay(1000);
            });
        } else if (_type > 0) {
            if (_type == 1) // Приказать бежать за хозяином.
            {
                for (TamedBeastInstance tamedBeast : player.getTrainedBeasts().values()) {
                    tamedBeast.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, player, Config.FOLLOW_RANGE);
                }
            } else if (_type == 3) // Использовать особое умение
            {
                player.getTrainedBeasts().values().forEach(TamedBeastInstance::buffOwner);
            } else if (_type == 4) // Отпустить всех зверей.
            {
                player.getTrainedBeasts().values().forEach(TamedBeastInstance::doDespawn);
            }
        }
    }
}