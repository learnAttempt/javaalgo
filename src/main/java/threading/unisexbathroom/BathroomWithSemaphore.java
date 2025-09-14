package threading.unisexbathroom;



import java.util.*;
        import java.util.concurrent.*;
        import java.util.concurrent.locks.*;

class BathroomWithSemaphore {
    private final Semaphore slots = new Semaphore(3, true); // max 3 people
    private final Lock lock = new ReentrantLock();
    private final Condition democratsCondition = lock.newCondition();
    private final Condition republicansCondition = lock.newCondition();

    private final PriorityQueue<Person> democrats =
            new PriorityQueue<>((p1, p2) -> compareWithAging(p1, p2));
    private final PriorityQueue<Person> republicans =
            new PriorityQueue<>((p1, p2) -> compareWithAging(p1, p2));

    private Party currentParty = Party.NONE;
    private Party lastServed = Party.NONE;
    private int insideCount = 0;
    private final List<Person> inside = new ArrayList<>();

    // f(N): duration function
    private static int f(String name) {
        return name.length() * 200; // example: proportional to name length
    }

    // Aging comparator
    private static int compareWithAging(Person p1, Person p2) {
        long now = System.currentTimeMillis();
        long w1 = now - p1.enqueueTime;
        long w2 = now - p2.enqueueTime;

        double alpha = 0.05; // aging factor (ms to priority conversion)
        double eff1 = p1.duration - alpha * w1;
        double eff2 = p2.duration - alpha * w2;

        return Double.compare(eff1, eff2);
    }

    public void enterWithSemaphore(Person p) throws InterruptedException {
        lock.lock();
        try {
            p.setDuration(BathroomWithSemaphore::f);
            p.enqueueTime = System.currentTimeMillis();

            if (p.party == Party.D) {
                democrats.add(p);
                while (!canEnter(p)) {
                    democratsCondition.await();
                }
                democrats.remove(p);
            } else {
                republicans.add(p);
                while (!canEnter(p)) {
                    republicansCondition.await();
                }
                republicans.remove(p);
            }

            slots.acquire(); // enforce max 3 capacity
            insideCount++;
            inside.add(p);

            if (currentParty == Party.NONE) {
                currentParty = p.party;
            }
            logEvent(p, "ENTERED");
        } finally {
            lock.unlock();
        }
    }

    public void leaveWithSemaphore(Person p) {
        lock.lock();
        try {
            insideCount--;
            inside.remove(p);
            slots.release();
            logEvent(p, "LEFT");

            if (insideCount == 0) {
                lastServed = currentParty;
                currentParty = Party.NONE;

                // Alternate parties fairly
                if (lastServed == Party.D && !republicans.isEmpty()) {
                    republicansCondition.signalAll();
                } else if (lastServed == Party.R && !democrats.isEmpty()) {
                    democratsCondition.signalAll();
                } else {
                    if (!democrats.isEmpty()) democratsCondition.signalAll();
                    if (!republicans.isEmpty()) republicansCondition.signalAll();
                }
            } else {
                // Still same party inside, allow more
                if (currentParty == Party.D) democratsCondition.signalAll();
                else republicansCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean canEnter(Person p) {
        if (slots.availablePermits() == 0) return false;

        if (currentParty == Party.NONE) {
            if (!democrats.isEmpty() && !republicans.isEmpty()) {
                // alternate turns
                if (lastServed == Party.D) {
                    return p.party == Party.R && p.equals(republicans.peek());
                } else {
                    return p.party == Party.D && p.equals(democrats.peek());
                }
            } else if (!democrats.isEmpty()) {
                return p.party == Party.D && p.equals(democrats.peek());
            } else if (!republicans.isEmpty()) {
                return p.party == Party.R && p.equals(republicans.peek());
            }
            return false;
        }

        // BathroomWithSemaphore already locked to a party
        if (p.party != currentParty) return false;

        if (currentParty == Party.D) return democrats.peek() == p;
        else return republicans.peek() == p;
    }

    private void logEvent(Person p, String action) {
        String occupants = inside.toString();
        System.out.printf("%d | %-15s %-6s | Inside: %s%n",
                System.currentTimeMillis(), p, action, occupants);
    }
}

enum Party { D, R, NONE }

class Person implements Runnable {
    final String name;
    final Party party;
    final BathroomWithSemaphore bathroom;
    int duration;
    long enqueueTime; // used for aging

    Person(String name, Party party, BathroomWithSemaphore bathroom) {
        this.name = name;
        this.party = party;
        this.bathroom = bathroom;
    }

    void setDuration(java.util.function.Function<String, Integer> durationFunc) {
        this.duration = durationFunc.apply(name);
    }

    @Override
    public void run() {
        try {
            bathroom.enterWithSemaphore(this);
            Thread.sleep(duration); // simulate BathroomWithSemaphore usage
            bathroom.leaveWithSemaphore(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String toString() {
        return party + "-" + name + "(f=" + duration + ")";
    }
}

 class BathroomWithSemaphoreTest {
    public static void main(String[] args) {
        BathroomWithSemaphore BathroomWithSemaphore = new BathroomWithSemaphore();

        Thread[] people = {
                new Thread(new Person("Alice", Party.D, BathroomWithSemaphore)),
                new Thread(new Person("Bob", Party.R, BathroomWithSemaphore)),
                new Thread(new Person("Charlie", Party.D, BathroomWithSemaphore)),
                new Thread(new Person("Dave", Party.R, BathroomWithSemaphore)),
                new Thread(new Person("Elizabeth", Party.D, BathroomWithSemaphore)), // long job
                new Thread(new Person("Frank", Party.R, BathroomWithSemaphore))
        };

        for (Thread t : people) t.start();
    }
}
