package eves.de.pulse;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

//import eves.de.pulse.view.BpmView;
//import eves.de.pulse.view.PulseView;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.equalizeHist;
import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

//import eves.de.pulse.dialog.BpmDialog;
//import eves.de.pulse.dialog.ConfigDialog;

public class Startsite extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "Startsite";
    JavaCameraView javaCameraView;

    //private BpmView bpmView;
    //private PulseView pulseView;
    //private Pulse pulse;

    private Paint faceBoxPaint;
    private Paint faceBoxTextPaint;

    //private ConfigDialog configDialog;

    private boolean recording = false;
    private List<Double> recordedBpms;
    //private BpmDialog bpmDialog;
    private double recordedBpmAverage;

    //private static final String CAMERA_ID = "camera-id";
    //private static final String FPS_METER = "fps-meter";
    //private static final String FACE_DETECTION = "face-detection";
    //private static final String MAGNIFICATION = "magnification";
    //private static final String MAGNIFICATION_FACTOR = "magnification-factor";

    //private boolean initFaceDetection = true;
    //private boolean initMagnification = true;
    //private int initMagnificationFactor = 100;

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

    private File createFileFromResource(File dir, int id, String extension) {
        String name = getResources().getResourceEntryName(id) + "." + extension;
        InputStream is = getResources().openRawResource(id);
        File file = new File(dir, name);

        try {
            FileOutputStream os = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        } catch (IOException ex) {
            Log.e(TAG, "Failed to create file: " + file.getPath(), ex);
        }

        return file;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},1);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startsite);
        javaCameraView = findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        //bpmView = (BpmView) findViewById(R.id.bpm);
        //bpmView.setBackgroundColor(Color.DKGRAY);
        //bpmView.setTextColor(Color.LTGRAY);

        //pulseView = findViewById(R.id.pulse);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        //initFaceDetection = savedInstanceState.getBoolean(FACE_DETECTION, initFaceDetection);
        //initMagnification = savedInstanceState.getBoolean(MAGNIFICATION, initMagnification);
        //initMagnificationFactor = savedInstanceState.getInt(MAGNIFICATION_FACTOR, initMagnificationFactor);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // if OpenCV Manager is not installed, pulse hasn't loaded
        /*
        if (pulse != null) {
            outState.putBoolean(FACE_DETECTION, pulse.hasFaceDetection());
            outState.putBoolean(MAGNIFICATION, pulse.hasMagnification());
            outState.putInt(MAGNIFICATION_FACTOR, pulse.getMagnificationFactor());
        }
        */
    }

    @Override
    protected void onPause(){
        //bpmView.setNoBpm();
        //pulseView.setNoPulse();
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

    Mat mRgba;
    Mat mIntermediateMat;
    Mat oldFrame;

    CascadeClassifier haarcascade_frontalface = new CascadeClassifier();
    private int absoluteFaceSize = 0;

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width,CvType.CV_8UC4);
        mIntermediateMat = new Mat();
        loadRessource();
    }

    private void loadRessource(){
        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(
                    R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir,
                    "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            haarcascade_frontalface.load(mCascadeFile.getAbsolutePath());
        }catch (IOException e){
            Log.i(TAG,"Failed to load ressource");
        }
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat input = inputFrame.rgba().clone();

        if(!haarcascade_frontalface.empty()) {
            detectAndDisplay(input);
        }
        return input;
    }

    private void detectAndDisplay(Mat frame)
    {
        MatOfRect faces = new MatOfRect();
        Mat grayFrame = new Mat();

        // convert the frame in gray scale
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        // equalize the frame histogram to improve the result
        Imgproc.equalizeHist(grayFrame, grayFrame);

        // compute minimum face size (20% of the frame height, in our case)
        if (this.absoluteFaceSize == 0)
        {
            int height = grayFrame.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        // detect faces
        this.haarcascade_frontalface.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

        // each rectangle in faces is a face: draw them!
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);

    }
}
