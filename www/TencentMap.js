var exec = require('cordova/exec');

var TencentMap = {
	getLocation: function (success, error) {
		exec(success, error, 'TencentMap', 'getLocation', []);
	},
};

module.exports = TencentMap;
