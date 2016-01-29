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
	 * A reference to the {@code EscapeState}
	 */
	EscapeState escapeState;

	/**
	 * The best {code EscapePAth} found
	 */
	EscapePath escapePath;
	/**
	 * Maximum time allowed to find a valid path.
	 */
	int MAX_TIME_IN_MS = 10000;
	/**
	 * Estimate of average edge length
	 */
	double avgLength = 8;
	/**
	 * The number of escape routes found. Useful for testing
	 */
	int numberOfPathsFound;

	private int exitRow;
	private int exitCol;

	/**
	 * Constructor using the {@code EscapeState}
	 * 
	 * @param state
	 *            the {@code EscapeState} containing the details of the
	 *            {@code Cavern}
	 */
	public AbstractEscapePathFinder(EscapeState state) {

		escapeState = state;
		exitRow = state.getExit().getTile().getRow();
		exitCol = state.getExit().getTile().getColumn();
	}

	/**
	 * Find a given {@code Node} in a supplied collection of {@code SearchNode}
	 * 
	 * @param n
	 *            the {@code Node} to find
	 * @param pathNodes
	 *            any collection of {@code SearchNode}
	 * @return an {@code Optional} representing the first instance of the
	 *         {@code Node}
	 */
	protected Optional<SearchNode> getPathNodeByNode(Node n, Collection<SearchNode> pathNodes) {

		return pathNodes.stream().filter(pn -> n.getId() == pn.getNode().getId()).findFirst();
	}

	/**
	 * Distance between two Nodes using Euclidean points
	 * 
	 * @param n1
	 *            from {@code Node}
	 * @param n2
	 *            to {@code Node}
	 * @return the Euclidean Plane distance between the nodes represented by the
	 *         row and column values
	 */
	protected double euclideanDistance(Node n1, Node n2) {

		int rowDist = n1.getTile().getRow() - n2.getTile().getRow();
		int colDist = n1.getTile().getColumn() - n2.getTile().getColumn();
		return Math.sqrt((rowDist * rowDist) + (colDist * colDist));
	}

	/**
	 * Sets the {@code EscapePath} if it is short enough and has more gold than
	 * the previously supplied.
	 * 
	 * @param p
	 *            a comparison {@code EscapePath}
	 */
	protected synchronized void setEscapeRoute(EscapePath p) {

		numberOfPathsFound++;
		int currentGold = escapePath == null ? -1 : escapePath.getGold();
		int currentLength = escapePath == null ? Integer.MAX_VALUE : escapePath.getLength();
		// Save the path if it is valid and more valuable than the existing one
		if (p.getGold() > currentGold || (p.getGold() == currentGold && p.getLength() < currentLength)) {
			if (p.getLength() <= escapeState.getTimeRemaining()) {
				escapePath = p;
			}
		}
	}

	/**
	 * Estimates if the supplied {@code Node} is too far from the exit
	 * {@code Node} to probably reach given the average edge length
	 * 
	 * @param n
	 *            the {@code Node} for comparison
	 * @param pLength
	 *            the path length to reach the {@code Node}
	 * @return whether the supplied {@code Node} can be reached in the remaining
	 *         time
	 */
	protected boolean isInRange(Node n, int pLength) {

		int rDist = Math.abs(n.getTile().getRow() - exitRow) + (n.getTile().getColumn() - exitCol);
		rDist *= avgLength;
		return rDist + pLength < escapeState.getTimeRemaining();
	}
}