package threading.webcrawler;

import java.util.List;

interface HtmlParser {
    public List<String> getUrls(String url);
}
