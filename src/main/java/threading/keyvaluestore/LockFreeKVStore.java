package threading.keyvaluestore;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Lock-free key-value store using AtomicReferenceArray + CAS.
 * Simplified: integer keys & values.
 */
public class LockFreeKVStore {
    private static class Node {
        final int key;
        final int value;
        final Node next;

        Node(int key, int value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private final AtomicReferenceArray<Node> table;
    private final int capacity;

    public LockFreeKVStore(int capacity) {
        this.capacity = capacity;
        this.table = new AtomicReferenceArray<>(capacity);
    }

    private int hash(int key) {
        return Integer.hashCode(key) & 0x7fffffff;
    }

    /** Get value for key (lock-free, wait-free). */
    public Integer get(int key) {
        int idx = hash(key) % capacity;
        Node n = table.get(idx);
        while (n != null) {
            if (n.key == key) return n.value;
            n = n.next;
        }
        return null;
    }

    /** Put key/value (CAS-based). */
    public void put(int key, int value) {
        int idx = hash(key) % capacity;
        while (true) {
            Node head = table.get(idx);

            // check if key already exists
            for (Node n = head; n != null; n = n.next) {
                if (n.key == key) {
                    // replace value by inserting new node at head
                    Node newHead = new Node(key, value, removeFromChain(head, key));
                    if (table.compareAndSet(idx, head, newHead)) {
                        return;
                    } else {
                        break; // retry
                    }
                }
            }

            // insert new node at head
            Node newHead = new Node(key, value, head);
            if (table.compareAndSet(idx, head, newHead)) {
                return;
            }
            // else retry
        }
    }

    /** Remove key (CAS-based). Returns true if removed. */
    public boolean remove(int key) {
        int idx = hash(key) % capacity;
        while (true) {
            Node head = table.get(idx);
            if (head == null) return false;

            Node newHead = removeFromChain(head, key);
            if (newHead == head) {
                // key not found
                return false;
            }
            if (table.compareAndSet(idx, head, newHead)) {
                return true;
            }
            // retry
        }
    }

    /** Utility: rebuild chain without given key. */
    private Node removeFromChain(Node head, int key) {
        if (head == null) return null;
        if (head.key == key) return head.next;

        Node newNext = removeFromChain(head.next, key);
        if (newNext == head.next) return head; // unchanged
        return new Node(head.key, head.value, newNext);
    }

    // ------------------- DEMO -------------------
    public static void main(String[] args) throws InterruptedException {
        LockFreeKVStore kv = new LockFreeKVStore(1024);

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
        kv.remove(42);
        System.out.println("After remove, value for 42 = " + kv.get(42));
    }
}
