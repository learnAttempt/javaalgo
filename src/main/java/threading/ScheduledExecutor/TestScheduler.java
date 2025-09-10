package threading.ScheduledExecutor;

import java.util.concurrent.TimeUnit;

public class TestScheduler {
    public static void main(String[] args) throws InterruptedException {
        SimpleScheduledExecutorService scheduler = new SimpleScheduledExecutorService(2);

        // One-shot
        scheduler.schedule(() -> System.out.println("One-shot after 1s"), 1, TimeUnit.SECONDS);

        // Fixed rate
        scheduler.scheduleAtFixedRate(() -> System.out.println("Fixed rate at " + System.currentTimeMillis()),
                500, 1000, TimeUnit.MILLISECONDS);

        // Fixed delay
        scheduler.scheduleWithFixedDelay(() -> {
            System.out.println("Fixed delay at " + System.currentTimeMillis());
            try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        }, 500, 1000, TimeUnit.MILLISECONDS);

        Thread.sleep(5000);
        scheduler.shutdown();
    }
}

