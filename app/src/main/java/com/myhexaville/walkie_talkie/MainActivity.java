package com.myhexaville.walkie_talkie;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.myhexaville.walkie_talkie.databinding.ActivityMainBinding;

import java.io.IOException;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity {
    private MyViewModel vm;

    private static final String LOG_TAG = "AudioRecordTest";
    public static final int RC_RECORD_AUDIO = 1000;

    private MediaRecorder mRecorder;
    private ActivityMainBinding mBinding;
    private WebSocketClient client;
    private ImageButton recordBtn;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        vm = ViewModelProviders.of(this).get(MyViewModel.class);

        vm.sRecordedFileName = getCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";

        recordBtn = findViewById(R.id.imageButton);

        recordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(LOG_TAG, "onTouch: " + event.getAction());
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    setRecordIcon(true);
                    startRecording();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    setRecordIcon(false);
                    stopRecording();
                    send();
                }
                return true;
            }
        });

        client = new WebSocketClient(this, vm);
        client.run();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @AfterPermissionGranted(RC_RECORD_AUDIO)
    private void startRecording() {
        String[] perms = {Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setOutputFile(vm.sRecordedFileName);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed");
            }

            mRecorder.start();
        } else {
            EasyPermissions.requestPermissions(this, "Hi", RC_RECORD_AUDIO, perms);
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
    }

    private void setRecordIcon(boolean record) {
//        if (record) {
//            recordBtn
//                    .setBackground(
//                            VectorDrawableCompat.create(getResources(), R.drawable.recording, getTheme()));
//        } else {
//            recordBtn
//                    .setBackground(
//                            VectorDrawableCompat.create(getResources(), R.drawable.standby, getTheme()));
//        }
    }

    public void send() {
        client.sendAudio();
    }

}

