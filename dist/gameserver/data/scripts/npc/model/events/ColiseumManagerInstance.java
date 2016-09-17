package npc.model.events;

import java.util.List;
import l2p.commons.collections.CollectionUtils;
import l2p.gameserver.data.xml.holder.EventHolder;
import l2p.gameserver.model.Party;
import l2p.gameserver.model.Player;
import l2p.gameserver.model.entity.events.EventType;
import l2p.gameserver.model.entity.events.impl.UndergroundColiseumEvent;
import l2p.gameserver.serverpackets.components.HtmlMessage;
import l2p.gameserver.templates.npc.NpcTemplate;
import org.apache.commons.lang3.StringUtils;

/**
 * @author VISTALL
 * @date 15:40/12.07.2011
 *
 * events/kerthang_manager004.htm - не лидер пати, но в пати
 * events/kerthang_manager008.htm - нету пати events/kerthang_manager011.htm -
 * C1 непохдодит уровнем
 */
public class ColiseumManagerInstance extends ColiseumHelperInstance {

    private String _startHtm;
    private int _coliseumId;

    public ColiseumManagerInstance(int objectId, NpcTemplate template) {
        super(objectId, template);

        _startHtm = getParameter("start_htm", StringUtils.EMPTY);
        _coliseumId = getParameter("coliseum_id", 0);
    }

    @Override
    public void onBypassFeedback(Player player, String command) {
        if (!canBypassCheck(player, this)) {
            return;
        }

        UndergroundColiseumEvent coliseumEvent = EventHolder.getInstance().getEvent(EventType.MAIN_EVENT, _coliseumId);

        switch (command) {
            case "register":
                Party party = player.getParty();
                if (party == null) {
                    showChatWindow(player, "events/kerthang_manager008.htm");
                } else if (party.getPartyLeader() != player) {
                    showChatWindow(player, "events/kerthang_manager004.htm");
                } else {
                    for (Player $player : party) {
                        if ($player.getLevel() < coliseumEvent.getMinLevel() || $player.getLevel() > coliseumEvent.getMaxLevel()) {
                            showChatWindow(player, "events/kerthang_manager011.htm", "%name%", $player.getName());
                            return;
                        }
                    }
                }
                break;
            case "viewTeams":

                List<Player> reg = coliseumEvent.getRegisteredPlayers();

                HtmlMessage msg = new HtmlMessage(player, this);
                msg.setFile("events/kerthang_manager003.htm");
                for (int i = 0; i < 5; i++) {
                    Player $player = CollectionUtils.safeGet(reg, i);

                    msg.replace("%team" + i + "%", $player == null ? StringUtils.EMPTY : $player.getName());
                }

                player.sendPacket(msg);
                break;
            default:
                super.onBypassFeedback(player, command);
                break;
        }
    }

    @Override
    public void showChatWindow(Player player, int val, Object... ar) {
        showChatWindow(player, _startHtm);
    }
}
