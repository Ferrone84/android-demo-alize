package com.example.duret.testalize;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.emrekose.recordbutton.OnRecordListener;
import com.emrekose.recordbutton.RecordButton;

import java.io.IOException;
import java.util.UUID;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.IdAlreadyExistsException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class EditSpeakerModel extends BaseActivity {

    RecordButton recordButton;
    Button updateSpeaker;
    EditText editSpeakerName;
    MediaRecorder mediaRecorder;
    SimpleSpkDetSystem alizeSystem;
    String audioSavePathInDevice = null;
    String speakerId = "";
    boolean newSpeaker = false;
    boolean isRecording = false;
    boolean recordExist = false;

    @Override
    public void onCreate(Bundle savedInstanceState) { // TODO apparemment mes problèmes d'appli qui plante sont liés au fait que j'utilise pas cette variable
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.edit_speaker_model);

            alizeSystem = (SimpleSpkDetSystem) getIntent().getSerializableExtra("alizeSystem");
            speakerId = getIntent().getStringExtra("speakerId");
            editSpeakerName = findViewById(R.id.add_speaker_name_editText);
            recordButton = findViewById(R.id.recordBtn);
            updateSpeaker = findViewById(R.id.update_speaker_button);
            updateSpeaker.setEnabled(false);

            final String originalSpeakerId = speakerId;
            recordButton.setMaxMilisecond(3000);

            if (speakerId.isEmpty()) {
                Log.e("", "NULL ");
                speakerId = "NoName";
                newSpeaker =  true;
                recordButton.setVisibility(View.INVISIBLE);
            }
            else {
                editSpeakerName.setText(speakerId);
            }

            setTitle("Edit '" + speakerId + "' Model");
            if (checkPermission()) {
                audioSavePathInDevice =
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp.3gp";
            }
            else {
                requestPermission();
            }

            editSpeakerName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    speakerId = charSequence.toString();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (!speakerId.isEmpty()) {
                        recordButton.setVisibility(View.VISIBLE);
                    }
                    else {
                        recordButton.setVisibility(View.INVISIBLE);
                    }
                    if (recordExist) {
                        updateSpeaker.setEnabled(true);
                    }
                }
            });

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
                    recordExist = true;
                    isRecording = false;
                    if (!speakerId.equals("NoName")) {
                        updateSpeaker.setEnabled(true);
                    }
                    try {
                        alizeSystem.addAudio(audioSavePathInDevice);
                        // TODO delete le file
                    } catch (AlizeException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    makeToast("Recording Completed");
                }
            });

            updateSpeaker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (newSpeaker)
                            alizeSystem.createSpeakerModel(speakerId);
                        else if (!originalSpeakerId.equals(speakerId)) {
                            //TODO modify speaker name in alize system
                        }
                        else
                            alizeSystem.adaptSpeakerModel(speakerId);

                        alizeSystem.resetAudio();
                        alizeSystem.resetFeatures();
                        makeToast("Changes applied !");
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
}

/*audioSavePathInDevice =
    Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/" + speakerId + "_" + String.valueOf(UUID.randomUUID()) + ".3gp";


package com.example.duret.testalize;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.emrekose.recordbutton.OnRecordListener;
import com.emrekose.recordbutton.RecordButton;

import java.io.IOException;
import java.util.UUID;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.IdAlreadyExistsException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class EditSpeakerModel extends BaseActivity {

    RecordButton recordButton;
    Button updateSpeaker;
    EditText editSpeakerName;
    MediaRecorder mediaRecorder;
    SimpleSpkDetSystem alizeSystem;
    String audioSavePathInDevice = null;
    String speakerId = "";
    boolean newSpeaker = false;
    boolean isRecording = false;
    boolean recordExist = false;

    @Override
    public void onCreate(Bundle savedInstanceState) { // TODO apparemment mes problèmes d'appli qui plante sont liés au fait que j'utilise pas cette variable
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.edit_speaker_model);

            alizeSystem = (SimpleSpkDetSystem) getIntent().getSerializableExtra("alizeSystem");
            speakerId = getIntent().getStringExtra("speakerId");
            editSpeakerName = findViewById(R.id.add_speaker_name_editText);
            recordButton = findViewById(R.id.recordBtn);
            updateSpeaker = findViewById(R.id.update_speaker_button);
            updateSpeaker.setEnabled(false);

            final String originalSpeakerId = speakerId;
            recordButton.setMaxMilisecond(3000);

            if (speakerId.isEmpty()) {
                Log.e("", "NULL ");
                speakerId = "NoName";
                newSpeaker =  true;
                recordButton.setVisibility(View.INVISIBLE);
            }
            else {
                editSpeakerName.setText(speakerId);
            }

            setTitle("Edit '" + speakerId + "' Model");
            setOutputFileName();

            editSpeakerName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    speakerId = charSequence.toString();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (!speakerId.isEmpty()) {
                        recordButton.setVisibility(View.VISIBLE);
                    }
                    else {
                        recordButton.setVisibility(View.INVISIBLE);
                    }
                    if (recordExist) {
                        updateSpeaker.setEnabled(true);
                    }
                    setOutputFileName();
                }
            });

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
                    recordExist = true;
                    isRecording = false;
                    if (!speakerId.equals("NoName")) {
                        updateSpeaker.setEnabled(true);
                    }
                    try {
                        alizeSystem.addAudio(audioSavePathInDevice);
                        // TODO delete le file
                    } catch (AlizeException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    makeToast("Recording Completed");
                }
            });

            updateSpeaker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (newSpeaker)
                            alizeSystem.createSpeakerModel(speakerId);
                        else if (!originalSpeakerId.equals(speakerId)) {
                            //TODO modify speaker name in alize system
                        }
                        else
                            alizeSystem.adaptSpeakerModel(speakerId);

                        alizeSystem.resetAudio();
                        alizeSystem.resetFeatures();
                        makeToast("Changes applied !");
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

    private void setOutputFileName() {
        if (checkPermission()) {
            audioSavePathInDevice = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + speakerId + ".3gp";
        }
        else {
            requestPermission();
        }
    }
}
*/
