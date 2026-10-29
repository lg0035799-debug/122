package kr.murim.skills;

import kr.murim.skills.data.PlayerDataStore;
import kr.murim.skills.data.StatService;
import kr.murim.skills.gui.MenuManager;
import kr.murim.skills.hud.HudRenderer;
import kr.murim.skills.hud.IconRegistry;
import kr.murim.skills.manual.ManualRegistry;
import kr.murim.skills.skill.SkillModeManager;
import kr.murim.skills.skill.SkillService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MurimPlugin extends JavaPlugin implements Listener {
    private StatService stats;
    private PlayerDataStore store;
    private IconRegistry icons;
    private ManualRegistry manuals;
    private SkillService skills;
    private SkillModeManager mode;
    private HudRenderer hud;
    private MenuManager menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "manuals").exists()) {
            saveResource("manuals/cheonma_sword.yml", false);
            saveResource("manuals/cheongwon_fist.yml", false);
        }
        if (!new File(getDataFolder(), "icons.yml").exists()) saveResource("icons.yml", false);

        stats = new StatService(this);
        store = new PlayerDataStore(this);
        icons = new IconRegistry(this);
        manuals = new ManualRegistry(this);
        reloadAll();

        skills = new SkillService(this);
        mode = new SkillModeManager(this);
        hud = new HudRenderer(this);
        menus = new MenuManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(mode, this);
        getServer().getPluginManager().registerEvents(menus, this);

        MurimCommand cmd = new MurimCommand(this);
        getCommand("murim").setExecutor(cmd);
        getCommand("murim").setTabCompleter(cmd);

        hud.start();
        // 기력 회복 (1초)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) stats.regen(store.get(p.getUniqueId()));
        }, 20L, 20L);
        // 자동 저장 (5분)
        Bukkit.getScheduler().runTaskTimer(this, () -> store.saveAll(), 20L * 300, 20L * 300);
    }

    @Override
    public void onDisable() {
        if (mode != null) for (Player p : Bukkit.getOnlinePlayers()) mode.exit(p, false);
        if (store != null) store.saveAll();
    }

    public void reloadAll() {
        reloadConfig();
        manuals.load();
        icons.load();
        icons.ensureAll(manuals);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        mode.exit(p, false);
        skills.clear(p.getUniqueId());
        store.saveAndUnload(p.getUniqueId());
    }

    public String profName(String id) {
        return getConfig().getString("proficiency-types." + id, id);
    }

    public StatService stats() { return stats; }
    public PlayerDataStore store() { return store; }
    public IconRegistry icons() { return icons; }
    public ManualRegistry manuals() { return manuals; }
    public SkillService skills() { return skills; }
    public SkillModeManager mode() { return mode; }
    public HudRenderer hud() { return hud; }
    public MenuManager menus() { return menus; }
}
