package com.example.opencvvideostabilizer;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {
    private Bitmap temp;
    private ImageView ivImage;
    private Button btnGrey, btnNormal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.i(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        btnNormal = findViewById(R.id.btnNormal);
        btnGrey = findViewById(R.id.btnGrey);
        ivImage = findViewById(R.id.ivImage);
        temp = ((BitmapDrawable) ivImage.getDrawable()).getBitmap();

        btnGrey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mat mat = new Mat();
                Utils.bitmapToMat(temp, mat);
                Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGB2GRAY);
                Bitmap dest = temp.copy(Bitmap.Config.ARGB_8888, true);
                Utils.matToBitmap(mat, dest);
                ivImage.setImageBitmap(dest);
            }
        });

        btnNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivImage.setImageBitmap(temp);
            }
        });
    }

}
