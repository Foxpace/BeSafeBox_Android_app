package motionapps.besafebox;

import android.content.Context;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.datatools.storage.SensorOutput;

import static motionapps.besafebox.datatools.signal.SignalProcess.magnitude;
import static motionapps.besafebox.datatools.signal.SignalProcess.normalizeTime;
import static motionapps.besafebox.datatools.signal.SignalProcess.timeBeginningSubLong;

public class HelperClasses {

    /**
     * searches for Acceleration data in folders
     * @return list of paths to acceleration data
     */
    public static ArrayList<String> getPaths(){
        // getting paths
        Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
        AssetManager assetManager = ctx.getAssets();
        ArrayList<String> pathsFolders = readFiles(assetManager, "falls");
        ArrayList<String> pathsACGs = new ArrayList<>();
        for (String path : pathsFolders) {
            pathsACGs.addAll(readFiles(assetManager, path));
        }

        // searching for all acceleration data
        for (Iterator<String> iterator = pathsACGs.iterator(); iterator.hasNext(); ) {
            String value = iterator.next();
            if (!value.contains("ACG_0.csv")) {
                iterator.remove();
            }
        }

        return pathsACGs;
    }

    /**
     * reading files from the folder
     *
     * @param mgr  - assetManager
     * @param path - path to folder
     * @return list of files in folder
     */
    private static ArrayList<String> readFiles(AssetManager mgr, String path) {
        ArrayList<String> paths = new ArrayList<>();
        try {
            String[] list = mgr.list(path);
            if (list != null)
                for (String aList : list) {
                    paths.add(path + "/" + aList);
                }
        } catch (IOException e) {
            Log.v("List error:", "can't list" + path);
        }
        return paths;
    }

    /**
     * transforming lines of csv files into lists and arrays of data, normalise time and creates SensorOutputs
     * @param reader - bufferReader to csv file
     * @return - ReadData, which aggregates data
     * @throws IOException - wrong path
     */
    public static HelperClasses.ReadData getOutputs(BufferedReader reader) throws IOException {
        LinkedList<Long> time = new LinkedList<>();
        LinkedList<Double> x = new LinkedList<>();
        LinkedList<Double> y = new LinkedList<>();
        LinkedList<Double> z = new LinkedList<>();
        LinkedList<Long> millis;
        reader.readLine();
        while (reader.ready()) {
            String line = reader.readLine();
            int i = 0;
            for (String s : line.split(";")) {
                switch (i) {
                    case 0:
                        time.addLast(Long.valueOf(s));
                        break;
                    case 1:
                        x.addLast(Double.valueOf(s));
                        break;
                    case 2:
                        y.addLast(Double.valueOf(s));
                        break;
                    case 3:
                        z.addLast(Double.valueOf(s));
                        break;
                }
                i++;
            }
        }
        millis = normalizeTime(time);
        LinkedList<SensorOutput> sensorOutputs = new LinkedList<>();
        for (int i = 0; i < time.size(); i++) {
            sensorOutputs.add(new SensorOutput(Sensor.TYPE_ACCELEROMETER, time.get(i),
                    new float[]{x.get(i).floatValue(), y.get(i).floatValue(), z.get(i).floatValue()}, 0));
        }
        return new HelperClasses.ReadData(time, x, y, z, sensorOutputs, millis);
    }

    /**
     * stores data of single measurement
     */
    static class ReadData {
        private final LinkedList<Long> time;
        private final LinkedList<Long> millis;
        private final LinkedList<Double> x, y, z;
        private final LinkedList<SensorOutput> sensorOutputs;

        ReadData(LinkedList<Long> time,
                 LinkedList<Double> x,
                 LinkedList<Double> y,
                 LinkedList<Double> z,
                 LinkedList<SensorOutput> sensorOutputs, LinkedList<Long> millis) {
            this.sensorOutputs = sensorOutputs;
            this.time = time;
            this.x = x;
            this.y = y;
            this.z = z;
            this.millis = millis;
        }

        public LinkedList<Double> getX() {
            return x;
        }

        public LinkedList<Double> getY() {
            return y;
        }

        public LinkedList<Double> getZ() {
            return z;
        }

        LinkedList<Long> getTime() {
            return time;
        }

        LinkedList<SensorOutput> getSensorOutputs() {
            return sensorOutputs;
        }

        double[] getXArray() {
            return getArrayDouble(x);
        }

        double[] getYArray() {
            return getArrayDouble(y);
        }

        double[] getZArray() {
            return getArrayDouble(z);
        }

        long[] getMillisArray() {
            return getArrayLong(millis);
        }

        private double[] getArrayDouble(LinkedList<Double> values) {
            double[] array = new double[values.size()];
            int counter = 0;
            for (double value : values) {
                array[counter++] = value;
            }
            return array;
        }

        private long[] getArrayLong(LinkedList<Long> values) {
            long[] array = new long[values.size()];
            int counter = 0;
            for (long value : values) {
                array[counter++] = value;
            }
            return array;
        }
    }


    /**
     * transforms acceleration file to DataCarrier
     * @param appContext - context
     * @param file - path to ACG file
     * @return DataCarrier
     * @throws IOException - wrong path
     */
    public static DataCarrier readFile(Context appContext, String file) throws IOException{

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                appContext.getAssets().open(file)));

//
        ReadData outputs = getOutputs(reader); // reading file

        // forming needed objects for classification
        LinkedList<Double> magnitude = magnitude(outputs.getX(), outputs.getY(), outputs.getZ());
        LinkedList<Long> time = normalizeTime(timeBeginningSubLong(outputs.getTime()));

        DataCarrier dataCarrier = new DataCarrier(outputs.getSensorOutputs());
        dataCarrier.addData(DataCarrier.NORMALIZED_TIME, time);
        dataCarrier.addData(DataCarrier.MAGNITUDE, magnitude);

        return dataCarrier;
    }
}
