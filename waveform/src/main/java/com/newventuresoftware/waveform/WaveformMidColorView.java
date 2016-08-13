package com.newventuresoftware.waveform;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.newventuresoftware.waveform.utils.*;

import java.util.LinkedList;

import be.tarsos.dsp.util.fft.FFT;
/**
 * TODO: document your custom view class.
 */
public class WaveformMidColorView extends WaveformView {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private float value, hue, previousHue, averageVolume;

    private float[] mYPoints; //, mWaveformPoints

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WaveformMidColorView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WaveformMidColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WaveformMidColorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    void drawRecordingWaveform(short[] buffer, float[] waveformPoints) {    //difference between these arguments??? CRUCIAL!!!!!!

        int width = super.width;
        float centerY = super.centerY;

        mYPoints = super.mYPoints;

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        for(int i=0; i < 4*width; i+=2)     //making the line flat
            waveformPoints[i] = (i+2)/4;

        for(int i=1; i < 4*width; i+=2)     //making the line flat
            waveformPoints[i] = centerY;

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //PORTRAIT

        //500-2400Hz <===== Consider shifting lower range down (450/550) && upper range up (2600...?)

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        if(super.isPortrait) {

            float numerator = 0f;
            float denominator = 0.0001f;

            for(int i=13; i < 4*width; i+=12) {  //20: 2600Hz (Treble shift: 6*width/53) && 32: 2000Hz
                waveformPoints[i] = -Math.abs(mYPoints[i / 24 + width / 21]) + centerY;     //DOUBLING UP (to make fuller)

                numerator += i*(-waveformPoints[i]+centerY)*(-waveformPoints[i-12]+centerY);
                denominator += (-waveformPoints[i]+centerY)*(-waveformPoints[i-12]+centerY);
            }

            if(hue > 0)
                previousHue = hue;

            float frequency = numerator/denominator;
            frequency /= 4*width;
            hue = 320*frequency;    //128* && +96

            float deltaHue = hue - previousHue;       //making color transitions more smooth
            float ratioHue = Math.max(hue, previousHue) / Math.max(hue, previousHue);
            deltaHue /= ratioHue;
            hue = previousHue + deltaHue;

            averageVolume += denominator;
            averageVolume /= 2;

            if(averageVolume < 200f)       //THESE VALUES ARE (ON AVERAGE) LOWER WITHIN PORTRAIT
                denominator /= 100f;
            else if(averageVolume < 500f)
                denominator /= 250f;
            else if(averageVolume < 1000f)
                denominator /= 500f;
            else if(averageVolume < 5000f)
                denominator /= 2500f;
            else if(averageVolume < 10000f)
                denominator /= 5000f;
            else if(averageVolume < 20000f)
                denominator /= 10000f;
            else if(averageVolume < 50000f)
                denominator /= 25000f;
            else if(averageVolume < 100000f)
                denominator /= 50000f;
            else if(averageVolume < 200000f)
                denominator /= 100000f;
            else if(averageVolume < 500000f)
                denominator /= 250000f;
            else if(averageVolume < 1000000f)
                denominator /= 500000f;
            else
                denominator /= 1000000f;

            //denominator /= 30000f;
            value = (float) -Math.pow((denominator+1),-1)+1;

            super.hue = hue;
            super.value = value;

        } else {

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //LANDSCAPE

            //600-2600Hz  <===== Consider shifting lower range up (550/600) && upper range up (2600...?)

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            float numerator = 0f;
            float denominator = 0.0001f;

            for(int i=25; i < 4*width; i+=24) {  //20: 2600Hz (Treble shift: 6*width/53) && 32: 2000Hz
                waveformPoints[i] = -Math.abs(mYPoints[i / 48 + width / 38]) + centerY;     //DOUBLING UP (to make fuller)

                numerator += i*(-waveformPoints[i]+centerY)*(-waveformPoints[i-24]+centerY);
                denominator += (-waveformPoints[i]+centerY)*(-waveformPoints[i-24]+centerY);
            }

            if(hue > 0)
                previousHue = hue;

            float frequency = numerator/denominator;
            frequency /= 4*width;
            hue = 320*frequency;    //128* && +96

            float deltaHue = hue - previousHue;       //making color transitions more smooth
            float ratioHue = Math.max(hue, previousHue) / Math.max(hue, previousHue);
            deltaHue /= ratioHue;
            hue = previousHue + deltaHue;

            averageVolume += denominator;
            averageVolume /= 2;

            if(averageVolume < 500f)     //adjusting value for volume of music
                denominator /= 250f;
            else if(averageVolume < 1000f)
                denominator /= 500f;
            else if(averageVolume < 5000f)
                denominator /= 2500f;
            else if(averageVolume < 10000f)
                denominator /= 5000f;
            else if(averageVolume < 20000f)
                denominator /= 10000f;
            else if(averageVolume < 50000f)
                denominator /= 25000f;
            else if(averageVolume < 100000f)
                denominator /= 50000f;
            else if(averageVolume < 200000f)
                denominator /= 100000f;
            else if(averageVolume < 500000f)
                denominator /= 250000f;
            else if(averageVolume < 1000000f)
                denominator /= 500000f;
            else
                denominator /= 1000000f;

            //denominator /= 30000f;
            value = (float) -Math.pow((denominator+1),-1)+1;

            super.hue = hue;
            super.value = value;

        }

    }

}