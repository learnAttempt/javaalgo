package threading.unisexbathroom;


import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BathroomOccupancy {


    private final Queue<Person> democrats=new PriorityQueue<>((a, b)-> (int) (a.getPriority()-b.getPriority()));
    private final Queue<Person> republican=new PriorityQueue<>((a, b)-> (int) (a.getPriority()-b.getPriority()));
    private Party lastServed=Party.NONE;
    private Party currentServed=Party.NONE;
    private  int insideCount=0;
    private final Lock lock=new ReentrantLock();
    private final Condition democratWait=lock.newCondition();
    private final Condition republicWait=lock.newCondition();


    public void enter(Person p) throws InterruptedException {
        lock.lock();
        try {
            if (p.party == Party.DEMOCRATS){
                democrats.add(p);

                while (!canEnter(p)) {
                    democratWait.await();
                }
                democrats.remove();

            }
            else{
                republican.add(p);
                while (!canEnter(p)){
                    republicWait.await();
                }
                republican.remove();

            }
            if(currentServed==Party.NONE)
                currentServed=p.party;
            insideCount++;
            System.out.println(System.currentTimeMillis() +
                    " | " + p + " ENTERED bathroom. Inside count = " + insideCount);
        }finally {
            lock.unlock();
        }

    }

    public void leave(Person p){
        lock.lock();
        try{
            insideCount--;
            System.out.println(System.currentTimeMillis() +
                    " | " + p + " LEFT bathroom. Inside count = " + insideCount);
            if(insideCount==0){
                lastServed=p.party;
                currentServed=Party.NONE;
                if(lastServed==Party.DEMOCRATS && !republican.isEmpty())
                    republicWait.signalAll();
                else if(lastServed==Party.REPUBLICAN && !democrats.isEmpty())
                    democratWait.signalAll();
                else {
                    if(!democrats.isEmpty())
                        democratWait.signalAll();
                    if(!republican.isEmpty())
                        republicWait.signalAll();
                }
            }else{
                if(currentServed==Party.DEMOCRATS) democratWait.signalAll();
                if(currentServed==Party.REPUBLICAN) republicWait.signalAll();
            }


        }finally {
            lock.unlock();
        }
    }

    private  boolean canEnter(Person p){
        if(insideCount>=3) return false;
       if(currentServed==Party.NONE){
            if(!republican.isEmpty() && !democrats.isEmpty()){
                if(lastServed==Party.DEMOCRATS){
                    return p.party==Party.REPUBLICAN && p.equals(republican.peek());
                }
                else if(lastServed==Party.REPUBLICAN){
                    return p.party==Party.DEMOCRATS && p.equals(democrats.peek());
                }
            }else if(!democrats.isEmpty()){
                return p.party==Party.DEMOCRATS && p.equals(democrats.peek());
            }else if(!republican.isEmpty())
                 return p.party==Party.REPUBLICAN && p.equals(republican.peek());
            return false;

       }

       if(p.party!=currentServed) return false;
       Queue<Person> pq = p.party==Party.DEMOCRATS? democrats:republican;
       return p.equals(pq.peek());

    }


    enum Party{
        DEMOCRATS,
        REPUBLICAN,
        NONE;
    }

    static class  Person implements Runnable{
        String name ;
        Party  party;
        long arrival;
        long duration;
        BathroomOccupancy bathroom;
        Person(String name, Party p,BathroomOccupancy bathroom ){
            this.name=name;
            this.party=p;
            this.arrival=System.currentTimeMillis();
            this.bathroom=bathroom;
            this.duration= getDuration(name);
        }

        private long getDuration(String name){
            return name.length()* 200L;
        }

        public double getPriority(){
            long waited=System.currentTimeMillis()-arrival;
            return duration-0.75*waited;
        }

        public String toString() {
            return party + "-" + name + "(f=" + duration + ")";
        }

        @Override
       public void run(){
            try{
                bathroom.enter(this);
                Thread.sleep(duration);
                bathroom.leave(this);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static void main(String[] args){
        BathroomOccupancy bathroom=new BathroomOccupancy();
       // ExecutorService executors=Executors.newFixedThreadPool(6);
        Thread[] people = {
                new Thread(new Person("Alice", Party.DEMOCRATS, bathroom)),
                new Thread(new Person("Bob", Party.REPUBLICAN, bathroom)),
                new Thread(new Person("Charlie", Party.DEMOCRATS, bathroom)),
                new Thread(new Person("Dave", Party.REPUBLICAN, bathroom)),
                new Thread(new Person("Elizabeth", Party.DEMOCRATS, bathroom)), // long job
                new Thread(new Person("Frank", Party.REPUBLICAN, bathroom))
        };

        for (Thread t : people) {
            t.start();
        }
    }
}
