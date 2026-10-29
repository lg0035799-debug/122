package kr.murim.skills.event;

import kr.murim.skills.manual.SkillDef;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** 스킬 발동 직전에 호출 (조건 검사 통과 후). */
public final class SkillCastEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final SkillDef skill;
    private boolean cancelled;

    public SkillCastEvent(Player player, SkillDef skill) { this.player = player; this.skill = skill; }
    public Player getPlayer() { return player; }
    public SkillDef getSkill() { return skill; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean c) { cancelled = c; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
