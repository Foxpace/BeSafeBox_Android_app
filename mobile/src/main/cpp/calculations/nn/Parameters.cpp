
#include "Parameters.h"
#include "cmath"

using namespace std;

/**
 * Calculation of all parameters for trained neural network - all are normalised to 0 - 1
 * */

/**
 * Peak value / RMS - root mean square - comparison of peak value to RMS
 * resource:  https://tmi.yokogawa.com/library/resources/training-modules/power-meter-tutorials-background/
 * @param dataCarrier - all data and indexes together
 * @return float with crest factor
 */
float Parameters::crestFactor(DataCarrier *dataCarrier) {
    double pow2 = 0;
    for(float v: *(dataCarrier->getInterestingMagnitude())){
        pow2 += pow(v, 2);
    }

    return checkBounds((0.5 * (*(dataCarrier->getMaxValue()) - *(dataCarrier->getMinValue())))/sqrt(pow2/dataCarrier->getInterestingMagnitude()->size())
            , -0.2243184931948999, 2.342395696320005);
}

/**

:return: change of angle - float
*/

/**
 * authors: SANTOYO-RAMÓN, José Antonio, Eduardo CASILARI and José Manue CANO-GARCÍA
 * work: Analysis of a smartphone-based architecture with multiple mobility sensors for fall detection with supervised learning.
 * DOI: doi:10.3390/s18041155
 * Change in angle from X and Z axis of the acceleration - simplified way how to describe change in axis
 * @param dataCarrier - all data and indexes together
 * @return change in angle - number version
 */
float Parameters::caNumber(DataCarrier *dataCarrier) {
    double sum = 0;
    for(SensorOutput sensorOutput: *(dataCarrier->getSensorOutputs())){
        sum += sqrt(
                pow(*(sensorOutput.getX()), 2)+
                pow(*(sensorOutput.getZ()), 2)
                );
    }
    return checkBounds(sum / dataCarrier -> getSensorOutputs() -> size(),
            -1.5669456462903149, 10.370303761997937);
}


/**
 * authors: FIGUEIREDO, Isabel N., Carlos LEAL, Luís PINTO, Jason BOLITO a André LEMOS.
 * work: Exploring smartphone sensors for fall detection
 * DOI: doi:10.1186/s13678-016-0004-1
 * similar to change in angle, but it is taken from 1s before fall and 1s after fall
 * @param dataCarrier - all data and indexes together
 * @return change in angle with transform of cosine
 */
float Parameters::caCos(DataCarrier *dataCarrier) {

    unsigned int beforeStart = 0;
    unsigned int beforeEnding = *(dataCarrier -> getBeginIndex())-1;

    unsigned int afterStart = *(dataCarrier -> getEndIndex())+1;
    unsigned int afterEnding = 0;

    vector<long> *time = dataCarrier -> getNormalizedTime();


    // getting one second before and after
    for (unsigned int i = beforeEnding; i >= 1; i--) {
        if( time -> at(beforeEnding) - time -> at(i) >= 1000L){
            beforeStart = i;
            break;
        }
    }

    for (unsigned int i = afterStart; i < time -> size(); i++) {
        if( time -> at(i) - time -> at(afterStart) >= 1000L){
            afterEnding = i;
            break;
        }
    }

    if(afterEnding == 0){
        afterEnding = (time -> size() - 1);
    }

    vector<double> aa = averageVector(dataCarrier->getSensorOutputs(), beforeStart, beforeEnding);
    vector<double> ab = averageVector(dataCarrier->getSensorOutputs(), afterStart, afterEnding);

    double acca = magnitude3DD(aa);
    double accb = magnitude3DD(ab);

    return checkBounds(toDegrees(acos(dot1DD(aa, ab)/(acca * accb))),
            -0.3272040851208985,
            169.16406031488282);
}

/**
 * calculation of average value for every axis
 * @param sensorOutputs - raw values
 * @param start - where to start in vector
 * @param end - where to end in vector
 * @return vector with 3 axis - average values for them
 */
vector<double> Parameters::averageVector(vector<SensorOutput> *sensorOutputs,
        unsigned int start, unsigned int end) {
    vector<SensorOutput>::const_iterator first = sensorOutputs->begin() + start;
    vector<SensorOutput>::const_iterator last =  sensorOutputs->begin() + end;
    vector<SensorOutput> partSensors(first, last);
    vector<double> avgs {0, 0, 0};

    for(SensorOutput s: partSensors){
        avgs[0] = avgs[0] + *(s.getX());
        avgs[1] = avgs[1] + *(s.getY());
        avgs[2] = avgs[2] + *(s.getZ());
    }

    for(double &f : avgs){
        f = f/(end-start);
    }

    return avgs;
}


/**
 *
 * @param sample - value of acceleration - 3D
 * @return magnitude of the sample
 */
double Parameters::magnitude3DD(const vector<double> &sample) {
    double total = 0;
    for(double f: sample){
        total += pow(f, 2);
    }
    return sqrt(total);
}

/**
 *
 * @param sample - value of acceleration - 3D
 * @return magnitude of the sample
 */
double Parameters::magnitude3D(const vector<float> &sample) {
    double total = 0;
    for(double f: sample){
        total += pow(f, 2);
    }
    return sqrt(total);
}

/**
 *
 * @param v1 - vector
 * @param v2 - vector
 * @return dot product of the vectors
 */
double Parameters::dot1DD(vector<double> v1, vector<double> v2) {
    double total = 0;
    for(unsigned int i = 0; i < v1.size(); i++){
        total += v1[i] * v2[i];
    }
    return total;
}

/**
 *
 * @param v1 - vector
 * @param v2 - vector
 * @return dot product of the vectors
 */
double Parameters::dot1D(vector<float> v1, vector<float> v2) {
    double total = 0;
    for(unsigned int i = 0; i < v1.size(); i++){
        total += v1[i] * v2[i];
    }
    return total;
}

/**
 * @param r - radians
 * @return degrees
 */
double Parameters::toDegrees(double r) {
    return  (r*180.0) / M_PI;
}


/**
 * authors: FIGUEIREDO, Isabel N., Carlos LEAL, Luís PINTO, Jason BOLITO a André LEMOS.
 * work: Exploring smartphone sensors for fall detection
 * DOI: doi:10.1186/s13678-016-0004-1
 * Angle deviation - generalization of change in angle, which takes into account all axes
 * @param dataCarrier - all data and indexes together
 * @return angle deviation value - float
 */
float Parameters::angleDeviation(DataCarrier *dataCarrier) {
    double ad = 0;
    double temp;
    unsigned int pass = 0;
    for(unsigned int i = 1; i < dataCarrier -> getSensorOutputs() -> size(); i++){

        temp =  toDegrees(acos(dot1D(
                dataCarrier -> getSensorOutputs() -> at(i-1).getVector(),
                dataCarrier -> getSensorOutputs() -> at(i).getVector())/
                        (magnitude3D(dataCarrier -> getSensorOutputs() -> at(i-1).getVector())*
                        magnitude3D(dataCarrier -> getSensorOutputs() -> at(i).getVector()))));
        if(isnan(temp)){
            pass++;
        }else{
            ad += temp;
        }
    }

    return checkBounds(ad/((dataCarrier -> getSensorOutputs() -> size()) - pass),
                       -0.41380159690396107, 3.8362189666966633);
}


/**
 *
 * @param dataCarrier - all data and indexes together
 * @return difference between min and max
 */
float Parameters::minMax(DataCarrier *dataCarrier) {
    return checkBounds(*(dataCarrier->getMaxValue())-*(dataCarrier->getMinValue()), -6.419273195905566, 87.51390165162407);
}

/**
 * ratio of number of values above 3g to values below 3g
 * @param dataCarrier - all data and indexes together
 * @return float with ratio
 */
float Parameters::ratio3g(DataCarrier *dataCarrier) {
    float plus = 0;
    float minus = 0;

    for(float f : *(dataCarrier -> getAfterFreeFall())){
        if(f > 30){
            plus++;
        }else{
            minus++;
        }
    }
    return checkBounds(plus/minus, 0, 0.4146341463414634);
}

/**
 * average of the after free fall
 * @param dataCarrier - all data and indexes together
 * @return float
 */
double Parameters::average(DataCarrier *dataCarrier) {
    double sum = 0;
    for(float f : *(dataCarrier -> getAfterFreeFall())){
        sum += f;
    }

    return (sum / dataCarrier -> getAfterFreeFall() -> size());
}


/**
 * Moment calculation for skewness and kurtosis
 * resources:
 * https://mathworld.wolfram.com/StandardizedMoment.html
 * https://en.wikipedia.org/wiki/Standardized_moment
 * @param values - 1D array of acceleration
 * @param avg - to optimize calculation
 * @param moment - degree of moment 2,3,4,...
 * @return
 */
double Parameters::momentum(vector<float> *values, double avg, int moment) {
    double sum = 0;
    for (float value: *values) {
        sum += pow(value-avg, moment);
    }

    return sum / values -> size();
}


/**
 * Kurtosis of the magnitude - 4. standardized moment
 * resource:
 * https://mathworld.wolfram.com/StandardizedMoment.html
 * https://en.wikipedia.org/wiki/Standardized_moment
 * @param dataCarrier - all data and indexes together
 * @param avg - to optimize calculation
 * @param momentum2 - second momentum calculated
 * @return float of kurtosis
 */
float Parameters::kurtosis(DataCarrier *dataCarrier, double avg, double momentum2) {
    double m = momentum(dataCarrier -> getAfterFreeFall(), avg, 4) / pow(momentum2, 2) - 3;
    return checkBounds(m+1.8505589310020305, 0, 9.227833565801843);
}


/**
 * Skewness of the magnitude - 3. standardized moment
 * resource:
 * https://mathworld.wolfram.com/StandardizedMoment.html
 * https://en.wikipedia.org/wiki/Standardized_moment
 * @param dataCarrier - all data and indexes together
 * @param avg - to optimize calculation
 * @param momentum2 - second momentum calculated
 * @return
 */
float Parameters::skewness(DataCarrier *dataCarrier, double avg, double momentum2) {
    double m = momentum(dataCarrier -> getAfterFreeFall(), avg, 3) / pow(momentum2, 1.5);
    return checkBounds(m+1.608315282767581, 0, 5.241063808363597);
}

/**
 * if the value is above 1 or below 0, it will be put to border
 * @param value - to check
 * @param bottom - border
 * @param top - border
 * @return normalised value between 0 - 1
 */
float Parameters::checkBounds(double value, double bottom, double top) {
    value = (value + bottom)/(top + bottom);
    if(value > 1){
        return 1;
    }else if(value < 0){
        return 0;
    }
    return static_cast<float> (value);
}

