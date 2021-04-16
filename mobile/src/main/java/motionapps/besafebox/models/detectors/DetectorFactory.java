package motionapps.besafebox.models.detectors;

import android.util.Log;

import motionapps.besafebox.model_managers.ModelManager;


/**
 * creation of the detectors
 */
public class DetectorFactory {

    private static final String TAG = "Detector Factory";


    public static Detector newInstance(ModelManager modelManager, Integer type) {
        if (type == null) {
            type = Detector.FALL_NEW;
        }
        return createDetector(modelManager, type);
    }

    private static Detector createDetector(ModelManager modelManager, int type) {

        Detector detector;

        switch (type) {
            case Detector.FALL:
                detector = new DetectorFall(modelManager);
                Log.i(TAG, "Fall detector created");
                break;

            case Detector.FALL_NATIVE:
                detector = new DetectorFallNative(modelManager);
                Log.i(TAG, "Fall-native detector created");
                break;
            case Detector.FALL_NEW:
                detector = new DetectorNewFall(modelManager);
                Log.i(TAG, "Fall detector created");
                break;
            //case Detector.CAR:
            //    detector = new DetectorCar(modelManager);
            //    Log.i(TAG, "Car detector created");
            //    break;

            default:
                detector = new DetectorNewFall(modelManager);
                Log.e(TAG, "Fall detector created - problem with type");
                break;
        }


        return detector;
    }

}
