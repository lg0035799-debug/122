package kr.murim.skills.manual;

import kr.murim.skills.MurimPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public final class ManualRegistry {
    private final MurimPlugin plugin;
    private final Map<String, ManualDef> manuals = new LinkedHashMap<>();
    private final Map<String, SkillDef> skillsByKey = new HashMap<>();

    public ManualRegistry(MurimPlugin plugin) { this.plugin = plugin; }

    public void load() {
        manuals.clear();
        skillsByKey.clear();
        File dir = new File(plugin.getDataFolder(), "manuals");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            try {
                parse(f);
            } catch (Exception ex) {
                plugin.getLogger().warning("비급 로드 실패: " + f.getName() + " - " + ex.getMessage());
            }
        }
        plugin.getLogger().info("비급 " + manuals.size() + "개, 스킬 " + skillsByKey.size() + "개 로드");
    }

    private void parse(File f) {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        String id = y.getString("id", f.getName().replace(".yml", ""));
        String name = y.getString("name", id);
        Material mat = Material.matchMaterial(y.getString("material", "BOOK"));
        if (mat == null) mat = Material.BOOK;
        Requirements reg = Requirements.from(y.getConfigurationSection("register_requirements"));

        List<SkillDef> list = new ArrayList<>();
        int n = 0;
        for (Map<?, ?> raw : y.getMapList("skills")) {
            ConfigurationSection s = y.createSection("__skill" + (n++), raw);
            String sid = s.getString("id");
            if (sid == null) continue;
            SkillDef def = new SkillDef(id, sid, s.getString("name", sid),
                    s.getString("mythic_skill", sid), s.getString("icon", "default"),
                    s.getDouble("cooldown", 5), s.getDouble("cost_qi", 0),
                    Requirements.from(s.getConfigurationSection("learn_requirements")),
                    s.getStringList("description"));
            list.add(def);
            skillsByKey.put(def.key(), def);
        }
        manuals.put(id, new ManualDef(id, name, mat, y.getStringList("description"), reg, list));
    }

    public Collection<ManualDef> all() { return manuals.values(); }
    public ManualDef manual(String id) { return manuals.get(id); }
    public SkillDef skill(String key) { return key == null ? null : skillsByKey.get(key); }
}
