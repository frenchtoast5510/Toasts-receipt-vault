# Toasts Receipt Vault

An offline-first Android app for storing receipts, warranties, serial numbers, and important documents locally on your device. No accounts, no cloud sync, no ads, and no telemetry.

## Features

- **Receipts, Warranties & Documents:** Save purchase receipts, warranty info, serial numbers, and standalone PDFs or images (manuals, registrations, insurance papers) in one place.
- **On-Device Scanning & OCR:** Scan receipts and product barcodes/serial labels locally using ML Kit to pre-fill store names, dates, totals, and serial numbers, with automatic masking of full card numbers.
- **Return & Warranty Alerts:** Track active return windows and warranty expiration dates with local background notifications before deadlines pass.
- **Easy Sharing:** Share original attached images/PDFs, formatted text summaries, or a single-page PDF claim packet through the Android share sheet.
- **Optional App Lock:** Keep the vault open behind your normal phone lock, or enable device biometrics/PIN/pattern/password or a custom app PIN in settings.
- **Encrypted Backups:** Export and restore password-protected AES-GCM archives containing your database and attachments.
- **Demo Mode:** Includes sample receipts, warranties, and documents out of the box so you can test the interface before adding your own items.

## Installation

Download the latest `.apk` from the [Releases](../../releases) page and install it on your Android device.

## Building from Source

1. Clone the repository:
   ```bash
   git clone [https://github.com/frenchtoast5510/Toasts-receipt-vault.git](https://github.com/frenchtoast5510/Toasts-receipt-vault.git)
   cd Toasts-receipt-vault
