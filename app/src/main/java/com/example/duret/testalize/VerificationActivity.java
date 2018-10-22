package com.example.duret.testalize;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class VerificationActivity extends RecordActivity {

    private final int ERROR_COLOR = Color.RED;
    private final int SUCCESS_COLOR = Color.rgb(0,150,0);

    private String speakerName;
    private TextView resultText;
    private boolean identify = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.verification);

            alizeSystem = SharedAlize.getInstance(getApplicationContext());
            speakerName = getIntent().getStringExtra("speakerName");
            resultText = findViewById(R.id.result_text);
            startRecordButton = findViewById(R.id.startBtn);
            stopRecordButton = findViewById(R.id.stopBtn);
            timeText = findViewById(R.id.timeText);

            String title = "Verify '" + speakerName + "' Model";
            if (speakerName.isEmpty()) {
                identify = true;
                title = getResources().getString(R.string.identify_speaker);
            }
            setTitle(title);

            startRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resultText.setText("");
                    startRecording();
                }
            });

            stopRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopRecording();
                }
            });
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }


    protected void afterRecordProcessing() {
        String result = "Error";

        if (identify) {
            //Try to match a speaker with the record

            try {
                SimpleSpkDetSystem.SpkRecResult identificationResult = alizeSystem.identifySpeaker();

                if (identificationResult.match) {
                    result = "Match:\n" + identificationResult.speakerId + "\nScore:\n" + identificationResult.score;
                    resultText.setTextColor(SUCCESS_COLOR);
                }
                else {
                    result = "No Match\nScore:\n" + identificationResult.score;
                    resultText.setTextColor(ERROR_COLOR);
                }
            }
            catch (AlizeException e) {
                e.printStackTrace();
            }
        }
        else {
            //compare the record with the speaker model

            try {
                SimpleSpkDetSystem.SpkRecResult verificationResult = alizeSystem.verifySpeaker(speakerName);

                if (verificationResult.match) {
                    result = "Match\nScore:\n" + verificationResult.score;
                    resultText.setTextColor(SUCCESS_COLOR);
                }
                else {
                    result = "No Match\nScore:\n" + verificationResult.score;
                    resultText.setTextColor(ERROR_COLOR);
                }
            }
            catch (AlizeException e) {
                e.printStackTrace();
            }
        }
        resultText.setText(result);
    }
}
