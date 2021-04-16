package motionapps.besafebox.activities.options.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.Objects;

/**
 * methods to store all the preferences
 */

public class PreferencesHolder {

    // version for the Preferences
    private final static String PREFERENCES_VERSION_KEY = "PREFERENCES_VERSION_KEY";
    private final static int PREFERENCES_VERSION = 1;
    // first start key
    public final static String FIRST_START = "first";

    // contact keys
    public final static String MAIL = "MAIL", NUMBER = "NUMBER", NAME = "NAME";

    // alarm settings
//    public final static String ALARM_NOTIFICATION = "alarm_notification";
    public final static String ALARM_ME = "alarm_alone";
    public final static String ALARM_ASSIST = "alarm_assist";

    // model settings
    public final static String SENSITIVITY_NN = "sensitivity_NN";
    public static final String THRESHOLD_ACTIVITY = "threshold_NN";

    // battery settings
    public final static String BATTERY_LOW = "battery_low";
    public final static String WIFI_FALL = "wifi_fall";
    public final static String POCKET_ACTIVE = "Pocket_active";
    public static final String ALLOW_SLEEP = "Allow sleep";
    public static final String IGNORE_BATTERY_OPTIMIZER = "IGNORE BATTERY";
    public static final String VIBRATE_CHANGE = "vibrate_change";

    // public final static String CAR_PLUGGED = "plugged_car_accident";


    /**
     * Place to add code for new versions of SharedPreferences
     * @param context any
     */
    public static void versionCheck(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getInt(PREFERENCES_VERSION_KEY, 0) < PREFERENCES_VERSION){
            // do changes in settings

            sharedPreferences.edit().putInt(PREFERENCES_VERSION_KEY, PREFERENCES_VERSION).apply();
        }
    }

    /**
     * General method to store anything to SharedPreferences
     * @param context any
     * @param key - string to preference
     * @param o - object to store
     */
    public static void onChange(Context context, String key, Object o){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(o instanceof String){
            editor.putString(key, (String) o);
        }
        else if(o instanceof Boolean){
            editor.putBoolean(key, (Boolean) o);
        }
        else if (o instanceof Float){
            editor.putFloat(key, (Float) o);
        }
        else if (o instanceof Integer){
            editor.putInt(key, (Integer) o);
        }
        editor.apply();
    }

    // methods to get anything from SharedPreferences

    @NonNull
    public static Boolean getBoolean(Context context, String key, Boolean def){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(key, def);
    }

    @NonNull
    public static String getString(Context context, String key, String def){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Objects.requireNonNull(sharedPreferences.getString(key, def));
    }

    /**
     * @param context any
     * @param key String for preference
     * @param def default value
     * @return return String, but if the stored value does not exist or is "", the default value is returned
     */
    public static String getStringNone(Context context, String key, @NonNull String def){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString(key, def);

        if(value == null){
            return def;
        }

        if(value.equals("")){
            return def;
        }else{
            return value;
        }
    }

    @NonNull
    public static Integer getInt(Context context, String key, int def){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(key, def);
    }

    @NonNull
    public static Float getFloat(Context context, String key, float def){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getFloat(key, def);
    }

}
