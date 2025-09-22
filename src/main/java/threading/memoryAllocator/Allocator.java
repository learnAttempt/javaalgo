package threading.memoryAllocator;

import java.util.*;

class Allocator {
    private final int n;
    private final TreeSet<int[]> free;  // free intervals [start, end]
    private final Map<Integer, List<int[]>> allocated;

    public Allocator(int n) {
        this.n = n;
        this.free = new TreeSet<>(Comparator.comparingInt(a -> a[0]));
        this.allocated = new HashMap<>();
        free.add(new int[]{0, n - 1});  // initially all free
    }

    public int allocate(int size, int mID) {
        for (int[] interval : free) {
            int start = interval[0], end = interval[1];
            if (end - start + 1 >= size) {
                int allocStart = start;
                int allocEnd = start + size - 1;

                // update free intervals
                free.remove(interval);
                if (allocEnd < end) free.add(new int[]{allocEnd + 1, end});

                // store allocation
                allocated.computeIfAbsent(mID, k -> new ArrayList<>())
                        .add(new int[]{allocStart, allocEnd});
                return allocStart;
            }
        }
        return -1; // no space
    }

    public int freeMemory(int mID) {
        List<int[]> blocks = allocated.remove(mID);
        if (blocks == null) return 0;

        int freed = 0;
        for (int[] block : blocks) {
            freed += block[1] - block[0] + 1;
            insertAndMerge(block);
        }
        return freed;
    }

    // helper: insert free block and merge with neighbors
    private void insertAndMerge(int[] block) {
        int start = block[0], end = block[1];

        // find neighbors
        int[] lower = free.floor(new int[]{start, start});
        int[] higher = free.ceiling(new int[]{start, start});

        if (lower != null && lower[1] + 1 >= start) {
            start = lower[0];
            end = Math.max(end, lower[1]);
            free.remove(lower);
        }
        if (higher != null && end + 1 >= higher[0]) {
            end = Math.max(end, higher[1]);
            free.remove(higher);
        }
        free.add(new int[]{start, end});
    }
}

