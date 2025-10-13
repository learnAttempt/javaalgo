package linkedin;

import java.util.*;

public class WordLadderWithAddDelete {

    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> wordSet = new HashSet<>(wordList);
        if (!wordSet.contains(endWord)) {
            return 0; // Target word not in the dictionary
        }

        Queue<String> queue = new LinkedList<>();
        queue.offer(beginWord);
        wordSet.remove(beginWord); // Mark beginWord as visited

        int level = 1;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String currentWord = queue.poll();

                if (currentWord.equals(endWord)) {
                    return level;
                }

                // Try all possible transformations (change, add, delete)
                for (String nextWord : getTransformations(currentWord, wordSet)) {
                    if (wordSet.contains(nextWord)) {
                        queue.offer(nextWord);
                        wordSet.remove(nextWord); // Mark as visited
                    }
                }
            }
            level++;
        }
        return 0; // No path found
    }

    private Set<String> getTransformations(String word, Set<String> wordSet) {
        Set<String> transformations = new HashSet<>();

        // 1. Change a character
        char[] charArray = word.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char originalChar = charArray[i];
            for (char c = 'a'; c <= 'z'; c++) {
                if (c == originalChar) continue;
                charArray[i] = c;
                String changedWord = new String(charArray);
                if (wordSet.contains(changedWord)) {
                    transformations.add(changedWord);
                }
            }
            charArray[i] = originalChar; // Backtrack
        }

        // 2. Add a character
        for (int i = 0; i <= word.length(); i++) {
            for (char c = 'a'; c <= 'z'; c++) {
                String addedWord = word.substring(0, i) + c + word.substring(i);
                if (wordSet.contains(addedWord)) {
                    transformations.add(addedWord);
                }
            }
        }

        // 3. Delete a character
        if (word.length() > 1) { // Cannot delete from a single-character word
            for (int i = 0; i < word.length(); i++) {
                String deletedWord = word.substring(0, i) + word.substring(i + 1);
                if (wordSet.contains(deletedWord)) {
                    transformations.add(deletedWord);
                }
            }
        }

        return transformations;
    }

    public static void main(String[] args) {
        WordLadderWithAddDelete solver = new WordLadderWithAddDelete();
        List<String> wordList = Arrays.asList("hot", "dot", "dog", "lot", "log", "cog", "ho", "do", "dogg");
        String beginWord = "hit";
        String endWord = "cog";

        int length = solver.ladderLength(beginWord, endWord, wordList);
        System.out.println("Shortest ladder length: " + length); // Expected: 5 (hit -> hot -> dot -> dog -> cog)

        beginWord = "cat";
        endWord = "dog";
        wordList = Arrays.asList("cot", "dot", "cog", "dog", "ca", "do");
        length = solver.ladderLength(beginWord, endWord, wordList);
        System.out.println("Shortest ladder length: " + length); // Expected: 3 (cat -> cot -> dot -> dog) or (cat -> ca -> do -> dog)
    }
}