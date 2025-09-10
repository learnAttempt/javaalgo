package threading;

import java.util.concurrent.Semaphore;

public class FizzBuzz {

    private int n;
    private int curr;
    Semaphore num=new Semaphore(1);
    Semaphore fizz=new Semaphore(0);
    Semaphore buzz=new Semaphore(0);
    Semaphore fizzbuzz=new Semaphore(0);

    public FizzBuzz(int n) {
        this.n = n;
        this.curr = 1;
    }

    private void release(){
        curr++;
        if(curr<=n){
            if(curr%3==0&& curr%5!=0)
                fizz.release();
           else if(curr%3!=0 && curr%5==0)
                buzz.release();
           else if(curr%15==0)
               fizzbuzz.release();
           else
               num.release();

        }
        else{
            fizz.release();
            buzz.release();
            fizzbuzz.release();
            num.release();
        }
    }

    public void fizz() throws InterruptedException{
        while(curr<=n){
            fizz.acquire();
            if(curr<=n)
                System.out.print("fizz"+" ");
            release();
        }
    }
    public void buzz() throws InterruptedException{
        while(curr<=n){
            buzz.acquire();
            if(curr<=n)
                System.out.print("buzz"+" ");
            release();
        }
    }

    public void fizzbuzz() throws InterruptedException{
        while(curr<=n){
            fizzbuzz.acquire();
            if(curr<=n)
                System.out.print("fizzbuzz"+" ");
            release();
        }
    }
    public void number() throws InterruptedException{
        while(curr<=n){
            num.acquire();
            if(curr<=n)
                System.out.print(curr+" ");
            release();
        }
    }


}

class DemoFizzBuzz{
    public static void main(String [] args){
        FizzBuzz fb=new FizzBuzz(10);
        Thread t1=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    fb.fizz();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    fb.buzz();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t3=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    fb.fizzbuzz();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t4=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    fb.number();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();

    }
}
