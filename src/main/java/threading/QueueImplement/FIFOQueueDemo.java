package threading.QueueImplement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FIFOQueueDemo {
    public static void main(String[] args) {
        BoundedFIFOQueue<Integer> queue = new BoundedFIFOQueue<>(5);
        ExecutorService pool = Executors.newCachedThreadPool();

        // Producer
        pool.submit(() -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    queue.put(i);
                    System.out.println("Produced " + i);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer
        pool.submit(() -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    int val = queue.take();
                    System.out.println("Consumed " + val);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        pool.shutdown();
    }
}
