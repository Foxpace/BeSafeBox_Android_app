package motionapps.besafebox.model_managers;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.location.LocationAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;

import motionapps.besafebox.activities.Alarm;
import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.datatools.storage.DataManager;
import motionapps.besafebox.datatools.storage.SensorOutput;
import motionapps.besafebox.gps.GPSCallback;
import motionapps.besafebox.models.detectors.DetectorFallNative;
import motionapps.besafebox.notifications.Notify;
import motionapps.besafebox.models.detectors.Detector;
import motionapps.besafebox.models.detectors.DetectorFactory;
import motionapps.besafebox.sensors.SensorHandler;
import motionapps.besafebox.services.DetectionService;

import static android.content.Context.SENSOR_SERVICE;

/**
 * main class, which interconnect data storage, detector, GPS and life cycle of the app
 */

public class ModelManager implements SensorEventListener, GPSCallback.OnLocationChangedCallback{

    private final static String TAG = "ModelManager";
    private Context context;
    private DetectionService service;
    private Handler handler;

    private boolean working = false;

    private SensorHandler sensorHandler;
    private DataManager dataManager;
    private Detector detector;
    private GPSCallback gpsCallback;

    public static int getBasicType() {
        return detectorType;
    }


    /**
     * @param context - any
     * @param handler - executor of the processes
     * @param type - type of detector (there is only fall detection)
     */
    public void initModel(Context context, Handler handler, Integer type){
        this.context = context;
        this.handler = handler;

        if(context instanceof DetectionService){
            service = (DetectionService) context;
        }

        initModel(type);
    }

    /**
     * @param type - detector type
     * init of GPS, temporal memory, sensors and detector
     */

    private static final int detectorType = Detector.FALL_NEW;

    private void initModel(Integer type){
        Log.i(TAG, "initModel: turning on model");
        gpsCallback = new GPSCallback(context, this, true);

        detector = DetectorFactory.newInstance(this, type);

        if(detector == null) throw new NullPointerException("Detector is null - problem with init");

        sensorHandler = new SensorHandler(this, detector,
                (SensorManager) context.getSystemService(SENSOR_SERVICE));

        dataManager = new DataManager(this, sensorHandler.getSensors());

        onResume();

    }

    public boolean isIdle(){
        return working;
    }

    public boolean isDestroyed(){
        return context == null;
    }

    /**
     * activation of sensors and GPS
     */
    public void onResume(){
        Log.i(TAG, "onResume: model");
        working = true;
        sensorHandler.registerSensors(this);
        gpsCallback.changeRequest();
    }

    /**
     * removing GPS and sensors
     */
    public void onPause() {
        working = false;
        if(isDestroyed()){
            return;
        }
        sensorHandler.unregisterTracker(this);
        sensorHandler.unregisterSignificant();
        gpsCallback.gpsOff();

        Log.i(TAG, "onPause: model");
    }

    /**
     *  cancellation of all references, model is going to be destroyed
     */
    public void onDestroy(){

        onPause();
        Log.i(TAG, "onDestroy: model");

        sensorHandler = null;
        dataManager = null;
        gpsCallback = null;
        if(detector != null){
            detector.destroy();
        }
        detector = null;
        context = null;

    }

    /**
     * @param sensorEvent - output from sensor
     * system sends samples of acceleration -> save them -> process them
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(detector == null) return;

        // custom object
        SensorOutput sensorOutput = new SensorOutput(sensorEvent);

        // storing data temporally - C++ has its own storage
        if(!(detector instanceof DetectorFallNative)) dataManager.postSensorEvent(sensorOutput);

        // detecting events
        detector.detect(sensorOutput);

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onLocationChanged(Location location) {
        onLocation(location);
    }

    @Override
    public void onLastLocationSuccess(Location location) {
        onLocation(location);
    }

    /**
     * @param location - GPS output - storing last location
     *
     */
    private void onLocation(Location location){
        Log.i(TAG, "onLocation: changed");
        if(detector != null && location != null && detector.getGPS() != null) {
            dataManager.setLastLocation(location);
        }
    }

    @Override
    public void onAvailabilityChanged(LocationAvailability locationAvailability) {}


    /**
     * Detector is changed in real time
     * @param type - detector type
     */
    public void changeDetector(int type){
        if(type != detector.getType()) {
            onDetectorChange(DetectorFactory.newInstance(this, type));
        }
    }


    /**
     * @param detector - new detector
     * mainly needed, if there are multiple detectors with various requirements
     */
    private void onDetectorChange(Detector detector){

        //registering new sensors, GPS and storage
        sensorHandler.createSensors(detector.getSensors(), this);
        gpsCallback.setGpsParameters(detector.getGPS());

        dataManager = new DataManager(this, sensorHandler.getSensors());

        // destroy previous detector and set up new one
        this.detector.destroy();
        this.detector = detector;

        if(service != null) {
            Notify.updateForegroundNotification(context, context.getString(
                    detector.getStringID()), service.getFOREGROUND_ID(),
                    detector.getType());
        }
        working = false;
    }

    /**
     * @param actualType - type of the alarm - starts new activity, which can override lock and notifications
     * spustenie alarmu a zastavenie služby
     */
    public void sendAlert(int actualType) {
        if (PreferencesHolder.getBoolean(context, PreferencesHolder.ALARM_ASSIST, false) ||
                PreferencesHolder.getBoolean(context, PreferencesHolder.ALARM_ME, false)) {

            Intent intent = new Intent(context, Alarm.class);
            intent.putExtra(Alarm.REAL_ALARM, true);

            context.startActivity(intent);
            if(service != null) service.stopSelf();
        }
    }


    /**
     * @param requestedSensorTypes - required sensors IDs
     * @param lastTenSeconds - boolean - if we want data centered around 5th second
     */
    public void getData(int[] requestedSensorTypes, boolean lastTenSeconds){
        ArrayList<DataCarrier> dataCarriers = new ArrayList<>();
        for(int sensor : requestedSensorTypes){
            DataCarrier dataCarrier = dataManager.getData(sensor, lastTenSeconds);
            if(dataCarrier == null){
                dataCarriers = null;
                break;
            }
            dataCarriers.add(dataCarrier);
        }
        try {
            detector.classify(dataCarriers);
        }catch (Exception e){
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public Context getContext() {
        return context;
    }

//    public void tryModel() {
//        if(detector != null){
//            if(detector instanceof DetectorFall){
//                ((DetectorFall) detector).testModel(new ArrayList<Double>(){{addAll(Collections.nCopies(8, 0.0));}});
//            }
//        }
//    }

    public Handler getHandler() {
        return handler;
    }

    public Integer getDetectorType() {
        if(detector != null) {
            return detector.getType();
        }else{
            return -1;
        }
    }

    public Detector getDetector(){
        return detector;
    }

    /**
     * reset of the detector
     */
    public void cancelAllDetections() {
        detector.reset();
    }

    public int getPickedType() {
        return detectorType;
    }
}
