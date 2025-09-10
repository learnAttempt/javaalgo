package threading.ScheduledExecutor;

import java.util.concurrent.TimeUnit;

public class MiniSchedulerDemo {
    public static void main(String[] args) throws InterruptedException {
        MiniScheduler scheduler = new MiniScheduler(2);

        scheduler.schedule(() -> System.out.println("One-shot after 1s"), 1, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(
                () -> System.out.println("Fixed rate " + System.currentTimeMillis()),
                500, 1000, TimeUnit.MILLISECONDS);

        scheduler.scheduleWithFixedDelay(
                () -> {
                    System.out.println("Fixed delay " + System.currentTimeMillis());
                    try { Thread.sleep(700); } catch (InterruptedException ignored) {}
                },
                500, 1000, TimeUnit.MILLISECONDS);

        Thread.sleep(5000);
        scheduler.shutdown();
    }
}

