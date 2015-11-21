package fr.neamar.kiss;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class CameraHandler {
    public static final String TAG = "CameraHandler";
    private Camera camera = null;
    private SurfaceTexture surfaceTexture = null;
    private Boolean torchState = null;
    private Boolean torchAvailable = null;

    private void openCamera() throws IOException {
        if (camera == null) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (surfaceTexture == null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                surfaceTexture = new SurfaceTexture(0);
                camera.setPreviewTexture(surfaceTexture);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void releaseCamera() {
        try {
            if (camera != null) {
                camera.release();
                if (surfaceTexture != null) {
                    // Call only available for ICS+, but we've already made the check on openCamera
                    surfaceTexture.release();
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "unable to release camera " + ex);
        } finally {
            camera = null;
            surfaceTexture = null;
            torchState = false;
        }
    }

    public Boolean getTorchState() {

        try {
            if (torchState == null) {
                openCamera();
                Parameters parms = camera.getParameters();
                torchState = parms.getFlashMode().equals(Parameters.FLASH_MODE_TORCH);
            }
        } catch (Exception ex) {
            Log.e(TAG, "unable to get torch state " + ex);
            releaseCamera();
            torchState = false;
        }

        return torchState;
    }

    public void setTorchState(Boolean state) {
        try {
            openCamera();
            Parameters parms = camera.getParameters();
            if (state)
                parms.setFlashMode(Parameters.FLASH_MODE_TORCH);
            else
                parms.setFlashMode(Parameters.FLASH_MODE_OFF);

            camera.setParameters(parms);
            if (state) {//enable torch but retain camera
                camera.startPreview();
            } else { //disable torch and release camera
                camera.stopPreview();
                releaseCamera();
            }
            torchState = state;
        } catch (Exception ex) {
            releaseCamera();
            torchState = false;
        }
    }

    public boolean isTorchAvailable() {
        try {
            if (torchAvailable == null) {
                torchAvailable = false;
                openCamera();
                List<String> torchModes = camera.getParameters().getSupportedFlashModes();
                releaseCamera();

                //no flash
                if (torchModes == null)
                    return false;

                for (String mode : torchModes) {
                    if (mode.equalsIgnoreCase(Camera.Parameters.FLASH_MODE_TORCH))
                        torchAvailable = true;
                }
            }
        } catch (Exception ex) {
            torchAvailable = false;
        }

        return torchAvailable;
    }
}
