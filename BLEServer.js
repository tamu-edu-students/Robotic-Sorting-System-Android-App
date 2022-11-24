/* This JavaScript code creates a BLE server on the Raspberry Pi that the Android application searches for.
 * For this to work properly, the following must be installed.
 * bluez, nodejs, npm, bleno
 */

var Descriptor = bleno.Descriptor;
var descriptor = new Descriptor({
	uuid: '2901',
	value: 'value' // Buffer/string static value
});

var Characteristic = bleno.Characteristic;
var characteristic = new Characteristic({
	uuid: 'fff1',
	properties: [ 'read', 'write', 'writeWithoutResponse' ],
	value: 'ff' // optional static value
	descriptors: [ descriptor ]
});

var PrimaryService = bleno.PrimaryService;
var primaryService = new PrimaryService({
	uuid: 'fffffffffffffffffffffffffffffff0',
	characteristics: [ characteristic ]
});

var services = [ primaryService ];

bleno.on('advertisingStart', function(error) {
	bleno.setServices( services );
});

bleno.on('stateChange'), function(state) {
	console.log('BLE state changed to: ', + state);
	if (state === 'poweredOn') {
		bleno.startAdvertising('MyDevice',['fffffffffffffffffffffffffffffff0']); // Starts BLE advertising with this name and service UUID
	} else {
		bleno.stopAdvertising();
	}
});