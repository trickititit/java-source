package l2p.gameserver.serverpackets;

public class ExEventMatchList extends L2GameServerPacket {

    @Override
    protected void writeImpl() {
        writeEx(0x0D);
        // TODO пока не реализован даже в коиенте
    }
}