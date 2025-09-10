package threading.lrucache;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentLRUCache <K,V>{


    private final ConcurrentHashMap<K,Node<K,V>> map ;
    private final int capacity;
    private final Node<K,V> head;
    private final Node<K,V> tail;
    private final ReentrantLock lock=new ReentrantLock();


    ConcurrentLRUCache(int capacity){
        if(capacity<=0)
            throw new IllegalArgumentException("capacity cannot be zero");
        this.capacity=capacity;
        this.map=new ConcurrentHashMap<>(Math.max(this.capacity*2,16));
        this.head=new Node<>(null,null);
        this.tail=new Node<>(null,null);
        this.head.next=tail;
        this.tail.prev=head;

    }

    public V get(K key){
        Objects.requireNonNull(key);
        Node<K,V> node=map.get(key);
        if(node==null)
            return null;
        moveToFront(node);

        return node.value;

    }

    public void put(K key,V value){
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        Node<K,V> existingNode=map.get(key);
        if(existingNode!=null){
            existingNode.value=value;
            moveToFront(existingNode);
            return;
        }

        Node<K,V> newNode=new Node<>(key,value);
        Node<K,V> prev=map.putIfAbsent(key,newNode);
        if(prev!=null){
            prev.value=value;
            moveToFront(prev);
            return;

        }

        lock.lock();
        try{
            insertFront(newNode);
            if(map.size()>capacity){
                Node<K,V> oldest=tail.prev;
                if(oldest!=head){
                    removeNode(oldest);
                    map.remove(oldest.key);
                }
            }
        }finally {
            lock.unlock();
        }

    }

    public V remove(K key){
        Objects.requireNonNull(key);
        Node<K,V> node=map.remove(key);
        if(node==null) return null;
        lock.lock();
        try
        {
            removeNode(node);

        }finally {

            lock.unlock();
        }
        return node.value;
    }

    public void clear(){
        lock.lock();
        try {
            map.clear();
            head.next=tail;
            tail.prev=head;
        }finally {
            lock.unlock();
        }
    }

    public int size(){
        return map.size();
    }

    private void moveToFront(Node<K,V> node) {

        lock.lock();
        try {
            removeNode(node);
            insertFront(node);
        } finally {
            lock.unlock();
        }
    }
    private void insertFront(Node<K,V> node){
        node.next=head.next;
        node.prev=head;
        head.next.prev=node;
        head.next=node;
    }

    private void removeNode(Node<K,V> node){
        Node<K,V> prev=node.prev;
        Node<K,V> next=node.next;
        if(prev!=null)prev.next=next;
        if(next!=null) next.prev=prev;
        node.next=null;
        node.prev=null;

    }



    private static class Node<K,V>{
        private final K key;
        volatile V value;
        Node<K,V> next;
        Node<K,V> prev;
        Node(K key,V value){

            this.key=key;
            this.value=value;
            next=prev=null;

        }
    }
}
