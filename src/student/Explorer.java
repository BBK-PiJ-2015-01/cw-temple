package student;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import game.Edge;
import game.EscapeState;
import game.ExplorationState;
import game.Node;

public class Explorer {

	private ExplorationState explorationState;
	private List<SearchNode> explorePath;

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
		if (explorationState.getDistanceToTarget() == 0) {
			return;
		}
		explorationState = state;
		explorePath = new ArrayList<>();

	}

	private void searchPath(SearchNode searchNode) {

		long currentLocationId = explorationState.getCurrentLocation();
		long searchNodeId = searchNode.getNode().getId();

		if (currentLocationId != searchNodeId) {
			explorationState.moveTo(searchNodeId);
		}

		// Found it
		if (explorationState.getDistanceToTarget() == 0) {
			return;
		}

		// Update any open nodes if this is a shorter route
		explorationState.getNeighbours().stream()
			.filter(ns -> openNodeExists(ns.getId()))
			.filter(ns -> getSearchNodeById(ns.getId()).getgCost() > searchNode.getgCost() + 1)
			.forEach(ns -> updateOpenNode(getSearchNodeById(ns.getId()), searchNode));
		
		// Add the neighbours to the open list
		explorationState.getNeighbours().stream()
			.filter(ns -> nodeExists(ns.getId()))
			.forEach(n -> open.add(new OpenNodeImpl(n.getId(), startNode.getGCost() + 1, n.getDistanceToTarget(),
						startNode.getId())));

		// At exit node so store as the base escape path
		if (n == exit) {
			createExitPathFromPathNodes(pathNodes);
			return;
		}
		// This node is now closed
		on.close();
		// Set the neighbours as open nodes, or update them if this is a shorter
		// path
		for (Edge e : n.getExits()) {

			Node neighbour = e.getDest();
			int distToExit = (int) euclideanDistance(neighbour, exit);
			Optional<PathNode> opt = getPathNodeByNode(neighbour, pathNodes);

			int newGCost = on.getGCost() + e.length();
			if (!opt.isPresent()) {
				pathNodes.add(new PathNodeImpl(neighbour, newGCost, distToExit, n));
			} else {
				PathNode found = opt.get();
				if (found.isOpen() && newGCost < found.getGCost()) {
					found.setParentNode(n);
					found.setGCost(newGCost);
				}
			}
		}
		// Get the best PathNode
		PathNode nearestPathNode = pathNodes.stream().filter(f -> f.isOpen()).sorted().findFirst().get();
		buildShortestRouteToExit(nearestPathNode, pathNodes);
	}

	private boolean nodeExists(long id) {

		return explorePath.stream().filter(sn -> sn.getNode().getId() == id ).findAny().isPresent();
	}
	
	private boolean openNodeExists(long id) {

		return explorePath.stream().filter(sn -> sn.getNode().getId() == id && sn.isOpen()).findAny().isPresent();
	}
	
	private SearchNode getSearchNodeById(long id) {
		// Assume that existence has already been proved
		return explorePath.stream().filter(sn -> sn.getNode().getId() == id).findFirst().get();
	}



	private void updateOpenNode(SearchNode targetNode, SearchNode parentNode) {

		targetNode.setgCost(parentNode.getgCost() + 1);
		targetNode.setParentNode(parentNode.getNode());
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
		// TODO: Escape from the cavern before time runs out
	}
}
