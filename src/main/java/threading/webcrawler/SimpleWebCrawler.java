package threading.webcrawler;

import java.util.*;

public class SimpleWebCrawler {
/*
    private HashSet<String> visited=new HashSet<>();
    private String startHostName;
    private String getHostName(String url){
        String [] part=url.split("/");
        return part[2];
    }

    public void dfs(String hostUrl,HtmlParser htmlParser){
        visited.add(hostUrl);
        for(String nextUrl: htmlParser.getUrls(hostUrl)){
            if(!visited.contains(nextUrl) && getHostName(nextUrl).equals(startHostName)){
                dfs(nextUrl,htmlParser);
            }
        }
    }

    public List<String> crawl(String startHostUrl, HtmlParser htmlParser){
        startHostName=getHostName(startHostUrl);
        dfs(startHostUrl,htmlParser);
        return new ArrayList<>(visited);
    }*/

    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        String hostname = getHostname(startUrl);
        Set<String> result = new HashSet<>();

        Queue<String> queue = new LinkedList<>();
        queue.offer(startUrl);
        result.add(startUrl);

        while(!queue.isEmpty()) {
            String cur = queue.poll();
            for (String next: htmlParser.getUrls(cur)) {
                if (next.startsWith(hostname) && !result.contains(next)) {
                    queue.offer(next);
                    result.add(next);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private String getHostname(String url) {
        int index = url.indexOf("/", 7);
        return index == -1 ? url : url.substring(0, index);
    }
}
