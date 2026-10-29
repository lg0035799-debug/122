package kr.murim.skills.hud;

import kr.murim.skills.MurimPlugin;
import kr.murim.skills.manual.ManualDef;
import kr.murim.skills.manual.ManualRegistry;
import kr.murim.skills.manual.SkillDef;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 아이콘 이름 -> 폰트 글자(코드포인트) 매핑. icons.yml에 저장됩니다.
 * 새 아이콘을 쓰면 자동으로 코드포인트가 할당되고, /murim genfont 로 icon.json 을 만들 수 있습니다.
 */
public final class IconRegistry {
    public static final String DEFAULT = "default";
    private static final int DEFAULT_CP = 0xE2FF;
    private static final int FIRST_CP = 0xE300;

    private final MurimPlugin plugin;
    private final File file;
    private final Map<String, Integer> codepoints = new LinkedHashMap<>();

    public IconRegistry(MurimPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "icons.yml");
    }

    public void load() {
        codepoints.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String k : y.getKeys(false)) {
            try { codepoints.put(k, Integer.parseInt(y.getString(k, ""), 16)); }
            catch (NumberFormatException ignored) { }
        }
        codepoints.putIfAbsent(DEFAULT, DEFAULT_CP);
    }

    public void ensureAll(ManualRegistry manuals) {
        boolean changed = false;
        for (ManualDef m : manuals.all()) {
            for (SkillDef s : m.skills()) {
                if (!codepoints.containsKey(s.icon())) {
                    codepoints.put(s.icon(), nextFree());
                    changed = true;
                }
            }
        }
        if (changed) save();
    }

    private int nextFree() {
        int max = FIRST_CP - 1;
        for (int cp : codepoints.values()) if (cp != DEFAULT_CP) max = Math.max(max, cp);
        return max + 1;
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        codepoints.forEach((k, v) -> y.set(k, String.format("%04X", v)));
        try { y.save(file); } catch (IOException ex) { plugin.getLogger().warning("icons.yml 저장 실패"); }
    }

    public String glyph(String icon) {
        Integer cp = codepoints.get(icon);
        if (cp == null) cp = DEFAULT_CP;
        return String.valueOf((char) (int) cp);
    }

    /** 리소스팩용 icon.json 생성 (아이콘 PNG는 assets/murim/textures/font/skill/<이름>.png, 16x16 권장) */
    public File writeFontJson() throws IOException {
        File out = new File(plugin.getDataFolder(), "generated/icon.json");
        out.getParentFile().mkdirs();
        StringBuilder sb = new StringBuilder("{\n  \"providers\": [\n");
        int i = 0;
        for (var e : codepoints.entrySet()) {
            sb.append("    {\"type\":\"bitmap\",\"file\":\"murim:font/skill/").append(e.getKey())
              .append(".png\",\"ascent\":").append(HudRenderer.ICON_ASCENT)
              .append(",\"height\":16,\"chars\":[\"\\u").append(String.format("%04X", e.getValue())).append("\"]}");
            sb.append(++i < codepoints.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        Files.writeString(out.toPath(), sb.toString(), StandardCharsets.UTF_8);
        return out;
    }
}
