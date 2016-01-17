package student;

import game.Node;

public class SearchNode {

	private Node node;
	private Node parentNode;
	private int gCost;
	private int hCost;
	private boolean isOpen = true;
	
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
