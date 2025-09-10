package threading.tokenBucketFilter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LeakyBucket {
    private final int capacity;
    private int water;
    private final int leakRate;
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    LeakyBucket(int capacity, int leakRate) {
        this.leakRate = leakRate;
        this.capacity = capacity;
        this.water = 0;
        scheduledExecutorService.scheduleAtFixedRate(this::leak, 0, 1, TimeUnit.SECONDS);
    }

    private void leak() {
        lock.lock();
        try {
            if (water > 0) {
                int leaked = Math.min(water, leakRate);
                water -= leaked;
                System.out.println("Leaked " + leaked + " tokens, remaining: " + water);
            }
        } finally {
            lock.unlock();
        }

    }

    public boolean tryConsume() {
        lock.lock();
        try {
            if (water < capacity) {
                water++;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        scheduledExecutorService.shutdownNow();
    }

    public static void main(String[] args) throws InterruptedException {
        LeakyBucket leakyBucket = new LeakyBucket(10, 2);
        ExecutorService workers = Executors.newFixedThreadPool(5);
        Runnable task = () -> {
            for (int i = 0; i < 5; i++) {
                boolean accepted = leakyBucket.tryConsume();
                System.out.println(Thread.currentThread().getName() + " request " + (accepted ? "ACCEPTED" : "DROPPED"));
                try {
                    Thread.sleep(200); // simulate some delay
                } catch (InterruptedException ignored) {
                }
            }
        };
        for (int i = 0; i < 5; i++)
            workers.submit(task);

        workers.shutdown();
        try {
            workers.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        leakyBucket.shutdown();
    }
}
