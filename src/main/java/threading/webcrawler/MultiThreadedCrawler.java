package threading.webcrawler;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class MultiThreadedCrawler {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        counter.incrementAndGet();
        new Thread(new AysncTask(startUrl, htmlParser)).start();
        while (counter.get() > 0) {
            sleep(15);
        }
        return new LinkedList<String>(seen);

    }

    private void sleep(final long time) {
        try {
            Thread.sleep(time);
        } catch(final Exception e) {
            // do nothing
        }
    }

    class AysncTask implements Runnable {
        private String startURL;
        private HtmlParser htmlParser;
        private ExecutorService executor = Executors.newFixedThreadPool(6);
        AysncTask(String startURL, HtmlParser htmlParser) {
            this.startURL = startURL;
            this.htmlParser = htmlParser;
        }

        public void run() {
            seen.add(startURL);
            String host = getHost(startURL);
            List<String> nextURLs = htmlParser.getUrls(startURL);
            for (String nextURL : nextURLs) {
                if (seen.contains(nextURL)) continue;
                String nextHost = getHost(nextURL);
                if (nextHost.equals(host)) {
                    counter.incrementAndGet();
                    new Thread(new AysncTask(nextURL, htmlParser)).start();
                }
            }
            counter.decrementAndGet();
        }

        private String getHost(String url) {

            return url.split("/")[2];
        }


    }
}
/*class Solution {
    private int cnt = 0;
    private String hostname;
    private HtmlParser htmlParser;
    private Set<String> visited = ConcurrentHashMap.newKeySet();
    //private ExecutorService executor = Executors.newFixedThreadPool(6);
    private ThreadPoolExecutor executor = (ThreadPoolExecutor)(Executors.newFixedThreadPool(6));
  private static String getHostname(String url) {
        if (url.toLowerCase().startsWith("http://")) {
            int len = url.length();
            // http://
            for (int i = 7; i < len; ++i) {
                if (url.charAt(i) == '/') {
                    return url.substring(7, i);
                }
            }
            return url.substring(7, len);
        }
        return "";

    }

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        this.hostname = getHostname(startUrl);
        //System.out.printf("hostname: %s\n", this.hostname);
        this.htmlParser = htmlParser;

        visited.add(startUrl);
        executor.submit(() -> { appendUrl(startUrl); });

        while (executor.getActiveCount() > 0 || executor.getQueue().size() > 0) {
            //System.out.println("Active tasks: " + executor.getActiveCount() +
            //                   ", Queued tasks: " + executor.getQueue().size());
            try {
                Thread.sleep(5);
            }
            catch (Exception e) {}
        }

        executor.shutdown();


        return new LinkedList<String>(visited);
    }

    private void appendUrl(String url) {
        //System.out.printf("%s\n", url);
        for (String u : htmlParser.getUrls(url)) {
            // for (String t : visited) {
            //     System.out.printf("%s ", t);
            // }
            // System.out.printf(" end\n");
            // if (++cnt == 10) {
            // //    return;
            // }
            //System.out.printf("%s hostname=%s visited=%d\n", u, getHostname(u), visited.size());
            //if (!visited.contains(u)) System.out.printf("!!!");
            if (!visited.contains(u) && this.hostname.equals(getHostname(u))) {
                visited.add(u);
                //System.out.printf("added\n");
                //list.add(u);
                executor.submit(() -> { appendUrl(u); });
            }
        }
    }
}*/