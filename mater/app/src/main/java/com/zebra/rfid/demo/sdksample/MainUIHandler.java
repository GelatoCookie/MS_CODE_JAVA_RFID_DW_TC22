package com.zebra.rfid.demo.sdksample;

import android.app.Activity;
import android.content.Context;
import com.zebra.rfid.api3.TagData;

/**
 * Abstract class to handle all UI updates from background handlers.
 * It acts as a bridge between the hardware logic and the UI components.
 */
public abstract class MainUIHandler {
    
    protected final Activity activity;

    public MainUIHandler(Activity activity) {
        this.activity = activity;
    }

    public enum UpdateType {
        READER_STATUS,
        SCAN_BUTTON_STATE,
        TAG_DATA,
        TRIGGER_PRESS,
        BARCODE_DATA,
        TOAST_MESSAGE
    }

    /**
     * Dispatch an update to the UI thread.
     */
    public void sendUpdate(UpdateType type, Object... data) {
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> handleUIUpdate(type, data));
        }
    }

    /**
     * Abstract method implemented by MainActivity to touch specific UI Views.
     */
    protected abstract void handleUIUpdate(UpdateType type, Object... data);

    // Helper methods to simplify calls from Handlers
    public void updateStatus(String message, boolean connected) {
        sendUpdate(UpdateType.READER_STATUS, message, connected);
    }

    public void showToast(String message) {
        sendUpdate(UpdateType.TOAST_MESSAGE, message);
    }

    public void handleBarcode(String barcode) {
        sendUpdate(UpdateType.BARCODE_DATA, barcode);
    }

    public void handleTags(TagData[] tags) {
        sendUpdate(UpdateType.TAG_DATA, (Object) tags);
    }

    public void handleTrigger(boolean pressed) {
        sendUpdate(UpdateType.TRIGGER_PRESS, pressed);
    }

    public Context getContext() {
        return activity;
    }
}
