var exports = module.exports = {};

exports.assert = function(condition, message) { 
    if (!condition)
        throw Error("Assert failed" + (typeof message !== "undefined" ? ": " + message : ""));
};

exports.similarString = function(toMatch, object, properties) {
    toMatch = toMatch.replace(/ /g, "").toUpperCase();
    return properties.find(function(prop) {
        return object[prop].replace(/ /g, "").toUpperCase() === toMatch;
    }) !== undefined;
};

exports.merge = function (obj1, obj2) {
    for (var key in obj2) {
        if (obj1[key] === undefined) {
            obj1[key] = obj2[key];
        }
    }
    return obj1;
};

exports.isMatchFor = function(name, object, properties) {
    for (var p in properties) {
        if (object[p] && (name.toUpperCase() === object[p].toUpperCase())) {
            return true;
        }
    }
    return false;
};



