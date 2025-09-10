package threading.ScheduledExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Scheduler {

    private  final DelayQueue<ScheduledTask> queue= new DelayQueue<>();
    private  final Thread dispatcher;
    private final ExecutorService workers;
    private final AtomicBoolean running=new AtomicBoolean(true);

    Scheduler(int numThreads){
        workers=Executors.newFixedThreadPool(numThreads);
        dispatcher=new Thread(this::dispatchLoop,"Dispatcher");
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    private void  dispatchLoop(){
        try {
            while (running.get()){
                ScheduledTask task=queue.take();
                if(task.isCancelled()) continue;

                workers.submit(()->{
                    try {
                        task.command.run();
                    }finally {

                        if(task.isPeriodic()){
                            task.reschedule();
                            queue.put(task);
                        }
                    }
                });
            }
        } catch (InterruptedException _) {

        }
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay,TimeUnit unit){
        ScheduledTask task=new ScheduledTask(command,unit.toNanos(delay),0,false);
        queue.put(task);
        return task;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,long period,TimeUnit unit){
        ScheduledTask task=new ScheduledTask(command,unit.toNanos(initialDelay),unit.toNanos(period),true);
        queue.put(task);
        return task;
    }

    public ScheduledFuture<?>  scheduleWithFixedDelay(Runnable command, long initialDelay,long delay,TimeUnit unit){
        ScheduledTask task=new ScheduledTask(command,unit.toNanos(initialDelay),unit.toNanos(delay),false);
        queue.put(task);
        return task;
    }
    public void shutDown(){
        running.compareAndSet(true,false);
        dispatcher.interrupt();
        workers.shutdown();
    }


    private  class ScheduledTask implements RunnableScheduledFuture<Void> {

        final Runnable command;
       final long period;
      final boolean fixedRate;
      volatile long nextRun;
      final AtomicBoolean cancelled=new AtomicBoolean(false);

        public ScheduledTask(Runnable command, long initialDelay,long period, boolean fixedRate) {
            this.command = command;
            this.period = period;
            this.fixedRate = fixedRate;
            this.nextRun=System.nanoTime()+initialDelay;
        }

        @Override
        public boolean isPeriodic() {
            return period>0;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(nextRun-System.nanoTime(),TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.nextRun,((ScheduledTask)o).nextRun);
        }

        @Override
        public void run() {
                command.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result=cancelled.compareAndSet(false,true);
            if(result){
                queue.remove(this);
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return cancelled.get();
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }

        void reschedule(){
            if(fixedRate){
                nextRun+=period;
            }
            else{
                nextRun=System.nanoTime()+period;
            }
        }
    }
}
