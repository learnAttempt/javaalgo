package threading.taskSchedulerWithDependencies;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Simple multithreaded DAG task scheduler.
 * - Each task identified by String id.
 * - Tasks produce result of type V (use Void for no result).
 */
public class DependentTaskScheduler<V> {

    public static class DependencyFailedException extends RuntimeException {
        public DependencyFailedException(String msg, Throwable cause) { super(msg, cause); }
    }

    private final Map<String, TaskNode<V>> nodes = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final boolean failFast; // if true, dependents of failed tasks are completed exceptionally

    public DependentTaskScheduler(int workerThreads, boolean failFast) {
        this.executor = Executors.newFixedThreadPool(workerThreads);
        this.failFast = failFast;
    }

    private static class TaskNode<V> {
        final String id;
        final Callable<V> work;
        final List<String> dependencies;     // ids of tasks this node depends on
        final List<String> dependents = new ArrayList<>(); // filled in scheduler build
        final AtomicInteger unmetDeps = new AtomicInteger(0);
        final CompletableFuture<V> future = new CompletableFuture<>();

        TaskNode(String id, Callable<V> work, List<String> dependencies) {
            this.id = id;
            this.work = work;
            this.dependencies = dependencies == null ? Collections.emptyList() : dependencies;
        }
    }

    /**
     * Add a task. Id must be unique across adds.
     */
    public void addTask(String id, Callable<V> work, List<String> dependencies) {
        if (nodes.putIfAbsent(id, new TaskNode<>(id, work, dependencies)) != null) {
            throw new IllegalArgumentException("Duplicate task id: " + id);
        }
    }

    /**
     * Builds internal dependency graph, checks cycles, and runs tasks.
     * Returns map id -> CompletableFuture<V> for each task.
     * Throws IllegalStateException if cycle detected or missing dependency.
     */
    public Map<String, CompletableFuture<V>> runAll() {
        buildDependentsAndInDegrees();
        // Detect cycles via Kahn's algorithm
        if (hasCycle()) {
            throw new IllegalStateException("Cycle detected in task dependencies");
        }

        // Initialize unmetDep counts
        for (TaskNode<V> n : nodes.values()) {
            n.unmetDeps.set(n.dependencies.size());
        }

        // Submit ready tasks
        for (TaskNode<V> n : nodes.values()) {
            if (n.unmetDeps.get() == 0) submitNode(n);
        }

        // Return futures map
        Map<String, CompletableFuture<V>> out = new HashMap<>();
        for (Map.Entry<String, TaskNode<V>> e : nodes.entrySet()) out.put(e.getKey(), e.getValue().future);
        return out;
    }

    private void buildDependentsAndInDegrees() {
        // Clear dependents (in case runAll called multiple times)
        nodes.values().forEach(n -> n.dependents.clear());

        // Validate dependencies exist & fill dependents
        for (TaskNode<V> n : nodes.values()) {
            for (String depId : n.dependencies) {
                TaskNode<V> dep = nodes.get(depId);
                if (dep == null) throw new IllegalStateException("Task " + n.id + " depends on missing task " + depId);
                dep.dependents.add(n.id);
            }
        }
    }

    private boolean hasCycle() {
        // Kahn
        Map<String, Integer> indeg = new HashMap<>();
        for (TaskNode<V> n : nodes.values()) indeg.put(n.id, n.dependencies.size());
        Deque<String> q = new ArrayDeque<>();
        for (var e : indeg.entrySet()) if (e.getValue() == 0) q.add(e.getKey());
        int processed = 0;
        while (!q.isEmpty()) {
            String id = q.removeFirst();
            processed++;
            for (String depId : nodes.get(id).dependents) { // dependents are nodes that depend on id
                indeg.compute(depId, (k, v) -> v - 1);
                if (indeg.get(depId) == 0) q.add(depId);
            }
        }
        return processed != nodes.size();
    }

    private void submitNode(TaskNode<V> node) {
        // quick check: if already completed (could happen if canceled earlier), skip
        if (node.future.isDone()) return;

        executor.submit(() -> {
            try {
                V result = node.work.call();
                node.future.complete(result);
                // notify dependents
                for (String depId : node.dependents) {
                    TaskNode<V> child = nodes.get(depId);
                    // decrement; if hits zero, submit
                    int left = child.unmetDeps.decrementAndGet();
                    if (left == 0) submitNode(child);
                }
            } catch (Throwable t) {
                node.future.completeExceptionally(t);
                if (failFast) {
                    // Propagate failure to dependents
                    propagateFailureToDependents(node, t);
                } else {
                    // If not failFast, still decrement dependents so they may run (depends on semantics)
                    for (String depId : node.dependents) {
                        TaskNode<V> child = nodes.get(depId);
                        int left = child.unmetDeps.decrementAndGet();
                        if (left == 0) submitNode(child);
                    }
                }
            }
        });
    }

    private void propagateFailureToDependents(TaskNode<V> failedNode, Throwable cause) {
        ArrayDeque<String> q = new ArrayDeque<>();
        q.addAll(failedNode.dependents);
        while (!q.isEmpty()) {
            String id = q.removeFirst();
            TaskNode<V> n = nodes.get(id);
            // Try to complete exception only once
            n.future.completeExceptionally(new DependencyFailedException(
                    "Dependency failed for task " + n.id + " (cause from " + failedNode.id + ")", cause));
            // push further dependents
            q.addAll(n.dependents);
        }
    }

    /**
     * Shutdown scheduler's executor. Call after tasks done or on error.
     */
    public void shutdown(boolean now) {
        if (now) executor.shutdownNow();
        else executor.shutdown();
    }

    // Example convenience: wait until all tasks complete (success or failure)
    public CompletableFuture<Void> allOf(Map<String, CompletableFuture<V>> futures) {
        CompletableFuture<?>[] arr = futures.values().toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(arr);
    }

    // --- demo / quick test ---
    public static void main(String[] args) throws Exception {
        var sched = new DependentTaskScheduler<String>(4, true);

        // A simple DAG:
        // t1 -> t3
        // t2 -> t3
        // t3 -> t4
        sched.addTask("t1", () -> { Thread.sleep(300); System.out.println("t1 done"); return "r1"; }, List.of());
        sched.addTask("t2", () -> { Thread.sleep(200); System.out.println("t2 done"); return "r2"; }, List.of());
        sched.addTask("t3", () -> { System.out.println("t3 started"); Thread.sleep(100); System.out.println("t3 done"); return "r3"; }, List.of("t1","t2"));
        sched.addTask("t4", () -> { System.out.println("t4 started"); return "r4"; }, List.of("t3"));

        Map<String, CompletableFuture<String>> futures = sched.runAll();
        CompletableFuture<Void> all = sched.allOf(futures);
        all.whenComplete((v, ex) -> {
            if (ex != null) System.out.println("One or more tasks failed: " + ex);
            else System.out.println("All tasks finished successfully.");
            sched.shutdown(false);
        });

        // For demo, block main until all done:
        all.join();

        // inspect results individually:
        futures.forEach((id, f) -> {
            if (f.isCompletedExceptionally()) System.out.println(id + " failed");
            else System.out.println(id + " -> " + f.join());
        });
    }
}

