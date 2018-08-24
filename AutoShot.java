package cjh.faceexpression;

//实现自动拍照

import android.os.Handler;
import android.util.Log;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

public class AutoShot {
    //handler
    //mcurrenttime
    //count:5
    //
    private static final String TAG = "AutoShot";
    private int mCurrentTimer;

    CameraView cameraView;

    private Handler handler = new Handler();

    public AutoShot(CameraView cameraView, int mCurrentTimer) {
        this.cameraView = cameraView;
        this.mCurrentTimer = mCurrentTimer;
        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

    }

    ;

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCurrentTimer > 0) {
                Log.i(TAG, "run: ");
                mCurrentTimer--;
                cameraView.captureImage();

                handler.postDelayed(timerRunnable, 1000);
            } else {

            }
        }
    };
}
