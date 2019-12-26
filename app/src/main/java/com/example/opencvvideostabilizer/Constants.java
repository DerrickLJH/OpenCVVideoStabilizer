package com.example.opencvvideostabilizer;

import org.bytedeco.opencv.opencv_core.Mat;

public class Constants {
    public static Mat frame2;
    public static Mat frame1;

    public static int k;

    public static final int HORIZONTAL_BORDER_CROP = 30;

    public static Mat smoothedMat;
    public static Mat affine;

    public static Mat smoothedFrame;

    public static double dx;
    public static double dy;
    public static double da;
    public static double ds_x;
    public static double ds_y;

    public static double sx;
    public static double sy;

    public static double scaleX;
    public static double scaleY;
    public static  double thetha;
    public static double transX;
    public static double transY;

    public static double diff_scaleX;
    public static double diff_scaleY;
    public static double diff_transX;
    public static double diff_transY;
    public static double diff_thetha;

    public static double errscaleX;
    public static double errscaleY;
    public static double errthetha;
    public static double errtransX;
    public static double errtransY;

    public static double Q_scaleX;
    public static double Q_scaleY;
    public static double Q_thetha;
    public static double Q_transX;
    public static double Q_transY;

    public static double R_scaleX;
    public static double R_scaleY;
    public static double R_thetha;
    public static double R_transX;
    public static double R_transY;

    public static double sum_scaleX;
    public static double sum_scaleY;
    public static double sum_thetha;
    public static double sum_transX;
    public static double sum_transY;
}
