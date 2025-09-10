package string;

class Solution {

    public static void main(String [] args){
        Solution s=new Solution();
        s.minWindow("ADOBECODEBANC","ABC");
    }
    public String minWindow(String s, String t) {
        int [] map=new int[128];
        for (char c:t.toCharArray()){
            map[c]++;
        }
        int count=t.length();
        char [] strArr=s.toCharArray();
        int left=0;
        int minL=Integer.MAX_VALUE;
        int minS=0;
        for(int i=0;i<strArr.length;i++){
            char c = strArr[i];
            if(map[c]-- > 0){
                count--;
            }
            while(count==0){
                if(i-left+1<minL){
                    minL=i-left+1;
                    minS=left;
                }
                if(map[strArr[left]]++==0){
                    count++;
                }
                left++;
            }
        }
        return minL==Integer.MAX_VALUE?"":s.substring(minS, minS + minL);
    }

}
