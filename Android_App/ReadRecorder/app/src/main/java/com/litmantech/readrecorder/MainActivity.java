package com.litmantech.readrecorder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.litmantech.readrecorder.audio.Playback;
import com.litmantech.readrecorder.audio.Recorder;
import com.litmantech.readrecorder.fileexplore.FileBrowser;
import com.litmantech.readrecorder.read.NewSessionDialog;
import com.litmantech.readrecorder.read.Session;
import com.litmantech.readrecorder.read.SessionExistsException;
import com.litmantech.readrecorder.read.line.Entry;
import com.litmantech.readrecorder.utilities.UiUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Recorder recorder;
    private Playback playback;
    private boolean testAudioStuffON = false;
    private Button prevBTN;
    private Button nextBTN;
    private Button recBTN;
    private Session session;
    private TextView currentSentenceTXT;
    private EditText sessionDirNameETXT;
    private Button openSessionBTN;
    private Button newSessionBTN;
    private TextView currentSessionLabelTXT;
    private FileBrowser fileBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//dont let phone sleep

        if (ContextCompat.checkSelfPermission(this,  Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {


        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                55);

        currentSentenceTXT = (TextView) findViewById(R.id.current_sentence);
        currentSessionLabelTXT = (TextView) findViewById(R.id.session_dir_label);
        prevBTN = (Button) findViewById(R.id.prev_btn);
        recBTN  = (Button) findViewById(R.id.rec_btn);
        nextBTN = (Button) findViewById(R.id.next_btn);
        newSessionBTN = (Button) findViewById(R.id.new_session_btn);
        openSessionBTN = (Button) findViewById(R.id.open_session_btn);

        ListView listView = (ListView) findViewById(R.id.listview);




        prevBTN.setOnClickListener(this);
        recBTN.setOnClickListener(this);
        nextBTN.setOnClickListener(this);
        newSessionBTN.setOnClickListener(this);
        openSessionBTN.setOnClickListener(this);


        String[] mTestArray = getResources().getStringArray(R.array.testArray);
        String sessionDirName = Session.DEFAULT_DIR_NAME;
        try {
            session = new Session(this,sessionDirName,mTestArray);
        } catch (SessionExistsException e) {
            File alreadyExistingSession = e.getSession();

            try {
                session = new Session(this,alreadyExistingSession);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }
        UpdateUI();
    }


    @Override
    protected void onResume(){
        super.onResume();
        if(testAudioStuffON)
            RunTest();

        if(recorder == null)
            recorder = new Recorder();

        recorder.Start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(testAudioStuffON){
            playback.Stop();
        }
        recorder.Stop();
    }

    private void RunTest() {

        if(playback == null)
            playback = new Playback();


        LinkedBlockingQueue audioListener = playback.Play();

        recorder.setAudioListener(audioListener);

    }


    @Override
    public void onClick(View v) {
        if(v == prevBTN){
            session.PreviousEntry();
        }else if(v == nextBTN){
            session.NextEntry();
        }else if(v == recBTN){
            if(session.isRecording())
                session.StopRecording();
            else
                session.StartRecording(recorder);
        }else if(v == newSessionBTN){
            ShowNewSessionDialog();

        }else if(v == openSessionBTN){

        }


        UpdateUI();
    }

    private void ShowNewSessionDialog() {
        File lastGoodSessionDir = null;
        if(session != null)
            lastGoodSessionDir = session.getSessionDir();

        //it is safe and ok to pass in null for lastGoodSessionDir
        final NewSessionDialog sessionDialog = new NewSessionDialog(this,lastGoodSessionDir);
        sessionDialog.setOnFileSelectedListener(new NewSessionDialog.OnFileSelectedListener() {
            /**
             * onNewSession will dismiss and close the dialog box
             * @param session_
             */
            @Override
            public void onNewSession(Session session_) {
                session = session_;
                sessionDialog.setOnFileSelectedListener(null);
                UpdateUI();
            }

            @Override
            public void onDismissed() {
                sessionDialog.setOnFileSelectedListener(null);
                UpdateUI();
            }
        });

    }

    /**
     * ONLY call this on the UI thread.!!!!
     */
    private void UpdateUI() {
        Entry currentEntry = session.getCurrentEntry();
        if(session.isRecording()){
            prevBTN.setEnabled(false);
            nextBTN.setEnabled(false);
        }else{
            prevBTN.setEnabled(true);
            nextBTN.setEnabled(true);
        }

        currentSentenceTXT.setText(currentEntry.getSentence());
        currentSessionLabelTXT.setText("Session Name:"+session.getName());

    }
}
