package com.ntc2019.walkie_talkie;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

public class MyViewModel extends ViewModel {
    public String sRecordedFileName = "";
    public String yourName = "";
    public String talkerName = "";
    public Boolean clientRunning = false;

    public MutableLiveData<Integer> connectionTrial = new MutableLiveData<>();

    public MutableLiveData<Boolean> serverConnection = new MutableLiveData<>();

    public MutableLiveData<List<Talk>> talkHistory = new MutableLiveData<>();

    public void updateConnectionTrial() {
        if (connectionTrial.getValue() != null) {
            int temp = connectionTrial.getValue();
            connectionTrial.postValue(temp + 1);
        } else connectionTrial.postValue(0);
    }

    public Boolean getServerConnection() {
        if (serverConnection.getValue() != null) return serverConnection.getValue();
        else {
            serverConnection.setValue(false);
            return false;
        }
    }
}
