package com.myhexaville.walkie_talkie;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

public class MyViewModel extends ViewModel {
    public String sRecordedFileName = "";
    public String yourName = "";
    public String talkerName = "";
    public Boolean clientRunning = false;

    public MutableLiveData<Boolean> serverConnection = new MutableLiveData<>();

    public MutableLiveData<List<Talk>> talkHistory = new MutableLiveData<>();

    public String getLastTalkString() {
        if (talkHistory.getValue() != null) {
            int size = talkHistory.getValue().size();
            if (size > 0) return talkHistory.getValue().get(size-1).getHTMLString();
            else return "<text></text>";
        }
        else return "<text></text>";
    }

    public Boolean getServerConnection() {
        if (serverConnection.getValue() != null) return serverConnection.getValue();
        else {
            serverConnection.setValue(false);
            return false;
        }
    }
}
