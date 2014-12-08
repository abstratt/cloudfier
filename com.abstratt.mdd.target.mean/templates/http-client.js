var http = require("http");
var url = require("url");
var q = require('q');
require('array.prototype.find');
var helpers = require("./helpers.js"); 
var assert = helpers.assert;
var merge = helpers.merge;
var util = require('util');

var HttpClient = function (baseUrl, runAs) {
    var self = this;
    
    assert(baseUrl, "baseUrl missing");
    
    self.baseUrl = baseUrl;
    
//    console.log(baseUrl);

    self.performRequestOnURL = function(requestUrl, method, expectedStatus, body) {
        var parsedUrl = url.parse(requestUrl);
        var options = {
          hostname: parsedUrl.hostname,
          path: parsedUrl.path,
          port: parsedUrl.port,
          method: method || 'GET',
          headers: { 'content-type': 'application/json' }
        };
        if (runAs) {
            options.headers['X-Kirra-RunAs'] = runAs;
        }
        var deferred = q.defer();
        //console.error(options.method + " " + options.path + " " + options.headers['X-Kirra-RunAs'] + " " + JSON.stringify(body || {}));
        var start = new Date().getTime()
        var req = http.request(options, function(res) {
            var data = "";
            res.on('data', function(chunk) {
                data += chunk.toString();
            }).on('end', function() {
                //console.error("Raw response: ", data);
                var parsed = JSON.parse(data);
                if ((typeof expectedStatus === 'number' && expectedStatus !== res.statusCode) || 
                    (typeof expectedStatus === 'object' && expectedStatus.indexOf(res.statusCode) === -1)) {
                    //console.error("Error response: ", util.inspect(parsed));
                    deferred.reject(parsed);
                } else {
                    var end = new Date().getTime()
                    //console.error("Time: ", (end - start) + "ms");                    
                    //console.error("Success response: ", util.inspect(parsed));
                    deferred.resolve(parsed);
                }
            });
        });
        if (body) {
            req.write(JSON.stringify(body)); 
        }
        req.end();
        req.on('error', function(e) {
            deferred.reject(e);
        });
        return deferred.promise;
    }
    
    self.performRequest = function(path, method, expectedStatus, body) {
        var requestUrl = self.baseUrl + path;            
        return self.performRequestOnURL(requestUrl, method, expectedStatus, body);
    };
    return self;
};

var exports = module.exports = HttpClient;