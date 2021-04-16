#include "NativeModel.h"

#include "calculations/Calculations.h"
#include "calculations/Classifier.h"

#include "datatypes/DataCarrier.h"

#include <vector>

using namespace std;

NativeModel::NativeModel(int tempLimit, float activityLevel, float nnLimit) {
    data = new vector<SensorOutput>; // main vector of data
    for (int i = 0; i < tempLimit; ++i) { // init of the vector
        data->emplace_back(SensorOutput(0, 0.0, 0.0, 0.0));
    }
    // init of neural network
    classifier = new Classifier(activityLevel, nnLimit);
}

int NativeModel::onValuesChanged(long time, float x, float y, float z) {
    // init of sample
    SensorOutput sensorOutput = SensorOutput(time, x, y, z);

    saveValues(sensorOutput);

    // 4 states in which the detector can be
    switch (state){

        case FILLING: // filling of the main vector - classification is not working
            timeBuffer = *sensorOutput.getTime();
            if(counter++ > data->size()){
                state = WAITING;

                //__android_log_print(ANDROID_LOG_INFO, "Change of status:", "WAITING");
            }
            break;

        case WAITING: // waiting for the 3g magnitude
            if(Calculations::calculateMagnitude(&sensorOutput) > THRESHOLD){
                state = BUFFER;
                timeBuffer = *sensorOutput.getTime();
                //__android_log_print(ANDROID_LOG_INFO, "Change of status:", "BUFFER");
            }
            break;
        case BUFFER: // letting the signal going after 3g magnitude for certain amount of time
            if((*sensorOutput.getTime() - timeBuffer) > BUFFER_TIME){
                state = HIGH_ACTIVITY;
                //__android_log_print(ANDROID_LOG_INFO, "Change of status:", "HIGH_ACTIVITY");
            }
            break;
        case HIGH_ACTIVITY:
            // waiting for the next 3g magnitude to occurre -> restart
            if(Calculations::calculateMagnitude(&sensorOutput) > THRESHOLD){
                state = WAITING;
                //__android_log_print(ANDROID_LOG_INFO, "Change of status:", "WAITING");
            } else if((*sensorOutput.getTime() - timeBuffer) > WAIT_TIME){
                state = WAITING; // sensor can work after the classification
                vector<SensorOutput> copyValues = *data;
                return classify(&copyValues); // TODO add individual thread


            }else{
                //TODO other method of activity detection
            }
            break;

        default:break;
    }

    return 0;
}

// storing sample and deleting last one
void NativeModel::saveValues(const SensorOutput &sensorOutputs) {
    data->emplace_back(sensorOutputs);
    data->erase(data->begin());
}
// classification of out data
int NativeModel::classify(vector<SensorOutput> *sensorOutputs) {
    //__android_log_print(ANDROID_LOG_INFO, "Status", "classifying");
    return classifier->classify(sensorOutputs);
}

NativeModel::~NativeModel(){
    delete data;
    delete classifier;
}
// forced test of the classification with certain vectors
int NativeModel::analyseTest(vector<SensorOutput>* sensorOutputs) {
    int result = classifier -> classify(sensorOutputs);
    data->clear();
    timeBuffer = 0L;
    return result;
}
