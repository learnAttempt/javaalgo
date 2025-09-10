package threading;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PrintOrder {


}

class Foo{
    boolean firstDone=false;
    boolean secondDone=false;

    Lock lock=new ReentrantLock();
    Condition firstFinished=lock.newCondition();
    Condition secondFinished=lock.newCondition();

    public void  first(){
        lock.lock();
        try {


            firstDone = true;
            System.out.println("first");
            firstFinished.signal();
        }finally {
            lock.unlock();
        }

    }
    public void second() throws InterruptedException {
        lock.lock();
        try {
            while (!firstDone)
                firstFinished.await();
            secondDone = true;
            System.out.println("second");
            secondFinished.signal();
        }
        finally{
            lock.unlock();
        }
    }
    public void third() throws InterruptedException{
        lock.lock();
        try {
            while (!secondDone)
                secondFinished.await();

            System.out.println("third");

        }
        finally{
            lock.unlock();
        }
    }
}

class DemoPrintOrder{
    public static void main(String[] args) throws InterruptedException {
        final Foo p=new Foo();
        Thread t1=new Thread(new Runnable() {
            @Override
            public void run() {
                p.first();
            }
        });

        Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.second();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t3=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.third();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t1.start();
        Thread.sleep(4000);
        t3.start();
        Thread.sleep(4000);
        t2.start();
        t1.join();
        t2.join();
        t3.join();
    }

}