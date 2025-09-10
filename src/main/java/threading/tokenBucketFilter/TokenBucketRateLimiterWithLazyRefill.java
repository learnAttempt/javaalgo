package threading.tokenBucketFilter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

/**
 * Thread-safe Token Bucket rate limiter.
 *
 * - capacity: maximum tokens in bucket (burst size)
 * - refillRatePerSecond: tokens added per second
 *
 * Methods:
 *  - boolean tryConsume(int tokens)          // immediate, non-blocking
 *  - boolean tryConsume(int tokens, long timeout, TimeUnit unit) // blocking with timeout
 *  - void consume(int tokens) throws InterruptedException     // block until tokens available
 */
public class TokenBucketRateLimiterWithLazyRefill {
    private final double capacity;
    private final double refillTokensPerNano; // tokens per nanosecond
    private double availableTokens;
    private long lastRefillTimeNanos;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition waitCondition = lock.newCondition();

    public TokenBucketRateLimiterWithLazyRefill(double capacity, double refillRatePerSecond) {
        if (capacity <= 0 || refillRatePerSecond < 0) {
            throw new IllegalArgumentException("capacity > 0 and refillRatePerSecond >= 0 required");
        }
        this.capacity = capacity;
        this.refillTokensPerNano = refillRatePerSecond / 1_000_000_000.0;
        this.availableTokens = capacity; // start full (common policy)
        this.lastRefillTimeNanos = System.nanoTime();
    }

    // Refill tokens based on elapsed time. Must be called under lock.
    private void refill() {
        long now = System.nanoTime();
        if (now <= lastRefillTimeNanos) {
            return;
        }
        long elapsed = now - lastRefillTimeNanos;
        double tokensToAdd = elapsed * refillTokensPerNano;
        if (tokensToAdd > 0) {
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            lastRefillTimeNanos = now;
        }
    }

    /**
     * Try to consume tokens immediately. Returns true if successful.
     */
    public boolean tryConsume(int tokens) {
        if (tokens <= 0) throw new IllegalArgumentException("tokens must be > 0");
        lock.lock();
        try {
            refill();
            if (availableTokens >= tokens) {
                availableTokens -= tokens;
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Try to consume tokens, waiting up to timeout if necessary.
     * Returns true if tokens were consumed, false on timeout.
     */
    public boolean tryConsume(int tokens, long timeout, TimeUnit unit) throws InterruptedException {
        if (tokens <= 0) throw new IllegalArgumentException("tokens must be > 0");
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            for (;;) {
                refill();
                if (availableTokens >= tokens) {
                    availableTokens -= tokens;
                    return true;
                }
                long now = System.nanoTime();
                long remaining = deadline - now;
                if (remaining <= 0) {
                    return false; // timed out
                }
                // Estimate how long until enough tokens will be available.
                double tokensNeeded = tokens - availableTokens;
                long nanosToWait;
                if (refillTokensPerNano > 0) {
                    nanosToWait = (long)Math.ceil(tokensNeeded / refillTokensPerNano);
                } else {
                    // refill rate is zero -> will never get tokens
                    return false;
                }
                // Wait at most min(remaining, nanosToWait). We'll loop and refill on wake-up.
                long waitNanos = Math.min(remaining, nanosToWait);
                if (waitNanos <= 0) waitNanos = 1;
                waitCondition.awaitNanos(waitNanos);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Blocking consume - waits until tokens are available.
     */
    public void consume(int tokens) throws InterruptedException {
        // Wait with a very large timeout - implementation loops until success.
        boolean ok = tryConsume(tokens, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        if (!ok) {
            // In practice this won't return false because we used a huge timeout,
            // but keep this check for completeness.
            throw new IllegalStateException("Failed to consume tokens (unexpected)");
        }
    }

    /**
     * Get snapshot of available tokens (for monitoring).
     */
    public double getAvailableTokens() {
        lock.lock();
        try {
            refill();
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Convenience: single-token tryConsume.
     */
    public boolean tryConsume() {
        return tryConsume(1);
    }

    /**
     * Signal waiting threads that something changed. Not strictly necessary because
     * waiting threads use timed waits and do lazy refill based on system time.
     * But calling this can reduce latency if tokens were added externally.
     */
    public void notifyWaiters() {
        lock.lock();
        try {
            waitCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // --- Example usage & simple concurrency test ---
    public static void main(String[] args) throws InterruptedException {
        final TokenBucketRateLimiterWithLazyRefill limiter = new TokenBucketRateLimiterWithLazyRefill(10, 5.0); // capacity 10 tokens, 5 tokens/sec
        final int threads = 8;
        final ExecutorService ex = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger passed = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        // Each thread will try to obtain 3 tokens repeatedly for ~5 seconds
        long stopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        for (int t = 0; t < threads; t++) {
            ex.submit(() -> {
                try {
                    start.await();
                    while (System.nanoTime() < stopTime) {
                        // block upto 1 second to get 3 tokens
                        boolean got = limiter.tryConsume(3, 1, TimeUnit.SECONDS);
                        if (got) {
                            passed.incrementAndGet();
                            // simulate work
                            Thread.sleep(100);
                        } else {
                            failed.incrementAndGet();
                            // small backoff
                            Thread.sleep(50);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        ex.shutdown();
        ex.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("passed=" + passed.get() + ", failed=" + failed.get());
        System.out.printf("tokens left (snapshot) = %.3f%n", limiter.getAvailableTokens());
    }
}

