package com.dealrinc.gmvScanner;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import javax.security.auth.callback.Callback;


/**
 * This class echoes a string called from JavaScript.
 */
public class CDVAndroidScanner extends CordovaPlugin 
{
    protected CallbackContext mCallbackContext;
    
    private TorchStateChangeReceiver mReceiver;
    
    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String BARCODE_TYPE = "barcodeType";
    private static final String TORCH_TYPE = "torchType";

    public static final int TORCH_RETURN = 5387;


    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Context context = cordova.getActivity().getApplicationContext();
        mCallbackContext = callbackContext;
        mReceiver = new TorchStateChangeReceiver(new Handler());
        
        mReceiver.setReceiver(new TorchStateChangeReceiver.Receiver(){
        	@Override
        	public void onReceiveResult(int resultCode, Bundle data)
        	{
                JSONArray result = new JSONArray();
                boolean flashState = data.getFloat("flashState") == 1 ? true : false;
                result.put(TORCH_TYPE);
                result.put(flashState);

                PluginResult pRes = new PluginResult(PluginResult.Status.OK, result);
                pRes.setKeepCallback(true);
                
                mCallbackContext.sendPluginResult(pRes);

                Log.d("CDVAndroidScanner", "Torch State Changed: " + (flashState?"On":"Off"));
        	}
        });

        
		if (action.equals("startScan")) {

            class OneShotTask implements Runnable {
                private Context context;
                private JSONArray args;
                private OneShotTask(Context ctx, JSONArray as) { context = ctx; args = as; }
                public void run() {
                    openNewActivity(context, args);
                }
            }
            Thread t = new Thread(new OneShotTask(context, args));
            t.start();
            return true;
        }
        return false;
    }

    private void openNewActivity(Context context, JSONArray args) {
		Intent intent = new Intent(context, SecondaryActivity.class);
        intent.putExtra("DetectionTypes", args.optInt(0, 1234));
        intent.putExtra("ViewFinderWidth", args.optDouble(1, .5));
        intent.putExtra("ViewFinderHeight", args.optDouble(2, .7));
        intent.putExtra("TorchOn", args.optBoolean(3, false));
        intent.putExtra("Orientation", args.optInt(4, 0));
        intent.putExtra("Receiver", mReceiver);

        this.cordova.setActivityResultCallback(this);
        this.cordova.startActivityForResult(this, intent, RC_BARCODE_CAPTURE);
	}


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                Intent d = new Intent();
                
                if (data != null) {
                	Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    JSONArray result = new JSONArray();
                    result.put(BARCODE_TYPE);
                    result.put(barcode.rawValue);
                    result.put("");
                    mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));

                    Log.d("CDVAndroidScanner", "Barcode read: " + barcode.displayValue);
                }
            } else {
                String err = data.getStringExtra("err");
                JSONArray result = new JSONArray();
                result.put(err);
                result.put("");
                mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
            }
        }
    }
    
    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        mCallbackContext = callbackContext;
    }
    
/*
    private void startScan(CallbackContext callbackContext) {
		Intent intent = new Intent(this, MainActivity.class);
		//intent.putExtra(BarcodeCaptureActivity.AutoFocus, autoFocus.isChecked());
		//intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());

		startActivityForResult(intent, RC_BARCODE_CAPTURE);

        if (true) {
			callbackContext.success("Test response!!!!");
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }*/
}
