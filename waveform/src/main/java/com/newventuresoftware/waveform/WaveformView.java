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
public abstract class WaveformView extends View {

    public static final int MODE_RECORDING = 1;
    public static final int MODE_PLAYBACK = 2;

    private static final int HISTORY_SIZE = 6;

    private TextPaint mTextPaint;
    private Paint mStrokePaint, mFillPaint, mMarkerPaint;

    // Used in draw
    private int brightness;
    private Rect drawRect;

    protected int width, height;
    protected float xStep, centerY;
    private int mMode, mAudioLength, mMarkerPosition, mSampleRate, mChannels;
    protected short[] mSamples;
    private LinkedList<float[]> mHistoricalData;
    private Picture mCachedWaveform;
    private Bitmap mCachedWaveformBitmap;
    private int colorDelta = 255 / (HISTORY_SIZE + 1);

    //protected float[] waveformPoints;
    protected float[] mYPoints;

    protected boolean isPortrait;
    protected float value, hue, previousHue, averageVolume;

    public WaveformView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    protected void init(Context context, AttributeSet attrs, int defStyle) {
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
        if (mMode == MODE_PLAYBACK) {
            createPlaybackWaveform();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {     //This is where ACTUAL wave (points) is being drawn onto screen (I belive): TEST
        super.onDraw(canvas);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //System.out.println("BUG");

        float[] hsv = {hue,1,value};
        //float[] hsv = {hue,1,1};
        mStrokePaint.setColor(Color.HSVToColor(hsv));

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        LinkedList<float[]> temp = mHistoricalData;      //what's this all about? find out from "TIP" below...
        if (mMode == MODE_RECORDING && temp != null) {
            brightness = colorDelta;
            for (float[] p : temp) {
                mStrokePaint.setAlpha(brightness);
                canvas.drawLines(p, mStrokePaint);       //WHAT THE HELL IS GOING ON HERE??? ===> INSPECT API: canvas.drawLines()
                brightness += colorDelta;
            }
            //canvas.drawRect(0,0,3.4f*centerY,2*centerY,mStrokePaint);
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

    public void setSamples(short[] samples, float[] yPoints) {
        mYPoints = yPoints;
        mSamples = samples;
        calculateAudioLength();
        onSamplesChanged();        //actually calling this method... could be important... probably not..
    }

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

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //public float[] getWaveformPoints() { return waveformPoints; }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    abstract void drawRecordingWaveform(short[] buffer, float[] waveformPoints);

    Path drawPlaybackWaveform(int width, int height, short[] buffer) {   //Can basically ignore.....
        Path waveformPath = new Path();
        float centerY = height / 2f;
        float max = Short.MAX_VALUE;

        short[][] extremes = SamplingUtils.getExtremes(buffer, width);

        // draw maximums
        for (int x = 0; x < width; x++) {
            short sample = extremes[x][0];
            float y = centerY - ((sample / max) * centerY);
            waveformPath.lineTo(x, y);
        }

        // draw minimums
        for (int x = width - 1; x >= 0; x--) {
            short sample = extremes[x][1];
            float y = centerY - ((sample / max) * centerY);
            waveformPath.lineTo(x, y);
        }

        waveformPath.close();

        return waveformPath;
    }

    private void createPlaybackWaveform() {
        if (width <= 0 || height <= 0 || mSamples == null)
            return;

        Canvas cacheCanvas;
        if (Build.VERSION.SDK_INT >= 23 && isHardwareAccelerated()) {
            mCachedWaveform = new Picture();
            cacheCanvas = mCachedWaveform.beginRecording(width, height);
        } else {
            mCachedWaveformBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            cacheCanvas = new Canvas(mCachedWaveformBitmap);
        }

        Path mWaveform = drawPlaybackWaveform(width, height, mSamples);
        cacheCanvas.drawPath(mWaveform, mFillPaint);
        cacheCanvas.drawPath(mWaveform, mStrokePaint);
        drawAxis(cacheCanvas, width);

        if (mCachedWaveform != null)
            mCachedWaveform.endRecording();
    }

    private void drawAxis(Canvas canvas, int width) {
        int seconds = mAudioLength / 1000;
        float xStep = width / (mAudioLength / 1000f);
        float textHeight = mTextPaint.getTextSize();
        float textWidth = mTextPaint.measureText("10.00");
        int secondStep = (int)(textWidth * seconds * 2) / width;
        secondStep = Math.max(secondStep, 1);
        for (float i = 0; i <= seconds; i += secondStep) {
            canvas.drawText(String.format("%.2f", i), i * xStep, textHeight, mTextPaint);
        }
    }

    public void setYPoints(float[] yPoints) { mYPoints = yPoints; }

    public float[] getYPoints() { return mYPoints; }

    public void setPortraitOrientation(boolean isPortrait) {
        this.isPortrait = isPortrait;
    }

    public boolean getPortraitOrientation() {
        return this.isPortrait;
    }
}