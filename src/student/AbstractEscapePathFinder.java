package student;

import java.util.Collection;
import java.util.Optional;

import game.Node;
/**
 * Abstract superclass to provide common methods as an alternative to a utility
 * class.
 */
public abstract class AbstractEscapePathFinder implements EscapePathFinder {

	EscapePath escapePath;
	
	public AbstractEscapePathFinder() {
		super();
	}

	protected Optional<SearchNode> getPathNodeByNode(Node n, Collection<SearchNode> pathNodes) {
	
		return pathNodes.stream().filter(pn -> n.getId() == pn.getNode().getId()).findFirst();
	}

	protected double euclideanDistance(Node n1, Node n2) {
	
		int rowDist = n1.getTile().getRow() - n2.getTile().getRow();
		int colDist = n1.getTile().getColumn() - n2.getTile().getColumn();
		return Math.sqrt((rowDist * rowDist) + (colDist * colDist));
	}

}