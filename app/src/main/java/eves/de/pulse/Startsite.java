package eves.de.pulse;

import android.Manifest;
import android.content.Context;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

//import eves.de.pulse.view.BpmView;
//import eves.de.pulse.view.PulseView;

import ddf.minim.analysis.FFT;

//import eves.de.pulse.dialog.BpmDialog;
//import eves.de.pulse.dialog.ConfigDialog;

public class Startsite extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "Startsite";
    JavaCameraView javaCameraView;
    int totalFrameCount = 0;
    int fps = 0;
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
        fft = new FFT(bufferSize, sampleRate);
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

    Mat mRgba;
    Mat mIntermediateMat;

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
        totalFrameCount++;
        Mat input = inputFrame.rgba().clone();

        if(!haarcascade_frontalface.empty()) {
            try {
                detectAndDisplay(input);
            }catch (org.opencv.core.CvException unknown){
                Log.i(TAG,"Cant detect anything");
            }
        }

        return input;
    }



    private float FORE_HEAD_DETECTION_PERCENTAGE = 40;
    private float FACE_DETECTION_PERCENTAGE = 90;
    private int startAfterFPS = 60;

    ArrayList<Float> floats = new ArrayList();
    float[] sample;
    int bufferSize = 32;
    int sampleRate = bufferSize;
    FFT fft;
    float BPM_RATE;

    private void getHeartRate(Rect rect,Mat mat){
        Point face_start_point = new Point(rect.x + (rect.width * (100 - FACE_DETECTION_PERCENTAGE) / 100), rect.y + (rect.height * (100 - FACE_DETECTION_PERCENTAGE) / 100));
        Point face_end_point = new Point((rect.x + rect.width) - (rect.width * (100 - FACE_DETECTION_PERCENTAGE) / 100), (rect.y + rect.height) - (rect.height * (100 - FACE_DETECTION_PERCENTAGE) / 100));
        Imgproc.rectangle(mat, face_start_point, face_end_point, new Scalar(0, 255, 0));
        Rect rect_face = new Rect(face_start_point, face_end_point);

        Point start_point = new Point(rect.x + (rect.width * (100 - FORE_HEAD_DETECTION_PERCENTAGE) / 100), rect.y + 50);

        Point end_point = new Point((rect.x + rect.width) - (rect.width * (100 - FORE_HEAD_DETECTION_PERCENTAGE) / 100), rect.y + 75);
        Imgproc.rectangle(mat, start_point, end_point, new Scalar(0, 0, 255)); // Drawing Blue Rectangle on forehead.
        Rect rect_fore_head = new Rect(start_point, end_point);

        Mat fore_head_mat = mat.submat(rect_fore_head);

        Mat inputMatForHR = fore_head_mat;
//                    Mat inputMatForHR = faceMat;
//                    Mat inputMatForHR = mat;


        float green_avg = 0;
        int numPixels = inputMatForHR.rows() * inputMatForHR.cols();
        for (int px_row = 0; px_row < inputMatForHR.rows(); px_row++) { // For each pixel in the video frame of forehead...
            for (int px_col = 0; px_col < inputMatForHR.cols(); px_col++) {
                double[] px_data = inputMatForHR.get(px_row, px_col);
                int c = (int) px_data[1];  //  [0][1][2] RGB or BGR .. G is always in center
//                            System.out.println("Colour Value  : " + c);
//                            float luminG = c >> 010 & 0xFF;
//                            System.out.println("LuminG value :" + luminG);
                // getting green color channel of pixel
//                            float luminRangeG = (float) (c / 255.0);
                green_avg = green_avg + c;
            }
        }

        green_avg = green_avg / numPixels;
//                    System.out.println("Green  Avg :" + green_avg);
        if (floats.size() < bufferSize) {
            floats.add(green_avg);
        } else floats.remove(0);

        sample = new float[floats.size()];
        for (int i = 0; i < floats.size(); i++) {
            float f = (float) floats.get(i);
            sample[i] = f;
        }


        if (sample.length >= bufferSize) {
            //fft.window(FFT.NONE);

            fft.forward(sample, 0);
            //    bpf = new BandPass(centerFreq, bandwidth, sampleRate);
            //    in.addEffect(bpf);


            float heartBeatFrequency = 0;
//                        System.out.println("FFT Secsize : " + fft.specSize());
            for (int i = 0; i < fft.specSize(); i++) { // draw the line for frequency band i, scaling it up a bit so we can see it
                heartBeatFrequency = Math.max(heartBeatFrequency, fft.getBand(i));
//                    System.out.println("Band value : " + fft.getBand(i));
            }

            float bw = fft.getBandWidth(); // returns the width of each frequency band in the spectrum (in Hz).
//                System.out.println("Bandwidth : " + bw); // returns 21.5332031 Hz for spectrum [0] & [512]

            heartBeatFrequency = bw * heartBeatFrequency;

            //float BPM_RATE = heartBeatFrequency / (60);
            BPM_RATE = heartBeatFrequency / (60+fps);

            // Adding Text
            Imgproc.putText(
                    mat,                          // Matrix obj of the image
                    "BPM: " + BPM_RATE,          // Text to be added
                    new Point(10, 50),               // point
                    Core.FONT_HERSHEY_SIMPLEX,      // front face
                    2,                               // front scale
                    new Scalar(255, 255, 255),             // Scalar object for color
                    1                                // Thickness
            );

            Log.i(TAG,"BPM: " + BPM_RATE);
            Log.i(TAG,"BPM Current FPS: " + heartBeatFrequency / (60+fps));

        } else {
            Log.i(TAG,"NO BPM");
            BPM_RATE=0;
        }
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
        this.haarcascade_frontalface.detectMultiScale(grayFrame, faces, 1.1, 2, 2,
                new Size(absoluteFaceSize, absoluteFaceSize), new Size());

        // each rectangle in faces is a face: draw them!
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
        }
        if(facesArray.length >= 1) {
            getHeartRate(facesArray[0],frame);
        }

    }
}
