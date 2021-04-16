package motionapps.besafebox.datatools.signal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.datatools.storage.IndexStorage;
import motionapps.besafebox.datatools.storage.SensorOutput;

import static motionapps.besafebox.datatools.signal.Parameters.adCalc;
import static motionapps.besafebox.datatools.signal.Parameters.average;
import static motionapps.besafebox.datatools.signal.Parameters.caCos;
import static motionapps.besafebox.datatools.signal.Parameters.caNumber;
import static motionapps.besafebox.datatools.signal.Parameters.crestFactor;
import static motionapps.besafebox.datatools.signal.Parameters.crossingsCounter;
import static motionapps.besafebox.datatools.signal.Parameters.freeFallIndex;
import static motionapps.besafebox.datatools.signal.Parameters.kurtosis;
import static motionapps.besafebox.datatools.signal.Parameters.minMax;
import static motionapps.besafebox.datatools.signal.Parameters.crossingsRatio;
import static motionapps.besafebox.datatools.signal.Parameters.skewness;

/**
 * class for signal interactions
 */

public class SignalProcess {

    /**
     * @param time - time in nanoseconds
     * @return - linkedlist in milliseconds
     */
    public static LinkedList<Long> normalizeTime(LinkedList<Long> time){
        LinkedList<Long> normalized = new LinkedList<>();
        for(long t : time){
            normalized.addLast(TimeUnit.MILLISECONDS.convert(t, TimeUnit.NANOSECONDS));
        }
        return normalized;
    }

    /**
     * @param outputs - sensorOutputs
     * @return - time starting with zero
     */
    private static LinkedList<Long> timeBeginningSub(LinkedList<SensorOutput> outputs){
        LinkedList<Long> subtracted = new LinkedList<>();
        long first = outputs.get(0).time;
        for(SensorOutput sensorOutput : outputs){
            subtracted.addLast(sensorOutput.time - first);
        }
        return subtracted;
    }

    /**
     * @param time - vector of time
     * @return - time starting with zero
     */
    public static LinkedList<Long> timeBeginningSubLong(LinkedList<Long> time){
        LinkedList<Long> subtracted = new LinkedList<>();
        long first = time.get(0);
        for(long t : time){
            subtracted.addLast(t - first);
        }
        return subtracted;
    }

    /**
     * @param x - axis
     * @param y - axis
     * @param z - axis
     * @return - magnitude calculation
     */
    public static LinkedList<Double> magnitude(LinkedList<Double> x, LinkedList<Double> y, LinkedList<Double> z){
        LinkedList<Double> magnitude = new LinkedList<>();

        ListIterator<Double> iteratorX = x.listIterator(0);
        ListIterator<Double> iteratorY = y.listIterator(0);
        ListIterator<Double> iteratorZ = z.listIterator(0);

        double total;

        while (iteratorX.hasNext() && iteratorY.hasNext() && iteratorZ.hasNext()){
            total = Math.pow(iteratorX.next(),2)+Math.pow(iteratorY.next(),2)+Math.pow(iteratorZ.next(),2);
            magnitude.addLast(Math.sqrt(total));
        }

        return magnitude;
    }

    /**
     * @param doubles - vector values
     * @return - magnitude of the vector
     */
    static double magnitudeD(List<Double> doubles){
        double total = 0;
        for (double d : doubles) {
            total += Math.pow(d, 2);
        }
        return Math.sqrt(total);
    }

    public static double magnitude(float[] floats){
        float total = 0;
        for (float aFloat : floats) {
            total += Math.pow(aFloat, 2);
        }
        return Math.sqrt(total);
    }

    /**
     * @param values 3D values
     * @return - magnitude of whole signal
     */
    public static LinkedList<Double> magnitude(LinkedList<float[]> values){
        LinkedList<Double> magnitudes = new LinkedList<>();
        for (float[] aFloat : values) {
            magnitudes.addLast(magnitude(aFloat));
        }
        return magnitudes;
    }

    /**
     * @param outputs of the sensor
     * @return - magnitude of whole signal
     */
    private static LinkedList<Double> magnitudeVector(LinkedList<SensorOutput> outputs){
        LinkedList<Double> magnitudes = new LinkedList<>();
        for (SensorOutput output : outputs) {
            magnitudes.addLast(magnitude(output.values));
        }
        return magnitudes;
    }

    /**
     * @param dataCarrier - carrier of all data and indexes
     * @return - average activity after event of interest (comparison with ACTIVITY_THRESHOLD)
     */
    public static double getActivity(DataCarrier dataCarrier) {

        // if the magnitude or time is absent - recalculation
        LinkedList<Double> magnitude = dataCarrier.getData(DataCarrier.MAGNITUDE);
        LinkedList<Long> time = dataCarrier.getData(DataCarrier.NORMALIZED_TIME);

        if(magnitude == null || time == null) {
            magnitude = SignalProcess.magnitudeVector(dataCarrier.getValues());
            time = SignalProcess.timeBeginningSub(dataCarrier.getValues());

            dataCarrier.addData(DataCarrier.MAGNITUDE, magnitude);
            dataCarrier.addData(DataCarrier.NORMALIZED_TIME, time);
        }

        // max value in signal
        IndexStorage indexStorage = dataCarrier.getIndexStorage();
        Parameters.getMaxIndexTime(time, magnitude, indexStorage);

        int topBorder = magnitude.size()-1;

        // 1.5 seconds after the event
        ListIterator<Double> magnitudeIterator = magnitude.listIterator(indexStorage.getMaxValueIndex());
        ListIterator<Long> timeIterator = time.listIterator(indexStorage.getMaxValueIndex());
        int index = indexStorage.getMaxValueIndex();
        while(magnitudeIterator.hasNext() && timeIterator.hasNext()){
            if (Math.abs(timeIterator.next() - time.get(indexStorage.getMaxValueIndex())) > 1500) { //1.5s
                if(topBorder == magnitude.size()-1){
                    topBorder = index;
                    indexStorage.setEndIndex(index);
                }
                break;
            }

            index++;
            double value = magnitudeIterator.next();
            if(value > 15){
                topBorder = index;
                indexStorage.setEndIndex(index);
            }
        }

        // safeguards for the
        if(topBorder == (time.size()-1)){
            return 9.81;
        }

        if(indexStorage.getEndIndex() == (magnitude.size()-1)){
            return 9.81;
        }

        return Parameters.average(magnitude.subList(indexStorage.getEndIndex(), magnitude.size()-1));

    }

    /**
     * @param dataCarrier - carrier of data and indexes
     * @return - search for period with values below 2 m/s2, which wil be at elast 50 ms long and
     * in range of 750 mx from peak value
     */
    public static boolean isFreeFall(DataCarrier dataCarrier){
        LinkedList<Double> magnitude = dataCarrier.getData(DataCarrier.MAGNITUDE);
        LinkedList<Long> time = dataCarrier.getData(DataCarrier.NORMALIZED_TIME);

        if(magnitude == null || time == null) {
            magnitude = SignalProcess.magnitudeVector(dataCarrier.getValues());
            time = SignalProcess.timeBeginningSub(dataCarrier.getValues());

            dataCarrier.addData(DataCarrier.MAGNITUDE, magnitude);
            dataCarrier.addData(DataCarrier.NORMALIZED_TIME, time);
        }

        int index = magnitude.indexOf(Collections.max(magnitude));
        long peakTime = time.get(index);

        ArrayList<Integer> indexes  = new ArrayList<>();

        ListIterator<Long> iteratorTime = time.listIterator(index);
        ListIterator<Double> iteratorMagnitude = magnitude.listIterator(index);
        int indexer = index;

        // 750 ms range from peak into the past of the signal
        while(iteratorTime.hasPrevious() && iteratorMagnitude.hasPrevious()){
            indexer--;
            long timePrevious = iteratorTime.previous();
            if(Math.abs(peakTime - timePrevious) > 750){
                break;
            }
            if(iteratorMagnitude.previous() < 2 && Math.abs(peakTime - timePrevious) > 50) {
                indexes.add(indexer);
            }
        }

        if(indexes.size() == 0){
            return false;
        }
        // search for period longer than 50 ms
        ArrayList<Long> times = new ArrayList<>();
        long longestFluent = 0L;
        for(int k = 1; k < indexes.size(); k++){
            // tolerance for low sampling rate is 20 ms
            if(indexes.get(k) - indexes.get(k-1) < 20L){
                longestFluent += Math.abs(time.get(indexes.get(k)) - time.get(indexes.get(k-1)));
            }else{
                if(longestFluent != 0L){
                    times.add(longestFluent);
                    longestFluent = 0L;
                }
            }
        }

        times.add(longestFluent);

        long max = Collections.max(times);
        return max > 50L;

    }

    /**
     * @param dataCarrier - carrier of the signals and indexes
     * @return - boolean - if everything went well - all indexes are stored into DataCarrier and IndexStorage
     */
    public static boolean getMainArray(DataCarrier dataCarrier){

        ArrayList<Long> time = new ArrayList<>(dataCarrier.getData(DataCarrier.NORMALIZED_TIME));
        ArrayList<Double> magnitude = new ArrayList<>(dataCarrier.getData(DataCarrier.MAGNITUDE));

        IndexStorage indexStorage = dataCarrier.getIndexStorage();
        indexStorage.setBeginIndex(0);

        if(magnitude.size() - indexStorage.getMaxValueIndex() <= 0){
            return false;
        }

        for (int i = indexStorage.getMaxValueIndex(); i >= 0; i--){
            // search for period of acceleration below 9 m/s2 - if there is nothing to be found - the event starts 0.3 seconds before main peak
            if(Math.abs(indexStorage.getMaxValueTime() - time.get(i)) > 300){
                indexStorage.setBeginIndex(i);
                break;
            }
            //first sample below 9 m/s2

            if(magnitude.get(i) <= 9){
                //searching for other samples
                double value = magnitude.get(i);
                int index = i, counter = 0;

                indexStorage.setFreeFall(true); // saving result, that there is free fall
                indexStorage.setEndFree(i);

                while(true){
                    if(index-1 == 0){
                        indexStorage.setBeginIndex(0);
                        break;
                    }

                    // there can be other interactions - e.g. phone interacts with body / pocket
                    // ignoring 5 samples above 9.25 m/2
                    if(value > 9.25){
                        ++counter;
                        if(counter >= 5){ // stopping the search, when there is more than 5 samples above 9.25 m/s2
                            for(int k = 0; k < 5; k++){
                                //ich vymazanie
                                indexStorage.setBeginIndex(index);
                                if(magnitude.get(index+k) < 20){
                                    indexStorage.setBeginIndex(index+k);
                                    break;
                                }
                            }
                            break;

                        }
                        // next sample
                        value = magnitude.get(index--);
                        continue;
                    }else{
                        counter = 0; // resets counter, with another sample below 9.25 m/s2
                    }

                    // storing 0.3s index
                    if(Math.abs(indexStorage.getMaxValueTime() - time.get(index)) > 300){ // 0.3s
                        indexStorage.setBeginIndex(index);
                        break;
                    }

                    value = magnitude.get(index--);
                }
                break;

            }
        }

        // creating lists with needed data
        LinkedList<Double> interesting = new LinkedList<>();
        LinkedList<SensorOutput> sensorOutputs = new LinkedList<>();
        LinkedList<Double> fromEndFreeFall =  new LinkedList<>();

        ListIterator<SensorOutput> iteratorOutputs = dataCarrier.getValues().listIterator(indexStorage.getBeginIndex());
        ListIterator<Double>iteratorMagnitude = magnitude.listIterator(indexStorage.getBeginIndex());

        int indexer = indexStorage.getBeginIndex();
        double magnitudeTemp;

        while(iteratorMagnitude.hasNext()){
            indexer++;
            if(indexer >= indexStorage.getEndIndex()){
                break;
            }
            magnitudeTemp = iteratorMagnitude.next();
            interesting.addLast(magnitudeTemp);
            sensorOutputs.addLast(iteratorOutputs.next());

            if(indexer >= indexStorage.getEndFree()){
                fromEndFreeFall.addLast(magnitudeTemp);
            }
        }

        // saving al interesting part of the signal
        dataCarrier.addData(DataCarrier.INTERESTING_MAGNITUDE, interesting);
        dataCarrier.addData(DataCarrier.INTERESTING, sensorOutputs);
        dataCarrier.addData(DataCarrier.INTERESTING_AFTER_FREE_FALL, fromEndFreeFall);
        indexStorage.setMinValue(Collections.min(interesting));

        return indexStorage.getBeginIndex() != 0;
    }

    /**
     * Checks if all values are between 0 and 1
     * @param features - for model - list
     */
    private static void checkBorders(ArrayList<Double> features){
        for (int i = 0; i < features.size(); i++) {
            if(features.get(i) > 1){
                features.set(i, 1.0);
            }else if(features.get(i) < 0){
                features.set(i, 0.0);
            }
        }
    }

    /**
     * Checks if all values are between 0 and 1
     * @param features - for model - array
     */
    private static void checkBorders(double[] features){
        for (int i = 0; i < features.length; i++) {
            if(features[i] > 1){
                features[i] = 1.0;
            }else if(features[i] < 0){
                features[i] = 0.0;
            }
        }
    }


    /**
     * @param dataCarrier - carrier of the signals and indexes
     * @return - calculation of the parameters between 0 - 1
     * values are normalised with values from dataset
     */
    public static ArrayList<Double> getFallParams(DataCarrier dataCarrier) {


        double crest = (crestFactor(dataCarrier.getData(DataCarrier.INTERESTING_MAGNITUDE))-0.2243184931948999)/
                (2.342395696320005-0.2243184931948999);
        double ca_n = (caNumber(dataCarrier.getValues())-1.5669456462903149)/(10.370303761997937-1.5669456462903149);
        double ca_c = (caCos(dataCarrier)-0.3272040851208985)/(169.16406031488282-0.3272040851208985);
        double ad = (adCalc(dataCarrier.getValues())-0.41380159690396107)/(3.8362189666966633-0.41380159690396107 );
        double mm = (minMax(dataCarrier)-6.419273195905566)/(87.51390165162407-6.419273195905566);

        double g3 = crossingsRatio(dataCarrier.getData(DataCarrier.INTERESTING_AFTER_FREE_FALL), 30)/0.4146341463414634;

        double a = Parameters.average(dataCarrier.getData(DataCarrier.INTERESTING_AFTER_FREE_FALL));
        double k = (kurtosis(dataCarrier.getData(DataCarrier.INTERESTING_AFTER_FREE_FALL), a)+1.8505589310020305)
                /(9.227833565801843);
        double s = (skewness(dataCarrier.getData(DataCarrier.INTERESTING_AFTER_FREE_FALL), a)
                +1.608315282767581)/(5.241063808363597);

        ArrayList<Double> features = new ArrayList<Double>(){{
            add(crest);
            add(ca_n);
            add(ca_c);
            add(ad);
            add(mm);
            add(g3);
            add(k);
            add(s);
        }};

        // check if everything is between 0 - 1
        checkBorders(features);

        return features;
    }


    public static double[] getFallParamsNew(DataCarrier dataCarrier) {
        double ca_c = caCos(dataCarrier);
        double avg = average(dataCarrier.getData(DataCarrier.INTERESTING_MAGNITUDE));
        double kurtosis = kurtosis(dataCarrier.getData(DataCarrier.INTERESTING_AFTER_FREE_FALL));
        double crossings = crossingsCounter(dataCarrier.getData(DataCarrier.INTERESTING_AFTER_FREE_FALL), 9.25);
        double ffi = freeFallIndex(dataCarrier);

        double[] features = {
                (ca_c - 0.327204) / (179.193589 - 0.327204),
                crossings / 14.0,
                (avg - 6.015552) / (29.871963 - 6.015552),
                (kurtosis - 1.149441)/(11.990034 - 1.149441),
                (ffi - 1.128973)/(14.958949 - 1.128973)
        };

        checkBorders(features);

        return features;
    }
}
