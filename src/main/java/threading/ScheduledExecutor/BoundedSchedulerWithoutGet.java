package threading.ScheduledExecutor;

import java.util.concurrent.*;
        import java.util.*;
        import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;

public class BoundedSchedulerWithoutGet {
    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();
    private final List<Thread> workers = new ArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final int capacity;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public BoundedSchedulerWithoutGet(int poolSize, int capacity) {
        this.capacity = capacity;
        for (int i = 0; i < poolSize; i++) {
            Thread worker = new Thread(this::runWorker, "Scheduler-Worker-" + i);
            worker.start();
            workers.add(worker);
        }
    }

    private void runWorker() {
        try {
            while (running.get()) {
                ScheduledTask task;
                lock.lock();
                try {
                    while (queue.isEmpty() && running.get()) {
                        notEmpty.await();
                    }
                    if (!running.get()) return;

                    long now = System.nanoTime();
                    task = queue.peek();
                    long delay = task.time - now;

                    if (delay > 0) {
                        notEmpty.awaitNanos(delay);
                        continue;
                    }
                    task = queue.poll();
                    notFull.signal(); // free a slot for producers
                } finally {
                    lock.unlock();
                }

                if (task != null) {

                    task.runTask();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void scheduleTask(ScheduledTask task) {
        lock.lock();
        try {
            while (queue.size() >= capacity && running.get()) {
                notFull.await();
            }
            queue.add(task);
            notEmpty.signalAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    // -------- Scheduling API (simplified) --------
    public void schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(delay), 0, false);
        scheduleTask(task);
    }

    public void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(initialDelay), unit.toNanos(period), true);
        scheduleTask(task);
    }

    public void scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(initialDelay), unit.toNanos(delay), false);
        task.fixedDelayMode = true;
        scheduleTask(task);
    }

    // -------- ScheduledTask --------
    private class ScheduledTask implements Runnable, Comparable<ScheduledTask> {
        private final Runnable command;
        private final long period;
        private final boolean fixedRate;
        private boolean fixedDelayMode = false;
        private long time;

        ScheduledTask(Runnable command, long initialDelay, long period, boolean fixedRate) {
            this.command = command;
            this.time = System.nanoTime() + initialDelay;
            this.period = period;
            this.fixedRate = fixedRate;
        }

        void runTask() {
            try {
                command.run();
            } finally {
                if (period > 0) {
                    if (fixedDelayMode) {
                        time = System.nanoTime() + period; // next run after delay
                    } else if (fixedRate) {
                        time += period; // strict fixed-rate schedule
                    }
                    scheduleTask(this);
                }
            }
        }

        @Override
        public void run() {
            runTask();
        }

        @Override
        public int compareTo(ScheduledTask o) {
            return Long.compare(this.time, o.time);
        }
    }

    // -------- Lifecycle --------
    public void shutdown() {
        running.set(false);
        lock.lock();
        try {
            notEmpty.signalAll();
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
        for (Thread t : workers) t.interrupt();
    }
}

 class SimpleSchedulerTest {
    public static void main(String[] args) throws Exception {
        BoundedSchedulerWithoutGet scheduler = new BoundedSchedulerWithoutGet(2, 3);

        System.out.println("=== One-shot ===");
        scheduler.schedule(() ->
                        System.out.println("[One-shot] " + System.currentTimeMillis()),
                1, TimeUnit.SECONDS);

        System.out.println("=== Fixed Rate (5 runs) ===");
        scheduler.scheduleAtFixedRate(new Runnable() {
            int count = 0;
            @Override
            public void run() {
                System.out.println("[FixedRate] Run " + (++count) + " at " + System.currentTimeMillis());
             //   if (count >= 5) scheduler.shutdown(); // stop after 5 runs
            }
        }, 500, 1000, TimeUnit.MILLISECONDS);

    Thread.sleep(5000);

     scheduler.scheduleWithFixedDelay(new Runnable() {
         int count = 0;
         @Override
         public void run() {
             System.out.println("[FixedDelay] Run " + (++count) + " at " + System.currentTimeMillis());
             if (count >= 5) scheduler.shutdown(); // stop after 5 runs
         }
     }, 500, 1000, TimeUnit.MILLISECONDS);
 }
}

