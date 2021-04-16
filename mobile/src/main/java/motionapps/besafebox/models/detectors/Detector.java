package motionapps.besafebox.models.detectors;

import android.os.Handler;

import java.util.ArrayList;

import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.datatools.storage.SensorOutput;
import motionapps.besafebox.gps.GPSParameters;
import motionapps.besafebox.model_managers.ModelManager;
import motionapps.besafebox.sensors.Sensors;

/**
 * template for the detector, which has access to handlers, temp memory and send alarm
 */

public abstract class Detector{

    public final static int NONE = -1, FALL = 0, FALL_NATIVE = 2, FALL_NEW = 3; // CAR = 1

    int StringID, type;
    String TAG;
    ModelManager modelManager;
    Handler handler;

    Detector(){}

    Detector(Handler handler){
        this.handler = handler;
    }

    // getters for GPS parameters and required
    public abstract Sensors[] getSensors();
    public abstract GPSParameters getGPS();

    /**
     * Passes all required data for classification
     * @param dataCarriers - carrier for all data and indexes - array for all sensors
     */
    public abstract void classify(ArrayList<DataCarrier> dataCarriers);

    /**
     * Model uses detect method to pass samples
     * @param sensorEvent - sensor events
     */
    public abstract void detect(SensorOutput sensorEvent);

    public int getStringID(){return StringID;}
    public int getType(){
        return type;
    }
    public Handler getHandler() {
        return handler;
    }

    /**
     * on end of the detection
     */
    public abstract void destroy();

    /**
     * reset of detection
     */
    public abstract void reset();

    //public boolean isAutomated(){
    //    return automated;
    //}
}
