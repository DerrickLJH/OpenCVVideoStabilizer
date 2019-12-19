package com.example.opencvvideostabilizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.SparseOpticalFlow;
import org.opencv.video.Video;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.List;
import java.util.Random;


public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private static final int VIEW_MODE_KLT_TRACKER = 0;
    private static final int VIEW_MODE_OPTICAL_FLOW = 1;

    private Mat mRgba, mGray, mIntermediateMat, mPrevGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private int mViewMode;

    MatOfPoint2f prevFeatures, nextFeatures;
    MatOfPoint features;

    MatOfByte status;
    MatOfFloat err;

    int counter = 0;


    private MenuItem mItemPreviewOpticalFlow, mItemPreviewKLT;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.cameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        resetVars();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
//        final int viewMode = mViewMode;
//        switch (viewMode) {
//            case VIEW_MODE_OPTICAL_FLOW:
                mRgba = inputFrame.rgba();

                if (counter % 2 == 0){
                    Core.flip(mRgba,mRgba,1);
                    Imgproc.cvtColor(mRgba,mRgba, Imgproc.COLOR_RGB2GRAY);
                }

                counter = counter + 1;
//                src = mGray;
//                update();
//                if (features.toArray().length == 0) {
//                    int rowStep = 50, colStep = 100;
//                    int nRows = mGray.rows() / rowStep, nCols = mGray.cols() / colStep;
//
//                    Point points[] = new Point[nRows * nCols];
//                    for (int i = 0; i < nRows; i++) {
//                        for (int j = 0; j < nCols; j++) {
//                            points[i * nCols + j] = new Point(j * colStep, i * rowStep);
//                        }
//                    }
//                    features.fromArray(points);
//                    prevFeatures.fromList(features.toList());
//                    mPrevGray = mGray.clone();
//                    nextFeatures.fromArray(prevFeatures.toArray());
//                    Video.calcOpticalFlowPyrLK(mPrevGray, mGray, prevFeatures, nextFeatures, status, err);
//                    List<Point> prevList = features.toList(), nextList = nextFeatures.toList();
//
//                    Scalar color = new Scalar(255);
//
//                    for (int i = 0; i < prevList.size(); i++) {
//                        Imgproc.line(mGray, prevList.get(i), nextList.get(i), color);
//                        mPrevGray = mGray.clone();
//
//                    }
//                    break;
//                }
//            default:
//                mViewMode = VIEW_MODE_OPTICAL_FLOW;
//        }
        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mItemPreviewKLT = menu.add("KLT Tracker");
        mItemPreviewOpticalFlow = menu.add("Optical Flow");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item == mItemPreviewOpticalFlow) {
            mViewMode = VIEW_MODE_OPTICAL_FLOW ;
            resetVars();
        } else if (item == mItemPreviewKLT) {
            mViewMode = VIEW_MODE_KLT_TRACKER;
            resetVars();
        }

        return true;
    }

    private void resetVars() {
        mPrevGray = new Mat(mGray.rows(), mGray.cols(), CvType.CV_8UC1);
        features = new MatOfPoint();
        prevFeatures = new MatOfPoint2f();
        nextFeatures = new MatOfPoint2f();
        status = new MatOfByte();
        err = new MatOfFloat();
    }

//    private void update() {
//        /// Parameters for Shi-Tomasi algorithm
//        maxCorners = Math.max(maxCorners, 1);
//        MatOfPoint corners = new MatOfPoint();
//        double qualityLevel = 0.01;
//        double minDistance = 10;
//        int blockSize = 3, gradientSize = 3;
//        boolean useHarrisDetector = false;
//        double k = 0.04;
//
//        /// Copy the source image
//        Mat copy = src.clone();
//
//        /// Apply corner detection
//        Imgproc.goodFeaturesToTrack(srcGray, corners, maxCorners, qualityLevel, minDistance, new Mat(),
//                blockSize, gradientSize, useHarrisDetector, k);
//
//        /// Draw corners detected
//        System.out.println("** Number of corners detected: " + corners.rows());
//        int[] cornersData = new int[(int) (corners.total() * corners.channels())];
//        corners.get(0, 0, cornersData);
//        int radius = 4;
//        for (int i = 0; i < corners.rows(); i++) {
//            Imgproc.circle(copy, new Point(cornersData[i * 2], cornersData[i * 2 + 1]), radius,
//                    new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)), Core.FILLED);
//        }
//
//    }
}
