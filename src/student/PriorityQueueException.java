package student;

public class PriorityQueueException extends RuntimeException {

    /**
     * Constructor: an instance with message m
     */
    public PriorityQueueException(String m) {
        super(m);
    }

    /**
     * Constructor: an instance with no message
     */
    public PriorityQueueException() {
        super();
    }
}
