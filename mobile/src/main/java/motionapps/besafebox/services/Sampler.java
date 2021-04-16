package motionapps.besafebox.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.tools.TestSampling;

import android.os.PowerManager;
import android.util.Log;


/**
 * Sensor sampling frequency acquisition service - Android usually already has this information
 * android with lower versions do not have this information, so the sensors run for 10 s and the
 * service recalculates approximately how big a window is needed
 * it is mainly used to estimate the size of the temporary memory for the detector
 * - launches right at the beginning of the application opening only one time, while introduction
 */

public class Sampler extends Service {


    String TAG = "IntentService";
    CountDownTimer countDownTimer;
    Context context;
    PowerManager.WakeLock wakeLock;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        context = this;

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if(powerManager != null && wakeLock == null) {

            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "Sampler::WalkWakeLock");

            if(wakeLock != null && !wakeLock.isHeld()){
                wakeLock.acquire(15000L);
            }else{
                Log.e(TAG, "Wakelock is null");
            }

        }else{
            Log.e(TAG, "PowerManager is null");
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            startCounting();
        }else if(!checkMaxDelays()){
            startCounting();
        }

        return START_STICKY;
    }

    /**
     * starts countdown and sensors - we can predict, that they will go on full speed, because user
     * is using phone actively
     */
    private void startCounting(){
        TestSampling testSampling = new TestSampling((SensorManager) getSystemService(SENSOR_SERVICE));
        testSampling.registerSensors();
        countDownTimer = new CountDownTimer(10000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, String.valueOf(millisUntilFinished));
            }

            @Override
            public void onFinish() {

                testSampling.unregisterSensors();
                int[] counts = testSampling.getCounts();
                for(int i = 0; i < TestSampling.SENSOR_TYPES.length; i++){
                    PreferencesHolder.onChange(context, TestSampling.SENSOR_TYPES[i]+"_MAX", counts[i] > 0 ? counts[i] : -999f);
                    Log.i(TestSampling.SENSOR_NAMES.get(i), String.valueOf(counts[i]));
                }

                stopSelf();
            }
        }.start();
    }

    /**
     * Uses methods of sensors to calculate number of needed samples - Lollipop and above only
     * @return if the sensors have required parameters to calculate number of samples
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean checkMaxDelays() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if(sensorManager == null) return false;

        for (int sensorType : TestSampling.SENSOR_TYPES) {
            Sensor sensor = sensorManager.getDefaultSensor(sensorType);

            if(sensor == null) return false;

            int delay = sensor.getMinDelay();

            if(delay <= 0){
                PreferencesHolder.onChange(context, sensor.getType() + "_MAX", -999);
                continue;
            }

            int count = 20000000 / delay;
            Log.i(sensor.getName(), String.valueOf(count));
            PreferencesHolder.onChange(context, sensor.getType() + "_MAX", count);
        }
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
