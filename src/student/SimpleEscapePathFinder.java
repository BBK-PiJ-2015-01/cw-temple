package student;

import game.EscapeState;
import game.Node;
/**
 * A simple brute force implementation to use all the alloted time in testing
 * different escape paths. The shortest escape path will be returned as a default
 */
public class SimpleEscapePathFinder extends AbstractEscapePathFinder {
	
	private EscapeState escapeState;
	private Node exit;
	private int exitRow;
	private int exitCol;
	
	@Override
	public EscapePath findEscapePath(EscapeState state) {

		escapeState = state;
		exit = state.getExit();
		exitRow = state.getExit().getTile().getRow();
		exitCol = state.getExit().getTile().getColumn();
		
		// Set the shortest escape route as a default
		escapePath = new ShortestEscapePathFinder().findEscapePath(escapeState);
		
		return escapePath;
	}

}
