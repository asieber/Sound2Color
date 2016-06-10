package com.newventuresoftware.waveform;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

    private float amplitude, frequency;

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
        if (mMode == MODE_PLAYBACK) {
            createPlaybackWaveform();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {     //This is where ACTUAL wave (points) is being drawn onto screen (I belive): TEST
        super.onDraw(canvas);

        LinkedList<float[]> temp = mHistoricalData;      //what's this all about? find out from "TIP" below...
        if (mMode == MODE_RECORDING && temp != null) {
            brightness = colorDelta;
            for (float[] p : temp) {
                mStrokePaint.setAlpha(brightness);
                canvas.drawLines(p, mStrokePaint);       //WHAT THE HELL IS GOING ON HERE??? ===> INSPECT API: canvas.drawLines()
                brightness += colorDelta;
            }
        } else if (mMode == MODE_PLAYBACK) {
            if (mCachedWaveform != null) {
                canvas.drawPicture(mCachedWaveform);
            } else if (mCachedWaveformBitmap != null) {
                canvas.drawBitmap(mCachedWaveformBitmap, null, drawRect, null);
            }
            if (mMarkerPosition > -1 && mMarkerPosition < mAudioLength)
                canvas.drawLine(xStep * mMarkerPosition, 0, xStep * mMarkerPosition, height, mMarkerPaint);
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
        } else if (mMode == MODE_PLAYBACK) {
            mMarkerPosition = -1;
            createPlaybackWaveform();
        }
    }



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

    void drawRecordingWaveform(short[] buffer, float[] waveformPoints) {    //difference between these arguments??? CRUCIAL!!!!!!
        float lastX = -1;
        float lastY = -1;
        int pointIndex = 0;
        float max = Short.MAX_VALUE;     //key indicator for above predicament..?

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //float[] yPoints = new float[waveformPoints.length/2];      //2*width
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

        for(int i=1; i < 4*width; i+=2)     //making the line flat
            waveformPoints[i] = centerY;

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //NO CROSSOVER (yet!)

        //60-500Hz <===== Possibly lower upper bound to 300/400Hz (eliminate this range)

        //i+=194: ACTUAL, FAR too inaccurate

        for(int i=1; i < 4*width; i+=66)                      //2*width: cuts off right half (all freqs above 350Hz)
            waveformPoints[i] = -Math.abs(yPoints[i/198 + width/450]) + centerY;      //TRIPLING UP (to make fuller)    //450 < x < 500

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
}