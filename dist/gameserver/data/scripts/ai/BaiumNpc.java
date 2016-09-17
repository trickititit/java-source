package ai;

import java.util.List;

import l2p.gameserver.ai.DefaultAI;
import l2p.gameserver.model.Creature;
import l2p.gameserver.model.instances.NpcInstance;
import l2p.gameserver.serverpackets.Earthquake;
import l2p.gameserver.serverpackets.L2GameServerPacket;

public class BaiumNpc extends DefaultAI {

    private long _wait_timeout = 0;
    private static final int BAIUM_EARTHQUAKE_TIMEOUT = 1000 * 60 * 15; // 15 мин

    public BaiumNpc(NpcInstance actor) {
        super(actor);
    }

    @Override
    public boolean isGlobalAI() {
        return true;
    }

    @Override
    protected boolean thinkActive() {
        NpcInstance actor = getActor();
        // Пора устроить землятрясение
        if (_wait_timeout < System.currentTimeMillis()) {
            _wait_timeout = System.currentTimeMillis() + BAIUM_EARTHQUAKE_TIMEOUT;
            L2GameServerPacket eq = new Earthquake(actor.getLoc(), 40, 10);
            List<Creature> chars = actor.getAroundCharacters(5000, 10000);
            chars.stream().filter(character -> character.isPlayer()).forEach(character -> {
                character.sendPacket(eq);
            });
        }
        return false;
    }

    @Override
    protected boolean randomWalk() {
        return false;
    }
}