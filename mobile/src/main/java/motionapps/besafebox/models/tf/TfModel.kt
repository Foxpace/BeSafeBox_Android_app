package motionapps.besafebox.models.tf

import motionapps.besafebox.ml.Model3100
import android.content.Context

import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

import java.nio.ByteBuffer
import java.nio.ByteOrder

class TfModel{

    private var model: Model3100? = null

    private val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, featureSize), DataType.FLOAT32)

    /** initialization of neural network - NN - this simple ! */
    fun initModel(context: Context) {
        model = Model3100.newInstance(context)
    }

    /**
     * @param features - number of features - placeholder has 5 inputs
     * @return - there are 2 outputs from 2 neurons, but only one is needed to return, the other one is 1 - returned value
     */
    fun predict(context: Context, features: ArrayList<Double>): Float {
        val inputData = ByteBuffer.allocateDirect(4 * featureSize) // input buffer with floats
        inputData.order(ByteOrder.nativeOrder())

        for (feature in features) {
            inputData.putFloat(feature.toFloat())
        }

        inputFeature0.loadBuffer(inputData)

        // Runs model inference and gets result.
        if(model == null){
            initModel(context)
        }
        model?.let {
            val outputs = it.process(inputFeature0)
            return outputs.outputFeature0AsTensorBuffer.floatArray[0]
        }

        return 1f
    }

    fun closeModel() {
        model?.close()
    }

    /**
     * singleton access to NN
     */
    companion object{
        const val featureSize = 8
        private var classifier: TfModel? = null

        @Throws(IOException::class)
        fun create(context: Context): TfModel? {
            if (classifier == null) { // existing NN is returned
                classifier = TfModel()
                classifier?.initModel(context)
            }
            return classifier
        }

        fun destroy() {
            if (classifier != null) {
                classifier!!.closeModel()
                classifier = null
            }
        }
    }


}
