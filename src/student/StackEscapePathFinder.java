package student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import game.Edge;
import game.EscapeState;
import game.Node;

/**
 * Multi-threaded Path finder which uses the shortest path as a base. New paths
 * are created by iterating from the current node and stacked. Each thread pops
 * the best available incomplete {@code EscapePath} and pursues it until it is
 * no longer viable.
 */
public class StackEscapePathFinder extends AbstractEscapePathFinder {

	private Node exit;

	private long timeout; // Elapsed time of exit planning

	// Wait time for empty stack reads
	private final int STACK_TIMEOUT_IN_MILLIS = 100;

	// A use a sorted set to provide a stack
	private SortedSet<EscapePath> stack;

	// The shortest path is used as the basis for much of the path completion
	private EscapePath shortestEscapePath;
	private int shortestPathLength;
	private List<Node> shortestPathCompletion;
	private int shortTestPathCompletionGold;

	// Dead-end nodes that we need to ignore
	private Set<Node> closedNodes;

	// Weighted gold values for each node
	private Map<Node, Integer> goldValues;

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
		timeout = System.currentTimeMillis() + MAX_TIME_IN_MS / 1;

		// Pre-processing tasks
		setUpTasks(state);

		// Formulate the plan
		populateStack();
		buildEscapePaths();

		System.out.println(String.format("%d additional paths found, %d incomplete", numberOfPathsFound, stack.size()));
		return escapePath;
	}

	/*
	 * Perform some initial analysis of the map
	 */
	private void setUpTasks(EscapeState state) {

		// Allow for parallel processing
		closedNodes = Collections.synchronizedSet(new HashSet<>());

		// Close of all dead end nodes that do not have any gold
		state.getVertices().parallelStream().filter(dn -> !exit.equals(dn)).filter(dn -> dn.getExits().size() == 1)
				.collect(Collectors.toSet()).stream().forEach(dn -> closedNodeSetter(dn));

		// Get the weighted gold values for all the nodes
		goldValues = new HashMap<>();
		state.getVertices().parallelStream().forEach(n -> weightedGoldValueSetter(n));
	}

	/*
	 * The closed node set is initially populated with Nodes that only have one
	 * exit and no gold. These are expanded into those neighbour nodes with 2
	 * exits and no gold. This has the effect of closing off dead ends that have
	 * no gold
	 */
	private synchronized void closedNodeSetter(Node n) {

		if (n.equals(escapeState.getCurrentNode()) || n.getTile().getGold() != 0 || n.getNeighbours().size() > 2) {
			return;
		}
		closedNodes.add(n);
		n.getNeighbours().stream().filter(nn -> !closedNodes.contains(nn)).forEach(nn -> closedNodeSetter(nn));
	}

	/*
	 * Include a value for the gold in the neighbouring nodes so we can make
	 * more informed choices later
	 */
	private synchronized void weightedGoldValueSetter(Node n) {

		int neighbouringGoldValue = 0;
		for (Node neighbour : n.getNeighbours()) {
			neighbouringGoldValue += neighbour.getTile().getGold() / 2;
		}
		goldValues.put(n, n.getTile().getGold() + neighbouringGoldValue);
	}

	/*
	 * The initial value on the stack is a path with only the start node
	 */
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

	/*
	 * Use all available threads
	 */
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

		// Comparator for evaluating node selection order
		private Comparator<Edge> escapePathComparator = (e1, e2) -> escapePathComparator(e1, e2);

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

		/*
		 * Follow each path while it is viable. When the path splits (at each
		 * exit path) spawn a new path and push it onto the stack for later
		 * processing
		 */
		private void processPath(EscapePath p) {

			// Used a while loop as recursion ran into stack problems
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

				// Check each path to see if reversing it out gives us a new
				// best solution
				if (reversePathConditions(p)) {
					reversePathToExit(p, p.getNode());
				}

				// If all our exits are blocked then go back until something is
				// open and then stack the new path
				EscapePath returnPath = p;
				Node endNode = p.getNode();
				int currentIndex = p.getPath().size() - 1;
				while (!pathIsOpen(returnPath)) {
					if (returnPath == null) {
						returnPath = (EscapePath) p.clone();
					}
					endNode = p.getPath().get(--currentIndex);
					int returnLength = returnPath.getNode().getEdge(endNode).length();
					returnPath.addNode(endNode);
					returnPath.addLength(returnLength);
				}
				if (returnPath != p) {
					stackPath(returnPath);
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
							if (nodeExistsInPath && reversePathConditions(p)) {
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

			stack.add(p);
		}

		/*
		 * Check to see if we want to escape back down the way we came
		 */
		private boolean reversePathConditions(EscapePath p) {

			if (p.getLength() == 0 || p.getGold() + shortTestPathCompletionGold < escapePath.getGold()) {
				return false;
			}
			return p.getLength() + shortestPathLength < escapeState.getTimeRemaining();
		}

		/*
		 * Check if all exits are closed, either already in the path or in the
		 * closed nodes list
		 */
		private boolean pathIsOpen(EscapePath p) {

			return p.getNode().getNeighbours().stream().filter(n -> !p.getPath().contains(n))
					.filter(n -> !closedNodes.contains(n)).findFirst().isPresent();
		}

		/*
		 * Check to see if we want to pursue this path using the supplied node
		 */
		private boolean continuePathConditions(EscapePath p, Edge e, Node n) {

			if (closedNodes.contains(n)) {
				return false;
			}
			return p.getLength() + e.length <= escapeState.getTimeRemaining();
		}

		/*
		 * Collect the highest value unresolved path. See notes above on
		 * synchronization
		 */
		private EscapePath getNextPath() {

			EscapePath returnPath = removeFromStack();

			// Try sleeping and looping until timeout
			while (returnPath == null && System.currentTimeMillis() < timeout) {
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

			// Primary comparison is weighted gold value
			int returnValue = Integer.compare(goldValues.get(o2), goldValues.get(o1));

			if (returnValue == 0) { // Edge length
				returnValue = Integer.compare(e1.length(), e2.length());
			}
			// Compare on distance to exit
			Double d1 = euclideanDistance(o1, exit);
			Double d2 = euclideanDistance(o2, exit);
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
