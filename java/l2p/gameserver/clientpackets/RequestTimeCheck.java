package l2p.gameserver.clientpackets;

public class RequestTimeCheck extends L2GameClientPacket {

    private int unk, unk2;

    /**
     * format: dd
     */
    @Override
    protected void readImpl() {
        unk = readD();
        unk2 = readD();
    }

    @Override
    protected void runImpl() {
        //TODO not implemented
    }
}