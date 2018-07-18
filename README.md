# cordova-plugin-honeywell-barcode-scanner

Cordova plugin for Android Honeywell barcode scanner.

This plugin uses Honeywell's Android Data Collection Java API V1.90

## Installation

```bash
cordova plugin add cordova-plugin-honeywell-barcode-scanner --save
```

## Supported Platforms
- Android

## Usage
Listen to barcode scans as follows
```javascript
cordova.plugins.honeywell.barcode.onBarcodeScanned(result => {
    console.log("data", result.data); // actual barcode data
    console.log("code", result.code);
    console.log("charset", result.charset);
    console.log("aim-id", result.aimId);
    console.log("timestamp", result.timestamp);
}, error => {
    console.error(error);
})
```
