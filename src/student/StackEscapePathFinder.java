package student;

import game.EscapeState;
/**
 * Path finder which uses the shortest path as a base. New paths are created by descending 
 * the path from the exit looking for alternative routes which start close to the exit and 
 * get progressively longer.
 */
public class StackEscapePathFinder extends AbstractEscapePathFinder {

	public StackEscapePathFinder(EscapeState state) {
		super(state);
		// TODO Auto-generated constructor stub
	}

	@Override
	public EscapePath findEscapePath(EscapeState state) {
		// TODO Auto-generated method stub
		return null;
	}

}
