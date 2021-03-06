package com.mbppower;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.content.pm.PackageManager;
import java.util.Arrays;

public class CameraPreview extends CordovaPlugin implements CameraActivity.CameraPreviewListener {

	private final String TAG = "CameraPreview";
	private final String setOnPictureTakenHandlerAction = "setOnPictureTakenHandler";
	private final String startCameraAction = "startCamera";
	private final String stopCameraAction = "stopCamera";
	private final String switchCameraAction = "switchCamera";
	private final String takePictureAction = "takePicture";
	private final String showCameraAction = "showCamera";
	private final String hideCameraAction = "hideCamera";
	private final String setFlashModeAction = "setFlashMode";

	private CameraActivity fragment;
	private CallbackContext takePictureCallbackContext;
	private int containerViewId = 1;

    private final String [] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final int permissionsReqId = 0;
    private CallbackContext execCallback;
    private JSONArray execArgs;

	public CameraPreview(){
		super();
		Log.d(TAG, "Constructing");
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		if (setOnPictureTakenHandlerAction.equals(action)){
			return setOnPictureTakenHandler(args, callbackContext);
		}
		/*else if (startCameraAction.equals(action)){
			return startCamera(args, callbackContext);
		}*/
        else if (startCameraAction.equals(action)) {
            if (cordova.hasPermission(permissions[0])) {
                return startCamera(args, callbackContext);
            }
            else {
                execCallback = callbackContext;
                execArgs = args;
                cordova.requestPermissions(this, permissionsReqId, permissions);
            }
        }
		else if (takePictureAction.equals(action)){
			return takePicture(args, callbackContext);
		}
		else if (stopCameraAction.equals(action)){
			return stopCamera(args, callbackContext);
		}
		else if (hideCameraAction.equals(action)){
			return hideCamera(args, callbackContext);
		}
		else if (showCameraAction.equals(action)){
			return showCamera(args, callbackContext);
		}
		else if (switchCameraAction.equals(action)){
			return switchCamera(args, callbackContext);
		}
		else if (setFlashModeAction.equals(action)) {
			return setFlashMode(args, callbackContext);
		}

		return false;
	}

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        for(int r:grantResults) {
            if(r == PackageManager.PERMISSION_DENIED) {
                execCallback.sendPluginResult(new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION));
                return;
            }
        }
        if (requestCode == permissionsReqId) {
            startCamera(execArgs, execCallback);
        }
    }

	private boolean startCamera(final JSONArray args, final CallbackContext callbackContext) {
		if(fragment != null){
			PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
			pluginResult.setKeepCallback(true);
			callbackContext.sendPluginResult(pluginResult);
			return false;
		}
		fragment = new CameraActivity();
		fragment.setEventListener(this);

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {

				try {
					DisplayMetrics metrics = cordova.getActivity().getResources().getDisplayMetrics();
					int x = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(0), metrics);
					int y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(1), metrics);
					int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(2), metrics);
					int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, args.getInt(3), metrics);
					String defaultCamera = args.getString(4);
					Boolean toBack = args.getBoolean(5);
					int lockRotation = args.getInt(6);
					String filePrefix = args.isNull(8) ? "picture" : args.getString(8);
					fragment.defaultCamera = defaultCamera;
					fragment.setRect(x, y, width, height);
                    fragment.lockRotation = lockRotation;
					fragment.filePrefix = filePrefix;

					//create or update the layout params for the container view
					FrameLayout containerView = (FrameLayout)cordova.getActivity().findViewById(containerViewId);
					if(containerView == null){
						containerView = new FrameLayout(cordova.getActivity().getApplicationContext());
						containerView.setId(containerViewId);

						FrameLayout.LayoutParams containerLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
						cordova.getActivity().addContentView(containerView, containerLayoutParams);
					}
					//display camera bellow the webview
					if(toBack){
						webView.getView().setBackgroundColor(0x00000000);
						((ViewGroup)webView.getView()).bringToFront();
					}
					else{
						//set camera back to front
						containerView.setAlpha(Float.parseFloat(args.getString(7)));
						containerView.bringToFront();
					}

					//add the fragment to the container
					FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
					FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
					fragmentTransaction.add(containerView.getId(), fragment);
					fragmentTransaction.commit();
					PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                    pluginResult.setKeepCallback(false);
					callbackContext.sendPluginResult(pluginResult);
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		});
		return true;
	}
	private boolean takePicture(final JSONArray args, CallbackContext callbackContext) {
		if(fragment == null){
			PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
			pluginResult.setKeepCallback(true);
			callbackContext.sendPluginResult(pluginResult);
			return false;
		}

		try {
			double maxWidth = args.getDouble(0);
			double maxHeight = args.getDouble(1);
			fragment.takePicture(maxWidth, maxHeight);
			PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
			pluginResult.setKeepCallback(false);
			callbackContext.sendPluginResult(pluginResult);
		}
		catch(Exception e){
			e.printStackTrace();
			PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR);
			pluginResult.setKeepCallback(true);
			callbackContext.sendPluginResult(pluginResult);
			return false;
		}
		return true;
	}

	public void onPictureTaken(String originalPicturePath){
		JSONArray data = new JSONArray();
		data.put(originalPicturePath);
		PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
		pluginResult.setKeepCallback(true);
		takePictureCallbackContext.sendPluginResult(pluginResult);
	}

	private boolean stopCamera(final JSONArray args, CallbackContext callbackContext) {
		if(fragment == null){
			return false;
		}

		FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.remove(fragment);
		fragmentTransaction.commit();
		fragment = null;

		return true;
	}

	private boolean showCamera(final JSONArray args, CallbackContext callbackContext) {
		if(fragment == null){
			return false;
		}

		FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.show(fragment);
		fragmentTransaction.commit();

		return true;
	}
	private boolean hideCamera(final JSONArray args, CallbackContext callbackContext) {
		if(fragment == null) {
			return false;
		}

		FragmentManager fragmentManager = cordova.getActivity().getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.hide(fragment);
		fragmentTransaction.commit();

		return true;
	}
	private boolean switchCamera(final JSONArray args, CallbackContext callbackContext) {
		if(fragment == null){
			return false;
		}
		fragment.switchCamera();
		return true;
	}

	private boolean setOnPictureTakenHandler(JSONArray args, CallbackContext callbackContext) {
		Log.d(TAG, "setOnPictureTakenHandler");
		takePictureCallbackContext = callbackContext;
		return true;
	}

	private boolean setFlashMode(JSONArray args, CallbackContext callbackContext) {
		try {
			int mode = args.getInt(0);
			switch(mode) {
				case 0:
					fragment.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
					break;
				case 1:
					fragment.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
					break;
				case 2:
					fragment.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					break;
				case 3:
					fragment.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
					break;
				default:
					throw new IllegalArgumentException(String.format("Unknow flash mode %d", mode));
			}
			return true;
		} catch (JSONException ex) {
			throw new IllegalArgumentException(ex);
		}
	}
}
