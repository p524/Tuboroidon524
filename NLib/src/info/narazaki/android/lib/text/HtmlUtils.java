package info.narazaki.android.lib.text;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * HTMLに関するユーティリティ関数群
 * 
 * @author H.Narazaki
 */
public class HtmlUtils {
    private static final Pattern strip_br_pattern_ = Pattern.compile("<br.*?\\>", Pattern.CASE_INSENSITIVE
            | Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern strip_tag_pattern = Pattern.compile("<.*?>", Pattern.MULTILINE | Pattern.DOTALL);
    
    private static final Pattern need_escaping_pattern = Pattern.compile("[<\\>&\"\']", Pattern.MULTILINE
            | Pattern.DOTALL);
    
    private static HashMap<String, String> html_entities;
    static {
        html_entities = new HashMap<String, String>();
        // 基本
        html_entities.put("&apos;", "'");
        html_entities.put("&lt;", "<");
        html_entities.put("&gt;", ">");
        html_entities.put("&amp;", "&");
        html_entities.put("&quot;", "\"");
        html_entities.put("&nbsp;", " ");
        //
        html_entities.put("&iexcl;", "¡");
        html_entities.put("&cent;", "¢");
        html_entities.put("&pound;", "£");
        html_entities.put("&curren;", "¤");
        html_entities.put("&yen;", "¥");
        html_entities.put("&brvbar;", "¦");
        html_entities.put("&sect;", "§");
        html_entities.put("&uml;", "¨");
        html_entities.put("&copy;", "©");
        html_entities.put("&ordf;", "ª");
        html_entities.put("&laquo;", "«");
        html_entities.put("&not;", "¬");
        html_entities.put("&shy;", "­");
        html_entities.put("&reg;", "®");
        html_entities.put("&macr;", "¯");
        html_entities.put("&deg;", "°");
        html_entities.put("&plusmn;", "±");
        html_entities.put("&sup2;", "²");
        html_entities.put("&sup3;", "³");
        html_entities.put("&acute;", "´");
        html_entities.put("&micro;", "µ");
        html_entities.put("&para;", "¶");
        html_entities.put("&middot;", "·");
        html_entities.put("&cedil;", "¸");
        html_entities.put("&sup1;", "¹");
        html_entities.put("&ordm;", "º");
        html_entities.put("&raquo;", "»");
        html_entities.put("&frac14;", "¼");
        html_entities.put("&frac12;", "½");
        html_entities.put("&frac34;", "¾");
        html_entities.put("&iquest;", "¿");
        html_entities.put("&Agrave;", "À");
        html_entities.put("&Aacute;", "Á");
        html_entities.put("&Acirc;", "Â");
        html_entities.put("&Atilde;", "Ã");
        html_entities.put("&Auml;", "Ä");
        html_entities.put("&Aring;", "Å");
        html_entities.put("&AElig;", "Æ");
        html_entities.put("&Ccedil;", "Ç");
        html_entities.put("&Egrave;", "È");
        html_entities.put("&Eacute;", "É");
        html_entities.put("&Ecirc;", "Ê");
        html_entities.put("&Euml;", "Ë");
        html_entities.put("&Igrave;", "Ì");
        html_entities.put("&Iacute;", "Í");
        html_entities.put("&Icirc;", "Î");
        html_entities.put("&Iuml;", "Ï");
        html_entities.put("&ETH;", "Ð");
        html_entities.put("&Ntilde;", "Ñ");
        html_entities.put("&Ograve;", "Ò");
        html_entities.put("&Oacute;", "Ó");
        html_entities.put("&Ocirc;", "Ô");
        html_entities.put("&Otilde;", "Õ");
        html_entities.put("&Ouml;", "Ö");
        html_entities.put("&times;", "×");
        html_entities.put("&Oslash;", "Ø");
        html_entities.put("&Ugrave;", "Ù");
        html_entities.put("&Uacute;", "Ú");
        html_entities.put("&Ucirc;", "Û");
        html_entities.put("&Uuml;", "Ü");
        html_entities.put("&Yacute;", "Ý");
        html_entities.put("&THORN;", "Þ");
        html_entities.put("&szlig;", "ß");
        html_entities.put("&agrave;", "à");
        html_entities.put("&aacute;", "á");
        html_entities.put("&acirc;", "â");
        html_entities.put("&atilde;", "ã");
        html_entities.put("&auml;", "ä");
        html_entities.put("&aring;", "å");
        html_entities.put("&aelig;", "æ");
        html_entities.put("&ccedil;", "ç");
        html_entities.put("&egrave;", "è");
        html_entities.put("&eacute;", "é");
        html_entities.put("&ecirc;", "ê");
        html_entities.put("&euml;", "ë");
        html_entities.put("&igrave;", "ì");
        html_entities.put("&iacute;", "í");
        html_entities.put("&icirc;", "î");
        html_entities.put("&iuml;", "ï");
        html_entities.put("&eth;", "ð");
        html_entities.put("&ntilde;", "ñ");
        html_entities.put("&ograve;", "ò");
        html_entities.put("&oacute;", "ó");
        html_entities.put("&ocirc;", "ô");
        html_entities.put("&otilde;", "õ");
        html_entities.put("&ouml;", "ö");
        html_entities.put("&divide;", "÷");
        html_entities.put("&oslash;", "ø");
        html_entities.put("&ugrave;", "ù");
        html_entities.put("&uacute;", "ú");
        html_entities.put("&ucirc;", "û");
        html_entities.put("&uuml;", "ü");
        html_entities.put("&yacute;", "ý");
        html_entities.put("&thorn;", "þ");
        html_entities.put("&yuml;", "ÿ");
        html_entities.put("&OElig;", "Œ");
        html_entities.put("&oelig;", "œ");
        html_entities.put("&Scaron;", "Š");
        html_entities.put("&scaron;", "š");
        html_entities.put("&Yuml;", "Ÿ");
        html_entities.put("&circ;", "ˆ");
        html_entities.put("&tilde;", "˜");
        html_entities.put("&fnof;", "ƒ");
        html_entities.put("&Alpha;", "Α");
        html_entities.put("&Beta;", "Β");
        html_entities.put("&Gamma;", "Γ");
        html_entities.put("&Delta;", "Δ");
        html_entities.put("&Epsilon;", "Ε");
        html_entities.put("&Zeta;", "Ζ");
        html_entities.put("&Eta;", "Η");
        html_entities.put("&Theta;", "Θ");
        html_entities.put("&Iota;", "Ι");
        html_entities.put("&Kappa;", "Κ");
        html_entities.put("&Lambda;", "Λ");
        html_entities.put("&Mu;", "Μ");
        html_entities.put("&Nu;", "Ν");
        html_entities.put("&Xi;", "Ξ");
        html_entities.put("&Omicron;", "Ο");
        html_entities.put("&Pi;", "Π");
        html_entities.put("&Rho;", "Ρ");
        html_entities.put("&Sigma;", "Σ");
        html_entities.put("&Tau;", "Τ");
        html_entities.put("&Upsilon;", "Υ");
        html_entities.put("&Phi;", "Φ");
        html_entities.put("&Chi;", "Χ");
        html_entities.put("&Psi;", "Ψ");
        html_entities.put("&Omega;", "Ω");
        html_entities.put("&alpha;", "α");
        html_entities.put("&beta;", "β");
        html_entities.put("&gamma;", "γ");
        html_entities.put("&delta;", "δ");
        html_entities.put("&epsilon;", "ε");
        html_entities.put("&zeta;", "ζ");
        html_entities.put("&eta;", "η");
        html_entities.put("&theta;", "θ");
        html_entities.put("&iota;", "ι");
        html_entities.put("&kappa;", "κ");
        html_entities.put("&lambda;", "λ");
        html_entities.put("&mu;", "μ");
        html_entities.put("&nu;", "ν");
        html_entities.put("&xi;", "ξ");
        html_entities.put("&omicron;", "ο");
        html_entities.put("&pi;", "π");
        html_entities.put("&rho;", "ρ");
        html_entities.put("&sigmaf;", "ς");
        html_entities.put("&sigma;", "σ");
        html_entities.put("&tau;", "τ");
        html_entities.put("&upsilon;", "υ");
        html_entities.put("&phi;", "φ");
        html_entities.put("&chi;", "χ");
        html_entities.put("&psi;", "ψ");
        html_entities.put("&omega;", "ω");
        html_entities.put("&thetasym;", "ϑ");
        html_entities.put("&upsih;", "ϒ");
        html_entities.put("&piv;", "ϖ");
        html_entities.put("&ndash;", "–");
        html_entities.put("&mdash;", "—");
        html_entities.put("&lsquo;", "‘");
        html_entities.put("&rsquo;", "’");
        html_entities.put("&sbquo;", "‚");
        html_entities.put("&ldquo;", "“");
        html_entities.put("&rdquo;", "”");
        html_entities.put("&bdquo;", "„");
        html_entities.put("&dagger;", "†");
        html_entities.put("&Dagger;", "‡");
        html_entities.put("&bull;", "•");
        html_entities.put("&hellip;", "…");
        html_entities.put("&permil;", "‰");
        html_entities.put("&prime;", "′");
        html_entities.put("&Prime;", "″");
        html_entities.put("&lsaquo;", "‹");
        html_entities.put("&rsaquo;", "›");
        html_entities.put("&oline;", "‾");
        html_entities.put("&frasl;", "⁄");
        html_entities.put("&euro;", "€");
        html_entities.put("&image;", "ℑ");
        html_entities.put("&ewierp;", "℘");
        html_entities.put("&real;", "ℜ");
        html_entities.put("&trade;", "™");
        html_entities.put("&alefsym;", "ℵ");
        html_entities.put("&larr;", "←");
        html_entities.put("&uarr;", "↑");
        html_entities.put("&rarr;", "→");
        html_entities.put("&darr;", "↓");
        html_entities.put("&harr;", "↔");
        html_entities.put("&crarr;", "↵");
        html_entities.put("&lArr;", "⇐");
        html_entities.put("&uArr;", "⇑");
        html_entities.put("&rArr;", "⇒");
        html_entities.put("&dArr;", "⇓");
        html_entities.put("&hArr;", "⇔");
        html_entities.put("&forall;", "∀");
        html_entities.put("&part;", "∂");
        html_entities.put("&exist;", "∃");
        html_entities.put("&empty;", "∅");
        html_entities.put("&nabla;", "∇");
        html_entities.put("&isin;", "∈");
        html_entities.put("&notin;", "∉");
        html_entities.put("&ni;", "∋");
        html_entities.put("&prod;", "∏");
        html_entities.put("&sum;", "∑");
        html_entities.put("&minus;", "−");
        html_entities.put("&lowast;", "∗");
        html_entities.put("&radic;", "√");
        html_entities.put("&prop;", "∝");
        html_entities.put("&infin;", "∞");
        html_entities.put("&ang;", "∠");
        html_entities.put("&and;", "∧");
        html_entities.put("&or;", "∨");
        html_entities.put("&cap;", "∩");
        html_entities.put("&cup;", "∪");
        html_entities.put("&int;", "∫");
        html_entities.put("&there4;", "∴");
        html_entities.put("&sim;", "∼");
        html_entities.put("&cong;", "≅");
        html_entities.put("&asymp;", "≈");
        html_entities.put("&ne;", "≠");
        html_entities.put("&equiv;", "≡");
        html_entities.put("&le;", "≤");
        html_entities.put("&ge;", "≥");
        html_entities.put("&sub;", "⊂");
        html_entities.put("&sup;", "⊃");
        html_entities.put("&nsub;", "⊄");
        html_entities.put("&sube;", "⊆");
        html_entities.put("&supe;", "⊇");
        html_entities.put("&oplus;", "⊕");
        html_entities.put("&otimes;", "⊗");
        html_entities.put("&perp;", "⊥");
        html_entities.put("&sdot;", "⋅");
        html_entities.put("&lceil;", "⌈");
        html_entities.put("&rceil;", "⌉");
        html_entities.put("&lfloor;", "⌊");
        html_entities.put("&rfloor;", "⌋");
        html_entities.put("&lang;", "〈");
        html_entities.put("&rang;", "〉");
        html_entities.put("&loz;", "◊");
        html_entities.put("&spades;", "♠");
        html_entities.put("&clubs;", "♣");
        html_entities.put("&hearts;", "♥");
        html_entities.put("&diams;", "♦");
    }
    
    public static String escapeHtml(String orig) {
        return escapeHtml(orig, false);
    }
    
    public static String escapeHtml(String orig, boolean escape_single_quote) {
        if (!need_escaping_pattern.matcher(orig).find()) return orig;
        if(true)return orig;
        
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < orig.length(); i++) {
            char c = orig.charAt(i);
            switch (c) {
            case '<':
                buf.append("&lt;");
                break;
            case '>':
                buf.append("&gt;");
                break;
            
            case '&':
                buf.append("&amp;");
                break;
            case '"':
                buf.append("&quot;");
                break;
            case '\'':
                if (escape_single_quote) {
                    buf.append("&apos;");
                }
                else {
                    buf.append(c);
                }
                break;
            default:
                buf.append(c);
            }
        }
        return buf.toString();
    }
    
    public static String unescapeHtml(String orig) {
        if (orig.indexOf("&") == -1) return orig;
        
        StringBuilder result = new StringBuilder();
        
        int i;
        int j;
        int pos = 0;
        while (true) {
            i = orig.indexOf("&", pos);
            if (i == -1) break;
            
            j = orig.indexOf(";", i);
            if (j == -1) break;
            
            String entity = orig.substring(i, j + 1);
            
            String value = "";
            try {
            if(entity.startsWith("&#x") || entity.startsWith("&#X")) {
                // 数値文字参照(16進数)
                char c = (char) Integer.parseInt(entity.substring(3, entity.length() - 1), 16);
                value = new String(new char[] { c });
            }
            else if(entity.startsWith("&#")) {
                // 数値文字参照
                char c = (char) Integer.parseInt(entity.substring(2, entity.length() - 1));
                value = new String(new char[] { c });
            }else if(html_entities.containsKey(entity)) {
            	value = (String) html_entities.get(entity);
            }else{
                result.append(orig.substring(pos, j + 1));
                pos = j + 1;
                continue;
            }
            }
            catch (NumberFormatException e) {
                value = "";
            }
            
           
            result.append(orig.substring(pos, i));
            result.append(value);
            pos = j + 1;
        }
        result.append(orig.substring(pos));
        
        return result.toString();
    }
    
    public static String stripAllHtmls(String orig, boolean conv_br) {
        if (orig.indexOf('<') != -1) {
            if (conv_br) {
                orig = strip_br_pattern_.matcher(orig).replaceAll("\n");
            }
            orig = strip_tag_pattern.matcher(orig).replaceAll("");
        }
        return unescapeHtml(orig);
    }
    
}
