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
public class WaveformBassColorView extends View {

    public static final int MODE_RECORDING = 1;
    public static final int MODE_PLAYBACK = 2;

    private static final int HISTORY_SIZE = 6;

    private TextPaint mTextPaint;
    private Paint mStrokePaint, mFillPaint, mMarkerPaint;

    // Used in draw
    private int brightness;
    private Rect drawRect;

    private int width, height;
    private float xStep, centerY;
    private int mMode, mAudioLength, mMarkerPosition, mSampleRate, mChannels;
    private short[] mSamples;
    private LinkedList<float[]> mHistoricalData;
    private Picture mCachedWaveform;
    private Bitmap mCachedWaveformBitmap;
    private int colorDelta = 255 / (HISTORY_SIZE + 1);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private float value, hue, previousHue, averageVolume;

    private float[] mYPoints;   //, mWaveformPoints

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

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.WaveformView, defStyle, 0);

        mMode = a.getInt(R.styleable.WaveformView_mode, MODE_PLAYBACK);

        float strokeThickness = a.getFloat(R.styleable.WaveformView_waveformStrokeThickness, 1f);
        int mStrokeColor = a.getColor(R.styleable.WaveformView_waveformColor,
                ContextCompat.getColor(context, R.color.default_waveform));
        int mFillColor = a.getColor(R.styleable.WaveformView_waveformFillColor,
                ContextCompat.getColor(context, R.color.default_waveformFill));
        int mMarkerColor = a.getColor(R.styleable.WaveformView_playbackIndicatorColor,
                ContextCompat.getColor(context, R.color.default_playback_indicator));
        int mTextColor = a.getColor(R.styleable.WaveformView_timecodeColor,
                ContextCompat.getColor(context, R.color.default_timecode));

        a.recycle();

        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(TextUtils.getFontSize(getContext(),
                android.R.attr.textAppearanceSmall));

        mStrokePaint = new Paint();
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(strokeThickness);
        mStrokePaint.setAntiAlias(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setAntiAlias(true);
        mFillPaint.setColor(mFillColor);

        mMarkerPaint = new Paint();
        mMarkerPaint.setStyle(Paint.Style.STROKE);
        mMarkerPaint.setStrokeWidth(0);
        mMarkerPaint.setAntiAlias(true);
        mMarkerPaint.setColor(mMarkerColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {    //good reference method.. not important though
        super.onSizeChanged(w, h, oldw, oldh);

        width = getMeasuredWidth();      //just drawing the layout of the "app screen"
        height = getMeasuredHeight();
        xStep = width / (mAudioLength * 1.0f);
        centerY = height / 2f;
        drawRect = new Rect(0, 0, width, height);

        if (mHistoricalData != null) {
            mHistoricalData.clear();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {     //This is where ACTUAL wave (points) is being drawn onto screen (I believe): TEST
        super.onDraw(canvas);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        float[] hsv = {hue,1,value};
        mStrokePaint.setColor(Color.HSVToColor(hsv));

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        LinkedList<float[]> temp = mHistoricalData;      //what's this all about? find out from "TIP" below...
        if (mMode == MODE_RECORDING && temp != null) {
            brightness = colorDelta;
            for (float[] p : temp) {

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                hsv = new float[] {hue,1,value};
                mStrokePaint.setColor(Color.HSVToColor(hsv));

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                
                mStrokePaint.setAlpha(brightness);
                canvas.drawLines(p, mStrokePaint);       //WHAT THE HELL IS GOING ON HERE??? ===> INSPECT API: canvas.drawLines()
                brightness += colorDelta;
            }
        }
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int mMode) {
        mMode = mMode;
    }

    public short[] getSamples() {
        return mSamples;
    }

    public void setSamples(short[] samples) {
        mSamples = samples;
        calculateAudioLength();
        onSamplesChanged();        //actually calling this method... could be important... probably not..
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setYPoints(float[] yPoints) {
        mYPoints = yPoints;
    }

    public float[] getYPoints() {
        return mYPoints;
    }

    /*
    public float[] getWaveformPoints() {
        return mWaveformPoints;
    }
    */

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public int getMarkerPosition() {
        return mMarkerPosition;
    }

    public void setMarkerPosition(int markerPosition) {     //......?
        mMarkerPosition = markerPosition;
        postInvalidate();
    }

    public int getAudioLength() {
        return mAudioLength;
    }                                     //WHAT DOES THIS REPRESENT??? (IMPORTANT ...TO ME..)

    public int getSampleRate() {
        return mSampleRate;
    }

    public void setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
        calculateAudioLength();
    }

    public int getChannels() {
        return mChannels;
    }

    public void setChannels(int channels) {
        mChannels = channels;
        calculateAudioLength();
    }

    private void calculateAudioLength() {
        if (mSamples == null || mSampleRate == 0 || mChannels == 0)
            return;

        mAudioLength = AudioUtils.calculateAudioLength(mSamples.length, mSampleRate, mChannels);     //API: FInd out what this is..!
    }

    private void onSamplesChanged() {      //WHY NOT REFERENCING/USING mSamples IN HERE????? ...maybe goes into buffer in drawRecWave..
        if (mMode == MODE_RECORDING) {
            if (mHistoricalData == null)
                mHistoricalData = new LinkedList<>();
            LinkedList<float[]> temp = new LinkedList<>(mHistoricalData);

            // For efficiency, we are reusing the array of points.
            float[] waveformPoints;
            if (temp.size() == HISTORY_SIZE) {
                waveformPoints = temp.removeFirst();  //historicalData: MAYBE FOR "PREVIOUS" NOISES? (like 10ns ago on screen)
            } else {
                waveformPoints = new float[width * 4];    //HERE IS THE SIZE OF waveformPoints!!!
            }

            drawRecordingWaveform(mSamples, waveformPoints);      //mSamples == buffer (below)

            temp.addLast(waveformPoints);
            mHistoricalData = temp;
            postInvalidate();
        }
    }

    void drawRecordingWaveform(short[] buffer, float[] waveformPoints) {    //difference between these arguments??? CRUCIAL!!!!!!

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

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        mYPoints = yPoints;      //MAYBE CONSIDER PUTTING INSIDE drawRecordingWaveform / other methods

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        for(int i=1; i < 4*width; i+=2)     //making the line flat
            waveformPoints[i] = centerY;

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //mWaveformPoints = waveformPoints;

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //LANDSCAPE

        //NO CROSSOVER (yet!)

        //80?-260Hz <===== Possibly raise upper bound to 300/350

        //i+=194: ACTUAL, FAR too inaccurate

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

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //PORTRAIT

        //NO CROSSOVER (yet!)

        //75-500Hz <===== Possibly lower upper bound to 300/400Hz (eliminate this range)

        //i+=194: ACTUAL, FAR too inaccurate

        //for(int i=1; i < 4*width; i+=66)                      //2*width: cuts off right half (all freqs above 350Hz)
        //    waveformPoints[i] = -Math.abs(yPoints[i/198 + width/450]) + centerY;      //TRIPLING UP (to make fuller)    //450 < x < 500

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    }
}