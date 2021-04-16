package motionapps.besafebox.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import es.dmoral.toasty.Toasty;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.gps.GPSCallback;
import motionapps.besafebox.notifications.Alerts;
import motionapps.besafebox.notifications.Notify;
import motionapps.besafebox.R;
import motionapps.besafebox.tools.PermissionHandler;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.ncorti.slidetoact.SlideToActView;

import static motionapps.besafebox.activities.options.preferences.PreferencesHolder.ALARM_ASSIST;

/**
 * Alarm activity - tries to attract someone from surrounding, user can be directed to emergency dialog
 * or can cancel emergency button
 * after 60 seconds, phone will send alert to contact via sms and mail
 */

public class Alarm extends AppCompatActivity implements GPSCallback.OnLastLocation {

    CircularProgressBar circularProgressBar;
    int secondsLeft = 60, previousSoundMode;
    Handler handler;

    MediaPlayer mediaPlayer;
    AudioManager audioManager;

    String cameraId;
    boolean torch = false;

    public static final String REAL_ALARM = "REAL_ALARM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        turnOnScreen();
        setContentView(R.layout.activity_alarm);
        handler = new Handler(Looper.getMainLooper());
        turnOnSoundStartSoundAlarm();
        setUpCircularTime();
        setUpButtons();
        setUpTorchLight();
        setUpCounter();
    }


    /**
     * Screen can appear, even if it locked and phone will stay awake
     */
    private void turnOnScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        } else {

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    /**
     * Start of alarm sound and vibration
     */
    private void turnOnSoundStartSoundAlarm() {

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_begin);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        audioManager = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        previousSoundMode = audioManager.getRingerMode();
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);

        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Notify.vibrateOnce(getApplicationContext());
                handler.postDelayed(this, 1000L);
            }
        }, 1000L);
    }

    /**
     * resets the view for progress bar for countdown
     */
    private void setUpCircularTime() {
        circularProgressBar = findViewById(R.id.circularProgressBar);
        circularProgressBar.setProgressWithAnimation(secondsLeft, 500L);
        circularProgressBar.setProgressMax(secondsLeft);
    }

    /**
     * changes resource of the sound alarm - higher noise
     */
    private void changeTone() {
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_full);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

    }

    /**
     * countdown is handled by Handler for every second
     */
    private void setUpCounter() {
        handler.post(new Runnable() {
            @Override
            public void run() {

                // after 20 second the sound changes
                if (secondsLeft == 20) {
                    changeTone();
                }

                // sends alarm, if the option is true for it
                if (secondsLeft == 0) {
                    boolean realAlarm = getIntent().getBooleanExtra(REAL_ALARM, true);
                    if (PreferencesHolder.getBoolean(Alarm.this, ALARM_ASSIST, false) && realAlarm) {

                        if(PermissionHandler.checkLocation(Alarm.this, false)){
                            // request for GPS to get location, which is send
                            GPSCallback.getLastLocation(Alarm.this, Alarm.this);
                        }

                        //TODO cannot obtain location, but alarm is ringing

                    } else {
                        Toasty.warning(Alarm.this, R.string.alarm_message, Toast.LENGTH_LONG, true).show();
                    }

                    // infinite progressbar and change text
                    circularProgressBar.setIndeterminateMode(true);
                    TextView textView = findViewById(R.id.alarm_counter);
                    textView.setText(R.string.alert_sent);
                    textView.setTextSize(25);

                } else {

                    // text update
                    handler.postDelayed(this, 1000L);
                    TextView textView = findViewById(R.id.alarm_counter);
                    textView.setText(String.format("%s s", secondsLeft));
                    circularProgressBar.setProgress(secondsLeft--);
                }


            }
        });
    }

    /**
     * sets up emergency and cancel button
     */
    private void setUpButtons() {

        // cancel button
        SlideToActView cancel = findViewById(R.id.sos_cancel_slider);
        cancel.setOnSlideCompleteListener(v -> {
            audioManager.setRingerMode(previousSoundMode);
            onEndOfAlarm();
        });

        // sos button
        SlideToActView sos = findViewById(R.id.sos_alarm);
        sos.setOnSlideCompleteListener(v -> {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(ComponentName.unflattenFromString("com.android.phone/.EmergencyDialer"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            finish();
            startActivity(intent);
        });
    }


    /**
     * turns on torch periodically
     */
    private void setUpTorchLight() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setUpTorchAbove21();
            } else {
                setUpTorchBelow21();
            }
        }
    }

    /**
     * turns off torch once
     */
    private void turnOffTorch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            turnOffTorchAbove21();
        } else {
            turnOffTorchBelow21();
        }

    }

    /**
     * method to turn off torch for android M and higher
     * @return - false if the camera does not have torch
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean turnOffTorchAbove21() {
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = camManager.getCameraIdList()[0];
            camManager.setTorchMode(cameraId, false);
            return true;
        } catch (CameraAccessException | RuntimeException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * method to turn on torch periodically for android M and higher
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setUpTorchAbove21() {
        if(!turnOffTorchAbove21()) return;
        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraId = camManager.getCameraIdList()[0];
                    camManager.setTorchMode(cameraId, !torch);
                    torch = !torch;
                    handler.postDelayed(this, 1500L);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }, 1500L);

    }


    Camera camera;

    /**
     * Turns off torch for Android KitKat and Lollipop
     */
    private void turnOffTorchBelow21() {
        if (camera == null) {
            camera = Camera.open();
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(parameters);
    }

    /**
     * turns on torch for Android KitKat and Lollipop
     */
    private void setUpTorchBelow21() {
        try {
            if (camera == null) {
                camera = Camera.open();
            }
            turnOffTorchBelow21();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setFlashMode(torch ? Camera.Parameters.FLASH_MODE_ON : Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    torch = !torch;
                    handler.postDelayed(this, 1500L);
                }
            }, 1500L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * sends info to contact
     * @param location - actual location
     *
     */
    @Override
    public void onLastLocation(Location location) {
        Alerts.sendAlert(Alarm.this, new LatLng(location.getLatitude(), location.getLongitude()), Alerts.FALL);
    }


    /**
     * needs to be called on the end of alarm - lights, vibrations, countdown - everything is stopped
     */
    private void onEndOfAlarm() {
        // release audio
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // remove all repeating events - countdown, vibration, torch
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        turnOffTorch();

        // screen can be put to sleep
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        finish();

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onEndOfAlarm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onEndOfAlarm();
    }
}
