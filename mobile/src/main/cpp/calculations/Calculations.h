
#ifndef FALLDETECTION_CALCULATIONS_H
#define FALLDETECTION_CALCULATIONS_H


#include "../datatypes/SensorOutput.h"
#include "../datatypes/DataCarrier.h"
#include <vector>
#include <limits>

using namespace std;

class Calculations {
public:
    static float average(vector<float> *values);

    static float calculateMagnitude(SensorOutput *sensorOutput);
    static vector<float> calculateVectorMagnitude(vector<SensorOutput> *sensorOutput);
    static vector<long> subtractBeginning(vector<SensorOutput> *sensorOutputs);

    static void getMaxPeak(DataCarrier *dataCarrier);
    static float getActivity(DataCarrier *dataCarrier);
    static bool isFreeFall(DataCarrier *dataCarrier);
    static bool getMainVector(DataCarrier *dataCarrier);

    static float minValue(std::vector<float>& vectorToSearch);
    static float maxValue(std::vector<float>& vectorToSearch);
    static int maxValue(std::vector<int>& vectorToSearch);
    static int maxValueIndex(std::vector<float>& vectorToSearch);
    static unsigned int findValue(std::vector<float>& vectorName, float value);

};


#endif //FALLDETECTION_CALCULATIONS_H
