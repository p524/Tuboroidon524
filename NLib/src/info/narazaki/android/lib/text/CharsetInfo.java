package info.narazaki.android.lib.text;

import java.nio.charset.Charset;

public class CharsetInfo {
    static private String text_encode = Charset.isSupported("x-docomo-shift_jis-2007") ? "x-docomo-shift_jis-2007" : "MS932";
    
    public static String getEmojiShiftJis() {
        return text_encode;
    }	
}
