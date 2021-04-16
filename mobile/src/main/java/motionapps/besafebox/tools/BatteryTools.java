package motionapps.besafebox.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.afollestad.materialdialogs.MaterialDialog;

import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.dialogs.dialogs_kt.DialogAlertsKt;

import static android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

public class BatteryTools {

    /**
     * checks if the battery is ignored
     * @param context any
     * @return boolean
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean checkBatteryPermission(Context context){
        // if not ignoring battery optimizations
        if (!PreferencesHolder.getBoolean(context, PreferencesHolder.IGNORE_BATTERY_OPTIMIZER, false)) {
            PowerManager pm = (PowerManager) context.getSystemService(android.content.Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    /**
     * @param activity - any
     * @return null if the start of the activity is successful / MaterialDialog if not
     */
    @SuppressLint("BatteryLife")
    @Nullable
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static MaterialDialog startBatteryPermissionDialog(Activity activity){
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getApplicationContext().getPackageName()));
            activity.startActivity(intent);
            return null;
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            return DialogAlertsKt.batteryOptimizationDialog(activity);

        }

    }

}
