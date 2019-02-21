/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyclebikeapp.lookup.airplanes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import opensky.OpenSkyStates;
import opensky.StateVector;

import static com.cyclebikeapp.lookup.airplanes.Constants.CC_MAP_KEY_AIRPLANE_NAME;
import static com.cyclebikeapp.lookup.airplanes.Constants.CC_MAP_KEY_ICAO24;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_MANUFACTURER;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_MODEL;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_OWNER;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_REGISTRATION;
import static com.cyclebikeapp.lookup.airplanes.Constants.DB_KEY_AIR_SPECIES;
import static com.cyclebikeapp.lookup.airplanes.Constants.DISTANCE_TYPE_METRIC;
import static com.cyclebikeapp.lookup.airplanes.Constants.FORMAT_2_0F;
import static com.cyclebikeapp.lookup.airplanes.Constants.FORMAT_3_0F;
import static com.cyclebikeapp.lookup.airplanes.Constants.FORMAT_4_0F;
import static com.cyclebikeapp.lookup.airplanes.Constants.MAX_AIRPLANE_DISTANCE;
import static com.cyclebikeapp.lookup.airplanes.Constants.SHARING_IMAGE_NAME;
import static com.cyclebikeapp.lookup.airplanes.Constants.SPECIES_BLIMP;
import static com.cyclebikeapp.lookup.airplanes.Constants.SPECIES_HELICOPTER;
import static com.cyclebikeapp.lookup.airplanes.Constants.SPECIES_LAND_PLANE;
import static com.cyclebikeapp.lookup.airplanes.Constants.ZERO;
import static com.cyclebikeapp.lookup.airplanes.Constants.foot_per_meter;
import static com.cyclebikeapp.lookup.airplanes.Constants.km_per_meter;
import static com.cyclebikeapp.lookup.airplanes.Constants.kph_per_mps;
import static com.cyclebikeapp.lookup.airplanes.Constants.mile_per_meter;
import static com.cyclebikeapp.lookup.airplanes.Constants.mph_per_mps;
import static com.cyclebikeapp.lookup.airplanes.MainActivity.DEBUG;
import static com.cyclebikeapp.lookup.airplanes.Util.getAlbumStorageDir;
import static com.cyclebikeapp.lookup.airplanes.Utilities.findAirplaneIndex;


/**
 * View that draws the camera preview overlay, keeps track of touch parameters, etc.
 *
 */
@SuppressWarnings("ConstantConditions")
public class GridSatView extends View {
    public static final int DATA_CONNECTION_STATUS_NONE = 1;
    public static final int DATA_CONNECTION_STATUS_OKAY = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 0;
    public static final int LOCATION_STATUS_NONE = 1;
    public static final int LOCATION_STATUS_OLD = 2;
    public static final int LOCATION_STATUS_OKAY = 3;
    private static final String DEG_SYMBOL = "\u00B0";
    public static final int ICON_SIZE = 72;
    private static final int AIRPLANE_TEXT_FONT_SIZE = 24;
    private static final int DRAW_DATA_TEXT_SIZE = 32;
    private static final int DRAW_LOCATION_STATUS_TEXT_SIZE = 32;
    private static final int DRAW_AIRPLANE_WARNING_TEXT_SIZE = 32;
    private static final int FAB_MARGIN = 16;
    private static final int FAB_DIAMETER = 56;
    private static final String LOOK_UP = "LookUp";
    private static final int _360 = 360;
    private static final String TRUE_NORTH = "True North";
    private static final String FORMAT2_1 = "%2.1f";
    private static final String FORMAT3_1 = "%3.1f";
    private static final String X_FOV = "x";
    private static final String ZOOM = "Zoom: ";
    private static final String CHARACTER_LEN_25 = "kkkkkkkkkkkkkkkkkkkkkkkkk";
    private static final int X_TEXT_SIZE = 48;
    private static final int ALPHA = 180;
    private final int  closeBoxColor;
    private final int orangeColor;
    private final int statusBarHeight;
    private float losElDeg;
    private float losAzDeg;
    private float pixPerDeg;
    private float screenRotation;
    String distanceUnit;
    public float touchDownX = 0;
    public float touchDownY = 0;
    public boolean isZooming = false;
    public long pressStartTime;
    public boolean stayedWithinClickDistance;
    private final Drawable playButtonDrawable;
    private final Drawable pauseButtonDrawable;
    private final Drawable menuButtonDrawable;
    private final Drawable lookTitleKomicaWideY;
    private final Drawable grayCameraPreviewBackground;
    public Bitmap previewImage;
    public Bitmap rotatedPreviewImage;
    private final Drawable hotAirBalloonDrawable;

    private final Drawable airplaneDrawable_ltr;
    private final Drawable airplaneDrawable_rtl;
    private final Drawable airplaneDrawable_front;
    private final Drawable airplaneDrawable_rear;
    private final Drawable blimpDrawable_ltr;
    private final Drawable blimpDrawable_rtl;
    private final Drawable blimpDrawable_front;
    private final Drawable blimpDrawable_rear;
    private final Drawable helicopterDrawable_ltr;
    private final Drawable helicopterDrawable_rtl;
    private final Drawable helicopterDrawable_front;
    private final Drawable helicopterDrawable_rear;
    private final Drawable testPatternDrawable_20;

    final HashMap<String, String> airlineCodes;
    final ArrayList<Airplane> mAirplanes;
    Airplane airplaneData;
    HashMap<String, String> airplaneDBData;
    /**
     * Current height of the surface/canvas.
     *
     * @see #setSurfaceSize
     */
    public int mCanvasHeight = 1;
    /**
     * Current width of the surface/canvas.
     */
    private int mCanvasWidth = 1;
    private final int fabMarginPixel;
    private final int fabDiameterPixel;
    public int locationStatus;
    public int dataConnectionStatus;
    private int azIncrement;
    private int elIncrement;
    // when config changes to portrait or landscape the definition of x,y changes
    private float azelConfigCorrection = 0;
    public float panAz = 0;
    public float panEl = 0;
    float tempPanEl = 0;
    float tempPanAz = 0;
    private boolean liveMode = true;
    private double tempCameraZoomFactor;
    private double tempPausedZoomFactor;
    // the zoomFactor user sets via ScaleDetector
    private double pausedZoomFactor;
    // the total zoomFactor, product of pausedZoomFactor and previewImageZoomScaleFactor
    // this is a number from 1.0 to 10.0
    private double zoomScaleFactor;
    // the scale factor at which the previewImage was captured
    // a number from 1.0 to 3.3
    private double previewImageZoomScaleFactor;
    private boolean rotatePreviewImage = false;
    public boolean saveSharingImage;
    private final File mySharingFile;
    public float magDeclination;
    private Snackbar mWriteSettingsSnackbar;
    public String debugText;
    public boolean loadingAirplanes;
    private String selectedAirplaneicao24;
    private static Bitmap ScaleBitmap(Bitmap source, float scale) {
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale, source.getWidth() / 2, source.getHeight() / 2);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    /**
     * method called when we invoke invalidate() on the canvas
      * @param canvas the drawing surface passed to us
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // draw the content like grid lines and satellites
        long startTime = System.nanoTime();
        doDrawGrid(canvas);
        showAirplaneData(canvas);
        drawLookTitle(canvas);
        if (saveSharingImage) {
            // have to disable saving sharingImage right away because onDraw gets called again before File has finished writing
            saveSharingImage = false;
            Log.i(this.getClass().getName(), "saveSharingImage");
            this.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(this.getDrawingCache());
            this.setDrawingCacheEnabled(false);
            if (DEBUG) Log.i(this.getClass().getName(), "mySharingFile location:" + mySharingFile.toString());
            if (Util.isExternalStorageWritable(this.getContext())) {
                try {
                    @SuppressLint("DrawAllocation") FileOutputStream mFileOutStream = new FileOutputStream(mySharingFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, mFileOutStream);
                    mFileOutStream.flush();
                    mFileOutStream.close();
                } catch (Exception e) {
                    if (DEBUG) { e.printStackTrace(); }
                }
            } else {
                // show snackBar complaining about write permission
                mWriteSettingsSnackbar = Snackbar.make(
                        this,
                        getResources().getString(R.string.complain_write_permission),
                        Snackbar.LENGTH_LONG);
                mWriteSettingsSnackbar.show();
            }
        }
        //draw the simulated floating action buttons
        drawMenuButton(canvas);
        drawPlayPauseButton(canvas);
        //drawDataLeft(canvas);
        //drawDataRight(canvas);
        //drawDebugText(canvas);
        drawNoAirplaneWarning(canvas);
        drawLocationStatus(canvas);
        super.onDraw(canvas);
       //Log.d(this.getClass().getName(), "onDraw takes " + String.format("%4.2f",(System.nanoTime() - startTime) / 1000000.) + " msec");
    }

    private void drawNoAirplaneWarning(Canvas canvas) {
        eraseBuildingAirplaneMessage(canvas);
        if (mAirplanes == null) {return;}
        if (loadingAirplanes){
            //instead show loading satellites message
            drawAirplaneStatusMessage(canvas);
            return;
        }
        int numSatellites = mAirplanes.size();
        if (numSatellites > 0){
            // if we have satellites, just return
            return;
        }
        String airplaneWarningText = getResources().getString(R.string.no_airplanes_visible);
        Paint textPaint = new Paint();
        textPaint.setColor(orangeColor);
        textPaint.setTextSize(DRAW_AIRPLANE_WARNING_TEXT_SIZE);
        if (dataConnectionStatus != DATA_CONNECTION_STATUS_OKAY){
            textPaint.setColor(Color.RED);
            airplaneWarningText = getResources().getString(R.string.no_data_connection);
        }
        int top = fabMarginPixel;
        float topOffset = top + lookTitleKomicaWideY.getMinimumHeight() + 20;
        if (locationStatus != LOCATION_STATUS_OKAY){
            topOffset += textPaint.getFontMetrics().bottom - textPaint.getFontMetrics().top;
        }
        float satWarningTextOffset = canvas.getWidth() / 2 - textPaint.measureText(airplaneWarningText)/ 2;
        canvas.drawText(airplaneWarningText, satWarningTextOffset, topOffset, textPaint);
    }

    private void eraseBuildingAirplaneMessage(Canvas canvas) {
        String loadingSatText = "";
        Paint textPaint = new Paint();
        textPaint.setColor(orangeColor);
        textPaint.setTextSize(DRAW_AIRPLANE_WARNING_TEXT_SIZE);
        float topOffset = fabMarginPixel + lookTitleKomicaWideY.getMinimumHeight() + 20;
        if (locationStatus != LOCATION_STATUS_OKAY){
            topOffset += textPaint.getFontMetrics().bottom - textPaint.getFontMetrics().top;
        }
        float loadingAirplaneTextOffset = canvas.getWidth() / 2 - textPaint.measureText(loadingSatText)/ 2;
        canvas.drawText(loadingSatText, loadingAirplaneTextOffset, topOffset, textPaint);
    }

    private void drawAirplaneStatusMessage(Canvas canvas) {
        String airplaneStatusText = getResources().getString(R.string.loading_airplanes);
        Paint textPaint = new Paint();
        textPaint.setColor(orangeColor);
        textPaint.setTextSize(DRAW_AIRPLANE_WARNING_TEXT_SIZE);
        float topOffset = fabMarginPixel + lookTitleKomicaWideY.getMinimumHeight() + 20;
        if (locationStatus != LOCATION_STATUS_OKAY){
            topOffset += textPaint.getFontMetrics().bottom - textPaint.getFontMetrics().top;
        }
        float loadingAirplaneTextOffset = canvas.getWidth() / 2 - textPaint.measureText(airplaneStatusText)/ 2;
        canvas.drawText(airplaneStatusText, loadingAirplaneTextOffset, topOffset, textPaint);
    }

    private void drawLocationStatus(Canvas canvas) {
        String locationStatusText;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        switch(locationStatus){
            case LOCATION_STATUS_NONE:
            case GridSatView.LOCATION_STATUS_UNKNOWN:
                locationStatusText = getResources().getString(R.string.location_unknown);
                textPaint.setColor(Color.RED);
                break;
            case LOCATION_STATUS_OLD:
                locationStatusText = getResources().getString(R.string.location_not_current);
                textPaint.setColor(orangeColor);
            break;
            // don't draw anything if locationstatus is okay
            default:
                return;
        }
        textPaint.setTextSize(DRAW_LOCATION_STATUS_TEXT_SIZE);
        int top = fabMarginPixel;
        float topOffset = top + lookTitleKomicaWideY.getMinimumHeight() + 20;
        float locationStatusTextOffset = canvas.getWidth() / 2 - textPaint.measureText(locationStatusText)/ 2;
        canvas.drawText(locationStatusText, locationStatusTextOffset, topOffset, textPaint);
    }

    private void drawDataLeft(Canvas canvas) {
        @SuppressLint("DefaultLocale")
        String dataLeft = ZOOM + String.format(FORMAT2_1, zoomScaleFactor) + X_FOV;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        float topOffset = DRAW_DATA_TEXT_SIZE;
        float dataLeftTextOffset = fabMarginPixel;
        canvas.drawText(dataLeft, dataLeftTextOffset, topOffset, textPaint);
    }

    private void drawDebugText(Canvas canvas) {
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        float topOffset = canvas.getHeight() - DRAW_DATA_TEXT_SIZE;
        float dataLeftTextOffset = canvas.getWidth() / 2 -20;
        canvas.drawText(touchDownX + " / " + touchDownY, dataLeftTextOffset, topOffset, textPaint);
    }

    private void drawDataRight(Canvas canvas) {
        String dataRight = TRUE_NORTH;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        float topOffset = DRAW_DATA_TEXT_SIZE;
        float dataRightTextOffset;
        dataRightTextOffset = mCanvasWidth - textPaint.measureText(dataRight);
        dataRightTextOffset -= fabMarginPixel;
        canvas.drawText(dataRight, (dataRightTextOffset), topOffset, textPaint);
    }

    private void drawLookTitle(Canvas canvas) {
        int left = canvas.getWidth() / 2 - lookTitleKomicaWideY.getMinimumWidth() / 2;
        int right = left + lookTitleKomicaWideY.getMinimumWidth();
        int top = fabMarginPixel;
        int bottom = top + lookTitleKomicaWideY.getMinimumHeight();
        lookTitleKomicaWideY.setBounds(left, top, right, bottom);
        lookTitleKomicaWideY.draw(canvas);
    }

    /**
     * Draw a background, if we want to cover the camera preview, or just draw over the preview.
     * the camera preview will erase the previous drawing
     *
     * @param canvas the drawing surface
     */
    private void doDrawGrid(Canvas canvas) {
    boolean drawPreviewImage = true;
        int cw_2 = mCanvasWidth / 2;
        int ch_2 = mCanvasHeight / 2;
        if (!liveMode) {
            Bitmap drawingBitmap;
            grayCameraPreviewBackground.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            grayCameraPreviewBackground.draw(canvas);
            try {
                // when viewing previewImage in LiveMode, Camera scales the image to fit screen because previewImage may be less than screen density
                // ie we have a [720 x 480] preview image because the ratio best fits the screen aspect.
                // here we must scale the previewImage to fit the screen when pausedZoom = 1
                float scale = 1;
                float left = 0;
                float top = 0;
                double pausedScaleFactor = zoomScaleFactor / previewImageZoomScaleFactor;
                //this scales the previewImage up to screen dimension, even when zoom = 1
                double deltaAz = (panAz + _360) % _360;
                if (deltaAz > 180) {
                    deltaAz -= _360;
                } else if (deltaAz < -180) {
                    deltaAz += _360;
                }
                if (rotatePreviewImage) { // previewImage taken in portrait

                    if (inLandscapeMode(canvas)) {
                        //todo formulate left_offset and top_offset
                        drawPreviewImage = false;
                        // if previewImage taken in portrait and now we're in landscape scale = canvas.width / previewImage.width
                        // if previewImage taken in portrait and now we're in portrait scale = canvas.width / previewImage.height
                        // match center of previewImage to center of canvas
                    } else {
                        left = (float) ((1 - pausedScaleFactor) * cw_2 - deltaAz * pixPerDeg);
                        top = (float) ((1 - pausedScaleFactor) * ch_2 + panEl * pixPerDeg);
                        scale = (float) (pausedScaleFactor) * canvas.getHeight() / rotatedPreviewImage.getHeight();
                    }

                    // if image too big, scale by 1/2 and divide left, top by 15%
                    if (rotatedPreviewImage.getWidth() * scale > 2000 || rotatedPreviewImage.getHeight() * scale > 2000){
                        // crop center half of image- generalize crop factor
                        double cropFactor = Math.floor(scale* rotatedPreviewImage.getHeight()/2000) * 1.15;
                        //create bitmap based on previewImage, which has not been scaled to the canvas
                        int width = (int) (rotatedPreviewImage.getWidth() / cropFactor);
                        int height = (int) (rotatedPreviewImage.getHeight() / cropFactor);
                        int cropLeft = 0;
                        if (left < 0){
                            cropLeft = (int) ((int) -left / scale);
                            left = 0;
                        }
                        int cropTop = 0;
                        if (top < 0){
                            cropTop = (int) ((int) -top / scale);
                            top = 0;
                        }
                        if ((width + cropLeft) > rotatedPreviewImage.getWidth()){
                            width = rotatedPreviewImage.getWidth() - cropLeft;
                        }
                        if ((height + cropTop) > rotatedPreviewImage.getHeight()){
                            height = rotatedPreviewImage.getHeight() - cropTop;
                        }

//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "cropFactor: " + String.format(FORMAT3_1,cropFactor)
//                                    + " scale: " + String.format(FORMAT3_1,scale)
//                                    + " [cropLeft, cropTop] " + "["+cropLeft + ", "+cropTop + "]"
//                                    + " [width, height] " + "["+width + ", "+height + "]");
//                        }

                        drawingBitmap = Bitmap.createBitmap(rotatedPreviewImage,cropLeft,cropTop,width,height);
//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "drawingBitmap: "
//                                    + " [width, height] " + "["+drawingBitmap.getWidth() + ", "+drawingBitmap.getHeight() + "]");
//                        }
                        if (inLandscapeMode(canvas)) {
                            drawPreviewImage = false;
                        } else {
                            //left = (float) ((1 - pausedScaleFactor) * cw_2 - deltaAz * pixPerDeg);
                           // top = (float) ((1 - pausedScaleFactor) * ch_2 + panEl * pixPerDeg);
                            scale = (float) (pausedScaleFactor) * canvas.getHeight() / rotatedPreviewImage.getHeight();
                        }
//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "rotatedPreviewImage tooBig [left, top, scale] "
//                                    + "[" + String.format(FORMAT3_1, left) + ", " + String.format(FORMAT3_1, top) + ", "
//                                    + String.format(FORMAT3_1, scale) + "]");
//                        }
                    }else {//previewImage is not too big
//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "rotatedPreviewImage okay [left, top, scale] "
//                                    + "[" + String.format(FORMAT3_1,left) + ", "+ String.format(FORMAT3_1,top) + ", "
//                                    + String.format(FORMAT3_1,scale) + "]");
//                        }
                        drawingBitmap = Bitmap.createBitmap(rotatedPreviewImage);
/*                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "drawingBitmap: "
                                    + " [width, height] " + "["+drawingBitmap.getWidth() + ", "+drawingBitmap.getHeight() + "]");
                        }*/
                    }
                    if (drawPreviewImage) {
                        canvas.drawBitmap(ScaleBitmap(drawingBitmap, scale), left, top, new Paint());
                    }
                } else {// previewImage taken in landscape

                    if (inLandscapeMode(canvas)) {
                        // if previewImage taken in landscape and now we're in landscape scale = canvas.width/previewImage.width
                        // if previewImage taken in landscape and now we're in portrait scale = canvas.width / previewImage.height
                        left = (float) ((1 - pausedScaleFactor) * cw_2 - deltaAz * pixPerDeg);
                        top = (float) ((1 - pausedScaleFactor) * ch_2 + panEl * pixPerDeg);
                        scale = (float) (pausedScaleFactor) * canvas.getWidth() / previewImage.getWidth();
                    } else {
                        drawPreviewImage = false;
                        //todo formulate left_offset and top_offset
                        // previewImage is wider than canvas now
                        scale = (float) (pausedScaleFactor) * canvas.getHeight() / previewImage.getWidth();
                        left = (float) ((cw_2 - ch_2) - deltaAz * pixPerDeg);
                        top = ((ch_2 - cw_2) + panEl * pixPerDeg);
                    }

                    // if image too big, scale and divide left, top by cropFactor
                    if ( previewImage.getWidth() * scale > 2000 || previewImage.getHeight() * scale > 2000){
                        // crop center of image- generalize crop factor
                        double cropFactor = Math.floor(scale* previewImage.getWidth()/2000) * 1.15;
                        //create bitmap based on previewImage, which has not been scaled to the canvas
                        int width = (int) (previewImage.getWidth() / cropFactor);
                        int height = (int) (previewImage.getHeight() / cropFactor);
                        int cropLeft = 0;
                        if (left < 0){
                            cropLeft = (int) ((int) -left / scale);
                        left = 0;
                        }
                        int cropTop = 0;
                        if (top < 0){
                            cropTop = (int) ((int) -top / scale);
                            top = 0;
                        }
                        if ((width + cropLeft) > previewImage.getWidth()){
                            width = previewImage.getWidth() - cropLeft;
                        }
                        if ((height + cropTop) > previewImage.getHeight()){
                            height = previewImage.getHeight() - cropTop;
                        }

/*
                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "cropFactor: " + String.format(FORMAT3_1,cropFactor)
                                    + " scale: " + String.format(FORMAT3_1,scale)
                                    + " [cropLeft, cropTop] " + "["+cropLeft + ", "+cropTop + "]"
                                    + " [width, height] " + "["+width + ", "+height + "]");
                        }
*/

                        drawingBitmap = Bitmap.createBitmap(previewImage,cropLeft,cropTop,width,height);
                        // if previewImage taken in landscape and now we're in landscape scale = canvas.width/previewImage.width
                        // if previewImage taken in landscape and now we're in portrait scale = canvas.width / previewImage.height
                        if (inLandscapeMode(canvas)) {
                            scale = (float) (pausedScaleFactor) * canvas.getWidth() / previewImage.getWidth();
                        } else {
                            drawPreviewImage = false;
                            // drawingBitmap is wider than canvas now

                        }

                        if (DEBUG) {
                            Log.i(this.getClass().getName(), "previewImage tooBig [left, top, scale] "
                                    + "[" + String.format(FORMAT3_1,left) + ", "+ String.format(FORMAT3_1,top) + ", "
                                    + String.format("%3.2f",scale) + "]");
                        }

                    }else {//previewImage is not too big
/*                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "previewImage okay [left, top, scale] "
                                    + "[" + String.format(FORMAT3_1,left) + ", "+ String.format(FORMAT3_1,top) + ", "
                                    + String.format(FORMAT3_1,scale) + "]");
                        }*/
                        drawingBitmap = Bitmap.createBitmap(previewImage);
/*                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "drawingBitmap: "
                                    + " [width, height] " + "["+drawingBitmap.getWidth() + ", "+drawingBitmap.getHeight() + "]");
                        }*/

                    }
                    if (drawPreviewImage) {
                        canvas.drawBitmap(ScaleBitmap(drawingBitmap, scale), left, top, new Paint());
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }// only have to manipulate the previewImage when Paused
        canvas.save();
        // rotate the canvas about canvas half-width and half-height to compensate for screen rotation
        // and the configuration change from landscape to portrait
        canvas.rotate(screenRotation + azelConfigCorrection, cw_2, ch_2);
        drawGridLines_Labels(canvas, cw_2, ch_2);
        // while we're rotated, draw the satellites at their az, el locations
        drawAirplanes(canvas);
        //drawTestPattern(canvas);
        // rotate the canvas back to normal
        canvas.restore();
    }

    private void drawGridLines_Labels(Canvas canvas, int cw_2, int ch_2) {
        //set-up elevation lines and labels
        //number of el & azimuth lines we'll need - don't have to cover 360°
        int imax = 6;
        float[] elPts = new float[imax*4];
        String[] elLabel = new String[imax];
        float losElDeg = getLosElDeg() + panEl;
        //starting el label - offscreen in case screen is rotated
        float lineElDeg = (losElDeg - losElDeg % elIncrement - 2 * elIncrement);
        if (lineElDeg < -10){
            lineElDeg = -10;
        }
        int startX = -cw_2;
        int endX = 3 * cw_2;
        for (int i = 0; i < elLabel.length; i++) {
            int startY = (int) (ch_2 + (losElDeg - lineElDeg) * pixPerDeg);
            elPts[4 * i] = startX;
            elPts[4 * i + 1] = startY;
            elPts[4 * i + 2] = endX;
            elPts[4 * i + 3] = startY;
            elLabel[i] = Integer.toString((int) lineElDeg) + DEG_SYMBOL;
            lineElDeg += elIncrement;
        }
        Paint linePaint = new Paint();
        linePaint.setColor(Color.RED);
        canvas.drawLines(elPts, linePaint);
        // Calculate pixel position for the azimuth grid lines and labels
        float losAzDeg = (getLosAzDeg() + panAz + _360) % _360;
        float[] azPts = new float[imax * 4];
        String[] azLabel = new String[imax];
        //starting az label - offscreen in case screen is rotated
        double lineAzDeg = (losAzDeg - losAzDeg % azIncrement - 2 * azIncrement);
        int startY = -ch_2;
        int endY = 3 * ch_2;
        for (int i = 0; i < azLabel.length; i++) {
            startX = (int) (cw_2 - (losAzDeg - lineAzDeg) * pixPerDeg);
            azPts[4 * i] = startX;
            azPts[4 * i + 1] = startY;
            azPts[4 * i + 2] = startX;
            azPts[4 * i + 3] = endY;
            azLabel[i] = Integer.toString((int) ((lineAzDeg + _360) % _360)) + DEG_SYMBOL;
            lineAzDeg += azIncrement;
        }
        linePaint.setColor(Color.GREEN);
        canvas.drawLines(azPts, linePaint);
        labelLines(canvas, elPts, elLabel, azPts, azLabel);
    }

    private boolean inLandscapeMode(Canvas canvas) {
        return canvas.getWidth() > canvas.getHeight();
    }

    private void drawAirplanes(Canvas canvas) {
        Paint airTextPaint = new Paint();
        airTextPaint.setColor(Color.YELLOW);
        airTextPaint.setTextAlign(Paint.Align.LEFT);
        airTextPaint.setTextSize(AIRPLANE_TEXT_FONT_SIZE);
        int halfIcon = dpToPixel(ICON_SIZE / 2);
        RectF canvasRect = new RectF(-halfIcon, -halfIcon, mCanvasWidth + halfIcon, mCanvasHeight + halfIcon);
        if (mAirplanes != null) {
            for (Airplane aPlane : mAirplanes) {
                //the aSat look angle assumes we're referencing True North
                FindAirplaneXY findAirplaneXY = new FindAirplaneXY(getLosElDeg() + panEl,
                        (getLosAzDeg() + panAz + _360) % _360, aPlane).invoke();
                int startX = findAirplaneXY.getStartX();
                int startY = findAirplaneXY.getStartY();
                // test if point is in canvas via Rect.contains(startX, startY)
                if (canvasRect.contains(startX, startY)) {
                    drawAirplane(aPlane, startX, startY, halfIcon, canvas);
                    // label with satellite name from satellite list
                    labelAirplane(aPlane, canvas, startX, startY, halfIcon, airTextPaint);
                }
            }
        }
    }

    private void drawAirplane(Airplane aPlane, int startX, int startY, int halfIcon, Canvas canvas) {
        double planeDeltaDegree = Math.abs((aPlane.getLookAngleAz() + _360) % _360 - aPlane.getSv().getHeading() - 90);
        double planeDeltaDegree_FR = (Math.abs(aPlane.getLookAngleAz() - aPlane.getSv().getHeading()) + _360) % _360;
        boolean airplaneIsLTR = planeDeltaDegree >= 90. && planeDeltaDegree < 270.;
        boolean airplaneIsRear = planeDeltaDegree_FR <= 15. && planeDeltaDegree_FR >= 0;
        boolean airplaneIsFront = planeDeltaDegree_FR >= 165. && planeDeltaDegree_FR <= 180;
        boolean speciesIsAirplane = aPlane.getSpecies() == SPECIES_LAND_PLANE || aPlane.getSpecies() == 0;
        boolean speciesIsHelicopter = aPlane.getSpecies() == SPECIES_HELICOPTER;
        boolean speciesIsBlimp = aPlane.getSpecies() == SPECIES_BLIMP;
        Drawable planeDrawable = hotAirBalloonDrawable;
        if (speciesIsAirplane) {
            if (airplaneIsFront) {
                planeDrawable = airplaneDrawable_front;
            } else if (airplaneIsRear) {
                planeDrawable = airplaneDrawable_rear;
            } else if (airplaneIsLTR) {
                planeDrawable = airplaneDrawable_ltr;
            } else {
                planeDrawable = airplaneDrawable_rtl;
            }
        } else if (speciesIsHelicopter){
            if (airplaneIsFront) {
                planeDrawable = helicopterDrawable_front;
            } else if (airplaneIsRear) {
                planeDrawable = helicopterDrawable_rear;
            } else if (airplaneIsLTR) {
                planeDrawable = helicopterDrawable_ltr;
            } else {
                planeDrawable = helicopterDrawable_rtl;
            }
        } else if (speciesIsBlimp){
            if (airplaneIsFront) {
                planeDrawable = blimpDrawable_front;
            } else if (airplaneIsRear) {
                planeDrawable = blimpDrawable_rear;
            } else if (airplaneIsLTR) {
                planeDrawable = blimpDrawable_ltr;
            } else {
                planeDrawable = blimpDrawable_rtl;
            }
        }

        halfIcon = getHalfIcon(halfIcon, aPlane.getDistance());
        planeDrawable.setBounds(startX - halfIcon, startY - halfIcon, startX + halfIcon, startY + halfIcon);
        // convert angle to pixel left, top, right, bottom
        planeDrawable.draw(canvas);
    }

    private int getHalfIcon(int halfIcon, double distance) {
        if (distance >= MAX_AIRPLANE_DISTANCE/2) {
            halfIcon = halfIcon / 2;
        } else if (distance > MAX_AIRPLANE_DISTANCE/4) {
            halfIcon = halfIcon * 3 / 4;
        }
        return halfIcon;
    }

    private void drawTestPattern(Canvas canvas) {
        int halfIcon = 10;
        int left = 10;
        int top = 10;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        int cw= canvas.getWidth();
        int ch = canvas.getHeight();
        left = cw - 2*halfIcon;
        top = 10;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        left = cw/2 - halfIcon;
        top = ch/2 - halfIcon;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        left = 10;
        top = ch - halfIcon;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        left = cw - halfIcon;
        top = ch - halfIcon;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        left = cw/2 - halfIcon;
        top = ch/4 - halfIcon;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        left = cw/2 - halfIcon;
        top = ch * 3/4-halfIcon;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        left = cw/4 - halfIcon;
        top = ch/2 - halfIcon;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
        left = cw * 3/4 - halfIcon;
        top = ch/2 - halfIcon;
        testPatternDrawable_20.setBounds(left, top, left + 2*halfIcon, top + 2*halfIcon);
        labeltestPattern(canvas, left, top, halfIcon);
        testPatternDrawable_20.draw(canvas);
    }


    /**
     * Return a list of hashmaps containing airplane info when a user clicks on the canvas.
     * The hashmap has the name, icao24. The name will appear in a
     * drop-down list if there is more than one target in the click region. The icao24 number
     * will be used to extract info from the database for the airplane info.
     *
     * @param lookAngAz is the azimuth angle of the canvas click spot
     * @param lookAngEl is the elevation angle of the canvas click spot
     * @param tol       is the angle tolerance of the canvas click;
     *                  this depends on the canvas scale and zoom factor
     * @return the ArrayList; could be null if no airplanes in the click spot
     */
    ArrayList<HashMap<String, String>> getCanvasClickAirplanes(float lookAngAz, float lookAngEl, float tol) {
        ArrayList<HashMap<String, String>> returnList = new ArrayList<>();
        if (mAirplanes != null) {
            for (Airplane anAirplane : mAirplanes) {
                addAirplaneToList(lookAngAz, lookAngEl, tol, returnList, anAirplane);
            }
        }
        return returnList;
    }

    /**
     * See if we should add this airplane to the pop-up list when user clicks on the canvas
     *
     * @param lookAngAz  horizontal angle of screen center
     * @param lookAngEl  vertical angle of screen center
     * @param tol        a tolerance in angle such that we can't distinguish between two overlapping satellites
     * @param returnList an ArrayList of Hashmaps with airplane name and icao24 number
     * @param anAirplane       the airplane to possibly add to the ArrayList
     */
    private void addAirplaneToList(float lookAngAz, float lookAngEl, float tol,
            ArrayList<HashMap<String, String>> returnList, Airplane anAirplane) {
        HashMap<String, String> hmItem = new HashMap<>();
/*        Log.w(this.getClass().getSimpleName(), String.format("name: %s az/el %s/%s lookangle az/el %s/%s", anAirplane.getName(),
                String.format(FORMAT_3_1F, anAirplane.getLookAngleAz()), String.format(FORMAT_3_1F, anAirplane.getLookAngleEl()),
                String.format(FORMAT_3_1F, lookAngAz), String.format(FORMAT_3_1F, lookAngEl)));*/
        if (anAirplane.getLookAngleAz() < lookAngAz + tol
                && anAirplane.getLookAngleAz() > lookAngAz - tol
                && anAirplane.getLookAngleEl() < lookAngEl + tol
                && anAirplane.getLookAngleEl() > lookAngEl - tol) {
            hmItem.put(CC_MAP_KEY_AIRPLANE_NAME, anAirplane.getName());
            hmItem.put(CC_MAP_KEY_ICAO24, String.valueOf(anAirplane.getSv().getIcao24()));
            returnList.add(hmItem);
        }
    }

    private String findAirplaneIdentifier(StateVector aState, AirplaneDBAdapter db) {
        String indentifier;
        String callsign = aState.getCallsign();
        String tailNumber = null;
        if (db != null && !db.isClosed()) {
            tailNumber = db.fetchAirplaneData(aState.getIcao24()).get(DB_KEY_AIR_REGISTRATION);
        }
        if(callsign != null && callsign.length() > 2) {
            indentifier = callsign + " (" + aState.getIcao24() + ")";
        } else if (tailNumber != null && tailNumber.length() > 2) {
            indentifier = tailNumber + " (" + aState.getIcao24() + ")";
        } else {
            indentifier = aState.getIcao24();
        }
        return indentifier;
    }

    void mergeNewStates(OpenSkyStates os, AirplaneDBAdapter db) {
        // given a new Collection of StateVectors (an OpenSkyStates), update information in current list, myOpenSkyStateArrayList
        // for each element in new Collection, find index to myArrayList using icao24 String.
        // if element exists in myArrayList, get StateVector at that index and replace non-null components
        // else add element to myArrayList
        if (os.getStates() == null){
            if (DEBUG)Log.wtf(this.getClass().getSimpleName(), "os.getStates() is null!");
            return;
        }
        if (DEBUG)Log.w(this.getClass().getSimpleName(), String.format("merging - found %d new StateVectors", os.getStates().size()));
        // if getLastPositionUpdate() is null, remove this StateVector from myArrayList since we haven't had a position update in 15 seconds.
        for (StateVector sv : os.getStates()) {
            int index = findAirplaneIndex(sv.getIcao24(), mAirplanes);
            boolean timeIsRecent = sv.getLastPositionUpdate() != null
                    && System.currentTimeMillis() / 1000. - sv.getLastPositionUpdate() < 10.;
            if (index < mAirplanes.size() && index >=0 && !sv.isOnGround() && timeIsRecent) {
                updateMyStateVectorValues(sv, index);
            } else if (!sv.isOnGround() && timeIsRecent) {
                Airplane newAirplane = new Airplane(sv);
                int species  =Constants.SPECIES_LAND_PLANE;
                if (db != null && !db.isClosed()) {
                    HashMap<String, String> hm = db.fetchAirplaneData(sv.getIcao24());
                    if (hm != null){
                        String s = hm.get(DB_KEY_AIR_SPECIES);
                        if (s != null){
                            species = Integer.parseInt(s);
                        }
                    }
                }
                newAirplane.setSpecies(species);
                newAirplane.setName(sv, db);
                newAirplane.setAirline(getAirlineFromCallsign(sv.getCallsign()));
                mAirplanes.add(newAirplane);
                String[] params = new String[]{sv.getIcao24()};
                // todo new RetrieveOriginDestBackground().execute(params);
            }
        }
    }

    private String getAirlineFromCallsign(String callsign) {
        if (callsign.length() < 3){
            return "";
        }
        String code = callsign.substring(0,3).toUpperCase();
        //Log.i("getAirlineFromCallsign", String.format("callsign: %s airline: %s", code, airlineCodes.containsKey(code)?airlineCodes.get(code):""));
        return (airlineCodes.containsKey(code)?airlineCodes.get(code):"");
    }

    void updateMyStateVectorValues(StateVector sv, int index) {
        // replace non-null values from retrieved StateVector in current MyStateVector List, at given index.
        // Index was found thru icao24 value
        StateVector myStateVector = mAirplanes.get(index).getSv();
        if (sv.getGeoAltitude() != null) {
            myStateVector.setGeoAltitude(sv.getGeoAltitude());
        }
        if (sv.getBaroAltitude() != null) {
            myStateVector.setBaroAltitude(sv.getBaroAltitude());
        }
        //can't trust lastPositionUpdate time to change even if lat/long does change.
        // Test that lastPositionUpdate > previous value before updating this
        if (sv.getLongitude() != null && sv.getLastPositionUpdate() != null
                && myStateVector.getLastPositionUpdate() != null
                && sv.getLastPositionUpdate() > myStateVector.getLastPositionUpdate()) {
            myStateVector.setLongitude(sv.getLongitude());
        }
        //can't trust lastPositionUpdate time to change even if lat/long does change.
        // Test that lastPositionUpdate > previous value before updating this
        if (sv.getLatitude() != null && sv.getLastPositionUpdate() != null
                && myStateVector.getLastPositionUpdate() != null
                && sv.getLastPositionUpdate() > myStateVector.getLastPositionUpdate()) {
            myStateVector.setLatitude(sv.getLatitude());
        }
        if (sv.getVelocity() != null) {
            myStateVector.setVelocity(sv.getVelocity());
        }
        if (sv.getHeading() != null) {
            myStateVector.setHeading(sv.getHeading());
        }
        if (sv.getVerticalRate() != null) {
            myStateVector.setVerticalRate(sv.getVerticalRate());
        }
        myStateVector.setCallsign(sv.getCallsign());
        myStateVector.setOnGround(sv.isOnGround());
        if (sv.getLastContact() != null) {
            myStateVector.setLastContact(sv.getLastContact());
        }
        if (sv.getLastPositionUpdate() != null) {
            myStateVector.setLastPositionUpdate(sv.getLastPositionUpdate());
        }
        myStateVector.setOriginCountry(sv.getOriginCountry());
        myStateVector.setSquawk(sv.getSquawk());
        myStateVector.setSpi(sv.isSpi());
        myStateVector.setPositionSource(sv.getPositionSource());
        mAirplanes.get(index).setSv(myStateVector);
    }


    void purgeOldStateVectors() {
        // after updating new information, get rid of StateVectors in myOpenStateArrayList where getLastPositionUpdate is null
        // Can be {@code null} if there was no position report received by OpenSky within 15s before.
        // Also not interested in StateVectors that don't have any position information, or airplanes on the ground, or
        // no position update for 60 seconds
        for (int i = 0; i < mAirplanes.size(); i++) {
            boolean nullPositionUpdate = mAirplanes.get(i).getSv().getLastPositionUpdate() == null;
            boolean onGround = mAirplanes.get(i).getSv().isOnGround();
            // todo adjust System.currentTime for GPS offset time
            boolean noUpdates = (System.currentTimeMillis()/1000. - mAirplanes.get(i).getSv().getLastPositionUpdate()) > 15.;
/*            Log.w(this.getClass().getSimpleName(),
                    String.format("purging - nullPositionUpdate: %s onGround: %s noUpdates: %s",
                            (nullPositionUpdate?"y":"n"), (onGround?"y":"n"), (noUpdates?"y":"n")));*/
            if (nullPositionUpdate || onGround || noUpdates) {
                mAirplanes.remove(i);
            }
        }
    }

    private void labelAirplane(Airplane aPlane, Canvas canvas, int startX, int startY, int halfIcon, Paint textPaint) {
        // halfIcon size changes with distance to airplane
        halfIcon = getHalfIcon(halfIcon, aPlane.getDistance());
        // center the label under the airplane
        float textStart = textPaint.measureText(aPlane.getName()) / 2;
        canvas.drawText(aPlane.getName(),
                startX - textStart, startY + halfIcon, textPaint);
    }

    private void labeltestPattern(Canvas canvas, int left, int top, int halfIcon) {
        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(AIRPLANE_TEXT_FONT_SIZE);
        int textTop = ((top+3*halfIcon) > canvas.getHeight())?top - 2*halfIcon:top + 3*halfIcon;
        int textLeft = ((left+halfIcon) > canvas.getWidth())?left - 2*halfIcon:left + halfIcon;

        canvas.drawText((left + halfIcon) + " / " + (top + halfIcon),
                textLeft, textTop, textPaint);
    }
    private void labelLines(Canvas canvas, float[] elPts, String[] elLabel, float[] azPts, String[] azLabel) {
        // calculate position for elevation labels and labels
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(32);
        float elTextOffset;
        for (int i = 0; i < elLabel.length; i++) {
            elTextOffset = textPaint.measureText(elLabel[i]);
            canvas.drawText(elLabel[i], (mCanvasWidth - elTextOffset) / 2, elPts[4 * i + 1], textPaint);
        }
        // calculate position for azimuth labels and draw labels
        textPaint.setColor(Color.GREEN);
        float azTextOffset;
        for (int i = 0; i < azLabel.length; i++) {
            azTextOffset = textPaint.measureText(azLabel[i]) / 2;
            canvas.drawText(azLabel[i], azPts[4 * i] - azTextOffset, mCanvasHeight / 2, textPaint);
        }
    }

    /* Callback invoked when the surface dimensions change. */
    public void setSurfaceSize(int width, int height) {
        mCanvasWidth = width;
        mCanvasHeight = height;
        //calculate azIncrement so that we always have two az lines on the screen
        azIncrement = Math.round(mCanvasWidth / (5 * 2 * pixPerDeg)) * 5;
        elIncrement = Math.round(mCanvasHeight / (5 * 2 * pixPerDeg)) * 5;
    }

    /**
     * scale factor to convert world angle in degrees to pixels to match camera FOV
     */
    public void setFOVScale(float[] fov) {
        float thetaHDeg = (float) (fov[1] / zoomScaleFactor);
        float thetaVDeg = (float) (fov[0] / zoomScaleFactor);
        float scaleW = (mCanvasWidth / thetaHDeg);
        float scaleH = (mCanvasHeight / thetaVDeg);
        //depending on screen rotation have to match FOV with canvas size
        if ((mCanvasHeight > mCanvasWidth) && (thetaHDeg > thetaVDeg)){
            scaleW = (mCanvasHeight / thetaHDeg);
            scaleH = (mCanvasWidth / thetaVDeg);
        }
        //Log.i(this.getClass().getName(), "[canvasW, canvasH]: [" + mCanvasWidth + ", " + mCanvasHeight + "]" + "[scaleW, scaleH]: [" + String.format("%3.1f", scaleW) + ", " + String.format("%3.1f", scaleH) + "]");
        // assume that camera image gets cropped in narrow direction, so FOV in the wide direction is preserved
        if (scaleW > scaleH) {
            pixPerDeg = scaleW;
        } else {
            pixPerDeg = scaleH;
        }
        azIncrement = Math.round(mCanvasWidth / (5 * 2 * pixPerDeg)) * 5;
        elIncrement = Math.round(mCanvasHeight / (5 * 2 * pixPerDeg)) * 5;
        // set minimum az, el increments
        if (azIncrement == 0) {
            azIncrement = 2;
        }
        if (elIncrement == 0) {
            elIncrement = 2;
        }
    }

    /**
     * Given canvas click coordinates, convert to azimuth and elevation, taking account of screen rotation
     * and all the scale factors and pan distance
     *
     * @return a float array containing the azimuth angle and elevation angle of the click
     */
    public float[] convertClickXYToAzEl() {
        // in landscape need to correct screenRotation value using azelConfigCorrection
        double cos = Math.cos(Math.toRadians(screenRotation + azelConfigCorrection));
        double sin = Math.sin(Math.toRadians(screenRotation + azelConfigCorrection));
        float tdX = touchDownX;
        float tdY = touchDownY - statusBarHeight;

        // correct screen rotation by rotating x-y axes around z;
//        float azClick = (float) ((((touchDownX - mCanvasWidth / 2) * cos - (mCanvasHeight / 2 - touchDownY + statusBarHeight) * sin) / pixPerDeg)
//                + losAzDeg + panAz + 12* _360) % _360;
//        float elClick = (float) (((touchDownX - mCanvasWidth / 2) * sin + (mCanvasHeight / 2 - touchDownY + statusBarHeight) * cos) / pixPerDeg)
//                + losElDeg + panEl;
        float azClick = (float) ((((tdX - mCanvasWidth / 2) * cos - (mCanvasHeight / 2 - tdY) * sin) / pixPerDeg)
                + losAzDeg + panAz + 12 * _360) % _360;
        float elClick = (float) (((tdX - mCanvasWidth / 2) * sin + (mCanvasHeight / 2 - tdY) * cos) / pixPerDeg)
                + losElDeg + panEl;
        return new float[]{azClick, elClick};
    }

    public GridSatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.distanceUnit= String.valueOf(Constants.DISTANCE_TYPE_MILE);
        mAirplanes = new ArrayList<>();
        airlineCodes = new HashMap<>();
        airplaneDBData = new HashMap<>();
        selectedAirplaneicao24 = ZERO;
        setFocusable(true); // make sure we get key events
        locationStatus = LOCATION_STATUS_OKAY;
        boolean hasInternet = Util.hasWifiInternetConnection(context) ||
                (Util.hasInternetConnection(context) && Util.hasMobileDataPermission(context));
        dataConnectionStatus = hasInternet?GridSatView.DATA_CONNECTION_STATUS_OKAY:GridSatView.DATA_CONNECTION_STATUS_NONE;
        loadingAirplanes = true;
        losElDeg = 55;
        losAzDeg = (float) 357.5;
        pixPerDeg = 50;
        zoomScaleFactor = 1.0;
        previewImageZoomScaleFactor = 1.;
        pausedZoomFactor = 1.;
        tempCameraZoomFactor = 1.;
        tempPausedZoomFactor = 1.;
        screenRotation = 5;
        statusBarHeight = getStatusBarHeight(context);
        //store these constants for drawing buttons
        fabMarginPixel = dpToPixel(FAB_MARGIN);
        fabDiameterPixel = dpToPixel(FAB_DIAMETER);
        debugText = "";
        //cache Drawables
        grayCameraPreviewBackground  = ContextCompat.getDrawable(context, R.drawable.random_gray_background_100);
        lookTitleKomicaWideY = ContextCompat.getDrawable(context, R.drawable.lookuptitlekomikawidey);
        menuButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_floating_menu_button_56_better);
        pauseButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_floating_pause_button_56_better);
        playButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_floating_play_button_56_better);
        hotAirBalloonDrawable = ContextCompat.getDrawable(context, R.drawable.balloon_icon_192);
        blimpDrawable_ltr = ContextCompat.getDrawable(context, R.drawable.blimp_icon_192_ltr);
        blimpDrawable_rtl = ContextCompat.getDrawable(context, R.drawable.blimp_icon_192_rtl);
        blimpDrawable_front = ContextCompat.getDrawable(context, R.drawable.blimp_icon_192_front);
        blimpDrawable_rear = ContextCompat.getDrawable(context, R.drawable.blimp_icon_192_rear);
        airplaneDrawable_ltr = ContextCompat.getDrawable(context, R.drawable.airplane_icon_192_ltr);
        airplaneDrawable_rtl = ContextCompat.getDrawable(context, R.drawable.airplane_icon_192_rtl);
        airplaneDrawable_front = ContextCompat.getDrawable(context, R.drawable.airplane_icon_192_front);
        airplaneDrawable_rear = ContextCompat.getDrawable(context, R.drawable.airplane_icon_192_rear);
        helicopterDrawable_ltr = ContextCompat.getDrawable(context, R.drawable.helicopter_icon_192_ltr);
        helicopterDrawable_rtl = ContextCompat.getDrawable(context, R.drawable.helicopter_icon_192_rtl);
        helicopterDrawable_front = ContextCompat.getDrawable(context, R.drawable.helicopter_icon_192_front);
        helicopterDrawable_rear = ContextCompat.getDrawable(context, R.drawable.helicopter_icon_192_rear);
        testPatternDrawable_20 = ContextCompat.getDrawable(context, R.drawable.test_pattern_20);
        closeBoxColor = ContextCompat.getColor(context, R.color.colorDisabledText);
        orangeColor = ContextCompat.getColor(context, R.color.colorOrange);
        saveSharingImage = false;
        mySharingFile = new File(getAlbumStorageDir(LOOK_UP), SHARING_IMAGE_NAME);
    }

    /**
     * Status bar takes up pixels at the top of the canvas; want to draw FABs with a margin below the status bar
     *
     * @param pContext application context given to GridSatView
     * @return the size of the status bar, or 0
     */
    private static int getStatusBarHeight(Context pContext) {
        Resources resources = pContext.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public void setZoomScaleFactor(double scaleFactor) { zoomScaleFactor = scaleFactor;}
    public double getZoomScaleFactor() { return this.zoomScaleFactor;}
    public void setLosElDeg(float losElDeg){this.losElDeg = losElDeg;}
    public float getLosElDeg(){return losElDeg;}
    public void setScreenRotation(float screenRotation){this.screenRotation = screenRotation;}
    public float getScreenRotation(){return screenRotation;}
    public void setLosAzDeg(float losAzDeg){this.losAzDeg = losAzDeg;}
    public float getLosAzDeg(){return losAzDeg;}
    public  float getPixelPerDegree(){return  pixPerDeg;}
    public boolean isLiveMode() {return liveMode;}
    public void setLiveMode(boolean liveMode) {this.liveMode = liveMode;}
    public void setAzelConfigCorrection(float azelConfigCorrection) {
        this.azelConfigCorrection = azelConfigCorrection;
    }

    private int dpToPixel(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void drawPlayPauseButton(Canvas mCanvas) {
        Drawable d;
        if (liveMode) {
            d = pauseButtonDrawable;
        } else {
            d = playButtonDrawable;
        }
        int right = mCanvasWidth - fabMarginPixel;
        int left = right - fabDiameterPixel;
        int top = fabMarginPixel;
        int bottom = top + fabDiameterPixel;
        d.setBounds(left, top, right, bottom);
        d.draw(mCanvas);
    }
    private void drawMenuButton(Canvas mCanvas){
        int left = fabMarginPixel;
        int right = left + fabDiameterPixel;
        int top = fabMarginPixel;
        int bottom = top + fabDiameterPixel;
        menuButtonDrawable.setBounds(left, top, right, bottom);
        menuButtonDrawable.draw(mCanvas);
    }

    /**
     * test if the touchDown click is in the play/pause floating action button
     *
     * @return true if click is in the rectangle
     */
    public boolean clickIsInPlayPauseButton() {
        int right = mCanvasWidth - fabMarginPixel;
        int left = right - fabDiameterPixel;
        int top = fabMarginPixel+ statusBarHeight;
        int bottom = top + fabDiameterPixel;
        RectF ppButtonRect = new RectF(left, top, right, bottom);
        return ppButtonRect.contains(touchDownX, touchDownY);

    }

    /**
     * test if the touchDown click is in the menu floating action button
     *
     * @return true if click is in the rectangle
     */
    public boolean clickIsInMenuButton() {
        int left = fabMarginPixel;
        int right = left + fabDiameterPixel;
        int top = fabMarginPixel+ statusBarHeight;
        int bottom = top + fabDiameterPixel;
        RectF menuButtonRect = new RectF(left, top, right, bottom);
        return menuButtonRect.contains(touchDownX, touchDownY);
    }

    public boolean isClickInAirplaneInfoCloseBox() {
        int right = mCanvasWidth;
        int left = right - fabDiameterPixel;
        int top = mCanvasHeight - 13 * DRAW_DATA_TEXT_SIZE;
        int bottom = top + fabDiameterPixel;
        RectF closeButtonRect = new RectF(left, top, right, bottom);
/*        if (DEBUG) {
            Log.i(this.getClass().getName(), String.format("isClickInAirplaneInfoCloseBox touchDownX: " +
                            "%f touchDownY: %f statusBarHeight: %d top: %d bottom: %d left: %d right: %d",
                    touchDownX, touchDownY, statusBarHeight, top, bottom, left, right));
        }*/
        return closeButtonRect.contains(touchDownX, touchDownY - statusBarHeight);
    }
    public boolean isClickInMoreInfoBox() {
        Paint textPaint = new Paint();
        int top = mCanvasHeight - DRAW_DATA_TEXT_SIZE - 5;
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        int left = (int) (mCanvasWidth - textPaint.measureText(CHARACTER_LEN_25));
        int bottom = top + fabDiameterPixel;
        int right = (int) (left + textPaint.measureText(CHARACTER_LEN_25));
/*        if (DEBUG) {
            Log.i(this.getClass().getName(), String.format("isClickInMoreInfoBox touchDownX-Y: " +
                            "%s-%s statusBarHeight: %d top: %d bottom: %d left: %d right: %d",
                    String.format(FORMAT3_1, touchDownX), String.format(FORMAT3_1, touchDownY), statusBarHeight, top, bottom, left, right));
        }*/
        RectF closeButtonRect = new RectF(left, top, right, bottom);
        return closeButtonRect.contains(touchDownX, touchDownY);
    }
    /**
     * When switching from paused mode to LiveMode, reset the pan values to zero,
     * so satellites appear at their true az/el locations
     */
    public void resetPan() {
        tempPanAz = 0;
        tempPanEl = 0;
        panEl = 0;
        panAz = 0;
    }

    public void setPreviewImageZoomScaleFactor(double previewImageZoomScaleFactor) {
        this.previewImageZoomScaleFactor = previewImageZoomScaleFactor;
    }

    public double getPreviewImageZoomScaleFactor() {
        return previewImageZoomScaleFactor;
    }

    public void setPreviewImageRotation(boolean rotatePreviewImage) {
        this.rotatePreviewImage = rotatePreviewImage;
    }

    public int getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(int locationStatus) {
        String locStatStr = "?";
        switch (locationStatus) {
            case 0:
                locStatStr = "UNKNOWN";
                break;
            case 1:
                locStatStr = "NONE";
                break;
            case 2:
                locStatStr = "OLD";
                break;
            case 3:
                locStatStr = "OKAY";
                break;
        }
        this.locationStatus = locationStatus;
    }

    public double getPausedZoomFactor() {
        return pausedZoomFactor;
    }

    public void setPausedZoomFactor(double pausedZoomFactor) {
        this.pausedZoomFactor = pausedZoomFactor;
    }

    public double getTempPausedZoomFactor() {
        return tempPausedZoomFactor;
    }

    public void setTempPausedZoomFactor(double tempPausedZoomFactor) {
        this.tempPausedZoomFactor = tempPausedZoomFactor;
    }

    public double getTempCameraZoomFactor() {
        return tempCameraZoomFactor;
    }

    public void setTempCameraZoomFactor(double tempCameraZoomFactor) {
        this.tempCameraZoomFactor = tempCameraZoomFactor;
    }
    @SuppressLint("DefaultLocale")
    private void showAirplaneData(Canvas canvas) {
        if ((ZERO).equals(selectedAirplaneicao24)) {
            return;
        }
        float topOffset = mCanvasHeight - 13 * DRAW_DATA_TEXT_SIZE + 5;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAlpha(ALPHA);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        float dataRightTextOffset = mCanvasWidth - textPaint.measureText(CHARACTER_LEN_25);
        RectF rect = new RectF(dataRightTextOffset - DRAW_DATA_TEXT_SIZE, topOffset - X_TEXT_SIZE, mCanvasWidth - 5, mCanvasHeight - 5);
        canvas.drawRoundRect(rect, 10, 10, textPaint);
        textPaint.setColor(closeBoxColor);
        textPaint.setTextSize(X_TEXT_SIZE);
        int xOffset = mCanvasWidth - fabMarginPixel - 5;
        canvas.drawText("X", xOffset, topOffset, textPaint);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        textPaint.setAlpha(255);
        //        Log.i(this.getClass().getName(), String.format("showAirplaneData for hex icao24: %s reg: %s owner: %s Manuf: %s model: %s ",
//                selectedAirplaneicao24, airplaneDBData.get(DB_KEY_AIR_REGISTRATION), airplaneDBData.get(DB_KEY_AIR_OWNER),
//                airplaneDBData.get(DB_KEY_AIR_MANUFACTURER), airplaneDBData.get(DB_KEY_AIR_MODEL)));
        String tailNumber = airplaneDBData.get(DB_KEY_AIR_REGISTRATION);
        if (tailNumber == null){ tailNumber = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(tailNumber, (dataRightTextOffset), topOffset, textPaint);
        String owner = airplaneDBData.get(DB_KEY_AIR_OWNER);
        //todo put this on two lines if .length() > 25
        if (owner == null){ owner = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(owner, (dataRightTextOffset), topOffset, textPaint);

        String manufacturer = airplaneDBData.get(DB_KEY_AIR_MANUFACTURER);
        if (manufacturer == null){ manufacturer = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(manufacturer, (dataRightTextOffset), topOffset, textPaint);

        String model = airplaneDBData.get(DB_KEY_AIR_MODEL);
        if (model == null){ model = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(model, (dataRightTextOffset), topOffset, textPaint);

        String country = airplaneData.getSv().getOriginCountry();
        if (country == null){ country = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(country, (dataRightTextOffset), topOffset, textPaint);

        String callsign = airplaneData.getSv().getCallsign();
        if (callsign == null){ callsign = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(callsign, (dataRightTextOffset), topOffset, textPaint);

        String airline = getAirlineFromCallsign(airplaneData.getSv().getCallsign());
        if (airline == null){ airline = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(airline, (dataRightTextOffset), topOffset, textPaint);
        String velocityLabel = "%s mph";
        String altitudeLabel = "%s'";
        String distanceLabel = "%s mi";
        String headingLabel = "%s" + DEG_SYMBOL;

        double altitudeMultiplier = foot_per_meter;
        double distMultiplier = mile_per_meter;
        double speedMultiplier = mph_per_mps;
        if (Integer.parseInt(distanceUnit) == DISTANCE_TYPE_METRIC) {
            speedMultiplier = kph_per_mps;
            distMultiplier = km_per_meter;
            altitudeMultiplier = 1;
            velocityLabel = "%s kph";
            altitudeLabel = "%s m";
            distanceLabel = "%s km";
        }
        String velocity = String.format(velocityLabel,
                String.format(FORMAT_3_0F, airplaneData.getSv().getVelocity() * speedMultiplier));
        if (velocity == null){ velocity = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(velocity, (dataRightTextOffset), topOffset, textPaint);

        String altitude = String.format(altitudeLabel,
        String.format(FORMAT_4_0F, airplaneData.getAltitude() * altitudeMultiplier));
        if (altitude == null){ altitude = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(altitude, (dataRightTextOffset), topOffset, textPaint);

        String distance = String.format(distanceLabel,
        String.format(FORMAT_2_0F, airplaneData.getDistance() * distMultiplier));
        if (distance == null){ distance = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(distance, (dataRightTextOffset), topOffset, textPaint);

        String heading = String.format(headingLabel, String.format(FORMAT_3_0F, airplaneData.getSv().getHeading()));
        if (heading == null){ heading = Constants.DOUBLE_DASH; }
        topOffset += DRAW_DATA_TEXT_SIZE;
        canvas.drawText(heading, (dataRightTextOffset), topOffset, textPaint);
        drawMoreInfoBox(canvas, textPaint);
    }

    private void drawMoreInfoBox(Canvas canvas, Paint textPaint) {
        float topOffset;
        float dataRightTextOffset;
        String moreInfo = Constants.MORE_INFO;
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        topOffset = mCanvasHeight - DRAW_DATA_TEXT_SIZE;
        dataRightTextOffset = mCanvasWidth - textPaint.measureText(CHARACTER_LEN_25) / 2 - textPaint.measureText(moreInfo) / 2;
        canvas.drawText(moreInfo, (dataRightTextOffset), topOffset, textPaint);
    }

    public void setSelectedAirplaneicao24(String selectedAirplaneicao24) {
        this.selectedAirplaneicao24 = selectedAirplaneicao24;
    }

    public String getSelectedAirplaneicao24( ) {
        return selectedAirplaneicao24;
    }
    private class FindAirplaneXY {
        private final float losElDeg;
        private final float losAzDeg;
        private final Airplane aPlane;
        private int startX;
        private int startY;

        FindAirplaneXY(float losElDeg, float losAzDeg, Airplane aPlane) {
            this.losElDeg = losElDeg;
            this.losAzDeg = losAzDeg;
            this.aPlane = aPlane;
        }

        int getStartX() {
            return startX;
        }

        int getStartY() {
            return startY;
        }

        FindAirplaneXY invoke() {
            double deltaAz = (losAzDeg - aPlane.getLookAngleAz());
            if (deltaAz > 180) {
                deltaAz -= _360;
            } else if (deltaAz < -180) {
                deltaAz += _360;
            }
            startX = (int) Math.round(mCanvasWidth / 2 - deltaAz * pixPerDeg);
            startY = (int) Math.round(mCanvasHeight / 2 + (losElDeg - aPlane.getLookAngleEl()) * pixPerDeg);
            return this;
        }
    }
}
