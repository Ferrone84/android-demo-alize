package com.example.duret.testalize;

import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.emrekose.recordbutton.OnRecordListener;
import com.emrekose.recordbutton.RecordButton;

import java.io.IOException;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class Verification extends BaseActivity{
    final int ERROR_COLOR = Color.RED;
    final int SUCCESS_COLOR = Color.GREEN;
    SimpleSpkDetSystem alizeSystem;
    String speakerId = "";
    TextView textViewTitle, resultText;
    RecordButton recordButton;
    MediaRecorder mediaRecorder;
    String audioSavePathInDevice = null;
    boolean identify = false;
    boolean isRecording = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.verification);

            alizeSystem = (SimpleSpkDetSystem) getIntent().getSerializableExtra("alizeSystem");
            speakerId = getIntent().getStringExtra("speakerId");
            textViewTitle = findViewById(R.id.verification_title);
            recordButton = findViewById(R.id.recordBtn);
            resultText = findViewById(R.id.result_text);

            recordButton.setMaxMilisecond(3000);// TODO enlever ça

            String title = "Verify '" + speakerId + "' Model";
            if (speakerId.isEmpty()) {
                identify = true;
                title = "Identify a speaker";
            }
            setTitle(title);
            textViewTitle.setText(title);

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

    public void makeResult() throws AlizeException {
        String result = "";
        if (identify) {
            Log.e("", "makeResult: identify");
            //essaye de trouver le speaker avec le record

            SimpleSpkDetSystem.SpkRecResult identificationResult = alizeSystem.identifySpeaker();
            if (identificationResult.match) {
                result = "Match speaker : " + identificationResult.speakerId;
                resultText.setTextColor(SUCCESS_COLOR);
            }
            else {
                result = "No Match";
                resultText.setTextColor(ERROR_COLOR);
            }
        }
        else {
            Log.e("", "makeResult: verify");
            //compare le record avec le model de l'user

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
