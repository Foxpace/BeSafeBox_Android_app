package motionapps.besafebox.model_managers;

import android.annotation.SuppressLint;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.List;

import motionapps.besafebox.activities.main.Main;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.notifications.Notify;
import motionapps.besafebox.models.detectors.Detector;
import motionapps.besafebox.R;
import motionapps.besafebox.services.DetectionService;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.SENSOR_SERVICE;


/**
 * trieda ktorá rozhoduje o modeli ako má byť aktívny voči pohybu uživateľa
 */

public class BatteryOptimizer implements SensorEventListener {

    public final static int NO_ACTIVITY = 0, STILL = 1, FALL_DETECTION = 2; // codes for sequences
//    public static final String SLEEP_DETECTION = "battery_sleep", MOVING_CONFIDENCE = "confidence"; // tags for intents
//    static final long REFRESH_RATE_CONFIDENCE = 240_000L; // max limit for activity recognition response [ms]

    private int actualActivity = -1, // storage of previous block - required while the car accident detection was here
            lastActivity = FALL_DETECTION;

    private PowerManager.WakeLock wakeLock; // holding to wakelock, to keep device awake for the app

    private final DetectionService detectionService; // main service
    private ModelManager model;

    private ActivityRecognitionClient activityRecognitionClient; // activity recognition API
    private PendingIntent changeSensor, confidence; // intents to send for service
    // - change of sensor / registration of the activity recognition

    private boolean proxi = false, // cover of proxy sensor
            decidingActivity = false;  // deciding about activity

    private float lastProxiValue = -1; // proximity value

    private final String TAG = "BatteryOptimizer";


    public BatteryOptimizer(DetectionService service) {
        this.detectionService = service;
    }

    private void decideActivity() {
        if (decidingActivity) return;

        decidingActivity = true;

        // if the sleep is allowed
        if (PreferencesHolder.getBoolean(detectionService, PreferencesHolder.ALLOW_SLEEP, true)) {
            registerActivityRecognition();
        }

        // detection in pocket
        if (PreferencesHolder.getBoolean(detectionService, PreferencesHolder.POCKET_ACTIVE, true)) {
            registerProxi();
        }

        // fall detection - infinite
        if (model.getDetectorType() == -1) setActivity(FALL_DETECTION);

        decidingActivity = false;
    }

    /**
     * @param actualActivity - code of the detector / action to put to still / sleep the model
     */
    public void setActivity(int actualActivity) {
        if (actualActivity == this.actualActivity) return;

        // vibrate if it is available on change of the sensor
        if (PreferencesHolder.getBoolean(detectionService, PreferencesHolder.VIBRATE_CHANGE, false)) {
            Notify.vibrateOnce(detectionService);
        }

        int type = model.getPickedType();

        switch (actualActivity) {

            case NO_ACTIVITY: // detector is destroyed and model is paused
                Log.i(TAG, "setActivity: NO ACTIVITY");
                releaseWakeLock();
                unregisterProxi();
                model.onDestroy();
                Notify.updateForegroundNotification(detectionService, detectionService.getString(
                        R.string.sleep), detectionService.getFOREGROUND_ID(), Detector.NONE);
                break;
            case STILL: // pauses model
                Log.i(TAG, "setActivity: STILL");
                releaseWakeLock();
                model.onPause();
                resetProxi();
                Notify.updateForegroundNotification(detectionService, detectionService.getString(
                        R.string.sleep),
                        detectionService.getFOREGROUND_ID(), Detector.NONE);
                break;
            case FALL_DETECTION:
                Log.i(TAG, "setActivity: FALL DETECTION");
                registerWakeLock();
                registerProxi();
                if (model.isDestroyed()) {
                    // init of the model, if it is destroyed
                    model.initModel(detectionService, detectionService.getHandler(), type);
                } else { // change of detector
                    model.changeDetector(type);
                }
                model.onResume(); // resume of the model and set up of noptification
                Notify.updateForegroundNotification(detectionService, detectionService.getString(
                        model.getDetector().getStringID()),
                        detectionService.getFOREGROUND_ID(), model.getDetector().getType());
                break;
        }

        this.lastActivity = this.actualActivity;
        this.actualActivity = actualActivity;
    }

    /**
     * method to lock the application, with the WakeLock object to run in the background - when it loses the application in Doze mode
     * automatically goes into idle - this is not the rule - measurements do not have to come from the accelerometer,
     * execution of threads may be paused also, no intents may come from system or application itself
     */
    @SuppressLint("WakelockTimeout")
    private void registerWakeLock() {
        // wakelock hold
        PowerManager powerManager = (PowerManager) detectionService.getSystemService(POWER_SERVICE);
        if (powerManager == null) {
            Log.e(TAG, "PowerManager is null");
            return;
        }

        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "FallDetection::WalkWakeLock");
        } else {

            if (wakeLock.isHeld()) {
                Log.e(TAG, "Wakelock is held");
                return;
            }
        }

        wakeLock.acquire();
        Log.i(TAG, "registerWakeLock: acquired");

    }


    private void releaseWakeLock() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.w(TAG, "releaseWakeLock: released");
            }
        }
    }

    /**
     * @param model - main object for classification of the fall
     */
    public void setModel(ModelManager model) {
        this.model = model;
        decideActivity();
    }

    /**
     * registration of activity recognition method - unreliable method unfortunately
     * 2 kinds of information:
     * 1. changes of state
     * 2. confidence scores in intervals about activity - not used currently, because of Doze mode
     * REFRESH_RATE_CONFIDENCE - max interval to late
     */
    private void registerActivityRecognition() {

        if (changeSensor != null) {
            return;
        }

        List<ActivityTransition> transitions = new ArrayList<>();
        for (Integer i : new ArrayList<Integer>() {{
            add(DetectedActivity.IN_VEHICLE);
            add(DetectedActivity.STILL);
            add(DetectedActivity.ON_FOOT);
            add(DetectedActivity.WALKING);
            add(DetectedActivity.RUNNING);
        }}) {

            transitions.add(new ActivityTransition.Builder()
                    .setActivityType(i)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build());
            transitions.add(new ActivityTransition.Builder()
                    .setActivityType(i)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build());
        }

        Intent cancel = new Intent(Main.BROADCAST_STATE_MOVING_CHANGE);

        changeSensor = PendingIntent.getBroadcast(detectionService, 1654,
                cancel, PendingIntent.FLAG_UPDATE_CURRENT);

        // registration of changes in movement
        activityRecognitionClient = ActivityRecognition.getClient(detectionService);
        activityRecognitionClient.requestActivityTransitionUpdates(
                new ActivityTransitionRequest(transitions), changeSensor)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Activity recognition registered successfully"))
                .addOnFailureListener(e -> {
                    Log.i(TAG, "Activity recognition registration failed");
                    changeSensor = null;
                });

        // confidence score registration
//        Intent intent = new Intent(BROADCAST_SERVICE);
//        intent.putExtra(BROADCAST_STATE, MOVING_CONFIDENCE);
//        confidence = PendingIntent.getBroadcast(detectionService, 457,
//                intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        activityRecognitionClient.requestActivityUpdates(REFRESH_RATE_CONFIDENCE, confidence)
//                .addOnSuccessListener(aVoid -> Log.i(TAG, "BatteryOptimizer: Activity recognition is ON"))
//                .addOnFailureListener(e -> Log.i(TAG, "BatteryOptimizer: Activity recognition FAILED"));
    }

    private void unregisterActivityRecognition() {
        if (activityRecognitionClient != null) {
            if (changeSensor != null) {
                activityRecognitionClient.removeActivityTransitionUpdates(changeSensor);
            }

            if (confidence != null) {
                activityRecognitionClient.removeActivityUpdates(confidence);
            }

            activityRecognitionClient = null;
            confidence = null;
            changeSensor = null;
            Log.i(TAG, "unregisterActivityRecognition: unregistering");
        }
    }


    /**
     * @param intent - intent with info about change of activity - changes IDLE / FALL detection
     */
    public void sendOnActivityChange(Intent intent) {

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            if (result != null) {
                result.getTransitionEvents();
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    if (event.getTransitionType() == 0) {
                        switch (event.getActivityType()) {
                            case DetectedActivity.IN_VEHICLE:
                            case DetectedActivity.ON_BICYCLE:
                            case DetectedActivity.RUNNING:
                            case DetectedActivity.WALKING:
                            case DetectedActivity.ON_FOOT:
                            case DetectedActivity.UNKNOWN: // change to fall detection
                                setActivity(FALL_DETECTION);
                                break;
                            case DetectedActivity.STILL: // person is still, no we do not need to detect
                                setActivity(STILL);
                                break;
                            default:
                                setActivity(NO_ACTIVITY);
                                break;
                        }
                    }

                    Log.i(TAG, "onReceive: " + String.format(
                            "Activity type: %s, Transition type: %s",
                            convertTransitionToString(event.getActivityType()),
                            event.getElapsedRealTimeNanos() == 1 ? "EXIT" : "ENTER"));
                }
            }
        }
    }


//    /**
//     * @param intent - itnencia s pravdepodobnosťou jednotlivých druhov pohybu
//     *               najviac preferovaný pohyb - jemu určená akcia
//     *               pravidelne počas detekcia prichádza (ak to funguje ako to má)
//     */
//    public void setConfidence(Intent intent) {
//
//
//        if (ActivityRecognitionResult.hasResult(intent)) {
//            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
//            if (result != null) {
//                int type = result.getMostProbableActivity().getType();
//                switch (actualActivity) {
//                    case STILL:
//                    case NO_ACTIVITY:
//                        switch (type) {
//                            case DetectedActivity.ON_FOOT:
//                            case DetectedActivity.TILTING:
//                                if (lastActivity == FALL_DETECTION) {
//                                    setActivity(FALL_DETECTION);
//                                }
//                                break;
//                        }
//                        break;
//                    case FALL_DETECTION:
//                        if (type == DetectedActivity.STILL &&
//                                PreferencesHolder.getBoolean(detectionService, PreferencesHolder.ALLOW_SLEEP, true)) {
//                            setActivity(STILL);
//                        }
//                        break;
//                }
//            }
//        }
//    }


    /**
     * @param context      - any
     * @param intentFilter - addition to intent filter with battery intents and screen actions
     */
    public static void addBatteryActions(Context context, IntentFilter intentFilter) {
        if (PreferencesHolder.getBoolean(context, PreferencesHolder.BATTERY_LOW, true)) {
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
            intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        }

        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    }


    /**
     * registration of proxi sensor, so the app can determine, if it should be active or not
     */
    private void registerProxi() {
        if (proxi) return;

        if (!PreferencesHolder.getBoolean(detectionService, PreferencesHolder.POCKET_ACTIVE, true))
            return;

        SensorManager sensorManager = (SensorManager) detectionService.getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) return;

        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (sensor == null) return;

        if (sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)) {
            proxi = true;
            Log.i(TAG, "registerProxi: registering proxi sensor");
        }

        if (proxi && lastProxiValue > 0) {
            setActivity(STILL);
        }

    }

    private void unregisterProxi() {
        SensorManager sensorManager = (SensorManager) detectionService.getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            proxi = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // if the value is close to 0 - phone is in pocket
        if (event.sensor.getType() != Sensor.TYPE_PROXIMITY) return;

        if (event.values[0] == lastProxiValue) return;

        lastProxiValue = event.values[0];
        if (model.isIdle()) {
            setActivity(STILL);
        } else {
            setActivity(lastActivity);
        }


    }

    public void resetProxi() {
        this.lastProxiValue = 5f; // resets the sensor, like it would be outside
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    } // we ignore accuracy of the samples

    /**
     * reactions to intents from the Android like battery and screen
     */
    public void setAction(String action) {
        switch (action) {
            case Intent.ACTION_BATTERY_LOW:
                setActivity(NO_ACTIVITY);
                return;
            case Intent.ACTION_BATTERY_OKAY:
                decideActivity();
                return;
            case Intent.ACTION_SCREEN_ON: // if the screen is on, we do not need to detect anything
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    PowerManager powerManager = (PowerManager) detectionService.getSystemService(POWER_SERVICE);
                    if (model.isIdle() && powerManager.isInteractive()) {
                        model.cancelAllDetections();
                    }
                } else {
                    if (model.isIdle()) {
                        model.cancelAllDetections();
                    }
                }
        }
    }

    /**
     * destroys all the registrations
     */
    public void onDestroy() {
        unregisterActivityRecognition();
        unregisterProxi();
        releaseWakeLock();
    }


    /**
     * @param transition - activity transition to string
     * @return - String
     */
    private static String convertTransitionToString(int transition) {
        switch (transition) {
            case DetectedActivity.IN_VEHICLE:
                return "Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "Bike";
            case DetectedActivity.ON_FOOT:
                return "Foot";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.WALKING:
                return "Walking";
            case DetectedActivity.RUNNING:
                return "Run";
            default:
                return "Unknown";
        }
    }
}
