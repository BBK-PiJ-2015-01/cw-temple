package student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import game.Edge;
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

	// Comparator for evaluating exit paths
	private Comparator<Edge> escapePathComparator = (e1, e2) -> escapePathComparator(e1, e2);

	private List<Node> shortestPath;

	// The stack may actually require use as a queue
	private Deque<EscapePath> stack;

	public StackEscapePathFinder(EscapeState state) {

		super(state);
	}

	@Override
	public EscapePath findEscapePath(EscapeState state) {

		escapeState = state;
		exit = state.getExit();
		// The exit nodes have only one neighbour
		exitCovering = exit.getNeighbours().stream().findFirst().get();

		// Set the shortest escape route as a default
		escapePath = new ShortestEscapePathFinder(state).findEscapePath(escapeState);
		shortestPath = escapePath.getPath();
		timeout = System.currentTimeMillis() + this.MAX_TIME_IN_MS;
		populateStack(escapePath.getPath());

		try {
			buildEscapePaths();
		} catch (StackOverflowError e) {
			StackTraceElement[] s = e.getStackTrace();
			System.out.println("Halt");
		}
		System.out.println(String.format("%d additional paths found", numberOfPathsFound));
		return escapePath;
	}

	// Need to generate an EscapePath for each node in the shortest route
	private void populateStack(Collection<Node> nodes) {

		// System.out.println(String.format("%d paths supplied to stack",
		// nodes.size()));
		stack = new ConcurrentLinkedDeque<>();
		// stack = new LinkedBlockingDeque<>(1000);

		Node parentNode = null;
		EscapePath p = null;
		for (Node n : nodes) {

			// Don't add the final two path nodes
			if (n.equals(exit) || n.equals(exitCovering)) {
				continue;
			}

			final Node enclosingParentNode = parentNode;

			if (parentNode == null) { //
				p = new EscapePath(n);
				p.addGold(n.getTile().getGold());
				stack.add(p);
				parentNode = n;
				continue;
			}
			// Get the edge between the parent and the node
			Edge e = enclosingParentNode.getExits().stream().filter(edge -> edge.getDest().equals(n)).findFirst().get();

			p = new EscapePath(p);
			p.addGold(n.getTile().getGold());
			p.addLength(e.length());
			p.addNode(n);
			stack.add(p);
			parentNode = n;
		}

		// System.out.println(String.format("%d paths added to stack",
		// stack.size()));
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

		System.out.println("All threads complete");

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

		int pushedNodes;
		int nodesFound;
		int pathsCreated;

		@Override
		public Object call() {

			EscapePath p = getNextPath();
			if (p == null) {
				return null;
			}
			System.out.println(Thread.currentThread().getName() + ": Start process of  " + p.getNode().getId());
			processPath(p);
			System.out.println(Thread.currentThread().getName() + ": Completed process");
			return null;
		}

		private void processPath(EscapePath p) {

			while (p != null) {

				if (System.currentTimeMillis() > timeout) {
					return;
				}

				EscapePath firstPath = null;

				for (Edge e : p.getNode().getExits()) {

					Node nextNode = e.getDest();
					if (isInRange(nextNode, p.getLength()) && p.getLength() + e.length <= escapeState.getTimeRemaining()
							&& !p.getPath().contains(nextNode)) {
						// If the exit is covered then the only node that can be
						// added is the exit node as the path cannot cross
						// itself
						if (p.getPath().contains(exitCovering) && !nextNode.equals(exit)) {
							// System.out.println(Thread.currentThread().getName()
							// + ": Exit is covered");
							continue;
						}

						EscapePath np = new EscapePath(p);
						np.addGold(nextNode.getTile().getGold());
						np.addLength(e.length);
						np.addNode(nextNode);

						if (exit.equals(nextNode) && np.getLength() <= escapeState.getTimeRemaining()) {
							setEscapeRoute(np);
						} else {
							if (firstPath == null) {
								firstPath = np;
							} else {
								stack.push(np);
							}
						}
					}

				}
				if (firstPath != null) {
					p = firstPath;
				} else {
					p = getNextPath();
				}
			}

		}

		private EscapePath getNextPath() {

			try {
				return (stack.pop());
			} catch (NoSuchElementException e) {
				// The stack is empty so quit
				return null;
			}
		}
	}
}
