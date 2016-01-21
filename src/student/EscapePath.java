package student;

import java.util.ArrayList;
import java.util.List;

import game.Node;

public class EscapePath {

	private int length;
	private int gold;
	private Node node;
	private List<Node> path;

	public EscapePath(Node node) {

		this.node = node;

		path = new ArrayList<>();
		path.add(node);
	}

	public EscapePath(EscapePath escapePath) {

		length = escapePath.length;
		gold = escapePath.gold;
		node = escapePath.node;
		path = new ArrayList<>(escapePath.path);

	}

	public void addLength(int length) {
		this.length += length;
	}

	public int getLength() {
		return length;
	}

	public void addGold(int gold) {
		this.gold += gold;
	}

	public int getGold() {
		return gold;
	}

	public void addNode(Node node) {

		this.node = node;
		path.add(node);
	}

	public Node getNode() {
		return node;
	}

	public List<Node> getPath() {
		return path;
	}

	public void setPath(List<Node> path) {
		this.path = path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + gold;
		result = prime * result + length;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass())
			return false;
		
		EscapePath other = (EscapePath) obj;
		if (gold != other.gold) {
			return false;
		}
		if (length != other.length) {
			return false;
		}
		if (node == null && other.node != null) {
			return false;
		}
		return node.getId() == other.node.getId();
	}
}
