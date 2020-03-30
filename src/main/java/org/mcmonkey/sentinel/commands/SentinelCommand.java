package org.mcmonkey.sentinel.commands;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.command.CommandContext;
import net.citizensnpcs.api.command.CommandManager;
import net.citizensnpcs.api.command.Injector;
import net.citizensnpcs.api.command.Requirements;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.trait.Owner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mcmonkey.sentinel.SentinelPlugin;
import org.mcmonkey.sentinel.SentinelTrait;
import org.mcmonkey.sentinel.totalwar.TotalWarCommands;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Helper class for the Sentinel command.
 */
public class SentinelCommand {

    /**
     * Output string representing a message color.
     */
    public static final String colorBasic = ChatColor.YELLOW.toString();

    /**
     * Output string representing a success prefix.
     */
    public static final String prefixGood = ChatColor.DARK_GREEN + "[Sentinel] " + colorBasic;

    /**
     * Output string representing a failure prefix.
     */
    public static final String prefixBad = ChatColor.DARK_GREEN + "[Sentinel] " + ChatColor.RED;

    /**
     * The "/sentinel" command manager.
     */
    public static CommandManager manager;

    /**
     * Prepares the command handling system.
     */
    public static void buildCommandHandler() {
        manager = new CommandManager();
        manager.setInjector(new Injector());
        grabCommandMethodMap(manager);
        manager.register(SentinelAttackCommands.class);
        manager.register(SentinelChaseCommands.class);
        manager.register(SentinelGreetingCommands.class);
        manager.register(SentinelHealthCommands.class);
        manager.register(SentinelInfoCommands.class);
        manager.register(SentinelIntelligenceCommands.class);
        manager.register(SentinelTargetCommands.class);
        
        manager.register(TotalWarCommands.class);
    }

    private static Map<String, Method> sentinelCommandMethodMap;

    private static void grabCommandMethodMap(CommandManager instance) {
        try {
            Field field = CommandManager.class.getDeclaredField("commands");
            field.setAccessible(true);
            sentinelCommandMethodMap = (Map<String, Method>) field.get(instance);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static boolean isSentinelRequired(String command, String modifier) {
        Method method = sentinelCommandMethodMap.get((command + " " + modifier).toLowerCase());
        if (method == null) {
            return false;
        }
        Requirements[] req = method.getDeclaredAnnotationsByType(Requirements.class);
        if (req == null || req.length == 0) {
            return false;
        }
        for (Class<? extends Trait> traitClass : req[0].traits()) {
            if (traitClass.equals(SentinelTrait.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canCommandNpc(CommandSender sender, NPC npc) {
    	if(sender instanceof Player) {
    		if(npc.getTrait(Owner.class).isOwnedBy(sender)) return true;
    		OfflinePlayer owner = Bukkit.getOfflinePlayer(npc.getTrait(Owner.class).getOwnerId());
    		if(owner == null) return true;
    		Nation ownerNation;
    		Nation senderNation;
    		try {
    			Resident ownerboi = TownyUniverse.getDataSource().getResident(owner.getName());
    			Resident senderboi = TownyUniverse.getDataSource().getResident(((Player) sender).getName());
    			if(!ownerboi.hasTown() || !senderboi.hasTown()) return false;
    			ownerNation = ownerboi.getTown().getNation();
				senderNation = senderboi.getTown().getNation();
			} catch (NotRegisteredException e) {return false;}
    		return ownerNation == senderNation;
    	} else return true;
    }
    
    /**
     * Handles a player or server command.
     */
    public static boolean onCommand(SentinelPlugin instance, CommandSender sender, Command command, String label, String[] args) {
        String modifier = args.length > 0 ? args[0] : "";
        if (!manager.hasCommand(command, modifier) && !modifier.isEmpty()) {
            String closest = manager.getClosestCommandModifier(command.getName(), modifier);
            if (!closest.isEmpty()) {
                sender.sendMessage(prefixBad + "Unknown command. Did you mean:");
                sender.sendMessage(prefixGood + " /" + command.getName() + " " + closest);
            }
            else {
                sender.sendMessage(prefixBad + "Unknown command.");
            }
            return true;
        }
        NPC selected = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
        SentinelTrait sentinel = null;
        CommandContext context = new CommandContext(sender, args);
        if (context.hasValueFlag("id") && sender.hasPermission("npc.select")) {
            selected = CitizensAPI.getNPCRegistry().getById(context.getFlagInteger("id"));
        }
        if (selected != null) {
            if (selected.hasTrait(SentinelTrait.class)) {
                sentinel = selected.getTrait(SentinelTrait.class);
            }
        }
        Object[] methodArgs;
        if (isSentinelRequired(command.getName(), modifier)) {
            if (sentinel == null) {
                if (selected == null) {
                    sender.sendMessage(prefixBad + "Must have a Sentinel NPC selected!");
                }
                else {
                    sender.sendMessage(prefixBad + "Selected NPC is not a Sentinel! Use /trait sentinel to ensure an NPC becomes a Sentinel.");
                }
                return true;
            }
            if (!canCommandNpc(sender, selected) && !sender.hasPermission("citizens.admin")) {
                sender.sendMessage(prefixBad + "You do not own this NPC (and you are not an admin).");
                return true;
            }
            methodArgs = new Object[]{sender, sentinel};
        }
        else {
            methodArgs = new Object[]{ sender };
        }
        return manager.executeSafe(command, args, sender, methodArgs);
    }
}
