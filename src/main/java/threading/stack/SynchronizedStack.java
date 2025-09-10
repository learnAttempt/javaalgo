package threading.stack;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;

class SynchronizedStack<T> {

    StackNode<T> top;

    public synchronized void push(T item) {

        if (top == null) {
            top = new StackNode<>(item);
        } else {
            StackNode<T> oldHead = top;
            top = new StackNode<>(item);
            top.setNext(oldHead);
        }
    }

    public synchronized T pop() {

        if (top == null) {
            return null;
        }

        StackNode<T> oldHead = top;
        top = top.getNext();
        return oldHead.getItem();
    }
}



class Demonstration {

    public static void main( String args[] ) throws Exception {

        SynchronizedStack<Integer> stackOfInts = new SynchronizedStack<>();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        int numThreads = 2;
        CyclicBarrier barrier = new CyclicBarrier(numThreads);

        Integer testValue = Integer.valueOf(51);;

        try {
            for (int i = 0; i < numThreads; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 10000; i++) {
                            stackOfInts.push(testValue);
                        }

                        try {
                            barrier.await();
                        } catch (InterruptedException | BrokenBarrierException ex) {
                            System.out.println("ignoring exception");
                            //ignore both exceptions
                        }

                        for (int i = 0; i < 10000; i++) {
                            stackOfInts.pop();
                        }
                    }
                });
            }
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        }
    }
}
