package threading;

import java.util.concurrent.Semaphore;

class PrintAlternatively {

    private final Semaphore fooSem=new Semaphore(1);
    private final Semaphore barSem=new Semaphore(0);
    int n;
    PrintAlternatively(int n){
        this.n=n;

    }

    public void foo() throws InterruptedException {
        for(int i=0;i<n;i++){
            fooSem.acquire();
            System.out.print("foo");
            barSem.release();
        }
    }
    public void bar() throws InterruptedException {
        for(int i=0;i<n;i++){
            barSem.acquire();
            System.out.print("bar");
            fooSem.release();
        }
    }
}

class DemoAlternative{
    public static void main(String[] args) throws InterruptedException {
        final PrintAlternatively p=new PrintAlternatively(5);
        Thread t1=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    p.foo();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                   p.bar();
                }catch (InterruptedException e){
                    throw new RuntimeException(e);
                }
            }
        });

        t2.start();
        t1.start();
        t1.join();
    }
}
