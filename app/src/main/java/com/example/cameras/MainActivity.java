package com.example.cameras;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String noc;
    public Camera mCamera;
    private CameraPreview mPreview;
    public long startTime;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Cameras");

        if(!mediaStorageDir.exists()){
            if(!mediaStorageDir.mkdirs()){
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if(type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath()+File.separator+"IMG_"+timeStamp+".jpg");
        }
        else if(type == MEDIA_TYPE_VIDEO){
            mediaFile = new File(mediaStorageDir.getPath()+File.separator+"VID_"+timeStamp+".mp4");
        }
        else{
            return null;
        }

        return mediaFile;

    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if(pictureFile == null){
                Log.d("onPictureTaken: ","Error creating media file, check storage permissions");
                return;
            }
            try{
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("onPictureTaken: ","File not Found: "+e.getMessage());
            } catch (IOException e) {
                Log.d("onPictureTaken:","Error accessing File: "+e.getMessage());
            }
            mCamera.startPreview();
        }
    };

    private Button capture,video;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capture = (Button) findViewById(R.id.capture);
        video = (Button) findViewById(R.id.video);
        newCameraInstance();
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(this,mCamera);
        preview.addView(mPreview);
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mCamera.cancelAutoFocus();
                    mCamera.stopPreview();
                    focusOnTouch(event);
                }
                return true;
            }
        });
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null,null,mPicture);
            }
        });
    }

    private void newCameraInstance(){
        if(mThread == null){
            mThread = new CameraHandlerThread();
        }
        synchronized(mThread){
            mThread.openCamera();
        }
    }

    private CameraHandlerThread mThread = null;
    private static class CameraHandlerThread extends HandlerThread{
        Handler mHandler = null;
        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }
        synchronized void notifyCameraOpened(){
            notify();
        }
        void openCamera(){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    getCameraInstance();
                    notifyCameraOpened();
                }
            });
            try{
                wait();
            } catch (InterruptedException e) {
                Log.w("MActivity openCamera: ","wait was interrupted");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //releaseMediaRecorder();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera.startPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //releaseMediaRecorder();
        releaseCamera();
    }

    /*private void releaseMediaRecorder(){
        if(mediaRecorder ! =null){
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            mCamera.lock();
        }
    }*/

    private void releaseCamera(){
        if(mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try{
            c = Camera.open();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return c;
    }

    private static  final int FOCUS_AREA_SIZE= 300;

    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null ) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0){
                Rect rect = calculateFocusArea(event.getX(), event.getY());
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(rect, 800));
                parameters.setFocusAreas(meteringAreas);
                parameters.setJpegQuality(100);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                mCamera.autoFocus(mAutoFocusTakePictureCallback);
            }else {
                mCamera.autoFocus(mAutoFocusTakePictureCallback);
            }
        }
    }
    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                // do something...
                Log.i("tap_to_focus","success!");
            } else {
                // do something...
                Log.i("tap_to_focus","fail!");
            }
        }
    };

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / mPreview.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / mPreview.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }

}
