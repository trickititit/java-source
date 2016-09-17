package l2p.gameserver.serverpackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import l2p.gameserver.model.pledge.Clan;
import l2p.gameserver.model.Skill;
import l2p.gameserver.model.pledge.SubUnit;

/**
 * Reworked: VISTALL
 */
public class PledgeSkillList extends L2GameServerPacket {

    private List<SkillInfo> _allSkills = Collections.emptyList();
    private List<UnitSkillInfo> _unitSkills = new ArrayList<>();

    public PledgeSkillList(Clan clan) {
        Collection<Skill> skills = clan.getSkills();
        _allSkills = new ArrayList<>(skills.size());

        _allSkills.addAll(skills.stream().map(sk -> new SkillInfo(sk.getId(), sk.getLevel())).collect(Collectors.toList()));

        for (SubUnit subUnit : clan.getAllSubUnits()) {
            _unitSkills.addAll(subUnit.getSkills().stream().map(sk -> new UnitSkillInfo(subUnit.getType(), sk.getId(), sk.getLevel())).collect(Collectors.toList()));
        }
    }

    @Override
    protected final void writeImpl() {
        writeEx(0x3a);
        writeD(_allSkills.size());
        writeD(_unitSkills.size());

        for (SkillInfo info : _allSkills) {
            writeD(info._id);
            writeD(info._level);
        }

        for (UnitSkillInfo info : _unitSkills) {
            writeD(info._type);
            writeD(info._id);
            writeD(info._level);
        }
    }

    static class SkillInfo {

        public int _id, _level;

        public SkillInfo(int id, int level) {
            _id = id;
            _level = level;
        }
    }

    static class UnitSkillInfo extends SkillInfo {

        private int _type;

        public UnitSkillInfo(int type, int id, int level) {
            super(id, level);
            _type = type;
        }
    }
}