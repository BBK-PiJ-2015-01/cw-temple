package student;

import game.EscapeState;

/**
 * Objects implementing this interface must provide a complete
 * {@link EscapePath} that exits the Cavern within the specified time.
 * 
 * @author sbaird02
 *
 */
public interface EscapePathFinder {

	EscapePath findEscapePath(EscapeState state);
}
