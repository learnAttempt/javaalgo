package threading.lrucache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Approximate (lock-free-ish) LRU-style cache:
 * - Uses ConcurrentHashMap for storage.
 * - Each node has a monotonic accessSequence (AtomicLong from cache).
 * - On get/put we update the node.accessSeq to a new increasing number.
 * - Eviction (when size > capacity) scans / samples the map and removes the entries
 *   with smallest accessSeq values (approximate LRU).
 *
 * Tradeoffs:
 * - No global lock => high concurrency.
 * - Eviction is approximate and may be more expensive than a perfect O(1) LRU eviction.
 * - map.remove(key, node) ensures we don't remove a node that's been updated concurrently.
 */
public class ApproximateLRUCache<K, V> {
    private final int capacity;
    private final ConcurrentHashMap<K, Node<V>> map;
    private final AtomicLong accessCounter = new AtomicLong(1);

    // how many entries to sample when evicting; tune for workload
    private final int evictionSampleSize;

    private static class Node<V> {
        final V value;
        // monotonic sequence number indicating last access (higher = more recent)
        volatile long accessSeq;

        Node(V value, long seq) {
            this.value = value;
            this.accessSeq = seq;
        }

        void touch(long seq) {
            // a plain volatile write; race is ok because seq monotonic increases
            this.accessSeq = seq;
        }
    }

    public ApproximateLRUCache(int capacity) {
        this(capacity, Math.max(64, capacity / 4)); // default sample size heuristic
    }

    public ApproximateLRUCache(int capacity, int evictionSampleSize) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity > 0");
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>(Math.min(16, capacity));
        this.evictionSampleSize = Math.max(16, evictionSampleSize);
    }

    /**
     * Get value for key. If present, update access sequence to mark it as recently used.
     */
    public V get(K key) {
        Node<V> n = map.get(key);
        if (n == null) return null;
        long seq = accessCounter.incrementAndGet();
        n.touch(seq); // mark recent (racy but fine)
        return n.value;
    }

    /**
     * Put key->value. If key exists replace node (atomically) with a new node that has newer seq.
     * After insertion we may trigger eviction if size exceeds capacity.
     */
    public void put(K key, V value) {
        long seq = accessCounter.incrementAndGet();
        // Replace existing node in a loop to update accessSeq atomically via new Node:
        map.compute(key, (k, old) -> {
            if (old == null) return new Node<>(value, seq);
            // if you want to keep object identity (less GC), we could update old.value if it's mutable;
            // here we create a fresh Node for simplicity and correctness.
            return new Node<>(value, seq);
        });

        // opportunistic eviction if we went over capacity
        if (map.size() > capacity) {
            evictIfNeeded();
        }
    }

    public int size() {
        return map.size();
    }

    /**
     * Evict entries until size <= capacity.
     * Strategy: sample up to evictionSampleSize entries (or all if fewer),
     * pick the smallest accessSeq entries and attempt to remove them using compare-remove.
     */
    private void evictIfNeeded() {
        // Quick check - may be stale but good to avoid unnecessary work
        if (map.size() <= capacity) return;

        // Collect sample keys and their seq numbers.
        // To avoid iterating whole map in huge caches, sample up to evictionSampleSize entries
        List<Map.Entry<K, Node<V>>> sample = new ArrayList<>(evictionSampleSize);
        Iterator<Map.Entry<K, Node<V>>> it = map.entrySet().iterator();
        int taken = 0;
        while (it.hasNext() && taken < evictionSampleSize) {
            Map.Entry<K, Node<V>> e = it.next();
            sample.add(e);
            taken++;
        }

        if (sample.isEmpty()) return;

        // Number to remove (optimistic)
        int toRemove = Math.max(1, map.size() - capacity);

        // Keep 'toRemove' entries with smallest accessSeq (least recent in sample)
        sample.sort(Comparator.comparingLong(e -> e.getValue().accessSeq));

        int removed = 0;
        for (int i = 0; i < sample.size() && removed < toRemove; i++) {
            K key = sample.get(i).getKey();
            Node<V> node = sample.get(i).getValue();
            // remove only if same node still in map (safe compare-remove)
            boolean didRemove = map.remove(key, node);
            if (didRemove) removed++;
        }

        // If we still didn't make enough room, we can repeat with a larger scan or full scan.
        // Keep it simple: do a second pass scanning more (but bounded) to avoid long pauses.
        if (map.size() > capacity && removed < toRemove) {
            // second chance: scan more (this is more expensive, but rare)
            List<Map.Entry<K, Node<V>>> fullScan = new ArrayList<>(Math.min(map.size(), evictionSampleSize * 4));
            Iterator<Map.Entry<K, Node<V>>> it2 = map.entrySet().iterator();
            int cap = evictionSampleSize * 4;
            int t = 0;
            while (it2.hasNext() && t < cap) {
                fullScan.add(it2.next());
                t++;
            }
            fullScan.sort(Comparator.comparingLong(e -> e.getValue().accessSeq));
            for (int i = 0; i < fullScan.size() && map.size() > capacity; i++) {
                Map.Entry<K, Node<V>> e = fullScan.get(i);
                map.remove(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * For debugging/test purposes: returns a snapshot of current keys and their accessSeqs.
     */
    public Map<K, Long> snapshotAccessSeqs() {
        Map<K, Long> snap = new HashMap<>();
        for (Map.Entry<K, Node<V>> e : map.entrySet()) {
            snap.put(e.getKey(), e.getValue().accessSeq);
        }
        return snap;
    }

    // ------------------------
    // Simple multithreaded test
    // ------------------------
    public static void main(String[] args) throws InterruptedException {
        final int CAP = 1000;
        final ApproximateLRUCache<Integer, Integer> cache = new ApproximateLRUCache<>(CAP, 256);

        final int THREADS = 32;
        final int OPS_PER_THREAD = 100_000;
        ExecutorService ex = Executors.newFixedThreadPool(THREADS);
        CountDownLatch done = new CountDownLatch(THREADS);

        final AtomicLong hits = new AtomicLong();
        final AtomicLong misses = new AtomicLong();

        for (int t = 0; t < THREADS; t++) {
            ex.submit(() -> {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                for (int i = 0; i < OPS_PER_THREAD; i++) {
                    int key = rnd.nextInt(0, 2000);
                    if (rnd.nextDouble() < 0.6) { // more reads
                        Integer v = cache.get(key);
                        if (v != null) hits.incrementAndGet();
                        else misses.incrementAndGet();
                    } else {
                        cache.put(key, key);
                    }
                }
                done.countDown();
            });
        }

        done.await();
        ex.shutdown();

        System.out.println("Done. size=" + cache.size() +
                " hits=" + hits.get() + " misses=" + misses.get());

        // Print some oldest items (approx) by snapshot
        Map<Integer, Long> snap = cache.snapshotAccessSeqs();
        List<Map.Entry<Integer, Long>> entries = new ArrayList<>(snap.entrySet());
        entries.sort(Comparator.comparingLong(Map.Entry::getValue));
        System.out.println("Approx least-recently-used sample (key:seq) -> first 10:");
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            System.out.println(entries.get(i).getKey() + ":" + entries.get(i).getValue());
        }
    }
}

