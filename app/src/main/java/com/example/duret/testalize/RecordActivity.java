package com.example.duret.testalize;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import AlizeSpkRec.AlizeException;

import static android.Manifest.permission.RECORD_AUDIO;

public class RecordActivity extends BaseActivity {

    protected static final int RECORDER_SAMPLERATE = 8000;
    protected static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    protected static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    protected int bufferElements2Rec = 2000;
    protected int bytesPerElement = 2; // 2 bytes in 16bit format

    protected long startTime;
    protected TextView timeText;
    protected boolean recordExists = false;
    protected AudioRecord recorder = null;
    protected Button startRecordButton, stopRecordButton;
    protected Thread recordingThread = null, addSamplesThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected boolean checkPermission() {
        return ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestPermission() {
        ActivityCompat.requestPermissions(RecordActivity.this, new
                String[]{RECORD_AUDIO}, 42);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 42: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecording();
                }
                else {
                    makeToast(getResources().getString(R.string.permission_error_message));
                }
                break;
            }
        }
    }

    protected void startRecording() {
        if (!checkPermission()) {
            requestPermission();
            return;
        }

        startRecordButton.setVisibility(View.INVISIBLE);
        stopRecordButton.setVisibility(View.VISIBLE);
        timeText.setText(R.string.default_time);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferElements2Rec * bytesPerElement);
        recorder.startRecording();

        if (recordExists) {
            try {
                alizeSystem.resetAudio();
                alizeSystem.resetFeatures();
            } catch (AlizeException e) {
                e.printStackTrace();
            }
            recordExists = false;
        }

        final List<short[]> audioPackets = Collections.synchronizedList(new ArrayList<short[]>());

        recordingThread = new Thread(new Runnable() {
            private Handler handler = new Handler();

            public void run() {
                startTime = System.currentTimeMillis();

                short[] tmpAudioSamples = new short[bufferElements2Rec];
                while (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    int samplesRead = recorder.read(tmpAudioSamples, 0, bufferElements2Rec);
                    if (samplesRead > 0) {
                        short[] samples = new short[samplesRead];
                        System.arraycopy(tmpAudioSamples, 0, samples, 0, samplesRead);

                        synchronized (audioPackets) {
                            audioPackets.add(samples);
                        }
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            long currentTime = System.currentTimeMillis() - startTime;
                            String result = new SimpleDateFormat("mm:ss:SS", defaultLanguage)
                                    .format(new Date(currentTime));
                            timeText.setText(result);
                        }
                    });
                }
            }
        }, "AudioRecorder Thread");

        addSamplesThread = new Thread(new Runnable() {
            private Handler handler = new Handler();

            @Override
            public void run() {
                short[] nextElement;
                try {
                    while ((recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                            || (!audioPackets.isEmpty())) {
                        nextElement = null;
                        synchronized (audioPackets) {
                            if (!audioPackets.isEmpty()) {
                                nextElement = audioPackets.get(0);
                                audioPackets.remove(0);
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
                        if (nextElement != null) {
                            try {
                                alizeSystem.addAudio(nextElement);
                            } catch (AlizeException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Throwable e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            makeToast(getResources().getString(R.string.no_sound_detected_recoloc));
                        }
                    });
                }
            }
        }, "addSamples Thread");

        recordingThread.start();
        addSamplesThread.start();
    }

    protected void stopRecording() {
        stopRecordButton.setVisibility(View.INVISIBLE);

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
            recordExists = true;
            recordingThread = null;
            addSamplesThread = null;
            startRecordButton.setVisibility(View.VISIBLE);

            makeToast(getResources().getString(R.string.recording_completed));
            afterRecordProcessing();
        }
    }

    protected void afterRecordProcessing() {}
}
