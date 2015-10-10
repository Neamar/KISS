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

    private Camera camera = null;
    private SurfaceTexture surfaceTexture = null;
    private Boolean torchState = null;
    private Boolean torchAvailable = null;

    private void openCamera() throws IOException {
        if (camera == null) {
            Log.d("openCamera", "Open Camera");
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (surfaceTexture == null && android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                Log.d("openCamera", "Create dummy surface texture");
                surfaceTexture = new SurfaceTexture(0);
                camera.setPreviewTexture(surfaceTexture);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void releaseCamera() {
        try {
            if (camera != null) {
                Log.d("releaseCamera", "Release Camera");
                camera.release();
                if (surfaceTexture != null) {
                    Log.d("releaseCamera", "Release dummy surface texture");
                    // Call only available for ICS+, but we've already made the check on openCamera
                    surfaceTexture.release();
                }
            }
        } catch (Exception ex) {
            Log.e("releaseCamera", "unable to release camera " + ex);
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
            Log.d("getTorchState", "Current torch state " + torchState);
        } catch (Exception ex) {
            Log.e("getTorchState", "unable to get torch states " + ex);
            releaseCamera();
            torchState = false;
        } finally {
            return torchState;
        }

    }

    public void setTorchState(Boolean state) {
        try {
            openCamera();
            Log.d("setTorchState", "Set torch state " + state);
            Parameters parms = camera.getParameters();
            if (state)
                parms.setFlashMode(Parameters.FLASH_MODE_TORCH);
            else
                parms.setFlashMode(Parameters.FLASH_MODE_OFF);

            camera.setParameters(parms);
            if (state) {//enable torch but retain camera
                Log.d("setTorchState", "Start preview to light on flash");
                camera.startPreview();
            } else { //disable torch and release camera
                Log.d("setTorchState", "Stop preview to light off flash");
                camera.stopPreview();
                releaseCamera();
            }
            torchState = state;
        } catch (Exception ex) {
            Log.e("wtf", "unable to set torch state " + state + " " + ex);
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
            if (!torchAvailable)
                Log.d("isTorchAvailable", "Torch mode not available");

        } catch (Exception ex) {
            Log.e("isTorchAvailable", "unable to check if torch is available " + ex);
            torchAvailable = false;
        } finally {
            return torchAvailable;
        }
    }
}
