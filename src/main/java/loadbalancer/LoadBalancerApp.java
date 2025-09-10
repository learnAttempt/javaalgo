package loadbalancer;

import org.junit.runner.Request;
import org.junit.runner.Runner;

import java.util.ArrayList;
import java.util.List;

// Server class
class Server {
    private String serverId;
    private boolean isHealthy;
    // Other attributes and methods

    public Server(String serverId) {
        this.serverId = serverId;
        this.isHealthy = true;
    }

    public String getServerId() {
        return serverId;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    // Other server operations
}

// LoadBalancer class (Singleton)
class LoadBalancer {
    private static LoadBalancer instance;
    private List<Server> servers;
    private LoadBalancingStrategy strategy;

    private LoadBalancer() {
        this.servers = new ArrayList<>();
    }

    public static LoadBalancer getInstance() {
        if (instance == null) {
            instance = new LoadBalancer();
        }
        return instance;
    }

    public void addServer(Server server) {
        servers.add(server);
    }

    public void removeServer(Server server) {
        servers.remove(server);
    }

    public Server getServer(Request request) {
        return strategy.getServer(servers, request);
    }

    public void setLoadBalancingStrategy(LoadBalancingStrategy strategy) {
        this.strategy = strategy;
    }

    // Other load balancer operations
}

// LoadBalancingStrategy interface
interface LoadBalancingStrategy {
    Server getServer(List<Server> servers, Request request);
}

// RoundRobinStrategy class
class RoundRobinStrategy implements LoadBalancingStrategy {
    private int currentIndex;

    public RoundRobinStrategy() {
        this.currentIndex = 0;
    }

    @Override
    public Server getServer(List<Server> servers, Request request) {
        int totalServers = servers.size();
        if (totalServers == 0) {
            throw new IllegalStateException("No servers available");
        }
        Server server = servers.get(currentIndex);
        currentIndex = (currentIndex + 1) % totalServers;
        return server;
    }
}

// LeastConnectionsStrategy class
class LeastConnectionsStrategy implements LoadBalancingStrategy {
    @Override
    public Server getServer(List<Server> servers, Request request) {
        int minConnections = Integer.MAX_VALUE;
        Server selectedServer = null;

        for (Server server : servers) {
            if (server.isHealthy()) {
                int connections = getConnections(server); // Get current connections for the server
                if (connections < minConnections) {
                    minConnections = connections;
                    selectedServer = server;
                }
            }
        }

        if (selectedServer == null) {
            throw new IllegalStateException("No healthy servers available");
        }
        return selectedServer;
    }

    private int getConnections(Server server) {
        // Perform logic to get current connections for the server
        return 0; // Placeholder for connections count
    }
}

// Main Class
public class LoadBalancerApp {
    public static void main(String[] args) {
        // Create servers
        Server server1 = new Server("server1");
        Server server2 = new Server("server2");

        // Create load balancer
        LoadBalancer loadBalancer = LoadBalancer.getInstance();
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);

        // Set load balancing strategy
        LoadBalancingStrategy roundRobinStrategy = new RoundRobinStrategy();
        loadBalancer.setLoadBalancingStrategy(roundRobinStrategy);

        // Create requests
        Request request1 = new Request1();
        Request request2 = new Request1();

        // Get server for request1
        Server selectedServer1 = loadBalancer.getServer(request1);
        System.out.println("Selected server for request1: " + selectedServer1.getServerId());

        // Get server for request2
        Server selectedServer2 = loadBalancer.getServer(request2);
        System.out.println("Selected server for request2: " + selectedServer2.getServerId());
    }
}

class Request1 extends Request{

    @Override
    public Runner getRunner() {
        return null;
    }
}