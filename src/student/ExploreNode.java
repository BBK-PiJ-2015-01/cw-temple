package student;

public class ExploreNode {

	private long id;
	private long parentId;
	private int gCost;
	private int hCost;
	private boolean isOpen = true;

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
