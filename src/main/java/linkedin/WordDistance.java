package linkedin;
import java.util.*;
/* https://leetcode.com/problems/shortest-word-distance-ii
*For the scenario if one string's occurences are less than other, used binary search for the more frequent string.
* */

class WordDistance {
    private final Map<String, List<Integer>> wordMap;

    public WordDistance(String[] wordsDict) {
        wordMap = new HashMap<>();
        for (int i = 0; i < wordsDict.length; i++) {
            wordMap.computeIfAbsent(wordsDict[i], k -> new ArrayList<>()).add(i);
        }
    }

    public int shortest(String word1, String word2) {
        List<Integer> list1 = wordMap.get(word1);
        List<Integer> list2 = wordMap.get(word2);
        if (list1 == null || list2 == null) return -1;

        // Always iterate over the smaller list
        if (list1.size() > list2.size()) {
            List<Integer> temp = list1;
            list1 = list2;
            list2 = temp;
        }

        // Choose binary search when frequency difference is large
        if ((double) list2.size() / list1.size() < 4.0)
            return shortestTwoPointer(list1, list2);
        else
            return shortestBinarySearch(list1, list2);
    }

    // ------------------ Two-pointer approach ------------------
    private int shortestTwoPointer(List<Integer> list1, List<Integer> list2) {
        int i = 0, j = 0, minDist = Integer.MAX_VALUE;
        while (i < list1.size() && j < list2.size()) {
            int idx1 = list1.get(i);
            int idx2 = list2.get(j);
            minDist = Math.min(minDist, Math.abs(idx1 - idx2));
            if (idx1 < idx2) i++;
            else j++;
        }
        return minDist;
    }

    // ------------------ Manual Binary Search approach ------------------
    private int shortestBinarySearch(List<Integer> small, List<Integer> large) {
        int minDist = Integer.MAX_VALUE;
        for (int idx : small) {
            int pos = lowerBound(large, idx);  // custom binary search
            if (pos < large.size())
                minDist = Math.min(minDist, Math.abs(large.get(pos) - idx));
            if (pos > 0)
                minDist = Math.min(minDist, Math.abs(large.get(pos - 1) - idx));
        }
        return minDist;
    }

    // Custom implementation of lower_bound (first element >= target)
    private int lowerBound(List<Integer> list, int target) {
        int left = 0, right = list.size(); // right is exclusive
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (list.get(mid) < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left; // insertion position
    }
}
