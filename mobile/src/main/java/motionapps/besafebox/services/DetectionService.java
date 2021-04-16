package motionapps.besafebox.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;

import motionapps.besafebox.R;
import motionapps.besafebox.activities.main.Main;
import motionapps.besafebox.model_managers.BatteryOptimizer;
import motionapps.besafebox.models.detectors.DetectorFallNative;
import motionapps.besafebox.notifications.Notify;
import motionapps.besafebox.model_managers.ModelManager;

import android.os.SystemClock;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;
import static motionapps.besafebox.models.detectors.Detector.FALL;

/**
 * service, under which whole model works
 */

public class DetectionService extends Service {

    private ServiceReceiver serviceReceiver; // custom receiver
    private ModelManager model; // model for detection
    private BatteryOptimizer batteryOptimizer; // coordinates model with life cycle

    // executes jobs with delay
    private Handler handler;
    private HandlerThread handlerThread;

    private long timerMain; // time of start
    private boolean serviceIdle = true, restarting = false; // flags

    final String TAG = "DetectionService";
    private final int FOREGROUND_ID = 2000;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        initService();
        return START_STICKY; // with collapse of the service -> restart
        // activity is not bounded to service -> lower chance of killing the service
    }

    public int getFOREGROUND_ID() {
        return FOREGROUND_ID;
    }

    /**
     * // send back info about working service for UI update
     *
     * @param serviceActive - boolean if the service is working / canceling
     */
    private void sendToMain(boolean serviceActive) {
        Intent intent = new Intent(serviceActive ? Main.BROADCAST_UI_ON : Main.BROADCAST_UI_OFF);
        intent.setFlags(FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Main.BROADCAST_ACTUAL_TIME, timerMain);
        sendBroadcast(intent);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // activity is not bounded to service -> lower chance of killing the service
    }

    @Override
    public void onCreate() {
        super.onCreate();

        timerMain = SystemClock.elapsedRealtime();

        startForeground(FOREGROUND_ID, Notify.createNotificationForDetection(this,
                getString(R.string.notification_subtext), ModelManager.getBasicType())); // creation of notification

        // receiver registration
        if (serviceReceiver == null) {
            IntentFilter intentFilter = new IntentFilter();
            BatteryOptimizer.addBatteryActions(this, intentFilter);
            intentFilter.addAction(Main.BROADCAST_STATE_QUESTION);
            intentFilter.addAction(Main.BROADCAST_STATE_CANCEL);
            intentFilter.addAction(Main.BROADCAST_RESTART);
            intentFilter.addAction(Main.BROADCAST_SWITCH_DETECTOR);

            serviceReceiver = new ServiceReceiver();
            registerReceiver(serviceReceiver, intentFilter);

            // sending back info
            sendToMain(true);
        }
    }


    /**
     * creation of model and battery optimizer
     */
    @SuppressLint({"MissingPermission", "WakelockTimeout"})
    public synchronized void initService() {
        if (!serviceIdle) {
            Log.w(TAG, "initService: attempt to init new model while active state");
            return;
        }

        serviceIdle = false;
        createHandlerThread();
        this.model = new ModelManager();
        this.batteryOptimizer = new BatteryOptimizer(this);

        batteryOptimizer.setModel(model);

        Log.w(TAG, "initService: initialized");
    }

    /**
     * destroy all instances of model
     */
    private void cancelService() {
        Log.i(TAG, "sending back FALSE");
        sendToMain(false);

        partialStop(); // stopped battery optimizer

        if (serviceReceiver != null) {
            unregisterReceiver(serviceReceiver);
            serviceReceiver = null;
        }

        if (model != null) {
            model.onDestroy();
            model = null;
        }


        stopSelf();
        Log.w(TAG, "Service stopped");
    }

    /**
     * on the end of service remove receiver
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        partialStop();

        if (serviceReceiver != null) {
            unregisterReceiver(serviceReceiver);
            serviceReceiver = null;
        }

    }

    /**
     * only sensors are stopped, but model objects remain active
     */
    public synchronized void partialStop() {

        if (serviceIdle) {
            Log.w(TAG, "partialStop: attempt to stop already stopped model");
            return;
        }
        serviceIdle = true;

        destroyHandler();
        if (batteryOptimizer != null) {
            batteryOptimizer.onDestroy();
            batteryOptimizer = null;
        }

        if (model != null) {
            model.onDestroy();
            model = null;
        }

        Log.w(TAG, "initService: stopped");

    }


    private void createHandlerThread() {
        if (handler == null & handlerThread == null) {
            handlerThread = new HandlerThread("BeSafeBox_HandlerThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }
    }

    private void destroyHandler() {

        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    public Handler getHandler() {
        return handler;
    }

    /**
     * custom class of receiver to manage all system intents
     */
    private class ServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            Log.i(TAG, "receiving message ".concat(action));

            batteryOptimizer.setAction(action); // passes to battery optimizer, to check its conditions

            switch (action) {
                case DetectorFallNative.NATIVE_ALARM: // to launch alarm from native environment
                    if (model != null) {
                        model.sendAlert(FALL);
                    }
//                case BatteryOptimizer.MOVING_CONFIDENCE: // activity confidence
//                    batteryOptimizer.setConfidence(intent);
//                    break;
                case Main.BROADCAST_STATE_MOVING_CHANGE: // change in activity
                    batteryOptimizer.sendOnActivityChange(intent);
                    break;
                case Main.BROADCAST_STATE_QUESTION: // sends back info about service
                    sendToMain(true);
                    Log.i(TAG, "sending back TRUE");
                    break;
                case Main.BROADCAST_STATE_CANCEL: // cancel of service
                    cancelService();
                    break;
                case Main.BROADCAST_SWITCH_DETECTOR: // change detector
                    batteryOptimizer.setActivity(BatteryOptimizer.FALL_DETECTION);
                    break;
                case Main.BROADCAST_RESTART:
                    Log.w(TAG, "onReceive: restarting service");
                    if (!restarting) {
                        restarting = true;
                        partialStop();
                        initService();
                        restarting = false;
                    } else {
                        Log.e(TAG, "onReceive: RESTARTING MULTIPLE TIMES");
                    }
                    break;
                // if there would be no recognition of movement during next 5 mins -> turn off sensor
//                case BatteryOptimizer.SLEEP_DETECTION:
//                    batteryOptimizer.setActivity(BatteryOptimizer.STILL);
//                    batteryOptimizer.cancelWaitWalk();
//                    batteryOptimizer.resetProxi();
//                    break;
                default:
                    FirebaseCrashlytics.getInstance().recordException(new Exception("No state - " + action));

            }
        }
    }
}

