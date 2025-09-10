package threading.producerconsumer;

public class Demonstration {

    public static void main(String[] args) throws InterruptedException {
        final BlockingQueue<Integer> blockingQueue=new BlockingQueue<>(5);
        Thread t1=new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    for(int i=0;i<50;i++){
                        blockingQueue.enqueue(i);
                        System.out.println("enqueued " + i);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    for(int i=0;i<25;i++){

                        System.out.println("dequeued " + blockingQueue.dequeue());
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Thread t3 = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    for (int i = 0; i < 25; i++) {
                        System.out.println("Thread 3 dequeued: " + blockingQueue.dequeue());
                    }
                } catch (InterruptedException ie) {

                }
            }
        });
            t1.start();
            Thread.sleep(4000);
            t2.start();
            t2.join();
            t3.start();
            t1.join();
            t3.join();

    }
}
