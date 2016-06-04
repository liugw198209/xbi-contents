package test;

/**
 * Created by usr0101862 on 2016/06/04.
 */
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


public class KuromojiTest {

    /**
     * 分かち書きした文章をリストにして返す。
     *
     * @param src
     * @return
     */
    private List<String> kuromojineologd(String src){
        List<String> ret = new ArrayList<>();
        try(JapaneseTokenizer jt =
                    // JapaneseTokenizerの引数は(元データ, ユーザー辞書, 記号を無視するか, モード)
                    new JapaneseTokenizer(new StringReader(src), null, false, JapaneseTokenizer.DEFAULT_MODE)){
            CharTermAttribute ct = jt.addAttribute(CharTermAttribute.class);
            jt.reset();
            while(jt.incrementToken()){
                ret.add(ct.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * main
     * 自分をnewしてexecuteメソッド呼ぶだけ。
     * こうすることで他メソッドを作成するときに無駄にstatic化しなくてすむ
     *
     * @param args
     */
    public static void main(String[] args) {
        new KuromojiTest().execute();
    }

    /**
     * 事実上のmain
     */
    public void execute(){
        System.out.println(kuromojineologd("東京スカイツリーできゃりーぱみゅぱみゅとDAIGOがロケしてた。"));
        System.out.println(kuromojineologd("ヨツンヴァインになれよ。あくしろよ。"));
    }
}