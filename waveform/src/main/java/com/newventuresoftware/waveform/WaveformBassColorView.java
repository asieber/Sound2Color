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
public class WaveformBassColorView extends WaveformView {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private float value, hue, previousHue, averageVolume;

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WaveformBassColorView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WaveformBassColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WaveformBassColorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    void drawRecordingWaveform(short[] buffer, float[] waveformPoints) {    //difference between these arguments??? CRUCIAL!!!!!!

        int width = super.width;
        float centerY = super.centerY;

        float lastX = -1;
        float lastY = -1;
        int pointIndex = 0;
        float max = Short.MAX_VALUE;     //key indicator for above predicament..?

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        float[] yPoints = new float[2*width];

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // For efficiency, we don't draw all of the samples in the buffer, but only the ones
        // that align with pixel boundaries.
        for (int x = 0; x < width; x++) {    //x just increments continuously & constantly

            int index = (int) (((x * 1.0f) / width) * buffer.length);   //index proportional to x but on different (buffer) scale
            short sample = buffer[index];     //buffer == mSamples (only in above method (onSamplesChanged)
            float y = centerY - ((sample / max) * centerY);

            //FLOATS JUST CONVERTING SHORTS INTO USEFUL "SCREEN-ADJUSTED" DATA POINTS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            if (lastX != -1) {
                waveformPoints[pointIndex++] = lastX;

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                yPoints[pointIndex/2] = -(lastY-centerY)/centerY;

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                waveformPoints[pointIndex++] = lastY;
                waveformPoints[pointIndex++] = x;

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                yPoints[pointIndex/2] = -(y-centerY)/centerY;

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                waveformPoints[pointIndex++] = y;
            }
            lastX = x;
            lastY = y;
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        FFT fft = new FFT(2*width, null);     //[100, 2*width] == "safe" range ....... null window == BUGGY, cos == GOOD
        fft.forwardTransform(yPoints);

        super.mYPoints = yPoints;      //FINAL PIECE OF THE PUZZLE

        for(int i=1; i < 4*width; i+=2)     //making the line flat
            waveformPoints[i] = centerY;


        if(super.isPortrait) {         //NO IDEA ABOUT THE LOWER RANGE ON THIS ONE (CRITICAL KNOWLEDGE TO CONTINUE)

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //PORTRAIT

            //80?-500Hz?????? <===== Possibly raise upper bound to 300/350

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            float numerator = 0f;
            float denominator = 0.0001f;

            for(int i=45; i < 4*width; i+=44) {                     //2*width: cuts off right half (all freqs above 350Hz)
                waveformPoints[i] = -Math.abs(yPoints[i / 220 + width / 250]) + centerY;      //QUINTUPLED UP (to make fuller)

                numerator += i*(-waveformPoints[i]+centerY)*(-waveformPoints[i-44]+centerY);
                denominator += (-waveformPoints[i]+centerY)*(-waveformPoints[i-44]+centerY);
            }

            if(hue > 0)
                previousHue = hue;

            float frequency = numerator/denominator;
            frequency /= 4*width;
            hue = 320*frequency;    ///128* && +0

            float deltaHue = hue - previousHue;      //making color transitions more smooth
            float ratioHue = Math.max(hue, previousHue) / Math.max(hue, previousHue);
            deltaHue /= ratioHue;
            hue = previousHue + deltaHue;

            averageVolume = super.averageVolume;

            averageVolume += denominator;
            averageVolume /= 2;

            if(averageVolume < 500f)     //adjusting value for volume of music
                denominator /= 400f;
            else if(averageVolume < 1000f)
                denominator /= 800f;
            else if(averageVolume < 5000f)
                denominator /= 4000f;
            else if(averageVolume < 10000f)
                denominator /= 8000f;
            else if(averageVolume < 20000f)
                denominator /= 16000f;
            else if(averageVolume < 50000f)
                denominator /= 40000f;
            else if(averageVolume < 100000f)
                denominator /= 80000f;
            else if(averageVolume < 200000f)
                denominator /= 160000f;
            else if(averageVolume < 500000f)
                denominator /= 400000f;
            else if(averageVolume < 1000000f)
                denominator /= 800000f;
            else
                denominator /= 1000000f;

            //denominator /= 30000f;
            value = (float) -Math.pow((denominator+1),-1)+1;

            super.hue = hue;
            super.value = value;

        } else {

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //PORTRAIT

            //75-260Hz <===== Possibly lower upper bound to 300/400Hz (eliminate this range)

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            float numerator = 0f;
            float denominator = 0.0001f;

            for(int i=87; i < 4*width; i+=86) {                     //2*width: cuts off right half (all freqs above 350Hz)
                waveformPoints[i] = -Math.abs(yPoints[i / 430 + width / 400]) + centerY;      //QUINTUPLED UP (to make fuller)

                numerator += i*(-waveformPoints[i]+centerY)*(-waveformPoints[i-86]+centerY);
                denominator += (-waveformPoints[i]+centerY)*(-waveformPoints[i-86]+centerY);
            }

            if(hue > 0)
                previousHue = hue;

            float frequency = numerator/denominator;
            frequency /= 4*width;
            hue = 320*frequency;    ///128* && +0

            float deltaHue = hue - previousHue;      //making color transitions more smooth
            float ratioHue = Math.max(hue, previousHue) / Math.max(hue, previousHue);
            deltaHue /= ratioHue;
            hue = previousHue + deltaHue;

            averageVolume += denominator;
            averageVolume /= 2;

            if(averageVolume < 500f)     //adjusting value for volume of music
                denominator /= 400f;
            else if(averageVolume < 1000f)
                denominator /= 800f;
            else if(averageVolume < 5000f)
                denominator /= 4000f;
            else if(averageVolume < 10000f)
                denominator /= 8000f;
            else if(averageVolume < 20000f)
                denominator /= 16000f;
            else if(averageVolume < 50000f)
                denominator /= 40000f;
            else if(averageVolume < 100000f)
                denominator /= 80000f;
            else if(averageVolume < 200000f)
                denominator /= 160000f;
            else if(averageVolume < 500000f)
                denominator /= 400000f;
            else if(averageVolume < 1000000f)
                denominator /= 800000f;
            else
                denominator /= 1000000f;

            //denominator /= 30000f;
            value = (float) -Math.pow((denominator+1),-1)+1;

            super.hue = hue;
            super.value = value;

        }

    }

}