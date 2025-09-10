package threading.lrucache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Objects;

/**
 * Segmented (sharded) thread-safe LRU Cache.
 * Each segment has its own lock and LRU list.
 */
public class SegmentedLRUCache<K, V> {
    private final int capacity;
    private final int segmentCount;
    private final Segment<K, V>[] segments;

    @SuppressWarnings("unchecked")
    public SegmentedLRUCache(int capacity, int segmentCount) {
        if (capacity <= 0 || segmentCount <= 0)
            throw new IllegalArgumentException("capacity and segmentCount must be > 0");

        this.capacity = capacity;
        this.segmentCount = segmentCount;
        this.segments = new Segment[segmentCount];

        int perSegment = (int) Math.ceil((double) capacity / segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            segments[i] = new Segment<>(perSegment);
        }
    }

    private Segment<K, V> segmentFor(Object key) {
        int h = key.hashCode();
        // spread bits to avoid clustering
        h ^= (h >>> 16);
        return segments[(h & 0x7fffffff) % segmentCount];
    }

    public V get(K key) {
        return segmentFor(key).get(key);
    }

    public void put(K key, V value) {
        segmentFor(key).put(key, value);
    }

    public V remove(K key) {
        return segmentFor(key).remove(key);
    }

    public int size() {
        int total = 0;
        for (Segment<K, V> seg : segments) {
            total += seg.size();
        }
        return total;
    }

    public void clear() {
        for (Segment<K, V> seg : segments) {
            seg.clear();
        }
    }

    // -------- Segment (mini LRU) --------
    private static class Segment<K, V> {
        private final int capacity;
        private final ConcurrentHashMap<K, Node<K, V>> map;
        private final ReentrantLock lock = new ReentrantLock();

        private final Node<K, V> head;
        private final Node<K, V> tail;

        Segment(int capacity) {
            this.capacity = capacity;
            this.map = new ConcurrentHashMap<>(capacity * 2);
            this.head = new Node<>(null, null);
            this.tail = new Node<>(null, null);
            head.next = tail;
            tail.prev = head;
        }

        V get(K key) {
            Node<K, V> node = map.get(key);
            if (node == null) return null;
            moveToFront(node);
            return node.value;
        }

        void put(K key, V value) {
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                moveToFront(existing);
                return;
            }

            Node<K, V> newNode = new Node<>(key, value);
            Node<K, V> prev = map.putIfAbsent(key, newNode);
            if (prev != null) {
                prev.value = value;
                moveToFront(prev);
                return;
            }

            lock.lock();
            try {
                insertAtFront(newNode);
                if (map.size() > capacity) {
                    Node<K, V> lru = tail.prev;
                    if (lru != head) {
                        removeNode(lru);
                        map.remove(lru.key);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        V remove(K key) {
            Node<K, V> node = map.remove(key);
            if (node == null) return null;
            lock.lock();
            try {
                removeNode(node);
            } finally {
                lock.unlock();
            }
            return node.value;
        }

        int size() {
            return map.size();
        }

        void clear() {
            lock.lock();
            try {
                map.clear();
                head.next = tail;
                tail.prev = head;
            } finally {
                lock.unlock();
            }
        }

        // --- Doubly-linked list ops ---
        private void moveToFront(Node<K, V> node) {
            lock.lock();
            try {
                removeNode(node);
                insertAtFront(node);
            } finally {
                lock.unlock();
            }
        }

        private void insertAtFront(Node<K, V> node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        private void removeNode(Node<K, V> node) {
            Node<K, V> p = node.prev;
            Node<K, V> n = node.next;
            if (p != null) p.next = n;
            if (n != null) n.prev = p;
            node.prev = null;
            node.next = null;
        }
    }

    // -------- Node --------
    private static final class Node<K, V> {
        final K key;
        volatile V value;
        Node<K, V> prev, next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}

