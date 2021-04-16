



#ifndef FALLDETECTION_KERASSEQUENTIALMODEL_H
#define FALLDETECTION_KERASSEQUENTIALMODEL_H

#include <vector>

using namespace std;

/**
 * class for dense layer initialization
 */
class Layer{
public:
    /**
     * Dense layer
     * @param weights - 2D vector to describe all the neurons
     * @param bias - for every neuron
     * @param activation - activation of the summed value
     */
    Layer(vector<vector<float>> *weights, vector <float> *bias, unsigned activation) {
        this->weights = weights;
        this->bias = bias;
        this->activation = activation;
    }

    /**
     * calculation of the neural network
     * @param input - first or intermediate input for the layer
     * @return vector of floats from the neurons
     */
    vector<float> calculateOutput(vector<float> *input); // prepocet danej vrstvy

private :
    vector<vector<float>> *weights;
    vector <float> *bias;
    unsigned int activation;

    /**
     * application of the acitvation function - ReLU, SIGMOID, ...
     * @param value - vector on which to apply activation
     * @param function - which type of function
     * @return transformed outputs of the layer
     */
    static vector<float> useActivationFunction(vector<float> *value, unsigned int function); // vyuzitie aktivacnej funkcie

};

/**
 * aggregates all the layers into one model - Sequential
 */
class KerasSequentialModel {
public:
    KerasSequentialModel();
    ~KerasSequentialModel();

    /**
     * iterates through all the layers
     * @param input - input of the neural network
     * @return vector as output
     */
    vector<float> calculateNN(vector<float> input);
private:
    vector<Layer*> layers;

};


#endif //FALLDETECTION_KERASSEQUENTIALMODEL_H
