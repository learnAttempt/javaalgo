package threading.lrucache;


import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


public class SegmentedLRUCacheVersion2<K, V> {
    private final int numSegments;
    private final CoarseLockLRUCache<K, V>[] segments;
    private final int segmentMask;

    @SuppressWarnings("unchecked")
    public SegmentedLRUCacheVersion2(int totalCapacity, int segmentsCount) {
        if (segmentsCount <= 0) throw new IllegalArgumentException("segments > 0");
        // make segments power of two for fast masking
        int s = 1;
        while (s < segmentsCount) s <<= 1;
        this.numSegments = s;
        this.segmentMask = s - 1;
        int perSegment = Math.max(1, totalCapacity / s);
        this.segments = new CoarseLockLRUCache[s];
        for (int i = 0; i < s; i++) {
            this.segments[i] = new CoarseLockLRUCache<>(perSegment);
        }
    }

    private int spreadHash(Object key) {
        int h = key == null ? 0 : key.hashCode();
        h ^= (h >>> 16);
        return h;
    }

    private CoarseLockLRUCache<K, V> segmentFor(K key) {
        int h = spreadHash(key);
        int idx = h & segmentMask;
        return segments[idx];
    }

    public V get(K key) {
        return segmentFor(key).get(key);
    }

    public void put(K key, V value) {
        segmentFor(key).put(key, value);
    }

    // For tests only; not atomic across segments
    public int totalSize() {
        int sum = 0;
        for (CoarseLockLRUCache<K, V> seg : segments) sum += seg.size();
        return sum;
    }

    // Small concurrency test
    public static void main(String[] args) throws InterruptedException {
        final SegmentedLRUCache<Integer, Integer> cache = new SegmentedLRUCache<>(1000, 16);
        final int THREADS = 32;
        final int OPS = 100_000;
        Thread[] threads = new Thread[THREADS];
        final AtomicInteger hits = new AtomicInteger();
        final AtomicInteger misses = new AtomicInteger();

        for (int t = 0; t < THREADS; t++) {
            threads[t] = new Thread(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                for (int i = 0; i < OPS; i++) {
                    int key = rnd.nextInt(0, 2000);
                    if (rnd.nextDouble() < 0.5) {
                        Integer v = cache.get(key);
                        if (v != null) hits.incrementAndGet();
                        else misses.incrementAndGet();
                    } else {
                        cache.put(key, key);
                    }
                }
            });
            threads[t].start();
        }
        for (Thread th : threads) th.join();
        System.out.println("Done. totalSize=" + cache.size() +
                " hits=" + hits.get() + " misses=" + misses.get());
    }
}


 class CoarseLockLRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node> map;
    private final ReentrantLock lock = new ReentrantLock();

    private Node head; // most recent
    private Node tail; // least recent
    private int size = 0;

    private class Node {
        K key;
        V value;
        Node prev, next;
        Node(K k, V v) { key = k; value = v; }
    }

    public CoarseLockLRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity > 0");
        this.capacity = capacity;
        this.map = new HashMap<>(capacity * 2);
    }

    public V get(K key) {
        lock.lock();
        try {
            Node n = map.get(key);
            if (n == null) return null;
            moveToHead(n);
            return n.value;
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            Node n = map.get(key);
            if (n != null) {
                n.value = value;
                moveToHead(n);
                return;
            }
            Node newNode = new Node(key, value);
            addToHead(newNode);
            map.put(key, newNode);
            size++;
            if (size > capacity) {
                Node removed = removeTail();
                if (removed != null) {
                    map.remove(removed.key);
                    size--;
                }
            }
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

    private void addToHead(Node n) {
        n.prev = null;
        n.next = head;
        if (head != null) head.prev = n;
        head = n;
        if (tail == null) tail = head;
    }

    private void moveToHead(Node n) {
        if (n == head) return;
        // unlink
        if (n.prev != null) n.prev.next = n.next;
        if (n.next != null) n.next.prev = n.prev;
        if (n == tail) tail = n.prev;
        // put at head
        n.prev = null;
        n.next = head;
        if (head != null) head.prev = n;
        head = n;
    }

    private Node removeTail() {
        if (tail == null) return null;
        Node old = tail;
        if (tail.prev != null) {
            tail = tail.prev;
            tail.next = null;
        } else {
            // single element
            head = tail = null;
        }
        old.prev = old.next = null;
        return old;
    }

    // For quick manual debug/inspection (not thread-safe if called externally)
    public String debugKeysInOrder() {
        StringBuilder sb = new StringBuilder();
        Node cur = head;
        while (cur != null) {
            sb.append(cur.key).append(" ");
            cur = cur.next;
        }
        return sb.toString().trim();
    }

    // Simple test
    public static void main(String[] args) throws InterruptedException {
        CoarseLockLRUCache<Integer, String> cache = new CoarseLockLRUCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        System.out.println("Initial: " + cache.debugKeysInOrder()); // 3 2 1
        cache.get(1);
        System.out.println("After get(1): " + cache.debugKeysInOrder()); // 1 3 2
        cache.put(4, "four");
        System.out.println("After put(4): " + cache.debugKeysInOrder()); // 4 1 3 (2 evicted)
    }
}


