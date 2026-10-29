package kr.murim.skills.manual;

import java.util.List;

public record SkillDef(String manualId, String id, String name, String mythicSkill, String icon,
                       double cooldown, double costQi, Requirements learn, List<String> description) {
    /** 저장/슬롯에 쓰이는 고유 키 */
    public String key() { return manualId + ":" + id; }
}
