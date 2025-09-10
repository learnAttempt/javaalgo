package threading.QueueImplement;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Demo {
    public static void main(String[] args) throws InterruptedException {
        CircularBlockingQueue<Integer> q = new CircularBlockingQueue<>(5);
        ExecutorService pool = Executors.newCachedThreadPool();

        AtomicInteger seq = new AtomicInteger();

        // 2 producers
        for (int p = 0; p < 2; p++) {
            final int id = p;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < 20; i++) {
                        int v = seq.incrementAndGet();
                        q.put(v);
                        System.out.println("P" + id + " -> " + v);
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            });
        }

        // 3 consumers
        for (int c = 0; c < 3; c++) {
            final int id = c;
            pool.submit(() -> {
                try {
                    for (;;) {
                        Integer v = q.take();
                        System.out.println("   C" + id + " <- " + v);
                        Thread.sleep(30);
                    }
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            });
        }

        // Let it run briefly then shutdown
        Thread.sleep(1500);
        pool.shutdownNow();
        pool.awaitTermination(2, TimeUnit.SECONDS);
    }
}
