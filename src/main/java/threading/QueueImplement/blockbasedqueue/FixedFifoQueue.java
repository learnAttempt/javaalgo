package threading.QueueImplement.blockbasedqueue;


class FixedFifoQueue {
    private final int[] buffer;
    private final int capacity;
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    public FixedFifoQueue(int capacity) {
        this.capacity = capacity;
        this.buffer = new int[capacity];
    }

    public boolean put(int val) {
        if (size == capacity) return false; // full
        buffer[tail] = val;
        tail = (tail + 1) % capacity;
        size++;
        return true;
    }

    public Integer get() {
        if (size == 0) return null; // empty
        int val = buffer[head];
        head = (head + 1) % capacity;
        size--;
        return val;
    }

    public boolean isEmpty() { return size == 0; }
    public boolean isFull()  { return size == capacity; }
}

