/*
 * Copyright 2008-2012, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.scripting.missions;

import hu.openig.core.Pair;
import hu.openig.mechanics.DefaultAIControls;
import hu.openig.model.Fleet;
import hu.openig.model.FleetTask;
import hu.openig.model.InventoryItem;
import hu.openig.model.ObjectiveState;
import hu.openig.model.Planet;
import hu.openig.model.Player;
import hu.openig.model.ResearchType;

/**
 * Mission 18: Conquer the Garthogs.
 * <p>Also: initialize level 3.</p>
 * @author akarnokd, Feb 13, 2012
 */
public class Mission18 extends Mission {
	
	@Override
	public boolean applicable() {
		return world.level == 3;
	}
	@Override
	public void onLevelChanged() {
		if (world.level != 3) {
			return;
		}
		removeMissions(1, 25);
		
		player.setAvailable(research("Battleship1"));
		createMainShip();

		// achievement
		String a = "achievement.commander";
		if (!world.env.profile().hasAchievement(a)) {
			world.env.achievementQueue().add(a);
			world.env.profile().grantAchievement(a);
		}
	}
	/**
	 * Creates the main ship for level 2.
	 */
	void createMainShip() {
		Pair<Fleet, InventoryItem> own = findTaggedFleet("CampaignMainShip3", player);
		if (own != null) {
			return;
		}
		own = findTaggedFleet("CampaignMainShip2", player);
		if (own == null) {
			own = findTaggedFleet("CampaignMainShip1", player);
		}
		Fleet f = null;
		if (own != null) {
			f = own.first;
		} else {
			Planet ach = planet("Achilles");
			f = createFleet(label("Empire.main_fleet"), player, ach.x + 5, ach.y + 5);
		}			
		ResearchType rt = research("Battleship1");
		f.addInventory(rt, 1);
		f.addInventory(research("LightTank"), 4);
		
		InventoryItem ii = f.getInventoryItem(rt);
		ii.tag = "CampaignMainShip3";

		// loadout
		setSlot(ii, "laser", "Laser1", 14);
		setSlot(ii, "bomb", "Bomb1", 6);
		setSlot(ii, "rocket", "Rocket1", 4);
		setSlot(ii, "radar", "Radar1", 1);
		setSlot(ii, "cannon", "IonCannon", 6);
		setSlot(ii, "shield", "Shield1", 14);
		setSlot(ii, "hyperdrive", "HyperDrive1", 14);

	}
	@Override
	public void onTime() {
		checkMainShip();
		checkSuccess();
		if (checkTimeout("Mission-18-Failed")) {
			helper.gameover();
			loseGameMessageAndMovie("Douglas-Fire-Lost-Planet-2", "loose/fired_level_3");
		}
		if (checkTimeout("Mission-18-Hide")) {
			helper.objective("Mission-18").visible = false;
		}
	}
	/** Check if the main ship is still operational. */
	void checkMainShip() {
		Pair<Fleet, InventoryItem> ft = findTaggedFleet("CampaignMainShip3", player);
		if (ft == null) {
			if (!helper.hasTimeout("MainShip-Lost")) {
				helper.setTimeout("MainShip-Lost", 3000);
			}
			if (helper.isTimeout("MainShip-Lost")) {
				helper.gameover();
				loseGameMovie("loose/destroyed_level_3");
			}
		}
	}
	/** Check if we own all the necessary planets. */
	void checkSuccess() {
		Player g = player("Garthog");
		if (g.statistics.planetsOwned == 0) {
			helper.setObjectiveState("Mission-18", ObjectiveState.SUCCESS);
			addTimeout("Mission-18-Hide", 13000);
			// TODO next level
		}
	}
	@Override
	public void onConquered(Planet planet, Player previousOwner) {
		if (planet.owner == player || previousOwner == player) {
			boolean win = previousOwner.id.equals("Garthog");
			if (planet.id.equals("Garthog 1")) {
				helper.setObjectiveState("Mission-18-Task-1", win ? ObjectiveState.SUCCESS : ObjectiveState.ACTIVE);
			} else
			if (planet.id.equals("Garthog 2")) {
				helper.setObjectiveState("Mission-18-Task-2", win ? ObjectiveState.SUCCESS : ObjectiveState.ACTIVE);
			} else
			if (planet.id.equals("Garthog 3")) {
				helper.setObjectiveState("Mission-18-Task-3", win ? ObjectiveState.SUCCESS : ObjectiveState.ACTIVE);
			} else
			if (planet.id.equals("Garthog 4")) {
				helper.setObjectiveState("Mission-18-Task-4", win ? ObjectiveState.SUCCESS : ObjectiveState.ACTIVE);
			} else
			if (planet.id.equals("Garthog 5")) {
				helper.setObjectiveState("Mission-18-Task-5", win ? ObjectiveState.SUCCESS : ObjectiveState.ACTIVE);
			} else {
				helper.setObjectiveState("Mission-18", ObjectiveState.FAILURE);
				gameover();
			}
		}
	}
	@Override
	public void onLost(Planet planet) {
		if (planet.owner == player) {
			if (planet.id.startsWith("Garthog")) {
				Planet ach = planet("Achilles");
				// create a fleet to colonize the planet, for convenience
				Fleet f = createFleet(label("Empire.colonizer_fleet"), player, ach.x, player.explorationOuterLimit.y - 5);
				f.addInventory(research("ColonyShip"), 1);
				f.moveTo(planet);
				f.task = FleetTask.SCRIPT;
				tagFleet(f, "Mission-18-Colonizer");
				addScripted(f);
			} else {
				helper.setObjectiveState("Mission-18", ObjectiveState.FAILURE);
				gameover();
			}
		}		
	}
	@Override
	public void onFleetAt(Fleet fleet, Planet planet) {
		if (fleet.owner == player && planet.owner == null 
				&& hasTag(fleet, "Mission-18-Colonizer")) {
			removeScripted(fleet);
			DefaultAIControls.colonizeWithFleet(fleet, planet);
		}
	}
	/** Issue game over. */
	void gameover() {
		addTimeout("Mission-18-Failed", 13000);
	}
}
