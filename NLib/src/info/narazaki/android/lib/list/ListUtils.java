package info.narazaki.android.lib.list;

import java.util.ArrayList;

public class ListUtils {
    public static ArrayList<String> split(String with, String orig) {
        ArrayList<String> result = new ArrayList<String>();
        if (orig.length() == 0 || with.length() == 0) {
            return result;
        }
        int token_len = with.length();
        int index = 0;
        while (true) {
            int new_index = orig.indexOf(with, index);
            if (new_index == -1) {
                if (index > 0) {
                    result.add(orig.substring(index));
                }
                else {
                    result.add(orig);
                }
                break;
            }
            result.add(orig.substring(index, new_index));
            index = new_index + token_len;
        }
        return result;
    }
	
	// è„ÇÃsplitÇÃç≈ìKâªî≈
    public static int split(String with, String orig, String [] result) {
    	final int count_max = result.length;
        final int token_len = with.length();
    	int count = 0;
        if (orig.length() == 0 || token_len == 0) {
            return count;
        }
        int index = 0;
        while (true) {
            int new_index = orig.indexOf(with, index);
            if (new_index == -1) {
            	if (count < count_max) {
            		result[count] = orig.substring(index);
            	}
            	count++;
                break;
            }
            if (count < count_max) {
            	result[count] = orig.substring(index, new_index);
            }
            count++;
            index = new_index + token_len;
        }
        return count;
    }

}
