package rippling;

import java.util.*;

public class CurrencyConverter {

    public static List<Double> calculateConversionRates(List<Object[]> rates, List<Object[]> queries) {
        // Build graph
        Map<String, Map<String, Double>> graph = new HashMap<>();
        for (Object[] rate : rates) {
            String from = (String) rate[0];
            String to = (String) rate[1];
            double value = (double) rate[2];
            graph.putIfAbsent(from, new HashMap<>());
            graph.putIfAbsent(to, new HashMap<>());
            graph.get(from).put(to, value);
            graph.get(to).put(from, 1/value);
        }

        // Perform DFS for each query
        List<Double> result = new ArrayList<>();
        for (Object[] query : queries) {
            String from = (String) query[0];
            String to = (String) query[1];
            Set<String> visited = new HashSet<>();
            double rate = dfs(graph, from, to, 1.0, visited);
            result.add(rate);
        }

        return result;
    }

    private static double dfs(Map<String, Map<String, Double>> graph, String start, String end, double value, Set<String> visited) {
        if (!graph.containsKey(start) || visited.contains(start)) {
            return -1.0;
        }
        if (start.equals(end)) {
            return value;
        }
        visited.add(start);
        Map<String, Double> neighbors = graph.get(start);
        for (String neighbor : neighbors.keySet()) {
            double rate = dfs(graph, neighbor, end, value * neighbors.get(neighbor), visited);
            if (rate != -1.0) {
                return rate;
            }
        }
        return -1.0;
    }
    public static void main(String[] args) {
        List<Object[]> rates = new ArrayList<>();
        rates.add(new Object[]{"USD", "JPY", 100.0});
        rates.add(new Object[]{"JPY", "CHN", 20.0});
        rates.add(new Object[]{"CHN", "THAI", 200.0});

        List<Object[]> queries = new ArrayList<>();
        queries.add(new Object[]{"USD", "CHN"});
        queries.add(new Object[]{"JPY", "THAI"});
        queries.add(new Object[]{"USD", "AUD"});

        List<Double> results = calculateConversionRates(rates, queries);
        System.out.println(results); // prints [1000.0, 4000.0, -1.0]
    }
}