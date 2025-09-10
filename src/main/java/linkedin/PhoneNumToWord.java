package linkedin;
/**LINKEDIN
 * I was asked this question in the interview:
 * Given the standard mapping from English letters to digits on a phone keypad (1 → "" 2 -> a,b,c 3 -> d,e,f 4 -> g,h,i 5 -> j,k,l 6 -> m,n,o 7 -> p,q,r,s 8 -> t,u,v 9 -> w,x,y,z),
 *
 * write a program that outputs all words that can be formed from any n-digit phone number from the list of given KNOWN_WORDS considering the mapping mentioned above.
 *
 * KNOWN_WORDS= ['careers', 'linkedin', 'hiring', 'interview', 'linkedgo']
 *
 * phoneNumber: 2273377
 * Output: ['careers']
 *
 * phoneNumber: 54653346
 * Output: ['linkedin', 'linkedgo']
 */

import java.util.*;

public class PhoneNumToWord {

   private static final Map<Character,String> DIGIT_TO_CHAR=new HashMap<>();
    private static final Map<Character,Character> CHAR_TO_DIGIT=new HashMap<>();
    static {
        DIGIT_TO_CHAR.put('2', "abc");
        DIGIT_TO_CHAR.put('3', "def");
        DIGIT_TO_CHAR.put('4', "ghi");
        DIGIT_TO_CHAR.put('5', "jkl");
        DIGIT_TO_CHAR.put('6', "mno");
        DIGIT_TO_CHAR.put('7', "pqrs");
        DIGIT_TO_CHAR.put('8', "tuv");
        DIGIT_TO_CHAR.put('9', "wxyz");

        for (Map.Entry<Character, String> entry : DIGIT_TO_CHAR.entrySet()) {
            char digit = entry.getKey();
            for (char ch : entry.getValue().toCharArray()) {
                CHAR_TO_DIGIT.put(ch, digit);
            }
        }

        //DIGIT_TO_CHAR.entrySet().stream().map(entry->DIGIT_TO_CHAR.put())
    
    }

    private String wordToDigit(String word){
        StringBuilder sb=new StringBuilder();
        for(char ch:word.toCharArray()){
            if(CHAR_TO_DIGIT.containsKey(ch))
                sb.append(CHAR_TO_DIGIT.get(ch));
        }
        return sb.toString();
    }


    public  List<String> findMatchingWords(String phoneNum){
        List<String> matchingWords=new ArrayList<>();
        if(phoneNum.isEmpty() )
            return null;

        for(String word:knownWords){
            if(word.length()!=phoneNum.length())
                continue;
            if(wordToDigit(word).equals(phoneNum))
                matchingWords.add(word);
        }
        return matchingWords;

    }

    private static final List<String> knownWords = Arrays.asList(
            "careers", "linkedin", "hiring", "interview", "linkedgo"
    );
    public static void main(String [] args){
        PhoneNumToWord ph=new PhoneNumToWord();
        String phoneNumber="54653346";
        System.out.println(ph.findMatchingWords(phoneNumber));
    }

}
