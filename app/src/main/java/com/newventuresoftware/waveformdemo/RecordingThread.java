/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.newventuresoftware.waveformdemo;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

public class RecordingThread extends AudioThreadBase {
    private static final String LOG_TAG = RecordingThread.class.getSimpleName();
    static final int SAMPLING_RATE = 44100;

    private boolean mShouldContinue;

    public RecordingThread(Context context, BlockingQueue<short[]> data, AudioThreadStatusListener listener) {
        super(context, data, listener);

        mShouldContinue = true;
    }

    @Override
    public void run() {
        Log.v(LOG_TAG, "Start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLING_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        onStarted();

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, bufferSize / 2);
            shortsRead += numberOfShort;

            // Notify waveform
            try {
                if (numberOfShort == bufferSize / 2) {
                    mQueue.put(audioBuffer);
                } else {
                    short[] finalArray = new short[numberOfShort];
                    System.arraycopy(audioBuffer, 0, finalArray, 0, numberOfShort);
                    mQueue.put(finalArray);
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
        }

        record.stop();
        record.release();

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));

        // Set marker bit
        Log.v(LOG_TAG, "Signal queue");
        try {
            mQueue.put(MARKER_BIT);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }

        onCompleted();
    }

    @Override
    public void start() {
        mShouldContinue = true;
        super.start();
    }

    public void stop() {
        mShouldContinue = false;
    }
}
