package threading.memoryAllocator;

class LinkedAllocator {
    private class Node {
        int start, end, mID;
        Node next;
        Node(int s, int e, int id) { start = s; end = e; mID = id; }
        int size() { return end - start + 1; }
    }

    private Node head;

    public LinkedAllocator(int n) {
        head = new Node(0, n - 1, 0); // initially one big free block
    }

    public int allocate(int size, int mID) {
        Node prev = null, cur = head;
        while (cur != null) {
            if (cur.mID == 0 && cur.size() >= size) {
                int allocStart = cur.start;
                int allocEnd = cur.start + size - 1;

                // create allocated node
                Node allocated = new Node(allocStart, allocEnd, mID);

                if (allocEnd < cur.end) {
                    // split: allocated + remainder free
                    Node remainder = new Node(allocEnd + 1, cur.end, 0);
                    remainder.next = cur.next;
                    allocated.next = remainder;
                } else {
                    // exact fit
                    allocated.next = cur.next;
                }

                if (prev == null) head = allocated;
                else prev.next = allocated;

                return allocStart;
            }
            prev = cur;
            cur = cur.next;
        }
        return -1; // no suitable block
    }

    public int freeMemory(int mID) {
        int freed = 0;
        Node cur = head;
        Node prev = null;

        while (cur != null) {
            if (cur.mID == mID) {
                freed += cur.size();
                cur.mID = 0; // mark free

                // try merging with previous free block
                if (prev != null && prev.mID == 0) {
                    prev.end = cur.end;
                    prev.next = cur.next;
                    cur = prev; // move back pointer
                }

                // try merging with next free block
                if (cur.next != null && cur.next.mID == 0) {
                    cur.end = cur.next.end;
                    cur.next = cur.next.next;
                }
            }
            prev = cur;
            cur = cur.next;
        }
        return freed;
    }
}

