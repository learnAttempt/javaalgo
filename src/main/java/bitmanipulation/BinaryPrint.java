package bitmanipulation;

import java.util.ArrayList;
import java.util.List;

public class BinaryPrint {

    public static void main(String [] args){
        int nums=11;
      //  pr_binary(nums);
        int [] arr={1,2,3};
        subsets(arr);
    }
    static void pr_binary(int num){
        System.out.println("***"+num);
        for(int i=10;i>=0;i--) {

            System.out.print("i"+i+"num  "+num+"    ");
            System.out.println((num >> i)&1);
        }
    }

    static String decToBinary(int n) {

        // String to store the binary representation
        StringBuilder bin = new StringBuilder();

        while (n > 0) {

            // Finding (n % 2) using bitwise AND operator
            // (n & 1) gives the least significant bit (LSB)
            int bit = n & 1;
            bin.append(bit);

            // Right shift n by 1 (equivalent to n = n / 2)
            // This removes the least significant bit (LSB)
            n = n >> 1;
        }

        return bin.reverse().toString();
    }

    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> output = new ArrayList();
        int n = nums.length;
        int nthBit = 1 << n;
        for (int i = 0; i < nthBit; ++i) {  // equivalent to (int)Math.pow(2, n)
            // generate bitmask, from 0..00 to 1..11
            String bitmask = Integer.toBinaryString(i | nthBit).substring(1);
            System.out.println("bit1 "+bitmask);
        }
        for (int i = (int) Math.pow(2, n); i < (int) Math.pow(2, n + 1); ++i) {
            // generate bitmask, from 0..00 to 1..11
            String bitmask = Integer.toBinaryString(i).substring(1);
            System.out.println("bit"+bitmask);
            // append subset corresponding to that bitmask
            List<Integer> curr = new ArrayList();
            for (int j = 0; j < n; ++j) {
                if (bitmask.charAt(j) == '1') curr.add(nums[j]);
            }
            output.add(curr);
        }
        return output;
    }
}

