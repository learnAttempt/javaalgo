package threading;
//how to stop a thread
public class StopThread {
    public static void main(String[] args) throws InterruptedException {
        StopExample s = new StopExample();
        Thread t1 = new Thread(s);
        t1.start();
        try {
            Thread.sleep(10L * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        s.stop();
    }
}

 class StopExample implements Runnable {

    private boolean doStop = false;

    public synchronized void stop() {
        this.doStop = true;
    }

    private synchronized boolean keepRunning() {
        return !doStop;
    }

    @Override
    public void run() {
        while (keepRunning()) {
            System.out.println("Running");
            try {
                Thread.sleep(3L * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}