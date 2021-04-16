package motionapps.besafebox.activities.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gauravbhola.ripplepulsebackground.RipplePulseLayout;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.R;
import motionapps.besafebox.activities.options.functional.ContactCheckers;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.dialogs.dialogs_kt.DialogAlertsKt;
import motionapps.besafebox.services.DetectionService;
import motionapps.besafebox.tools.BatteryTools;

import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;
import static motionapps.besafebox.activities.main.Main.BROADCAST_ACTUAL_TIME;
import static motionapps.besafebox.activities.main.Main.BROADCAST_STATE_CANCEL;

public class MainButton {

    private MaterialDialog dialog;
    Boolean monitoring = null, bAnimation = false;

    public Boolean getMonitoring() {
        return monitoring;
    }

    private final String TAG = "MainButton";

    MainButton(Activity activity) {
        ImageButton monitoringButton = activity.findViewById(R.id.main_button_monitoring);
        monitoringButton.setOnClickListener(v ->
                Toast.makeText(activity, R.string.main_longclick_required, Toast.LENGTH_SHORT).show());

        monitoringButton.setOnLongClickListener(v -> {

            if (monitoring) {
                serviceSwitcher(activity);
                activity.findViewById(R.id.main_button_monitoring).setEnabled(false);
                return true;
            }

            // checks if the GPS is enabled
            final LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !monitoring) {
                dialog = DialogAlertsKt.createNoGPSDialog(activity);
                return false;
            }

            // check battery permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if (!BatteryTools.checkBatteryPermission(activity)) {
                    dialog = BatteryTools.startBatteryPermissionDialog(activity);
                    return false;
                }

                // permissions for the use of SMS and location
                if (PreferencesHolder.getBoolean(activity, PreferencesHolder.ALARM_ASSIST, false)) {
                    boolean permissionsOk;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionsOk = dexterAndroidQ(activity);
                    } else {
                        permissionsOk = dexterAndroidM(activity);
                    }

                    if (!permissionsOk) return false;

                }

            }

            // for activity recognition there is required BODY_SENSORS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    PreferencesHolder.getBoolean(activity, PreferencesHolder.ALLOW_SLEEP, false)) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {

                    PermissionListener snackbarPermissionListener =
                            SnackbarOnDeniedPermissionListener.Builder
                                    .with(monitoringButton,
                                            activity.getString(R.string.main_activity_recognition))
                                    .withOpenSettingsButton(activity.getString(R.string.settings)).build();

                    Dexter.withContext(activity)
                            .withPermission(Manifest.permission.ACTIVITY_RECOGNITION)
                            .withListener(snackbarPermissionListener).check();
                    return false;
                }
            }

            serviceSwitcher(activity);
            activity.findViewById(R.id.main_button_monitoring).setEnabled(false);
            Log.i(TAG, "monitoring button");

            return true;
        });
    }

    /**
     * Above Android M - sending SMS and accessing location requires permission
     * if there is no permission, the user will be asked for it before launching the detection
     * the permissions are only required, if the user wants to send alarm to his contact
     * SMS and location permissions are required here
     * if denied, snackbar is shown with button to settings
     *
     * @param activity - main activity
     * @return boolean - if all permissions are ok
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean dexterAndroidM(Activity activity) {

        int contactState = ContactCheckers.getContactCompletion(activity);

        switch (contactState) {

            case ContactCheckers.NO_CONTACTS:
                Toasty.error(activity, R.string.main_contact_required, Toasty.LENGTH_LONG, true);
                return false;
            case ContactCheckers.ALL_CONTACTS:
            case ContactCheckers.NAME_SMS:

                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }

                MultiplePermissionsListener snackbarMultiplePermissionsListener = SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
                        .with(activity.findViewById(R.id.main_button_monitoring), activity.getString(R.string.main_sms_needed_snackbar))
                        .withOpenSettingsButton(activity.getString(R.string.settings)).build();

                Dexter.withContext(activity)
                        .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS)
                        .withListener(snackbarMultiplePermissionsListener).check();
                return false;
            case ContactCheckers.NAME_MAIL:
                Toasty.warning(activity, R.string.main_use_phone_number, Toasty.LENGTH_LONG, true).show();
                return false;
//            case ContactCheckers.NAME_MAIL:
//                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//                    return true;
//
//
//                PermissionListener snackbarPermissionListener =
//                        SnackbarOnDeniedPermissionListener.Builder
//                                .with(activity.findViewById(R.id.main_button_monitoring), activity.getString(R.string.main_mail_needed_snackbar))
//                                .withOpenSettingsButton(activity.getString(R.string.settings)).build();
//
//                Dexter.withContext(activity)
//                        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//                        .withListener(snackbarPermissionListener).check();
//                return false;
        }
        return false;
    }

    /**
     * similar to method above
     * crucial difference - Android Q has separated permission for the background access of location
     *
     * @param activity - main activity
     * @return boolean - if all permissions are ok
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean dexterAndroidQ(Activity activity) {

        int contactState = ContactCheckers.getContactCompletion(activity);

        switch (contactState) {
            case ContactCheckers.NO_CONTACTS:
                Toasty.error(activity, R.string.main_contact_required, Toasty.LENGTH_LONG, true);
                return false;

            case ContactCheckers.ALL_CONTACTS:
            case ContactCheckers.NAME_SMS:
                MultiplePermissionsListener snackbarMultiplePermissionsListener;
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }

                snackbarMultiplePermissionsListener = SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
                        .with(activity.findViewById(R.id.main_button_monitoring), activity.getString(R.string.main_sms_background_needed_snackbar))
                        .withOpenSettingsButton(activity.getString(R.string.settings)).build();

                Dexter.withContext(activity)
                        .withPermissions(Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        .withListener(snackbarMultiplePermissionsListener).check();
                return false;

            case ContactCheckers.NAME_MAIL:
                Toasty.warning(activity, R.string.main_use_phone_number, Toasty.LENGTH_LONG, true).show();
                return false;
//                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
//                        ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
//                    return true;
//
//
//                snackbarMultiplePermissionsListener = SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
//                        .with(activity.findViewById(R.id.main_button_monitoring), activity.getString(R.string.main_mail_background_needed_snackbar))
//                        .withOpenSettingsButton(activity.getString(R.string.settings)).build();
//
//                Dexter.withContext(activity)
//                        .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//                        .withListener(snackbarMultiplePermissionsListener).check();
//                return false;
        }
        return false;
    }

    /**
     * "switch" to change service to nonactive / active
     *
     * @param activity - activity activity
     */
    private void serviceSwitcher(Activity activity) {

        if (monitoring) {
            cancelService(activity);
        } else {
            Log.i(TAG, "Initialization of the service");
            Intent intent = new Intent(activity, DetectionService.class);
            activity.startService(intent);
        }
        monitoring = !monitoring;
    }

    /**
     * cancellation of service with broadcast to service - no direct binding due to versatility
     *
     * @param activity - with main button
     */
    private void cancelService(Activity activity) {
        Log.i(TAG, "Cancelling the service");
        sendBroadcastToService(activity, BROADCAST_STATE_CANCEL);
        setServicesOff(activity);
    }

    public static void sendBroadcastToService(Context context, String command) {
        Log.i(context.getClass().getName(), "sendBroadcastToService: " + command);
        Intent intent = new Intent(command);
        intent.setFlags(FLAG_RECEIVER_FOREGROUND);
        context.sendBroadcast(intent);
    }

    /**
     * Updates emoji with description
     *
     * @param activity - with main button
     * @param image    - resource id
     * @param idString - resource id
     */
    private void updateStatusView(Activity activity, int image, int idString) {
        TextView textView = activity.findViewById(R.id.main_text_status_value);
        textView.setText(idString);

        ImageView imageView = activity.findViewById(R.id.main_smile);
        imageView.setImageResource(image);
    }

    /**
     * UI changes to turn off service
     *
     * @param activity - with main button
     */
    void setServicesOff(Activity activity) {
        Log.i(TAG, "setServicesOff");

        // turn on red ripple effect
        RipplePulseLayout ripplePulseLayout = activity.findViewById(R.id.layout_ripplepulse_active);
        ripplePulseLayout.stopRippleAnimation();
        ripplePulseLayout = activity.findViewById(R.id.layout_ripplepulse_inactive);
        ripplePulseLayout.startRippleAnimation();

        // enable button
        ImageButton monitoringButton = activity.findViewById(R.id.main_button_monitoring);
        monitoringButton.setEnabled(true);

        // change emoji and button view
        animButton(activity, R.drawable.main_start_button);
        updateStatusView(activity, R.drawable.ic_sad_face, R.string.main_detection_off);
        turnOffChronometer(activity);
        monitoring = false;
    }

    /**
     * UI changes to turn on service
     *
     * @param activity - with main button
     * @param bundle   - from broadcast of the service
     */
    void setServicesOn(Activity activity, Bundle bundle) {
        Log.i(TAG, "setServicesOn");

        // turn on active ripple effect
        RipplePulseLayout ripplePulseLayout = activity.findViewById(R.id.layout_ripplepulse_inactive);
        ripplePulseLayout.stopRippleAnimation();
        ripplePulseLayout = activity.findViewById(R.id.layout_ripplepulse_active);
        ripplePulseLayout.startRippleAnimation();

        // enable button
        ImageButton monitoringButton = activity.findViewById(R.id.main_button_monitoring);
        monitoringButton.setEnabled(true);

        // animate button and change status
        animButton(activity, R.drawable.main_stop_button);
        updateStatusView(activity, R.drawable.ic_happy_face, R.string.main_detection_on);

        if (bundle != null) {
            long time = bundle.getLong(BROADCAST_ACTUAL_TIME, SystemClock.elapsedRealtime());
            turnOnChronometer(activity, time);
        }

        monitoring = true;
    }

    /**
     * UI during the wait for the service, if it is alive or not
     *
     * @param activity - with main button
     */
    void setServicesWaiting(Activity activity) {

        if (monitoring != null) return;
        Log.i(TAG, "setServicesWaiting");

        RipplePulseLayout ripplePulseLayout = activity.findViewById(R.id.layout_ripplepulse_active);
        ripplePulseLayout.stopRippleAnimation();
        ripplePulseLayout = activity.findViewById(R.id.layout_ripplepulse_inactive);
        ripplePulseLayout.stopRippleAnimation();

        turnOffChronometer(activity);
        ImageButton monitoringButton = activity.findViewById(R.id.main_button_monitoring);
        monitoringButton.setEnabled(false);
        monitoringButton.setImageResource(R.drawable.main_wait_button);
        updateStatusView(activity, R.drawable.ic_sad_face, R.string.main_detection_off);
        monitoring = null;
    }

    /**
     * fade in and out animation for the button
     *
     * @param activity - with main button
     * @param id       - reource to change the button
     */
    private void animButton(Activity activity, int id) {
        Log.i(TAG, "animButton: started");
        if (bAnimation) {
            return;
        }
        bAnimation = true;
        Animation outAnimation = AnimationUtils.loadAnimation(activity, R.anim.fade_out);
        Animation inAnimation = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
        outAnimation.setAnimationListener(new Animation.AnimationListener() {

            // Other callback methods omitted for clarity.

            @Override
            public void onAnimationStart(Animation animation) {

            }

            public void onAnimationEnd(Animation animation) {

                // Modify the resource of the ImageButton
                ImageButton monitoringButton = activity.findViewById(R.id.main_button_monitoring);
                monitoringButton.setImageResource(id);
                monitoringButton.startAnimation(inAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        inAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bAnimation = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        ImageButton monitoringButton = activity.findViewById(R.id.main_button_monitoring);
        monitoringButton.startAnimation(outAnimation);
    }

    public void onDestroy() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }

    }

    /**
     * Starts timer for the service based on starting time from service
     *
     * @param activity - with main button
     * @param time     - SystemClock.elapsedTime()
     */
    private void turnOnChronometer(Activity activity, long time) {
        Chronometer chronometer = activity.findViewById(R.id.main_chronometer);
        chronometer.setVisibility(View.VISIBLE);
        chronometer.setBase(time);
        chronometer.start();
    }


    /**
     * turns off timer and makes it invisible
     *
     * @param activity - with main button
     */
    private void turnOffChronometer(Activity activity) {
        Chronometer chronometer = activity.findViewById(R.id.main_chronometer);
        chronometer.setVisibility(View.INVISIBLE);
        chronometer.stop();
    }
}
