package kr.murim.skills.event;

import kr.murim.skills.manual.ManualDef;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** 비급 등록 직전에 호출. 다른 플러그인이 취소/후처리 가능. */
public final class ManualRegisterEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ManualDef manual;
    private boolean cancelled;

    public ManualRegisterEvent(Player player, ManualDef manual) { this.player = player; this.manual = manual; }
    public Player getPlayer() { return player; }
    public ManualDef getManual() { return manual; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean c) { cancelled = c; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
