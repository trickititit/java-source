package services.community;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import l2p.gameserver.Config;
import l2p.gameserver.data.htm.HtmCache;
import l2p.gameserver.handler.bbs.CommunityBoardManager;
import l2p.gameserver.handler.bbs.ICommunityBoardHandler;
import l2p.gameserver.model.Player;
import l2p.gameserver.model.actor.instances.player.Friend;
import l2p.gameserver.serverpackets.ShowBoard;
import l2p.gameserver.scripts.ScriptFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageFriends implements ScriptFile, ICommunityBoardHandler {

    private static final Logger _log = LoggerFactory.getLogger(ManageFriends.class);

    @Override
    public void onLoad() {
        if (Config.COMMUNITYBOARD_ENABLED) {
            _log.info("CommunityBoard: Manage Friends service loaded.");
            CommunityBoardManager.getInstance().registerHandler(this);
        }
    }

    @Override
    public void onReload() {
        if (Config.COMMUNITYBOARD_ENABLED) {
            CommunityBoardManager.getInstance().removeHandler(this);
        }
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public String[] getBypassCommands() {
        return new String[]{
            "_friendlist_",
            "_friendblocklist_",
            "_frienddelete_",
            "_frienddeleteallconfirm_",
            "_frienddeleteall_",
            "_friendblockdelete_",
            "_friendblockadd_",
            "_friendblockdeleteallconfirm_",
            "_friendblockdeleteall_"};
    }

    @Override
    public void onBypassCommand(Player player, String bypass) {
        StringTokenizer st = new StringTokenizer(bypass, "_");
        String cmd = st.nextToken();
        String html = HtmCache.getInstance().getHtml(cmd.startsWith("friendbloc") ? Config.BBS_HOME_DIR + "bbs_block_list.htm" : Config.BBS_HOME_DIR + "bbs_friend_list.htm", player);
        player.setSessionVar("add_fav", null);
        if ("friendlist".equals(cmd)) {
            int act = Integer.parseInt(st.nextToken());
            html = html.replace("%friend_list%", getFriendList(player));
            switch (act) {
                case 0:
                    if (player.getSessionVar("selFriends") != null) {
                        player.setSessionVar("selFriends", null);
                    }
                    html = html.replace("%selected_friend_list%", "");
                    html = html.replace("%delete_all_msg%", "");
                    break;
                case 1: {
                    String objId = st.nextToken();
                    String selected;
                    if ((selected = player.getSessionVar("selFriends")) == null) {
                        selected = objId + ";";
                    } else if (!selected.contains(objId)) {
                        selected += objId + ";";
                    }
                    player.setSessionVar("selFriends", selected);
                    html = html.replace("%selected_friend_list%", getSelectedList(player));
                    html = html.replace("%delete_all_msg%", "");
                    break;
                }
                case 2: {
                    String objId = st.nextToken();
                    String selected = player.getSessionVar("selFriends");
                    if (selected != null) {
                        selected = selected.replace(objId + ";", "");
                        player.setSessionVar("selFriends", selected);
                    }
                    html = html.replace("%selected_friend_list%", getSelectedList(player));
                    html = html.replace("%delete_all_msg%", "");
                    break;
                }
            }
        } else if ("frienddeleteallconfirm".equals(cmd)) {
            html = html.replace("%friend_list%", getFriendList(player));

            html = html.replace("%selected_friend_list%", getSelectedList(player));
            html = html.replace("%delete_all_msg%", "<br>\nAre you sure you want to delete all friends from the friends list? <button value = \"OK\" action=\"bypass _frienddeleteall_\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\">");

        } else if ("frienddelete".equals(cmd)) {
            String selected = player.getSessionVar("selFriends");
            if (selected != null) {
                for (String objId : selected.split(";")) {
                    if (!objId.isEmpty()) {
                        player.getFriendList().removeFriend(player.getFriendList().getList().get(Integer.parseInt(objId)).getName());
                    }
                }
            }
            player.setSessionVar("selFriends", null);
            html = html.replace("%friend_list%", getFriendList(player));
            html = html.replace("%selected_friend_list%", "");
            html = html.replace("%delete_all_msg%", "");
        } else if ("frienddeleteall".equals(cmd)) {
            List<Friend> friends = new ArrayList<>(1);
            friends.addAll(player.getFriendList().getList().values());
            for (Friend friend : friends) {
                player.getFriendList().removeFriend(friend.getName());
            }
            player.setSessionVar("selFriends", null);
            html = html.replace("%friend_list%", "");
            html = html.replace("%selected_friend_list%", "");
            html = html.replace("%delete_all_msg%", "");
        } else if ("friendblocklist".equals(cmd)) {
            html = html.replace("%block_list%", getBlockList(player));
            html = html.replace("%delete_all_msg%", "");
        } else if ("friendblockdeleteallconfirm".equals(cmd)) {
            html = html.replace("%block_list%", getBlockList(player));
            html = html.replace("%delete_all_msg%", "<br>\nDo you want to delete all characters from the block list? <button value = \"OK\" action=\"bypass _friendblockdeleteall_\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\" >");

        } else if ("friendblockdelete".equals(cmd)) {
            String objId = st.nextToken();
            if (objId != null && !objId.isEmpty()) {
                int objectId = Integer.parseInt(objId);
                String name = player.getBlockListMap().get(objectId);
                if (name != null) {
                    player.removeFromBlockList(name);
                }
            }
            html = html.replace("%block_list%", getBlockList(player));
            html = html.replace("%delete_all_msg%", "");
        } else if ("friendblockdeleteall".equals(cmd)) {
            List<String> bl = new ArrayList<>(1);
            bl.addAll(player.getBlockList());
            bl.forEach(player::removeFromBlockList);
            html = html.replace("%block_list%", "");
            html = html.replace("%delete_all_msg%", "");
        }
        ShowBoard.separateAndSend(html, player);
    }

    @Override
    public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5) {
        String html = HtmCache.getInstance().getHtml(Config.BBS_HOME_DIR + "bbs_block_list.htm", player);

        if ("_friendblockadd_".equals(bypass) && arg3 != null && !arg3.isEmpty()) {
            player.addToBlockList(arg3);
        }

        html = html.replace("%block_list%", getBlockList(player));
        html = html.replace("%delete_all_msg%", "");
        ShowBoard.separateAndSend(html, player);
    }

    private static String getFriendList(Player player) {
        StringBuilder friendList = new StringBuilder("");
        Map<Integer, Friend> fl = player.getFriendList().getList();
        for (Map.Entry<Integer, Friend> entry : fl.entrySet()) {
            friendList.append("<a action=\"bypass _friendlist_1_").append(entry.getKey()).append("\">").append(entry.getValue().getName()).append("</a> (").append(entry.getValue().isOnline() ? "on" : "off").append(") &nbsp;");
        }

        return friendList.toString();
    }

    private static String getSelectedList(Player player) {
        String selected = player.getSessionVar("selFriends");

        if (selected == null) {
            return "";
        }

        String[] sels = selected.split(";");
        StringBuilder selectedList = new StringBuilder("");
        for (String objectId : sels) {
            if (!objectId.isEmpty()) {
                selectedList.append("<a action=\"bypass _friendlist_2_").append(objectId).append("\">").append(player.getFriendList().getList().get(Integer.parseInt(objectId)).getName()).append("</a>;");
            }
        }

        return selectedList.toString();
    }

    private static String getBlockList(Player player) {
        StringBuilder blockList = new StringBuilder("");
        Map<Integer, String> bl = player.getBlockListMap();
        for (Integer objectId : bl.keySet()) {
            blockList.append(bl.get(objectId)).append("&nbsp; <a action=\"bypass _friendblockdelete_").append(objectId).append("\">Delete</a>&nbsp;&nbsp;");
        }

        return blockList.toString();
    }
}
