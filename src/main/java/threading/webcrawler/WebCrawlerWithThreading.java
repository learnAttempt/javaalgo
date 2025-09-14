package threading.webcrawler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WebCrawlerWithThreading {

    ExecutorService workers;
    HtmlParser htmlParser;
    CountDownLatch latch =new CountDownLatch(1);
    WebCrawlerWithThreading(int poolSize,HtmlParser htmlParser){
        workers= Executors.newFixedThreadPool(poolSize);
        this.htmlParser=htmlParser;

    }
    private String getHostName(String url){
        String host=url.substring(7);
        int index=host.indexOf("/");
        return index==-1? host: host.substring(0,index);
    }
public List<String> crawl(String hostUrl){
    String hostname=getHostName(hostUrl);
    Set<String> visited= ConcurrentHashMap.newKeySet();
    visited.add(hostUrl);
    workers.submit(()->crawlUrl(hostUrl,hostname,visited));
    try {
        latch.await();
    }catch (InterruptedException e){
        Thread.currentThread().interrupt();
    }
    workers.shutdown();
    return new ArrayList<>(visited);
}

private void crawlUrl(String hostUrl,String hostName , Set<String> visited) {

    try {
        for (String nextUrl : htmlParser.getUrls(hostUrl)) {
            if (!visited.contains(nextUrl) && hostName.equals(getHostName(nextUrl))) {
                visited.add(nextUrl);
                latch.countUp();
                workers.submit(() -> crawlUrl(hostUrl, hostName, visited));

            }
        }
    }finally {
        latch.countDown();
    }

}
static class CountDownLatch{

        private final AtomicInteger counter;
        CountDownLatch(int count) {
            this.counter=new AtomicInteger(count);

        }
        public void countUp() {
            counter.incrementAndGet();
        }
        public void countDown() {
            if (counter.decrementAndGet() == 0)
                synchronized (this) {

                    notifyAll();
                }
        }

        public void await() throws InterruptedException {
            synchronized (this){
                while (counter.get()>0)
                       wait();
               }
            }

    }

}

class WebCrawlerDemo{
    public static void main(String[] args){
        HtmlParser mockParser = url -> {
            switch (url) {
                case "http://example.org": return Arrays.asList(
                        "http://example.org/about",
                        "http://example.org/contact",
                        "http://othersite.com/home"  // different host, ignored
                );
                case "http://example.org/about": return Arrays.asList(
                        "http://example.org/team",
                        "http://example.org/jobs"
                );
                default: return Collections.emptyList();
            }
        };
        WebCrawlerWithThreading webCrawlerWithThreading=new WebCrawlerWithThreading(4,mockParser);
        List<String> result = webCrawlerWithThreading.crawl("http://example.org");

        System.out.println("\nFinal visited URLs:");
        result.forEach(System.out::println);
    }
}

