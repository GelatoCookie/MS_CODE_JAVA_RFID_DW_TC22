package com.zebra.rfid.demo.sdksample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Main Activity for the RFID Sample application.
 */
public class MainActivity extends AppCompatActivity implements RFIDHandler.ResponseHandlerInterface {

    private static final String TAG = "MainActivity";
    
    public TextView statusTextViewRFID;
    private ListView tagListView;
    private ArrayAdapter<String> tagAdapter;
    private final ArrayList<String> tagList = new ArrayList<>();
    private TextView scanResult;
    private Button btnStart;
    private Button btnStop;
    private Button btnScan;
    
    private RFIDHandler rfidHandler;
    private DataWedgeHandler dataWedgeHandler;
    private MainUIHandler uiHandler;
    
    private final HashSet<String> tagSet = new HashSet<>();
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        setupHandlers();
        checkPermissionsAndInit();
    }

    private void initUI() {
        statusTextViewRFID = findViewById(R.id.textViewStatusrfid);
        if (statusTextViewRFID != null) {
            statusTextViewRFID.setOnClickListener(v -> {
                if (rfidHandler != null) rfidHandler.toggleConnection();
            });
        }

        scanResult = findViewById(R.id.scanResult);
        tagListView = findViewById(R.id.tag_list);
        tagAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tagList);
        if (tagListView != null) tagListView.setAdapter(tagAdapter);
        
        btnStart = findViewById(R.id.TestButton);
        btnStop = findViewById(R.id.TestButton2);
        btnScan = findViewById(R.id.scan);
        
        if (btnStart != null) btnStart.setEnabled(false);
        if (btnStop != null) btnStop.setEnabled(false);
        if (btnScan != null) btnScan.setEnabled(false);
    }

    private void setupHandlers() {
        uiHandler = new MainUIHandler(this) {
            @Override
            protected void handleUIUpdate(UpdateType type, Object... data) {
                switch (type) {
                    case READER_STATUS:
                        updateReaderStatusUI((String) data[0], (Boolean) data[1]);
                        break;
                    case SCAN_BUTTON_STATE:
                        if (btnScan != null) btnScan.setEnabled((Boolean) data[0]);
                        break;
                    case TAG_DATA:
                        handleTagdata((TagData[]) data[0]);
                        break;
                    case TRIGGER_PRESS:
                        handleTriggerPress((Boolean) data[0]);
                        break;
                    case BARCODE_DATA:
                        barcodeData((String) data[0]);
                        break;
                    case TOAST_MESSAGE:
                        sendToast((String) data[0]);
                        break;
                }
            }
        };

        rfidHandler = new RFIDHandler();
        dataWedgeHandler = new DataWedgeHandler(this);
        dataWedgeHandler.onCreate();
    }

    private void updateReaderStatusUI(String status, boolean isConnected) {
        if (statusTextViewRFID != null) {
            statusTextViewRFID.setText(status);
            int color = isConnected ? R.color.status_connected : R.color.status_disconnected;
            statusTextViewRFID.setTextColor(ContextCompat.getColor(this, color));
            if (btnStart != null) btnStart.setEnabled(isConnected);
            if (!isConnected && btnStop != null) btnStop.setEnabled(false);
        }
    }

    private void checkPermissionsAndInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_PERMISSION_REQUEST_CODE);
            } else {
                rfidHandler.onCreate(this, uiHandler);
            }
        } else {
            rfidHandler.onCreate(this, uiHandler);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            rfidHandler.onCreate(this, uiHandler);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.antenna_settings) {
            sendToast(rfidHandler.Test1());
            return true;
        } else if (id == R.id.Singulation_control) {
            sendToast(rfidHandler.Test2());
            return true;
        } else if (id == R.id.Default) {
            sendToast(rfidHandler.Defaults());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        rfidHandler.onPause();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        rfidHandler.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rfidHandler.onDestroy();
        if (dataWedgeHandler != null) dataWedgeHandler.onDestroy();
    }

    private void toggleInventoryButtons(boolean isRunning) {
        if (btnStart != null) btnStart.setEnabled(!isRunning);
        if (btnStop != null) btnStop.setEnabled(isRunning);
    }

    public void setScanButtonEnabled(boolean enabled) {
        uiHandler.sendUpdate(MainUIHandler.UpdateType.SCAN_BUTTON_STATE, enabled);
    }

    public void StartInventory(View view) {
        toggleInventoryButtons(true);
        clearTagData();
        rfidHandler.performInventory();
    }

    private void clearTagData() {
        tagSet.clear();
        tagList.clear();
        if (tagAdapter != null) tagAdapter.notifyDataSetChanged();
    }

    public void scanCode(View view) {
        rfidHandler.scanCode();
    }

    public void testFunction(View view) {
        rfidHandler.testFunction();
    }

    public void StopInventory(View view) {
        toggleInventoryButtons(false);
        rfidHandler.stopInventory();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void handleTagdata(TagData[] tagData) {
        if (tagData == null || tagData.length == 0) return;

        final ArrayList<String> newTags = new ArrayList<>();
        for (TagData tag : tagData) {
            if (tag != null && tag.getTagID() != null && !tagSet.contains(tag.getTagID())) {
                tagSet.add(tag.getTagID());
                newTags.add(tag.getTagID() + " (RSSI: " + tag.getPeakRSSI() + ")");
            }
        }
        
        if (!newTags.isEmpty()) {
            tagList.addAll(0, newTags); 
            if (tagAdapter != null) tagAdapter.notifyDataSetChanged();
            
            if (statusTextViewRFID != null && statusTextViewRFID.getText() != null) {
                String statusStr = statusTextViewRFID.getText().toString();
                if (statusStr.contains("Connected")) {
                    String currentStatus = statusStr.split("\n")[0];
                    statusTextViewRFID.setText(currentStatus + "\nUnique Tags: " + tagSet.size());
                }
            }
        }
    }

    @Override
    public void handleTriggerPress(boolean pressed) {
        toggleInventoryButtons(pressed);
        if (pressed) {
            clearTagData();
            rfidHandler.performInventory();
        } else {
            rfidHandler.stopInventory();
        }
    }

    @Override
    public void barcodeData(String val) {
        if (scanResult != null) {
            scanResult.setText(String.format("Scan Result : %s", val != null ? val : ""));
        }
    }

    @Override
    public void sendToast(String val) {
        Toast.makeText(MainActivity.this, val, Toast.LENGTH_SHORT).show();
    }

    public void updateReaderStatus(String status, boolean isConnected) {
        uiHandler.updateStatus(status, isConnected);
    }
}
