package threading.stack;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.*;

class NonblockingStack<T> {

    private final AtomicInteger count = new AtomicInteger(0);
    private final AtomicReference<StackNode<T>> top = new AtomicReference<>();

    public int size() {
        return count.get();
    }

    public void push(T newItem) {
        StackNode<T> oldTop;
        StackNode<T> newTop;
        do {
            oldTop = top.get();
            newTop = new StackNode<>(newItem);
            newTop.setNext(oldTop);
        } while (!top.compareAndSet(oldTop, newTop));

        count.incrementAndGet();
    }

    public T pop() {
        StackNode<T> oldTop;
        StackNode<T> newTop;

        do {
            oldTop = top.get();
            if (oldTop == null) return null;
            newTop = oldTop.getNext();
        } while (!top.compareAndSet(oldTop, newTop));

        count.decrementAndGet();
        return oldTop.getItem();
    }
}


class DemoNonBlockingStack {
    public static void main( String args[] ) throws Exception {

        NonblockingStack<Integer> stack = new NonblockingStack<>();
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        int numThreads = 2;
        CyclicBarrier barrier = new CyclicBarrier(numThreads);

        long start = System.currentTimeMillis();
        Integer testValue = new Integer(51);

        try {
            for (int i = 0; i < numThreads; i++) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 10000; i++) {
                            stack.push(testValue);
                        }

                        try {
                            barrier.await();
                        } catch (InterruptedException | BrokenBarrierException ex) {
                            System.out.println("ignoring exception");
                            //ignore both exceptions
                        }

                        for (int i = 0; i < 10000; i++) {
                            stack.pop();
                        }
                    }
                });
            }
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
        }

        System.out.println("Number of elements in the stack = " + stack.size());
    }
}
