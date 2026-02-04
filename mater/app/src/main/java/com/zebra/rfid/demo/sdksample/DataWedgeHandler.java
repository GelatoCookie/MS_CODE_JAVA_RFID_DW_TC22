package com.zebra.rfid.demo.sdksample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

/**
 * Handler for Zebra DataWedge Intent API.
 */
public class DataWedgeHandler {
    private static final String TAG = "DataWedgeHandler";

    // DataWedge Intent API constants
    private static final String ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION";
    private static final String EXTRA_GET_VERSION_INFO = "com.symbol.datawedge.api.GET_VERSION_INFO";
    private static final String EXTRA_REGISTER_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION";
    private static final String EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG";
    
    private static final String NOTIFICATION_ACTION = "com.symbol.datawedge.api.NOTIFICATION_ACTION";
    private static final String RESULT_ACTION = "com.symbol.datawedge.api.RESULT_ACTION";
    private static final String RESULT_CATEGORY = "android.intent.category.DEFAULT";

    // Profile constants
    private static final String PROFILE_NAME = "RFIDSampleProfile";
    private static final String BARCODE_ACTION = "com.zebra.rfid.demo.sdksample.RECV_BARCODE";

    private final MainActivity context;

    public DataWedgeHandler(MainActivity context) {
        this.context = context;
    }

    public void onCreate() {
        registerReceivers();
        createDataWedgeProfile();
        getDWVersion();
        registerForNotifications();
    }

    public void onDestroy() {
        try {
            context.unregisterReceiver(dwReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(RESULT_ACTION);
        filter.addAction(NOTIFICATION_ACTION);
        filter.addAction(BARCODE_ACTION);
        filter.addCategory(RESULT_CATEGORY);
        context.registerReceiver(dwReceiver, filter);
    }

    /**
     * Creates and configures a DataWedge profile programmatically.
     */
    private void createDataWedgeProfile() {
        Bundle profileConfig = new Bundle();
        profileConfig.putString("PROFILE_NAME", PROFILE_NAME);
        profileConfig.putString("PROFILE_ENABLED", "true");
        profileConfig.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");

        // Associate profile with this app
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", context.getPackageName());
        appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});
        profileConfig.putParcelableArray("APP_LIST", new Bundle[]{appConfig});

        ArrayList<Bundle> pluginConfigs = new ArrayList<>();

        // Configure BARCODE Input
        Bundle barcodeConfig = new Bundle();
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE");
        barcodeConfig.putString("RESET_CONFIG", "true");
        Bundle barcodeProps = new Bundle();
        barcodeProps.putString("scanner_selection", "auto");
        // Enable common decoders by default
        barcodeProps.putString("decoder_code128", "true");
        barcodeProps.putString("decoder_code39", "true");
        barcodeProps.putString("decoder_ean8", "true");
        barcodeProps.putString("decoder_ean13", "true");
        barcodeProps.putString("decoder_upca", "true");
        barcodeProps.putString("decoder_upce0", "true");
        barcodeProps.putString("decoder_pdf417", "true");
        barcodeProps.putString("decoder_qrcode", "true");
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps);
        pluginConfigs.add(barcodeConfig);

        // Configure RFID Input (Disable by default)
        Bundle rfidConfig = new Bundle();
        rfidConfig.putString("PLUGIN_NAME", "RFID");
        rfidConfig.putString("RESET_CONFIG", "true");
        Bundle rfidProps = new Bundle();
        rfidProps.putString("rfid_input_enabled", "false");
        rfidConfig.putBundle("PARAM_LIST", rfidProps);
        pluginConfigs.add(rfidConfig);

        // Configure INTENT Output
        Bundle intentConfig = new Bundle();
        intentConfig.putString("PLUGIN_NAME", "INTENT");
        intentConfig.putString("RESET_CONFIG", "true");
        Bundle intentProps = new Bundle();
        intentProps.putString("intent_output_enabled", "true");
        intentProps.putString("intent_action", BARCODE_ACTION);
        intentProps.putString("intent_delivery", "2"); // 2 = Broadcast Intent
        intentConfig.putBundle("PARAM_LIST", intentProps);
        pluginConfigs.add(intentConfig);

        // Add plugins to profile config
        profileConfig.putParcelableArrayList("PLUGIN_CONFIG", pluginConfigs);

        sendDataWedgeIntentWithExtra(EXTRA_SET_CONFIG, profileConfig);
        Log.d(TAG, "Created/Configured DataWedge Profile: " + PROFILE_NAME + " (RFID Disabled, Default Decoders Enabled)");
    }

    private void getDWVersion() {
        sendDataWedgeIntentWithExtra(EXTRA_GET_VERSION_INFO, "");
    }

    private void registerForNotifications() {
        Bundle b = new Bundle();
        b.putString("com.symbol.datawedge.api.APPLICATION_NAME", context.getPackageName());
        b.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS");
        sendDataWedgeIntentWithExtra(EXTRA_REGISTER_NOTIFICATION, b);
    }

    private void sendDataWedgeIntentWithExtra(String extraKey, Object extraValue) {
        Intent i = new Intent();
        i.setAction(ACTION_DATAWEDGE);
        if (extraValue instanceof String) {
            i.putExtra(extraKey, (String) extraValue);
        } else if (extraValue instanceof Bundle) {
            i.putExtra(extraKey, (Bundle) extraValue);
        }
        context.sendBroadcast(i);
    }

    private final BroadcastReceiver dwReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (action.equals(RESULT_ACTION)) {
                if (intent.hasExtra("com.symbol.datawedge.api.RESULT_GET_VERSION_INFO")) {
                    Bundle versionInfo = intent.getBundleExtra("com.symbol.datawedge.api.RESULT_GET_VERSION_INFO");
                    String dwVersion = versionInfo.getString("DATAWEDGE");
                    Log.d(TAG, "DataWedge Version: " + dwVersion);
                }
            } else if (action.equals(NOTIFICATION_ACTION)) {
                if (intent.hasExtra("com.symbol.datawedge.api.NOTIFICATION")) {
                    Bundle b = intent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION");
                    String status = b.getString("STATUS");
                    Log.d(TAG, "Scanner Status: " + status);
                }
            } else if (action.equals(BARCODE_ACTION)) {
                String barcode = intent.getStringExtra("com.symbol.datawedge.data_string");
                if (barcode != null && DataWedgeHandler.this.context != null) {
                    DataWedgeHandler.this.context.barcodeData(barcode);
                }
            }
        }
    };
}
