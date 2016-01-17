package student;

import java.util.LinkedList;
import java.util.List;

import game.Node;

public class EscapePath {

	private int length = 0;
	private int gold = 0;
	private Node node;
	List<Node> path;

	public EscapePath(Node node) {

		this.node = node;
		path = new LinkedList<>();
		path.add(node);
	}

	public EscapePath(EscapePath escapePath) {

		length = escapePath.length;
		gold = escapePath.gold;
		node = escapePath.node;
		path = new LinkedList<>(escapePath.path);
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

}
