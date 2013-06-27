package mon4h.collector.queue;

import java.io.Serializable;

public interface Queue<T extends Serializable> {

	public void product(T item) throws QueueException;
	public void productBatch(T[] item) throws QueueException;
	public T consume() throws QueueException;
	public T[] consumeBatch(int maxSize) throws QueueException;
	

	public static class QueueException extends Exception {
		private static final long serialVersionUID = 7714244345143293742L;

		public QueueException() {
			super();
		}

		public QueueException(String message) {
			super(message);
		}

		public QueueException(String message, Throwable cause) {
			super(message, cause);
		}

		public QueueException(Throwable cause) {
			super(cause);
		}
	}
}
