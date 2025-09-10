package threading.QueueImplement;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

/** A bounded, blocking, thread-safe circular queue (ring buffer). */
public final class CircularBlockingQueue<E> {
    private final E[] buf;
    private int head = 0;   // next position to take
    private int tail = 0;   // next position to put
    private int size = 0;   // number of elements

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull  = lock.newCondition();

    @SuppressWarnings("unchecked")
    public CircularBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.buf = (E[]) new Object[capacity];
    }

    public int capacity() { return buf.length; }

    public int size() {
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }

    /** Inserts, blocking while full. */
    public void put(E e) throws InterruptedException {
        Objects.requireNonNull(e);
        lock.lockInterruptibly();
        try {
            while (size == buf.length) {
                notFull.await();
            }
            enqueue(e);
            notEmpty.signal(); // wake a taker
        } finally {
            lock.unlock();
        }
    }

    /** Removes and returns head, blocking while empty. */
    public E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (size == 0) {
                notEmpty.await();
            }
            E e = dequeue();
            notFull.signal(); // wake a producer
            return e;
        } finally {
            lock.unlock();
        }
    }

    /** Non-blocking: returns true if enqueued. */
    public boolean offer(E e) {
        Objects.requireNonNull(e);
        lock.lock();
        try {
            if (size == buf.length) return false;
            enqueue(e);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Timed offer. */
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(e);
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (size == buf.length) {
                if (nanos <= 0L) return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Non-blocking: returns null if empty. */
    public E poll() {
        lock.lock();
        try {
            if (size == 0) return null;
            E e = dequeue();
            notFull.signal();
            return e;
        } finally {
            lock.unlock();
        }
    }

    /** Timed poll. */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (size == 0) {
                if (nanos <= 0L) return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            E e = dequeue();
            notFull.signal();
            return e;
        } finally {
            lock.unlock();
        }
    }

    // Internal helpers (callers already hold the lock)
    private void enqueue(E e) {
        buf[tail] = e;
        tail = (tail + 1) % buf.length;
        size++;
    }

    private E dequeue() {
        E e = buf[head];
        buf[head] = null; // help GC
        head = (head + 1) % buf.length;
        size--;
        return e;
    }
}
