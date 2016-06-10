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

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.newventuresoftware.waveform.WaveformBassColorView;
import com.newventuresoftware.waveform.WaveformMidColorView;
import com.newventuresoftware.waveform.WaveformTrebleColorView;
import com.newventuresoftware.waveform.WaveformView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends AppCompatActivity {

    private WaveformTrebleColorView mRealtimeWaveformViewTrebleColor;
    private WaveformMidColorView mRealtimeWaveformViewMidColor;
    private WaveformBassColorView mRealtimeWaveformViewBassColor;

    private RecordingThread mRecordingThread;
    private EncodingThread mEncodingThread;
    private static final int REQUEST_RECORD_AUDIO = 13;

    private float[] waveformPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //mRealtimeWaveformView = (WaveformView) findViewById(R.id.waveformView);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        mRealtimeWaveformViewTrebleColor = (WaveformTrebleColorView) findViewById(R.id.waveformTrebleColorView);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        mRealtimeWaveformViewMidColor = (WaveformMidColorView) findViewById(R.id.waveformMidColorView);
        mRealtimeWaveformViewBassColor = (WaveformBassColorView) findViewById(R.id.waveformBassColorView);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        BlockingQueue<short[]> buffer = new ArrayBlockingQueue<short[]>(RecordingThread.SAMPLING_RATE);
        mRecordingThread = new RecordingThread(this, buffer, null);

        mEncodingThread = new EncodingThread(this, buffer, new AudioThreadStatusListener() {
            @Override
            public void onStarted() {
            }

            @Override
            public void onFailure(String reason, Exception error) {
            }

            @Override
            public void onCompleted() {
                onEncodingThreadCompleted();
            }
        }, new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data) {
                mRealtimeWaveformViewTrebleColor.setSamples(data);

                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                mRealtimeWaveformViewMidColor.setSamples(data);
                mRealtimeWaveformViewBassColor.setSamples(data);

                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //mPlaybackView = (WaveformView) findViewById(R.id.playbackWaveformView);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mRecordingThread.isRunning()) {
                    startAudioRecordingSafe();
                    mEncodingThread.start();
                } else {
                    mRecordingThread.stop();
                }
            }
        });
    }

    private void onEncodingThreadCompleted() {
        // load data
        File file = mEncodingThread.getAudioFile();
        byte[] data = null;
        try {
            InputStream is = new FileInputStream(file);
            data = IOUtils.toByteArray(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).asShortBuffer();
        short[] samples = new short[(int)file.length() / 2];
        sb.get(samples);

        // update

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //mPlaybackView.setChannels(1);
        //mPlaybackView.setSampleRate(RecordingThread.SAMPLING_RATE);
        //mPlaybackView.setSamples(samples);

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Override
    protected void onStop() {
        super.onStop();

        mRecordingThread.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startAudioRecordingSafe() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            mRecordingThread.start();
        } else {
            requestMicrophonePermission();
        }
    }

    private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
            // Show dialog explaining why we need record audio
            Snackbar.make(mRealtimeWaveformViewTrebleColor, "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }).show();

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            Snackbar.make(mRealtimeWaveformViewMidColor, "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }).show();

            Snackbar.make(mRealtimeWaveformViewBassColor, "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }).show();


            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mRecordingThread.stop();
        }
    }
}