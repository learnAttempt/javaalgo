package threading.ScheduledExecutor;

import java.util.concurrent.*;

public class MiniSchedulerGetDemo {
    public static void main(String[] args) throws Exception {
        MiniSchedulerWithGet scheduler = new MiniSchedulerWithGet(2);

        // One-shot with result
        ScheduledFuture<String> f1 = scheduler.schedule(() -> "Hello after 1s", 1, TimeUnit.SECONDS);
        System.out.println("Result: " + f1.get()); // waits and prints "Hello after 1s"

        // One-shot with exception
        ScheduledFuture<String> f2 = scheduler.schedule(() -> { throw new RuntimeException("Boom"); },
                1, TimeUnit.SECONDS);
        try {
            System.out.println(f2.get());
        } catch (ExecutionException e) {
            System.out.println("Caught exception: " + e.getCause());
        }

        // Periodic task (get never returns)
        ScheduledFuture<?> f3 = scheduler.scheduleAtFixedRate(
                () -> System.out.println("Tick at " + System.currentTimeMillis()),
                0, 500, TimeUnit.MILLISECONDS);

        Thread.sleep(2000);
        f3.cancel(false);
        System.out.println("Cancelled periodic task");

        scheduler.shutdown();
    }
}
