package airbnb;

import java.util.*;
/*
Imagine you're tasked with optimizing Airbnb's booking system to better
accommodate group bookings. For example, a group of 10 people is planning to travel to
SoHo NYC, and there are no single properties that can accommodate 10 guests.
The system should recommend multiple properties within the same neighborhood
so the group can stay close together. The goal is to suggest the optimal combination
of neighboring properties for groups wanting to stay close to each other,
aiming to minimize the total number of properties used.
You are given a list of properties, each described by an
object that includes the following attributes: ID (a unique numeric identifier),
neighborhood (a string), and capacity (an integer representing how many guests can be accommodated). Write an algorithm that suggests properties for a specific group size in a specific neighborhood.
 The solution should prioritize minimizing the number of properties and capacity used while ensuring all group members are accommodated. Properties are considered 'neighboring' if they are in the same neighborhood.
  # Schema Property(id, neighborhood, capacity) # Example 1 (Single property can accomodate everyone) ## Input Properties = [ Property(1, "downtown", 5), Property(2, "downtown", 3), Property(3, "downtown", 1), Property(4, "uptown", 4), Property(5, "uptown", 2) ] Neighborhood = "downtown" GroupSize: 4 ## Output RecommendedProperties = [1] # Example 2 (Multiple properties needed) ## Input Properties = [ Property(1, "downtown", 5), Property(2, "downtown", 3), Property(3, "downtown", 1), Property(4, "uptown", 4), Property(5, "uptown", 2) ] Neighborhood = "downtown" GroupSize: 6 ## Output RecommendedProperties = [1, 3] */





class Property {
    int id;
    String neighborhood;
    int capacity;

    Property(int id, String neighborhood, int capacity) {
        this.id = id;
        this.neighborhood = neighborhood;
        this.capacity = capacity;
    }
}

public class AirbnbGroupBookingDP {

    static List<Integer> recommendProperties(List<Property> properties, String neighborhood, int groupSize) {
        // Filter only target neighborhood
        List<Property> candidates = new ArrayList<>();
        for (Property p : properties) {
            if (p.neighborhood.equals(neighborhood)) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) return Collections.emptyList();

        // Upper bound: sum of all capacities in this neighborhood
        int maxCapacity = candidates.stream().mapToInt(p -> p.capacity).sum();

        // dp[c] = best subset that achieves capacity exactly c
        @SuppressWarnings("unchecked")
        List<Property>[] dp = new List[maxCapacity + 1];
        dp[0] = new ArrayList<>(); // base: empty set

        for (Property p : candidates) {
            // process in reverse to avoid reusing same property
            for (int c = maxCapacity; c >= 0; c--) {
                if (dp[c] != null) {
                    int newCap = c + p.capacity;
                    if (newCap > maxCapacity) continue;
                    List<Property> newList = new ArrayList<>(dp[c]);
                    newList.add(p);

                    if (dp[newCap] == null ||
                            isBetter(newList, dp[newCap], groupSize)) {
                        dp[newCap] = newList;
                    }
                }
            }
        }

        // Find best dp[c] where c >= groupSize
        List<Property> best = null;
        for (int c = groupSize; c <= maxCapacity; c++) {
            if (dp[c] != null) {
                if (best == null || isBetter(dp[c], best, groupSize)) {
                    best = dp[c];
                }
            }
        }

        if (best == null) return Collections.emptyList();

        List<Integer> result = new ArrayList<>();
        for (Property p : best) {
            result.add(p.id);
        }
        return result;
    }

    // Comparison rule:
    // 1. Fewer properties is better
    // 2. If tie, smaller total capacity is better (min over-allocation)
    private static boolean isBetter(List<Property> a, List<Property> b, int groupSize) {
        if (b == null) return true;

        if (a.size() != b.size()) {
            return a.size() < b.size();
        }
        int sumA = a.stream().mapToInt(p -> p.capacity).sum();
        int sumB = b.stream().mapToInt(p -> p.capacity).sum();
        return sumA < sumB;
    }

    // ---------------- DEMO ----------------
    public static void main(String[] args) {
        List<Property> props = Arrays.asList(
                new Property(1, "downtown", 5),
                new Property(2, "downtown", 3),
                new Property(3, "downtown", 1),
                new Property(4, "uptown", 4),
                new Property(5, "uptown", 2)
        );

        System.out.println(recommendProperties(props, "downtown", 4)); // [1]
        System.out.println(recommendProperties(props, "downtown", 6)); // [1, 3]
        System.out.println(recommendProperties(props, "uptown", 5));   // [4, 5]
    }
}
