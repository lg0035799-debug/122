package kr.murim.skills.hud;

import kr.murim.skills.MurimPlugin;
import kr.murim.skills.data.PlayerData;
import kr.murim.skills.manual.SkillDef;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * 액션바 + 리소스팩 커스텀 폰트로 8칸 스킬바를 그립니다.
 * 글리프 배치는 assets/murim/font/hud.json 과 반드시 일치해야 합니다.
 */
public final class HudRenderer {
    static final Key HUD_FONT = Key.key("murim", "hud");
    static final Key ICON_FONT = Key.key("murim", "icon");
    public static final int ICON_ASCENT = 14;   // = 슬롯 ascent(16) - 2
    public static final int ICON_HEIGHT = 20;

    // hud.json 글리프
    private static final String SLOT_NORMAL = "\uE000";
    private static final String SLOT_COOLDOWN = "\uE001";
    private static final String SLOT_EMPTY = "\uE002";
    private static final int CD_DIGIT_BASE = 0xE200;   // 쿨타임 숫자 (0~9, 10='.')
    private static final int LABEL_DIGIT_BASE = 0xE210; // 키 번호 라벨

    private static final int SLOT_W = 24;               // 슬롯 크기(px)
    private static final int SLOT_ADV = SLOT_W + 1;     // 글리프 advance = 폭 + 1
    private static final int ICON_ADV = 21;             // 20px 아이콘 + 1
    private static final int PITCH = 26;                // 슬롯 간격(px)
    private static final int ICON_INSET = 2;
    private static final int LABEL_INSET = 3;
    private static final int LABEL_ADV = 6;             // 5px 숫자 + 1

    private static final TextColor LABEL = TextColor.color(0xCCCCCC);
    private static final TextColor DARK = TextColor.color(0x555555);

    private final MurimPlugin plugin;

    public HudRenderer(MurimPlugin plugin) { this.plugin = plugin; }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : plugin.mode().active()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.sendActionBar(render(p));
            }
        }, 1L, 2L);
    }

    public Component render(Player p) {
        PlayerData d = plugin.store().get(p);
        Bar bar = new Bar();
        for (int i = 0; i < PlayerData.SLOT_COUNT; i++) {
            int x = i * PITCH;
            bar.moveTo(x);
            SkillDef def = plugin.manuals().skill(d.slots[i]);
            long rem = def == null ? 0 : plugin.skills().remainingMs(p, def);
            boolean cd = rem > 0;

            bar.glyph(def == null ? SLOT_EMPTY : cd ? SLOT_COOLDOWN : SLOT_NORMAL, HUD_FONT, NamedTextColor.WHITE, SLOT_ADV);
            if (def != null) {
                bar.moveTo(x + ICON_INSET);
                bar.glyph(plugin.icons().glyph(def.icon()), ICON_FONT, cd ? DARK : NamedTextColor.WHITE, ICON_ADV);
            }
            if (cd) {
                String t = formatCooldown(rem);
                int w = -1;
                for (char c : t.toCharArray()) w += adv(c);
                bar.moveTo(x + (SLOT_W - w) / 2);
                for (char c : t.toCharArray()) {
                    int idx = c == '.' ? 10 : c - '0';
                    bar.glyph(String.valueOf((char) (CD_DIGIT_BASE + idx)), HUD_FONT, NamedTextColor.WHITE, adv(c));
                }
            } else {
                bar.moveTo(x + LABEL_INSET);
                bar.glyph(String.valueOf((char) (LABEL_DIGIT_BASE + i + 1)), HUD_FONT, LABEL, LABEL_ADV);
            }
        }
        // 전체 폭을 실제 그려진 폭과 맞춰야 액션바가 정중앙에 온다
        bar.moveTo((PlayerData.SLOT_COUNT - 1) * PITCH + SLOT_ADV);
        return bar.build();
    }

    private static int adv(char c) { return c == '.' ? 3 : 11; }  // 쿨타임 숫자: 글자폭 10(+1), '.'는 2(+1)

    private static String formatCooldown(long ms) {
        if (ms >= 10_000) return String.valueOf(Math.min(99, (ms + 999) / 1000));
        return String.format(Locale.ROOT, "%.1f", ms / 1000.0);
    }

    /** 픽셀 커서를 추적하며 글리프/공백을 이어 붙이는 빌더 */
    private static final class Bar {
        private final TextComponent.Builder b = Component.text();
        private int cursor = 0;

        void glyph(String s, Key font, TextColor color, int advance) {
            b.append(Component.text(s).font(font).color(color));
            cursor += advance;
        }

        void moveTo(int x) {
            int diff = x - cursor;
            if (diff == 0) return;
            boolean neg = diff < 0;
            int n = Math.abs(diff);
            StringBuilder sb = new StringBuilder();
            for (int bit = 128; bit >= 1; bit >>= 1) {
                while (n >= bit) { sb.append((char) ((neg ? 0xF800 : 0xF900) + bit)); n -= bit; }
            }
            b.append(Component.text(sb.toString()).font(HUD_FONT));
            cursor = x;
        }

        Component build() { return b.build(); }
    }
}
