package loadbalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicLoadBalancer {

    private List<String> serverList;
    private AtomicInteger counter = new AtomicInteger(0);

    public AtomicLoadBalancer(List<String> serverList) {
        this.serverList = serverList;
    }
    public String getServer() {
        int index = counter.getAndIncrement() % serverList.size();
        return serverList.get(index);
    }
}
