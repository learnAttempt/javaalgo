package dynamicprogram;

public class UnboundedKnapScack {

    public static void main (String args[]){
        UnboundedKnapScack unboundedKnapScack=new UnboundedKnapScack();
        int[] val = { 1, 1 };
        int[] wt = { 2, 1 };
        int capacity = 3;
        System.out.println(unboundedKnapScack.solveKnapsack(val,wt,capacity));
    }

    public int solveKnapsack(int [] val,int [] wt,int capacity){
        int[] dp =new int[capacity+1];
      /*  for(int i=0;i<wt.length;i++){
            for(int j=capacity;j>=0;j--){
                int take=0;
                if(wt[i]<=j){
                    take=val[i]+dp[j-wt[i]];
                }
                int noTake=dp[j];
                dp[j]=Math.max(take,noTake);
            }

        }
        return dp[capacity];*/
        for (int i = val.length - 1; i >= 0; i--) {
            for (int j = 1; j <= capacity; j++) {

                int take = 0;
                if (j - wt[i] >= 0) {
                    take = val[i] + dp[j - wt[i]];
                }
                int noTake = dp[j];

                dp[j] = Math.max(take, noTake);
            }
        }

        return dp[capacity];
    }
}
