package motionapps.besafebox;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.models.detectors.DetectorFallNative;
import motionapps.besafebox.models.sklearn.RandomForestClassifier;
import motionapps.besafebox.models.tf.TfModel;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static motionapps.besafebox.HelperClasses.getOutputs;
import static motionapps.besafebox.HelperClasses.getPaths;
import static motionapps.besafebox.HelperClasses.readFile;
import static motionapps.besafebox.datatools.signal.SignalProcess.getActivity;
import static motionapps.besafebox.datatools.signal.SignalProcess.getFallParams;
import static motionapps.besafebox.datatools.signal.SignalProcess.getFallParamsNew;
import static motionapps.besafebox.datatools.signal.SignalProcess.getMainArray;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class InstrumentedTest {


    /**
     * tests for the fall with data
     */

    private final String fallFile = "ACG_0_fall.csv", sitFile = "ACG_0_sit.csv";

    @Test
    public void fallTest() throws IOException {
        assertTrue(testFile(fallFile));
    }

    @Test
    public void sitTest() throws IOException {
        assertFalse(testFile(sitFile));
    }

    @Test
    public void fallTestCpp() throws IOException {
        assertTrue(testFileCpp(fallFile));
    }

    @Test
    public void sitTestCpp() throws IOException {
        assertFalse(testFileCpp(sitFile));
    }

    @Test
    public void fallTestNew() throws IOException {
        assertTrue(testFileNewApproach(fallFile));
    }

    @Test
    public void sitTestNew() throws IOException {
        assertFalse(testFileNewApproach(sitFile));
    }

    @Test
    public void moreFalls() throws IOException {

        ArrayList<String> pathsACGs = getPaths();

        double falls = 0;
        double notFalls = 0;
        for (String path : pathsACGs) {
            if (testFile(path)) { // testing single file
                falls++;
            } else {
                notFalls++;
            }
        }
        Log.e("Falls", String.format("%.2f correct classification, %.2f not correct",
                falls / pathsACGs.size(), notFalls / pathsACGs.size()));
        assertTrue(falls / pathsACGs.size() > 0.7);
    }

    @Test
    public void moreFallsCpp() throws IOException {

        ArrayList<String> pathsACGs = getPaths();

        double falls = 0;
        double notFalls = 0;
        for (String path : pathsACGs) { // testing cpp on files
            if (testFileCpp(path)) {
                falls++;
            } else {
                notFalls++;
            }
        }
        Log.e("Falls", String.format("%.2f correct classification, %.2f not correct",
                falls / pathsACGs.size(), notFalls / pathsACGs.size()));
        assertTrue(falls / pathsACGs.size() > 0.7);
    }

    @Test
    public void moreFallsNewApproach() throws IOException {

        ArrayList<String> pathsACGs = getPaths();

        double falls = 0;
        double notFalls = 0;
        for (String path : pathsACGs) {
            if (testFileNewApproach(path)) {
                falls++;
            } else {
                notFalls++;
            }
        }
        Log.e("Falls", String.format("%.2f correct classification, %.2f not correct",
                falls / pathsACGs.size(), notFalls / pathsACGs.size()));
        assertTrue(falls / pathsACGs.size() > 0.7);
    }

    /**
     * test for the old approach with tensorflow network
     * @param file - acceleration data
     * @return - boolean if it is fall or not
     * @throws IOException - wrong path
     */
    private boolean testFile(String file) throws IOException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getContext();
        DataCarrier dataCarrier = readFile(appContext, file);
        // processing data
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if(getActivity(dataCarrier) <= 10.25f) {
            if (getMainArray(dataCarrier)) {
                ArrayList<Double> doubles = getFallParams(dataCarrier);
                TfModel fallClassifierLite = TfModel.Companion.create(appContext);

                if (fallClassifierLite == null) return false;

                double chance = fallClassifierLite.predict(appContext, doubles);
                return chance < 0.7;
            }
        }

        return false;
    }

    /**
     * tests the old approach written in C++
     * @param file - acceleration data
     * @return - boolean if it is fall or not
     * @throws IOException - wrong path
     */
    private boolean testFileCpp(String file) throws IOException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getContext();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                appContext.getAssets().open(file)));

        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // creation of arrays
        HelperClasses.ReadData data = getOutputs(reader);

        DetectorFallNative detectorFallNative = new DetectorFallNative(appContext);

        // passing java arrays to native and test them
        return detectorFallNative.analyseTest(data.getMillisArray(), data.getXArray(),
                data.getYArray(), data.getZArray()) == 1;
    }

    /**
     * test for the new approach with fewer features and randomForest
     * @param file - acceleration data
     * @return - boolean if it is fall or not
     * @throws IOException - wrong path
     */
    private boolean testFileNewApproach(String file) throws IOException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getContext();
        DataCarrier dataCarrier = readFile(appContext, file);
        // processing data
        getActivity(dataCarrier);
        if (getMainArray(dataCarrier)) {
            double[] features = getFallParamsNew(dataCarrier);
            return RandomForestClassifier.predict(features) == 1;
        }
        return false;
    }


}
