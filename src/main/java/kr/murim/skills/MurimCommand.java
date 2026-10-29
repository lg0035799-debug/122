package kr.murim.skills;

import kr.murim.skills.data.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MurimCommand implements TabExecutor {
    private final MurimPlugin plugin;

    public MurimCommand(MurimPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        String sub = a.length == 0 ? "menu" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu" -> {
                if (s instanceof Player p) plugin.menus().openMain(p);
            }
            case "mode" -> {
                if (s instanceof Player p) plugin.mode().toggle(p);
            }
            case "stat" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
                if (t == null) { msg(s, "대상을 찾을 수 없습니다.", NamedTextColor.RED); return true; }
                PlayerData d = plugin.store().get(t);
                msg(s, t.getName() + " | 내공 " + d.internalPower + " / 외공 " + d.externalPower
                        + String.format(" / 기력 %.0f/%.0f", plugin.stats().qi(d), plugin.stats().maxQi(d)), NamedTextColor.GOLD);
                StringBuilder sb = new StringBuilder("숙련도: ");
                for (String id : plugin.getConfig().getConfigurationSection("proficiency-types").getKeys(false))
                    sb.append(plugin.profName(id)).append(' ').append(d.prof(id)).append("  ");
                msg(s, sb.toString(), NamedTextColor.GRAY);
            }
            case "set" -> {
                if (!s.hasPermission("murim.admin")) { msg(s, "권한이 없습니다.", NamedTextColor.RED); return true; }
                return set(s, a);
            }
            case "reload" -> {
                if (!s.hasPermission("murim.admin")) { msg(s, "권한이 없습니다.", NamedTextColor.RED); return true; }
                plugin.reloadAll();
                msg(s, "리로드 완료", NamedTextColor.GREEN);
            }
            case "genfont" -> {
                if (!s.hasPermission("murim.admin")) { msg(s, "권한이 없습니다.", NamedTextColor.RED); return true; }
                try {
                    var f = plugin.icons().writeFontJson();
                    msg(s, "생성됨: " + f.getPath() + " (리소스팩 assets/murim/font/icon.json 에 덮어쓰세요)", NamedTextColor.GREEN);
                } catch (Exception ex) {
                    msg(s, "생성 실패: " + ex.getMessage(), NamedTextColor.RED);
                }
            }
            default -> msg(s, "/murim [menu|mode|stat|set|reload|genfont]", NamedTextColor.GRAY);
        }
        return true;
    }

    /** /murim set <플레이어> internal|external <값>  |  /murim set <플레이어> prof <종류> <값> */
    private boolean set(CommandSender s, String[] a) {
        if (a.length < 4) {
            msg(s, "/murim set <플레이어> internal|external <값>  또는  prof <종류> <값>", NamedTextColor.GRAY);
            return true;
        }
        Player t = Bukkit.getPlayerExact(a[1]);
        if (t == null) { msg(s, "플레이어를 찾을 수 없습니다.", NamedTextColor.RED); return true; }
        PlayerData d = plugin.store().get(t);
        try {
            switch (a[2].toLowerCase(Locale.ROOT)) {
                case "internal" -> d.internalPower = Integer.parseInt(a[3]);
                case "external" -> d.externalPower = Integer.parseInt(a[3]);
                case "prof" -> {
                    if (a.length < 5) { msg(s, "prof <종류> <값>", NamedTextColor.RED); return true; }
                    d.proficiency.put(a[3], Integer.parseInt(a[4]));
                }
                default -> { msg(s, "internal / external / prof 중 선택", NamedTextColor.RED); return true; }
            }
        } catch (NumberFormatException ex) {
            msg(s, "숫자를 입력하세요.", NamedTextColor.RED);
            return true;
        }
        msg(s, "적용했습니다.", NamedTextColor.GREEN);
        return true;
    }

    private static void msg(CommandSender s, String text, NamedTextColor color) {
        s.sendMessage(Component.text(text, color));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("menu", "mode", "stat"));
            if (s.hasPermission("murim.admin")) out.addAll(List.of("set", "reload", "genfont"));
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("set") || a[0].equalsIgnoreCase("stat"))) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (a.length == 3 && a[0].equalsIgnoreCase("set")) {
            out.addAll(List.of("internal", "external", "prof"));
        } else if (a.length == 4 && a[0].equalsIgnoreCase("set") && a[2].equalsIgnoreCase("prof")) {
            out.addAll(plugin.getConfig().getConfigurationSection("proficiency-types").getKeys(false));
        }
        String prefix = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }
}
