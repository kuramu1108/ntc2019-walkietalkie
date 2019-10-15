/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ntc2019.walkie_talkie;


import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class WebSocketClient extends WebSocketListener {
    private static final String LOG_TAG = "WebSocketClient";
    public static final String START = "start";
    public static final String END = "end";
    public static final String TALKER_PREFIX = "talking:";
    public static final String MESSAGE_PREFIX = "message:";

    private final Context mContext;
    private final MyViewModel vm;
    static List<byte[]> sList = new ArrayList<>();
    WebSocket mSocket;
    private MediaPlayer mPlayer;


    public WebSocketClient(Context c, MyViewModel viewModel) {
        mContext = c;
        vm = viewModel;
    }

    public void run() {
        vm.clientRunning = true;
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("ws://ntc2019-walkietalkie.herokuapp.com/chat")
                .build();
        client.newWebSocket(request, this);

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    public void close() {
        mSocket.close(1000, null);
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onOpen(final WebSocket webSocket, Response response) {
        Log.d(LOG_TAG, "onOpen: ");
        mSocket = webSocket;
        vm.serverConnection.postValue(true);
    }

    public void sendAudio() {
        FileChannel in = null;

        try {
            File f = new File(vm.sRecordedFileName);
            in = new FileInputStream(f).getChannel();

            mSocket.send(START);
            mSocket.send(TALKER_PREFIX + vm.yourName);

            sendAudioBytes(in);

            mSocket.send(END);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendAudioBytes(FileChannel in) throws IOException {
        ByteBuffer buff = ByteBuffer.allocateDirect(32);

        while (in.read(buff) > 0) {
            buff.flip();
            String bytes = ByteString.of(buff).toString();
            mSocket.send(bytes);
            buff.clear();
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        if (text.equals(START)) {
            sList.clear();
        } else if (text.startsWith(TALKER_PREFIX)) {
            vm.talkerName = text.substring(8);
            List<Talk> temp = vm.talkHistory.getValue();
            temp.add(new Talk(vm.talkerName, true));
            vm.talkHistory.postValue(temp);
        } else if (text.startsWith(MESSAGE_PREFIX)) {
            String message = text.substring(MESSAGE_PREFIX.length());
            List<Talk> temp = vm.talkHistory.getValue();
            temp.add(new Talk("", message));
            vm.talkHistory.postValue(temp);
        } else if (text.equals(END)) {
            List<Talk> temp = vm.talkHistory.getValue();
            temp.add(new Talk(vm.talkerName, false));
            vm.talkHistory.postValue(temp);
            playReceivedFile();
        } else {
            try {
                String hexValue = text.substring(text.indexOf("hex=") + 4, text.length() - 1);
                ByteString d = ByteString.decodeHex(hexValue);
                byte[] bytes = d.toByteArray();

                sList.add(bytes);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.d(LOG_TAG, "onMessage: " + bytes.toByteArray());
        sList.add(bytes.toByteArray());
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d(LOG_TAG, "onClosing: " + reason);
//        webSocket.close(1000, null);
        vm.clientRunning = false;
        vm.serverConnection.postValue(false);
//        vm.updateConnectionTrial();
        run();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.e(LOG_TAG, "onFailure: ", t);
        vm.clientRunning = false;
        vm.serverConnection.postValue(true);
        vm.serverConnection.postValue(false);
//        vm.updateConnectionTrial();
//        run();
        t.printStackTrace();
    }

    private void playReceivedFile() {
        File f = buildAudioFileFromReceivedBytes();

        playAudio(f);
    }

    @NonNull
    private File buildAudioFileFromReceivedBytes() {
        File f = new File(mContext.getCacheDir().getAbsolutePath() + "/received.3gp");
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        try {
            out = (new FileOutputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            for (byte[] b : sList) {
                out.write(b);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return f;
    }

    private void playAudio(File f) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mContext, Uri.parse(f.getPath()));
            mPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "onClosing: duration in millis: " + mPlayer.getDuration());

        mPlayer.start();
    }

    public void sendMessage(String s) {
        mSocket.send(MESSAGE_PREFIX + s);
    }
}
