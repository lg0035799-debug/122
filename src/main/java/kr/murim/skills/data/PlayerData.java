package kr.murim.skills.data;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 플레이어 한 명의 무공 관련 데이터. 슬롯 값은 "비급id:스킬id" 형식의 스킬 키. */
public final class PlayerData {
    public static final int SLOT_COUNT = 8;

    public final UUID uuid;
    public int internalPower;   // 내공
    public int externalPower;   // 외공
    public final Map<String, Integer> proficiency = new HashMap<>();
    public final Set<String> registeredManuals = new LinkedHashSet<>();
    public final Set<String> learnedSkills = new LinkedHashSet<>();
    public final String[] slots = new String[SLOT_COUNT];
    public double qi = Double.MAX_VALUE; // 현재 기력 (최대치로 자동 보정됨)

    public PlayerData(UUID uuid) { this.uuid = uuid; }

    public int prof(String type) { return proficiency.getOrDefault(type, 0); }
}
