package linkedin;

public class EditDistance1D {
    public static int minDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[] dp = new int[n + 1];

        // initialize first row (distance from "" to word2)
        for (int j = 0; j <= n; j++) {
            dp[j] = j;
        }

        for (int i = 1; i <= m; i++) {
            int prev = dp[0];  // dp[i-1][j-1]
            dp[0] = i;         // distance from word1[0..i) to ""

            for (int j = 1; j <= n; j++) {
                int temp = dp[j];  // store dp[i-1][j] before updating

                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[j] = prev;  // no operation
                } else {
                    dp[j] = Math.min(prev, Math.min(dp[j], dp[j - 1])) + 1;
                }

                prev = temp;  // move diagonal for next iteration
            }
        }

        return dp[n];
    }

    public static void main(String[] args) {
        System.out.println(minDistance("intention", "execution")); // Output: 5
    }
}
