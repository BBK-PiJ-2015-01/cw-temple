package student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

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
		
		timeout = System.currentTimeMillis() + this.MAX_TIME_IN_MS;
		populateStack(escapePath.getPath());
		buildEscapePaths();
		System.out.println(String.format("%d additional paths found", numberOfPathsFound));
		return escapePath;
	}

	// Need to generate an EscapePath for each node in the shortest route
	private void populateStack(Collection<Node> nodes) {

//		System.out.println(String.format("%d paths supplied to stack", nodes.size()));
		stack = new ConcurrentLinkedDeque<>();

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
			stack.add(p);
			parentNode = n;
		}

//		System.out.println(String.format("%d paths added to stack", stack.size()));
	}

	private void buildEscapePaths() {

//		int maxThreads = Runtime.getRuntime().availableProcessors();
		ForkJoinPool pool = new ForkJoinPool();
//		ExecutorService pool = Executors.newFixedThreadPool(maxThreads);
		int submissions = 0;
		while(System.currentTimeMillis() < timeout) {
			
			pool.invoke(new SearchThread());
			
//			System.out.println(++submissions + " invokations");
		}

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

	class SearchThread extends RecursiveAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8473109576846837452L;

		@Override
		public void compute() {
			
			if (System.currentTimeMillis() > timeout) {
				return;
			}
			
			// Take the top Escape path from the stack
			EscapePath p = null;
			try {
				p = stack.pop();
			} catch(NoSuchElementException e) {
				// The stack is empty so quit
				return;
			}

			List<Edge> newExits = new ArrayList<Edge>(p.getNode().getExits());
			Collections.sort(newExits, escapePathComparator);
			for (Edge e : newExits) {

				Node nextNode = e.getDest();
				if (isInRange(nextNode, p.getLength()) && p.getLength() + e.length <= escapeState.getTimeRemaining()
						&& !p.getPath().contains(nextNode)) {
					// If the exit is covered then the only node that can be
					// added
					// is the exit node as the path cannot cross itself
					if (p.getPath().contains(exitCovering) && !nextNode.equals(exit)) {
						continue;
					}

					EscapePath np = new EscapePath(p);
					np.addGold(nextNode.getTile().getGold());
					np.addLength(e.length);
					np.addNode(nextNode);

					if (exit.equals(nextNode) && np.getLength() <= escapeState.getTimeRemaining()) {
//						System.out.println(Thread.currentThread().getName() + ": Solution found");
						setEscapeRoute(np);
						continue;
					} else {
						stack.push(np);
					}
				}
				invokeAll(new SearchThread());
			}
		}
	}
}
