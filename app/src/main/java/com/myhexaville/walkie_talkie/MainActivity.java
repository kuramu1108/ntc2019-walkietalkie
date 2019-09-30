package com.myhexaville.walkie_talkie;

import android.Manifest;
import android.annotation.SuppressLint;
import android.databinding.DataBindingUtil;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    public static final int RC_RECORD_AUDIO = 1000;
    public static String sRecordedFileName;

    private MediaRecorder mRecorder;
    private WebSocketClient client;
    private Person person = new Person();

    private ImageButton recordBtn;
    private EditText et_name;
    private TextView testview_whosecall;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        sRecordedFileName = getCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";

        recordBtn          = findViewById(R.id.imageButton);
        et_name            = (EditText) findViewById(R.id.editText);
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
                    person.setName(et_name);
                    // 取得說話者的名稱及WIFI位址
                    String talker_name = person.getName();
                    // 畫面顯示誰正在說話
                    String text = "<font color='blue'>" + talker_name + " is talking..." + "</font><br>";
                    testview_whosecall.append(Html.fromHtml(text));

                    // change button color
                    setRecordIcon(true);
                    startRecording();
                }

                // 按鈕放開時
                if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    // 取得說話者的名稱及WIFI位址
                    String talker_name = person.getName();
                    // 畫面顯示誰正在說話
                    String text = "<font color='red'>" + talker_name + " stop talking..." + "</font><br>";
                    testview_whosecall.append(Html.fromHtml(text));

                    // change button color
                    setRecordIcon(false);
                    stopRecording();
                    send();
                }

                return true;
            }
        });

        client = new WebSocketClient(this);
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
            mRecorder.setOutputFile(sRecordedFileName);
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
//        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
    }

    //設定按鈕顏色
    private void setRecordIcon(boolean record) {
        if (record) {
            recordBtn.setBackground(getResources().getDrawable(R.drawable.round_button_action_down));
        } else {
            recordBtn.setBackground(getResources().getDrawable(R.drawable.round_button_action_up));
        }
    }

    //傳送音檔
    public void send() {
        client.sendAudio();
    }


}

