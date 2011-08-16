package info.narazaki.android.lib.system;

import java.lang.reflect.Field;

import android.content.Intent;
import android.os.Build;
import android.text.TextPaint;
import android.view.MotionEvent;

public class MigrationSDK5 {
    public static boolean supported() {
        return Integer.parseInt(Build.VERSION.SDK) >= 5;
    }
    
    public static void Intent_addFlagNoAnimation(Intent intent) {
        /*
        if (!supported()) return;
        try {
            Field field = Intent.class.getField("FLAG_ACTIVITY_NO_ANIMATION");
            int flag = field.getInt(null);
            intent.addFlags(flag);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        */
    }
    
    static volatile Field field_TextPaint_SetDensity_ = null;
    
    public static void TextPaint_SetDensity(final TextPaint paint, final float density) {
        if (!supported()) return;
        try {
            if (field_TextPaint_SetDensity_ == null) {
                field_TextPaint_SetDensity_ = TextPaint.class.getField("density");
            }
            field_TextPaint_SetDensity_.set(paint, density);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return;
    }
    
}
