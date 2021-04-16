package motionapps.besafebox.models.detectors;

import java.util.ArrayList;

import motionapps.besafebox.datatools.signal.SignalProcess;
import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.model_managers.ModelManager;
import motionapps.besafebox.models.sklearn.RandomForestClassifier;

/**
 * new version of the model from https://github.com/Foxpace/BeSafeBox_research
 */

public class DetectorNewFall extends DetectorFall {


    DetectorNewFall(ModelManager modelManager) {
        super(modelManager);
    }

    @Override
    void executeClassification(ArrayList<DataCarrier> dataCarriers){
        double[] features = SignalProcess.getFallParamsNew(dataCarriers.get(0));
        if (RandomForestClassifier.predict(features) == 1) modelManager.sendAlert(FALL);
    }
}
