package org.mcmonkey.sentinel.totalwar;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.citizensnpcs.api.command.Command;
import net.citizensnpcs.api.command.CommandContext;
import net.citizensnpcs.api.util.Paginator;

@SuppressWarnings("deprecation")
public class TotalWarCommands {

	@Command (aliases = {"tws"}, modifiers = {"regcreate"}, usage = "regcreate <name> [town]",
	desc = "Creates a new Regiment.", permission = "twsentinel.regcreate",
	min = 2, max = 3)
	public void createRegiment(CommandContext args, CommandSender sender) {
		String name = args.getString(1);
		Town town;
		if(args.argsLength() > 2 && (sender.hasPermission("twsentinel.admin") || !(sender instanceof Player))) {
			try {
				town = TownyUniverse.getDataSource().getTown(args.getString(2));
			} catch(NotRegisteredException e) {
				sender.sendMessage("§6§l [TW-Sentinel] §c There is no town named " + args.getString(2) + "!");
				return;
			}
		} else {
			if(!(sender instanceof Player)) {
				sender.sendMessage("§6§l [TW-Sentinel] §c You have to specify which town to create the unit for!");
				return;
			} else {
				try {
					Resident player = TownyAPI.getInstance().getDataSource().getResident(((Player)sender).getName());
					if(!player.hasTown()) {
						sender.sendMessage("§6§l [TW-Sentinel] §c You have to be an officer/leader of a town to create a unit!");
						return;
					}
					//TODO officer ranks
					if(!player.isMayor()) {
						sender.sendMessage("§6§l [TW-Sentinel] §c You have to be an officer/leader of a town to create a unit!");
						return;
					}
					town = player.getTown();
				} catch(NotRegisteredException e) {
					sender.sendMessage("§6§l [TW-Sentinel] §c You have to be an officer/leader of a town to create a unit!");
					return;
				}
			}
		}
		if(town == null) {
			sender.sendMessage("§6§l [TW-Sentinel] §c You have to be an officer/leader of a town to create a unit!");
			return;
		}
		SentinelRegiment.createUnit(name, town);
		sender.sendMessage("§6§l [TW-Sentinel] §2 Unit named " + name + " for town " + town.getFormattedName() + " has been succesfully created!");
	}
	
	@Command (aliases = {"tws"}, modifiers = {"reglist"}, usage = "reglist <town|nation> [name] [page]",
			desc = "Lists all regiments belonging to a town or nation.", permission = "twsentinel.reglist",
			min = 2, max = 4)
	public void listRegiments(CommandContext args, CommandSender sender) {
		Town town = null;
		Nation nation = null;
		int pageArgument = 2;
		if(args.getString(1).equalsIgnoreCase("town") || args.getString(1).equalsIgnoreCase("t")) {
			if(args.argsLength() > 2 && (sender.hasPermission("twsentinel.reglist.others") || !(sender instanceof Player))) {
				try {
					town = TownyUniverse.getDataSource().getTown(args.getString(2));
					pageArgument = 3;
				} catch(NotRegisteredException e) {
					sender.sendMessage("§6§l [TW-Sentinel] §c There is no town named " + args.getString(2) + "!");
					return;
				}
			} else {
				if(!(sender instanceof Player)) {
					sender.sendMessage("§6§l [TW-Sentinel] §c You have to specify which town's regiments to view!");
					return;
				} else {
					try {
						Resident player = TownyAPI.getInstance().getDataSource().getResident(((Player)sender).getName());
						if(!player.hasTown()) {
							sender.sendMessage("§6§l [TW-Sentinel] §c You have to specify which town's regiments to view!");
							return;
						}
						town = player.getTown();
					} catch(NotRegisteredException e) {
						sender.sendMessage("§6§l [TW-Sentinel] §c You have to specify which town's regiments to view!");
						return;
					}
				}
			}
		} else if(args.getString(1).equalsIgnoreCase("nation") || args.getString(1).equalsIgnoreCase("n")) {
			if(args.argsLength() > 2 && (sender.hasPermission("twsentinel.reglist.others") || !(sender instanceof Player))) {
				try {
					nation = TownyUniverse.getDataSource().getNation(args.getString(2));
					pageArgument = 3;
				} catch(NotRegisteredException e) {
					sender.sendMessage("§6§l [TW-Sentinel] §c There is no nation named " + args.getString(2) + "!");
					return;
				}
			} else {
				if(!(sender instanceof Player)) {
					sender.sendMessage("§6§l [TW-Sentinel] §c You have to specify which nation's regiments to view!");
					return;
				} else {
					try {
						Resident player = TownyAPI.getInstance().getDataSource().getResident(((Player)sender).getName());
						if(!player.hasTown()) {
							sender.sendMessage("§6§l [TW-Sentinel] §c You have to specify which nation's regiments to view!");
							return;
						}
						nation = player.getTown().getNation();
					} catch(NotRegisteredException e) {
						sender.sendMessage("§6§l [TW-Sentinel] §c You have to specify which nation's regiments to view!");
						return;
					}
				}
			}
		} else {
			sender.sendMessage("§6§l [TW-Sentinel] §c Please specify whether you want to list a town's (town/t) or nation's (nation/n) regiments.");
			return;
		}
		int page = 1;
		try {
			if(args.argsLength() > pageArgument) page = args.getInteger(pageArgument);
		} catch(NumberFormatException e) {
			sender.sendMessage("§6§l [TW-Sentinel] §c " + args.getString(pageArgument) + " is not a number!");
		}
		List<SentinelRegiment> regs = null;
		String head = null;
		if(town != null) {
			regs = SentinelRegiment.getRegiments(town);
			head = "town of " + town.getFormattedName();
		} else if(nation != null) {
			regs = SentinelRegiment.getRegiments(nation);
			head = "nation of " + nation.getFormattedName();
		} else {
			sender.sendMessage("oof");
			return;
		}
		Paginator paginator = new Paginator().header("§6§l [TW-Sentinel] §2 List of all regiments in " + head + ".");
		for(SentinelRegiment reg : regs) {
			paginator.addLine(reg.getTown().getFormattedName() + ": " + reg.getName() + "-" + reg.getSoldiers().size());
		}
		paginator.sendPage(sender, page);
	}
}
