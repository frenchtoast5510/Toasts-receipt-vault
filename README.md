# Toasts Receipt Vault

An offline-first Android app for storing receipts, warranties, documents, store return deadlines, and product serial numbers.

## Overview

Toasts Receipt Vault runs completely on-device with zero cloud accounts, zero telemetry, zero ads, and no `INTERNET` permission in the app manifest. Room database records, photos, attached PDFs, OCR text extraction, app lock credentials, and deadline alerts all stay local on your device unless you explicitly share an item or export an encrypted `.trv` backup file.

## Features

- **Vault Home**: Material 3 dynamic colors, top search bar, single horizontal filter row (`All`, `Receipts`, `Warranties`, `Documents`, `Expiring Soon`, `Expired`), live return/warranty countdown badges, and quick-share buttons on cards.
- **Built-in Demo Mode**: Enabled on first launch with realistic sample receipts, warranties, serial labels, and a sample PDF insurance policy so you can inspect every screen immediately, with one-tap controls in the top-right Demo menu to clear or reset sample data.
- **Receipts, Warranties & Documents (PDFs & Files)**:
  - Attach receipt and serial/box label photos via ML Kit Document Scanner (with Photo Picker and Camera fallbacks).
  - Attach PDFs and general documents (manuals, insurance policies, certificates, invoices) via Android Storage Access Framework (`ACTION_OPEN_DOCUMENT`), copied into app-private storage so files work offline.
  - View PDFs directly in-app with `PdfRenderer` or open them in an external viewer via `FileProvider`.
  - On-device ML Kit Text Recognition and Barcode Scanning extract merchant, total, date, and serial numbers while automatically masking payment card digits to the last 4 digits.
- **Three-Way Android Share Sheet**:
  - **Share Original Files**: Attaches saved receipt photos and/or PDF documents directly (`ACTION_SEND` / `ACTION_SEND_MULTIPLE`).
  - **Share Item Summary**: Clean formatted plain-text summary with title, store/issuer, date, total, serial/reference number, and expiration dates.
  - **Share PDF Claim / Summary Packet**: Generates a single-page offline PDF combining metadata, serial number, and an attached receipt photo or PDF page preview.
- **Optional App Security**:
  - **None (Default)**: Opens straight away using your normal phone lock.
  - **Phone Biometrics & Screen Lock**: Uses AndroidX `BiometricPrompt` (`BIOMETRIC_STRONG | BIOMETRIC_WEAK | DEVICE_CREDENTIAL`) with clean fallback on older API levels.
  - **Custom App PIN / Password**: Stored locally in DataStore as a random-salted SHA-256 hash, with an optional toggle to also allow fingerprint or face unlock.
  - Configurable auto-lock timeout (`Immediately`, `After 1 minute`, `After 5 minutes`) and a **Hide preview in recent apps** (`FLAG_SECURE`) toggle.
- **Local Reminders & Encrypted Backups**:
  - Configurable WorkManager local notifications for upcoming return deadlines and warranty/document expirations.
  - Password-protected backup export and restore via SAF, encrypting a ZIP archive of Room records, images, and documents with PBKDF2-HMAC-SHA256 (120,000 iterations) and AES-256-GCM.

## Tech Stack

- **Language & UI**: Kotlin, Jetpack Compose (Compose BOM `2026.11.01`, Material 3 `1.5.0` with dynamic colors)
- **Persistence**: Room (`androidx.room`), Jetpack DataStore Preferences, app-private `filesDir` storage
- **On-Device Vision & Security**: ML Kit Document Scanner, Text Recognition, Barcode Scanning, AndroidX BiometricPrompt, `PdfRenderer`
- **Background Work & Sharing**: WorkManager, `NotificationCompat`, `PdfDocument`, `FileProvider`, Storage Access Framework

## Building

1. Open the project in Android Studio (AGP 9.1+, JDK 17/21, compileSdk 36, minSdk 24).
2. Build the debug APK:

```bash
gradle :app:assembleDebug
```

3. Run local unit tests:

```bash
gradle :app:testDebugUnitTest
```

## Privacy & Permissions

- **No network access**: `INTERNET` and `ACCESS_NETWORK_STATE` permissions are explicitly stripped from the merged manifest.
- **No auto-backup leakage**: `android:allowBackup="false"` prevents unencrypted cloud backup of vault records; use the built-in encrypted `.trv` export instead.
- **Zero-permission media & files**: Gallery selection uses the Android Photo Picker (`PickVisualMedia`), and documents/backups use the Storage Access Framework (`OpenDocument` / `CreateDocument`).
- **Optional runtime permissions**:
  - `CAMERA`: requested only when you tap Camera to capture a receipt or serial label.
  - `POST_NOTIFICATIONS` (Android 13+): requested only if you enable local WorkManager deadline reminders.

## License

MIT License

Copyright (c) 2026 Toasts Receipt Vault Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
