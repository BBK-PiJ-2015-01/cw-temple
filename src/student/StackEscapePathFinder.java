package student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import game.Edge;
import game.EscapeState;
import game.Node;

/**
 * Path finder which uses the shortest path as a base. New paths are created by
 * iterating from the current node.
 */
public class StackEscapePathFinder extends AbstractEscapePathFinder {

	private Node exit;

	private long timeout; // Elapsed time of exit planning

	// Wait time for empty stack reads
	private final int STACK_TIMEOUT_IN_MILLIS = 100;

	// private BlockingDeque<EscapePath> stack;
	private SortedSet<EscapePath> stack;

	private int shortestPathLength;

	private List<Node> shortestPathCompletion;
	private int shortTestPathCompletionGold;

	private EscapePath shortestEscapePath;

	public StackEscapePathFinder(EscapeState state) {

		super(state);
	}

	@Override
	public EscapePath findEscapePath(EscapeState state) {

		escapeState = state;
		exit = state.getExit();
		// Get the shortest route out as a fall back
		escapePath = new ShortestEscapePathFinder(state).findEscapePath(escapeState);
		shortestEscapePath = escapePath;

		// Set up the shortest path from the start node to the exit
		shortestPathCompletion = new ArrayList<>(escapePath.getPath());
		shortestPathCompletion.remove(shortestPathCompletion.indexOf(state.getCurrentNode()));
		shortTestPathCompletionGold = escapePath.getGold() - state.getCurrentNode().getTile().getGold();
		shortestPathLength = escapePath.getLength();

		// Allow ourselves n-seconds to formulate a plan
		timeout = System.currentTimeMillis() + this.MAX_TIME_IN_MS;

		// Formulate the plan
		populateStack();
		buildEscapePaths();

		System.out.println(String.format("%d additional paths found, %d unresolved", numberOfPathsFound, stack.size()));
		return escapePath;
	}

	private void populateStack() {

		stack = Collections.synchronizedSortedSet(new TreeSet<>(new EscapePathOrderComparator()));
		// stack = new LinkedBlockingDeque<>();

		Node n = escapeState.getCurrentNode();
		EscapePath p = new EscapePath(n);
		p.addGold(n.getTile().getGold());
		stack.add(p);
	}

	/*
	 * Although the stack is actually thread safe, taking an item from an empty
	 * collection throws an exception. I do an isEmpty() consistency check
	 * beforehand but with multiple threads the status of the collection can
	 * change between isEmpty() and first() so access is wrapped in this method
	 * to provide transactional safety
	 */
	synchronized private EscapePath removeFromStack() {

		EscapePath returnPath = stack.isEmpty() ? null : stack.first();
		if (returnPath != null) {
			stack.remove(returnPath);
		}
		return returnPath;
	}

	private void buildEscapePaths() {

		int maxThreads = Runtime.getRuntime().availableProcessors();
		List<SearchThread> threads = new ArrayList<>(maxThreads);
		for (int i = 0; i < maxThreads; i++) {
			threads.add(new SearchThread());
		}
		ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
		try {
			pool.invokeAll(threads);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		pool.shutdown();
	}

	/**
	 * Class to be used in multi-threaded approach. This will pop incomplete
	 * paths from the stack and attempt to resolve them
	 * 
	 * @author sbaird02
	 *
	 */
	class SearchThread implements Callable<Object> {

		@SuppressWarnings("unused")
		private static final long serialVersionUID = 8473109576846837452L;

		// Comparator for evaluating exit paths
		private Comparator<Edge> escapePathComparator = (e1, e2) -> escapePathComparator(e1, e2);

		int pathsCreated;
		int pathsFollowed;

		@Override
		public Object call() {

			EscapePath p = getNextPath();
			if (p == null) {
				return null;
			}
			try {
				processPath(p);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;
		}

		private void processPath(EscapePath p) {

			while (p != null) {

				if (System.currentTimeMillis() > timeout) {
					return;
				}

				// If the path is too long then abandon it
				if (p.getLength() >= escapeState.getTimeRemaining()) {
					p = getNextPath();
					continue;
				}

				// If it's probably out of range then then abandon it
				if (!isInRange(p.getNode(), p.getLength())) {
					p = getNextPath();
					continue;
				}

				// Order the exits appropriately
				List<Edge> newExits = new ArrayList<Edge>(p.getNode().getExits());
				Collections.sort(newExits, escapePathComparator);

				EscapePath continuePath = null;
				for (Edge e : newExits) {

					Node nextNode = e.getDest();
					if (continuePathConditions(p, e, nextNode)) {

						if (exit.equals(nextNode)) {
							setEscapeRoute(createNewEscapePath(p, nextNode, e));
						} else {
							// Determine this just once as it is an expensive
							// operation
							boolean nodeExistsInPath = p.getPath().contains(nextNode);

							// Test rejoining the path and reversing out
							if (reversePathConditions(p, nodeExistsInPath)) {
								EscapePath np = createNewEscapePath(p, nextNode, e);
								// Take away the gold added in creation
								np.addGold(nextNode.getTile().getGold() * -1);
								reversePathToExit(np, nextNode);
								continue;
							}

							// If not reversed out then we don't want to
							// continue with this
							if (nodeExistsInPath) {
								continue;
							}

							// Continue with the best option and stack the rest
							if (continuePath == null) {
								continuePath = createNewEscapePath(p, nextNode, e);
							} else {
								EscapePath np = createNewEscapePath(p, nextNode, e);
								stackPath(np);
							}
						}
					}
				}

				if (continuePath != null) {
					p = continuePath;
				} else {
					// No path left to follow
					p = getNextPath();
				}
			}
		}

		/*
		 * Not currently sync'd but as it's an atomic method then the SortedTree
		 * can take care of it
		 */
		private void stackPath(EscapePath p) {

			pathsCreated++;
			stack.add(p);
		}

		/*
		 * Check to see if we want to escape back down the way we came
		 */
		private boolean reversePathConditions(EscapePath p, boolean nodeExistsInPath) {

			if (!nodeExistsInPath) {
				return false;
			}
			if (p.getLength() == 0 || p.getGold() + shortTestPathCompletionGold < escapePath.getGold()) {
				return false;
			}
			return p.getLength() + shortestPathLength < escapeState.getTimeRemaining();
		}

		/*
		 * Check to see if we want to pursue this path
		 */
		private boolean continuePathConditions(EscapePath p, Edge e, Node n) {

			return p.getLength() + e.length <= escapeState.getTimeRemaining();
		}

		/*
		 * Collect the highest value unresolved path. See notes above on
		 * synchronization
		 */
		private EscapePath getNextPath() {

			EscapePath returnPath = removeFromStack();

			// Try sleeping and looping until timeout
			if (returnPath == null) {
				try {
					Thread.sleep(STACK_TIMEOUT_IN_MILLIS);
					returnPath = removeFromStack();
				} catch (InterruptedException e) {
					//
					e.printStackTrace();
				}
			}
			return returnPath;
		}

		/*
		 * Convenience method for creating a new path
		 */
		private EscapePath createNewEscapePath(EscapePath p, Node n, Edge e) {
			EscapePath np = new EscapePath(p);
			np.addGold(n.getTile().getGold());
			np.addLength(e.length);
			np.addNode(n);
			return np;
		}

		/*
		 * Reverses the path from the given node and follows the shortest route
		 * out
		 */
		private void reversePathToExit(EscapePath p, Node n) {

			// If called in error
			if (!p.getPath().contains(n)) {
				return;
			}
			EscapePath cp = new EscapePath(p);
			// Find the node in the path
			int indexOfNode = cp.getPath().indexOf(n);
			// Add the route back
			Node lastNode = n;
			for (int i = indexOfNode - 1; i >= 0; i--) {
				Node nextNode = cp.getPath().get(i);
				cp.addNode(nextNode);
				cp.addLength(lastNode.getEdge(nextNode).length());
				lastNode = nextNode;
			}
			// Add the remainder of the shortest escape path
			for (Node spn : shortestPathCompletion) {
				cp.addNode(spn);
			}
			cp.addGold(shortTestPathCompletionGold);
			cp.addLength(shortestEscapePath.getLength());
			setEscapeRoute(cp);
		}

		/*
		 * Comparator to sort the immediate neighbours in the preferential order
		 */
		private int escapePathComparator(Edge e1, Edge e2) {

			Node o1 = e1.getDest();
			Node o2 = e1.getDest();

			// Primary comparison is Gold
			int returnValue = Integer.compare(o2.getTile().getGold(), o1.getTile().getGold());

			if (returnValue == 0) { // Edge length
				returnValue = Integer.compare(e1.length(), e2.length());
			}
			int rowDist = o1.getTile().getRow() - exit.getTile().getRow();
			int colDist = o1.getTile().getColumn() - exit.getTile().getColumn();
			Double d1 = Math.sqrt((rowDist * rowDist) + (colDist * colDist));

			o2 = e2.getDest();
			rowDist = o2.getTile().getRow() - exit.getTile().getRow();
			colDist = o2.getTile().getColumn() - exit.getTile().getColumn();
			Double d2 = Math.sqrt((rowDist * rowDist) + (colDist * colDist));

			if (returnValue == 0) { // Distance from exit
				returnValue = d1.compareTo(d2);
			}
			// Finally compare on id to enforce determinism
			if (returnValue == 0) {
				returnValue = Long.compare(o2.getId(), o1.getId());
			}
			return returnValue;
		}
	}
}
