package kr.murim.skills.skill;

import io.lumine.mythic.bukkit.MythicBukkit;
import kr.murim.skills.MurimPlugin;
import kr.murim.skills.data.PlayerData;
import kr.murim.skills.event.ManualRegisterEvent;
import kr.murim.skills.event.SkillCastEvent;
import kr.murim.skills.manual.ManualDef;
import kr.murim.skills.manual.SkillDef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkillService {
    private final MurimPlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SkillService(MurimPlugin plugin) { this.plugin = plugin; }

    // ---------- 비급 등록 / 스킬 습득 ----------

    public boolean register(Player p, ManualDef m) {
        PlayerData d = plugin.store().get(p);
        if (d.registeredManuals.contains(m.id())) return true;
        if (!m.register().met(d)) {
            p.sendMessage(Component.text("비급 등록 조건을 만족하지 못했습니다.", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f);
            return false;
        }
        ManualRegisterEvent ev = new ManualRegisterEvent(p, m);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return false;
        d.registeredManuals.add(m.id());
        p.sendMessage(Component.text("[" + m.name() + "] 비급을 등록했습니다.", NamedTextColor.GOLD));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
        return true;
    }

    public boolean learn(Player p, SkillDef s) {
        PlayerData d = plugin.store().get(p);
        if (d.learnedSkills.contains(s.key())) return true;
        if (!d.registeredManuals.contains(s.manualId())) return false;
        if (!s.learn().met(d)) {
            p.sendMessage(Component.text("스킬 습득 조건을 만족하지 못했습니다.", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.7f);
            return false;
        }
        d.learnedSkills.add(s.key());
        // 빈 슬롯이 있으면 자동 배치
        for (int i = 0; i < PlayerData.SLOT_COUNT; i++) {
            if (d.slots[i] == null) { d.slots[i] = s.key(); break; }
        }
        p.sendMessage(Component.text("[" + s.name() + "] 스킬을 습득했습니다.", NamedTextColor.GREEN));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        return true;
    }

    // ---------- 쿨타임 ----------

    public long remainingMs(Player p, SkillDef def) {
        Map<String, Long> m = cooldowns.get(p.getUniqueId());
        if (m == null) return 0;
        Long until = m.get(def.key());
        if (until == null) return 0;
        long rem = until - System.currentTimeMillis();
        if (rem <= 0) { m.remove(def.key()); return 0; }
        return rem;
    }

    public void clear(UUID uuid) { cooldowns.remove(uuid); }

    // ---------- 스킬 발동 ----------

    /** slot: 0~7 */
    public void cast(Player p, int slot) {
        PlayerData d = plugin.store().get(p);
        String key = d.slots[slot];
        if (key == null) { deny(p, "빈 슬롯입니다."); return; }
        SkillDef def = plugin.manuals().skill(key);
        if (def == null) { d.slots[slot] = null; deny(p, "존재하지 않는 스킬입니다."); return; }
        if (!d.learnedSkills.contains(key) || !d.registeredManuals.contains(def.manualId())) {
            deny(p, "사용할 수 없는 스킬입니다."); return;
        }
        if (remainingMs(p, def) > 0) { p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.6f); return; }
        if (plugin.stats().qi(d) < def.costQi()) { deny(p, "기력이 부족합니다."); return; }

        SkillCastEvent ev = new SkillCastEvent(p, def);
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) return;

        boolean ok = MythicBukkit.inst().getAPIHelper().castSkill(p, def.mythicSkill());
        if (!ok) { deny(p, "스킬 발동 실패: " + def.mythicSkill()); return; }

        plugin.stats().consume(d, def.costQi());
        cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
                .put(key, System.currentTimeMillis() + (long) (def.cooldown() * 1000));
    }

    private void deny(Player p, String msg) {
        p.showTitle(Title.title(Component.empty(), Component.text(msg, NamedTextColor.RED),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(200))));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
    }
}
