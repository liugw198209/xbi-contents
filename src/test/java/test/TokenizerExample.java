package test;

/**
 * Created by usr0101862 on 2016/06/04.
 */
import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

public class TokenizerExample {
    public static void main(String[] args) {
        Tokenizer tokenizer = Tokenizer.builder()
                .mode(Tokenizer.Mode.NORMAL)
                .split(true)
                .build();
        for (Token token : tokenizer.tokenize("寿司を食べたい。C#,国際|関西空港と関西国際空港、LSTM深層学習")) {
            System.out.println(token.getSurfaceForm() + "\t" + token.getBaseForm() + "\t" + token.getPartOfSpeech() + "\t" +token.isKnown() + "\t" + token.getPosition());
            //System.out.println(token.getSurfaceForm() + "\t" + token.getAllFeatures());
        }
    }
}
