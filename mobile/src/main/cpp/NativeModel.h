

#ifndef FALLDETECTION_NATIVEMODEL_H
#define FALLDETECTION_NATIVEMODEL_H

#include <vector>

#include "datatypes/SensorOutput.h"
#include "calculations/Classifier.h"

#define FILLING -1 // filling with the samples

#define WAITING 0 // waiting for the activity
#define THRESHOLD 29.43 // threshold for 3g

#define BUFFER 1 // waiting to accumulate samples after first 3g value
#define BUFFER_TIME 750L // interval to wait

#define HIGH_ACTIVITY 2 // resets the indication
#define WAIT_TIME 10000L // interval needed


using namespace std;

class NativeModel {

public:
    NativeModel(int tempLimit, float activityLevel, float nnLimit);
    ~NativeModel();
    int classify(vector<SensorOutput> *sensorOutPuts); // classification of the events
    int onValuesChanged(long time, float x, float y, float z); // new value passing
    int analyseTest(vector<SensorOutput> *s); // forced to test



private:
    int state = FILLING; // init state
    int counter = 0; // counter for the filling of the arrays
    long timeBuffer = 0L;
    vector<SensorOutput> *data; // carries data

    Classifier *classifier; // object with neural network

    void saveValues(const SensorOutput &s); // saves sample


};


#endif //FALLDETECTION_NATIVEMODEL_H
