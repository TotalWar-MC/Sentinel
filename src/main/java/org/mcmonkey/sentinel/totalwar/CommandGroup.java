package org.mcmonkey.sentinel.totalwar;
//new class
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.util.Vector;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class CommandGroup {

	private Nation owner = null;
	
	//when player logs off it has no commander, exactly lmao oof
	private static BiMap<CommandGroup, OfflinePlayer> groups = HashBiMap.create(); //TODO save to file
	
	//when the group "forms", the units move to positions relative to the commander (and form according to their formations)
	private Map<SentinelRegiment, FormationParams> units = new HashMap<SentinelRegiment, FormationParams>();
	
	public boolean addUnit(SentinelRegiment unit, boolean ignoreChecks) {
		if(!isOrphanUnit(unit)) return false;
		if(ignoreChecks) {
			units.put(unit, FormationParams.DEFAULT);
			return true;
		}
		Town unitOwner = unit.getTown();
		Nation unitNation = null;
		try {
			unitNation = unitOwner.getNation();
		} catch (NotRegisteredException e) {return false;}
		if(unitNation != owner || !unitNation.getAllies().contains(owner)) return false; 
		units.put(unit, FormationParams.DEFAULT);
		return true;
	}
	
	public boolean hasUnit(SentinelRegiment unit) {
		return getUnits().contains(unit);
	}
	
	public boolean isOrphanUnit(SentinelRegiment unit) {
		for(CommandGroup cg : groups.keySet()) {
			if(cg.hasUnit(unit)) return false;
		}
		return true;
	}
	
	public boolean setUnitFormation(SentinelRegiment unit, FormationParams formation) {
		if(!hasUnit(unit)) return false;
		units.put(unit, formation);
		return true;
	}
	
	public boolean setCommander(OfflinePlayer commander, boolean ignoreChecks, boolean ignoreOnline) {
		if(getForCommander(commander) != null) return false;
		if(ignoreChecks && (ignoreOnline || commander.isOnline())) {
			groups.inverse().put(commander, this);
			return true;
		}
		Nation commanderNation = null;
		try {
			commanderNation = TownyUniverse.getDataSource().getResident(commander.getName()).getTown().getNation();
		} catch (NotRegisteredException e) {return false;}
		if(commanderNation != owner) return false;
		groups.inverse().put(commander, this);
		return true;
	}
	
	public Set<SentinelRegiment> getUnits() {
		return new HashSet<SentinelRegiment>(units.keySet());
	}
	
	public static CommandGroup getForUnit(SentinelRegiment unit) {
		for(CommandGroup cg : groups.keySet()) {
			if(cg.hasUnit(unit)) return cg;
		}
		return null;
	}
	
	public OfflinePlayer getCommander() {
		return groups.get(this);
	}
	
	public static CommandGroup getForCommander(OfflinePlayer commander) {
		return groups.inverse().get(commander);
	}
	
	public void form(Location loc, double angle) {
		for(SentinelRegiment unit : getUnits()) {
			FormationParams fp = units.get(unit);
			unit.moveTo(loc.add(fp.x, 0, fp.z), fp.addAngle(angle));
		}
	}
	
	public static class FormationParams {
		public static final FormationParams DEFAULT = new FormationParams(0, 0, 0, 3, 0, 1, LineFormation.INSTANCE);
		public double x,z,angle,spacing;
		public int depth,frontage;
		public Formation formation;
		public FormationParams(double x, double z, double angle, int depth, int frontage, double spacing, Formation formation) {
			this.x = x;
			this.z = z;
			this.angle = angle;
			this.depth = depth;
			this.frontage = frontage;
			this.spacing = spacing;
		}
		public Vector[] form(SentinelRegiment regiment) {
			return formation.form(depth, frontage, spacing, regiment);
		}
		public FormationParams addAngle(double angle) {
			return new FormationParams(x, z, this.angle + angle, depth, frontage, spacing, formation);
		}
	}
}
