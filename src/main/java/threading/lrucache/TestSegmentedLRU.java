package threading.lrucache;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class TestSegmentedLRU {
    public static void main(String[] args) throws InterruptedException {
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

/*

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
                    " hits=" + hits.get() + " misses=" + misses.get());*/

    }

}

