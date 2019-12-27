package com.example.opencvvideostabilizer;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;

import static org.bytedeco.opencv.global.opencv_core.IPL_DEPTH_8U;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String LOG_TAG = "MainActivity";
    final int RECORD_LENGTH = 10;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat edgesMat;
    private FloatingActionButton btnRecord;
    private Frame yuvImage;
    private int resolutionIndex = 0;

    private IplImage videoImage = null;

    boolean recording = false;
    private volatile FFmpegFrameRecorder recorder;
    Frame[] images;
    long[] timestamps;
    ShortBuffer[] samples;
    int imagesIndex, samplesIndex;

    private int sampleAudioRateInHz = 44100;
    // private int imageWidth = 480;
    // private int imageHeight = 320;
    private int imageWidth = 480;
    private int imageHeight = 320;
    private int frameRate = 30;
    private int IplImageChannel = 4;
    private String RECIEVE_BYTE_BUFFER = "";
    private Thread audioThread;
    volatile boolean runAudioThread = true;
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;

    private String ffmpeg_link;

    long startTime = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(LOG_TAG, "OpenCV loaded successfully");
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
        Log.i(LOG_TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        if (checkPermission()) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1234);
        }

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.cameraView);
        btnRecord = findViewById(R.id.btnCapture);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recording) {
                    btnRecord.setImageResource(R.drawable.ic_stop);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        DrawableCompat.setTintList(DrawableCompat.wrap(btnRecord.getBackground()), getColorStateList(R.color.colorTrans));
                    }
                    Log.w(LOG_TAG, "Start Button Pushed");
                    startRecording(v);
                    recording = true;
                } else {
                    btnRecord.setImageResource(R.drawable.ic_videocam);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        DrawableCompat.setTintList(DrawableCompat.wrap(btnRecord.getBackground()), getColorStateList(R.color.colorGray));
                    }
                    Log.w(LOG_TAG, "Stop Button Pushed");
                    stopRecording();
                    recording = false;
                }
            }
        });
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
            Log.d(LOG_TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(LOG_TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        edgesMat = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        if (edgesMat != null)
            edgesMat.release();

        edgesMat = null;
    }


    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        Mat rgba = inputFrame.rgba();
/*        MatOfPoint featuresOld = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(inputFrame.gray(), featuresOld, 100, 0.01, 0.1);
        Mat corrected = stabilizeImage(rgba, inputFrame.rgba(), new MatOfPoint2f(featuresOld.toArray()));*/
        if (recording) {
            byte[] byteFrame = new byte[(int) (rgba.total() * rgba.channels())];
            rgba.get(0, 0, byteFrame);
            onFrame(byteFrame);
        }
        return rgba;
    }

    int frames = 0;

    private void onFrame(byte[] data) {

        Log.e("", "data frame::" + data.length);

        if (videoImage != null && recording) {
            long videoTimestamp = 1000 * (System.currentTimeMillis() - startTime);
            // Put the camera preview frame right into the yuvIplimage object
            // videoImage.getByteBuffer().put(data);

            // byte[] byteData = new byte[1024];
            // for (int i = 0; i < byteData.length; i++) {
            // byteData[i] = data[i];
            // }
            // videoImage.getByteBuffer().put(byteData);
            videoImage.getByteBuffer().put(data);

            // videoImage = IplImage.createFrom(data);
            // videoImage = cvDecodeImage(cvMat(1, data.length, CV_8UC1,
            // new BytePointer(data)));
            try {

                if (recorder != null) {
                    // Get the correct time
                    recorder.setTimestamp(videoTimestamp);

                    // Record the image into FFmpegFrameRecorder
                    // recorder.record(videoImage);
                    OpenCVFrameConverter.ToIplImage iplImageConverter = new OpenCVFrameConverter.ToIplImage();
                    Frame videoFrame = iplImageConverter.convert(videoImage);
                    recorder.record(videoFrame);

                    frames++;

                    Log.i(LOG_TAG, "Wrote Frame: " + frames);
                }

            } catch (FFmpegFrameRecorder.Exception e) {
                Log.v(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
        }

    }

    public Mat stabilizeImage(Mat newFrame, Mat oldFrame, MatOfPoint2f featuresOld) {
        Mat greyNew = new Mat();
        Mat greyOld = new Mat();
        Imgproc.cvtColor(newFrame, greyNew, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(oldFrame, greyOld, Imgproc.COLOR_BGR2GRAY);
        MatOfPoint2f currentFeatures = new MatOfPoint2f();
        MatOfFloat err = new MatOfFloat();
        MatOfByte status = new MatOfByte();
        Video.calcOpticalFlowPyrLK(greyOld, greyNew, featuresOld, currentFeatures, status, err);
        Mat correctionMatrix = Calib3d.estimateAffine2D(currentFeatures, featuresOld);
        Mat corrected = new Mat();
        Imgproc.warpAffine(newFrame, corrected, correctionMatrix, newFrame.size());
        return corrected;
    }

    private void initRecorder() {
        Log.w(LOG_TAG, "initRecorder");

        // int depth = com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

        // if (yuvIplimage == null) {
        // Recreated after frame size is set in surface change method
        // videoImage = IplImage.create(imageWidth, imageHeight, depth,
        // channels);
        // yuvIplimage = IplImage
        // .create(imageWidth, imageHeight, IPL_DEPTH_32S, 2);
        // videoImage = IplImage.create(imageWidth, imageHeight, IPL_DEPTH_8U,
        // 2);
//		 videoImage = IplImage.create(1280, 720, IPL_DEPTH_8U,
//		 IplImageChannel);
		videoImage = IplImage.create(720, 1280, IPL_DEPTH_8U, IplImageChannel);
//        videoImage = IplImage.create(720, 1280, IPL_DEPTH_8U, 2);

        Log.v(LOG_TAG, "IplImage.create");
        // }

        File videoFile = new File(Environment.getExternalStorageDirectory(), "video.mp4");
        boolean mk = videoFile.getParentFile().mkdirs();
        Log.v(LOG_TAG, "Mkdir: " + mk);

        boolean del = videoFile.delete();
        Log.v(LOG_TAG, "del: " + del);

        try {
            boolean created = videoFile.createNewFile();
            Log.v(LOG_TAG, "Created: " + created);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ffmpeg_link = videoFile.getAbsolutePath();
        recorder = new FFmpegFrameRecorder(ffmpeg_link, imageWidth,
                imageHeight, 1);
        Log.v(LOG_TAG, "FFmpegFrameRecorder: " + ffmpeg_link + " imageWidth: "
                + imageWidth + " imageHeight " + imageHeight);

        recorder.setFormat("mp4");
        Log.v(LOG_TAG, "recorder.setFormat(\"mp4\")");

        recorder.setSampleRate(sampleAudioRateInHz);
        Log.v(LOG_TAG, "recorder.setSampleRate(sampleAudioRateInHz)");

        // re-set in the surface changed method as well
        recorder.setFrameRate(frameRate);
        Log.v(LOG_TAG, "recorder.setFrameRate(frameRate)");
        recorder.setVideoCodec(13);
        recorder.setVideoQuality(1.0D);
        // recorder.setVideoBitrate(40000);

        // Create audio recording thread
        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
    }

    // Start the capture
    public void startRecording(View v) {
        initRecorder();
        recording = !recording;

        Log.i(LOG_TAG, "Recording: " + recording);

        if (recording) {
            startTime = System.currentTimeMillis();
            try {
                recorder.start();

                Log.i(LOG_TAG, "STARTED RECORDING.");

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            stopRecording();
        }
    }


    public void stopRecording() {
        // This should stop the audio thread from running
        runAudioThread = false;

        if (recorder != null) {
            Log.v(LOG_TAG,
                    "Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();

            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(MainActivity.this,
                    "saved ffmpeg_link::" + ffmpeg_link, Toast.LENGTH_SHORT)
                    .show();
            recorder = null;
            recording = false;
        }

        MediaScannerConnection.scanFile(MainActivity.this,
                new String[] { ffmpeg_link }, null, null);
    }


    //---------------------------------------------
    // audio thread, gets and encodes audio data
    //---------------------------------------------
    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            if (RECORD_LENGTH > 0) {
                samplesIndex = 0;
                samples = new ShortBuffer[RECORD_LENGTH * sampleAudioRateInHz * 2 / bufferSize + 1];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = ShortBuffer.allocate(bufferSize);
                }
            } else {
                audioData = ShortBuffer.allocate(bufferSize);
            }

            Log.d(LOG_TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {
                if (RECORD_LENGTH > 0) {
                    audioData = samples[samplesIndex++ % samples.length];
                    audioData.position(0).limit(0);
                }
                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
                    Log.v(LOG_TAG, "bufferReadResult: " + bufferReadResult);
                    // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                    // Why?  Good question...
                    if (recording) {
                        if (RECORD_LENGTH <= 0) {
                            try {
                                recorder.recordSamples(audioData);
                                //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                            } catch (FFmpegFrameRecorder.Exception e) {
                                Log.v(LOG_TAG, e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            Log.v(LOG_TAG, "AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(LOG_TAG, "audioRecord released");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1234: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private boolean checkPermission() {
        int permissionCheck_Record = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO);
        int permissionCheck_Cam = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA);
        int permissionCheck_Write = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck_Cam == PermissionChecker.PERMISSION_GRANTED && permissionCheck_Record == PermissionChecker.PERMISSION_GRANTED && permissionCheck_Write == PermissionChecker.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

}
