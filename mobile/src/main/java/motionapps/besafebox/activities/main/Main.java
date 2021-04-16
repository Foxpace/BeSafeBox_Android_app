package motionapps.besafebox.activities.main;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;

import de.psdev.licensesdialog.LicensesDialog;
import motionapps.besafebox.R;
import motionapps.besafebox.activities.About;
import motionapps.besafebox.activities.introduction.Introduction;
import motionapps.besafebox.activities.options.SettingsActivity;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;

/**
 * main screen logic and views
 */

public class Main extends AppCompatActivity {

    // for activity actions
    public final static String BROADCAST_UI_ON = "UI_ON";
    public final static String BROADCAST_UI_OFF = "UI_OFF";

    // for service actions
    public final static String BROADCAST_STATE_CANCEL = "CANCEL", BROADCAST_STATE_QUESTION = "QUESTION";
    public final static String BROADCAST_SWITCH_DETECTOR = "SWITCH DETECTOR";
    public final static String BROADCAST_RESTART = "RESTART";
    public final static String BROADCAST_STATE_MOVING_CHANGE = "MOVING CHANGE";
    public static final String BROADCAST_ACTUAL_TIME = "ACTUAL_TIME";
    private final String TAG = "Main";

    Boolean waitingForResponse = false;
    ActivityReceiver activityReceiver;
    MainButton mainButton;
    MaterialDialog dialog;
    Dialog licenseDialog;
    Handler handler;

    /**
     * class listens to intents, if the service is alive or not - UI changes by state
     * service binding makes service more susceptible to battery optimization
     */
    public class ActivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(android.content.Context context, Intent intent) {

            // make button clickable
            ImageButton monitoringButton = findViewById(R.id.main_button_monitoring);
            monitoringButton.setClickable(true);
            waitingForResponse = false;

            //handler for delay is no more needed
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                handler = null;
            }

            // change UI based on state
            if(intent.getAction() == null) return;
            switch (intent.getAction()){
                case BROADCAST_UI_ON:
                    mainButton.setServicesOn(Main.this, intent.getExtras());
                    Log.i(TAG, "receiving message from main TRUE");
                    break;
                case BROADCAST_UI_OFF:
                    mainButton.setServicesOff(Main.this);
                    Log.i(TAG, "receiving message from main FALSE");
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // check of first start and preferences - if true, the activity is about to change, stop init
        if (firstCheck()) {
            return;
        }

        setContentView(R.layout.activity_main);

        // UI methods
        setUpMainButton();
        setUpOtherButtons();

        if (activityReceiver == null) {
            receiverRegistration(); // registration of receiver to respond to service
        }

        if (!waitingForResponse) {
            isRunningService(); // checks if service if alive
        }

    }

    /**
     * checks shared preferences, if the app is initialized and if the preferences did not take update
     *
     * @return boolean - true to stop initialization of activity - due to change of activity
     */
    private boolean firstCheck() {
        PreferencesHolder.versionCheck(this);

        if (!PreferencesHolder.getBoolean(this, PreferencesHolder.FIRST_START, false)) {
            startActivity(new Intent(this, Introduction.class));
            finish();
            return true;
        }

        return false;
    }


    /**
     * handler for the main button to launch fall detection
     */
    private void setUpMainButton() {
        mainButton = new MainButton(this);
    }

    /**
     * sets up settings and license buttons
     */
    private void setUpOtherButtons() {

        ImageButton button = findViewById(R.id.main_settings);
        button.setOnClickListener(view -> {
            finish();
            startActivity(new Intent(this, SettingsActivity.class));
        });

        button = findViewById(R.id.main_licenses);
        button.setOnClickListener(view ->
                licenseDialog = new LicensesDialog.Builder(Main.this).
                        setNotices(R.raw.notices).setThemeResourceId(R.style.custom_theme)
                        .setDividerColor(R.color.black_foreground_elements).build().show()
        );

        button = findViewById(R.id.main_about);
        button.setOnClickListener(view -> {
            finish();
            startActivity(new Intent(this, About.class));
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }

        if (licenseDialog != null) {
            licenseDialog.dismiss();
            licenseDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiverCancel();

        if(mainButton != null){
            mainButton.onDestroy();
            mainButton = null;
        }
    }


    private void receiverRegistration() {
        Log.i(TAG, "receiverRegistration");
        IntentFilter filter = new IntentFilter(BROADCAST_UI_ON);
        filter.addAction(BROADCAST_UI_OFF);
        activityReceiver = new ActivityReceiver();
        registerReceiver(activityReceiver, filter);
    }

    private void receiverCancel() {
        if (activityReceiver != null) {
            unregisterReceiver(activityReceiver);
            activityReceiver = null;
        }
    }

    /**
     * sends broadcast to service and waits for answer for 1s
     */
    private void isRunningService() {
        waitingForResponse = true;
        mainButton.setServicesWaiting(Main.this);
        if (mainButton.getMonitoring() == null) {
            MainButton.sendBroadcastToService(this, BROADCAST_STATE_QUESTION);
        }

        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            mainButton.setServicesOff(Main.this);
            waitingForResponse = false;
        }, 1000L);
    }






}
