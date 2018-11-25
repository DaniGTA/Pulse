package eves.de.pulse;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaceRecognition {
    private static String TAG = "FaceRecognition";
    private static CascadeClassifier haarcascade_frontalface = new CascadeClassifier();

    private int absoluteFaceSize = 0;
    private Context context;

    FaceRecognition(Context current){
        this.context = current;
        loadResources();
    }

    /**
     * Get the libcascade file for the frontal face detection.
     * /res/raw/file_name.xml
     */
    private void loadResources() {
        try {
            // load cascade file from application resources
            InputStream is = context.getResources().openRawResource(
                    R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
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

    /**
     * Get all Faces from a frame.
     * @param frame
     * The frame
     * @return
     * all faces in a rect array.
     */
    Rect[] getFaces(Mat frame){
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
        haarcascade_frontalface.detectMultiScale(grayFrame, faces, 1.1, 2, 2,
                new Size(absoluteFaceSize, absoluteFaceSize), new Size());

        //return all detected faces
        return faces.toArray();
    }
}
