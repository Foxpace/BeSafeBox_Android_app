package motionapps.besafebox.datatools.signal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import motionapps.besafebox.datatools.storage.DataCarrier;
import motionapps.besafebox.datatools.storage.IndexStorage;
import motionapps.besafebox.datatools.storage.SensorOutput;

import static motionapps.besafebox.datatools.signal.SignalProcess.magnitude;
import static motionapps.besafebox.datatools.signal.SignalProcess.magnitudeD;


/**
 * class to calculate parameters for fall detection and other
 */
public class Parameters {

    public static double sum(List<Double> numbers){

        double total = 0;
        for (Double number : numbers){
            total += number;
        }
        return total;
    }

    public static double average(List<Double> numbers){
        return (sum(numbers)/numbers.size());
    }


    /**
     * @param time - time stamps in ms
     * @param magnitude - acceleration magnitude
     * @param storage - Index storage
     * centering the signal at the highest peak - between 4. and 6. second - from signal of length 10s,
     *                in the middle, there should be max peak
     */
    static void getMaxIndexTime(LinkedList<Long> time, LinkedList<Double> magnitude, IndexStorage storage){

        int fourth_second = 0;
        int sixth_second = 0; // borders for search

        ListIterator<Long> iterator = time.listIterator(0);

        int index = 0;
        while(iterator.hasNext()){
            long timeTemp = iterator.next();
            index++;
            if(timeTemp >= 4_000L && fourth_second == 0){
                fourth_second = index;

            }
            if(timeTemp >= 6_000L){
                sixth_second = index;
                break;
            }
        }

        if(sixth_second == 0){
            sixth_second = magnitude.size()-1;
        }

        // searching for max
        List<Double> window = magnitude.subList(fourth_second, sixth_second);
        double max = Collections.max(window);
        index = window.indexOf(max) + fourth_second;
        long time_max = time.get(index);

        if(max < 29.73){ // if the max is below threshold
            max = Collections.max(magnitude);
            index = magnitude.indexOf(max);
            time_max = time.get(index);
        }

        storage.setMaxValue(max); // saving of the values
        storage.setMaxValueIndex(index);
        storage.setMaxValueTime(time_max);

    }


    /**
     * @param values - acceleration magnitude
     * @return - crestov factor = maximum / quadratic average
     */
    static double crestFactor(LinkedList<Double> values){
        double numerator = 0.5*(Collections.max(values)-Collections.min(values));
        double pow2 = 0;
        for(Double value: values){
            pow2 += Math.pow(value, 2);
        }
        double denominator = Math.sqrt(pow2/((double) values.size()));
        return numerator / denominator;
    }

    /**
     * @param sensorOutputs acceleration full outputs
     * @return - sum of differences between X and Z axis with normalisation of length of the signal
     */
    static double caNumber(LinkedList<SensorOutput> sensorOutputs){
        double sum = 0;
        for (SensorOutput sensorOutput: sensorOutputs) {
            sum += Math.sqrt(Math.pow(sensorOutput.values[0], 2) +
                    Math.pow(sensorOutput.values[2], 2));
        }
        return sum/((double) sensorOutputs.size());

    }

    /**
     * @param dataCarrier - carrier of all signals and indexes
     * @return - dot product of averages of vectors before and after the peak which are normalised with multiplication of their amgnitude
     */
    static double caCos(DataCarrier dataCarrier) {
        ArrayList<LinkedList<float[]>> beforeAndAfter = getBeforeAndAfter(dataCarrier);

        List<Double> aa = avgVectors(beforeAndAfter.get(0));
        List<Double> ab = avgVectors(beforeAndAfter.get(1));

        double acca = magnitudeD(aa);
        double accb = magnitudeD(ab);

        return Math.toDegrees(Math.acos(dot1D(aa, ab)/(acca*accb)));

    }

    /**
     * @param dataCarrier - carrier of all signals and indexes
     * @return - 1s period before and after event
     */
    private static ArrayList<LinkedList<float[]>> getBeforeAndAfter(DataCarrier dataCarrier) {
        int beforeStart = 0;
        int beforeEnding = dataCarrier.getIndexStorage().getBeginIndex()-1;

        int afterStart = dataCarrier.getIndexStorage().getEndIndex()+1;
        int afterEnding = 0;

        LinkedList<Long> time = dataCarrier.getData(DataCarrier.NORMALIZED_TIME);

        // searching for 1 second before event
        long timeBeforeStart = time.get(beforeEnding);
        int index = 0;
        for (long t: time) {
            if(Math.abs(timeBeforeStart-t) <= 1000L){
                beforeStart = index;
                break;
            }
            index++;
        }

        // searching for 1 second after event
        long timeAfterStart = time.get(afterStart);
        index = afterStart;
        ListIterator<Long> iterator = time.listIterator(afterStart);
        while(iterator.hasNext()){
            if(Math.abs(timeAfterStart-iterator.next()) >= 1000L){
                afterEnding = ++index;
                break;
            }
            index++;
        }

        if(afterEnding == 0){
            afterEnding = time.size()-1;
        }

        LinkedList<SensorOutput> sensorOutputs = dataCarrier.getValues();
        LinkedList<float[]> before = new LinkedList<>() ;
        LinkedList<float[]> after = new LinkedList<>();

        for (int i = beforeStart; i < beforeEnding; i++) {
            before.addLast(sensorOutputs.get(i).values);
        }

        for (int i = afterStart; i < afterEnding; i++) {
            after.addLast(sensorOutputs.get(i).values);
        }

        return new ArrayList<LinkedList<float[]>>(){{
            add(before);
            add(after);
        }};
    }

    /**
     * @param values - list with 3D samples
     * @return - average along every single axis
     */
    private static List<Double> avgVectors(List<float[]> values){
        Double[] avgs = new Double[]{0.0, 0.0, 0.0};

        for (float[] floats: values) {
            avgs[0] = avgs[0] + floats[0];
            avgs[1] = avgs[1] + floats[1];
            avgs[2] = avgs[2] + floats[2];
        }

        for (int i = 0; i < 3; i++) {
            avgs[i] = avgs[i]/((double) values.size());
        }

        return Arrays.asList(avgs);

    }

    /**
     * @param l1 - vector 1
     * @param l2 - vector 2
     * @return - dot product of 2 vectors
     */
    private static double dot1D(List<Double> l1, List<Double> l2){
        double sum = 0;
        for (int i = 0; i < l1.size(); i++) {
            sum += l1.get(i) * l2.get(i);
        }
        return sum;
    }

    private static double dot1D(float[] l1, float[] l2){
        double sum = 0;
        for (int i = 0; i < l1.length; i++) {
            sum += l1[i] * l2[i];
        }
        return sum;
    }

    /**
     * @param sensorOutputs - acceleration samples
     * @return - average angle deviation from whole signal
     */
    static double adCalc(LinkedList<SensorOutput> sensorOutputs){
        double ad = 0, temp;
        int pass = 0;

        ListIterator<SensorOutput> iterator1 = sensorOutputs.listIterator(0);
        ListIterator<SensorOutput> iterator2 = sensorOutputs.listIterator(1);

        SensorOutput sensorOutput1;
        SensorOutput sensorOutput2;

        while (iterator1.hasNext() && iterator2.hasNext()){

            sensorOutput1 = iterator1.next();
            sensorOutput2 = iterator2.next();

            temp = Math.toDegrees(Math.acos(dot1D(sensorOutput2.values, sensorOutput1.values) /
                    (magnitude(sensorOutput2.values) * magnitude(sensorOutput1.values))));

            if(Double.isNaN(temp)) {
                pass++;
            }else {
                ad += temp;
            }
        }

        return ad/((double)((sensorOutputs.size()-pass)));
    }

    /**
     * @param dataCarrier - carrier of all signals and indexes
     * @return - difference between max and min
     */
    static double minMax(DataCarrier dataCarrier) {
        IndexStorage indexStorage = dataCarrier.getIndexStorage();
        return indexStorage.getMaxValue() - indexStorage.getMinValue();
    }

    /**
     * @param data - acceleration magnitude
     * @return - ratio of samples with acceleration higher than threshold
     */
    static double crossingsRatio(LinkedList<Double> data, double threshold) {
        int plus = 0;
        int minus = 0;

        for (double v: data) {
            if (v > threshold) {
                plus++;
            } else {
                minus++;
            }
        }

        return ((double) plus)/((double) minus);
    }

    /**
     * @param data - acceleration magnitude
     * @param avg - average of acceleration
     * @return - skewness
     */
    static double skewness(LinkedList<Double> data, double avg) {
        double numerator = momentum(data, avg, 3);
        double denominator = Math.pow(momentum(data, avg, 2), 1.5);
        return numerator/denominator;
    }

    /**
     * @param data - acceleration magnitude
     * @param avg - average of acceleration
     * @return - kurtosis
     */
    static double kurtosis(LinkedList<Double> data, double avg) {
        double numerator = momentum(data, avg, 4);
        double denominator = Math.pow(momentum(data, avg, 2), 2);
        return (numerator/denominator) - 3;
    }

    /**
     * @param data - acceleration magnitude
     * @return - kurtosis
     */
    static double kurtosis(LinkedList<Double> data) {
        double avg = average(data);
        double numerator = momentum(data, avg, 4);
        double denominator = Math.pow(momentum(data, avg, 2), 2);
        return numerator/denominator;
    }

    /**
     * @param values - acceleration magnitude
     * @param avg - average of acceleration
     * @param moment - integer of moment
     * @return - calculated moment of the signal
     */
    private static double momentum(LinkedList<Double> values, double avg, int moment){
        double sum = 0;
        for (double value: values) {
            sum += Math.pow(value-avg, moment);
        }
        return sum/((double) values.size());
    }

    /**
     * Number of crosses through specific threshold
     * @param values - magnitude: 1D array
     * @param threshold - to follow
     * @return integer
     */
    static int crossingsCounter(LinkedList<Double> values, double threshold){

        boolean crossed = false;
        int crosses = 0;

        for(double value: values){
            if(crossed && value > threshold){
                crosses++;
                crossed = false;
            }else if(!crossed && value < threshold){
                crosses++;
                crossed = true;
            }
        }
        return crosses;
    }

    /**
     * authors: ABBATE, Stefano, Marco AVVENUTI, Guglielmo COLA, Paolo CORSINI, Janet LIGHT a Alessio VECCHIO.
     * work: Recognition of false alarms in fall detection systems
     * DOI: doi:10.1109/CCNC.2011.5766464
     *
     * Average value of dip before impact
     * @param dataCarrier: acceleration data + indexes
     * @return 10 if dip does not exists, lower number otherwise
     */
    static double freeFallIndex(DataCarrier dataCarrier){
        IndexStorage indexStorage = dataCarrier.getIndexStorage();
        int beginning = indexStorage.getBeginIndex();
        int endOfFreeFall = indexStorage.getEndFree();

        if(endOfFreeFall == beginning) return 10.0;

        LinkedList<Double> values = dataCarrier.getData(DataCarrier.MAGNITUDE);
        return average(values.subList(beginning, endOfFreeFall));

    }

}
