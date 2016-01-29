package student;

/**
 * Represents a node used for exploring. The node can be closed to prevent it
 * from further evaluation.
 * 
 * @author sbaird02
 *
 */
public class ExploreNode {

	private long id;
	private long parentId;
	private int gCost;
	private int hCost;
	private boolean isOpen = true;

	/**
	 * Constructor to generate an open node.
	 * 
	 * @param id
	 *            the id of the {@code Node} this represents
	 * @param parentId
	 *            the id of the previous {@code Node} in the explore path
	 * @param gCost
	 *            this is the accumulated length of the path to the node
	 * @param hCost
	 *            the heuristic cost associated with this SearchNode
	 */
	public ExploreNode(long id, long parentId, int gCost, int hCost) {

		this.id = id;
		this.parentId = parentId;
		this.gCost = gCost;
		this.hCost = hCost;
	}

	public long getId() {
		return id;
	}

	public long getParentId() {
		return parentId;
	}

	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

	public int getfCost() {
		return gCost + hCost;
	}

	public int getgCost() {
		return gCost;
	}

	public void setgCost(int gCost) {
		this.gCost = gCost;
	}

	public int gethCost() {
		return hCost;
	}

	public void sethCost(int hCost) {
		this.hCost = hCost;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}
}
