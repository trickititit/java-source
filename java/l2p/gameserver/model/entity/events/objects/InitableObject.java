package l2p.gameserver.model.entity.events.objects;

import java.io.Serializable;

import l2p.gameserver.model.entity.events.GlobalEvent;

public interface InitableObject extends Serializable {

    void initObject(GlobalEvent e);
}
