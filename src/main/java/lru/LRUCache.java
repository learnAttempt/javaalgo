package lru;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCache<K, V> {
    private final int capacity;
    private final ConcurrentHashMap<K, V> cache;
    private final ConcurrentLinkedDeque<K> accessOrder;
    private final ReentrantLock lock = new ReentrantLock();

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
        this.accessOrder = new ConcurrentLinkedDeque<>();
    }

    public V get(K key) {
        lock.lock();
        try {
            V value = cache.get(key);
            if (value != null) {
                // Move key to the front (most recently used)
                accessOrder.remove(key);
                accessOrder.addFirst(key);
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            if (cache.containsKey(key)) {
                cache.put(key, value); // Update the value
                accessOrder.remove(key);
                accessOrder.addFirst(key); // Update access order
            } else {
                if (cache.size() >= capacity) {
                    K lruKey = accessOrder.pollLast(); // Remove least recently used
                    if (lruKey != null) {
                        cache.remove(lruKey);
                    }
                }
                cache.put(key, value); // Add new key-value pair
                accessOrder.addFirst(key); // Mark as most recently used
            }
        } finally {
            lock.unlock();
        }
    }
}
