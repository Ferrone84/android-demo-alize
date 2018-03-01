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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class Verification extends BaseActivity{
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private int BytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private short sData[];
    private final int ERROR_COLOR = Color.RED;
    private final int SUCCESS_COLOR = Color.rgb(0,150,0);
    private SimpleSpkDetSystem alizeSystem;
    private String speakerId = "";
    private TextView resultText, timeText;
    private Button stopButton, startButton;
    private MediaRecorder mediaRecorder;
    private String audioSavePathInDevice = null;
    private boolean identify = false;
    private boolean isRecording = false;
    private boolean recordExist = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.verification);

            alizeSystem = SharedAlize.getInstance(getApplicationContext());
            speakerId = getIntent().getStringExtra("speakerId");
            resultText = findViewById(R.id.result_text);
            startButton = findViewById(R.id.startBtn);
            stopButton = findViewById(R.id.stopBtn);
            timeText = findViewById(R.id.timeText);

            String title = "Verify '" + speakerId + "' Model";
            if (speakerId.isEmpty()) {
                identify = true;
                title = "Identify a speaker";
            }
            setTitle(title);

            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    isRecording = true;
                    startButton.setVisibility(View.INVISIBLE);
                    stopButton.setVisibility(View.VISIBLE);
                    timeText.setText(R.string.default_time);
                    resultText.setText("");

                    if (recordExist) {
                        try {
                            alizeSystem.resetAudio();
                            alizeSystem.resetFeatures();
                        } catch (AlizeException e) {
                            e.printStackTrace();
                        }
                        recordExist = false;
                    }

                    if (checkPermission()) {
                        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
                        recorder.startRecording();
                    }
                    else {
                        requestPermission();
                    }

                    recordingThread = new Thread(new Runnable() {
                        private Handler handler = new Handler();
                        private long startTime = System.currentTimeMillis();

                        public void run() {
                            sData = new short[BufferElements2Rec]; //TODO discuter de la taille avec Teva
                            while (isRecording) {
                                recorder.read(sData, 0, BufferElements2Rec);

                                try {
                                    alizeSystem.addAudio(sData);
                                } catch (AlizeException e) {
                                    e.printStackTrace();
                                }

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        long currentTime = System.currentTimeMillis() - startTime;
                                        String result =
                                                new SimpleDateFormat("mm:ss:SS", Locale.ENGLISH).format(new Date(currentTime));

                                        timeText.setText(result);
                                    }
                                });
                            }
                        }
                    }, "AudioRecorder Thread");

                    recordingThread.start();
                }
            });

            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startButton.setVisibility(View.VISIBLE);
                    if (recorder != null) {
                        isRecording = false;
                        stopButton.setVisibility(View.INVISIBLE);
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                        recordingThread = null;
                        recordExist = true;

                        try {
                            alizeSystem.addAudio(sData);
                            makeResult();
                        } catch (AlizeException e) {
                            e.printStackTrace();
                        }

                        makeToast("Recording Completed");
                    }
                }
            });
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void makeResult() throws AlizeException {
        String result;
        if (identify) {
            //Try to match a speaker with the record

            SimpleSpkDetSystem.SpkRecResult identificationResult = alizeSystem.identifySpeaker();
            if (identificationResult.match) {
                result = "Match:\n" + identificationResult.speakerId + "\nScore:\n" + identificationResult.score;
                resultText.setTextColor(SUCCESS_COLOR);
            }
            else {
                result = "No Match";
                resultText.setTextColor(ERROR_COLOR);
            }
        }
        else {
            //compare the record with the speaker model

            SimpleSpkDetSystem.SpkRecResult verificationResult = alizeSystem.verifySpeaker(speakerId);

            if (verificationResult.match) {
                result = "Match\nScore:\n" + verificationResult.score;
                resultText.setTextColor(SUCCESS_COLOR);
            }
            else {
                result = "No Match";
                resultText.setTextColor(ERROR_COLOR);
            }
        }
        resultText.setText(result);
    }
}
