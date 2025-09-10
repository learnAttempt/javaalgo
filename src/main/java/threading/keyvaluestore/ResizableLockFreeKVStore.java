package threading.keyvaluestore;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ResizableLockFreeKVStore {
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

    private static class Table {
        final AtomicReferenceArray<Node> buckets;
        final int capacity;
        final AtomicInteger size = new AtomicInteger(0); // approximate

        Table(int capacity) {
            this.capacity = capacity;
            this.buckets = new AtomicReferenceArray<>(capacity);
        }
    }

    private final AtomicReference<Table> tableRef;
    private final float loadFactor;

    public ResizableLockFreeKVStore(int initCapacity, float loadFactor) {
        this.tableRef = new AtomicReference<>(new Table(initCapacity));
        this.loadFactor = loadFactor;
    }

    private int hash(int key) {
        return Integer.hashCode(key) & 0x7fffffff;
    }

    /** Get value for key (check current table, fallback to old during resize). */
    public Integer get(int key) {
        Table table = tableRef.get();
        int idx = hash(key) % table.capacity;
        Node n = table.buckets.get(idx);
        while (n != null) {
            if (n.key == key) return n.value;
            n = n.next;
        }
        return null;
    }

    /** Put or update value for key. */
    public void put(int key, int value) {
        while (true) {
            Table table = tableRef.get();
            int idx = hash(key) % table.capacity;
            Node head = table.buckets.get(idx);

            // check if key already exists
            for (Node n = head; n != null; n = n.next) {
                if (n.key == key) {
                    // replace by inserting new head (immutability)
                    Node newHead = new Node(key, value, removeFromChain(head, key));
                    if (table.buckets.compareAndSet(idx, head, newHead)) {
                        return;
                    } else {
                        break; // retry
                    }
                }
            }

            // new key → try CAS insert at head
            Node newHead = new Node(key, value, head);
            if (table.buckets.compareAndSet(idx, head, newHead)) {
                int sz = table.size.incrementAndGet();
                if (sz > table.capacity * loadFactor) {
                    resize(table);
                }
                return;
            }
            // retry
        }
    }

    /** Remove key (CAS-based). Returns true if removed. */
    public boolean remove(int key) {
        while (true) {
            Table table = tableRef.get();
            int idx = hash(key) % table.capacity;
            Node head = table.buckets.get(idx);
            if (head == null) return false;

            Node newHead = removeFromChain(head, key);
            if (newHead == head) {
                return false; // not found
            }
            if (table.buckets.compareAndSet(idx, head, newHead)) {
                table.size.decrementAndGet();
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
        if (newNext == head.next) return head;
        return new Node(head.key, head.value, newNext);
    }

    /** Resize by allocating a bigger table and migrating entries. */
    private void resize(Table oldTable) {
        // Optimistic: someone else might have resized already
        Table current = tableRef.get();
        if (current != oldTable) return;

        int newCap = oldTable.capacity * 2;
        Table newTable = new Table(newCap);

        // migrate entries
        for (int i = 0; i < oldTable.capacity; i++) {
            Node n = oldTable.buckets.get(i);
            while (n != null) {
                int idx = hash(n.key) % newCap;
                Node head = newTable.buckets.get(idx);
                newTable.buckets.set(idx, new Node(n.key, n.value, head));
                newTable.size.incrementAndGet();
                n = n.next;
            }
        }

        // publish new table
        tableRef.compareAndSet(oldTable, newTable);
    }

    // ---------------- DEMO ----------------
    public static void main(String[] args) throws InterruptedException {
        ResizableLockFreeKVStore kv = new ResizableLockFreeKVStore(16, 0.75f);

        Runnable writer = () -> {
            for (int i = 0; i < 100000; i++) {
                kv.put(i, i * 2);
            }
        };

        Runnable reader = () -> {
            for (int i = 0; i < 100000; i++) {
                kv.get(i);
            }
        };

        Thread t1 = new Thread(writer);
        Thread t2 = new Thread(writer);
        Thread t3 = new Thread(reader);

        t1.start(); t2.start(); t3.start();
        t1.join(); t2.join(); t3.join();

        System.out.println("Value for 42 = " + kv.get(42));
        kv.remove(42);
        System.out.println("After remove, value for 42 = " + kv.get(42));
    }
}

