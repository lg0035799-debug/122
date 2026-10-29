package kr.murim.skills.data;

import kr.murim.skills.MurimPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 현재는 플레이어별 YAML 파일 저장. 나중에 SQLite/MySQL로 바꿀 때 이 클래스만 교체하면 됩니다. */
public final class PlayerDataStore {
    private final MurimPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataStore(MurimPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "playerdata");
        dir.mkdirs();
    }

    public PlayerData get(UUID uuid) { return cache.computeIfAbsent(uuid, this::load); }

    public PlayerData get(org.bukkit.entity.Player p) { return get(p.getUniqueId()); }

    private PlayerData load(UUID uuid) {
        PlayerData d = new PlayerData(uuid);
        File f = new File(dir, uuid + ".yml");
        var cfg = plugin.getConfig();
        if (!f.exists()) {
            d.internalPower = cfg.getInt("default-stats.internal-power", 10);
            d.externalPower = cfg.getInt("default-stats.external-power", 10);
            return d;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        d.internalPower = y.getInt("internal", 0);
        d.externalPower = y.getInt("external", 0);
        d.qi = y.getDouble("qi", Double.MAX_VALUE);
        ConfigurationSection p = y.getConfigurationSection("proficiency");
        if (p != null) for (String k : p.getKeys(false)) d.proficiency.put(k, p.getInt(k));
        d.registeredManuals.addAll(y.getStringList("manuals"));
        d.learnedSkills.addAll(y.getStringList("skills"));
        List<String> slots = y.getStringList("slots");
        for (int i = 0; i < Math.min(slots.size(), PlayerData.SLOT_COUNT); i++) {
            String s = slots.get(i);
            d.slots[i] = (s == null || s.isEmpty()) ? null : s;
        }
        return d;
    }

    public void save(PlayerData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("internal", d.internalPower);
        y.set("external", d.externalPower);
        y.set("qi", Math.min(d.qi, plugin.stats().maxQi(d)));
        d.proficiency.forEach((k, v) -> y.set("proficiency." + k, v));
        y.set("manuals", new ArrayList<>(d.registeredManuals));
        y.set("skills", new ArrayList<>(d.learnedSkills));
        List<String> slots = new ArrayList<>();
        for (String s : d.slots) slots.add(s == null ? "" : s);
        y.set("slots", slots);
        try {
            y.save(new File(dir, d.uuid + ".yml"));
        } catch (IOException ex) {
            plugin.getLogger().severe("플레이어 데이터 저장 실패: " + d.uuid + " - " + ex.getMessage());
        }
    }

    public void saveAll() { cache.values().forEach(this::save); }

    public void saveAndUnload(UUID uuid) {
        PlayerData d = cache.remove(uuid);
        if (d != null) save(d);
    }
}
