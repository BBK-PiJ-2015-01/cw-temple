package student;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import game.Edge;
import game.EscapeState;
import game.Node;
import student.EscapePath;

/**
 * An implementation of the A* algorithm to find the shortest possible escape
 * route. Whilst not expected to be the best solution it is intended to provide
 * a fall back escape route for other path finders.
 */
public class ShortestEscapePathFinder implements EscapePathFinder {

	private Node exit;

	private EscapePath escapePath;
	// Comparator for evaluating explore path sort order
	private Comparator<SearchNode> searchPathComparator = (sn1, sn2) -> searchPathComparator(sn1, sn2);

	@Override
	public EscapePath findEscapePath(EscapeState state) {

		exit = state.getExit();
		
		// Start at the current node
		final List<SearchNode> searchNodes = new LinkedList<>();
		Node n = state.getCurrentNode();
		SearchNode startNode = new SearchNode(n, null, 0, (int) euclideanDistance(n, exit));
		searchNodes.add(startNode);
		buildEscapePath(startNode, searchNodes);
		return escapePath;
	}

	void buildEscapePath(SearchNode sn, Collection<SearchNode> pathNodes) {

		Node n = sn.getNode();

		// At exit node so generate the escape plane from the search path
		if (n == exit) {
			createExitPathFromPathNodes(pathNodes);
			return;
		}
		// Close the node
		sn.close();

		// Set the neighbours as open nodes, or update them if this is a shorter
		// path
		for (Edge e : n.getExits()) {

			Node neighbour = e.getDest();
			int distToExit = (int) euclideanDistance(neighbour, exit);
			Optional<SearchNode> opt = getPathNodeByNode(neighbour, pathNodes);

			int newGCost = sn.getGCost() + e.length();
			if (!opt.isPresent()) {
				pathNodes.add(new SearchNode(neighbour, n, newGCost, distToExit));
			} else {
				SearchNode found = opt.get();
				if (found.isOpen() && newGCost < found.getGCost()) {
					found.setParentNode(n);
					found.setGCost(newGCost);
				}
			}
		}
		// Get the best PathNode
		SearchNode nearestPathNode = pathNodes.stream()
				.filter(f -> f.isOpen())
				.sorted(searchPathComparator)
				.findFirst()
				.get();
		buildEscapePath(nearestPathNode, pathNodes);
	}

	private void createExitPathFromPathNodes(Collection<SearchNode> pathNodes) {

		// Start at the exit node and follow the path backwards
		SearchNode pathNodeExit = getPathNodeByNode(exit, pathNodes).get();
		escapePath = new EscapePath(exit);

		Optional<SearchNode> optPathNode = getPathNodeByNode(pathNodeExit.getParentNode(), pathNodes);
		SearchNode nextPathNode = optPathNode.isPresent() ? optPathNode.get() : null;

		while (nextPathNode != null) {

			Node nextNode = nextPathNode.getNode();
			escapePath.addNode(nextNode);
			escapePath.addGold(nextNode.getTile().getGold());
			if (nextPathNode.getParentNode() == null) {
				nextPathNode = null;
				continue;
			}
			optPathNode = getPathNodeByNode(nextPathNode.getParentNode(), pathNodes);
			nextPathNode = optPathNode.isPresent() ? optPathNode.get() : null;
		}
		// This path is from exit -> start and must be reversed to be from
		// start-> exit
		Collections.reverse(escapePath.getPath());
	}

	Optional<SearchNode> getPathNodeByNode(Node n, Collection<SearchNode> pathNodes) {

		return pathNodes.stream().filter(pn -> n.getId() == pn.getNode().getId()).findFirst();
	}

	private int searchPathComparator(SearchNode sn1, SearchNode sn2) {
		// Explore path nodes should be ordered on descending F-cost
		return Integer.compare(sn1.getFCost(), sn2.getFCost());
	}

	double euclideanDistance(Node n1, Node n2) {

		int rowDist = n1.getTile().getRow() - n2.getTile().getRow();
		int colDist = n1.getTile().getColumn() - n2.getTile().getColumn();
		return Math.sqrt((rowDist * rowDist) + (colDist * colDist));
	}
}
