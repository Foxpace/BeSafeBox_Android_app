
#include "DataCarrier.h"
#include "../calculations/Calculations.h"

using namespace std;

/**
 * carries all the data and indexes for classification
 * all getters and setters
 * */

// init with vector of the data
DataCarrier::DataCarrier(vector<SensorOutput> *data, bool tenSeconds) {
    this->accValues = data;
    if(tenSeconds){
        limit10Seconds(); // pick of last 10 seconds
    }
    this->normalizedTime = Calculations::subtractBeginning(accValues); // normalised time - 0 - 10 000 ms
    this->magnitude = Calculations::calculateVectorMagnitude(accValues); // calculation of the magnitude
}

/**
 * limits vector to 0 - 10 s
 */
void DataCarrier::limit10Seconds() {

    unsigned int indexBegin = 0;
    unsigned int indexEnd = accValues->size() - 1;

    for(unsigned long i = accValues->size() - 1; i > 0; i--){ // searching for the last 10th second
        if( (*(accValues->at(indexEnd).getTime()) - *(accValues->at(i).getTime())) >= 10000L ||
            *(accValues->at(indexEnd).getTime()) == 0){
            indexBegin = i;
            break;
        }
    }

    vector<SensorOutput>::const_iterator b = accValues->begin() + indexBegin;
    vector<SensorOutput>::const_iterator e =  accValues->begin() + indexEnd - 1;
    accValues = new vector<SensorOutput>(b, e);


}

vector<SensorOutput> *DataCarrier::getSensorOutputs() {
    return accValues; // raw data
}

vector<long> *DataCarrier::getNormalizedTime() {
    return &normalizedTime; // normalised time  0 - 10
}

vector<float> *DataCarrier::getMagnitude() {
    return &magnitude; // magnitude of the acceleration
}

void DataCarrier::setMaxValue(float maxValue) {
    this->max = maxValue; // maxValue value of the signal
}

void DataCarrier::setMaxValueIndex(unsigned int indexMaxInput) {
    this->indexMax = indexMaxInput; // index of max value
}

void DataCarrier::setMaxValueTime(long time_max) {
    this->timeMax = time_max; // time of the max value
}

void DataCarrier::setBeginIndex(unsigned int i) {
    this->endingIndex = i; // beginning of the index
}

unsigned int *DataCarrier::getBeginIndex() {
    return &this->endingIndex;
}

unsigned int *DataCarrier::getEndIndex() {
    return &this->endIndex; // last index
}

void DataCarrier::setEndIndex(unsigned int i) {
    this->endIndex = i;
}

float * DataCarrier::getMaxValue() {
    return & this->max;
}

int * DataCarrier::getMaxValueIndex() {
    return & this->indexMax;
}

long * DataCarrier::getMaxValueTime() {
    return & this->timeMax;
}

void DataCarrier::setFreeFall(bool b) {
    this->freeFall = b; // presence of free fall
}

void DataCarrier::setEndFree(unsigned int i) {
    this->endFreeFall = i; // last index of the event
}

unsigned int *DataCarrier::getEndFree() {
    return &endFreeFall;
}

void DataCarrier::addInterestingMagnitude(const vector<float> &vector) {
    this->interestingMagnitude = vector; // magnitude of the fall
}

void DataCarrier::addInteresting(const vector<SensorOutput> &vector) {
    this->interesting = vector; // samples of the fall
}

void DataCarrier::addAfterFreeFall(const vector<float> &vector) {
    this->afterFreeFall = vector; // part after the free fall
}

vector<float> *DataCarrier::getInterestingMagnitude() {
    return &this->interestingMagnitude; //
}

vector<SensorOutput> *DataCarrier::getInteresting() {
    return &this->interesting;
}

vector<float> *DataCarrier::getAfterFreeFall() {
    return &this->afterFreeFall;
}

void DataCarrier::setMinValue(float minValue) {
    this->min = minValue;
}

float *DataCarrier::getMinValue() {
    return &this->min;
}








