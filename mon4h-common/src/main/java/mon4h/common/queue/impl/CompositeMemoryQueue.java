package mon4h.common.queue.impl;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import mon4h.common.queue.Queue;

public class CompositeMemoryQueue<T extends Serializable> implements Queue<T> {

	private final String name;
	private final int maxNumberInMemory;
	private final Queue<T> backedQueue;
	private final BlockingQueue<T> memoryQueue;
	
	public CompositeMemoryQueue(String name, int maxNumberInMemory, Queue<T> backedQueue) {
		this.name = name;
		this.maxNumberInMemory = maxNumberInMemory;
		this.backedQueue = backedQueue;
		this.memoryQueue = new ArrayBlockingQueue<T>(maxNumberInMemory);
	}
	
	public String getName() {
		return name;
	}
	
	public Queue<T> getBackedQueue() {
		return backedQueue;
	}
	
	@Override
	public void produce(T item) throws mon4h.common.queue.Queue.QueueException {
		if ((!memoryQueue.offer(item)) && backedQueue != null) {
			backedQueue.produce(item);
		}
	}

	@Override
	public T consume() throws mon4h.common.queue.Queue.QueueException {
		T item = null;
		if (backedQueue != null && backedQueue.readAvailable()) {
			try {
				item = backedQueue.consume();
			} catch (QueueException e) {
			}
		}
		if (item == null) {
			item = memoryQueue.poll();
		}
		return item;
	}

	@Override
	public boolean readAvailable() {
		return (memoryQueue.size() > 0) || ((backedQueue != null) && backedQueue.readAvailable());
	}

	@Override
	public boolean writeAvailable() {
		return (memoryQueue.size() < maxNumberInMemory) || ((backedQueue != null) && backedQueue.writeAvailable());
	}

	@Override
	public void shutdown() throws mon4h.common.queue.Queue.QueueException {
		T item;
		while (!memoryQueue.isEmpty()) {
			item = memoryQueue.poll();
			backedQueue.produce(item);
		}
		backedQueue.shutdown();
	}

}
