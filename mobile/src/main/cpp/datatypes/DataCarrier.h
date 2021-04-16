

#include <vector>
#include "SensorOutput.h"

#ifndef FALLDETECTION_DATACARRIER_H
#define FALLDETECTION_DATACARRIER_H

using namespace std;

/**
 *
 */

class DataCarrier {
public:
    DataCarrier(vector<SensorOutput> *data, bool tenSeconds);
    void limit10Seconds();

    // getters for stored data
    vector<SensorOutput> * getSensorOutputs();
    vector<long> * getNormalizedTime();
    vector<float> * getMagnitude();

    // INDEX
    void setMaxValue(float maxValue);
    void setMinValue(float minValue);
    void setMaxValueIndex(unsigned int indexMaxInput);
    void setMaxValueTime(long time_max);
    void setBeginIndex(unsigned int i); // beginning of the event
    void setFreeFall(bool b); // if the free fall is present
    void setEndFree(unsigned int i); // end of the free fall
    void setEndIndex(unsigned int i); // ending of the event

    float *getMaxValue();
    float *getMinValue();

    int *getMaxValueIndex();
    long *getMaxValueTime();
    unsigned int *getBeginIndex();
    unsigned int *getEndIndex();
    unsigned int *getEndFree();


    //other vectors for purpose of the classification
    void addInterestingMagnitude(const vector<float> &vector);
    void addInteresting(const vector<SensorOutput> &vector);
    void addAfterFreeFall(const vector<float> &vector);

    vector<float> *getInterestingMagnitude();
    vector<SensorOutput> *getInteresting();
    vector<float> *getAfterFreeFall();



private:


    vector<SensorOutput> *accValues; // sensor samples
    vector<long> normalizedTime; // time from 0 s
    vector<float> magnitude; // magnitude of the samples
    vector<float> interestingMagnitude; // from free fall
    vector<SensorOutput> interesting; // sensor samples from free fall
    vector<float> afterFreeFall;

    //indexes and values
    long timeMax{};
    float max{}, min{};
    int indexMax{};
    unsigned int endingIndex{}, endIndex{};
    bool freeFall{};
    unsigned int endFreeFall{};


};


#endif //FALLDETECTION_DATACARRIER_H
