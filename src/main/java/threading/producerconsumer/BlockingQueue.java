package threading.producerconsumer;

 class BlockingQueue<T>{
     T[] arr;
     int head;
     int tail;
     int capacity;
     int size;
     private final Object lock;


     public BlockingQueue(  int capacity) {
         this.arr = (T[]) new Object[capacity];
         this.head = 0;
         this.tail = 0;
         this.capacity = capacity;
         this.size = 0;
         lock=new Object();
     }

     public void enqueue(T item) throws InterruptedException {
         synchronized (lock) {
             while (size == capacity) {
                 lock.wait();
             }
             if(tail==capacity){
                 tail=0;
             }
             arr[tail++]=item;
             size++;

            lock.notifyAll();
         }
     }
     public T dequeue() throws InterruptedException{
         T item=null;
         synchronized (lock){
             while(size==0)
                 lock.wait();
             if(head==capacity)
                 head=0;
            item =arr[head];
             head++;
             size--;
             lock.notifyAll();

         }
         return item;
     }
 }
