package l2p.gameserver.serverpackets;

public class PledgeShowMemberListDeleteAll extends L2GameServerPacket {

    public static final L2GameServerPacket STATIC = new PledgeShowMemberListDeleteAll();

    @Override
    protected final void writeImpl() {
        writeC(0x88);
    }
}