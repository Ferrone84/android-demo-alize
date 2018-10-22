package com.example.duret.testalize;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.IdAlreadyExistsException;

public class EditSpeakerModelActivity extends RecordActivity {

    private String[] speakers;
    private String currentSpeakerName, originalSpeakerId;
    private EditText editSpeakerName;
    private boolean newSpeaker = false;
    private boolean speakerIdAlreadyExists = false;
    private Button updateSpeaker;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_speaker_model);

        try {
            alizeSystem = SharedAlize.getInstance(getApplicationContext());
            speakers = alizeSystem.speakerIDs();
        }
        catch (IOException | AlizeException e) {
            e.printStackTrace();
        }

        currentSpeakerName = "";
        String speakerName = getIntent().getStringExtra("speakerName");
        editSpeakerName = findViewById(R.id.add_speaker_name_editText);
        timeText = findViewById(R.id.timeText);
        startRecordButton = findViewById(R.id.startBtn);
        stopRecordButton = findViewById(R.id.stopBtn);
        updateSpeaker = findViewById(R.id.update_speaker_button);

        updateSpeaker.setEnabled(false);
        originalSpeakerId = speakerName;

        if (speakerName.isEmpty()) {
            newSpeaker = true;
            startRecordButton.setVisibility(View.INVISIBLE);
            timeText.setVisibility(View.INVISIBLE);
        }
        else {
            editSpeakerName.setText(speakerName);
        }

        String title = "Edit '" + speakerName + "' Model";
        if (newSpeaker) {
            title = getResources().getString(R.string.new_speaker);
            updateSpeaker.setText(R.string.new_speaker);
        }
        setTitle(title);

        editSpeakerName.addTextChangedListener(editSpeakerNameListener);
        updateSpeaker.setOnClickListener(updateSpeakerListener);

        startRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

    private TextWatcher editSpeakerNameListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            currentSpeakerName = charSequence.toString();
            if (speakers.length != 0) {
                speakerIdAlreadyExists = false;
                for (String spkId : speakers) {
                    if (spkId.equals(currentSpeakerName) && !currentSpeakerName.equals(originalSpeakerId)) {
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
            if (!currentSpeakerName.isEmpty()) {
                startRecordButton.setVisibility(View.VISIBLE);
                timeText.setVisibility(View.VISIBLE);

                if (recordExists && !speakerIdAlreadyExists) {
                    updateSpeaker.setEnabled(true);
                }
            }
            else {
                updateSpeaker.setEnabled(false);
            }
        }
    };

    private View.OnClickListener updateSpeakerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                if (newSpeaker) {
                    alizeSystem.createSpeakerModel(currentSpeakerName);
                }
                else if (!originalSpeakerId.equals(currentSpeakerName)) {
                    //TODO modify speaker name in alize system -> updateSpeakerId(originalSpeakerId, speakerId)
                    if (recordExists)
                        alizeSystem.adaptSpeakerModel(currentSpeakerName);
                }
                else {
                    alizeSystem.adaptSpeakerModel(currentSpeakerName);
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
    };

    protected void afterRecordProcessing() {
        if (!currentSpeakerName.isEmpty() && !speakerIdAlreadyExists) {
            updateSpeaker.setEnabled(true);
        }
    }
}