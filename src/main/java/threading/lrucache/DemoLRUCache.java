package threading.lrucache;

public class DemoLRUCache {
    public static void main(String[] args){
        ConcurrentLRUCache<Integer,String> cache=new ConcurrentLRUCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
       System.out.println( cache.get(1));
        cache.put(4, "four");
        System.out.println( cache.get(2));
    }
}
