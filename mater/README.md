# Zebra RFID SDK Sample Application (Android)

This sample application demonstrates how to integrate the Zebra RFID API3 SDK and Zebra Scanner SDK for Android. It provides a robust, production-ready implementation for connecting to Zebra RFID readers (RFD40, RFD8500, RFD90), performing inventory operations, and capturing barcode data.

## Features

- **Multi-Transport Connection:** Robust discovery and connection logic for SERVICE_USB, RE_SERIAL, RE_USB, and BLUETOOTH transports.
- **Real-time RFID Inventory:** Efficiently performs inventory operations, displaying unique EPC IDs and RSSI values in a responsive list.
- **Integrated Barcode Scanning:** 
    - **Scanner SDK:** Direct integration for readers with built-in scanners.
    - **DataWedge:** Support for Zebra DataWedge Intent API for seamless barcode capture.
- **Hardware Trigger Support:** Maps the reader's physical trigger to start/stop inventory or trigger the scanner.
- **Background Threading:** Uses `ExecutorService` for all hardware interactions to ensure a lag-free UI experience.
- **Lifecycle Management:** Cleanly handles Android activity lifecycle events (`onResume`, `onPause`, `onDestroy`) to manage reader connections and resource disposal.

## Project Structure

- `MainActivity.java`: Coordinates the application flow, handles runtime permissions (including Android 12+ Bluetooth permissions), and manages UI components.
- `RFIDHandler.java`: The core engine. Encapsulates the Zebra RFID API3 logic, connection state management, and hardware event listeners.
- `ScannerHandler.java`: Implements `IDcsSdkApiDelegate` to handle events from the Zebra Scanner SDK.
- `DataWedgeHandler.java`: Manages DataWedge profile creation and Intent-based barcode data reception.
- `MainUIHandler.java`: An abstract bridge that ensures all hardware events (tags, barcodes, status updates) are dispatched to the main UI thread safely.

## Getting Started

### Prerequisites

- **Android Studio:** Flamingo or newer recommended.
- **Hardware:** Zebra RFID Reader (e.g., RFD40, RFD90, RFD8500).
- **Zebra SDKs:** Ensure the following are in your `app/libs` directory:
    - `RFIDAPI3Library.aar`
    - `ScannerControlLibrary.aar`
- **Minimum SDK:** API 26 (Android 8.0).
- **Target SDK:** API 33 (Android 13.0).

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```
2. **Import into Android Studio:** Select "Open an existing project" and navigate to the root folder.
3. **Library Check:** Verify that the `.aar` files in `app/libs` are correctly recognized in `app/build.gradle`:
   ```gradle
   implementation fileTree(dir: 'libs', include: ['*.jar','*.aar'])
   ```
4. **Build & Run:** Connect your Android device and deploy the app.

## Usage

1. **Connection:** The app attempts to connect automatically. Tap the **Reader Status** text (top) to manually toggle the connection.
2. **Inventory:** Press **START** (or pull the physical trigger) to begin reading tags. EPCs will appear in the list. Press **STOP** to end.
3. **Barcode Scanning:** Use the **SCAN** button or the physical trigger to scan barcodes. Results appear in the "Scan Result" section.
4. **Configuration:** The options menu (⋮) allows resetting the reader to defaults or testing specific configurations.

## Troubleshooting

### "Receive thread interrupted" (InterruptedException)
You may see a `java.lang.InterruptedException` in Logcat during `reader.Dispose()`. This is **expected behavior** within the Zebra RFID SDK as it shuts down internal background threads (like `SerialInputOutputManager`) during resource cleanup. It does not indicate an app crash or failure.

## License

This project is intended for demonstration purposes. Use of the Zebra SDKs is subject to the Zebra Technologies EULA.
