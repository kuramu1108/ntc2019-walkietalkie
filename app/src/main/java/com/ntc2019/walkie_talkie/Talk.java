package com.ntc2019.walkie_talkie;

public class Talk {
    private String speakerName;
    private Boolean start;
    private String message;
    private Boolean isMessage;

    public Talk(String speakerName, Boolean start) {
        this.speakerName = speakerName;
        this.start = start;
        this.isMessage = false;
    }

    public Talk(String speakerName, String message) {
        this.speakerName = speakerName;
        this.message = message;
        this.start = false;
        this.isMessage = true;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsMessage() {
        return isMessage;
    }

    public Boolean getStart() {
        return start;
    }

    public void setStart(Boolean start) {
        this.start = start;
    }
}
