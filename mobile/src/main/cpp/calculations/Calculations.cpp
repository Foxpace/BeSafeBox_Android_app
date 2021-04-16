#include "Calculations.h"
#include <cmath>
#include <vector>

#define ZERO 0
#define FOURS_SECONDS 4000L // ms
#define SIX_SECONDS 6000L // ms
#define THRESHOLD 30 // m/s2
#define TOP_BORDER 1500 // ms
#define THRESHOLD_LAST_ACTIVITY 15 // m/s2
#define GRAVITATION_PULL 9.81 // m/s2
#define UPPER_LIMIT_FREE_FALL 750 // ms
#define LOWER_LIMIT_FREE_FALL 50 // ms
#define MINIMUM_TIME_FREE_FALL 20 //ms
#define INTERVAL_BEGINNING_TO_PEAK 300 //ms
#define DECCELARATION 9 //m/s2
#define DECCELARATION_LIMIT 9.25 //m/s2
#define DECCELARATION_TOLERANCE 5

using namespace std;

float Calculations::average(vector<float> *values) {
    float sum = 0;
    for(float f : (*values)){
        sum += f;
    }
    return sum/(*values).size();
}

float Calculations::calculateMagnitude(SensorOutput *sensorOutput) {
    return (float)
    (sqrt(
            pow(*(*sensorOutput).getX(), 2)
    + pow(*(*sensorOutput).getY(), 2)
    + pow(*(*sensorOutput).getZ(), 2)));
}

/**
 * normalisation of time - 0 - 10000 ms
 */

vector<long> Calculations::subtractBeginning(vector<SensorOutput> *sensorOutputs) {
    long beginning = *(sensorOutputs->at(0).getTime());
    vector<long> subTime((*sensorOutputs).size());

    for (unsigned int i = 0; i < (*sensorOutputs).size(); ++i) {
        subTime[i] = *(sensorOutputs->at(i).getTime()) - beginning;
    }
    return subTime;
}

/**
 * calculation of magnitude for whole vector
 */
vector<float> Calculations::calculateVectorMagnitude(vector<SensorOutput> *sensorOutput) {

    vector<float> magnitude((*sensorOutput).size());
    int counter = 0;
    for(SensorOutput s: *sensorOutput){
        magnitude[counter++] = calculateMagnitude(&s);
    }
    return magnitude;
}

/**
 * sets max value with index, value and time at the same time
 * @param dataCarrier - all indexes for classification
 */
void Calculations::getMaxPeak(DataCarrier *dataCarrier) {
    unsigned int fourth_second = 0;
    unsigned int sixth_second = 0; // bordering classification

    vector<long> *time = dataCarrier -> getNormalizedTime();

    for (unsigned int i = 0; i < (*time).size(); i++) {
        if((*time)[i] >= FOURS_SECONDS && fourth_second == 0){
            fourth_second = i; // search for peak between 4 - 6s
        }
        else if((*time)[i] >= SIX_SECONDS){
            sixth_second = i;
            break;
        }
    }

    float max = 0;
    unsigned int index = 0;
    long time_max = 0; // search for max value

    vector<float> *magnitude = (*dataCarrier).getMagnitude();
    for (unsigned int i = fourth_second; i < sixth_second; i++) {
        if(max < (*magnitude)[i]){ // search of max value in window
            max = (*magnitude)[i];
            index = i;
            time_max = (*time)[i];
        }
    }

    if(max < THRESHOLD) { // search of peak in whole signal
        max = maxValue(*magnitude);
        index = findValue(*magnitude, max);
        time_max = (*time)[index];
    }

    (*dataCarrier).setMaxValue(max); // saving values
    (*dataCarrier).setMaxValueIndex((index));
    (*dataCarrier).setMaxValueIndex((index));
    (*dataCarrier).setMaxValueTime(time_max);
}

/**
 * calculation of average activity after the event
 * @param dataCarrier - all indexes for classification
 * @return - float with average acceleration after the event
 */
float Calculations::getActivity(DataCarrier *dataCarrier) {

    getMaxPeak(dataCarrier);

    vector<float> *magnitude = (*dataCarrier).getMagnitude();
    vector<long> *time = (*dataCarrier).getNormalizedTime();
    int topBorder = (int) (*magnitude).size()-1; //search for upper value of 1.5g after peak

    // searching
    for(int i = *dataCarrier->getMaxValueIndex(); i < (int) (*magnitude).size(); i++) {
        if ((*time)[i] - (*time)[*(*dataCarrier).getMaxValueIndex()] > TOP_BORDER) { //1.5s
            break;
        }else if((*magnitude)[i] > THRESHOLD_LAST_ACTIVITY){
            topBorder = i;
            dataCarrier->setEndIndex(i);
        }
    }

    // error occurred
    if(topBorder == (*magnitude).size()-1){
        return GRAVITATION_PULL;
    }

    // getting subvector from the end of the signal
    vector<float>::const_iterator first = (*magnitude).begin() + topBorder;
    vector<float>::const_iterator last  = (*magnitude).begin() + (*magnitude).size() - 1;
    vector<float> subVector(first, last);

    return Calculations::average(&subVector);
}

/**
 * safeguard for free fall of the phone on the ground - acceleration close to 0
 * @param dataCarrier - all indexes for classification
 * @return boolean if it happend
 */
bool Calculations::isFreeFall(DataCarrier *dataCarrier) {

    vector<unsigned int> indexes;

    // indexes from 750 ms before main peak
    for(unsigned int i = *(*dataCarrier).getMaxValueIndex(); i > 1; i--){
        if((*(dataCarrier -> getMaxValueTime()) - dataCarrier->getNormalizedTime()->at(i)) > UPPER_LIMIT_FREE_FALL){
            break;
        }
        if((dataCarrier -> getMagnitude() ->at(i) < 2 && ((*dataCarrier -> getMaxValueTime()) - dataCarrier -> getNormalizedTime()->at(i)) > LOWER_LIMIT_FREE_FALL)) {
            indexes.push_back(i);
        }
    }

    if(indexes.empty()){
        return false;
    }

    // searching for the intervals longer than 50 ms
    vector<int> times;
    int longestFluent = 0;
    for(unsigned int k = 1; k < indexes.size(); k++){
        if(indexes[k] - indexes[k-1] < MINIMUM_TIME_FREE_FALL){ // tolerance for the samples
            longestFluent += dataCarrier -> getNormalizedTime()->at((indexes[k])) -
                    (dataCarrier -> getNormalizedTime()-> at(indexes[k-1]));
        }else{
            // storing intervals
            if(longestFluent != 0L){
                times.push_back(longestFluent);
                longestFluent = 0L;
            }
        }
    }
    times.push_back(longestFluent);

    // getting the longest interval -> if it is longer than 50ms, it is free fall
    long max = maxValue(times);
    return max < LOWER_LIMIT_FREE_FALL;
}

/**
 * main function to extract main event from the signal to classify
 * @param dataCarrier - all indexes for classification
 * @return boolean if everything is ok
 */
bool Calculations::getMainVector(DataCarrier *dataCarrier) {
    dataCarrier->setBeginIndex(0);

    if(dataCarrier->getMagnitude()->size() - *(dataCarrier->getMaxValueIndex()) <= 0){
        return false;
    }

    for (unsigned int i = *(dataCarrier->getMaxValueIndex()); i >= 0; i--){
        // searching for the magnitude lower than 9 m/s2 -> searching for free fall of the person -> if there is none, 0.3s from the peak is beginning
        if(*(dataCarrier->getMaxValueIndex()) - dataCarrier->getNormalizedTime()->at(i) > INTERVAL_BEGINNING_TO_PEAK){
            dataCarrier->setBeginIndex(i);
            break;
        }
        //first sample under 9 m/s2
        if(dataCarrier->getMagnitude()->at(i) <= DECCELARATION){
            // searching for the free fall
            double value = dataCarrier->getMagnitude()->at(i);

            unsigned int index = i;
            unsigned int counter = 0;

            dataCarrier->setFreeFall(true); // changing flag
            dataCarrier->setEndFree(i);

            // searching for all samples
            while(true){
                if(index-1 == 0){
                    dataCarrier->setBeginIndex(0);
                    break;
                }
                // locally, there can be interferences due to interaction with pocket / body
                // omitting 5 samples higher than  9.25 m/2
                if(value > DECCELARATION_LIMIT){
                    ++counter;
                    if(counter >= DECCELARATION_TOLERANCE){ // stop in search if there are more samples higher than 9.25 m/s2
                        for(int k = 0; k < DECCELARATION_TOLERANCE; k++){
                            // deletion
                            dataCarrier->setBeginIndex(index);
                            if(dataCarrier->getMagnitude()->at(index+k) < 20){
                                dataCarrier->setBeginIndex(index+k);
                                break;
                            }
                        }
                        break;

                    }
                    // next sample for search
                    value = dataCarrier->getMagnitude()->at(index--);
                    continue;
                }else{
                    counter = 0; // counter is restarted after every sample below 9.0 m/s2
                }
                // 0.3s is maximum distance
                if((*dataCarrier->getMaxValueTime()) - dataCarrier->getNormalizedTime()->at(index) > INTERVAL_BEGINNING_TO_PEAK){ // 0.3s
                    dataCarrier->setBeginIndex(index);
                    break;
                }
                value = dataCarrier->getMagnitude()->at(index--);
            }
            break;

        }
    }

    // storing interesting parts of the signal to dataCarrier
    vector<float> interesting;
    vector<SensorOutput> sensorOutputs;
    vector<float> fromEndFreeFall;

    for (unsigned int i = *(dataCarrier->getBeginIndex()); i < *(dataCarrier->getEndIndex()); i++) {
        interesting.push_back(dataCarrier->getMagnitude()->at(i));
        sensorOutputs.push_back(dataCarrier->getSensorOutputs()->at(i));

        if(i >= *(dataCarrier->getEndFree())){
            fromEndFreeFall.push_back(dataCarrier->getMagnitude()->at(i));
        }
    }

    // saving
    dataCarrier -> addInterestingMagnitude(interesting);
    dataCarrier -> addInteresting(sensorOutputs);
    dataCarrier -> addAfterFreeFall(fromEndFreeFall);
    dataCarrier -> setMinValue(minValue(interesting));

    return dataCarrier->getBeginIndex() != ZERO;
}

/**
 * searches for the maximum value
 * @param vectorToSearch - float vector in which we want to find max value
 * @return max value
 */
float Calculations::maxValue(std::vector<float>& vectorToSearch) {
    float max = std::numeric_limits<float>::min();
    for (auto val : vectorToSearch) {
        if (max < val) max = val;
    }
    return max;
}

/**
 * searches for the maximum value
 * @param vectorToSearch - int vector in which we want to find max value
 * @return max value
 */
int Calculations::maxValue(std::vector<int>& vectorToSearch) {
    int max = INT_MIN;
    for (auto val : vectorToSearch) {
        if (max < val) max = val;
    }
    return max;
}

/**
 * searches for the min value
 * @param vectorToSearch - float vector in which we want to find min value
 * @return min value
 */
float Calculations::minValue(std::vector<float>& vectorToSearch) {
    float min = std::numeric_limits<float>::max();
    for (auto val : vectorToSearch) {
        if (min > val) min = val;
    }
    return min;
}

/**
 * searches for the index of maximum value
 * @param vectorToSearch - float vector in which we want to find index of max value
 * @return index of max value
 */
int Calculations::maxValueIndex(std::vector<float>& vectorToSearch) {
    float min = std::numeric_limits<float>::min();
    int index = 0;
    int counter = 0;
    for (auto val : vectorToSearch) {
        if (min < val) {
            min = val;
            index = counter;
        }
        counter++;
    }
    return index;
}

/**
 * searches for the index of specified value
 * @param vectorToSearch - float vector of values
 * @param value - to search for
 * @return index of the value - first occurance only
 */
unsigned int Calculations::findValue(std::vector<float> &vectorToSearch, float value) {
    unsigned int counter = 0;
    for (auto val : vectorToSearch) {
        if (val == value) {
            return counter;
        }
        counter++;
    }

    return 0;
}




