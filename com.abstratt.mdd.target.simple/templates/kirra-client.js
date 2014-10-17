var http = require("http");
var url = require("url");
var q = require('q');
require('array.prototype.find');
var helpers = require("./helpers.js");
var assert = helpers.assert;
var merge = helpers.merge;
var util = require('util');

var Kirra = function (baseUrl, runAs) {
    var self = this;
    
    assert(baseUrl, "baseUrl missing");
    
    self.baseUrl = baseUrl;
    
    self.cachedEntities = undefined;
    
    console.log(baseUrl);

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
        console.log(options.method + " " + options.path + " " + options.headers['X-Kirra-RunAs'] + " " + JSON.stringify(body || {}));
        var start = new Date().getTime()
        var req = http.request(options, function(res) {
            var data = "";
            res.on('data', function(chunk) {
                data += chunk.toString();
            }).on('end', function() {
                console.log("Raw response: ", data);
                var parsed = JSON.parse(data);
                if ((typeof expectedStatus === 'number' && expectedStatus !== res.statusCode) || 
                    (typeof expectedStatus === 'object' && expectedStatus.indexOf(res.statusCode) === -1)) {
                    console.error("Error response: ", util.inspect(parsed));
                    deferred.reject(parsed);
                } else {
                    var end = new Date().getTime()
                    console.error("Time: ", (end - start) + "ms");                    
                    console.error("Success response: ", util.inspect(parsed));
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
    self.getApplication = function() {
        return self.performRequest('', undefined, 200);
    };
    
    self.getExactEntity = function(entityName) {
        return self.performRequest('/entities/' + entityName, undefined, 200);
    };
    
    self.getEntityFromCache = function(entityName) {
        var found = self.cachedEntities.find(function (it) { 
            return it.fullName.toUpperCase() === entityName.toUpperCase() || it.label.toUpperCase() === entityName.toUpperCase(); 
        });
        if (found) {
            return found;
        }
        throw new Error("Entity not found: " + entityName);
    };
    
    self.getEntity = function(entity) {
        return self.getEntities().then(function() {
            return self.getEntityFromCache(entity);
        });
    };
    
    self.getEntities = function() {
        if (self.cachedEntities) {
            return q(self.cachedEntities);
        }
        return self.performRequest('/entities/', undefined, 200).then(function (entities) {
            self.cachedEntities = entities;
            return entities;
        });
    };

    
    self.invokeOperation = function(objectId, operation, arguments) {
        return self.performRequest('/entities/' + operation.owner.fullName + '/instances/' + objectId + '/actions/' + operation.name, 'POST', 200, arguments);
    };

    self.getInstances = function(entity, filter) {
        var filterQuery = self.buildFilterQuery(filter);
        return self.performRequest('/entities/' + entity + '/instances/' + filterQuery, undefined, 200);
    };
    
    self.buildFilterQuery = function (filter) {
        var filterQuery = "?";
        if (filter) {
            var terms = [];
            for (var p in filter) {
                terms.push(p + "=" + encodeURIComponent(filter[p]));    
            }
            filterQuery += terms.join("&");
        }
        return filterQuery;
    };
    
    self.getRelatedInstances = function(entity, objectId, relationshipName) {
        return self.performRequest('/entities/' + entity + '/instances/' + objectId + '/relationships/' + relationshipName + '/', undefined, 200);
    };
    
    self.findInstances = function(entity, queryName, arguments) {
        return self.performRequest('/entities/' + entity + '/finders/' + queryName, 'POST', 200, arguments);
    };
    
    self.getInstanceTemplate = function(entity) {
        if (typeof(entity) === 'object') {
            entity = entity.entity;
        }
        return self.performRequest('/entities/' + entity + '/instances/_template', undefined, 200);
    };

    self.getInstance = function(entity, objectId) {
        if (typeof(entity) === 'object') {
            objectId = entity.objectId;
            entity = entity.entity;
        }
        return self.performRequest('/entities/' + entity + '/instances/' + objectId, undefined, 200);
    };

    self.createInstance = function(entity, values, links) {
        if (typeof(entity) === 'object') {
            values = entity.values;
            links = entity.links;
            entity = entity.entity;
        }
        return self.performRequest('/entities/' + entity + '/instances/', 'POST', [201, 200], { values: values, links: links });
    };
    self.updateInstance = function(entity, objectId, values, links) {
        if (typeof(entity) === 'object') {
            objectId = entity.objectId;        
            values = entity.values;
            links = entity.links;
            entity = entity.entity;
        }
        return self.performRequest('/entities/' + entity + '/instances/' + objectId, 'PUT', 200, { values: values, links: links });
    };
    
    
    return self;
};

var exports = module.exports = Kirra;