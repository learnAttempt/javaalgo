package threading.lrucache;

public class TestSegmentedLRU {
    public static void main(String[] args) {
        SegmentedLRUCache<Integer, String> cache = new SegmentedLRUCache<>(6, 2);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        cache.put(4, "D");
        cache.put(5, "E");
        cache.put(6, "F");

        System.out.println("Size after 6 inserts: " + cache.size()); // 6

        cache.get(2); // mark 2 as MRU
        cache.put(7, "G"); // evicts LRU (depends on access order within segment)

        System.out.println("Get(2): " + cache.get(2)); // should still be present
        System.out.println("Get(1): " + cache.get(1)); // may be evicted
    }
}

