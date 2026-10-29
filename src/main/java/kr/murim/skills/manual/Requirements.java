package kr.murim.skills.manual;

import kr.murim.skills.MurimPlugin;
import kr.murim.skills.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 내공 / 외공 / 무기 숙련도 조건. 0이면 조건 없음. */
public record Requirements(int internalPower, int externalPower, Map<String, Integer> proficiency) {

    public static Requirements none() { return new Requirements(0, 0, Map.of()); }

    public static Requirements from(ConfigurationSection s) {
        if (s == null) return none();
        Map<String, Integer> prof = new LinkedHashMap<>();
        ConfigurationSection p = s.getConfigurationSection("proficiency");
        if (p != null) for (String k : p.getKeys(false)) prof.put(k, p.getInt(k));
        return new Requirements(s.getInt("internal_power", 0), s.getInt("external_power", 0), prof);
    }

    public boolean met(PlayerData d) {
        if (d.internalPower < internalPower || d.externalPower < externalPower) return false;
        for (var e : proficiency.entrySet()) if (d.prof(e.getKey()) < e.getValue()) return false;
        return true;
    }

    /** GUI 로어용 한 줄씩 (충족 ✔ / 미충족 ✘) */
    public List<Component> lines(PlayerData d, MurimPlugin plugin) {
        List<Component> out = new ArrayList<>();
        if (internalPower > 0) out.add(line("내공", d.internalPower, internalPower));
        if (externalPower > 0) out.add(line("외공", d.externalPower, externalPower));
        for (var e : proficiency.entrySet())
            out.add(line(plugin.profName(e.getKey()), d.prof(e.getKey()), e.getValue()));
        if (out.isEmpty()) out.add(Component.text("조건 없음", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        return out;
    }

    private static Component line(String label, int cur, int need) {
        boolean ok = cur >= need;
        return Component.text((ok ? "✔ " : "✘ ") + label + " " + cur + " / " + need,
                ok ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
    }
}
