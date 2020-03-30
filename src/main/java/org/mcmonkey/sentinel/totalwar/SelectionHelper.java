package org.mcmonkey.sentinel.totalwar;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class SelectionHelper implements Listener {

	private Map<Player, SentinelRegiment> regSelects = new HashMap<Player, SentinelRegiment>();
	private Map<Player, CommandGroup> groupSelects = new HashMap<Player, CommandGroup>();
	
	@EventHandler
	public void onDisconnect(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		regSelects.put(p, null);
		groupSelects.put(p, null);
	}
	
	public void selectRegiment(Player player, SentinelRegiment regiment) {
		regSelects.put(player, regiment);
	}
	
	public void selectCommandGroup(Player player, CommandGroup group) {
		groupSelects.put(player, group);
	}
	
	public SentinelRegiment getSelectedRegiment(Player player) {
		return regSelects.get(player);
	}
	
	public CommandGroup getCommandGroup(Player player) {
		return groupSelects.get(player);
	}
}
