

#ifndef FALLDETECTION_CLASSIFICATION_H
#define FALLDETECTION_CLASSIFICATION_H

#include <vector>
#include "../datatypes/SensorOutput.h"
#include "../datatypes/DataCarrier.h"
#include "nn/KerasSequentialModel.h"

using namespace std;

class Classifier {

public:
    Classifier(float activityLevel, float nnLimit); // init of neural network
    ~Classifier();
    int classify(vector<SensorOutput> *accValues); // calculation of parameters
private:

    int useNN(); // usage of neural network

    KerasSequentialModel* keras; // model of neural network
    DataCarrier* dataCarrier{}; // takes data to neural network
    float activityLevel = 0; // level of activity after the event
    float nnLimit; // threshold for neural network

};


#endif //FALLDETECTION_CLASSIFICATION_H
