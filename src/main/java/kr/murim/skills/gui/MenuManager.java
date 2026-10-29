package kr.murim.skills.gui;

import kr.murim.skills.MurimPlugin;
import kr.murim.skills.data.PlayerData;
import kr.murim.skills.manual.ManualDef;
import kr.murim.skills.manual.SkillDef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 간단한 3단계 GUI: 비급 목록(등록) -> 스킬 목록(습득) -> 슬롯 배치.
 * 나중에 커스텀 텍스처 GUI로 바꾸려면 title 에 폰트 글리프를 넣는 방식으로 확장하면 됩니다.
 */
public final class MenuManager implements Listener {
    private static final class Menu implements InventoryHolder {
        final Inventory inv;
        final Map<Integer, Runnable> actions = new HashMap<>();

        Menu(Component title, int rows) { this.inv = Bukkit.createInventory(this, rows * 9, title); }

        @Override public Inventory getInventory() { return inv; }
    }

    private final MurimPlugin plugin;

    public MenuManager(MurimPlugin plugin) { this.plugin = plugin; }

    // ---------- 화면 1: 비급 목록 ----------

    public void openMain(Player p) {
        PlayerData d = plugin.store().get(p);
        Menu m = new Menu(Component.text("무공 비급"), 6);
        int slot = 0;
        for (ManualDef mf : plugin.manuals().all()) {
            if (slot >= 45) break;
            boolean reg = d.registeredManuals.contains(mf.id());
            List<Component> lore = new ArrayList<>();
            for (String s : mf.description()) lore.add(line(s, NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(line("등록 조건", NamedTextColor.GOLD));
            lore.addAll(mf.register().lines(d, plugin));
            lore.add(Component.empty());
            lore.add(line(reg ? "클릭: 스킬 목록 보기" : "클릭: 비급 등록", NamedTextColor.YELLOW));
            m.inv.setItem(slot, item(mf.material(),
                    line(mf.name() + (reg ? " [등록됨]" : ""), reg ? NamedTextColor.GREEN : NamedTextColor.WHITE), lore));
            m.actions.put(slot, () -> {
                if (reg || plugin.skills().register(p, mf)) openManual(p, mf);
            });
            slot++;
        }
        p.openInventory(m.inv);
    }

    // ---------- 화면 2: 스킬 목록 ----------

    public void openManual(Player p, ManualDef mf) {
        PlayerData d = plugin.store().get(p);
        Menu m = new Menu(Component.text(mf.name()), 6);
        int slot = 0;
        for (SkillDef s : mf.skills()) {
            if (slot >= 45) break;
            boolean learned = d.learnedSkills.contains(s.key());
            List<Component> lore = new ArrayList<>();
            for (String t : s.description()) lore.add(line(t, NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(line(String.format("쿨타임 %.1f초 / 기력 %.0f", s.cooldown(), s.costQi()), NamedTextColor.AQUA));
            lore.add(Component.empty());
            if (learned) {
                int idx = indexOf(d, s.key());
                lore.add(line(idx >= 0 ? "배치: " + (idx + 1) + "번 슬롯" : "배치: 없음", NamedTextColor.GREEN));
                lore.add(line("클릭: 슬롯 배치", NamedTextColor.YELLOW));
            } else {
                lore.add(line("습득 조건", NamedTextColor.GOLD));
                lore.addAll(s.learn().lines(d, plugin));
                lore.add(Component.empty());
                lore.add(line("클릭: 스킬 습득", NamedTextColor.YELLOW));
            }
            m.inv.setItem(slot, item(learned ? Material.ENCHANTED_BOOK : Material.BOOK,
                    line(s.name() + (learned ? " [습득]" : ""), learned ? NamedTextColor.GREEN : NamedTextColor.WHITE), lore));
            m.actions.put(slot, () -> {
                if (learned) openSlotPicker(p, s);
                else if (plugin.skills().learn(p, s)) openManual(p, mf);
            });
            slot++;
        }
        m.inv.setItem(49, item(Material.ARROW, line("뒤로", NamedTextColor.WHITE), List.of()));
        m.actions.put(49, () -> openMain(p));
        p.openInventory(m.inv);
    }

    // ---------- 화면 3: 슬롯 배치 ----------

    public void openSlotPicker(Player p, SkillDef s) {
        PlayerData d = plugin.store().get(p);
        Menu m = new Menu(Component.text(s.name() + " - 슬롯 배치"), 3);
        for (int i = 0; i < PlayerData.SLOT_COUNT; i++) {
            final int idx = i;
            String cur = d.slots[i];
            SkillDef other = plugin.manuals().skill(cur);
            Material mat;
            List<Component> lore = new ArrayList<>();
            if (s.key().equals(cur)) {
                mat = Material.LIME_STAINED_GLASS_PANE;
                lore.add(line("현재 이 스킬이 배치됨", NamedTextColor.GREEN));
            } else if (other != null) {
                mat = Material.ORANGE_STAINED_GLASS_PANE;
                lore.add(line("현재: " + other.name(), NamedTextColor.GOLD));
                lore.add(line("클릭하면 교체됩니다", NamedTextColor.YELLOW));
            } else {
                mat = Material.GRAY_STAINED_GLASS_PANE;
                lore.add(line("빈 슬롯", NamedTextColor.GRAY));
                lore.add(line("클릭: 여기에 배치", NamedTextColor.YELLOW));
            }
            ItemStack it = item(mat, line((i + 1) + "번 슬롯", NamedTextColor.WHITE), lore);
            it.setAmount(i + 1);
            m.inv.setItem(10 + i, it);
            m.actions.put(10 + i, () -> {
                for (int j = 0; j < PlayerData.SLOT_COUNT; j++) if (s.key().equals(d.slots[j])) d.slots[j] = null;
                d.slots[idx] = s.key();
                openSlotPicker(p, s);
            });
        }
        m.inv.setItem(22, item(Material.BARRIER, line("배치 해제", NamedTextColor.RED), List.of()));
        m.actions.put(22, () -> {
            for (int j = 0; j < PlayerData.SLOT_COUNT; j++) if (s.key().equals(d.slots[j])) d.slots[j] = null;
            openSlotPicker(p, s);
        });
        m.inv.setItem(18, item(Material.ARROW, line("뒤로", NamedTextColor.WHITE), List.of()));
        m.actions.put(18, () -> {
            ManualDef mf = plugin.manuals().manual(s.manualId());
            if (mf != null) openManual(p, mf); else openMain(p);
        });
        p.openInventory(m.inv);
    }

    // ---------- 이벤트 ----------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu m)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        Runnable r = m.actions.get(e.getSlot());
        if (r != null) Bukkit.getScheduler().runTask(plugin, r);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    // ---------- 유틸 ----------

    private static int indexOf(PlayerData d, String key) {
        for (int i = 0; i < d.slots.length; i++) if (key.equals(d.slots[i])) return i;
        return -1;
    }

    private static Component line(String s, TextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material mat, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(meta);
        return it;
    }
}
