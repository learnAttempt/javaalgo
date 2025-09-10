package threading.tokenBucketFilter;

import java.util.HashSet;
import java.util.Set;

public class DemoToken {
    public static void main(String [] args) throws InterruptedException {
        Set<Thread> allThreads=new HashSet<>();
        TokenBucketFilter tbf=TokenBucketFilterFactory.makeTokenBucketFilter(1);
        for(int i=0;i<10;i++){
            Thread t1=new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        tbf.getToken();
                    } catch (InterruptedException ie) {
                        System.out.println("We have a problem");
                    }
                }
            });
            t1.setName("Thread"+(i+1));
            allThreads.add(t1);



        }
        for(Thread t1:allThreads){
            t1.start();
        }
        for(Thread t1:allThreads){
            t1.join();
        }
    }
}
