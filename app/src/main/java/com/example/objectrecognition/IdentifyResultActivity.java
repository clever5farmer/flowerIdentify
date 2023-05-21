package com.example.objectrecognition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class IdentifyResultActivity extends AppCompatActivity {
    private Bitmap mRelatedImage;
    private String[] mSpeciesName;
    private TextView mTVScientificName, mTVCommonNames;
    private ImageView mImageView;
    private ImageButton mBTNBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_result);


        mTVScientificName = findViewById(R.id.tvScientificNameValue);
        mTVCommonNames = findViewById(R.id.tvCommonNamesValue);
        mImageView = findViewById(R.id.imageView);

        Intent intent = getIntent();
        String imageUrl = intent.getStringExtra("relatedImageUrl");
        DownloadImageTask downloadImageTask = new DownloadImageTask(mImageView);
        downloadImageTask.execute(imageUrl);
        if (mRelatedImage != null) {
            mImageView.setImageBitmap(mRelatedImage);
        }
        mSpeciesName = intent.getStringArrayExtra("speciesName");

        mTVScientificName.setText(mSpeciesName[0]);
        mTVCommonNames.setText(mSpeciesName[1]);

        mBTNBack = findViewById(R.id.btnBack);
        mBTNBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backIntent =  new Intent(getApplicationContext(),MainActivity.class);
                startActivity(backIntent);
            }
        });
    }
}
