#!/bin/env node

var q = require('q');
var express = require('express');
var fs = require('fs');
var bodyParser = require('body-parser');
var url = require("url");
var cls = require('continuation-local-storage');
var clsify= require('cls-middleware');
var session = cls.createNamespace('session');

var mongoose = require('./models/db.js');

var routes = require('./routes.js');

var App = function() {

    var self = this;


    /*  ================================================================  */
    /*  Helper functions.                                                 */
    /*  ================================================================  */

    /**
     *  Set up server IP address and port # using env variables/defaults.
     */
    self.setupVariables = function() {
        //  Set the environment variables we need.
        self.ipaddress = process.env.SERVER_IP || "127.0.0.1";
        self.port = process.env.SERVER_PORT || 48084;
        self.dbhost = process.env.MONGODB_DB_HOST || self.ipaddress;
        self.dbport = process.env.MONGODB_DB_PORT || 27017;
        self.dbname = process.env.MONGODB_DB_NAME || 'appdb';
        self.dbusername = process.env.MONGODB_DB_USERNAME || '';
        self.dbpassword = process.env.MONGODB_DB_PASSWORD || '';
        self.baseUrl = process.env.BASE_URL || ("http://" + self.ipaddress + ":"+ self.port + "/");
    };

    /**
     *  terminator === the termination handler
     *  Terminate server on receipt of the specified signal.
     *  @param {string} sig  Signal to terminate on.
     */
    self.terminator = function(sig) {
        if (typeof sig === "string") {
            //console.log('%s: Received %s - terminating the app ...', Date(Date.now()), sig);
            process.exit(1);
        }
        //console.log('%s: Node server stopped.', Date(Date.now()));
    };


    /**
     *  Setup termination handlers (for exit and a list of signals).
     */
    self.setupTerminationHandlers = function() {
        //  Process on exit and signals.
        process.on('exit', function() {
            self.terminator();
        });

        // Removed 'SIGPIPE' from the list - bugz 852598.
        ['SIGHUP', 'SIGINT', 'SIGQUIT', 'SIGILL', 'SIGTRAP', 'SIGABRT',
            'SIGBUS', 'SIGFPE', 'SIGUSR1', 'SIGSEGV', 'SIGUSR2', 'SIGTERM'
        ].forEach(function(element, index, array) {
            process.on(element, function() {
                self.terminator(element);
            });
        });
    };


    /**
     *  Initialize the server (express) and create the routes and register
     *  the handlers.
     */
    self.initializeServer = function() {
        self.app = express();
        self.app.use(bodyParser.urlencoded({
            extended: true
        }));
        
        // set the current user in the context
        self.app.use(function(req, res, next) {
            var namespace = cls.createNamespace('session');
            namespace.run(function(context) {
                context.username = req.get('X-Kirra-RunAs');
                next();
            });
        });
        
        self.app.use(bodyParser.json());
        routes.build(self.app, function (relative) { return self.baseUrl + relative; });
    };


    /**
     *  Initializes the application.
     */
    self.initialize = function() {
        self.setupVariables();
        self.setupTerminationHandlers();
        self.initializeServer();
    };


    /**
     *  Start the server (starts up the application).
     */
    self.start = function() {
        //  Start the app on the specific interface (and port).
        self.app.listen(self.port, self.ipaddress, function() {
            //console.log('%s: Node server started on %s:%d ...', Date(Date.now()), self.ipaddress, self.port);
        });
    };
};


var app = exports = module.exports = new App();
app.initialize();
app.start();
//console.log("App started!");
