package com.newventuresoftware.waveformdemo;

public interface AudioThreadStatusListener {
    void onStarted();
    void onFailure(String reason, Exception error);
    void onCompleted();
}
