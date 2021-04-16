package motionapps.besafebox.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import motionapps.besafebox.models.detectors.Detector;
import motionapps.besafebox.model_managers.ModelManager;


/**
 * registration of required sensors
 */
public class SensorHandler {

    private final SensorManager mSensorManager;
    private Sensor[] sensors;
    private SignificantSensor significantSensor;


    private boolean registered = false;
    private final String TAG = "SensorHandler";


//    private final HashMap<String, Float> maxValues = new HashMap<>(); // for normalization of data 0-1

    public SensorHandler(ModelManager modelManager, Detector detector, SensorManager sensorManager) {

        this.mSensorManager = sensorManager;

        if(registered){
            unregisterTracker(modelManager);
        }

        if(detector != null) {
            createSensors(detector.getSensors(), modelManager);
        }
    }

    //public boolean registerSignificant(SignificantSensor.onMotionListener onMotionListener) {
    //    Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
    //    if(sensor != null){
    //        significantSensor = new SignificantSensor();
    //        significantSensor.setListener(onMotionListener);
    //        significantSensor.setSensor(sensor);
    //        Log.i(TAG,  "Registering significant");
    //        return mSensorManager.requestTriggerSensor(significantSensor, sensor);
    //    }
    //    Log.e(TAG, "registerSignificant: No sensor");
    //    return false;
    //}


    /**
     * finds all the required sensors
     * @param sensors IDs of the sensors
     * @param modelManager - main model object
     */
    public void createSensors(Sensors[] sensors, ModelManager modelManager) {

        if(registered){
            unregisterTracker(modelManager);
        }

        this.sensors = new Sensor[sensors.length];
        if (mSensorManager != null) {
            int i = 0;
            for (Sensors sensor : sensors) {
                this.sensors[i] = mSensorManager.getDefaultSensor(sensor.getSensor());
//                if (sensors[i] != null) {
//                    maxValues.put(sensor.getShortName(), sensors[i].getMaximumRange());
//                }
                i++;
            }
        }
    }


    /**
     * @param modelManager registers sensors to modelManager
     */
    public void registerSensors(ModelManager modelManager) {

        if (mSensorManager != null && !registered) {
            for (Sensor sensor : sensors) {
                if (mSensorManager.registerListener(modelManager, sensor, SensorManager.SENSOR_DELAY_FASTEST)) {
                    Log.i(TAG, sensor.getName() + " is registered");
                }
            }
        }
        registered = true;

    }



    public Sensor[] getSensors() {
        return sensors;
    }

//    public HashMap<String, Float> getMaxValues() {
//        return maxValues;
//    }

    /*
    public void registerSignificant() {
        Sensor significant = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        significantMotion = new SignificantMotion();
        mSensorManager.registerListener(significantMotion, significant, SensorManager.SENSOR_DELAY_NORMAL);
    }*/


    /**
     * @param sensorEventListener - unregisters any sensor
     */
    public void unregisterTracker(SensorEventListener sensorEventListener) {
        if (registered) {
            mSensorManager.unregisterListener(sensorEventListener);
            /*if (significantMotion != null) {
                mSensorManager.unregisterListener(significantMotion);
            }*/
            Log.i(TAG,  "sensors unregistered");
            registered = false;
        }
    }

    public void unregisterSignificant() {
        if (significantSensor != null) {
            mSensorManager.cancelTriggerSensor(significantSensor, significantSensor.getSensor());
            significantSensor = null;
            Log.i(TAG,  "Significant canceled");
        }
    }

}