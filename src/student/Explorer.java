package student;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import game.EscapeState;
import game.ExplorationState;
import game.GameState;
import game.Node;

public class Explorer {

	private boolean explorationComplete;
	private ExplorationState explorationState;
	private List<ExploreNode> explorePath;
	// Comparator for evaluating explore path sort order
	private Comparator<ExploreNode> explorePathComparator = (en1, en2) -> explorePathComparator(en1, en2);

	
	private EscapeState escapeState;
	

	/**
	 * Explore the cavern, trying to find the orb in as few steps as possible.
	 * Once you find the orb, you must return from the function in order to pick
	 * it up. If you continue to move after finding the orb rather than
	 * returning, it will not count. If you return from this function while not
	 * standing on top of the orb, it will count as a failure.
	 * <p>
	 * There is no limit to how many steps you can take, but you will receive a
	 * score bonus multiplier for finding the orb in fewer steps.
	 * <p>
	 * At every step, you only know your current tile's ID and the ID of all
	 * open neighbor tiles, as well as the distance to the orb at each of these
	 * tiles (ignoring walls and obstacles).
	 * <p>
	 * In order to get information about the current state, use functions
	 * getCurrentLocation(), getNeighbours(), and getDistanceToTarget() in
	 * ExplorationState. You know you are standing on the orb when
	 * getDistanceToTarget() is 0.
	 * <p>
	 * Use function moveTo(long id) in ExplorationState to move to a neighboring
	 * tile by its ID. Doing this will change state to reflect your new
	 * position.
	 * <p>
	 * A suggested first implementation that will always find the orb, but
	 * likely won't receive a large bonus multiplier, is a depth-first search.
	 *
	 * @param state
	 *            the information available at the current state
	 */
	public void explore(ExplorationState state) {

		// In the unlikely event of the Orb being in the entrance
		if (state.getDistanceToTarget() == 0) {
			return;
		}
		explorationState = state;
		explorePath = new ArrayList<>();
		ExploreNode exploreNode = new ExploreNode(explorationState.getCurrentLocation(), 0, 0 , explorationState.getDistanceToTarget());
		explorePath(exploreNode); 
		return;
	}

	private void explorePath(ExploreNode exploreNode) {

		if (explorationComplete) {
			return;
		}

		long currentLocationId = explorationState.getCurrentLocation();
		long searchNodeId = exploreNode.getId();
		
		if (currentLocationId != searchNodeId) {
			explorationState.moveTo(searchNodeId);
		}
				
		// Found it
		int distanceToTarget = explorationState.getDistanceToTarget();
		if (distanceToTarget == 0) {
			explorationComplete = true;	// Cancel any further exploration
			return;
		}
/*
		// TODO: Implement check to see if retracing steps will get us closer
		if (closestApproach < distanceToTarget) {
			
			Optional<ExploreNode>  bestOptional = explorePath.stream()
				.filter(en -> en.gethCost() == closestApproach)
				.sorted( (en1,  en2) -> Integer.compare(Math.abs(en1.getgCost() - exploreNode.getgCost())
						, Math.abs(en2.getgCost() - exploreNode.getgCost()))).findFirst();
			ExploreNode bestNode = bestOptional.isPresent() ? bestOptional.get() : null;
			if (bestNode != null) {
				System.out.println(String.format("Current %d, closest %d is %d away"
						, distanceToTarget, closestApproach, Math.abs(bestNode.getgCost() - exploreNode.getgCost())));
				System.out.println("Benefit: " + (distanceToTarget - closestApproach -  Math.abs(bestNode.getgCost() - exploreNode.getgCost())));
			}			
		} else {
			
			closestApproach = distanceToTarget;
		}
*/		
		
		exploreNode.close();

		// Update any open nodes
		explorationState.getNeighbours().stream().filter(ns -> openNodeExists(ns.getId()))
//				.filter(ns -> getExploreNodeById(ns.getId()).getgCost() > exploreNode.getgCost() + 1)
				.forEach(ns -> updateExploreNode(getExploreNodeById(ns.getId()), exploreNode));

		// Add the neighbours to the open list
		explorationState.getNeighbours().stream()
				.filter(ns -> !nodeExists(ns.getId()))
				.forEach(ns -> explorePath.add(new ExploreNode(ns.getId(), exploreNode.getId(),
						exploreNode.getgCost() + 1, ns.getDistanceToTarget())));

		boolean goBack = !explorePath.stream()
				.filter(en -> en.getParentId() == exploreNode.getId()) 
				.filter(on -> on.isOpen()).findAny().isPresent();
		
		// If nowhere to go then retrace path back to parent..
		if (goBack) {
			explorePath(getExploreNodeById(exploreNode.getParentId()));
		} else {
			// ...otherwise find the best node to move to
			explorePath.stream()
				.filter(en -> en.getParentId() == exploreNode.getId())
				.filter(en -> en.isOpen())
				.sorted(explorePathComparator)
				.forEach(en -> explorePath(en));
		}
	}

	private boolean nodeExists(long id) {

		return explorePath.stream().filter(sn -> sn.getId() == id).findAny().isPresent();
	}

	private boolean openNodeExists(long id) {

		return explorePath.stream().filter(sn -> sn.getId() == id && sn.isOpen()).findAny().isPresent();
	}

	private ExploreNode getExploreNodeById(long id) {
		// Assume that existence has already been proved
		return explorePath.stream().filter(sn -> sn.getId() == id).findFirst().get();
	}

	private void updateExploreNode(ExploreNode targetNode, ExploreNode parentNode) {

		targetNode.setgCost(parentNode.getgCost() + 1);
		targetNode.setParentId(parentNode.getId());
	}

	private int explorePathComparator(ExploreNode en1, ExploreNode en2) {
		// Explore path nodes should be ordered on descending H-cost
		return Integer.compare(en1.gethCost(), en2.gethCost());
	}

	/**
	 * Escape from the cavern before the ceiling collapses, trying to collect as
	 * much gold as possible along the way. Your solution must ALWAYS escape
	 * before time runs out, and this should be prioritized above collecting
	 * gold.
	 * <p>
	 * You now have access to the entire underlying graph, which can be accessed
	 * through EscapeState. getCurrentNode() and getExit() will return you Node
	 * objects of interest, and getVertices() will return a collection of all
	 * nodes on the graph.
	 * <p>
	 * Note that time is measured entirely in the number of steps taken, and for
	 * each step the time remaining is decremented by the weight of the edge
	 * taken. You can use getTimeRemaining() to get the time still remaining,
	 * pickUpGold() to pick up any gold on your current tile (this will fail if
	 * no such gold exists), and moveTo() to move to a destination node adjacent
	 * to your current node.
	 * <p>
	 * You must return from this function while standing at the exit. Failing to
	 * do so before time runs out or returning from the wrong location will be
	 * considered a failed run.
	 * <p>
	 * You will always have enough time to escape using the shortest path from
	 * the starting position to the exit, although this will not collect much
	 * gold.
	 *
	 * @param state
	 *            the information available at the current state
	 */
	public void escape(EscapeState state) {
		
		escapeState = state;
		
		// Allow for different plans to be generated
		EscapePathFinder pathFinder = new StackEscapePathFinder(state);
//		EscapePathFinder pathFinder = new SimpleEscapePathFinder(state);

		EscapePath escapePlan = pathFinder.findEscapePath(state);
		implementEscapePlan(escapePlan);
	}
	
	private void implementEscapePlan(EscapePath escapePlan) {
		
		//for(Node n : escapePlan.getPath()) {
		//	System.out.println(String.format("Move to r%d:c%d", n.getTile().getRow(), n.getTile().getColumn()));
		//}

		escapePlan.getPath().stream().forEach(e -> followPath(e));
	}

	private void followPath(Node n) {

		// Pick up any gold before moving on
		if (escapeState.getCurrentNode().getTile().getGold() > 0) {
			escapeState.pickUpGold();
		}
		if (!n.equals(escapeState.getCurrentNode())) {
			escapeState.moveTo(n);
		}
	}
}
