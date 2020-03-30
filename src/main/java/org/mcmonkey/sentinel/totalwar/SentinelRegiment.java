package org.mcmonkey.sentinel.totalwar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.mcmonkey.sentinel.SentinelTrait;
import org.mcmonkey.sentinel.totalwar.CommandGroup.FormationParams;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

public class SentinelRegiment {
	
//TODO save to file
	
	private Town town;
	
	private List<NPC> soldiers = new ArrayList<NPC>();
	private NPC officer;
	
	private static List<SentinelRegiment> units = new ArrayList<SentinelRegiment>();
	
	private String name;
	
	public static SentinelRegiment createUnit(String name, Town town) {
		if(getByName(name) != null) return null;
		SentinelRegiment reg = new SentinelRegiment();
		units.add(reg);
		return reg;
	}
	
	public String getName() {
		return name;
	}
	
	public static SentinelRegiment getByName(String name) {
		for(SentinelRegiment reg : getAllUnits()) {
			if(reg.getName().equalsIgnoreCase(name)) {
				return reg;
			}
		}
		return null;
	}
	
	//0 is x, 1 is z
	public int[] getAverageLocation() {
		int x = 0;
		int z = 0;
		int N = getSoldiers().size();
		for(NPC n : getSoldiers()) {
			Location l = n.getEntity().getLocation();
			x += l.getX();
			z += l.getZ();
		}
		return new int[]{x / N, z / N};
	}
	
	public static List<SentinelRegiment> getAllUnits() {
		return new ArrayList<SentinelRegiment>(units);
	}
	
	public List<NPC> getSoldiers() {
		return new ArrayList<NPC>(soldiers);
	}
	
	public NPC getOfficer() {
		return officer;
	}
	
	public CommandGroup getCommandGroup() {
		return CommandGroup.getForUnit(this);
	}
	
	public Town getTown() {
		return town;
	}
	
	public boolean addSoldier(NPC npc, boolean ignoreChecks) {
		if(!npc.hasTrait(SentinelTrait.class)) return false;
		if(ignoreChecks) {
			return soldiers.add(npc);
		}
		UUID ownerId = npc.getTrait(Owner.class).getOwnerId();
		if(ownerId == null) return false;
		OfflinePlayer npcOwner = Bukkit.getOfflinePlayer(ownerId);
		if(npcOwner == null) return false;
		Town npcTown = null;
		try {
			npcTown = TownyUniverse.getDataSource().getResident(npcOwner.getName()).getTown();
		} catch (NotRegisteredException e) {return false;}
		if(npcTown != town) return false;
		return soldiers.add(npc);
	}
	
	public boolean setOfficer(NPC officer, boolean ignoreChecks) {
		if(!officer.hasTrait(SentinelTrait.class)) return false;
		if(ignoreChecks) {
			this.officer = officer;
			return true;
		}
		UUID ownerId = officer.getTrait(Owner.class).getOwnerId();
		if(ownerId == null) return false;
		OfflinePlayer npcOwner = Bukkit.getOfflinePlayer(ownerId);
		if(npcOwner == null) return false;
		Town npcTown = null;
		try {
			npcTown = TownyUniverse.getDataSource().getResident(npcOwner.getName()).getTown();
		} catch (NotRegisteredException e) {return false;}
		if(npcTown != town) return false;
		this.officer = officer;
		return true;
	}
	
	public static SentinelRegiment getRegiment(NPC npc) {
		for(SentinelRegiment reg : getAllUnits()) {
			for(NPC n : reg.getSoldiers()) {
				if(n == npc) return reg;
			}
		}
		return null;
	}
	
	public static List<SentinelRegiment> getRegiments(Town town) {
		List<SentinelRegiment> ret = new ArrayList<SentinelRegiment>();
		for(SentinelRegiment reg : getAllUnits()) {
			if(reg.getTown() == town) ret.add(reg);
		}
		return ret;
	}
	
	public static List<SentinelRegiment> getRegiments(Nation nat) {
		List<SentinelRegiment> ret = new ArrayList<SentinelRegiment>();
		for(SentinelRegiment reg : getAllUnits()) {
			try {
				if(reg.getTown().getNation() == nat) ret.add(reg);
			} catch (NotRegisteredException e) {}
		}
		return ret;
	}
	
	//returns an array where element 0 is x, element 1 is z
	private static double[] rotate(double x, double z, double angle) {
		double[] ret = new double[2];
		double boomer = angle % 360;
		//convert to radians
		double rAngle = boomer * Math.PI / 180;
		//rotate
		ret[0] = x * Math.cos(rAngle) + z * Math.sin(rAngle);
		ret[1] = z * Math.cos(rAngle) - x * Math.sin(rAngle);
		return ret;
	}
	
	private static Vector rotate(Vector loc, double angle) {
		double[] rotated = rotate(loc.getX(), loc.getZ(), angle);
		loc.setX(rotated[0]);
		loc.setZ(rotated[1]);
		return loc;
	}
	
	private static Location fixLocation(Location loc) {
		return null; //TODO
	}
	
	public static Location addVecLoc(Vector a, Location b) {
		World w = b.getWorld();
		double x = a.getX() + b.getX();
		double y = a.getY() + b.getY();
		double z = a.getZ() + b.getZ();
		return new Location(w, x, y, z);
	}
	
	public void moveTo(Location target, FormationParams formation) {
		Vector[] locations = formation.form(this);
		for(int i = 0; i < soldiers.size(); i++) {
			soldiers.get(i).getNavigator().setTarget(fixLocation(addVecLoc(rotate(locations[i], formation.angle), target)));
		}
	}
	
	public void teleportTo(Location target, FormationParams formation) {
		Vector[] locations = formation.form(this);
		for(int i = 0; i < soldiers.size(); i++) {
			soldiers.get(i).teleport(fixLocation(addVecLoc(rotate(locations[i], formation.angle), target)), TeleportCause.PLUGIN);
		}
	}
}