package string;

public class KthLargest {

    public static void main(String [] args){
        KthLargest kth=new KthLargest();
        int [] arr=new int[]{1,2,6,9,7};
        kth.getLargest(arr,2);
    }

    private int getLargest(int [] arr,int k){
        if(k>arr.length)
            return -1;
        int n=arr.length;
        int max=Integer.MIN_VALUE;
        int min=Integer.MAX_VALUE;
        for(int i=0;i<n;i++)
        {
             min=Math.min(min,arr[i]);
             max=Math.max(max,arr[i]);

        }
        int [] count=new int[max-min+1];
        for(int i=0;i<n;i++){
            count[arr[i]-min]++;
        }
        int remain=k;
        for(int i=max-min;i>=0;i++){
           remain-=arr[i];
           if(remain<=0)
               return i+min;
        }
        return -1;
    }
}
