package threading.tokenBucketFilter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ScheduledTokenBucketRateLimiter {
    private final int capacity;
    private final int refillRatePerSecond;

    private int availableTokens;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition waitCondition = lock.newCondition();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ScheduledTokenBucketRateLimiter(int capacity, int refillRatePerSecond) {
        if (capacity <= 0 || refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("capacity > 0 and refillRatePerSecond > 0 required");
        }
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.availableTokens = capacity; // start full

        long periodNanos = 1_000_000_000L / refillRatePerSecond; // interval per token
        scheduler.scheduleAtFixedRate(this::addToken, periodNanos, periodNanos, TimeUnit.NANOSECONDS);
    }

    private void addToken() {
        lock.lock();
        try {
            if (availableTokens < capacity) {
                availableTokens++;
                waitCondition.signal(); // wake one waiting thread
            }
        } finally {
            lock.unlock();
        }
    }

    /** Try to consume tokens immediately (non-blocking). */
    public boolean tryConsume(int tokens) {
        if (tokens <= 0) throw new IllegalArgumentException("tokens > 0 required");
        lock.lock();
        try {
            if (availableTokens >= tokens) {
                availableTokens -= tokens;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /** Blocking consume: wait until tokens are available. */
    public void consume(int tokens) throws InterruptedException {
        if (tokens <= 0) throw new IllegalArgumentException("tokens > 0 required");
        lock.lock();
        try {
            while (availableTokens < tokens) {
                waitCondition.await();
            }
            availableTokens -= tokens;
        } finally {
            lock.unlock();
        }
    }

    /** Blocking consume with timeout. Returns false if timed out. */
    public boolean tryConsume(int tokens, long timeout, TimeUnit unit) throws InterruptedException {
        if (tokens <= 0) throw new IllegalArgumentException("tokens > 0 required");
        long nanosTimeout = unit.toNanos(timeout);
        lock.lock();
        try {
            while (availableTokens < tokens) {
                if (nanosTimeout <= 0) return false;
                nanosTimeout = waitCondition.awaitNanos(nanosTimeout);
            }
            availableTokens -= tokens;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Get snapshot of available tokens (for monitoring). */
    public int getAvailableTokens() {
        lock.lock();
        try {
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }

    /** Gracefully shutdown the background scheduler. */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // Demo
    public static void main(String[] args) throws InterruptedException {
        ScheduledTokenBucketRateLimiter limiter = new ScheduledTokenBucketRateLimiter(5, 2);
        ExecutorService pool = Executors.newFixedThreadPool(3);

        Runnable task = () -> {
            try {
                while (true) {
                    limiter.consume(1); // block until token available
                    System.out.println(Thread.currentThread().getName() + " got token at " + System.currentTimeMillis());
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < 3; i++) pool.submit(task);

        Thread.sleep(5000);
        limiter.shutdown();
        pool.shutdownNow();
    }
}
