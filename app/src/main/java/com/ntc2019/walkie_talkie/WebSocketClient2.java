package com.ntc2019.walkie_talkie;


import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class WebSocketClient2 extends WebSocketListener {
    private static final String LOG_TAG = "WebSocketClient";
    private static final String START = "start";
    private static final String END = "end";

    private final Context mContext;
    private final MyViewModel vm;
    private WebSocket mSocket;

    private Thread recordingThread = null;
    private AudioRecord recorder = null;
    private AudioTrack track = null;
    private byte buffer[] = null;
    private byte playBuffer[] = null;
    int minSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private int bufferSize = minSize;
    private boolean isRecording = false;



    public WebSocketClient2(Context c, MyViewModel viewModel) {
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

        audioCreate();
    }

    public void close() {
        mSocket.close(1000, null);
    }

    @Override
    public void onOpen(final WebSocket webSocket, Response response) {
        Log.d(LOG_TAG, "onOpen: ");
        mSocket = webSocket;
        vm.serverConnection.postValue(true);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        if (text.equals(START)) {
            playBuffer = new byte[minSize];
            track.play();
            List<Talk> temp = vm.talkHistory.getValue();
            temp.add(new Talk(vm.talkerName, true));
            vm.talkHistory.postValue(temp);
        } else if (text.equals(END)) {
            List<Talk> temp = vm.talkHistory.getValue();
            temp.add(new Talk(vm.talkerName, false));
            vm.talkHistory.postValue(temp);
            if (track != null) {
                isRecording = true;
                track.stop();
            }
        } else {
            // receiving hex strings
            try {
                ByteString d = ByteString.decodeHex(text);
                byte[] bytes = d.toByteArray();

                track.write(bytes, 0, bytes.length);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.d(LOG_TAG, "onMessage: " + bytes.toByteArray());
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d(LOG_TAG, "onClosing: " + reason);
        vm.serverConnection.postValue(false);
        vm.clientRunning = false;
//        run();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.e(LOG_TAG, "onFailure: ", t);
        vm.serverConnection.postValue(false);
        vm.clientRunning = false;
        t.printStackTrace();
    }

    public void audioCreate() {
        // Audio track object
        track = new AudioTrack(AudioManager.STREAM_MUSIC,
                16000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
        // Audio record object
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
    }

    public void startRecording() {
        Log.d("AUDIO", "Assigning recorder");
        buffer = new byte[bufferSize];
        mSocket.send(START);
        recorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                sendRecording();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    public void sendRecording() {
        while (isRecording) {
            try {
                Log.d("AUDIO", "recording thread");
                recorder.read(buffer, 0, bufferSize);
                ByteString bs = ByteString.of(buffer, 0, bufferSize);

                mSocket.send(bs.hex());
            } catch (Exception e) {
                Log.e("SOCKET", "Error when sending recording", e);
            }
        }
    }

    public void stopRecording() {
        mSocket.send(END);
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
        }
    }
}
