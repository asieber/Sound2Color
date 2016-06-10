package com.newventuresoftware.waveformdemo;

import android.content.Context;
import android.os.Handler;

import java.util.concurrent.BlockingQueue;

abstract class AudioThreadBase implements Runnable {
    protected static final short[] MARKER_BIT = new short[]{-1};

    final BlockingQueue<short[]> mQueue;
    private AudioThreadStatusListener mListener;
    private Context mContext;
    private Handler mHandler;
    private Thread mThread;
    private boolean mRunning;

    public AudioThreadBase(Context context, BlockingQueue<short[]> data, AudioThreadStatusListener listener) {
        mContext = context;
        mQueue = data;
        mListener = listener;
        mHandler = new Handler(context.getMainLooper());
    }

    Context getContext() {
        return mContext;
    }

    public void start() {
        mThread = new Thread(this);
        mThread.start();
    }

    public boolean isRunning() {
        return mRunning;
    }

    protected void onStarted() {
        mRunning = true;
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onStarted();
                }
            });
        }
    }

    protected void onFailure(final String reason, final Exception error) {
        mRunning = false;
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFailure(reason, error);
                }
            });
        }
    }

    protected void onFailure(String reason) {
        onFailure(reason, null);
    }

    protected void onCompleted() {
        mRunning = false;
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onCompleted();
                }
            });
        }
    }
}
