package com.zebra.rfid.demo.sdksample;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.IRFIDLogger;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.scannercontrol.DCSSDKDefs;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler class for RFID operations.
 * This class encapsulates the Zebra RFID API logic and manages the reader lifecycle.
 */
class RFIDHandler implements Readers.RFIDReaderEventHandler {

    private static final String TAG = "RFID_SAMPLE";
    private Readers readers;
    private ArrayList<ReaderDevice> availableRFIDReaderList;
    private RFIDReader reader;
    private EventHandler eventHandler;
    private MainActivity context;
    private MainUIHandler uiHandler;
    private SDKHandler sdkHandler;
    private ScannerHandler scannerHandler;
    private ArrayList<DCSScannerInfo> scannerList;
    private int scannerID;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private int connectionTimer = 0;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (uiHandler != null) {
                uiHandler.updateStatus("Connecting... " + connectionTimer++ + "s", false);
                timerHandler.postDelayed(timerRunnable, 1000);
            }
        }
    };
    
    /** Executor for background tasks to avoid blocking the UI thread. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);
        void handleTriggerPress(boolean pressed);
        void barcodeData(String val);
        void sendToast(String val);
    }

    /**
     * Initializes the RFIDHandler with the activity context.
     * @param activity The MainActivity context.
     * @param uiHandler The UI handler for status updates.
     */
    void onCreate(MainActivity activity, MainUIHandler uiHandler) {
        this.context = activity;
        this.uiHandler = uiHandler;
        this.scannerList = new ArrayList<>();
        this.scannerHandler = new ScannerHandler(activity);
        InitSDK();
    }

    /**
     * Resets the reader settings to defaults.
     * @return Success or error message.
     */
    public String Defaults() {
        if (!isReaderConnected()) return "Not connected";
        try {
            int maxPower = 270;
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(maxPower);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);

            Antennas.SingulationControl singulationControl = reader.Config.Antennas.getSingulationControl(1);
            singulationControl.setSession(SESSION.SESSION_S0);
            singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, singulationControl);
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error in Defaults", e);
            return e.getMessage();
        }
        return "Default settings applied";
    }

    public String Test1() {
        return "Test1 called";
    }

    public String Test2() {
        return "Test2 called";
    }

    public void testFunction() {
        Log.d(TAG, "testFunction called");
    }

    private boolean isReaderConnected() {
        return reader != null && reader.isConnected();
    }

    /**
     * Toggles the connection to the reader.
     * If connected, it disconnects. If disconnected, it attempts to connect.
     */
    public void toggleConnection() {
        if (isReaderConnected()) {
            executor.execute(this::disconnect);
        } else {
            connectReader();
        }
    }

    void onResume() {
        // Re-establish connection or reader object on resume
        connectReader();
    }

    void onPause() {
        // Disconnect to release resources when app is in background
        executor.execute(this::disconnect);
    }

    void onDestroy() {
        executor.execute(() -> {
            dispose();
            executor.shutdown();
        });
    }

    private void InitSDK() {
        if (readers != null) {
            connectReader();
            return;
        }

        executor.execute(() -> {
            try {
                readers = new Readers(context, ENUM_TRANSPORT.SERVICE_USB);
                ENUM_TRANSPORT[] transports = {
                        ENUM_TRANSPORT.SERVICE_USB,
                        ENUM_TRANSPORT.RE_SERIAL,
                        ENUM_TRANSPORT.RE_USB,
                        ENUM_TRANSPORT.BLUETOOTH,
                        ENUM_TRANSPORT.ALL
                };

                for (ENUM_TRANSPORT transport : transports) {
                    Log.d(TAG, "Trying transport: " + transport.name());
                    readers.setTransport(transport);
                    ArrayList<ReaderDevice> list = readers.GetAvailableRFIDReaderList();
                    if (list != null && !list.isEmpty()) {
                        availableRFIDReaderList = new ArrayList<>(list);
                        break;
                    }
                }

                if (uiHandler != null) {
                    if (availableRFIDReaderList == null || availableRFIDReaderList.isEmpty()) {
                        uiHandler.showToast("No Available Readers Found");
                        readers = null;
                        uiHandler.updateStatus("No Readers Found", false);
                    } else {
                        connectReader();
                    }
                }
            } catch (InvalidUsageException e) {
                Log.e(TAG, "InitSDK failed", e);
                if (uiHandler != null) {
                    uiHandler.showToast("Failed to get Readers: " + e.getInfo());
                    uiHandler.updateStatus("Failed to get Readers", false);
                }
                readers = null;
            }
        });
    }

    private void connectReader() {
        executor.execute(() -> {
            if (uiHandler != null) {
                uiHandler.updateStatus("Connecting...", false);
            }

            synchronized (RFIDHandler.this) {
                if (!isReaderConnected()) {
                    GetAvailableReader();
                    String result = (reader != null) ? connect() : "Failed to find reader";
                    
                    if (uiHandler != null) {
                        uiHandler.updateStatus(result, isReaderConnected());
                    }
                } else {
                    if (uiHandler != null) {
                        uiHandler.updateStatus("Connected: " + reader.getHostName(), true);
                    }
                }
            }
        });
    }

    private synchronized void GetAvailableReader() {
        if (readers != null) {
            Readers.attach(this);
            try {
                ArrayList<ReaderDevice> availableReaders = readers.GetAvailableRFIDReaderList();
                if (availableReaders != null && !availableReaders.isEmpty()) {
                    availableRFIDReaderList = new ArrayList<>(availableReaders);
                    ReaderDevice readerDevice = null;
                    if (availableRFIDReaderList.size() == 1) {
                        readerDevice = availableRFIDReaderList.get(0);
                    } else {
                        // Example: Filter by a specific reader model prefix if multiple are found
                        String readerNamePrefix = "RFD40";
                        for (ReaderDevice device : availableRFIDReaderList) {
                            if (device != null && device.getName() != null && device.getName().startsWith(readerNamePrefix)) {
                                readerDevice = device;
                                break;
                            }
                        }
                    }
                    if (readerDevice != null) {
                        reader = readerDevice.getRFIDReader();
                    }
                }
            } catch (InvalidUsageException e) {
                Log.e(TAG, "Error getting available readers", e);
            }
        }
    }

    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        connectReader();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        if (uiHandler != null) uiHandler.showToast("RFIDReaderDisappeared: " + readerDevice.getName());
        if (reader != null && readerDevice != null && readerDevice.getName().equals(reader.getHostName())) {
            executor.execute(this::disconnect);
        }
    }

    private synchronized String connect() {
        if (reader != null) {
            try {
                if (!reader.isConnected()) {
                    connectionTimer = 0;
                    timerHandler.post(timerRunnable);
                    long startTime = System.currentTimeMillis();
                    try {
                        reader.connect();
                        ConfigureReader();
                        setupScannerSDK();
                    } catch (InvalidUsageException e) {
                        Log.e(TAG, "Connection failed: ", e);
                        return "Connection failed: " + e.getMessage();
                    } catch (OperationFailureException e1) {
                        Log.e(TAG, "Connection failed: " + e1.getStatusDescription());
                        return "Connection failed: " + e1.getStatusDescription();
                    } finally {
                        timerHandler.removeCallbacks(timerRunnable);
                    }
                    long duration = System.currentTimeMillis() - startTime;
                    if (reader.isConnected()) {
                        return "Connected: " + reader.getHostName() + " (" + duration + " ms)";
                    }
                } else {
                    return "Connected: " + reader.getHostName();
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection failed: ", e);
                return "Connection failed: " + e.getMessage();
            }
        }
        return "Disconnected";
    }

    private void ConfigureReader() {
        IRFIDLogger.getLogger("SDKSampleApp").EnableDebugLogs(true);
        if (reader != null && reader.isConnected()) {
            try {
                if (eventHandler == null) eventHandler = new EventHandler();
                if (reader.Events != null) {
                    reader.Events.addEventsListener(eventHandler);
                    reader.Events.setHandheldEvent(true);
                    reader.Events.setTagReadEvent(true);
                    reader.Events.setAttachTagDataWithReadEvent(false);
                    reader.Events.setReaderDisconnectEvent(true);
                }
            } catch (InvalidUsageException | OperationFailureException e) {
                Log.e(TAG, "Configuration failed", e);
            }
        }
    }

    public void setupScannerSDK() {
        if (sdkHandler == null) {
            sdkHandler = new SDKHandler(context);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC);
            sdkHandler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL);
            sdkHandler.dcssdkSetDelegate(scannerHandler);
            int notifications_mask = DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value |
                    DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value;
            sdkHandler.dcssdkSubsribeForEvents(notifications_mask);
        }

        ArrayList<DCSScannerInfo> availableScanners = (ArrayList<DCSScannerInfo>) sdkHandler.dcssdkGetAvailableScannersList();
        scannerList.clear();
        if (availableScanners != null) {
            scannerList.addAll(availableScanners);
        }

        if (reader != null && reader.isConnected()) {
            String hostName = reader.getHostName();
            for (DCSScannerInfo device : scannerList) {
                if (device != null && device.getScannerName() != null && hostName != null && device.getScannerName().contains(hostName)) {
                    try {
                        sdkHandler.dcssdkEstablishCommunicationSession(device.getScannerID());
                        scannerID = device.getScannerID();
                    } catch (Exception e) {
                        Log.e(TAG, "Error establishing scanner session", e);
                    }
                }
            }
        }
    }

    private synchronized void disconnect() {
        Log.d(TAG, "disconnecting...");
        if (reader == null) return;
        try {
            if (reader.isConnected()) {
                // Best practice: stop inventory before disconnecting to ensure internal SDK threads are ready for shutdown
                try {
                    reader.Actions.Inventory.stop();
                } catch (Exception e) {
                    Log.d(TAG, "Inventory stop error (expected if already stopped)");
                }
                
                if (eventHandler != null && reader.Events != null) {
                    reader.Events.removeEventsListener(eventHandler);
                }
                reader.disconnect();
            }
            
            if (sdkHandler != null) {
                try {
                    sdkHandler.dcssdkTerminateCommunicationSession(scannerID);
                } catch (Exception e) {
                    Log.e(TAG, "Error terminating scanner session", e);
                }
            }
            
            if (uiHandler != null) uiHandler.updateStatus("Disconnected", false);
            
            // Dispose releases internal SDK resources and shuts down background threads.
            // Note: InterruptedException in SerialInputOutputManager is common and harmless during this phase.
            reader.Dispose();
        } catch (Exception e) {
            Log.e(TAG, "Error during disconnect", e);
        } finally {
            reader = null;
        }
    }

    private synchronized void dispose() {
        disconnect();
        try {
            if (readers != null) {
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during dispose", e);
        }
    }

    synchronized void performInventory() {
        try {
            if (isReaderConnected()) reader.Actions.Inventory.perform();
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error performing inventory", e);
        }
    }

    synchronized void stopInventory() {
        try {
            if (isReaderConnected()) reader.Actions.Inventory.stop();
        } catch (InvalidUsageException | OperationFailureException e) {
            Log.e(TAG, "Error stopping inventory", e);
        }
    }

    public void scanCode() {
        String in_xml = "<inArgs><scannerID>" + scannerID + "</scannerID></inArgs>";
        executor.execute(() -> executeCommand(in_xml, scannerID));
    }

    private void executeCommand(String inXML, int scannerID) {
        if (sdkHandler != null) {
            sdkHandler.dcssdkExecuteCommandOpCodeInXMLForScanner(DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_PULL_TRIGGER, inXML, new StringBuilder(), scannerID);
        }
    }

    public class EventHandler implements RfidEventsListener {
        @Override
        public void eventReadNotify(RfidReadEvents e) {
            if (reader == null) return;
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null && uiHandler != null) {
                uiHandler.handleTags(myTags);
            }
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            if (rfidStatusEvents == null || rfidStatusEvents.StatusEventData == null) return;
            STATUS_EVENT_TYPE eventType = rfidStatusEvents.StatusEventData.getStatusEventType();
            if (eventType == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData != null) {
                    HANDHELD_TRIGGER_EVENT_TYPE triggerEvent = rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent();
                    boolean pressed = (triggerEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED);
                    if (uiHandler != null) {
                        uiHandler.handleTrigger(pressed);
                    }
                }
            }
            else if (eventType == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                executor.execute(() -> {
                    disconnect();
                    dispose();
                });
            }
        }
    }
}
