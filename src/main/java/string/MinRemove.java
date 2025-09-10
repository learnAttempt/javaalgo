package string;

public class MinRemove {

    public static void main(String [] args){
        MinRemove m=new MinRemove();
        String res= m.minRemoveToMakeValid("(((()))");
        System.out.println(res);
    }
    public String minRemoveToMakeValid(String s) {
        int balance=0;
        int openSeen=0;
        StringBuilder sb=new StringBuilder();
        for(int i =0;i<s.length();i++){
            char c=s.charAt(i);
            if(c == '('){
                openSeen++;
                balance++;

            }else if(c== ')'){
                if(balance==0) continue;
                balance--;
            }
            sb.append(c);
        }
        StringBuilder result = new StringBuilder();
        int openKeep=openSeen-balance;
        for(int i=0;i<sb.length();i++){
            char c=sb.charAt(i);
            if(c=='('){
                openKeep--;
                if(openKeep<0)continue;
            }
            result.append(c);
        }
        return result.toString();
    }
}
