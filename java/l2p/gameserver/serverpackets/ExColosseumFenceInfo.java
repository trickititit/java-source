package l2p.gameserver.serverpackets;

public class ExColosseumFenceInfo extends L2GameServerPacket {

    @Override
    protected void writeImpl() {
        writeEx(0x03);
        // TODO ddddddd
    }
}