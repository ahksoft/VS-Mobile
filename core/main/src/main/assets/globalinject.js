(function(){
    const fs = require('fs');
    const os = require('os');

    // Patch process.platform to always return "linux"
    Object.defineProperty(process, 'platform', {
        value: "linux",
        writable: false,
        configurable: false
    });

    // Patch os.networkInterfaces to return fake network info (fixes mac address error)
    var origNetworkInterfaces = os.networkInterfaces;
    os.networkInterfaces = function() {
        try {
            return origNetworkInterfaces.call(os);
        } catch(ex) {
            // Return fake network interfaces if real ones fail
            return {
                lo: [{
                    address: '127.0.0.1',
                    netmask: '255.0.0.0',
                    family: 'IPv4',
                    mac: '00:00:00:00:00:00',
                    internal: true,
                    cidr: '127.0.0.1/8'
                }],
                eth0: [{
                    address: '192.168.1.100',
                    netmask: '255.255.255.0',
                    family: 'IPv4',
                    mac: '02:00:00:00:00:00',
                    internal: false,
                    cidr: '192.168.1.100/24'
                }]
            };
        }
    };

    // Patch fs.readFile for /etc/shells (fixes shell detection)
    var origReadFile = fs.readFile;
    fs.readFile = function(path, options, callback) {
        if (path === '/etc/shells') {
            if (typeof options === 'function') {
                callback = options;
            }
            var shells = '/bin/sh\n/bin/bash\n';
            if (typeof callback === 'function') {
                callback(null, shells);
            }
            return;
        }
        return origReadFile.apply(this, arguments);
    };

    console.log('[globalinject] Platform patched to:', process.platform);
})();
