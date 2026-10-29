package kr.murim.skills.skill;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import kr.murim.skills.MurimPlugin;
import kr.murim.skills.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.*;

/**
 * F키(손 바꾸기)로 스킬 모드 토글.
 * 스킬 모드 중에는 핫바를 9번 칸(index 8)에 "주차"시키고, 1~8번 키 입력을 스킬 발동으로 가로챕니다.
 * (현재 선택된 칸과 같은 번호 키는 서버로 패킷이 오지 않기 때문에 주차 칸이 필요합니다.)
 */
public final class SkillModeManager implements Listener {
    private static final int PARK_SLOT = 8;

    private final MurimPlugin plugin;
    private final Set<UUID> active = new LinkedHashSet<>();
    private final Map<UUID, Integer> savedSlot = new HashMap<>();

    public SkillModeManager(MurimPlugin plugin) { this.plugin = plugin; }

    public boolean isActive(Player p) { return active.contains(p.getUniqueId()); }

    public Set<UUID> active() { return new LinkedHashSet<>(active); }

    public void toggle(Player p) {
        if (isActive(p)) exit(p, true); else enter(p);
    }

    public void enter(Player p) {
        if (isActive(p)) return;
        PlayerData d = plugin.store().get(p);
        if (d.registeredManuals.isEmpty()) {
            p.sendMessage(Component.text("등록된 무공 비급이 없습니다. /murim 으로 비급을 등록하세요.", NamedTextColor.RED));
            return;
        }
        savedSlot.put(p.getUniqueId(), p.getInventory().getHeldItemSlot());
        p.getInventory().setHeldItemSlot(PARK_SLOT);
        active.add(p.getUniqueId());
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.4f);
    }

    public void exit(Player p, boolean notify) {
        if (!active.remove(p.getUniqueId())) return;
        Integer slot = savedSlot.remove(p.getUniqueId());
        if (slot != null) p.getInventory().setHeldItemSlot(slot);
        p.sendActionBar(Component.empty());
        if (notify) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 0.8f);
    }

    // ---------- 입력 ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        e.setCancelled(true);
        toggle(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (!isActive(p)) return;
        e.setCancelled(true);
        int ns = e.getNewSlot();
        if (ns < PlayerData.SLOT_COUNT) plugin.skills().cast(p, ns);
    }

    // ---------- 스킬 모드 중 기본 행동 차단 ----------

    private boolean blocks(Player p) {
        return isActive(p) && plugin.getConfig().getBoolean("skill-mode.block-actions", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) { if (blocks(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) { if (blocks(e.getPlayer())) e.setCancelled(true); }

    /** 플레이어의 "공격 입력"만 막는다. (Mythic 스킬이 주는 데미지는 영향 없음) */
    @EventHandler(ignoreCancelled = true)
    public void onAttack(PrePlayerAttackEntityEvent e) { if (blocks(e.getPlayer())) e.setCancelled(true); }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) { exit(e.getPlayer(), false); }
}
