package motionapps.besafebox.models.detectors;

import android.hardware.Sensor;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import motionapps.besafebox.activities.options.preferences.PreferencesHolder;
import motionapps.besafebox.datatools.signal.SignalProcess;
import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.datatools.storage.SensorOutput;
import motionapps.besafebox.gps.GPSParameters;
import motionapps.besafebox.model_managers.ModelManager;
import motionapps.besafebox.R;
import motionapps.besafebox.models.tf.TfModel;
import motionapps.besafebox.sensors.Sensors;

/**
 * detector of fall with old approach
 */

public class DetectorFall extends Detector {

    private static final double ACCIDENT_ACCELERATION = 29.43; // 3g threshold for detection
    // TIME_TO_WAIT time to wait after indication of the fall, BUFFER time to wait, if the fall is occurring
    private static final long TIME_TO_WAIT = 5000L, BUFFER = 750L;
    private boolean elapsing, bufferB = false; // ignore indication if the detection is occurring
    private static final float ACTIVITY_THRESHOLD = 10.25f;
    private static final int THRESHOLD_NN_FALL = 70; // threshold for decision

    private TfModel classifier; // tf lite neural network
    // standalone executor for the neural network
    private final Executor executor = Executors.newSingleThreadExecutor();


    DetectorFall(ModelManager modelManager) {

        super(modelManager.getHandler());
        this.TAG = "DetectorFall";
        this.StringID = R.string.detection_fall;
        this.type = FALL;
        this.modelManager = modelManager;

        initTensorFlowAndLoadModel(); // initialization of the neural network

        // we do not expect fall in first 10s of working - user could drop phone while holding in hand
        elapsing = true;
        handler.postDelayed(() -> {
            elapsing = false;
            Log.i(TAG, "DetectorFall: Start sensing");
        }, 10_000L);
        Log.i(TAG, "DetectorFall: Elapsing begin of sensor");
        // latency to protect detector from inserting to pocket

    }

    @Override
    public Sensors[] getSensors() {
        return new Sensors[]{Sensors.ACG};
    }

    @Override
    public GPSParameters getGPS() {
        return GPSParameters.WALK_PARAMS;
    }

    @Override
    public void destroy() {
        TfModel.Companion.destroy();
        reset();
    }

    /**
     * reset of all analysis
     */
    @Override
    public void reset() {
        handler.removeCallbacksAndMessages(null);
        elapsing = false;
        bufferB = false;
    }

    /**
     * @param sensorEvent - samples of acceleration
     * 1. check if the samples are from accelerometer
     * 2. waiting for 3g acceleration
     * 3. wait for 750 ms
     * 4. waiting for required time for launch of the detection - obtaining later data
     * 5. if the next 3g peak comes after buffer, the detection is deleted
     */
    @Override
    public void detect(SensorOutput sensorEvent) {
        if (sensorEvent.type != Sensor.TYPE_ACCELEROMETER) return; // 1.
        if (SignalProcess.magnitude(sensorEvent.values) > ACCIDENT_ACCELERATION) { // 2.

            if (elapsing && !bufferB) { // 3.
                handler.removeCallbacksAndMessages(null);
                Log.i(TAG, "Accident acceleration - NN removed");
                elapsing = false;
            } else if (!elapsing) { // 4.
                bufferB = true;
                elapsing = true;
                Log.i(TAG, "Accident acceleration - postDelayed NN");
                handler.postDelayed(() -> {
                    modelManager.getData(new int[]{Sensor.TYPE_ACCELEROMETER}, true); // 5.
                    // handler.postDelayed(() -> elapsing = true,100L);
                }, TIME_TO_WAIT);

                handler.postDelayed(() -> bufferB = false, BUFFER);

            }
        }
    }


    /**
     * @param dataCarriers - data from sensors
     * @return - boolean, which decides about, if the phone fall on the ground
     */
    private boolean decideAboutFreeFall(ArrayList<DataCarrier> dataCarriers){
        if(PreferencesHolder.getBoolean(modelManager.getContext(), PreferencesHolder.POCKET_ACTIVE, false)){
            return true;
        }

        return !SignalProcess.isFreeFall(dataCarriers.get(0));
    }

    /**
     * @param dataCarriers - data with all the sensor samples with range of 10s
     */
    @Override
    public void classify(ArrayList<DataCarrier> dataCarriers) {

        if (dataCarriers == null) {
            reset();
            return;
        }

        // control of activity after fall
        if(SignalProcess.getActivity(dataCarriers.get(0)) >= ACTIVITY_THRESHOLD){
            handler.removeCallbacksAndMessages(null);
            Log.i(TAG, "classify: big activity");
            return;
        }

        if (!decideAboutFreeFall(dataCarriers)) { // check if the phone did not drop
            handler.removeCallbacksAndMessages(null);
            Log.i(TAG, "classify: it was free fall");
        }

        if (SignalProcess.getMainArray(dataCarriers.get(0))) { // centering detection
            Log.i(TAG, "Launching NN");
            executor.execute(() -> { // execution of classification
                try {
                    executeClassification(dataCarriers);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            });
        }


    }

    /**
     * calculates all the parameters and passes them to feed-forward neural network
     * @param dataCarriers - sensor data
     */
    void executeClassification(ArrayList<DataCarrier> dataCarriers){
        if(classifier == null) throw new NullPointerException("Null classifier");

        float chance = classifier.predict(modelManager.getContext(), SignalProcess.getFallParams(dataCarriers.get(0)));

        if (chance == -1) {
            Log.e(TAG, "Error in NN");
        } else if(chance * 100 < THRESHOLD_NN_FALL) {
            // comparison with threshold
            modelManager.sendAlert(FALL);
        }
    }

    private void initTensorFlowAndLoadModel() {
        TfModel.Companion.destroy();
        classifier = null;
        executor.execute(() -> { // thread is used to load model
            try {
                classifier = TfModel.Companion.create(modelManager.getContext());
                Log.d(TAG, "Load Success");
            } catch (final Exception e) {
                throw new RuntimeException("Error initializing TensorFlow!", e);
            }
        });
    }

    /**
     * @param v - dummy test for neural network
     */
    public void testModel(ArrayList<Double> v) {
        float chance = classifier.predict(modelManager.getContext(), v);
        if (chance == -1) {
            Toast.makeText(modelManager.getContext(), "Error in NN",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(modelManager.getContext(), String.format(
                    Locale.getDefault(), "Fall with chance of %f",
                    chance),
                    Toast.LENGTH_SHORT).show();

        }
    }

}
