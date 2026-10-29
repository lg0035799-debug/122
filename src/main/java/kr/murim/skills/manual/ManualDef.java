package kr.murim.skills.manual;

import org.bukkit.Material;

import java.util.List;

public record ManualDef(String id, String name, Material material, List<String> description,
                        Requirements register, List<SkillDef> skills) { }
