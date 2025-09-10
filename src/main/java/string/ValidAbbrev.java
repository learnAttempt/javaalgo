package string;

public class ValidAbbrev {
    public static void main(String [] args){
        ValidAbbrev s=new ValidAbbrev();
        boolean res=s.validWordAbbreviation("internationalization","i12iz4n");
    System.out.println(res);
    }

    public boolean validWordAbbreviation(String word, String abbr) {
        int i=0,j=0;
        while(i<word.length()&& j<abbr.length()){
            if(word.charAt(i)!=abbr.charAt(j)){
                if(Character.isDigit(abbr.charAt(j))){
                    if (abbr.charAt(j)==0)
                        return false;
                    //int start=j;
                    int num=0;
                  //  int k=0;
                    while(Character.isDigit(abbr.charAt(j))){
                        num=num*10+(abbr.charAt(j)-'0');
                        //k++;
                        j++;

                    }
                    i+=num;
                }
                else{
                    return false;
                }
            }
            else{
                i++;
                j++;
            }
        }
        return i==word.length()&& j==abbr.length();

    }
}
