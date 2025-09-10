package threading.ScheduledExecutor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleScheduledExecutorService implements ScheduledExecutorService {
    private final DelayQueue<ScheduledTask> queue = new DelayQueue<>();
    private final ExecutorService workerPool;
    private final AtomicLong taskCounter = new AtomicLong();
    private volatile boolean shutdown = false;

    public SimpleScheduledExecutorService(int threads) {
        this.workerPool = Executors.newFixedThreadPool(threads);
        Thread dispatcher = new Thread(this::dispatchLoop, "Scheduler-Dispatcher");
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    private void dispatchLoop() {
        try {
            while (!shutdown) {
                ScheduledTask task = queue.take();
                if (task.isCancelled()) continue;

                workerPool.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        if (task.isRepeating()) {
                            task.reschedule();
                            queue.put(task);
                        }
                    }
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // --------- Scheduling methods ---------
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(delay), 0, false);
        queue.add(task);
        return task;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return null;
    }


    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (period <= 0) throw new IllegalArgumentException("Period must be > 0");
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(initialDelay), unit.toNanos(period), true);
        queue.add(task);
        return task;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (delay <= 0) throw new IllegalArgumentException("Delay must be > 0");
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(initialDelay), unit.toNanos(delay), false);
        queue.add(task);
        return task;
    }

    // --------- Boilerplate ExecutorService methods ---------
    @Override public void shutdown() { shutdown = true; workerPool.shutdown(); }
    @Override public java.util.List<Runnable> shutdownNow() { shutdown = true; return workerPool.shutdownNow(); }
    @Override public boolean isShutdown() { return shutdown; }
    @Override public boolean isTerminated() { return workerPool.isTerminated(); }
    @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return workerPool.awaitTermination(timeout, unit);
    }
    @Override public <T> Future<T> submit(Callable<T> task) { return workerPool.submit(task); }
    @Override public <T> Future<T> submit(Runnable task, T result) { return workerPool.submit(task, result); }
    @Override public Future<?> submit(Runnable task) { return workerPool.submit(task); }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return workerPool.invokeAll(tasks);
    }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return workerPool.invokeAll(tasks, timeout, unit);
    }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return workerPool.invokeAny(tasks);
    }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return workerPool.invokeAny(tasks, timeout, unit);
    }
    @Override public void execute(Runnable command) { workerPool.execute(command); }

    // --------- ScheduledTask (internal) ---------
    private class ScheduledTask implements RunnableScheduledFuture<Object> {
        private final Runnable command;
        private final long period;
        private final boolean fixedRate;
        private volatile long nextRun;
        private volatile boolean cancelled = false;

        ScheduledTask(Runnable command, long initialDelay, long period, boolean fixedRate) {
            this.command = command;
            this.period = period;
            this.fixedRate = fixedRate;
            this.nextRun = System.nanoTime() + initialDelay;
        }

        @Override public long getDelay(TimeUnit unit) {
            return unit.convert(nextRun - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override public int compareTo(Delayed o) {
            return Long.compare(this.nextRun, ((ScheduledTask) o).nextRun);
        }

        @Override public void run() { command.run(); }

        @Override public boolean cancel(boolean mayInterruptIfRunning) { cancelled = true; return true; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public boolean isDone() { return cancelled; }
        @Override public Object get() throws InterruptedException, ExecutionException { return null; }
        @Override public Object get(long timeout, TimeUnit unit) { return null; }

        boolean isRepeating() { return period > 0; }
        @Override public boolean isPeriodic(){return period>0;}
        void reschedule() {
            if (cancelled) return;
            if (fixedRate) {
                nextRun += period; // fixed rate
            } else {
                nextRun = System.nanoTime() + period; // fixed delay
            }
        }
    }
}
