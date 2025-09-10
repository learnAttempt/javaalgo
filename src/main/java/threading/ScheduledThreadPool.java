package threading;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPool {
    public static void main(String[] args){
        ScheduledThreadPool sht=new ScheduledThreadPool();
        sht.createPool();
    }

    public void  createPool(){
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        for (int i = 10; i >= 0; i--) {
            scheduledExecutorService.schedule(new WorkerThread(i), 10 - i,
                    TimeUnit.SECONDS);
        }

        // remember to shutdown the scheduler
        // so that it no longer accepts
        // any new tasks
        scheduledExecutorService.shutdown();


    }
}


class WorkerThread implements Runnable {


    private int num;

    public WorkerThread(int num){
        this.num=num;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName()+" Start. Command = "+num+" time "+  Calendar.getInstance().get(Calendar.SECOND));
        processCommand();
        System.out.println(Thread.currentThread().getName()+" End."+" time "+  Calendar.getInstance().get(Calendar.SECOND));
    }

    private void processCommand() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        return String.valueOf(num);
    }

}
