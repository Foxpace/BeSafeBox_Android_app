#define NO_FALL 0
#define FALL 1
#define AFTER_HIGH_ACTIVITY 2
#define FREE_FALL 3
#define ERROR 4


#include "Classifier.h"
#include "Calculations.h"
#include "../datatypes/SensorOutput.h"
#include "../datatypes/DataCarrier.h"
#include "nn/KerasSequentialModel.h"
#include "nn/Parameters.h"

int Classifier::classify(vector<SensorOutput> *accValues) {
    dataCarrier = new DataCarrier(accValues, true); // creation of DataCarrier

    if(Calculations::getActivity(dataCarrier) < activityLevel){ // calculation of activity
        if(!(Calculations::isFreeFall(dataCarrier))){ // safe-guard for the fall of the phone
            if(Calculations::getMainVector(dataCarrier)){ // getting main event
                return useNN(); // application of neural network
            }else{
                return ERROR;
            }
        }else{
            return FREE_FALL;
        }
    }else{
        return AFTER_HIGH_ACTIVITY;
    }
}

Classifier::Classifier(float activityLevel, float nnLimit) {
    this->activityLevel = activityLevel;
    this->nnLimit = nnLimit;
    keras = new KerasSequentialModel(); // init of neural network
};

int Classifier::useNN() {
    double avg = Parameters::average(dataCarrier); //  calculation of parameters
    double momentum2 = Parameters::momentum(dataCarrier->getAfterFreeFall(), avg, 2);

    vector<float> test {Parameters::crestFactor(dataCarrier),
                        Parameters::caNumber(dataCarrier),
                        Parameters::caCos(dataCarrier),
                        Parameters::angleDeviation(dataCarrier),
                        Parameters::minMax(dataCarrier),
                        Parameters::ratio3g(dataCarrier),
                        Parameters::kurtosis(dataCarrier, avg, momentum2),
                        Parameters::skewness(dataCarrier, avg, momentum2)};
    float result = keras->calculateNN(test)[0]*100; // neural network calculation
    //__android_log_print(ANDROID_LOG_INFO, "Result: ", "%f", result);

    delete dataCarrier; // deletion of the data

    if(result > nnLimit) return NO_FALL;
    return FALL;
}

Classifier::~Classifier(){
    delete dataCarrier;
    delete keras;
}




