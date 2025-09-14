package threading.unisexbathroom;


        import java.util.*;
        import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
 * To execute Java, please define "static void main" on a class
 * named Solution.
 *
 * If you need more classes, simply define them inline.
 */

class BathroomWithoutQueue {
    private  int democrats;
    private  int republicans;
    STATE bathroomState;
    // Introducing turn variable for fairness
    TURN turnState;

    BathroomWithoutQueue(){
        democrats=0;
        republicans= 0;
        bathroomState = STATE.EMPTY;
        turnState = TURN.DEMOCRAT;
    }

    synchronized void entryRepublican(int time) throws InterruptedException {
        while(bathroomState.equals(STATE.DEMOCRAT) || republicans >= 3 || (turnState.equals(TURN.DEMOCRAT) && democrats > 0)){
            wait();
        }
        if(bathroomState.equals(STATE.EMPTY)){
            bathroomState = STATE.REPUBLICAN;
            turnState = TURN.REPUBLICAN;
        }
        republicans++;
        processR(time);
    }

    void processR(int time) throws InterruptedException {
        Thread th = new Thread(() -> {
            System.out.println("R is doing his thing for time: " + time);
            System.out.println("Number of (R,D) in bathroom : " + republicans + ", " + democrats);
            try {
                Thread.sleep(time * 1000);
                exitRepublican();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        th.start();

    }

    synchronized void entryDemocrat(int time) throws InterruptedException {
        while(bathroomState.equals(STATE.REPUBLICAN) || democrats >= 3 || (turnState.equals(TURN.REPUBLICAN) && republicans > 0)){
            wait();
        }
        if(bathroomState.equals(STATE.EMPTY)){
            bathroomState = STATE.DEMOCRAT;
            turnState = TURN.DEMOCRAT;
        }
        democrats++;
        processD(time);
    }

    void processD(int time) throws InterruptedException {
        Thread th = new Thread(() -> {
            System.out.println("D is doing his thing for time: " + time);
            System.out.println("Number of (R,D) in bathroom : " + republicans + ", " + democrats);
            try {
                Thread.sleep(time * 1000);
                exitDemocrat();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        th.start();

    }
    synchronized void exitDemocrat() {
        democrats--;
        if (democrats == 0) {
            bathroomState = STATE.EMPTY;
            turnState = TURN.REPUBLICAN;
            notifyAll();
        }
    }

    synchronized void exitRepublican() {
        republicans--;
        if (republicans == 0) {
            bathroomState = STATE.EMPTY;
            turnState = TURN.DEMOCRAT;
            notifyAll();
        }
    }


}

enum STATE {
    REPUBLICAN,
    DEMOCRAT,
    EMPTY;
}

enum TURN {
    DEMOCRAT,
    REPUBLICAN
}




 class BathroomWithoutQueueDemo {
    public static void main(String[] args) {
        BathroomWithoutQueue solution = new BathroomWithoutQueue();

        List<Future<?>> futures = new ArrayList<Future<?>>();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for(int i = 0; i < 5; i++){
            futures.add(executorService.submit(() -> {
                try {
                    solution.entryRepublican(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for(int i = 0; i < 10; i++){
            futures.add(executorService.submit(() -> {
                try {
                    solution.entryDemocrat(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }




        for(int i = 0; i < 10; i++){
            futures.add(executorService.submit(() -> {
                try {
                    solution.entryRepublican(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for(Future future : futures){
            try{
                future.get();
            }catch (Exception ex){

            }
        }

        executorService.shutdown();
    }
}

