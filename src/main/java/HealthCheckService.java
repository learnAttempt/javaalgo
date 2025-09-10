import com.sun.net.httpserver.HttpHandler;

import java.io.ObjectInputFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthCheckService {

   /* private HttpHandler httpHandler;
    private ObjectInputFilter.Config config;
    private ExecutorService executor;

    public HealthCheckService(HttpHandler httpHandler, ObjectInputFilter.Config config) {
        this.httpHandler = httpHandler;
        this.configuration = configuration;
        int size = config.servers.size();
        this.executor = Executors.newFixedThreadPool(size);
    }

    public List<HealthCheckResult> getHealthCheck() throws Exception {
        List<Callable<HealthCheck>> tasks = prepareTasks(config.uri);
        List<Future<HealthCheck>> futures = executor.invokeAll(tasks);
        List<HealthCheck> output = new ArrayList<>();
        for (Future<HealthCheckResult> future: futures) {
            output.add(future.get());
        }
        return output;
    }

    private List<Callable<HealthCheck>> prepareTasks(String api) {
        List<Callable<HealthCheckResult>> tasks = new ArrayList();
        for (String server: config.servers) {
            tasks.add(() -> httpHandler.getStatus(api));
        }
        return tasks;
    }*/
}

