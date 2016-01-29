package student;

import java.util.Comparator;

/**
 * A {@code Comparator} used to order the {@code EscapePath}s currently on the
 * evaluation stack.
 * 
 * @author sbaird02
 *
 */
public class EscapePathOrderComparator implements Comparator<EscapePath> {

	@Override
	public int compare(EscapePath o1, EscapePath o2) {

		// Order by descending gold, ascending length and then id
		int comparison = Integer.compare(o2.getGold(), o1.getGold());
		if (comparison == 0) {
			comparison = Integer.compare(o1.getLength(), o2.getLength());
		}
		if (comparison == 0) {
			comparison = Long.compare(o1.getNode().getId(), o1.getNode().getId());
		}
		return comparison;
	}

}
