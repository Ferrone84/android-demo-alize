package com.example.duret.testalize;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import AlizeSpkRec.AlizeException;
import AlizeSpkRec.SimpleSpkDetSystem;

public class SpeakersList extends BaseActivity{

    SimpleSpkDetSystem alizeSystem;
    private SpeakerListAdapter adapter;
    String[] speakers;
    RelativeLayout rl;
    TextView noSpeakers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.speakers_list);

            simpleSpkDetSystemInit();
            setupListViewAdapter();

            rl = findViewById(R.id.rl);
            noSpeakers = findViewById(R.id.no_speakers);
            noSpeakers.setVisibility(View.INVISIBLE);
            speakers = alizeSystem.speakerIDs();
            updateListViewContent();
            clearAndFill();
        }
        catch (Throwable e) {
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
        if (speakers.length == 0 && adapter.getCount() == 0) {
            noSpeakers.setVisibility(View.VISIBLE);
        }
        else {
            noSpeakers.setVisibility(View.INVISIBLE);
        }
    }

    private void setupListViewAdapter() {
        adapter = new SpeakerListAdapter(SpeakersList.this, R.layout.list_item, new ArrayList<Speaker>());
        ListView speakerListView = findViewById(R.id.speakerListView);
        speakerListView.setAdapter(adapter);
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
            if (!speakerId.isEmpty())
                alizeSystem.removeSpeaker(speakerId);
        } catch (AlizeException e) {
            System.out.println(e.getMessage());
        }
        refreshSpeakersList();
        adapter.remove(itemToRemove);
        updateListViewContent();
    }

    public void addSpeaker(View v) {
        adapter.insert(new Speaker(""), adapter.getCount());
        updateListViewContent();
    }

    public void identify(View v) {

    }

    public void removeAll(View v) {
        try {
            alizeSystem.removeAllSpeakers();
        } catch (AlizeException e) {
            e.printStackTrace();
        }
        adapter.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshSpeakersList();
        updateListViewContent();
        clearAndFill();
    }

    private void refreshSpeakersList() {
        try {
            speakers = alizeSystem.speakerIDs();
        } catch (AlizeException e) {
            e.printStackTrace();
        }
    }

    private void changeActivity(Class toActivity, String spkId) {
        Intent intent = new Intent(SpeakersList.this, toActivity);
        intent.putExtra("alizeSystem", alizeSystem);
        intent.putExtra("speakerId", spkId);
        startActivity(intent);
    }

    public void simpleSpkDetSystemInit() throws Throwable {
        // Initialization:
        // We create a new spk det system with a config (extracted from the assets) and a directory where it can store files (models + temporary files)
        InputStream configAsset = getApplicationContext().getAssets().open("AlizeDefault.cfg");
        alizeSystem = new SimpleSpkDetSystem(configAsset, getApplicationContext().getFilesDir().getPath());
        configAsset.close();

        // We also load the background model from the application assets
        InputStream backgroundModelAsset = getApplicationContext().getAssets().open("gmm/world.gmm");
        alizeSystem.loadBackgroundModel(backgroundModelAsset);
        backgroundModelAsset.close();


        // Testing the system
        System.out.println("Status after initialization:");
        System.out.println("  # of features: " + alizeSystem.featureCount());
        System.out.println("  # of models: " + alizeSystem.speakerCount());
        System.out.println("  UBM loaded: " + alizeSystem.isUBMLoaded());
        System.out.println("***********************************************");
        System.out.println();

        // Record audio and send it to the system
        // (Here, we just load audio from an asset, because it's easier than recording from the microphone for a quick demo)

        // Version A: using the method of SimpleSpkDetSystem that reads directly from the asset input stream.
        // For this approach, the data format in the asset file must match the format specified in the configuration.
        //alizeSystem.addAudio(getApplicationContext().getAssets().open("data/PB2M-2_UV.s16"));

        // Version B: testing the method of SimpleSpkDetSystem that takes 16-bit signed integer linear PCM,
        // regardless of what the configuration file says.
        InputStream audioIS = getApplicationContext().getAssets().open("data/PB2M-2_UV.s16");
        ByteArrayOutputStream audioBytes = new ByteArrayOutputStream();
        while (audioIS.available() > 0) {
            audioBytes.write(audioIS.read());
        }
        byte[] tmpBytes = audioBytes.toByteArray();
        short[] audioL16Samples = new short[tmpBytes.length/2];
        for (int i=0; i<tmpBytes.length/2; i++) {
            audioL16Samples[i] = (short) ((short)(tmpBytes[2*i])*256 + tmpBytes[2*i+1]);
        }
        alizeSystem.addAudio(audioL16Samples);
        //alizeSystem.addAudio("lel"); //list_item IOException
        //alizeSystem.removeSpeaker("lel"); //list_item simple Exception
        //alizeSystem.loadSpeakerModel("",""); //list_item FileNotFoundException
        //alizeSystem.createSpeakerModel("lel");
        //alizeSystem.createSpeakerModel("lel"); //list_item IdAlreadyExistsException
        //InputStream list_item = getApplicationContext().getAssets().open("gmm/AG.gmm");
        //alizeSystem.loadSpeakerModel("AG",list_item);
        //alizeSystem.loadSpeakerModel("AG",list_item);//eof exception !?? //faire remonter l'erreur créer une issue
        //alizeSystem.addFeatures("lel"); //list_item FileNotFoundException
        //alizeSystem.saveSpeakerModel("",""); //mixture not found ?
        alizeSystem.createSpeakerModel("UV");
        alizeSystem.saveSpeakerModel("UV","montestsave"); //pas besoin de spécifier d'extension
        SimpleSpkDetSystem.SpkRecResult srr = alizeSystem.verifySpeaker("UV");
        System.out.println(srr.speakerId+" "+srr.match+" / "+srr.score); //ça ça marche

        // Train a model with the audio
//        alizeSystem.createSpeakerModel("UV");

        System.out.println("Status after training first speaker model (UV):");
        System.out.println("  # of features: " + alizeSystem.featureCount());
        System.out.println("  # of models: " + alizeSystem.speakerCount());
        System.out.println("  UBM loaded: " + alizeSystem.isUBMLoaded());
        System.out.println("***********************************************");
        System.out.println();


        // Reset input before sending another signal
        alizeSystem.resetAudio();
        alizeSystem.resetFeatures();


        // "Record" some more audio
        alizeSystem.addAudio(getApplicationContext().getAssets().open("data/PB2M-6_UV.s16"));


        // Perform speaker verification against the model we created earlier
        SimpleSpkDetSystem.SpkRecResult verificationResult = alizeSystem.verifySpeaker("UV");
        System.out.println("Speaker verification against speaker UV:");
        System.out.println("  match: " + verificationResult.match);
        System.out.println("  score: " + verificationResult.score);
        System.out.println("***********************************************");
        System.out.println();


        // Extract a pre-built model that was packed with the application, and load it into the system
        InputStream speakerModelAsset = getApplicationContext().getAssets().open("gmm/AG.gmm");
        alizeSystem.loadSpeakerModel("AG",speakerModelAsset);
        speakerModelAsset.close();

        System.out.println("Status after sending second audio and loading second speaker model:");
        System.out.println("  # of features: " + alizeSystem.featureCount());
        System.out.println("  # of models: " + alizeSystem.speakerCount());
        System.out.println("  UBM loaded: " + alizeSystem.isUBMLoaded());
        System.out.println("***********************************************");
        System.out.println();


        // Now that we have 2 speakers in the system, let's try identification
        // (we don't need to resend the audio signal, since we haven't called resetAudio and resetFeatures)
        SimpleSpkDetSystem.SpkRecResult identificationResult = alizeSystem.identifySpeaker();
        System.out.println("Result of speaker identification:");
        System.out.println("  closest speaker: " + identificationResult.speakerId);
        System.out.println("  match: " + identificationResult.match);
        System.out.println("  score: " + identificationResult.score);
        System.out.println("***********************************************");
        System.out.println();


        // Now that we're done playing with this audio signal, let's not forget to unload it from the system
        // (This will remove some temporary files)
        alizeSystem.resetAudio();
        alizeSystem.resetFeatures();
    }

}
