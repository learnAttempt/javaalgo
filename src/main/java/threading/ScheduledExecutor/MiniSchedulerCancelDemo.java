package threading.ScheduledExecutor;

import java.util.concurrent.*;

public class MiniSchedulerCancelDemo {
    public static void main(String[] args) throws InterruptedException {
        MiniScheduler scheduler = new MiniScheduler(2);

        ScheduledFuture<?> repeating = scheduler.scheduleAtFixedRate(
                () -> System.out.println("Repeating task at " + System.currentTimeMillis()),
                0, 1, TimeUnit.SECONDS);

        // Cancel after 3.5 seconds
        scheduler.schedule(() -> {
            System.out.println("Cancelling repeating task...");
            repeating.cancel(false);
        }, 3500, TimeUnit.MILLISECONDS);

        Thread.sleep(6000);
        scheduler.shutdown();
    }
}

