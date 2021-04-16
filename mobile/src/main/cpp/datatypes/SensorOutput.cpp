

#include "SensorOutput.h"
#include "vector"


SensorOutput::SensorOutput(long time, float x, float y, float z) {
    this->x = x;
    this->y = y;
    this->z = z;
    this->time = time;
}

float *SensorOutput::getX() {
    return &x;
}

float *SensorOutput::getY() {
    return &y;
}

float *SensorOutput::getZ() {
    return &z;
}

long *SensorOutput::getTime() {
    return &time;
}

vector<float> SensorOutput::getVector() {
    vector<float> v{x, y, z};
    return v;
}

SensorOutput::~SensorOutput() = default;


