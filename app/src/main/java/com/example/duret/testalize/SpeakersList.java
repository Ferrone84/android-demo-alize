package com.example.duret.testalize;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class SpeakersList extends BaseActivity{

    private SimpleSpkDetSystem alizeSystem;
    private SpeakerListAdapter adapter;
    private String[] speakers;
    private TextView noSpeakers;
    private Button identifyButton, removeAll;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.speakers_list);

            simpleSpkDetSystemInit();
            setupListViewAdapter();

            identifyButton = findViewById(R.id.identify_button);
            removeAll = findViewById(R.id.removeall_button);
            noSpeakers = findViewById(R.id.no_speakers);
            noSpeakers.setVisibility(View.INVISIBLE);

            updateSpeakersListObject();
            clearAndFill();
            updateListViewContent();

        } catch (AlizeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateSpeakerOnClickHandler(View v) {
        Speaker itemToUpdate = (Speaker)v.getTag();
        changeActivity(EditSpeakerModel.class, itemToUpdate.getName());
    }

    public void testSpeakerOnClickHandler(View v) {
        Speaker itemToTest = (Speaker)v.getTag();
        changeActivity(Verification.class, itemToTest.getName());
    }

    public void removeSpeakerOnClickHandler(View v) {
        Speaker itemToRemove = (Speaker)v.getTag();
        String speakerId = itemToRemove.getName();
        try {
            if (!speakerId.isEmpty()) {
                alizeSystem.removeSpeaker(speakerId);
            }
            updateSpeakersListObject();
        } catch (AlizeException e) {
            System.out.println(e.getMessage());
        }
        adapter.remove(itemToRemove);
        updateListViewContent();
    }

    public void addSpeaker(View v) {
        changeActivity(EditSpeakerModel.class, "");
    }

    public void identify(View v) {
        changeActivity(Verification.class, "");
    }

    public void removeAll(View v) {
        //TODO trouver pourquoi removeAllSpeakers() plante l'appli
        try {
            alizeSystem.removeAllSpeakers();
            adapter.clear();
            updateSpeakersListObject();
            updateListViewContent();
        } catch (AlizeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        try {
            super.onResume();
            updateSpeakersListObject();
            clearAndFill();
            updateListViewContent();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void clearAndFill() {
        if (speakers.length == 0) {
            return;
        }
        adapter.clear();
        for (String speakerId : speakers)
            adapter.insert(new Speaker(speakerId), adapter.getCount());
    }

    private void updateListViewContent() {
        if (speakers.length == 0) {
            if (adapter.getCount() == 0) {
                noSpeakers.setVisibility(View.VISIBLE);
                identifyButton.setEnabled(false);
                removeAll.setEnabled(false);
            }
            else
                noSpeakers.setVisibility(View.INVISIBLE);
        }
        else {
            noSpeakers.setVisibility(View.INVISIBLE);
            identifyButton.setEnabled(true);
            removeAll.setEnabled(true);
        }
    }

    private void setupListViewAdapter() {
        adapter = new SpeakerListAdapter(SpeakersList.this, R.layout.list_item, new ArrayList<Speaker>());
        ListView speakerListView = findViewById(R.id.speakerListView);
        speakerListView.setAdapter(adapter);
    }

    private void updateSpeakersListObject() throws AlizeException {
        speakers = alizeSystem.speakerIDs();
    }

    private void changeActivity(Class toActivity, String spkId) {
        Intent intent = new Intent(SpeakersList.this, toActivity);
        intent.putExtra("speakerId", spkId);
        startActivity(intent);
    }

    private void simpleSpkDetSystemInit() throws IOException, AlizeException {
        // Initialization:
        alizeSystem = SharedAlize.getInstance(getApplicationContext());

        // We also load the background model from the application assets
        InputStream backgroundModelAsset = getApplicationContext().getAssets().open("gmm/world.gmm");
        alizeSystem.loadBackgroundModel(backgroundModelAsset);
        backgroundModelAsset.close();
    }

}
