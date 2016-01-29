package student;

import game.Node;

/**
 * Represents a node used for path searching. The node can be closed to prevent
 * it from further evaluation.
 */
public class SearchNode {

	private Node node;
	private Node parentNode;
	private int gCost;
	private int hCost;
	private boolean isOpen = true;

	/**
	 * Constructor to generate an open node.
	 * 
	 * @param node
	 *            the {@code Node} this represents
	 * @param parentNode
	 *            the previous {@code Node} in the resolved path
	 * @param gCost
	 *            this is the accumulated length of the path to the node
	 * @param hCost
	 *            the heuristic cost associated with this SearchNode
	 */
	public SearchNode(Node node, Node parentNode, int gCost, int hCost) {

		this.node = node;
		this.parentNode = parentNode;
		this.gCost = gCost;
		this.hCost = hCost;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public Node getParentNode() {
		return parentNode;
	}

	public void setParentNode(Node parentNode) {
		this.parentNode = parentNode;
	}

	public int getFCost() {
		return gCost + hCost;
	}

	public int getGCost() {
		return gCost;
	}

	public void setGCost(int gCost) {
		this.gCost = gCost;
	}

	public int getHCost() {
		return hCost;
	}

	public void setHCost(int hCost) {
		this.hCost = hCost;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void close() {
		isOpen = false;
	}
}
