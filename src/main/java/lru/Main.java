package lru;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(3);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Simulate concurrent access
        for (int i = 1; i <= 5; i++) {
            int threadId = i;
            executor.submit(() -> {
                String key = "key" + threadId;
                cache.put(key, "value" + threadId);
                System.out.println("Thread-" + threadId + " put " + key);

                // Access cache
                for (int j = 1; j <= 5; j++) {
                    String k = "key" + j;
                    String value = cache.get(k);
                    System.out.println("Thread-" + threadId + " got " + k + ": " + value);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
