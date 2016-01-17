package student;

import java.util.Collection;
import java.util.Optional;

import game.EscapeState;
import game.Node;

/**
 * Abstract superclass to provide common methods as an alternative to a utility
 * class.
 */
public abstract class AbstractEscapePathFinder implements EscapePathFinder {

	/**
	 * Maximum time allowed to find a valid path.
	 */
	EscapeState escapeState;

	EscapePath escapePath;
	/**
	 * Maximum time allowed to find a valid path.
	 */
	int MAX_TIME_IN_MS = 10000;
	/**
	 * Estimate of average edge length
	 */
	double avgLength = 10;
	/**
	 * The number of escape routes found. Useful for testing
	 */
	int numberOfPathsFound;
	
	private int exitRow;
	private int exitCol;

	public AbstractEscapePathFinder(EscapeState state) {
		escapeState = state;
		exitRow = state.getExit().getTile().getRow();
		exitCol = state.getExit().getTile().getColumn();
	}

	protected Optional<SearchNode> getPathNodeByNode(Node n, Collection<SearchNode> pathNodes) {

		return pathNodes.stream().filter(pn -> n.getId() == pn.getNode().getId()).findFirst();
	}

	protected double euclideanDistance(Node n1, Node n2) {

		int rowDist = n1.getTile().getRow() - n2.getTile().getRow();
		int colDist = n1.getTile().getColumn() - n2.getTile().getColumn();
		return Math.sqrt((rowDist * rowDist) + (colDist * colDist));
	}

	void setEscapeRoute(EscapePath p) {

		numberOfPathsFound++;
		// Save the path if it is valid and more valuable than the existing one
		if (p.getGold() > (escapePath == null ? -1 : escapePath.getGold())) {
			if (p.getLength() <= escapeState.getTimeRemaining()) {
				escapePath = p;
			}
		}
	}
	boolean isInRange(Node n, int pLength) {

		// Estimate if the node is too far from the exit to possibly reach
		int nRow = n.getTile().getRow();
		int nCol = n.getTile().getColumn();
		int rowDist = nRow - exitRow;
		int colDist = nCol - exitCol;
		Double d = Math.sqrt((rowDist * rowDist) + (colDist * colDist));
		d *= this.avgLength;
		return d + pLength < escapeState.getTimeRemaining();
	}
}