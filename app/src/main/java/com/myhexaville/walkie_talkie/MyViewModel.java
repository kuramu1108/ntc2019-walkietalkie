package com.myhexaville.walkie_talkie;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class MyViewModel extends ViewModel {
    public String sRecordedFileName = "";
    public String talkerName = "Shit";
    public String senderName = "";
    public Boolean clientRunning = false;

    public MutableLiveData<Boolean> serverConnection = new MutableLiveData<Boolean>();
}
