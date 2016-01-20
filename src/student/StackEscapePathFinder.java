package student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

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

	// Comparator for evaluating exit paths
	private Comparator<Edge> escapePathComparator = (e1, e2) -> escapePathComparator(e1, e2);


	private BlockingDeque<EscapePath> stack;
	// private Deque<EscapePath> stack;

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
		// The exit nodes have only one neighbour
		// exitCovering = exit.getNeighbours().stream().findFirst().get();
		// Set the shortest escape route as a default
		escapePath = new ShortestEscapePathFinder(state).findEscapePath(escapeState);
		shortestEscapePath = escapePath;

		// Set up the path from the start node to the exit
		shortestPathCompletion = new ArrayList<>(escapePath.getPath());
		shortestPathCompletion.remove(shortestPathCompletion.indexOf(state.getCurrentNode()));
		shortTestPathCompletionGold = escapePath.getGold() - state.getCurrentNode().getTile().getGold();
		shortestPathLength = escapePath.getLength();

		timeout = System.currentTimeMillis() + this.MAX_TIME_IN_MS;
		populateStack();
		buildEscapePaths();

		System.out.println(String.format("%d additional paths found", numberOfPathsFound));
		return escapePath;
	}

	private void populateStack() {

		// stack = new ConcurrentLinkedDeque<>();
		stack = new LinkedBlockingDeque<>();

		Node n = escapeState.getCurrentNode();
		EscapePath p = new EscapePath(n);
		p.addGold(n.getTile().getGold());
		stack.add(p);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pool.shutdown();
	}

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

		if (returnValue == 0) { // Finally compare on id to enforce determinism
			returnValue = Long.compare(o2.getId(), o1.getId());
		}
		return returnValue;
	}

	class SearchThread implements Callable<Object> {

		private static final long serialVersionUID = 8473109576846837452L;

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
			// System.out.println(String.format("%s: Created %d paths, followed
			// %d", Thread.currentThread().getName(),
			// pathsCreated, pathsFollowed));
			return null;
		}

		private void processPath(EscapePath p) {

			while (p != null) {

				if (System.currentTimeMillis() > timeout) {
					return;
				}

				// Would reversing this path give us a new best solution?
				if (reversePathConditions(p)) {

					EscapePath cp = new EscapePath(p);
					// Add the route back
					for (int i = cp.getPath().size() - 2; i >= 0; i--) {
						cp.addNode(cp.getPath().get(i));
					}
					cp.addLength(cp.getLength());
					// Add the remainder of the shortest escape path
					for (Node n : shortestPathCompletion) {
						cp.addNode(n);
					}
					cp.addGold(shortTestPathCompletionGold);
					cp.addLength(shortestEscapePath.getLength());
					setEscapeRoute(cp);
				}

				// If the exits are covered then back out
				while (p.getPath().containsAll(p.getNode().getNeighbours())) {
					int firstIndexOfNode = p.getPath().indexOf(p.getNode());
					Node goBackNode = p.getPath().get(firstIndexOfNode - 1);
					Edge goBackEdge = p.getNode().getEdge(goBackNode);
					p.addNode(goBackNode);
					p.addLength(goBackEdge.length());
				}

				// If it's too far to go now then abandon this path
				if (p.getLength() >= escapeState.getTimeRemaining()) {
					p = getNextPath();
					continue;
				}

				// If it's probably out of range then get another path
				if (!isInRange(p.getNode(), p.getLength())) {
					// Queue this rather than stack it
					stack.add(p);
					p = getNextPath();
					continue;
				}

				List<Edge> newExits = new ArrayList<Edge>(p.getNode().getExits());
				Collections.sort(newExits, escapePathComparator);
				EscapePath continuePath = null;
				for (Edge e : newExits) {

					Node nextNode = e.getDest();
					if (continuePathConditions(p, e, nextNode)) {
						if (exit.equals(nextNode)) {
							setEscapeRoute(createNewEscapePath(p, nextNode, e));
						} else {
							if (continuePath == null) {
								continuePath = createNewEscapePath(p, nextNode, e);
							} else {
								EscapePath np = createNewEscapePath(p, nextNode, e);
								stackPath(np);
								// pathsCreated++;
								// stack.add(p);
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

		private void stackPath(EscapePath p) {

			pathsCreated++;
			stack.push(p);
		}

		private boolean reversePathConditions(EscapePath p) {

			if (p.getLength() == 0 || p.getGold() + shortTestPathCompletionGold < escapePath.getGold()) {
				return false;
			}
			return p.getLength() * 2 + shortestPathLength < escapeState.getTimeRemaining();
		}

		private boolean continuePathConditions(EscapePath p, Edge e, Node n) {

			if (p.getLength() + e.length > escapeState.getTimeRemaining()) {
				return false; // Too far
			}
			return !p.getPath().contains(n);
		}

		private EscapePath getNextPath() {

			try {
				pathsFollowed++;
				EscapePath returnPath = stack.pollFirst(1000, TimeUnit.MILLISECONDS);
				return returnPath;
				// return (stack.pop());
			} catch (InterruptedException e) {
				// The stack is empty so quit
				System.out.println(String.format("%s: Empty stack", Thread.currentThread().getName()));
				return null;
			}
		}

		private EscapePath createNewEscapePath(EscapePath p, Node n, Edge e) {
			EscapePath np = new EscapePath(p);
			np.addGold(n.getTile().getGold());
			np.addLength(e.length);
			np.addNode(n);
			return np;
		}
	}
}
