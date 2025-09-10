package threading.QueueImplement.blockbasedqueue;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class BlockBasedQueue {


    static class Block{
        final int start;
        final int length;
        private int head=0;
        private int tail=0;
        Block next;
        Block(int start,int length){
            this.start=start;
            this.length=length;
        }

        public boolean isFull(){
            return  head==tail;
        }
        public boolean isEmpty(){
            return tail==0;
        }

        static class MemManager{
            private final int[] buffer;
         //   private final int blockSize;
            private ArrayBlockingQueue<Block> freeBlockList;

            MemManager(int capacity,int blockSize){
                if(capacity%blockSize!=0)
                    throw new IllegalArgumentException("block size not multiple");
              //  this.blockSize=blockSize;
                int numBlocks=capacity/blockSize;

                buffer=new int[capacity];
                freeBlockList=new ArrayBlockingQueue<>(numBlocks);
               // int start=0;
                for(int i=0;i<numBlocks;i++){
                    freeBlockList.add(new Block(i*blockSize,blockSize));
                    //start+=blockSize;
                }
            }

             Block allocate(){
                return freeBlockList.poll();
            }

            void freeBlock(Block block){
                block.head=block.tail=0;
                block.next=null;
                freeBlockList.offer(block);
            }
            int [] buffer(){
                return buffer;
            }
        }

        static class SharedQueue{
            private final MemManager memManager;
            private final ReentrantLock lock=new ReentrantLock();
            private Block headBlock;
            private Block tailBlock;

            SharedQueue(MemManager mem){
                this.memManager=mem;
            }

            public boolean put(int val){
                lock.lock();
                try {
                    if (tailBlock == null) {
                        tailBlock = memManager.allocate();
                        if (tailBlock == null) return false;
                        headBlock = tailBlock;

                    }
                    if (tailBlock.isFull()) {
                        Block block = memManager.allocate();
                        if (block == null) return false;
                        tailBlock.next = block;
                        tailBlock = block;
                    }
                    int pos = tailBlock.start + tailBlock.tail++;
                    memManager.buffer()[pos] = val;
                    return true;
                }
                finally{
                    lock.unlock();
                }
            }


            public Integer get(){
                lock.lock();
                try {
                    if (headBlock == null)
                        return null;
                    if(headBlock.isEmpty()){
                        Block old=headBlock;
                        headBlock=headBlock.next;
                        memManager.freeBlock(old);
                        if(headBlock==null)
                            return null;
                    }
                    int pos=headBlock.start+headBlock.head++;
                    return memManager.buffer()[pos];

                }finally {
                    lock.unlock();
                }
            }
        }


        public static void main(String [] args) throws InterruptedException {
            int totalSize=1024*1024;
            int blockSize=1024;
            MemManager memManager=new MemManager(totalSize,blockSize);
            SharedQueue q1=new SharedQueue(memManager);
            SharedQueue q2=new SharedQueue(memManager);

            Thread t1=new Thread(()-> {
                for (int i=0;i<10000;i++)
                    while (!q1.put(i))
                        Thread.yield();
            });
            Thread t2=new Thread(()-> {
                for (int i=0;i<10000;i++)
                    while (!q2.put(i+10000))
                        Thread.yield();
            });
            Thread c1=new Thread(()->{
                int count=0;
                while(count<10000){
                    Integer v=q1.get();
                    if(v!=null) count++;
                }
                System.out.println("Q1 done");
            });

            Thread c2=new Thread(()->{
                int count=0;
                while(count<10000){
                    Integer v=q2.get();
                    if(v!=null) count++;
                }
                System.out.println("Q2 done");
            });
            t1.start();
            t2.start();
            c1.start();
            c2.start();
            t1.join();
            t2.join();
            c1.join();
            c2.join();
            System.out.println("All queues completed successfully.");
        }
    }
}
