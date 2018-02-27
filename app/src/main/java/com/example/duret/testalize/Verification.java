package com.example.duret.testalize;

import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import com.emrekose.recordbutton.OnRecordListener;
import com.emrekose.recordbutton.RecordButton;

import java.io.IOException;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class Verification extends BaseActivity{
    private final int ERROR_COLOR = Color.RED;
    private final int SUCCESS_COLOR = Color.rgb(0,150,0);
    private SimpleSpkDetSystem alizeSystem;
    private String speakerId = "";
    private TextView resultText;
    private RecordButton recordButton;
    private MediaRecorder mediaRecorder;
    private String audioSavePathInDevice = null;
    private boolean identify = false;
    private boolean isRecording = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.verification);

            alizeSystem = SharedAlize.getInstance(getApplicationContext());
            speakerId = getIntent().getStringExtra("speakerId");
            recordButton = findViewById(R.id.recordBtn);
            resultText = findViewById(R.id.result_text);

            recordButton.setMaxMilisecond(3000);// TODO enlever ça

            String title = "Verify '" + speakerId + "' Model";
            if (speakerId.isEmpty()) {
                identify = true;
                title = "Identify a speaker";
            }
            setTitle(title);

            if (checkPermission()) {
                audioSavePathInDevice =
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp.3gp";
            }
            else {
                requestPermission();
            }

            recordButton.setRecordListener(new OnRecordListener() {
                @Override
                public void onRecord() {
                    if (!isRecording) {
                        isRecording = true;
                        try {
                            mediaRecorder = new MediaRecorder();
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                            mediaRecorder.setOutputFile(audioSavePathInDevice);
                            mediaRecorder.prepare();
                            mediaRecorder.start();
                        } catch (IllegalStateException e) {
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }

                @Override
                public void onRecordCancel() {
                    mediaRecorder.stop();
                    isRecording = false;
                }

                @Override
                public void onRecordFinish() {
                    mediaRecorder.stop();
                    isRecording = false;
                    try {
                        alizeSystem.addAudio(audioSavePathInDevice);
                        // TODO delete le file
                        makeResult();
                    } catch (AlizeException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    makeToast("Recording Completed");
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
                result = "Match: " + identificationResult.speakerId;
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
                result = "Match";
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
