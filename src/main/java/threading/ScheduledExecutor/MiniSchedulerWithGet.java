package threading.ScheduledExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MiniSchedulerWithGet {
    private final DelayQueue<ScheduledTask<?>> queue = new DelayQueue<>();
    private final ExecutorService workers;
    private final Thread dispatcher;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public MiniSchedulerWithGet(int threads) {
        workers = Executors.newFixedThreadPool(threads);
        dispatcher = new Thread(this::dispatchLoop, "Scheduler-Dispatcher");
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    private void dispatchLoop() {
        try {
            while (running.get()) {
                ScheduledTask<?> task = queue.take();
                if (task.isCancelled()) continue;

                workers.submit(() -> {
                    try {
                        task.runTask();
                    } finally {
                        if (task.isPeriodic() && !task.isCancelled()) {
                            task.reschedule();
                            queue.put(task);
                        }
                    }
                });
            }
        } catch (InterruptedException ignored) {}
    }

    // ---- Public scheduling methods ----

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        ScheduledTask<V> task = new ScheduledTask<>(callable, unit.toNanos(delay), 0, false);
        queue.put(task);
        return task;
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(Executors.callable(command, null), delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ScheduledTask<?> task = new ScheduledTask<>(Executors.callable(command, null),
                unit.toNanos(initialDelay), unit.toNanos(period), true);
        queue.put(task);
        return task;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ScheduledTask<?> task = new ScheduledTask<>(Executors.callable(command, null),
                unit.toNanos(initialDelay), unit.toNanos(delay), false);
        queue.put(task);
        return task;
    }

    public void shutdown() {
        running.set(false);
        dispatcher.interrupt();
        workers.shutdownNow();
    }

    // ---- Internal ScheduledTask ----
    private static class ScheduledTask<V> implements RunnableScheduledFuture<V> {
        private final Callable<V> callable;
        private final long period;
        private final boolean fixedRate;
        private volatile long nextRun;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final CountDownLatch done = new CountDownLatch(1);
        private V result;
        private Exception exception;

        ScheduledTask(Callable<V> callable, long initialDelay, long period, boolean fixedRate) {
            this.callable = callable;
            this.period = period;
            this.fixedRate = fixedRate;
            this.nextRun = System.nanoTime() + initialDelay;
        }

        void runTask() {
            if (isPeriodic()) {
                try { callable.call(); } catch (Exception ignored) {}
                return; // periodic tasks never complete
            }
            try {
                result = callable.call();
            } catch (Exception e) {
                exception = e;
            } finally {
                done.countDown();
            }
        }

        @Override public long getDelay(TimeUnit unit) {
            return unit.convert(nextRun - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override public int compareTo(Delayed o) {
            return Long.compare(this.nextRun, ((ScheduledTask<?>) o).nextRun);
        }

        @Override public void run() { runTask(); }
        @Override public boolean isPeriodic() { return period > 0; }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = cancelled.compareAndSet(false, true);
            if (result) {
                done.countDown(); // unblock get()
            }
            return result;
        }

        @Override public boolean isCancelled() { return cancelled.get(); }
        @Override public boolean isDone() {
            return isCancelled() || (!isPeriodic() && done.getCount() == 0);
        }

        @Override public V get() throws InterruptedException, ExecutionException {
            done.await();
            if (isCancelled()) throw new CancellationException();
            if (exception != null) throw new ExecutionException(exception);
            return result;
        }

        @Override public V get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            if (!done.await(timeout, unit)) throw new TimeoutException();
            if (isCancelled()) throw new CancellationException();
            if (exception != null) throw new ExecutionException(exception);
            return result;
        }

        void reschedule() {
            if (fixedRate) {
                nextRun += period; // fixed-rate
            } else {
                nextRun = System.nanoTime() + period; // fixed-delay
            }
        }
    }
}

