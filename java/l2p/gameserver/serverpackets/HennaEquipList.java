package l2p.gameserver.serverpackets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import l2p.gameserver.data.xml.holder.HennaHolder;
import l2p.gameserver.templates.Henna;
import l2p.gameserver.model.Player;

public class HennaEquipList extends L2GameServerPacket {

    private int _emptySlots;
    private long _adena;
    private List<Henna> _hennas = new ArrayList<>();

    public HennaEquipList(Player player) {
        _adena = player.getAdena();
        _emptySlots = player.getHennaEmptySlots();

        List<Henna> list = HennaHolder.getInstance().generateList(player);
        _hennas.addAll(list.stream().filter(element -> player.getInventory().getItemByItemId(element.getDyeId()) != null).collect(Collectors.toList()));
    }

    @Override
    protected final void writeImpl() {
        writeC(0xee);

        writeQ(_adena);
        writeD(_emptySlots);
        if (!_hennas.isEmpty()) {
            writeD(_hennas.size());
            for (Henna henna : _hennas) {
                writeD(henna.getSymbolId()); //symbolid
                writeD(henna.getDyeId()); //itemid of dye
                writeQ(henna.getDrawCount());
                writeQ(henna.getPrice());
                writeD(1); //meet the requirement or not
            }
        } else {
            writeD(0x01);
            writeD(0x00);
            writeD(0x00);
            writeQ(0x00);
            writeQ(0x00);
            writeD(0x00);
        }
    }
}