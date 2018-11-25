package eves.de.pulse;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class Startsite extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "Startsite";
    private JavaCameraView javaCameraView;
    private FaceRecognition faceRecognition;
    private HeartbeatChecker heartbeatChecker;
    private int totalFrameCount = 0;
    private static int fps = 0;

    private Mat mRgba;

    private static float BPM_RATE;

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    loaderCallbackSuccess();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    static {
        if(OpenCVLoader.initDebug()){

            Log.i(TAG,"OpenCV loaded successfully");
        } else {
            Log.i(TAG,"OpenCV not loaded");
        }
    }

    private void loaderCallbackSuccess() {
        javaCameraView.enableView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        faceRecognition = new FaceRecognition(this);
        heartbeatChecker = new HeartbeatChecker();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},1);
        }

        // Create the Handler object (on the main thread by default)
        final Handler handler = new Handler();
        // Define the code block to be executed
        final Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                fps = totalFrameCount;
                TextView fpsTextView = findViewById(R.id.activity_text_show_fps);
                fpsTextView.setText("FPS: " + fps);

                TextView bpmTextView = findViewById(R.id.activity_text_show_bpm);
                bpmTextView.setText("BPM: " + Math.round(BPM_RATE));

                totalFrameCount = 0;
                handler.postDelayed(this,1000);
            }
        };
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startsite);
        javaCameraView = findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCameraIndex(1);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause(){
        if(javaCameraView != null) {
            javaCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        if (javaCameraView != null){
            javaCameraView.disableView();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.i(TAG,"OpenCV loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG,"OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallBack);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width,CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        totalFrameCount++;
        Mat input = inputFrame.rgba().clone();

        try {
            detectAndDisplay(input);
        }catch (org.opencv.core.CvException unknown){
            Log.i(TAG,"Cant detect anything");
        }

        return input;
    }

    /**
     * Detect faces and draw them to the frame.
     * @param frame
     * The frame to scan.
     */
    private void detectAndDisplay(Mat frame)
    {
        Rect[] facesArray = faceRecognition.getFaces(frame);
        for (Rect face : facesArray) {
            Imgproc.rectangle(frame, face.tl(), face.br(), new Scalar(0, 255, 0), 3);
        }
        if(facesArray.length >= 1) {
            BPM_RATE = heartbeatChecker.getHeartRate(facesArray[0],frame);
        }
    }

    static public int getFPS(){
        return fps;
    }
}
