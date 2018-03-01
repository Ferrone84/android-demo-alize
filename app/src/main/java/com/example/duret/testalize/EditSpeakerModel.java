package com.example.duret.testalize;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.IdAlreadyExistsException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class EditSpeakerModel extends BaseActivity {

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private int bytesPerElement = 2; // 2 bytes in 16bit format

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private short audioSamples[];
    private Button updateSpeaker, stopButton, startButton;
    private TextView timeText;
    private EditText editSpeakerName;
    private SimpleSpkDetSystem alizeSystem;
    private String[] speakers;
    private int speakersCount;
    private String speakerId = "";
    private boolean newSpeaker = false;
    private boolean isRecording = false;
    private boolean recordExist = false;
    private boolean speakerIdAlreadyExists = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.edit_speaker_model);

            alizeSystem = SharedAlize.getInstance(getApplicationContext());
            speakerId = getIntent().getStringExtra("speakerId");
            editSpeakerName = findViewById(R.id.add_speaker_name_editText);
            timeText = findViewById(R.id.timeText);
            startButton = findViewById(R.id.startBtn);
            stopButton = findViewById(R.id.stopBtn);
            updateSpeaker = findViewById(R.id.update_speaker_button);
            updateSpeaker.setEnabled(false);
            speakers = alizeSystem.speakerIDs();
            speakersCount = speakers.length;

            final String originalSpeakerId = speakerId;

            if (speakerId.isEmpty()) {
                speakerId = "NoName";
                newSpeaker =  true;
                startButton.setVisibility(View.INVISIBLE);
            }
            else {
                editSpeakerName.setText(speakerId);
            }

            String title = "Edit '" + speakerId + "' Model";
            if (newSpeaker) {
                title = "New Speaker";
                updateSpeaker.setText(R.string.new_speaker);
            }
            setTitle(title);

            editSpeakerName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (isRecording)
                        stopRecording();

                    speakerId = charSequence.toString();
                    if (speakersCount != 0) {
                        speakerIdAlreadyExists = false;
                        for (String spkId : speakers) {
                            if (spkId.equals(speakerId) && !speakerId.equals(originalSpeakerId)) {
                                speakerIdAlreadyExists = true;
                                break;
                            }
                        }
                        if (speakerIdAlreadyExists) {
                            editSpeakerName.setError(getResources().getString(R.string.speakerExist));
                            updateSpeaker.setEnabled(false);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (!speakerId.isEmpty()) {
                        startButton.setVisibility(View.VISIBLE);
                        timeText.setVisibility(View.VISIBLE);
                    }
                    else {
                        startButton.setVisibility(View.INVISIBLE);
                        stopButton.setVisibility(View.INVISIBLE);
                        timeText.setVisibility(View.INVISIBLE);
                    }
                    if (recordExist && !speakerIdAlreadyExists) {
                        updateSpeaker.setEnabled(true);
                    }
                }
            });

            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startRecording();
                }
            });

            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopRecording();
                    startButton.setVisibility(View.VISIBLE);
                }
            });

            updateSpeaker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (newSpeaker) {
                            alizeSystem.createSpeakerModel(speakerId);
                            alizeSystem.saveSpeakerModel(speakerId,
                                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/save.pcm");
                        }
                        else if (!originalSpeakerId.equals(speakerId)) {
                            //TODO modify speaker name in alize system -> updateSpeakerId(originalSpeakerId, speakerId)
                            if (recordExist)
                                alizeSystem.adaptSpeakerModel(speakerId);
                        }
                        else {
                            alizeSystem.adaptSpeakerModel(speakerId);
                        }

                        alizeSystem.resetAudio();
                        alizeSystem.resetFeatures();
                        finish();
                    } catch (AlizeException e) {
                        e.printStackTrace();
                    } catch (IdAlreadyExistsException e) {
                        e.printStackTrace();
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

    private void startRecording() {
        isRecording = true;
        startButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        timeText.setText(R.string.default_time);

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
                    RECORDER_AUDIO_ENCODING, bufferElements2Rec * bytesPerElement /2);
            recorder.startRecording();
        }
        else {
            requestPermission();
        }

        recordingThread = new Thread(new Runnable() {
            private Handler handler = new Handler();
            private long startTime = System.currentTimeMillis();

            public void run() {
                short[] tmpAudioSamples = new short[bufferElements2Rec];
                while (isRecording) {
                    int samplesRead = recorder.read(tmpAudioSamples, 0, bufferElements2Rec);
                    short[] samples = new short[samplesRead];
                    System.arraycopy(tmpAudioSamples, 0, samples, 0, samplesRead);

                    Log.e("", "run: "+samplesRead);

                    //List list = Collections.synchronizedList(new ArrayList<short[]>());
                    //list.add(samples);
                    Log.e("lul", "lel ");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            /*
                                while (isRecording) {
                                    synchronized (list) {
                                      Iterator i = list.iterator(); // Must be in synchronized block
                                      if (i.hasNext())
                                        addAudio()
                                    }
                                }

                                while (il reste des éléments) {
                                    on les addAudio
                                }

                                on enable le bouton pour le add/update speaker

                             */
                            long currentTime = System.currentTimeMillis() - startTime;
                            String result = new SimpleDateFormat("mm:ss:SS", Locale.ENGLISH)
                                    .format(new Date(currentTime));

                            timeText.setText(result);
                        }
                    });
                }
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    private void stopRecording() {
        isRecording = false;
        stopButton.setVisibility(View.INVISIBLE);
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            recordExist = true;

            if (!speakerId.equals("NoName") && !speakerIdAlreadyExists) {
                updateSpeaker.setEnabled(true); //TODO kick ça et attendre que le thread du record le fasse
            }
            /*try {
                alizeSystem.addAudio(audioSamples);
            } catch (AlizeException e) {
                e.printStackTrace();
            }*/

            makeToast("Recording Completed");
        }
    }

    /* TODO can be usefull
    private byte[] short2byte(short[] audioSamples) {
        int shortArrsize = audioSamples.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (audioSamples[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (audioSamples[i] >> 8);
            audioSamples[i] = 0;
        }
        return bytes;
    }*/
}