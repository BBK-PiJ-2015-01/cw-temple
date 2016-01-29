package student;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import game.Node;

/**
 * An instance of a path from the start {@code Node}. 
 * 
 * @author sbaird02
 *
 */
public class EscapePath {

	private int length;
	private int gold;
	private Node node;
	private List<Node> path;

	private EscapePath() {
		
	}
	/**
	 * Creates an path from the supplied node. Note that any gold will need to
	 * be added manually.
	 * 
	 * @param node
	 *            the starting position of the path
	 */
	public EscapePath(Node node) {

		this.node = node;
		path = new LinkedList<>();
		path.add(node);
	}

	/**
	 * Creates an path from the supplied path. This is essentially a clone of
	 * the supplied argument followed by an addNode({@code Node} node)
	 * 
	 * @param escapePath
	 *            an EscapePath to copy
	 */
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
	
	public Object clone() {
		
		EscapePath clone = new EscapePath();
		clone.node = node;
		clone.path = new LinkedList<>(path);
		clone.gold = gold;
		clone.length = length;
		return clone;		
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
		if (!Objects.equals(node, other.node)) {
			return false;
		}
		return Objects.equals(path, other.path);
	}
}
