

#include "KerasSequentialModel.h"
#include "cmath"
#include "TensorflowExtraction.h"

using namespace std;

/**
 * neural network calculation - individual weights and thresholds were extracted from tensorflow and implemented
 * to local computation - the neural network is small, so its calculation does not constitute such an obstacle,
 * the problem would occur if it were e.g. convolutional neural network (neural network for image / matrix classification)
 */

/**
 * calculation for Dense layer
 * @param input for the layer - vector
 * @return output of the layer - vector
 */
vector<float> Layer::calculateOutput(vector<float>* input) {

    vector<float> sums; // init of output vector
    for(unsigned int v = 0; v < weights->at(0).size(); v++){
        sums.push_back(0);
    }

    unsigned int feature = 0;
    for(float i: *input){ // iteration through all the inputs and its neurons - adding together weighted results of multiplication
        unsigned int neuronIndex = 0;
        for(float weight: weights->at(feature)){
            sums[neuronIndex] += i * weight;
            neuronIndex += 1;
        }
        feature += 1;
    }

    for(unsigned int v = 0; v < bias->size(); v++){
        sums[v] = sums[v]+bias->at(v); // application of bayes for every neuron
    }

    return useActivationFunction(&sums, activation); // application for every neuron and returning result

}

vector<float> Layer::useActivationFunction(vector<float> *values, unsigned int function) {
    switch (function){
        case RELU:
            for (float &i : *values) {
                if(i < 0) {
                    i = 0;
                }
            }
            break;
        case SIGMOID:
            float denominator;
            for (float &value : *values) {
                denominator = 1 + exp(-(value));
                value = 1/denominator;
            }
            break;
        default:break;
    }

    return *values;
}


KerasSequentialModel::KerasSequentialModel() { // initialization of the layers of the models
    layers.push_back(new Layer(&weights0, &bias0, type0)); // from TensorflowExtraction - arrays of weights and biases
    layers.push_back(new Layer(&weights1, &bias1, type1));
}
// calculation of the neural network
vector<float> KerasSequentialModel::calculateNN(vector<float> input) {
    vector<float> output = std::move(input);
    for(Layer *l: layers){ // passing output of one layer to another as input
        output = l->calculateOutput(&output);
    }
    return output;
}

// deletion of all layers
KerasSequentialModel::~KerasSequentialModel() {
    for(Layer *l: layers){
        delete l;
    }
}
