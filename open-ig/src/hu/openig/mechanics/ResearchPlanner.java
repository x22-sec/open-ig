/*
 * Copyright 2008-2012, David Karnok 
 * The file is part of the Open Imperium Galactica project.
 * 
 * The code should be distributed under the LGPL license.
 * See http://www.gnu.org/licenses/lgpl.html for details.
 */

package hu.openig.mechanics;

import hu.openig.core.Action0;
import hu.openig.mechanics.DiscoveryPlanner.ProductionOrder;
import hu.openig.model.AIControls;
import hu.openig.model.AIFleet;
import hu.openig.model.AIPlanet;
import hu.openig.model.AIPlanner;
import hu.openig.model.AIWorld;
import hu.openig.model.AutoBuild;
import hu.openig.model.Building;
import hu.openig.model.BuildingType;
import hu.openig.model.Fleet;
import hu.openig.model.Planet;
import hu.openig.model.Player;
import hu.openig.model.ResearchType;
import hu.openig.model.Resource;
import hu.openig.model.World;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple research planner.
 * @author akarnokd, 2011.12.27.
 */
public class ResearchPlanner implements AIPlanner {
	/** The world copy. */
	final AIWorld world;
	/** The original world object. */
	final World w;
	/** The player. */
	final Player p;
	/** The actions to perform. */
	public final List<Action0> applyActions;
	/** The controls to affect the world in actions. */
	private final AIControls controls;
	/** The set of resource names. */
	private static final Set<String> LAB_RESOURCE_NAMES = 
			new HashSet<String>(Arrays.asList("ai", "civil", "computer", "mechanical", "military"));
	/**
	 * Constructor. Initializes the fields.
	 * @param world the world object
	 * @param controls the controls to affect the world in actions
	 */
	public ResearchPlanner(AIWorld world, AIControls controls) {
		this.world = world;
		this.controls = controls;
		this.p = world.player;
		this.w = p.world;
		this.applyActions = new ArrayList<Action0>();
	}
	@Override
	public List<Action0> run() {
		if (!p.id.equals("Empire")) {
			return applyActions;
		}
		if (world.runningResearch != null) {
			// if not enough labs, stop research and let the other management tasks apply
			if (!world.runningResearch.hasEnoughLabs(world.global)) {
				add(new Action0() {
					@Override
					public void invoke() {
						controls.actionStopResearch(world.runningResearch);
					}
				});
			}
			return applyActions;
		}
		final Map<ResearchType, Integer> enablesCount = new HashMap<ResearchType, Integer>();
		final Map<ResearchType, Integer> rebuildCount = new HashMap<ResearchType, Integer>();
		List<ResearchType> candidatesImmediate = new ArrayList<ResearchType>();
		List<ResearchType> candidatesReconstruct = new ArrayList<ResearchType>();
		List<ResearchType> candidatesGetMorePlanets = new ArrayList<ResearchType>();
		
		// prepare lab costs
		Map<String, Integer> labCosts = new HashMap<String, Integer>();
		for (BuildingType bt : w.buildingModel.buildings.values()) {
			for (String s : LAB_RESOURCE_NAMES) {
				if (bt.resources.containsKey(s)) {
					labCosts.put(s, bt.cost);
					break;
				}
			}
		}
		for (ResearchType rt : world.remainingResearch) {
			if (rt.hasEnoughLabs(world.global)) {
				candidatesImmediate.add(rt);
				setResearchEnables(rt, enablesCount);
			} else
			if (rt.labCount() <= world.ownPlanets.size()) {
				candidatesReconstruct.add(rt);
				setResearchEnables(rt, enablesCount);
				rebuildCount.put(rt, rebuildCost(rt, labCosts));
			} else {
				candidatesGetMorePlanets.add(rt);
				setResearchEnables(rt, enablesCount);
			}
		}
		if (candidatesImmediate.size() > 0) {
			Collections.sort(candidatesImmediate, new CompareFromMap<ResearchType>(enablesCount));
			final ResearchType rt = candidatesImmediate.get(0);
			double mf = 1.0;
			if (rt.researchCost * 5 <= world.money) {
				mf = 2.0;
			}
			final double moneyFactor = mf; // TODO decision variable
			applyActions.add(new Action0() {
				@Override
				public void invoke() {
					controls.actionStartResearch(rt, moneyFactor);
				}
			});
			return applyActions;
		}
		if (candidatesReconstruct.size() > 0) {
			return planReconstruction(rebuildCount, candidatesReconstruct);
		}
		if (candidatesGetMorePlanets.size() > 0) {
			Collections.sort(candidatesGetMorePlanets, new CompareFromMap<ResearchType>(rebuildCount));
			// TODO this is more complicated
			planConquest();
			return applyActions;
		}
		return applyActions;
	}
	/**
	 * Plan for conquest.
	 */
	void planConquest() {
		// if a fleet with colony ship is in position, colonize the planet
		for (AIFleet fleet : world.ownFleets) {
			if (fleet.hasInventory("ColonyShip") && !fleet.isMoving()) {
				if (fleet.statistics.planet != null) {
					for (AIPlanet planet : world.enemyPlanets) {
						if (planet.planet == fleet.statistics.planet && planet.owner == null) {
							final Fleet f0 = fleet.fleet;
							final Planet p0 = fleet.statistics.planet;
							add(new Action0() {
								@Override
								public void invoke() {
									if (p0.owner == null) {
										controls.actionColonizePlanet(f0, p0);
										p0.autoBuild = AutoBuild.CIVIL; // FIXME to avoid further problems
									}
								}
							});
							return;
						}
					}
				}
			}
		}
		// locate knownly colonizable planets
		List<AIPlanet> ps = new ArrayList<AIPlanet>();
		outer1:
		for (AIPlanet p : world.enemyPlanets) {
			if (p.owner == null) {
				// check if no one targets this planet already
				for (AIFleet f : world.ownFleets) {
					if (f.targetPlanet == p.planet) {
						continue outer1;
					}
				}
				ps.add(p);
			}
		}
		// if none exit
		if (ps.size() == 0) {
			return;
		}
		// bring one fleet to the target planet
		for (final AIFleet fleet : world.ownFleets) {
			if (fleet.hasInventory("ColonyShip") && !fleet.isMoving()) {
				final AIPlanet p0 = Collections.min(ps, new Comparator<AIPlanet>() {
					@Override
					public int compare(AIPlanet o1, AIPlanet o2) {
						double d1 = Math.hypot(fleet.x - o1.planet.x, fleet.y - o1.planet.y);
						double d2 = Math.hypot(fleet.x - o2.planet.x, fleet.y - o2.planet.y);
						return d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
					}
				});
				add(new Action0() {
					@Override
					public void invoke() {
						controls.actionMoveFleet(fleet.fleet, p0.planet);
					}
				});
			}
		}
		AIPlanet sp = null;
		for (AIPlanet pl : world.ownPlanets) {
			if (pl.statistics.hasMilitarySpaceport) {
				sp = pl;
				break;
			}
		}
		// if no planet has military spaceport, build one somewhere
		if (sp == null) {
			sp = w.random(world.ownPlanets);
			final BuildingType bt = findBuilding("MilitarySpaceport");
			if (bt == null) {
				System.err.println("Military spaceport not buildable for player " + p.id);
			} else {
				if (bt.cost <= world.money) {
					final Planet spaceport = sp.planet; 
					add(new Action0() {
						@Override
						public void invoke() {
							controls.actionPlaceBuilding(spaceport, bt);
						}
					});
				}
			}
			return;
		}
		final Planet spaceport = sp.planet; 
		// check if we have colony ships in the inventory
		for (Map.Entry<ResearchType, Integer> e : world.inventory.entrySet()) {
			final ResearchType rt = e.getKey();
			if (rt.id.equals("ColonyShip") && e.getValue() > 0) {
				add(new Action0() {
					@Override
					public void invoke() {
						Fleet f = controls.actionCreateFleet(w.env.labels().get(p.race + ".colony_fleet_name"), spaceport);
						f.addInventory(rt, 1);
					}
				});
				return;
			}
		}
		// check if the colony ship is actually available
		for (ResearchType rt : world.availableResearch) {
			if (rt.id.equals("ColonyShip")) {
				new ProductionOrder(world, rt, applyActions, controls).invoke();
				return;
			}
		}
	}
	/**
	 * Add the given action to the output.
	 * @param action the action to add
	 */
	void add(Action0 action) {
		applyActions.add(action);
	}
	/**
	 * Display the action log.
	 * @param message the message
	 * @param values the message parameters
	 */
	void log(String message, Object... values) {
		System.out.printf("AI:%s:", p.id);
		System.out.printf(message, values);
		System.out.println();
	}
	/**
	 * Plan how the labs will be reconstructed to allow the next research.
	 * @param rebuildCount the number of new buildings needed for each research
	 * @param candidatesReconstruct the candidates for the research
	 * @return the list of actions
	 */
	List<Action0> planReconstruction(
			final Map<ResearchType, Integer> rebuildCount,
			List<ResearchType> candidatesReconstruct) {
		// find the research that requires the fewest lab rebuilds
		Collections.sort(candidatesReconstruct, new CompareFromMap<ResearchType>(rebuildCount));

		final ResearchType rt = candidatesReconstruct.get(candidatesReconstruct.size() - 1);
		
		// first, check for planets which have power shortages and have lab
		for (AIPlanet planet : world.ownPlanets) {
			if (planet.statistics.labCount() != planet.statistics.activeLabCount()
					&& planet.statistics.workerDemand < planet.population
					&& planet.statistics.energyAvailable < 2 * planet.statistics.energyDemand
					&& !planet.statistics.constructing) {
				checkPlanetHealth(planet);
				return applyActions;
			}
		}
		// find an empty planet
		for (AIPlanet planet : world.ownPlanets) {
			if (planet.statistics.activeLabCount() == 0 
					&& !planet.statistics.constructing) {
				buildOneLabFor(rt, planet);
				return applyActions;
			}
		}
		// find a planet with excess labs.
		for (AIPlanet planet : world.ownPlanets) {
			if (demolishOneLabFor(rt, planet)) {
				return applyActions;
			}
		}
		return applyActions;
	}
	/**
	 * Find a type the building kind.
	 * @param kind the kind
	 * @return the building type
	 */
	BuildingType findBuildingKind(String kind) {
		for (BuildingType bt : w.buildingModel.buildings.values()) {
			if (bt.kind.equals(kind)) {
				return bt;
			}
		}
		return null;
	}
	/**
	 * Find a type the building kind.
	 * @param id the building type id
	 * @return the building type
	 */
	BuildingType findBuilding(String id) {
		for (BuildingType bt : w.buildingModel.buildings.values()) {
			if (bt.id.equals(id)) {
				return bt;
			}
		}
		return null;
	}
	/**
	 * Build, upgrade, repair any power plant on the planet.
	 * @param planet the target planet
	 */
	void checkPlanetHealth(final AIPlanet planet) {
		final Planet planet0 = planet.planet;
		// attempt to build a colony hub
		if (!planet.statistics.canBuildAnything()) {
			applyActions.add(new Action0() {
				@Override
				public void invoke() {
					controls.actionPlaceBuilding(planet0, findBuildingKind("MainBuilding"));
				}
			});
			return;
		}
		applyActions.add(new Action0() {
			@Override
			public void invoke() {
				// scan for buildings
				for (Building b : planet0.surface.buildings) {
					Resource r = b.type.resources.get("energy");
					if (r != null && r.amount > 0) {
						// if damaged, repair
						if (b.isDamaged()) {
							if (!b.repairing) {
								controls.actionRepairBuilding(planet0, b, true);
							}
							return;
						} else
						// if upgradable and can afford upgrade
						if (b.upgradeLevel < b.type.upgrades.size()) {
							int newLevel = Math.min(b.upgradeLevel + (int)(p.money / b.type.cost), b.type.upgrades.size());
							if (newLevel != b.upgradeLevel) {
								controls.actionUpgradeBuilding(planet0, b, newLevel);
								return;
							}
						}
					}
				}
				// if no existing building found
				// find the most expensive still affordable building
				BuildingType target = null;
				for (BuildingType bt : w.buildingModel.buildings.values()) {
					Resource r = bt.resources.get("energy");
					if (r != null && r.amount > 0 && planet0.canBuild(bt)) {
						if (target == null || (bt.cost <= p.money && bt.cost > target.cost)) {
							target = bt;
						}
					}					
				}
				if (target != null) {
					controls.actionPlaceBuilding(planet0, target);
				}
			}
		});
	}
	/**
	 * Demolish one of the excess labs on the planet to make room.
	 * @param rt the research type
	 * @param planet the target planet
	 * @return true if demolish added
	 */
	boolean demolishOneLabFor(ResearchType rt, AIPlanet planet) {
		if (demolishOneLabIf(rt.aiLab, world.global.aiLab, planet.statistics.aiLab, planet.planet, "ai")) {
			return true;
		}
		if (demolishOneLabIf(rt.civilLab, world.global.civilLab, planet.statistics.civilLab, planet.planet, "civil")) {
			return true;
		}
		if (demolishOneLabIf(rt.compLab, world.global.compLab, planet.statistics.compLab, planet.planet, "computer")) {
			return true;
		}
		if (demolishOneLabIf(rt.mechLab, world.global.mechLab, planet.statistics.mechLab, planet.planet, "mechanical")) {
			return true;
		}
		if (demolishOneLabIf(rt.milLab, world.global.milLab, planet.statistics.milLab, planet.planet, "military")) {
			return true;
		}
		return false;
	}
	/**
	 * Demolish one lab of the given resource.
	 * @param lab the required lab count
	 * @param global the global lab count
	 * @param local the local lab count
	 * @param planet the planet
	 * @param resource the lab resource name
	 * @return true if action added
	 */
	boolean demolishOneLabIf(int lab, int global, int local, final Planet planet, final String resource) {
		if (lab < global && local > 0) {
			applyActions.add(new Action0() {
				@Override
				public void invoke() {
					for (Building b : planet.surface.buildings) {
						if (b.type.resources.containsKey(resource)) {
							controls.actionDemolishBuilding(planet, b);
							return;
						}
					}
				}
			});
			return true;
		}
		return false;
	}
	/**
	 * Build one of the required labs.
	 * @param rt the research type
	 * @param planet the target planet
	 */
	void buildOneLabFor(final ResearchType rt, final AIPlanet planet) {
		if (buildOneLabIf(rt.aiLab, world.global.aiLab, planet.statistics.aiLab, planet, "ai")) {
			return;
		}
		if (buildOneLabIf(rt.civilLab, world.global.civilLab, planet.statistics.civilLab, planet, "civil")) {
			return;
		}
		if (buildOneLabIf(rt.compLab, world.global.compLab, planet.statistics.compLab, planet, "computer")) {
			return;
		}
		if (buildOneLabIf(rt.mechLab, world.global.mechLab, planet.statistics.mechLab, planet, "mechanical")) {
			return;
		}
		if (buildOneLabIf(rt.milLab, world.global.milLab, planet.statistics.milLab, planet, "military")) {
			return;
		}
	}
	/**
	 * Build one of the labs if the prerequisite counts match.
	 * @param required the required count of lab
	 * @param available the available count of lab
	 * @param local the locally built count
	 * @param planet the target planet
	 * @param resource the building type identification resource
	 * @return true if successful
	 */
	boolean buildOneLabIf(int required, int available, int local, final AIPlanet planet, final String resource) {
		if (required > available) {
			final Planet planet0 = planet.planet;
			if (!planet.statistics.canBuildAnything()) {
				return false;
			}
			// if there is one locally available
			if (local > 0) {
				applyActions.add(new Action0() {
					@Override
					public void invoke() {
						for (Building b : planet0.surface.buildings) {
							if (b.hasResource(resource) && b.isDamaged()) {
								if (!b.repairing) {
									controls.actionRepairBuilding(planet0, b, true);
									return;
								}
							}
						}
					}
				});
				return true;
			}
			final BuildingType bt = findBuildingType(resource);
			if (bt == null) {
				new AssertionError("Can't find building for resource " + resource).printStackTrace();
				return false;
			}
			if (bt.cost <= world.money) {
				Point pt = planet.placement.findLocation(planet.planet.getPlacementDimensions(bt));
				if (pt != null) {
					applyActions.add(new Action0() {
						@Override
						public void invoke() {
							controls.actionPlaceBuilding(planet0, bt);
						}
					});
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * Find the first building who provides the given resource.
	 * @param resource the resource name
	 * @return the building type or null
	 */
	BuildingType findBuildingType(String resource) {
		for (BuildingType bt : w.buildingModel.buildings.values()) {
			if (bt.resources.containsKey(resource)) {
				return bt;
			}
		}
		return null;
	}
	/**
	 * Comparator which takes an integer index from the supplied map. 
	 * @author akarnokd, 2011.12.26.
	 * @param <T> the element type
	 */
	class CompareFromMap<T> implements Comparator<T> {
		/** The backing map. */
		final Map<T, Integer> map;
		/**
		 * Constructor.
		 * @param map the backing map to use
		 */
		public CompareFromMap(Map<T, Integer> map) {
			this.map = map;
		}
		@Override
		public int compare(T o1, T o2) {
			int count1 = map.get(o1);
			int count2 = map.get(o2);
			return count1 < count2 ? 1 : (count1 > count2 ? -1 : 0);
		}
	}
	/**
	 * Count how many labs need to be built in addition to the current settings.
	 * @param rt the research type
	 * @param labCosts the cost of various labs
	 * @return the total number of new buildings required
	 */
	int rebuildCost(ResearchType rt, Map<String, Integer> labCosts) {
		return 
				rebuildRequiredCount(rt.aiLab, world.global.aiLab) * labCosts.get("ai")
				+ rebuildRequiredCount(rt.civilLab, world.global.civilLab) * labCosts.get("civil")
				+ rebuildRequiredCount(rt.compLab, world.global.compLab) * labCosts.get("computer")
				+ rebuildRequiredCount(rt.mechLab, world.global.mechLab) * labCosts.get("mechanical")
				+ rebuildRequiredCount(rt.milLab, world.global.milLab) * labCosts.get("military")
		;
	}
	/**
	 * If the lab count is greater than the active count, return the difference.
	 * @param lab the research required lab count
	 * @param active the active research counts
	 * @return zero or the difference
	 */
	int rebuildRequiredCount(int lab, int active) {
		if (lab > active) {
			return lab - active;
		}
		return 0;
	}
	/**
	 * Counts how many further research becomes available when the research is completed.
	 * @param rt the current research
	 * @param map the map for research to count
	 */
	void setResearchEnables(ResearchType rt, Map<ResearchType, Integer> map) {
		int count = 0;
		for (ResearchType rt2 : world.remainingResearch) {
			if (rt2.prerequisites.contains(rt)) {
				count++;
			}
		}
		for (ResearchType rt2 : world.furtherResearch) {
			if (rt2.prerequisites.contains(rt)) {
				count++;
			}
		}
		map.put(rt, count);
	}

}
