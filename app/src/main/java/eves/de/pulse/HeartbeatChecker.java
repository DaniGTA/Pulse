package eves.de.pulse;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import ddf.minim.analysis.FFT;

public class HeartbeatChecker {
    private static String TAG = "HeartBeatChecker";

    private ArrayList<Float> floats = new ArrayList<>();
    private int bufferSize = 32;
    private FFT fft;
    private float BPM_RATE;
    private int BPM_RATE_NONE_COUNTER = 0;

    HeartbeatChecker(){
        int sampleRate = bufferSize;
        fft = new FFT(bufferSize, sampleRate);
    }

    /**
     * Get the forehead for better results and better fps (smaller area too scan)
     * @param rect rect of the face.
     * @return the rect for the forehead
     */
    private Rect getForehead(Rect rect){
        float FORE_HEAD_DETECTION_PERCENTAGE = 46;

        Point start_point = new Point(rect.x + (rect.width * (FORE_HEAD_DETECTION_PERCENTAGE) / 100), rect.y + 75);
        Point end_point = new Point((rect.x + rect.width) - (rect.width * (FORE_HEAD_DETECTION_PERCENTAGE) / 100), rect.y + 90);

        return new Rect(start_point, end_point);
    }

    /**
     * Draw a blue rectangle on the forehead position.
     * @param mat
     * The current frame.
     * @param foreHead
     * The forehead rect. (use getForehead to get the Forehead rect)
     */
    private void drawForehead(Mat mat, Rect foreHead)
    {
        Imgproc.rectangle(mat, foreHead.tl(),foreHead.br(), new Scalar(0, 0, 255));
    }

    /**
     * (This is very CPU intensive)
     * @param rect face.
     * @param mat current frame.
     */
     float getHeartRate(Rect rect, Mat mat){
        Rect rectForeHead = getForehead(rect);
        drawForehead(mat,rectForeHead);

        Mat inputMatForHR = mat.submat(rectForeHead);
        float NEW_BPM_RATE = getBPMFromArea(inputMatForHR);
        if(NEW_BPM_RATE != 0) {
            BPM_RATE = NEW_BPM_RATE;
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

         }
        return BPM_RATE;
    }

    /**
     * (This is very CPU intensive)
     * @param rect face.
     * @param mat current frame.
     */
    float getHeartRateFromArea(Rect rect, Mat mat){
        drawForehead(mat,rect);

        Mat inputMatForHR = mat.submat(rect);
        float BPM_RATE = getBPMFromArea(inputMatForHR);
        if(BPM_RATE != 0) {
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
        }
        return BPM_RATE;
    }

    /**
     * Extract heart beat from the green color change in the area
     * @param inputMatForHR area
     * @return BPM
     */
    private float getBPMFromArea(Mat inputMatForHR){
        float green_avg = getAvgGreen(inputMatForHR);

        if (floats.size() < bufferSize) {
            floats.add(green_avg);
        } else floats.remove(0);
        float[] sample;
        sample = new float[floats.size()];
        for (int i = 0; i < floats.size(); i++) {
            float f = floats.get(i);
            sample[i] = f;
        }

        if (sample.length >= bufferSize) {
            //fft.window(FFT.NONE);

            fft.forward(sample, 0);
            //    bpf = new BandPass(centerFreq, bandwidth, sampleRate);
            //    in.addEffect(bpf);


            float heartBeatFrequency = 20;
//                        System.out.println("FFT Secsize : " + fft.specSize());
            for (int i = 0; i < fft.specSize(); i++) { // draw the line for frequency band i, scaling it up a bit so we can see it
                heartBeatFrequency = Math.max(heartBeatFrequency, fft.getBand(i));
//                    System.out.println("Band value : " + fft.getBand(i));
            }

            float bw = fft.getBandWidth(); // returns the width of each frequency band in the spectrum (in Hz).
//                System.out.println("Bandwidth : " + bw); // returns 21.5332031 Hz for spectrum [0] & [512]

            heartBeatFrequency = bw * heartBeatFrequency;

            //float BPM_RATE = heartBeatFrequency / (60);
            float BPM_RATE = heartBeatFrequency / 60;

            Log.i(TAG,"BPM: " + BPM_RATE);
            BPM_RATE_NONE_COUNTER = 0;
            return BPM_RATE;
        } else {
            Log.i(TAG,"NO BPM");
            BPM_RATE_NONE_COUNTER++;
            if (BPM_RATE_NONE_COUNTER >= Startsite.getFps()*4) {
                reset();
            }
            return 0;
        }
    }

    /**
     * Resets all values.
     */
    private void reset(){
        BPM_RATE_NONE_COUNTER = 0;
        BPM_RATE = 0;
        floats.clear();
    }

    /**
     * Get avg green of the area.
     * @param inputMatForHR area.
     * @return avg green.
     */
    private float getAvgGreen(Mat inputMatForHR){
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
        Log.i(TAG,"AvgGreen: "+ green_avg / numPixels);
        return green_avg / numPixels;
    }
}
