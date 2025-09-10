package threading.ScheduledExecutor;

import java.util.concurrent.TimeUnit;

public class SchedulerDemo {

    public static void main(String[] args) throws InterruptedException{
        Scheduler scheduler=new Scheduler(2);
        scheduler.schedule(()->System.out.println("run after 1 s"),1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> System.out.println("Fixed rate " + System.currentTimeMillis()),
                500, 1000, TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(()->{
            System.out.println("Fixed delay"+System.currentTimeMillis());
            try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        },500,1000,TimeUnit.MILLISECONDS);

        Thread.sleep(10000);
        scheduler.shutDown();
    }
}
