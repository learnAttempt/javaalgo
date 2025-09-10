package threading.lrucache;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-safe LRU cache backed by LinkedHashMap.
 * Simple and correct. Good for moderate concurrency.
 */
public class SynchronizedLRUCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> map;

    public SynchronizedLRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        // accessOrder = true -> LRU order
        this.map = new LinkedHashMap<K, V>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > SynchronizedLRUCache.this.capacity;
            }
        };
    }

    public synchronized V get(K key) {
        return map.get(key); // access-order will move it to recent
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized V remove(K key) {
        return map.remove(key);
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized void clear() {
        map.clear();
    }

    @Override
    public synchronized String toString() {
        return map.toString();
    }
}
