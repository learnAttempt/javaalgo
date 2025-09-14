package threading.ScheduledExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedScheduler {

    private final PriorityQueue<ScheduledTask> queue=new PriorityQueue<>();
    private final Lock lock=new ReentrantLock();
    private final Condition notFull=lock.newCondition();
    private final Condition notEmpty=lock.newCondition();
    private final AtomicBoolean running=new AtomicBoolean(true);
    private final ExecutorService executorService;
    private final List<Thread> threadpool =new ArrayList<>();
 //   private final Thread dispatcher;
    private final int capacity;
    BoundedScheduler(int poolSize, int capacity){
        executorService=Executors.newFixedThreadPool(poolSize);
        this.capacity = capacity;
        for(int i=0;i<poolSize;i++) {
            Thread dispatcher = new Thread(this::dispatchLoop, "Scheduler-Dispatcher");
            dispatcher.start();
            threadpool.add(dispatcher);
       // dispatcher=new Thread(this::dispatchLoop, "Scheduler-Dispatcher");
        }
    }

    private void dispatchLoop()  {
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
                    long delay = task.nextRun - now;
                    if (delay > 0) {
                        notEmpty.awaitNanos(delay);
                        continue;
                    }
                    task = queue.poll();
                    notFull.signalAll();
                } finally {
                    lock.unlock();
                }
                if (task != null && !task.isCancelled()){
                    task.runTask();
                }


            }

            }catch(InterruptedException e ){
            Thread.currentThread().interrupt();
        }
    }

    public void scheduleTask(ScheduledTask task){
        lock.lock();
        try{
            while(queue.size()>=capacity &&running.get()){
                notFull.await();
            }
            queue.offer(task);
            notEmpty.signalAll();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            lock.unlock();
        }
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(delay), 0, false);
        scheduleTask(task);
        return task;
    }


    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(initialDelay), unit.toNanos(period), true);
        scheduleTask(task);
        return task;
    }


    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(command, unit.toNanos(initialDelay), unit.toNanos(delay), false);

        scheduleTask(task);
        return task;
    }



    public void shutdown() {
        running.set(false);

        lock.lock();
        try {
            notEmpty.signalAll();
            notFull.signalAll();
           // dispatcher.interrupt();
        } finally {
            lock.unlock();

        }
        for (Thread t : threadpool) t.interrupt();
    }

     class ScheduledTask implements Runnable,ScheduledFuture<Void> {
        private final Runnable command;
        private final long period;
        private final boolean fixedRate;
        public volatile long nextRun;
        private final AtomicBoolean cancelled=new AtomicBoolean(false);
        private final Object lock=new Object();
        private boolean done=false;

        public ScheduledTask(Runnable command, long period,long initialDelay, boolean fixedRate) {
            this.command = command;
            this.period = period;
            this.fixedRate = fixedRate;
            this.nextRun=System.nanoTime()+initialDelay;
        }

        private void runTask(){
            if(isCancelled())
                return;
            try{
                command.run();
            }finally {
                if(!isPeriodic())
                    synchronized (lock){
                        done=true;
                        lock.notifyAll();
                    }
                else{
                    if(!isCancelled()) {
                        if (fixedRate)
                            nextRun += period;
                        else
                            nextRun = System.nanoTime() + period;
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
        public long getDelay(TimeUnit unit) {
            return unit.convert(nextRun-System.nanoTime(),TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.nextRun,((ScheduledTask) o).nextRun);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
          boolean result=cancelled.compareAndSet(false,true);
          if(result){
              synchronized (lock){
                  done=true;
                  lock.notifyAll();
              }
          }
        return result;
        }

        @Override
        public boolean isCancelled() {
           return cancelled.get();
        }

        @Override
        public boolean isDone() {
            synchronized (lock)
            {
                return done || cancelled.get();
            }
        }

        public boolean isPeriodic(){
            return period>0;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            synchronized (lock){
                while (!done && !cancelled.get())
                    lock.wait();
            }
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            synchronized (lock) {
                while (!done && !cancelled.get()) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) throw new TimeoutException();
                    TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                }
            }
            return null;
        }


    }
}


 class SchedulerTest {
    public static void main(String[] args) throws Exception {
        // pool with 2 workers, queue capacity = 3
        BoundedScheduler scheduler = new BoundedScheduler(2, 3);

        System.out.println("=== Test 1: One-shot ===");
        ScheduledFuture<?> f1 = scheduler.schedule(
                () -> System.out.println("[One-shot] Executed by " + Thread.currentThread().getName()),
                0, TimeUnit.SECONDS
        );
        f1.get(); // should block until execution is done
        System.out.println("[One-shot] Done!");

        System.out.println("\n=== Test 2: Fixed Rate (cancel after 5s) ===");
        ScheduledFuture<?> f2 = scheduler.scheduleAtFixedRate(
                () -> System.out.println("[FixedRate] " + System.currentTimeMillis() +
                        " by " + Thread.currentThread().getName()),
                500, 1000, TimeUnit.MILLISECONDS
        );

        Thread.sleep(5000);
        System.out.println("[FixedRate] Cancelling...");
        f2.cancel(false);
        System.out.println("[FixedRate] Cancelled!\n");

        System.out.println("=== Test 3: Fixed Delay (3 runs only) ===");
        ScheduledFuture<?> f3 = scheduler.scheduleWithFixedDelay(
                new Runnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        count++;
                        System.out.println("[FixedDelay] Run " + count +
                                " at " + System.currentTimeMillis() +
                                " by " + Thread.currentThread().getName());

                    }
                },
                500, 1000, TimeUnit.MILLISECONDS
        );

        Thread.sleep(5000);

        System.out.println("\n=== Test 4: Bounded Queue Blocking ===");
        // Submit more than capacity to check blocking
        for (int i = 0; i < 5; i++) {
            final int id = i;
            scheduler.schedule(() -> {
                System.out.println("[QueueTest] Task " + id +
                        " executed by " + Thread.currentThread().getName());
            }, 200, TimeUnit.MILLISECONDS);
            System.out.println("[QueueTest] Submitted task " + id);
        }

        Thread.sleep(3000);

        System.out.println("\nShutting down...");
        scheduler.shutdown();
    }
}
