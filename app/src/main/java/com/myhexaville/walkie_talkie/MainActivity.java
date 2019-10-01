package com.myhexaville.walkie_talkie;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;

import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity {
    private MyViewModel vm;

    private static final String LOG_TAG = "AudioRecordTest";
    public static final int RC_RECORD_AUDIO = 1000;

    private MediaRecorder mRecorder;
    private WebSocketClient client;

    private ImageButton recordBtn;
    private Switch serverConnectionSwitch;
    private EditText et_name;
    private TextView testview_whosecall;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        vm = ViewModelProviders.of(this).get(MyViewModel.class);

        vm.sRecordedFileName = getCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
        vm.talkHistory.setValue(new ArrayList<Talk>());

        serverConnectionSwitch = findViewById(R.id.switch_server_connection);
        recordBtn = findViewById(R.id.imageButton);
        et_name = (EditText) findViewById(R.id.editText);
        testview_whosecall = (TextView) findViewById(R.id.textView_whosecall);
        testview_whosecall.setMovementMethod(new ScrollingMovementMethod());

        recordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(LOG_TAG, "onTouch: " + event.getAction());

                // 按鈕按下時
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    // 給予觸覺回饋
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    // 設定說話者名稱以及其WIFI位址
                    vm.yourName = et_name.getText().toString();
                    // 取得說話者的名稱及WIFI位址
                    // 畫面顯示誰正在說話
                    List<Talk> temp = vm.talkHistory.getValue();
                    temp.add(new Talk(vm.yourName, true));
                    vm.talkHistory.setValue(temp);

                    // change button color
                    setRecordIcon(true);
                    startRecording();
                }

                // 按鈕放開時
                if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    // 畫面顯示誰正在說話
                    List<Talk> temp = vm.talkHistory.getValue();
                    temp.add(new Talk(vm.yourName, false));
                    vm.talkHistory.setValue(temp);

                    // change button color
                    setRecordIcon(false);
                    stopRecording();
                    send();
                }

                return true;
            }
        });

        vm.talkHistory.observe(this, new Observer<List<Talk>>() {
            @Override
            public void onChanged(@Nullable List<Talk> talks) {
                testview_whosecall.append(Html.fromHtml(vm.getLastTalkString()));
            }
        });

        vm.serverConnection.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                serverConnectionSwitch.setChecked(aBoolean);
            }
        });

        serverConnectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !vm.clientRunning) client.run();
                else if (!isChecked && vm.clientRunning) client.close();
            }
        });

        client = new WebSocketClient(this, vm);
        client.run();
    }

    @Override
    public void onStart() {
        super.onStart();
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

    //設定按鈕顏色
    private void setRecordIcon(boolean record) {
        if (!record) {
            recordBtn.setBackground(getResources().getDrawable(R.drawable.round_button_action_record));
        } else {
            recordBtn.setBackground(getResources().getDrawable(R.drawable.round_button_action_recording));
        }
    }

    //傳送音檔
    public void send() {
        client.sendAudio();
    }


}

