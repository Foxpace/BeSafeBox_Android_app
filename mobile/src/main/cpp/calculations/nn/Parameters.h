#ifndef FALLDETECTION_PARAMETERS_H
#define FALLDETECTION_PARAMETERS_H


#include "../../datatypes/DataCarrier.h"

class Parameters {
public:

    // main parameters
    static float crestFactor(DataCarrier *dataCarrier);
    static float caNumber(DataCarrier *dataCarrier);
    static float caCos(DataCarrier *dataCarrier);
    static float angleDeviation(DataCarrier *dataCarrier);
    static float minMax(DataCarrier *dataCarrier);
    static float ratio3g(DataCarrier *dataCarrier);
    static float kurtosis(DataCarrier *dataCarrier, double avg, double momentum2);
    static float skewness(DataCarrier *dataCarrier, double avg, double momentum2);

    // helper parameters
    static double average(DataCarrier *dataCarrier);
    static double momentum(vector<float> *values, double avg, int moment);

private:
    static vector<double> averageVector(vector<SensorOutput> *sensorOutputs, unsigned int start,
                                       unsigned int end);
    static double magnitude3DD(const vector<double> &sample);
    static double magnitude3D(const vector<float> &sample);
    static double dot1D(vector<float> v1, vector<float> v2);
    static double dot1DD(vector<double> v1, vector<double> v2);
    static double toDegrees(double r);

    static float checkBounds(double value, double bottom, double top);
};


#endif //FALLDETECTION_PARAMETERS_H
