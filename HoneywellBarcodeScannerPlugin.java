package za.co.palota.honeywell;

import android.content.Context;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HoneywellBarcodeScannerPlugin extends CordovaPlugin implements BarcodeReader.BarcodeListener {
    private static final String TAG = "HoneywellBarcodeScanner";

    private CallbackContext callbackContext;
    private static BarcodeReader barcodeReader;
    private AidcManager manager;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Context context = cordova.getActivity().getApplicationContext();

        AidcManager.create(context, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
                if (barcodeReader != null) {

                    // register bar code event listener
                    barcodeReader.addBarcodeListener(HoneywellBarcodeScannerPlugin.this);

                    Map<String, Object> properties = new HashMap<String, Object>();
                    // Set Symbologies On/Off
                    properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
                    properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, true);
                    // Set Max Code 39 barcode length
                    properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
                    // Turn on center decoding
                    properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
                    // Enable bad read response
                    properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
                    // Apply the settings
                    barcodeReader.setProperties(properties);

                    // If you want to load properties from profile name "DEFAULT"
                    barcodeReader.loadProfile("DEFAULT");
                    // after loading profile it stopped giving barcode events so I had to do the following
                    Map<String, Object> newProps = new HashMap<String, Object>();
                    for (Map.Entry<String, Object> entry : barcodeReader.getAllProperties().entrySet()) {
                        // Apparently this property is not included in BarcodeReader.class but honeywell SDK returns this also when we call getAllProperties which causes issues
                        if (entry.getKey().equalsIgnoreCase("DPR_WEDGE"))
                            continue;
                         newProps.put(entry);
                    }
                    // You will need to reintialize barcode reader before setting new properties
                    reinitializeBarcodeReader();
                    barcodeReader.setProperties(newProps);
                    try {
                        barcodeReader.claim();
                    } catch (ScannerUnavailableException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext)
            throws JSONException {
        if (action.equals("onBarcodeScanned")) {
            this.callbackContext = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            this.callbackContext.sendPluginResult(result);
        }
        return true;
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                NotifyError("Scanner unavailable");
            }
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        if (barcodeReader != null) {
            // release the scanner claim so we don't get any scanner
            // notifications while paused.
            barcodeReader.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (barcodeReader != null) {
            // unregister barcode event listener
            barcodeReader.removeBarcodeListener(this);
            barcodeReader.close();
            barcodeReader = null;
        }

        if (manager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            manager.close();
        }
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent failureEvent) {
        NotifyError("Barcode failed");
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        if (this.callbackContext != null) {
            try {
                JSONObject response = new JSONObject();

                response.put("data", event.getBarcodeData());
                response.put("code", event.getCodeId());
                response.put("charset", event.getCharset());
                response.put("aimId", event.getAimId());
                response.put("timestamp", event.getTimestamp());

                PluginResult result = new PluginResult(PluginResult.Status.OK, response);
                result.setKeepCallback(true);
                this.callbackContext.sendPluginResult(result);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void NotifyError(String error) {
        if (this.callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
            result.setKeepCallback(true);
            this.callbackContext.sendPluginResult(result);
        }
    }

    public void reinitializeBarcodeReader() {
        if (barcodeReader != null) {
            barcodeReader.release();
            barcodeReader.removeBarcodeListener(this);
            barcodeReader.close();
            barcodeReader = null;

            if (manager != null) {
                try {
                    barcodeReader = manager.createBarcodeReader();
                    barcodeReader.addBarcodeListener(HoneywellBarcodeScannerPlugin.this);
                    barcodeReader.claim();
                } catch (InvalidScannerNameException | ScannerUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
