package com.example.cameras;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

     SurfaceHolder holder;
     Camera mCamera;


    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        /*Camera.Parameters params = mCamera.getParameters();
        if(this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE){
            params.set("orientation","portrait");
            mCamera.setDisplayOrientation(90);
            params.setRotation(90);
        }
        else {
            params.set("orientation","landscape");
            mCamera.setDisplayOrientation(0);
            params.setRotation(0);
        }

        List<String> focusModes = params.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        mCamera.setParameters(params);*/

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(parameters);

        try{
            mCamera.setPreviewDisplay(holder);
            mCamera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("CameraPreview: " ,"Error setting camera preview "+e.getMessage());
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if(holder.getSurface() == null){
            return;
        }

        try {
            mCamera.stopPreview();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        Activity activity = (Activity) getContext();
        setCameraDiaplayOrientation(activity,0,mCamera);

        try{
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("CameraPreview Changed: ","Error starting camera preview "+e.getMessage());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        holder.removeCallback(this);
        mCamera.release();
    }

    public static void setCameraDiaplayOrientation(Activity activity, int cameraId, Camera camera){

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId,info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation){

            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;

        }
        int result;
        if(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            result = (info.orientation + degrees)%360;
            result = (360-result) % 360;
        }
        else{
            result = (info.orientation + degrees)%360;
        }
        camera.setDisplayOrientation(result);
    }


}
