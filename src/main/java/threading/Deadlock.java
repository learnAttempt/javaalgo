package threading;

public class Deadlock {

    int counter =0;
    private final Object lock1 =new Object();
    final Object lock2 = new Object();
    Runnable r1= ()->{
        for(int i=0;i<100;i++){
            try {
                incrementCounter();
                System.out.println("Incrementing");
            } catch (InterruptedException e) {
              //  throw new RuntimeException(e);
            }

        }
    };
    Runnable r2= ()->{
        for(int i=0;i<100;i++){
            try {
                decrementCounter();
                System.out.println("Decrementing");
            } catch (InterruptedException e) {
              //  throw new RuntimeException(e);
            }

        }
    };

    void incrementCounter() throws InterruptedException {
        synchronized (lock1){
            System.out.println("Acquired lock1");
            Thread.sleep(100);
            synchronized (lock2){
                counter++;
            }

        }
    }
    void decrementCounter() throws InterruptedException {
        synchronized (lock2){
            System.out.println("Acquired lock2");
            Thread.sleep(100);
            synchronized (lock1){
                counter--;
            }

        }
    }
    public void runTest() throws InterruptedException {
        Thread t1= new Thread(r1);
        Thread t2= new Thread(r2);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

    }
}
