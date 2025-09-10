package threading.keyvaluestore;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe hash map with striped locking.
 * Simplified version (keys/values are integers).
 */
public class ConcurrentKVStore {
    private static class Node {
        final int key;
        int value;
        Node next;
        Node(int key, int value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private final Node[] table;
    private final ReentrantLock[] locks;
    private final int capacity;

    public ConcurrentKVStore(int capacity, int stripes) {
        this.capacity = capacity;
        this.table = new Node[capacity];
        this.locks = new ReentrantLock[stripes];
        for (int i = 0; i < stripes; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    private int hash(int key) {
        return Integer.hashCode(key) & 0x7fffffff;
    }

    private ReentrantLock getLockForKey(int key) {
        int h = hash(key);
        return locks[h % locks.length];
    }

    /** Put or update value for key. */
    public void put(int key, int value) {
        ReentrantLock lock = getLockForKey(key);
        lock.lock();
        try {
            int idx = hash(key) % capacity;
            Node head = table[idx];
            for (Node n = head; n != null; n = n.next) {
                if (n.key == key) {
                    n.value = value; // update
                    return;
                }
            }
            table[idx] = new Node(key, value, head);
        } finally {
            lock.unlock();
        }
    }

    /** Get value for key, or null if not present. */
    public Integer get(int key) {
        int idx = hash(key) % capacity;
        for (Node n = table[idx]; n != null; n = n.next) {
            if (n.key == key) return n.value;
        }
        return null;
    }

    /** Remove key, return true if removed. */
    public boolean remove(int key) {
        ReentrantLock lock = getLockForKey(key);
        lock.lock();
        try {
            int idx = hash(key) % capacity;
            Node prev = null, curr = table[idx];
            while (curr != null) {
                if (curr.key == key) {
                    if (prev == null) table[idx] = curr.next;
                    else prev.next = curr.next;
                    return true;
                }
                prev = curr;
                curr = curr.next;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    // ------------------- DEMO -------------------
    public static void main(String[] args) throws InterruptedException {
        ConcurrentKVStore kv = new ConcurrentKVStore(1024, 16);

        Runnable writer = () -> {
            for (int i = 0; i < 10000; i++) {
                kv.put(i, i * 10);
            }
        };

        Runnable reader = () -> {
            for (int i = 0; i < 10000; i++) {
                kv.get(i);
            }
        };

        Thread t1 = new Thread(writer);
        Thread t2 = new Thread(reader);
        Thread t3 = new Thread(writer);
        t1.start(); t2.start(); t3.start();
        t1.join(); t2.join(); t3.join();

        System.out.println("Value for 42 = " + kv.get(42));
    }
}
