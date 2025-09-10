package string;

public class RepeatedCharacter {

    public static void  main(String[] args){
        RepeatedCharacter rc=new RepeatedCharacter();
        rc.repeatedCharacter("abccbaacz");
    }
    public char repeatedCharacter(String s) {
        long hash=0;
        long bit=0;
        for(char ch:s.toCharArray()){
            bit= 1L << (ch-'a');
            long val=hash&bit;
            if(val==0);
            if((hash&bit)!=0)
                return ch;
            hash |=bit;
        }
        return ' ';
    }
}
