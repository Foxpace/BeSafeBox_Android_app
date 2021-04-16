package motionapps.besafebox.tools;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.R;


public class PermissionHandler {

    private static final String[] neededPermission = new String[]{
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,

    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static final String[] neededPermissionAboveQ = new String[]{
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
    };

    public static boolean checkLocation(Context context, boolean showToast){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        boolean result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if(!result && showToast){
            Toasty.error(context, R.string.location_permission, Toasty.LENGTH_LONG, true).show();
        }

        return result;
    }


    public static boolean checkSMS(Context context, boolean showToast){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        boolean result = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        if(!result && showToast){
            Toasty.error(context, R.string.sms_permission, Toasty.LENGTH_LONG, true).show();
        }

        return result;
    }
}
