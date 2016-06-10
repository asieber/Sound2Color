package com.newventuresoftware.waveformdemo;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class EncodingThread extends AudioThreadBase {
    private final String LOG_TAG = EncodingThread.class.getSimpleName();

    private File mFile;
    private AudioDataReceivedListener mAudioDataReceivedListener;

    public EncodingThread(Context context, BlockingQueue<short[]> data,
                          AudioThreadStatusListener listener, AudioDataReceivedListener audioDataReceivedListener) {
        super(context, data, listener);
        mAudioDataReceivedListener = audioDataReceivedListener;
    }

    @Override
    public void run() {
        Log.v(LOG_TAG, "Start");

        mFile = getAudioFile();
        if (mFile == null) {
            Log.e(LOG_TAG, "Could not create file!");
            onFailure("Could not create file!");
            return;
        }

        DataOutputStream dataStream;
        try {
            dataStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mFile)));
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            onFailure("Could not create file!", e);
            return;
        }

        onStarted();

        Log.v(LOG_TAG, "Start receiving audio data from RecordingThread");

        // Pull data from audio thread
        try {
            short[] item;
            while (!Arrays.equals((item = mQueue.take()), MARKER_BIT)) {
                for (short s : item) {
                    dataStream.writeShort(s);
                }

                if (mAudioDataReceivedListener != null)
                    mAudioDataReceivedListener.onAudioDataReceived(item);
            }
        } catch (InterruptedException | IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }

        Log.v(LOG_TAG, "All audio data from RecordingThread received");

        try {
            dataStream.flush();
            dataStream.close();
            Log.v(LOG_TAG, "Audio file saved to disk");
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            onFailure("Could not save file", e);
        }

        onCompleted();
    }

    public File getAudioFile() {
        final String fileName = "audioRecord.pcm";
        final String folderName = "recordings";

        // Get directory
        File recordingsDir = null;
        if (isExternalStorageWritable()) {
            recordingsDir = Environment.getExternalStoragePublicDirectory(folderName);
        }
        if (recordingsDir == null) {
            recordingsDir = new File(getContext().getFilesDir(), folderName);
        }
        if (!recordingsDir.exists()) {
            if (!recordingsDir.mkdirs()) {
                return null;
            }
            if (recordingsDir.getUsableSpace() < (RecordingThread.SAMPLING_RATE * 2 * 30)) {
                // Recordings dir doesn't have enough space to store 30 seconds of audio
                return null;
            }
        }

        // Get file
        File recordingFile = new File(recordingsDir, fileName);
        try {
            if (recordingFile.createNewFile()) {
                Log.v(LOG_TAG, "File " + recordingFile + " created");
            } else {
                Log.v(LOG_TAG, "File " + recordingFile + " already created. Will overwrite");
            }
        } catch (IOException e) {
            return null;
        }
        return recordingFile;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
