package test;

/**
 * Created by usr0101862 on 2016/06/04.
 */
import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

public class TokenizerExample {
    public static void main(String[] args) {
        Tokenizer tokenizer = Tokenizer.builder().build();
        for (Token token : tokenizer.tokenize("寿司が食べたい。")) {
            System.out.println(token.getSurfaceForm() + "\t" + token.getAllFeatures());
        }
    }
}
