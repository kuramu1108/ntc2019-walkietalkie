package com.ntc2019.walkie_talkie;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;

import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity {
    private MyViewModel vm;
    private TalkAdapter talkAdapter;

    private static final String LOG_TAG = "AudioRecordTest";
    public static final int RC_RECORD_AUDIO = 1000;

    private WebSocketClient2 client;

    private ImageButton recordBtn;
    private ImageButton deleteTalkBtn;
    private ImageButton messageSendBtn;
    private Switch serverConnectionSwitch;
    private EditText talkerNameTxt;
    private EditText messageTxt;
    private ProgressBar progressBar;
    private RecyclerView talkHistory;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        vm = ViewModelProviders.of(this).get(MyViewModel.class);
        talkAdapter = new TalkAdapter(this);

        vm.sRecordedFileName = getCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
        vm.talkHistory.setValue(new ArrayList<Talk>());

        viewBinding();

        talkHistory.setLayoutManager(new LinearLayoutManager(this));
        talkHistory.setHasFixedSize(true);
        talkHistory.setItemAnimator(new DefaultItemAnimator());
        talkHistory.setAdapter(talkAdapter);

        buttonBinding();

        // Observers

        vm.talkHistory.observe(this, new Observer<List<Talk>>() {
            @Override
            public void onChanged(@Nullable List<Talk> talks) {
                talkAdapter.setData(talks);
                talkHistory.scrollToPosition(talks.size() - 1);
            }
        });

        vm.serverConnection.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean connected) {
                serverConnectionSwitch.setChecked(connected);
                serverConnectionSwitch.setClickable(!connected);
                if (!connected) {
                    Toast.makeText(getApplicationContext(), "伺服器無回應請檢察網路連接", Toast.LENGTH_LONG).show();
                }
            }
        });



        serverConnectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !vm.clientRunning) client.run();
                else if (!isChecked && vm.clientRunning) client.close();
            }
        });

        client = new WebSocketClient2(this, vm);
        client.run();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        client.close();
        super.onDestroy();
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
            client.startRecording();
        } else {
            EasyPermissions.requestPermissions(this, "Hi", RC_RECORD_AUDIO, perms);
        }
    }

    private void stopRecording() {
        client.stopRecording();
    }

    //設定按鈕顏色
    private void setRecordIcon(boolean record) {
        if (!record) {
            recordBtn.setBackground(getResources().getDrawable(R.drawable.round_button_action_record));
        } else {
            recordBtn.setBackground(getResources().getDrawable(R.drawable.round_button_action_recording));
        }
    }

    public void viewBinding() {
        serverConnectionSwitch = findViewById(R.id.switch_server_connection);
        recordBtn = findViewById(R.id.imageButton);
        talkerNameTxt = findViewById(R.id.txt_talker_name);
        progressBar = findViewById(R.id.progressBar);
        talkHistory = findViewById(R.id.talk_view);
        deleteTalkBtn = findViewById(R.id.imgBtn_delete);
        messageSendBtn = findViewById(R.id.imgBtn_send);
        messageTxt = findViewById(R.id.txt_message_box);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void buttonBinding() {
        deleteTalkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vm.talkHistory.setValue(new ArrayList<Talk>());
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        });

        messageSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageTxt.getText().toString();
                if (talkerNameTxt.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "請輸入發話者名字", Toast.LENGTH_SHORT).show();

                } else if (message.equals("")) {
                    Toast.makeText(getApplicationContext(), "訊息欄為空白", Toast.LENGTH_SHORT).show();
                } else {
                    if (vm.yourName.equals("")) vm.yourName = talkerNameTxt.getText().toString();
                    client.sendMessage(messageTxt.getText().toString());
                    messageTxt.setText("");
                    hideKeyBoard();
                }
            }
        });

        recordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(LOG_TAG, "onTouch: " + event.getAction());

                // 按鈕按下時
                if (talkerNameTxt.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "請輸入發話者名字", Toast.LENGTH_SHORT).show();
                    return true;
                }

                if(event.getAction() == MotionEvent.ACTION_DOWN && vm.getServerConnection())
                {
                    // 給予觸覺回饋
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    progressBar.setVisibility(View.VISIBLE);
                    // 設定說話者名稱以及其WIFI位址
                    vm.yourName = talkerNameTxt.getText().toString();
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
                else if (event.getAction() == MotionEvent.ACTION_UP && vm.getServerConnection())
                {
                    // 畫面顯示誰正在說話
                    List<Talk> temp = vm.talkHistory.getValue();
                    temp.add(new Talk(vm.yourName, false));
                    vm.talkHistory.setValue(temp);

                    // change button color
                    setRecordIcon(false);
                    stopRecording();
                    progressBar.setVisibility(View.INVISIBLE);
                }

                else if (event.getAction() == MotionEvent.ACTION_DOWN && !vm.getServerConnection()) {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    Toast.makeText(getApplicationContext(), "伺服器未連接", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });
    }

    public void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = this.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}

