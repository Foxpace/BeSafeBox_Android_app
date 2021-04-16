package motionapps.besafebox.tools;



import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * starts sensors and waits for the stop signal, so the number of needed samples can be calculated
 */
public class TestSampling implements SensorEventListener {

    private final SensorManager mSensorManager;
    private final Sensor[] sensors;

    private boolean registered = false;
    private final String TAG = "SensorHandler";

    // we are focusing only on acceleration - more sensors would require a lot of power
    static public int[] SENSOR_TYPES = new int[]{
            Sensor.TYPE_ACCELEROMETER,
            //Sensor.TYPE_LINEAR_ACCELERATION,
            //Sensor.TYPE_GYROSCOPE,
            //Sensor.TYPE_MAGNETIC_FIELD,
            //Sensor.TYPE_ROTATION_VECTOR
    };

//    public static int[] WALK_SENSORS = new int[]{
//            Sensor.TYPE_STEP_DETECTOR,
//            Sensor.TYPE_STEP_COUNTER
//    };
//    private boolean pedometer = false;

    static public ArrayList<String> SENSOR_NAMES = new ArrayList<String>() {{
        add("ACG");
//        add("ACC");
//        add("GYRO");
//        add("MAGNET");
//        add("ROTATION");
    }};

    private final int[] counts = new int[SENSOR_TYPES.length];

    /**
     * init of the sensors
     */
    public TestSampling(SensorManager sensorManager) {

        this.mSensorManager = sensorManager;
        sensors = new Sensor[SENSOR_TYPES.length];

        if (mSensorManager != null) {
            for (int i = 0; sensors.length > i; i++) {
                sensors[i] = mSensorManager.getDefaultSensor(SENSOR_TYPES[i]);
                if (sensors[i] != null) {
                    Log.i(TAG, "Registered" + sensors[i].getName());
                }
            }

//            for( int sensorType : WALK_SENSORS){
//                Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
//                if(sensor != null){
//                    pedometer = true;
//                    break;
//                }
//            }

        }
    }



    public void registerSensors() {
        if (mSensorManager != null && !registered) {
            for (Sensor sensor : sensors) {
                if (!(mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL))) {
                    Log.i(TAG, sensor.getName() + "is registered");
                }
            }
        }
        registered = true;
    }


    public void unregisterSensors() {
        if (registered) {
            mSensorManager.unregisterListener(this);
            registered = false;
            Log.i(TAG, "sensors unregistered");
        }
    }


    //public HashMap<String, Float> getMaxValues() {
    //    return maxValues;
    //}

    /**
     * counts every sample
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    counts[0]++;
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    counts[1]++;
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    counts[2]++;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    counts[3]++;
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    counts[4]++;
                    break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    public int[] getCounts() {
        return counts;
    }

//    public boolean isPedometer() {
//        return pedometer;
//    }

}
