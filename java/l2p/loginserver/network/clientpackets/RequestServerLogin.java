package l2p.loginserver.network.clientpackets;

import l2p.loginserver.Config;
import l2p.loginserver.GameServerManager;
import l2p.loginserver.accounts.Account;
import l2p.loginserver.network.L2LoginClient;
import l2p.loginserver.network.SessionKey;
import l2p.loginserver.network.L2LoginClient.LoginClientState;
import l2p.loginserver.network.gameservercon.GameServer;
import l2p.loginserver.network.serverpackets.PlayOk;
import l2p.loginserver.network.serverpackets.LoginFail.LoginFailReason;

/**
 * Fromat is ddc d: first part of session id d: second part of session id c:
 * server ID
 */
public class RequestServerLogin extends L2LoginClientPacket {

    private int _loginOkID1;
    private int _loginOkID2;
    private int _serverId;

    @Override
    protected void readImpl() {
        _loginOkID1 = readD();
        _loginOkID2 = readD();
        _serverId = readC();
    }

    @Override
    protected void runImpl() {
        L2LoginClient client = getClient();
        SessionKey skey = client.getSessionKey();
        if (skey == null || !skey.checkLoginPair(_loginOkID1, _loginOkID2)) {
            client.close(LoginFailReason.REASON_ACCESS_FAILED);
            return;
        }

        Account account = client.getAccount();
        GameServer gs = GameServerManager.getInstance().getGameServerById(_serverId);
        if (gs == null || !gs.isAuthed() || gs.isGmOnly() && account.getAccessLevel() < 100 || gs.getOnline() >= gs.getMaxPlayers() && account.getAccessLevel() < 50) {
            client.close(LoginFailReason.REASON_ACCESS_FAILED);
            return;
        }

        if (Config.FAKE_LOGIN_SERVER && (client.getState() == LoginClientState.FAKE_LOGIN)) {
            client.close(LoginFailReason.REASON_ACCESS_FAILED);
            return;
        }

        account.setLastServer(_serverId);
        account.update();

        client.close(new PlayOk(skey));
    }
}