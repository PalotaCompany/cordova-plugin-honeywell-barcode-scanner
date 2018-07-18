var exec = require('cordova/exec');

module.exports = {
    onBarcodeScanned: function(success, failure){
        return exec(success, failure, "HoneywellBarcodeScannerPlugin", "onBarcodeScanned", []);
    }
};