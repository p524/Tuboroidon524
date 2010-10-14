package info.narazaki.android.lib.system;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class MigrationSDK4 {
    public static boolean supported() {
        return Integer.parseInt(Build.VERSION.SDK) >= 4;
    }
    
    static volatile Method method_Typeface_createFromFile_ = null;
    
    public static Typeface Typeface_createFromFile(File file) {
        if (!supported()) return null;
        try {
            if (method_Typeface_createFromFile_ == null) {
                method_Typeface_createFromFile_ = Typeface.class
                        .getMethod("createFromFile", new Class[] { File.class });
            }
            Typeface aa_font = (Typeface) method_Typeface_createFromFile_.invoke(null, new Object[] { file });
            return aa_font;
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static volatile Field field_BitmapFactory_SetOptions_ = null;
    
    public static void BitmapFactory_SetOptions(final BitmapFactory.Options opt, final String name, final Object data) {
        if (!supported()) return;
        try {
            if (field_BitmapFactory_SetOptions_ == null) {
                field_BitmapFactory_SetOptions_ = BitmapFactory.Options.class.getField(name);
            }
            field_BitmapFactory_SetOptions_.set(opt, data);
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
    
    static volatile Method method_ViewConfiguration_getScaledMaximumFlingVelocity_ = null;
    
    public static int ViewConfiguration_getScaledMaximumFlingVelocity(final ViewConfiguration configuration) {
        if (!supported()) return 0;
        try {
            if (method_ViewConfiguration_getScaledMaximumFlingVelocity_ == null) {
                method_ViewConfiguration_getScaledMaximumFlingVelocity_ = ViewConfiguration.class
                        .getMethod("getScaledMaximumFlingVelocity");
            }
            int result = (Integer) method_ViewConfiguration_getScaledMaximumFlingVelocity_.invoke(configuration,
                    new Object[] {});
            return result;
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    static volatile Method method_VelocityTracker_computeCurrentVelocity_ = null;
    
    public static void VelocityTracker_computeCurrentVelocity(final VelocityTracker velocityTracker, final int units,
            final float maxVelocity) {
        if (supported()) {
            try {
                if (method_VelocityTracker_computeCurrentVelocity_ == null) {
                    method_VelocityTracker_computeCurrentVelocity_ = VelocityTracker.class.getMethod(
                            "computeCurrentVelocity", new Class[] { int.class, float.class });
                }
                method_VelocityTracker_computeCurrentVelocity_.invoke(velocityTracker, new Object[] {
                        new Integer(units), new Float(maxVelocity) });
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
            catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        velocityTracker.computeCurrentVelocity(units);
    }
    
}
