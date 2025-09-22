package threading.taskSchedulerWithDependencies;


import java.util.*;
        import java.util.concurrent.*;
        import java.util.concurrent.atomic.AtomicInteger;

public class DependentTaskSchedulerNoCF<V> {

    private final Map<String, TaskNode<V>> nodes = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public DependentTaskSchedulerNoCF(int workers) {
        this.executor = Executors.newFixedThreadPool(workers);
    }

    private static class TaskNode<V> {
        final String id;
        final Callable<V> work;
        final List<String> dependencies;
        final List<String> dependents = new ArrayList<>();
        final AtomicInteger unmetDeps = new AtomicInteger(0);

        // Result handling
        V result = null;
        Throwable error = null;
        final CountDownLatch done = new CountDownLatch(1);

        TaskNode(String id, Callable<V> work, List<String> dependencies) {
            this.id = id;
            this.work = work;
            this.dependencies = dependencies == null ? List.of() : dependencies;
        }

        boolean isDone() {
            return done.getCount() == 0;
        }

        void complete(V res) {
            this.result = res;
            done.countDown();
        }

        void fail(Throwable t) {
            this.error = t;
            done.countDown();
        }

        V getResult() throws Exception {
            done.await(); // block until task finished
            if (error != null) {
                throw new ExecutionException(error);
            }
            return result;
        }
    }

    // --- Public API ---

    public void addTask(String id, Callable<V> work, List<String> deps) {
        if (nodes.containsKey(id))
            throw new IllegalArgumentException("Duplicate id " + id);
        nodes.put(id, new TaskNode<>(id, work, deps));
    }

    public void runAll() {
        buildGraph();
        if (hasCycle()) throw new IllegalStateException("Cycle detected");

        for (TaskNode<V> n : nodes.values()) {
            n.unmetDeps.set(n.dependencies.size());
        }
        for (TaskNode<V> n : nodes.values()) {
            if (n.unmetDeps.get() == 0) submit(n);
        }
    }

    public V getResult(String id) throws Exception {
        TaskNode<V> node = nodes.get(id);
        if (node == null) throw new IllegalArgumentException("No such task " + id);
        return node.getResult();
    }

    public void shutdown() {
        executor.shutdown();
    }

    // --- Internal logic ---

    private void buildGraph() {
        for (TaskNode<V> n : nodes.values()) n.dependents.clear();
        for (TaskNode<V> n : nodes.values()) {
            for (String dep : n.dependencies) {
                TaskNode<V> d = nodes.get(dep);
                if (d == null) throw new IllegalStateException("Missing dependency " + dep);
                d.dependents.add(n.id);
            }
        }
    }

    private boolean hasCycle() {
        Map<String,Integer> indeg = new HashMap<>();
        for (TaskNode<V> n : nodes.values()) indeg.put(n.id, n.dependencies.size());
        Deque<String> q = new ArrayDeque<>();
        for (var e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());
        int processed = 0;
        while (!q.isEmpty()) {
            String id = q.removeFirst();
            processed++;
            for (String dep : nodes.get(id).dependents) {
                indeg.put(dep, indeg.get(dep)-1);
                if (indeg.get(dep) == 0) q.add(dep);
            }
        }
        return processed != nodes.size();
    }

    private void submit(TaskNode<V> node) {
        executor.submit(() -> {
            try {
                V res = node.work.call();
                node.complete(res);
                for (String depId : node.dependents) {
                    TaskNode<V> child = nodes.get(depId);
                    if (child.unmetDeps.decrementAndGet() == 0) {
                        submit(child);
                    }
                }
            } catch (Throwable t) {
                node.fail(t);
                // propagate failure? depends on policy.
                for (String depId : node.dependents) {
                    TaskNode<V> child = nodes.get(depId);
                    child.fail(new RuntimeException("Dependency " + node.id + " failed", t));
                }
            }
        });
    }

    // --- Demo ---
    public static void main(String[] args) throws Exception {
        var sched = new DependentTaskSchedulerNoCF<String>(4);

        sched.addTask("A", () -> { Thread.sleep(200); System.out.println("A done"); return "ra"; }, List.of());
        sched.addTask("B", () -> { Thread.sleep(100); System.out.println("B done"); return "rb"; }, List.of());
        sched.addTask("C", () -> { System.out.println("C starts"); return "rc"; }, List.of("A","B"));
        sched.addTask("D", () -> { System.out.println("D starts"); return "rd"; }, List.of("C"));

        sched.runAll();

        System.out.println("Result of D: " + sched.getResult("D"));

        sched.shutdown();
    }
}
