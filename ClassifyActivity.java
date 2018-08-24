package cjh.faceexpression;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClassifyActivity extends AppCompatActivity {
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "inputs_2";
    private static final String OUTPUT_NAME = "outputs_class_1/Softmax";
    private static final String MODEL_FILE = "file:///android_asset/tensor_model.pb";
    //  private static final String LABEL_FILE =
    //          "file:///android_asset/imagenet_comp_graph_label_strings.txt";
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView textViewResult;
    private ImageView imageViewResult;
    private CameraView cameraView;
    private ImageView switchCamera, takePicture, settings;
    private int timeInterval = 5000;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify);
        cameraView = findViewById(R.id.cameraView);
        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        textViewResult = (TextView) findViewById(R.id.textViewResult);
        switchCamera = findViewById(R.id.switch_camera_button);
        takePicture = findViewById(R.id.take_picture_button);
        settings = findViewById(R.id.settings_button);

        textViewResult.setMovementMethod(new ScrollingMovementMethod());


        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                Bitmap bitmap = cameraKitImage.getBitmap();
                FaceHelper faceHelper = new FaceHelper();
                bitmap = faceHelper.genFaceBitmap(bitmap);
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                imageViewResult.setImageBitmap(bitmap);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                textViewResult.setText(results.toString());
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cameraView.captureImage();
            }
        });
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.toggleFacing();
            }
        });
        initTensorFlowAndLoadModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    /*private void makeButtonVisible(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnTakePicture.setVisibility(View.VISIBLE);
            }
        });
    }*/

   /* public void onTakePictureButtonClicked(View view) {
        cameraView.captureImage();
    }

    public void onSwitchButtonClicked(View view) {
        cameraView.toggleFacing();
    }

    public void onSettingsButtonClicked(View view) {
    }
/*/
    //public native String stringFromJNI();
}
