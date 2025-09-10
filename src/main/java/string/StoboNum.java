package string;

public class StoboNum {

    public boolean isStrobogrammatic(String num) {
    char[] digits=new char[]{'0','1','\0','\0','\0','\0','9','\0','8','6'};

    int left=0;
    int right=num.length()-1;
        while(left<right){
        char leftC=num.charAt(left);
        char rightC=num.charAt(right);
        if(digits[leftC-'0']=='\0'|| digits[leftC-'0']!=rightC)
            return false;
        left++;
        right--;
    }
        return true;
}

public static void main(String [] args){
        StoboNum s=new StoboNum();
        s.isStrobogrammatic("69");
}
}
