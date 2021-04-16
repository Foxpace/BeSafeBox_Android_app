package motionapps.besafebox.models.detectors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.datatools.storage.SensorOutput;
import motionapps.besafebox.gps.GPSParameters;
import motionapps.besafebox.model_managers.ModelManager;
import motionapps.besafebox.R;
import motionapps.besafebox.sensors.Sensors;



public class DetectorFallNative extends Detector {

    private boolean objectsCreated;
    public static final String NATIVE_ALARM = "NATIVE_ALARM";

    public DetectorFallNative(Context context){
        this.StringID = R.string.detection_fall;
        this.type = FALL_NATIVE;
        initCPP(context);
        objectsCreated = true;
    }

    DetectorFallNative(@NonNull ModelManager modelManager) {
        this.modelManager = modelManager;
        this.StringID = R.string.detection_fall;
        this.type = FALL_NATIVE;
        initCPP(modelManager.getContext());
        objectsCreated = true;
    }

    /**
     * @param context - any
     * initialization of all C++ objects
     */
    private void initCPP(Context context){
        createObjects(getLimit(context), 10.25f, 70f);
    }

    /**
     * @param context any
     * @return integer for number of samples to store
     */
    private int getLimit(Context context){
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(getSensors()[0].getSensor());
        return PreferencesHolder.getInt(context, sensor.getType()+"_MAX", 250);
    }

    @Override
    public Sensors[] getSensors() {
        return new Sensors[]{Sensors.ACG};
    } // potrebny akcelerometer

    @Override
    public GPSParameters getGPS() {
        return GPSParameters.WALK_PARAMS;
    }

    @Override
    public void classify(ArrayList<DataCarrier> dataCarriers) {
        // not used in C++ implementation, because C++ classes handles everything
    }

    @Override
    public void detect(SensorOutput sensorEvent) {
        if(objectsCreated) { // passing all samples of the sensors to C++
            passData(sensorEvent.time, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        }
    }

    @Override
    public void destroy() {
        destroyObjects();
    }

    @Override
    public void reset() {
        objectsCreated = false;
        destroyObjects();
        initCPP(modelManager.getContext());
        objectsCreated = true;
    }

    static {
        System.loadLibrary("native-lib"); // loading C++ library
    }

    // JNI methods to manipulate with C++ objects
    private native void createObjects(int limit, float activitLimit, float nnLimit);
    private native void passData(long time, float x, float y, float z);
    private native void destroyObjects();
    public native int analyseTest(long[] time, double[] x, double[] y, double[] z);
}
