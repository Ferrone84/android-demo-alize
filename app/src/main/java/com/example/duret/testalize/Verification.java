package com.example.duret.testalize;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class Verification extends BaseActivity{
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferElements2Rec = 2000;
    private int bytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord recorder = null;
    private Thread recordingThread = null, addSamplesThread = null;
    private final int ERROR_COLOR = Color.RED;
    private final int SUCCESS_COLOR = Color.rgb(0,150,0);
    private SimpleSpkDetSystem alizeSystem;
    private String speakerId = "";
    private TextView resultText, timeText;
    private Button stopButton, startButton;
    private boolean identify = false;
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
                    resultText.setText("");
                    startRecording();
                }
            });

            stopButton.setOnClickListener(new View.OnClickListener() {
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void startRecording() {
        startButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        timeText.setText(R.string.default_time);

        try {
            alizeSystem.resetAudio();
            alizeSystem.resetFeatures();
        } catch (AlizeException e) {
            e.printStackTrace();
        }

        if (checkPermission()) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, bufferElements2Rec * bytesPerElement);
            recorder.startRecording();
        }
        else {
            requestPermission();
        }

        final List<short[]> audioPackets = Collections.synchronizedList(new ArrayList<short[]>());

        recordingThread = new Thread(new Runnable() {
            private Handler handler = new Handler();
            private long startTime = System.currentTimeMillis();

            public void run() {
                short[] tmpAudioSamples = new short[bufferElements2Rec];
                while (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    int samplesRead = recorder.read(tmpAudioSamples, 0, bufferElements2Rec);
                    Log.e("", "length read: " + samplesRead);
                    if (samplesRead > 0) {
                        short[] samples = new short[samplesRead];
                        System.arraycopy(tmpAudioSamples, 0, samples, 0, samplesRead);

                        synchronized (audioPackets) {
                            audioPackets.add(samples);
                            Log.e("", "length read: " + samplesRead + " / packets: " + audioPackets.size());
                        }
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            long currentTime = System.currentTimeMillis() - startTime;
                            String result = new SimpleDateFormat("mm:ss:SS", Locale.ENGLISH)
                                    .format(new Date(currentTime));

                            timeText.setText(result);
                        }
                    });
                }
                Log.e("endThread1", "arrsize: "+audioPackets.size());
            }
        }, "AudioRecorder Thread");

        addSamplesThread = new Thread(new Runnable() {
            private Handler handler = new Handler();
            @Override
            public void run() {
                short[] nextElement = null;
                while((recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                        || (!audioPackets.isEmpty())) {
                    nextElement = null;
                    synchronized (audioPackets) {
                        if (!audioPackets.isEmpty()) {
                            nextElement = audioPackets.get(0);
                            audioPackets.remove(0);
                            Log.e(String.valueOf(nextElement.length), "hasnext: " + Arrays.toString(nextElement));
                        }
                    }
                    if (nextElement != null) {
                        try {
                            alizeSystem.addAudio(nextElement);
                        } catch (AlizeException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    recordingThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!audioPackets.isEmpty()) {
                    nextElement = audioPackets.get(0);
                    audioPackets.remove(0);
                    Log.e(String.valueOf(nextElement.length), "hasnext: " + Arrays.toString(nextElement));
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            makeResult();
                        } catch (AlizeException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Log.e("endThread2", "arrsize: "+audioPackets.size());
            }
        }, "addSamples Thread");

        recordingThread.start();
        addSamplesThread.start();
    }

    private void stopRecording() {
        stopButton.setVisibility(View.INVISIBLE);

        if (recorder != null) {
            recorder.stop();
            try {
                recordingThread.join();
                addSamplesThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recorder.release();
            recorder = null;
            recordingThread = null;
            addSamplesThread = null;
            startButton.setVisibility(View.VISIBLE);

            makeToast("Recording Completed");
        }
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
                result = "No Match\nScore:\n" + identificationResult.score;
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
                result = "No Match\nScore:\n" + verificationResult.score;
                resultText.setTextColor(ERROR_COLOR);
            }
        }
        resultText.setText(result);
    }
}
