

#ifndef FALLDETECTION_SENSOROUTPUT_H
#define FALLDETECTION_SENSOROUTPUT_H

/**
 * main object, which holds all the data from acceleration sample
 */

#include "vector"

using namespace std;

class SensorOutput {
public:
    SensorOutput(long time, float x, float y, float z);
    ~SensorOutput();
    float * getX();
    float * getY();
    float * getZ();
    long * getTime();
    vector<float> getVector();


private:
    float x, y, z;
    long time;
};


#endif //FALLDETECTION_SENSOROUTPUT_H
