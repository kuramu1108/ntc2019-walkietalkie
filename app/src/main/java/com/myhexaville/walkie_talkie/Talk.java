package com.myhexaville.walkie_talkie;

public class Talk {
    private String speakerName;
    private Boolean start;

    public Talk(String speakerName, Boolean start) {
        this.speakerName = speakerName;
        this.start = start;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public Boolean getStart() {
        return start;
    }

    public void setStart(Boolean start) {
        this.start = start;
    }

    public String getHTMLString() {
        if (start) return "<font color='blue'>" + speakerName + " is talking..." + "</font><br>";
        else return "<font color='red'>" + speakerName + " stop talking..." + "</font><br>";
    }
}
