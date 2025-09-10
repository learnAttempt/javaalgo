package threading;

import java.util.concurrent.Semaphore;

public class ReaderWriterProblem {
    static Semaphore mutex = new Semaphore(1, true);  // Controls access to readCount
    static Semaphore wrt = new Semaphore(1, true);   // Writer semaphore
    static int readCount = 0;                        // Number of active readers
    static class Reader implements Runnable {
        @Override
        public void run() {
            try {
                // Reader entry
                mutex.acquire();
                readCount++;
                if (readCount == 1) {
                    wrt.acquire(); // First reader locks writers
                }
                mutex.release();
                // Critical section
                System.out.println(Thread.currentThread().getName() + " is reading.");
                Thread.sleep(1000); // Simulate reading
                // Reader exit
                mutex.acquire();
                readCount--;
                if (readCount == 0) {
                    wrt.release(); // Last reader unlocks writers
                }
                mutex.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    static class Writer implements Runnable {
        @Override
        public void run() {
            try {
                wrt.acquire(); // Writer entry
                System.out.println(Thread.currentThread().getName() + " is writing.");
                Thread.sleep(1000); // Simulate writing
                wrt.release(); // Writer exit
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        Thread[] readers = new Thread[5];
        Thread[] writers = new Thread[2];
        for (int i = 0; i < 5; i++) {
            readers[i] = new Thread(new Reader(), "Reader-" + (i + 1));
        }
        for (int i = 0; i < 2; i++) {
            writers[i] = new Thread(new Writer(), "Writer-" + (i + 1));
        }
        for (Thread reader : readers) {
            reader.start();
        }
        for (Thread writer : writers) {
            writer.start();
        }
    }
}
