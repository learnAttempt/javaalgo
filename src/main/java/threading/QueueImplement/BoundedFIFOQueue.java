package threading.QueueImplement;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedFIFOQueue<T> {
    private final Object[] buffer;
    private int head = 0;    // next element to remove
    private int tail = 0;    // next position to insert
    private int size = 0;    // current number of elements

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull  = lock.newCondition();

    public BoundedFIFOQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");
        buffer = new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (size == buffer.length) {
                notFull.await(); // wait if full
            }
            buffer[tail] = item;
            tail = (tail + 1) % buffer.length;
            size++;
            notEmpty.signal(); // wake up a consumer
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (size == 0) {
                notEmpty.await(); // wait if empty
            }
            T item = (T) buffer[head];
            buffer[head] = null;
            head = (head + 1) % buffer.length;
            size--;
            notFull.signal(); // wake up a producer
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }
}
