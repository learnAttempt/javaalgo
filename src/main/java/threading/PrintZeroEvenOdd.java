package threading;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PrintZeroEvenOdd {
    private final int n;
    private int curr;
    private  boolean printZero;
    private final Lock lock=new ReentrantLock();
    Condition zeroC=lock.newCondition();
    Condition oddC=lock.newCondition();
    Condition evenC=lock.newCondition();


    public PrintZeroEvenOdd(int n) {
        this.n = n;
        curr=1;
        printZero=true;
    }
    public void zero() throws InterruptedException {
        lock.lock();
        while(curr<=n) {
                while (!printZero)
                    zeroC.await();
                if (curr <= n) {
                    System.out.print("0");
            }
            printZero = false;
            if (curr % 2 != 0)
                oddC.signal();
            else
                evenC.signal();
    }

            lock.unlock();
    }

    public void odd() throws InterruptedException {
        lock.lock();

       while(curr<=n) {


               while (curr % 2 == 0 || printZero)
                   oddC.await();
               System.out.print(curr);
               curr++;
               printZero = true;
               zeroC.signal();

           }
        lock.unlock();


    }

    public void even() throws InterruptedException {
        lock.lock();

        while(curr<=n) {

                while (curr % 2 != 0 || printZero)
                    evenC.await();
                System.out.print(curr);
                curr++;
                printZero = true;
                zeroC.signal();

            }

        lock.unlock();

    }
}


class ZeroEvenOdd {
    private int n;
    private int currentNumberToBePrinted;
    private int currentNumber;

    private Lock lock = new ReentrantLock();
    private Condition semZero = lock.newCondition();
    private Condition semEven = lock.newCondition();
    private Condition semOdd = lock.newCondition();

    public ZeroEvenOdd(int n) {
        this.n = n;
        this.currentNumber = 1;
        this.currentNumberToBePrinted = 0;
    }

    // printNumber.accept(x) outputs "x", where x is an integer.
    public void zero() throws InterruptedException {
        lock.lock();
        while (currentNumber <= n) {
            while (currentNumberToBePrinted != 0) {
                semZero.await();
            }

            if (currentNumber <= n) {
                System.out.print(0);
            }
            currentNumberToBePrinted = currentNumber;

            if (currentNumber %2 == 0) {
                semEven.signal();
            } else {
                semOdd.signal();
            }
        }
        lock.unlock();
    }

    public void even() throws InterruptedException {
        lock.lock();
        while (currentNumber <= n) {
            while (currentNumberToBePrinted == 0 || currentNumberToBePrinted%2 != 0) {
                semEven.await();
            }

            if (currentNumberToBePrinted <= n) {
                System.out.print(currentNumberToBePrinted);
            }
            this.currentNumber += 1;
            currentNumberToBePrinted = 0;


            semZero.signal();
        }
        lock.unlock();
    }

    public void odd() throws InterruptedException {
        lock.lock();
        while (currentNumber <= n) {
            while (currentNumberToBePrinted == 0 || currentNumberToBePrinted %2 == 0) {
                semOdd.await();
            }


            if (currentNumberToBePrinted <= n) {
                System.out.print(currentNumberToBePrinted);
            }
            this.currentNumber += 1;
            currentNumberToBePrinted = 0;


            semZero.signal();
        }
        lock.unlock();
    }
}

class DemoEvenOdd{
    public static void main(String [] args) throws InterruptedException{
        final PrintZeroEvenOdd p=new PrintZeroEvenOdd(10);
      //  final ZeroEvenOdd p=new ZeroEvenOdd(10);
        Thread t1=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.zero();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.even();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t3=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.odd();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        t1.start();
        t2.start();
       t3.start();
        t2.join();
     //  t3.join();

    }
}

