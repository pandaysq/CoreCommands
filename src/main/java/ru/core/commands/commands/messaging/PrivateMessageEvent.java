package ru.core.commands.commands.messaging;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PrivateMessageEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player sender;
    private final Player receiver;

    public PrivateMessageEvent(Player sender, Player receiver) {
        this.sender = sender;
        this.receiver = receiver;
        LastMessageStore.remember(sender, receiver);
        LastMessageStore.remember(receiver, sender);
    }

    public Player sender() { return sender; }
    public Player receiver() { return receiver; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}