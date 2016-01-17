package student;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import game.EscapeState;
import game.Node;

/**
 * Path finder which uses the shortest path as a base. New paths are created by
 * descending the path from the exit looking for alternative routes which start
 * close to the exit and get progressively longer. The shortest escape route
 * will be returned as a default.
 */
public class StackEscapePathFinder extends AbstractEscapePathFinder {

	private Node exit;
	private Node exitCovering;

	private long timeout; // Elapsed time of exit planning
	
	private Deque<EscapePath>  stack;

	public StackEscapePathFinder(EscapeState state) {
		super(state);
		// TODO Auto-generated constructor stub
	}

	@Override
	public EscapePath findEscapePath(EscapeState state) {
		
		escapeState = state;
		exit = state.getExit();
		// The exit nodes have only one neighbour
		exitCovering = exit.getNeighbours().stream().findFirst().get();

		// Set the shortest escape route as a default
		escapePath = new ShortestEscapePathFinder(state).findEscapePath(escapeState);
		
		//
		stack = new ConcurrentLinkedDeque<>();
		
		
		System.out.println(String.format("%d additional paths found", numberOfPathsFound));
		return escapePath;
	}

}
