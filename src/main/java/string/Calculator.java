package string;

import java.util.ArrayList;
import java.util.List;

public class Calculator {
public static void main(String[] args){
    Calculator c=new Calculator();
    List<Integer> arr=new ArrayList<>();
   Integer[] res= arr.toArray(Integer[]::new);
    c.calculate("3+2*2");
}
    public int calculate(String s) {
        char[] letters = (s + "+").toCharArray();
        char prevOp = '+';
        int num = 0;
        int prev = 0;
        int res = 0;

        for (char ch : letters) {
            if (ch == ' ') {
                continue;
            }
            if (ch >= '0' && ch <= '9') {
                num = num * 10 + ch - '0';
            } else {
                if (prevOp == '+') {
                    res += prev;
                    prev = num;
                } else if (prevOp == '-') {
                    res += prev;
                    prev = -num;
                } else if (prevOp == '*') {
                    prev *= num;
                } else if (prevOp == '/') {
                    prev /= num;
                }
                prevOp = ch;
                num = 0;
            }
        }

        return res + prev;

    }
}
