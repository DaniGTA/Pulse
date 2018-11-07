package eves.de.pulse;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.camera2.params.Face;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraRenderer;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

//import eves.de.pulse.dialog.BpmDialog;
//import eves.de.pulse.dialog.ConfigDialog;
import eves.de.pulse.view.BpmView;
import eves.de.pulse.view.PulseView;

import static org.opencv.core.CvType.CV_8UC3;

public class Startsite extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "Startsite";
    JavaCameraView javaCameraView;

    private BpmView bpmView;
    private PulseView pulseView;
    private Pulse pulse;

    private Paint faceBoxPaint;
    private Paint faceBoxTextPaint;

    //private ConfigDialog configDialog;

    private boolean recording = false;
    private List<Double> recordedBpms;
    //private BpmDialog bpmDialog;
    private double recordedBpmAverage;

    private static final String CAMERA_ID = "camera-id";
    private static final String FPS_METER = "fps-meter";
    private static final String FACE_DETECTION = "face-detection";
    private static final String MAGNIFICATION = "magnification";
    private static final String MAGNIFICATION_FACTOR = "magnification-factor";

    private boolean initFaceDetection = true;
    private boolean initMagnification = true;
    private int initMagnificationFactor = 100;

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
        System.loadLibrary("pulse");

        pulse = new Pulse();
        pulse.setFaceDetection(initFaceDetection);
        pulse.setMagnification(initMagnification);
        pulse.setMagnificationFactor(initMagnificationFactor);

        File dir = getDir("cascade", Context.MODE_PRIVATE);


        File file = createFileFromResource(dir, R.raw.lbpcascade_frontalface, "xml");
        pulse.load(file.getAbsolutePath());
        dir.delete();

        pulseView.setGridSize(pulse.getMaxSignalSize());

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

        bpmView = (BpmView) findViewById(R.id.bpm);
        bpmView.setBackgroundColor(Color.DKGRAY);
        bpmView.setTextColor(Color.LTGRAY);

        pulseView = findViewById(R.id.pulse);

        faceBoxPaint = initFaceBoxPaint();
        faceBoxTextPaint = initFaceBoxTextPaint();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        initFaceDetection = savedInstanceState.getBoolean(FACE_DETECTION, initFaceDetection);
        initMagnification = savedInstanceState.getBoolean(MAGNIFICATION, initMagnification);
        initMagnificationFactor = savedInstanceState.getInt(MAGNIFICATION_FACTOR, initMagnificationFactor);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // if OpenCV Manager is not installed, pulse hasn't loaded
        if (pulse != null) {
            outState.putBoolean(FACE_DETECTION, pulse.hasFaceDetection());
            outState.putBoolean(MAGNIFICATION, pulse.hasMagnification());
            outState.putInt(MAGNIFICATION_FACTOR, pulse.getMagnificationFactor());
        }
    }

    @Override
    protected void onPause(){
        bpmView.setNoBpm();
        pulseView.setNoPulse();
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

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width,CvType.CV_8UC4);
        mIntermediateMat = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        pulse.onFrame(inputFrame.rgba());
        return inputFrame.rgba();
    }

    private Paint initFaceBoxPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setShadowLayer(2, 0, 0, Color.BLACK);
        return p;
    }

    private Paint initFaceBoxTextPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setShadowLayer(2, 0, 0, Color.DKGRAY);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(20f);
        return p;
    }

    private Path createFaceBoxPath(Rect box) {
        float size = box.width * 0.25f;
        Path path = new Path();
        // top left
        path.moveTo(box.x, box.y + size);
        path.lineTo(box.x, box.y);
        path.lineTo(box.x + size, box.y);
        // top right
        path.moveTo(box.x + box.width, box.y + size);
        path.lineTo(box.x + box.width, box.y);
        path.lineTo(box.x + box.width - size, box.y);
        // bottom left
        path.moveTo(box.x, box.y + box.height - size);
        path.lineTo(box.x, box.y + box.height);
        path.lineTo(box.x + size, box.y + box.height);
        // bottom right
        path.moveTo(box.x + box.width, box.y + box.height - size);
        path.lineTo(box.x + box.width, box.y + box.height);
        path.lineTo(box.x + box.width - size, box.y + box.height);
        return path;
    }
}
