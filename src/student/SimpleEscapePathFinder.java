package student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import game.Edge;
import game.EscapeState;
import game.Node;
import student.EscapePath;
/**
 * A simple brute force implementation to use all the alloted time in testing
 * different escape paths. The shortest escape path will be returned as a default
 */
public class SimpleEscapePathFinder extends AbstractEscapePathFinder {
	
	private Node exit;
	private int exitRow;
	private int exitCol;
	
	// Control variables
	private double avgLength = 10;				// Guestimate of average edge length
	private long timeout;						// Elapsed time of exit planning
	
	// Comparator for evaluating exit paths
	private Comparator<Edge> escapePathComparator = (e1, e2) -> escapePathComparator(e1, e2);
	
	public SimpleEscapePathFinder(EscapeState state) {
		super(state);
	}

	@Override
	public EscapePath findEscapePath(EscapeState state) {

		escapeState = state;
		exit = state.getExit();
		exitRow = state.getExit().getTile().getRow();
		exitCol = state.getExit().getTile().getColumn();
		
		// Set the shortest escape route as a default
		escapePath = new ShortestEscapePathFinder(state).findEscapePath(escapeState);
		
		// Start at the current node and process  until timeout expires or all options explored
		timeout = System.currentTimeMillis() + this.MAX_TIME_IN_MS;
		buildEscapePaths(new EscapePath(state.getCurrentNode()));
		return escapePath;
	}

	private void buildEscapePaths(EscapePath p) {
		
		if (System.currentTimeMillis() > timeout) {
			return;
		}

		List<Edge> newExits = new ArrayList<Edge>(p.getNode().getExits());
		Collections.sort(newExits, escapePathComparator);
		for (Edge e : newExits) {

			Node nextNode = e.getDest();
			if (isInRange(nextNode, p.getLength()) 
					&& p.getLength() + e.length <= escapeState.getTimeRemaining()
					&& !p.getPath().contains(nextNode)
					) 
			{

				EscapePath np = new EscapePath(p);
				np.addGold(nextNode.getTile().getGold());
				np.addLength(e.length);
				np.addNode(nextNode);

				if (exit.equals(nextNode) && np.getLength() <= escapeState.getTimeRemaining()) {
					setEscapeRoute(np);
					continue;
				} else {
					buildEscapePaths(np);
				}
			}
		}
	}

	private int escapePathComparator(Edge e1, Edge e2) {

		Node o1 = e1.getDest();
		Node o2 = e1.getDest();
		
		// Primary comparison is Euclidean distance from exit
		int rowDist = o1.getTile().getRow() - exit.getTile().getRow();
		int colDist = o1.getTile().getColumn() - exit.getTile().getColumn();
		Double d1 = Math.sqrt((rowDist * rowDist) + (colDist * colDist));

		o2 = e2.getDest();
		rowDist = o2.getTile().getRow() - exit.getTile().getRow();
		colDist = o2.getTile().getColumn() - exit.getTile().getColumn();
		Double d2 = Math.sqrt((rowDist * rowDist) + (colDist * colDist));
		
		int returnValue = d1.compareTo(d2); 	
		// If nodes are equidistant then apply other criteria
		
		if (returnValue == 0) {		// Amount of gold	
			returnValue = Integer.compare(o2.getTile().getGold(), o1.getTile().getGold());
		}
		if (returnValue == 0) {		// Edge length
			returnValue = Integer.compare(e1.length(), e2.length());
		}		
		if (returnValue == 0) {		// Finally compare on id to enforce determinism
			returnValue = Long.compare(o2.getId(), o1.getId());
		}
		return returnValue;
	}
	
	private boolean isInRange(Node n, int pLength) {

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
