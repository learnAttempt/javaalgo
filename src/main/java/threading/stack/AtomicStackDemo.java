package threading.stack;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicStackDemo {
    public static void main(String[] args) throws InterruptedException {
        final TreiberStack<Integer> stack = new TreiberStack<>();
        final int producers = 4;
        final int consumers = 4;
        final int itemsPerProducer = 50;

        ExecutorService ex = Executors.newFixedThreadPool(producers + consumers);
        CountDownLatch done = new CountDownLatch(producers + consumers);
        AtomicInteger pushed = new AtomicInteger();
        AtomicInteger popped = new AtomicInteger();

        // producers
        for (int p = 0; p < producers; p++) {
            ex.submit(() -> {
                for (int i = 0; i < itemsPerProducer; i++) {
                    int v = pushed.incrementAndGet();
                    stack.push(v);
                }
                done.countDown();
            });
        }

        // consumers
        for (int c = 0; c < consumers; c++) {
            ex.submit(() -> {
                int localPopped = 0;
                while (popped.get() < producers * itemsPerProducer) {
                    Integer v = stack.pop();
                    if (v != null) {
                        popped.incrementAndGet();
                        localPopped++;
                    } else {
                        // small pause to avoid busy-waiting
                        Thread.yield();
                    }
                }
                // System.out.println("Consumer popped: " + localPopped);
                done.countDown();
            });
        }

        // Wait for threads to finish
        done.await();
        ex.shutdownNow();

        System.out.printf("Pushed: %d, Popped: %d, Stack empty: %b%n",
                pushed.get(), popped.get(), stack.isEmpty());
    }
}

