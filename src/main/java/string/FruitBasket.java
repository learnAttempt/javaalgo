package string;

public class FruitBasket {
    public static void main(String [] args) {
        FruitBasket s = new FruitBasket();
        s.totalFruit(new int[]{1,2,1});
    }
        public int totalFruit(int[] fruits) {
        int n=fruits.length;
        int [] map=new int[n+1];
        int countDistinct=0;
        int start=0;
        int maxLength=0;
        for(int i=0;i<n;i++){
            if(map[fruits[i]]++==0)
                countDistinct++;
            while(countDistinct>2){
                if(map[fruits[start++]]--==1)
                    countDistinct--;
            }
            maxLength=Math.max(maxLength,i-start+1);

        }
        return maxLength;
    }
}