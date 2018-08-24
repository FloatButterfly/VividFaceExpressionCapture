package cjh.faceexpression;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.os.TraceCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

public class TensorFlowImageClassifier extends AppCompatActivity implements Classifier {

    private static final String TAG = "TensorFlowImageClassifi";
    private static final int MAX_RESULTS = 1;
    private static final float THRESHOLD = 0.1f;

    private String inputName;
    private String outputName;
    private int inputSize;
    private int imageMean;
    private float imageStd;
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;
    private boolean logStats = false;

    private TensorFlowInferenceInterface inferenceInterface;

    private TensorFlowImageClassifier() {
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     *                      //@param labelFilename The filepath of label file for classes.
     * @param inputSize     The input size. A square image of inputSize x inputSize is assumed.
     * @param imageMean     The assumed mean of the image values.
     * @param imageStd      The assumed std of the image values.
     * @param inputName     The label of the image input node.
     * @param outputName    The label of the output node.
     * @throws IOException
     */
    public static Classifier create(
            AssetManager assetManager,
            String modelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName) throws IOException {
        TensorFlowImageClassifier c = new TensorFlowImageClassifier();
        Log.i(TAG, "create: new ?!");
        c.inputName = inputName;
        c.outputName = outputName;
        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);

       /* Iterator<Operation> it = c.inferenceInterface.graph().operations();
        while (it.hasNext()){
            String str=(String) it.next().name();
            System.out.println(str);
        }*/

        // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
        final Operation operation = c.inferenceInterface.graphOperation(outputName);
        final int numClasses = (int) operation.output(0).shape().size(1);
        //Log.i(TAG, "Read " + c.labels.size() + " labels, output layer size is " + numClasses);
        // Ideally, inputSize could have been retrieved from the shape of the input operation.  Also,
        // the placeholder node for input in the graphdef typically used does not specify a shape, so it
        // must be passed in as a parameter.
        c.inputSize = inputSize;
        c.imageMean = imageMean;
        c.imageStd = imageStd;

        // Pre-allocate buffers.
        c.outputNames = new String[]{outputName};
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];
        c.outputs = new float[numClasses];
        return c;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        // Trace.beginSection("recognizeImage");
        TraceCompat.beginSection("recognizeImage");

        TraceCompat.beginSection("preprocessBitmap");
        // Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        //1 预处理输入图片，读取像素点，并将RGB三通道数值归一化. 归一化后分布于 -117 ~ 138
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;//归一化通道R
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;//归一化通道G
            floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;//归一化通道B
        }
        TraceCompat.endSection();

        // Copy the input data into TensorFlow.
        TraceCompat.beginSection("feed");
        // 2 将输入数据填充到TensorFlow中，并feed数据给模型
        // inputName为输入节点
        // floatValues为输入tensor的数据源，
        // dims构成了tensor的shape, [batch_size, height, width, in_channel], 此处为[1, inputSize, inputSize, 3]
        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);
        TraceCompat.endSection();

        // Run the inference call.
        TraceCompat.beginSection("run");
        inferenceInterface.run(outputNames, logStats);
        TraceCompat.endSection();

        // Copy the output Tensor back into the output array.
        TraceCompat.beginSection("fetch");
        inferenceInterface.fetch(outputName, outputs);
        TraceCompat.endSection();

        // Find the best classifications.
        PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        1,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });
        for (int i = 0; i < outputs.length; ++i) {
            if (outputs[i] > THRESHOLD) {
                String string = null;
                switch (i) {
                    case 0:
                        string = "中性";
                        break;
                    case 1:
                        string = "愤怒";
                        break;
                    case 2:
                        string = "恶心";
                        break;
                    case 3:
                        string = "恐惧";
                        break;
                    case 4:
                        string = "开心";
                        break;
                    case 5:
                        string = "伤心";
                        break;
                    case 6:
                        string = "惊讶";
                        break;
                }
                pq.add(
                        new Recognition(i, string, outputs[i], null));
            }
        }
        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        TraceCompat.endSection(); // "recognizeImage"
        return recognitions;
    }

    @Override
    public void enableStatLogging(boolean debug) {
        this.logStats = logStats;
    }

    @Override
    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    @Override
    public void close() {
        inferenceInterface.close();
    }
}
