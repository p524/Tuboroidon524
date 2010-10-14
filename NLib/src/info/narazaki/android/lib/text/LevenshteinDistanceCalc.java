package info.narazaki.android.lib.text;

/**
 * 2つの文字列についての編集距離計算機
 * 
 * @author H.Narazaki
 */
public class LevenshteinDistanceCalc {
    public static final int MAX_SIMILARITY_RATE = 65535;
    
    private int[] buf_;
    private int max_len_;
    
    /**
     * 編集距離計算機を初期化
     */
    public LevenshteinDistanceCalc() {
        createBuffer(1024);
    }
    
    /**
     * 編集距離計算機を初期化
     * 
     * バッファサイズは動的に変わるが、計算機を使いまわすなら GC発動を抑えるためある程度余裕のある数字にすると良い
     * 
     * @param max_len
     *            GC発動なしに計算可能な文字列の長さ
     */
    public LevenshteinDistanceCalc(int max_len) {
        createBuffer(max_len);
    }
    
    /**
     * 指定サイズでバッファを作成(または再作成)
     * 
     * @param max_len
     */
    private void createBuffer(int max_len) {
        max_len_ = max_len;
        buf_ = new int[max_len_ * 2 + 3];
    }
    
    /**
     * 2つの文字列について編集距離を計算する
     * 
     * 挿入・削除は1、置換について1か2かは実装による 最大はstr1.length() + str2.length()である
     * 
     * @param str1
     * @param str2
     * @return 編集距離
     */
    public int diff(String str1, String str2) {
        return calcONP(str1, str2);
    }
    
    /**
     * 2つの文字列について編集距離の「近さ」を計算する
     * 
     * 一致度が高いほど数値は大きくなり、完全一致で最大値、完全不一致で0、となる
     * 
     * @param str1
     * @param str2
     * @return 編集距離の近さ
     */
    public final int similar(String str1, String str2) {
        return str1.length() + str2.length() - diff(str1, str2);
    }
    
    /**
     * 2つの文字列について編集距離の「近さ」の割合を計算する
     * 
     * 最大距離からの割合で計算されるので MAX_SIMILARITY_RATE(完全一致)から0(完全不一致)までで表される
     * 
     * @param str1
     * @param str2
     * @return
     */
    public final int similarity(String str1, String str2) {
        int max = str1.length() + str2.length();
        return (max - diff(str1, str2)) * MAX_SIMILARITY_RATE / max;
    }
    
    protected int calcONP(String str1, String str2) {
        if (str1.length() > str2.length()) {
            // str1の方が長くなるようにswap!!
            String temp = str1;
            str1 = str2;
            str2 = temp;
        }
        
        int k;
        int p;
        
        int len1 = str1.length();
        int len2 = str2.length();
        
        int offset = len1 + 1;
        int delta = len2 - len1;
        
        // バッファ再作成
        if (len1 > max_len_) createBuffer(len1);
        
        for (int i = 0; i < len1 + len2 + 3; i++)
            buf_[i] = -1;
        
        for (p = 0; buf_[delta + offset] != len2; p++) {
            for (k = -p; k < delta; k++) {
                buf_[k + offset] = snakeONP(str1, str2, k,
                        java.lang.Math.max(buf_[k - 1 + offset] + 1, buf_[k + 1 + offset]));
            }
            for (k = delta + p; k > delta; k--) {
                buf_[k + offset] = snakeONP(str1, str2, k,
                        java.lang.Math.max(buf_[k - 1 + offset] + 1, buf_[k + 1 + offset]));
            }
            buf_[delta + offset] = snakeONP(str1, str2, delta,
                    java.lang.Math.max(buf_[delta - 1 + offset] + 1, buf_[delta + 1 + offset]));
        }
        return delta + (p - 1) * 2;
    }
    
    private int snakeONP(String str1, String str2, int k, int y) {
        int x = y - k;
        while (x < str1.length() && y < str2.length() && str1.charAt(x) == str2.charAt(y)) {
            x++;
            y++;
        }
        return y;
    }
    
}
