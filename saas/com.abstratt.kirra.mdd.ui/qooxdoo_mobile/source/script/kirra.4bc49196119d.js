/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2012 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Martin Wittemann (wittemann)
     * Tino Butz (tbtz)

************************************************************************ */

/**
*
 * Basic application routing manager.
 *
 * Define routes to react on certain GET / POST / DELETE / PUT operations.
 *
 * * GET is triggered when the hash value of the url is changed. Can be called
 *   manually by calling the {@link #executeGet} method.
 * * POST / DELETE / PUT has to be triggered manually right now (will be changed later)
 *    by calling the {@link #executePost}, {@link #executeDelete}, {@link #executePut} method.
 *
 * This manager can also be used to provide browser history.
 *
 * *Example*
 *
 * Here is a little example of how to use the widget.
 *
 * <pre class='javascript'>
 *   var r = new qx.application.Routing();
 *
 *   // show the start page, when no hash is given or the hash is "#/"
 *   r.onGet("/", function(data) {
 *     startPage.show();
 *   }, this);
 *
 *   // whenever the url /address is called show the addressbook page.
 *   r.onGet("/address", function(data) {
 *     addressBookPage.show();
 *   }, this);
 *
 *   // address with the parameter "id"
 *   r.onGet("/address/{id}", function(data) {
 *     addressPage.show();
 *     model.loadAddress(data.params.id);
 *   }, this);
 *
 *   // Alternative you can use regExp for a route
 *   r.onGet(/address\/(.*)/, function(data) {
 *     addressPage.show();
 *     model.loadAddress(data.params.0);
 *   }, this);
 *
 *   // make sure that the data is always loaded
 *   r.onGet("/address.*", function(data) {
 *     if (!model.isLoaded()) {
 *       model.loadAddresses();
 *     }
 *   }, this);
 *
 *   // update the address
 *   r.onPost("/address/{id}", function(data) {
 *     model.updateAddress(data.params.id);
 *   }, this);
 *
 *   // delete the address and navigate back
 *   r.onDelete("/address/{id}", function(data) {
 *     model.deleteAddress(data.params.id);
 *     r.executeGet("/address", {reverse:true});
 *   }, this);
 * </pre>
 *
 * This example defines different routes to handle navigation events.
 */
qx.Bootstrap.define("qx.application.Routing", {

  construct : function()
  {
    this.__messaging = new qx.event.Messaging();

    this.__navigationHandler = qx.bom.History.getInstance();
    this.__navigationHandler.addListener("changeState", this.__onChangeHash, this);
  },


  statics : {
    DEFAULT_PATH : "/",

    __back : [],
    __forward : []
  },


  members :
  {
    __navigationHandler : null,
    __messaging : null,

    __currentGetPath : null,


    /**
     * Initialization method used to execute the get route for the currently set history path.
     * If no path is set, either the given argument named <code>defaultRoute</code>
     * or the {@link #DEFAULT_PATH} will be used for initialization.
     *
     * @param defaultRoute {String?} Optional default route for initialization.
     */
    init : function(defaultRoute)
    {
      if (qx.core.Environment.get("qx.debug")) {
        if (defaultRoute != null) {
          qx.core.Assert.assertString(defaultRoute, "Invalid argument 'defaultRoute'");
        }
      }

      var path = this.getState();
      path = this._getPathOrFallback(path, defaultRoute);
      this._executeGet(path, null, true);
    },


    /**
     * Checks if path is valid and registered in channel "get" and then just returns it.
     * If the path is not valid either the <code>defaultPath</code> (if given) or the
     * {@link #DEFAULT_PATH} will be returned.
     *
     * @param path {String} Path which gets checked.
     * @param defaultPath {String?} Optional default path.
     * @return {String} A valid path.
     */
    _getPathOrFallback : function(path, defaultPath) {
      if (path == "" || path == null || !this.__messaging.has("get", path)) {
        path = defaultPath || qx.application.Routing.DEFAULT_PATH;
      }
      return path;
    },


    /**
     * Adds a route handler for the "get" operation. The route gets called
     * when the {@link #executeGet} method found a match.
     *
     * @param route {String|RegExp} The route, used for checking if the executed path matches.
     * @param handler {Function} The handler to call, when the route matches with the executed path.
     * @param scope {Object} The scope of the handler.
     * @return {String} Event listener ID
     */
    onGet : function(route, handler, scope) {
      return this.__messaging.on("get", route, handler, scope);
    },


    /**
     * This is a shorthand for {@link #onGet}.
     *
     * @param route {String|RegExp} The route, used for checking if the executed path matches.
     * @param handler {Function} The handler to call, when the route matches with the executed path.
     * @param scope {Object} The scope of the handler.
     * @return {String} Event listener ID
     */
    on : function(route, handler, scope) {
      return this.onGet(route, handler, scope);
    },


    /**
     * Adds a route handler for the "post" operation. The route gets called
     * when the {@link #executePost} method found a match.
     *
     * @param route {String|RegExp} The route, used for checking if the executed path matches.
     * @param handler {Function} The handler to call, when the route matches with the executed path.
     * @param scope {Object} The scope of the handler.
     * @return {String} Event listener ID
     */
    onPost : function(route, handler, scope) {
      return this.__messaging.on("post", route, handler, scope);
    },


    /**
     * Adds a route handler for the "put" operation. The route gets called
     * when the {@link #executePut} method found a match.
     *
     * @param route {String|RegExp} The route, used for checking if the executed path matches
     * @param handler {Function} The handler to call, when the route matches with the executed path
     * @param scope {Object} The scope of the handler
     * @return {String} Event listener ID
     */
    onPut : function(route, handler, scope) {
      return this.__messaging.on("put", route, handler, scope);
    },


    /**
     * Adds a route handler for the "delete" operation. The route gets called
     * when the {@link #executeDelete} method found a match.
     *
     * @param route {String|RegExp} The route, used for checking if the executed path matches
     * @param handler {Function} The handler to call, when the route matches with the executed path
     * @param scope {Object} The scope of the handler
     * @return {String} Event listener ID
     */
    onDelete : function(route, handler, scope) {
      return this.__messaging.on("delete", route, handler, scope);
    },


    /**
     * Adds a route handler for the "any" operation. The "any" operation is called
     * before all other operations.
     *
     * @param route {String|RegExp} The route, used for checking if the executed path matches
     * @param handler {Function} The handler to call, when the route matches with the executed path
     * @param scope {Object} The scope of the handler
     * @return {String} Event listener ID
     */
    onAny : function(route, handler, scope) {
      return this.__messaging.onAny(route, handler, scope);
    },


    /**
     * Removes a registered route by the given id.
     *
     * @param id {String} The id of the registered route
     */
    remove : function(id) {
      this.__messaging.remove(id);
    },


    /**
     * Hash change event handler.
     *
     * @param evt {qx.event.type.Data} The changeHash event.
     */
    __onChangeHash : function(evt)
    {
      var path = evt.getData();
      path = this._getPathOrFallback(path);

      if (path != this.__currentGetPath) {
        this._executeGet(path, null, true);
      }
    },


    /**
     * Executes the get operation and informs all matching route handler.
     *
     * @param path {String} The path to execute
     * @param customData {var} The given custom data that should be propagated
     * @param fromEvent {var} Determines whether this method was called from history
     *
     */
    _executeGet : function(path, customData, fromEvent)
    {
      this.__currentGetPath = path;

      var history = this.__getFromHistory(path);
      if (history)
      {
        if (!customData)
        {
          customData = history.data.customData || {};
          customData.fromHistory = true;
          customData.action = history.action;
          customData.fromEvent = fromEvent;
        } else {
          this.__replaceCustomData(path, customData);
        }
      } else {
        this.__addToHistory(path, customData);
        qx.application.Routing.__forward = [];
      }

      this.__navigationHandler.setState(path);
      this.__messaging.emit("get", path, null, customData);
    },


    /**
     * Executes the get operation and informs all matching route handler.
     *
     * @param path {String} The path to execute
     * @param customData {var} The given custom data that should be propagated
     */
    executeGet : function(path, customData) {
      this._executeGet(path, customData);
    },


    /**
     * This is a shorthand for {@link #executeGet}.
     *
     * @param path {String} The path to execute
     * @param customData {var} The given custom data that should be propagated
     */
    execute : function(path, customData) {
      this.executeGet(path, customData);
    },


    /**
     * Executes the post operation and informs all matching route handler.
     *
     * @param path {String} The path to execute
     * @param params {Map} The given parameters that should be propagated
     * @param customData {var} The given custom data that should be propagated
     */
    executePost : function(path, params, customData) {
      this.__messaging.emit("post", path, params, customData);
    },


    /**
     * Executes the put operation and informs all matching route handler.
     *
     * @param path {String} The path to execute
     * @param params {Map} The given parameters that should be propagated
     * @param customData {var} The given custom data that should be propagated
     */
    executePut : function(path, params, customData) {
      this.__messaging.emit("put", path, params, customData);
    },


    /**
     * Executes the delete operation and informs all matching route handler.
     *
     * @param path {String} The path to execute
     * @param params {Map} The given parameters that should be propagated
     * @param customData {var} The given custom data that should be propagated
     */
    executeDelete : function(path, params, customData) {
      this.__messaging.emit("delete", path, params, customData);
    },


    /**
     * Returns state value (history hash) of the navigation handler.
     * @return {String} State of history navigation handler
     */
    getState : function() {
      return this.__navigationHandler.getState();
    },


    /**
     * Adds the custom data of a given path to the history.
     *
     * @param path {String} The path to store.
     * @param customData {var} The custom data to store
     */
    __addToHistory : function(path, customData)
    {
      qx.application.Routing.__back.unshift({
        path : path,
        customData : customData
      });
    },


    /**
     * Replaces the customData in the history objects with the recent custom data.
     * @param path {String} The path to replace.
     * @param customData {var} The custom data to store.
     */
    __replaceCustomData : function(path, customData) {
      var register = [qx.application.Routing.__back, qx.application.Routing.__forward];
      for (var i=0; i < register.length; i++) {
        for (var j=0; j < register[i].length; j++) {
          if (register[i][j].path == path) {
            register[i][j].customData = customData;
          }
        }
      }
    },


    /**
     * Returns a history entry for a certain path.
     *
     * @param path {String} The path of the entry
     * @return {Map|null} The retrieved entry. <code>null</code> when no entry was found.
     */
    __getFromHistory : function(path)
    {
      var back = qx.application.Routing.__back;
      var forward = qx.application.Routing.__forward;
      var found = false;

      var entry = null;
      var length = back.length;
      for (var i = 0; i < length; i++)
      {
        if (back[i].path == path)
        {
          entry = back[i];
          var toForward = back.splice(0,i);
          for (var a=0; a<toForward.length; a++){
            forward.unshift(toForward[a]);
          }
          found = true;
          break;
        }
      }
      if (found){
        return {
          data : entry,
          action : "back"
        }
      }

      var length = forward.length;
      for (var i = 0; i < length; i++)
      {
        if (forward[i].path == path)
        {
          entry = forward[i];
          var toBack = forward.splice(0,i+1);
          for (var a=0; a<toBack.length; a++){
            back.unshift(toBack[a]);
          }
          break;
        }
      }

      if (entry){
        return {
          data : entry,
          action : "forward"
        }
      }
      return entry;
    },


    /**
     * Decouples the Routing from the navigation handler.
     */
    dispose : function() {
      this.__navigationHandler.removeListener("changeState", this.__onChangeHash, this);
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2011 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Tino Butz (tbtz)
     * Martin Wittemann (wittemann)

************************************************************************ */

/**
 * Define messages to react on certain channels.
 *
 * The channel names will be used in the {@link #on} method to define handlers which will
 * be called on certain channels and routes. The {@link #emit} method can be used
 * to execute a given route on a channel. {@link #onAny} defines a handler on any channel.
 *
 * *Example*
 *
 * Here is a little example of how to use the messaging.
 *
 * <pre class='javascript'>
 *   var m = new qx.event.Messaging();
 *
 *   m.on("get", "/address/{id}", function(data) {
 *     var id = data.params.id; // 1234
 *     // do something with the id...
 *   },this);
 *
 *   m.emit("get", "/address/1234");
 * </pre>
 */
qx.Bootstrap.define("qx.event.Messaging",
{
  construct : function()
  {
    this._listener = {},
    this.__listenerIdCount = 0;
    this.__channelToIdMapping = {};
  },


  members :
  {
    _listener : null,
    __listenerIdCount : null,
    __channelToIdMapping : null,


    /**
     * Adds a route handler for the given channel. The route is called
     * if the {@link #emit} method finds a match.
     *
     * @param channel {String} The channel of the message.
     * @param type {String|RegExp} The type, used for checking if the executed path matches.
     * @param handler {Function} The handler to call if the route matches the executed path.
     * @param scope {var ? null} The scope of the handler.
     * @return {String} The id of the route used to remove the route.
     */
    on : function(channel, type, handler, scope) {
      return this._addListener(channel, type, handler, scope);
    },



    /**
     * Adds a handler for the "any" channel. The "any" channel is called
     * before all other channels.
     *
     * @param type {String|RegExp} The route, used for checking if the executed path matches
     * @param handler {Function} The handler to call if the route matches the executed path
     * @param scope {var ? null} The scope of the handler.
     * @return {String} The id of the route used to remove the route.
     */
    onAny : function(type, handler, scope) {
      return this._addListener("any", type, handler, scope);
    },


    /**
     * Adds a listener for a certain channel.
     *
     * @param channel {String} The channel the route should be registered for
     * @param type {String|RegExp} The type, used for checking if the executed path matches
     * @param handler {Function} The handler to call if the route matches the executed path
     * @param scope {var ? null} The scope of the handler.
     * @return {String} The id of the route used to remove the route.
     */
    _addListener : function(channel, type, handler, scope) {
      var listeners = this._listener[channel] = this._listener[channel] || {};
      var id = this.__listenerIdCount++;
      var params = [];
      var param = null;

      // Convert the route to a regular expression.
      if (qx.lang.Type.isString(type))
      {
        var paramsRegexp = /\{([\w\d]+)\}/g;

        while ((param = paramsRegexp.exec(type)) !== null) {
          params.push(param[1]);
        }
        type = new RegExp("^" + type.replace(paramsRegexp, "([^\/]+)") + "$");
      }

      listeners[id] = {regExp:type, params:params, handler:handler, scope:scope};
      this.__channelToIdMapping[id] = channel;
      return id;
    },


    /**
     * Removes a registered listener by the given id.
     *
     * @param id {String} The id of the registered listener.
     */
    remove : function(id) {
      var channel = this.__channelToIdMapping[id];
      var listener = this._listener[channel];
      delete listener[id];
      delete this.__channelToIdMapping[id];
    },


    /**
     * Checks if a listener is registered for the given path in the given channel.
     *
     * @param channel {String} The channel of the message.
     * @param path {String} The path to check.
     * @return {Boolean} Whether a listener is registered.
     */
    has : function(channel, path) {
      var listeners = this._listener[channel];
      if (!listeners || qx.lang.Object.isEmpty(listeners)) {
        return false;
      }

      for (var id in listeners)
      {
        var listener = listeners[id];
        if (listener.regExp.test(path)) {
          return true;
        }
      }

      return false;
    },

    /**
     * Sends a message on the given channel and informs all matching route handlers.
     *
     * @param channel {String} The channel of the message.
     * @param path {String} The path to execute
     * @param params {Map} The given parameters that should be propagated
     * @param customData {var} The given custom data that should be propagated
     */
    emit : function(channel, path, params, customData) {
      this._emit(channel, path, params, customData);
    },


    /**
     * Executes a certain channel with a given path. Informs all
     * route handlers that match with the path.
     *
     * @param channel {String} The channel to execute.
     * @param path {String} The path to check
     * @param params {Map} The given parameters that should be propagated
     * @param customData {var} The given custom data that should be propagated
     */
    _emit : function(channel, path, params, customData)
    {
      var listenerMatchedAny = false;
      var listener = this._listener["any"];
      listenerMatchedAny = this._emitListeners(channel, path, listener, params, customData);

      var listenerMatched = false;
      listener = this._listener[channel];
      listenerMatched = this._emitListeners(channel, path, listener, params, customData);

      if (!listenerMatched && !listenerMatchedAny) {
        qx.Bootstrap.info("No listener found for " + path);
      }
    },


    /**
     * Executes all given listener for a certain channel. Checks all listeners if they match
     * with the given path and executes the stored handler of the matching route.
     *
     * @param channel {String} The channel to execute.
     * @param path {String} The path to check
     * @param listeners {Map[]} All routes to test and execute.
     * @param params {Map} The given parameters that should be propagated
     * @param customData {var} The given custom data that should be propagated
     *
     * @return {Boolean} Whether the route has been executed
     */
    _emitListeners : function(channel, path, listeners, params, customData)
    {
      if (!listeners || qx.lang.Object.isEmpty(listeners)) {
        return false;
      }
      var listenerMatched = false;
      for (var id in listeners)
      {
        var listener = listeners[id];
        listenerMatched |= this._emitRoute(channel, path, listener, params, customData);
      }
      return listenerMatched;
    },


    /**
     * Executes a certain listener. Checks if the listener matches the given path and
     * executes the stored handler of the route.
     *
     * @param channel {String} The channel to execute.
     * @param path {String} The path to check
     * @param listener {Map} The route data.
     * @param params {Map} The given parameters that should be propagated
     * @param customData {var} The given custom data that should be propagated
     *
     * @return {Boolean} Whether the route has been executed
     */
    _emitRoute : function(channel, path, listener, params, customData)
    {
      var match = listener.regExp.exec(path);
      if (match)
      {
        var params = params || {};
        var param = null;
        var value = null;
        match.shift(); // first match is the whole path
        for (var i=0; i < match.length; i++)
        {
          value = match[i];
          param = listener.params[i];
          if (param) {
            params[param] = value;
          } else {
            params[i] = value;
          }
        }
        listener.handler.call(listener.scope, {path:path, params:params, customData:customData});
      }

      return match != undefined;
    }
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Fabian Jakobs (fjakobs)

   ======================================================================

   This class contains code based on the following work:

   * Yahoo! UI Library
     http://developer.yahoo.com/yui
     Version 2.2.0

     Copyright:
       (c) 2007, Yahoo! Inc.

     License:
       BSD: http://developer.yahoo.com/yui/license.txt

   ----------------------------------------------------------------------

     http://developer.yahoo.com/yui/license.html

     Copyright (c) 2009, Yahoo! Inc.
     All rights reserved.

     Redistribution and use of this software in source and binary forms,
     with or without modification, are permitted provided that the
     following conditions are met:

     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in
       the documentation and/or other materials provided with the
       distribution.
     * Neither the name of Yahoo! Inc. nor the names of its contributors
       may be used to endorse or promote products derived from this
       software without specific prior written permission of Yahoo! Inc.

     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
     COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
     (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
     SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
     HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
     STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
     ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
     OF THE POSSIBILITY OF SUCH DAMAGE.

************************************************************************ */

/* ************************************************************************


************************************************************************ */

/**
 * A helper for using the browser history in JavaScript Applications without
 * reloading the main page.
 *
 * Adds entries to the browser history and fires a "request" event when one of
 * the entries was requested by the user (e.g. by clicking on the back button).
 *
 * This class is an abstract template class. Concrete implementations have to
 * provide implementations for the {@link #_readState} and {@link #_writeState}
 * methods.
 *
 * Browser history support is currently available for Internet Explorer 6/7,
 * Firefox, Opera 9 and WebKit. Safari 2 and older are not yet supported.
 *
 * This module is based on the ideas behind the YUI Browser History Manager
 * by Julien Lecomte (Yahoo), which is described at
 * http://yuiblog.com/blog/2007/02/21/browser-history-manager/. The Yahoo
 * implementation can be found at http://developer.yahoo.com/yui/history/.
 * The original code is licensed under a BSD license
 * (http://developer.yahoo.com/yui/license.txt).
 *
 * @asset(qx/static/blank.html)
 */
qx.Class.define("qx.bom.History",
{
  extend : qx.core.Object,
  type : "abstract",




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  construct : function()
  {
    this.base(arguments);

    this._baseUrl = window.location.href.split('#')[0] + '#';

    this._titles = {};
    this._setInitialState();
  },


  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events: {
    /**
     * Fired when the user moved in the history. The data property of the event
     * holds the state, which was passed to {@link #addToHistory}.
     */
    "request" : "qx.event.type.Data"
  },


  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */


  statics :
  {
    /**
     * @type {Boolean} Whether the browser supports the 'hashchange' event natively.
     */
    SUPPORTS_HASH_CHANGE_EVENT : qx.core.Environment.get("event.hashchange"),


    /**
     * Get the singleton instance of the history manager.
     *
     * @return {History}
     */
    getInstance : function()
    {
      var runsInIframe = !(window == window.top);

      if (!this.$$instance)
      {
        // in iframe + IE9
        if (runsInIframe
          && qx.core.Environment.get("browser.documentmode") == 9
        ) {
          this.$$instance = new qx.bom.HashHistory();
        }

        // in iframe + IE<9
        else if (runsInIframe
          && qx.core.Environment.get("engine.name") == "mshtml"
          && qx.core.Environment.get("browser.documentmode") < 9
        ) {
          this.$$instance = new qx.bom.IframeHistory();
        }

        // browser with hashChange event
        else if (this.SUPPORTS_HASH_CHANGE_EVENT) {
          this.$$instance = new qx.bom.NativeHistory();
        }

        // IE without hashChange event
        else if ((qx.core.Environment.get("engine.name") == "mshtml")) {
          this.$$instance = new qx.bom.IframeHistory();
        }

        // fallback
        else {
          this.$$instance = new qx.bom.NativeHistory();
        }
      }
      return this.$$instance;
    }
  },


  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /**
     * Property holding the current title
     */
    title :
    {
      check : "String",
      event : "changeTitle",
      nullable : true,
      apply    : "_applyTitle"
    },

    /**
     * Property holding the current state of the history.
     */
    state :
    {
      check : "String",
      event : "changeState",
      nullable : true,
      apply: "_applyState"
    }
  },




  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    _titles : null,


    // property apply
    _applyState : function(value, old)
    {
      this._writeState(value);
    },


    /**
     * Populates the 'state' property with the initial state value
     */
    _setInitialState : function() {
      this.setState(this._readState());
    },


    /**
     * Encodes the state value into a format suitable as fragment identifier.
     *
     * @param value {String} The string to encode
     * @return {String} The encoded string
     */
    _encode : function (value)
    {
      if (qx.lang.Type.isString(value)) {
        return encodeURIComponent(value);
      }

      return "";
    },


    /**
     * Decodes a fragment identifier into a string
     *
     * @param value {String} The fragment identifier
     * @return {String} The decoded fragment identifier
     */
    _decode : function (value)
    {
      if (qx.lang.Type.isString(value)) {
        return decodeURIComponent(value);
      }

      return "";
    },


    // property apply
    _applyTitle : function (title)
    {
      if (title != null) {
        document.title = title || "";
      }
    },


    /**
     * Adds an entry to the browser history.
     *
     * @param state {String} a string representing the state of the
     *          application. This command will be delivered in the data property of
     *          the "request" event.
     * @param newTitle {String ? null} the page title to set after the history entry
     *          is done. This title should represent the new state of the application.
     */
    addToHistory : function(state, newTitle)
    {
      if (!qx.lang.Type.isString(state)) {
        state = state + "";
      }

      if (qx.lang.Type.isString(newTitle))
      {
        this.setTitle(newTitle);
        this._titles[state] = newTitle;
      }

      if (this.getState() !== state) {
        this._writeState(state);
      }
    },


    /**
     * Navigates back in the browser history.
     * Simulates a back button click.
     */
     navigateBack : function() {
       qx.event.Timer.once(function() {history.back();}, this, 100);
     },


    /**
     * Navigates forward in the browser history.
     * Simulates a forward button click.
     */
     navigateForward : function() {
       qx.event.Timer.once(function() {history.forward();}, this, 100);
     },


    /**
     * Called on changes to the history using the browser buttons.
     *
     * @param state {String} new state of the history
     */
    _onHistoryLoad : function(state)
    {
      this.setState(state);
      this.fireDataEvent("request", state);
      if (this._titles[state] != null) {
        this.setTitle(this._titles[state]);
      }
    },


    /**
     * Browser dependent function to read the current state of the history
     *
     * @return {String} current state of the browser history
     */
    _readState : function() {
      throw new Error("Abstract method call");
    },


    /**
     * Save a state into the browser history.
     *
     */
    _writeState : function() {
      throw new Error("Abstract method call");
    },


    /**
     * Sets the fragment identifier of the window URL
     *
     * @param value {String} the fragment identifier
     */
    _setHash : function (value)
    {
      var url = this._baseUrl + (value || "");
      var loc = window.location;

      if (url != loc.href) {
        loc.href = url;
      }
    },


    /**
     * Returns the fragment identifier of the top window URL. For gecko browsers we
     * have to use a regular expression to avoid encoding problems.
     *
     * @return {String} the fragment identifier
     */
    _getHash : function()
    {
      var hash = /#(.*)$/.exec(window.location.href);
      return hash && hash[1] ? hash[1] : "";
    }
  },


  destruct : function()
  {
    this._titles = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2012 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Fabian Jakobs (fjakobs)
     * Mustafa Sak (msak)

************************************************************************ */

/**
 * History manager implementation for IE greater 7. IE reloads iframe
 * content on history actions even just hash value changed. This
 * implementation forwards history states (hashes) to a helper iframe.
 *
 * @internal
 */
qx.Class.define("qx.bom.HashHistory",
{
  extend : qx.bom.History,

  construct : function()
  {
    this.base(arguments);
    this._baseUrl = null;
    this.__initIframe();
  },


  members :
  {
    __checkOnHashChange : null,
    __iframe : null,
    __iframeReady : false,


    //overridden
    addToHistory : function(state, newTitle)
    {
      if (!qx.lang.Type.isString(state)) {
        state = state + "";
      }

      if (qx.lang.Type.isString(newTitle))
      {
        this.setTitle(newTitle);
        this._titles[state] = newTitle;
      }

      if (this.getState() !== state) {
        this._writeState(state);
      }
    },


    /**
     * Initializes the iframe
     *
     */
    __initIframe : function()
    {
      this.__iframe = this.__createIframe();
      document.body.appendChild(this.__iframe);

      this.__waitForIFrame(function()
      {
        this._baseUrl = this.__iframe.contentWindow.document.location.href;
        this.__attachListeners();
      }, this);
    },


    /**
     * IMPORTANT NOTE FOR IE:
     * Setting the source before adding the iframe to the document.
     * Otherwise IE will bring up a "Unsecure items ..." warning in SSL mode
     *
     * @return {Element}
     */
    __createIframe : function ()
    {
      var iframe = qx.bom.Iframe.create({
        src : qx.util.ResourceManager.getInstance().toUri(qx.core.Environment.get("qx.blankpage")) + "#"
      });

      iframe.style.visibility = "hidden";
      iframe.style.position = "absolute";
      iframe.style.left = "-1000px";
      iframe.style.top = "-1000px";

      return iframe;
    },


    /**
     * Waits for the IFrame being loaded. Once the IFrame is loaded
     * the callback is called with the provided context.
     *
     * @param callback {Function} This function will be called once the iframe is loaded
     * @param context {Object?window} The context for the callback.
     * @param retry {Integer} number of tries to initialize the iframe
     */
    __waitForIFrame : function(callback, context, retry)
    {
      if (typeof retry === "undefined") {
        retry = 0;
      }

      if ( !this.__iframe.contentWindow || !this.__iframe.contentWindow.document )
      {
        if (retry > 20) {
          throw new Error("can't initialize iframe");
        }

        qx.event.Timer.once(function() {
          this.__waitForIFrame(callback, context, ++retry);
        }, this, 10);

        return;
      }

      this.__iframeReady = true;
      callback.call(context || window);
    },


    /**
     * Attach hash change listeners
     */
    __attachListeners : function()
    {
      qx.event.Idle.getInstance().addListener("interval", this.__onHashChange, this);
    },


    /**
     * Remove hash change listeners
     */
    __detatchListeners : function()
    {
      qx.event.Idle.getInstance().removeListener("interval", this.__onHashChange, this);
    },


    /**
     * hash change event handler
     */
    __onHashChange : function()
    {
      var currentState = this._readState();

      if (qx.lang.Type.isString(currentState) && currentState != this.getState()) {
        this._onHistoryLoad(currentState);
      }
    },


    /**
     * Browser dependent function to read the current state of the history
     *
     * @return {String} current state of the browser history
     */
    _readState : function() {
      var hash = !this._getHash() ? "" : this._getHash().substr(1);
      return this._decode(hash);
    },


    /**
     * Returns the fragment identifier of the top window URL. For gecko browsers we
     * have to use a regular expression to avoid encoding problems.
     *
     * @return {String|null} the fragment identifier or <code>null</code> if the
     * iframe isn't ready yet
     */
    _getHash : function()
    {
      if (!this.__iframeReady){
        return null;
      }
      return this.__iframe.contentWindow.document.location.hash;
    },


    /**
     * Save a state into the browser history.
     *
     * @param state {String} state to save
     */
    _writeState : function(state)
    {
      this._setHash(this._encode(state));
    },


    /**
     * Sets the fragment identifier of the window URL
     *
     * @param value {String} the fragment identifier
     */
    _setHash : function (value)
    {
      if (!this.__iframe || !this._baseUrl){
        return;
      }
      var hash = !this.__iframe.contentWindow.document.location.hash ? "" : this.__iframe.contentWindow.document.location.hash.substr(1);
      if (value != hash) {
        this.__iframe.contentWindow.document.location.hash = value;
      }
    }
  },


  destruct : function() {
    this.__detatchListeners();
    this.__iframe = null;
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * This handler provides a "load" event for iframes
 */
qx.Class.define("qx.event.handler.Iframe",
{
  extend : qx.core.Object,
  implement : qx.event.IEventHandler,





  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {
    /** @type {Integer} Priority of this handler */
    PRIORITY : qx.event.Registration.PRIORITY_NORMAL,

    /** @type {Map} Supported event types */
    SUPPORTED_TYPES : {
      load: 1,
      navigate: 1
    },

    /** @type {Integer} Which target check to use */
    TARGET_CHECK : qx.event.IEventHandler.TARGET_DOMNODE,

    /** @type {Integer} Whether the method "canHandleEvent" must be called */
    IGNORE_CAN_HANDLE : false,

    /**
     * Internal function called by iframes created using {@link qx.bom.Iframe}.
     *
     * @signature function(target)
     * @internal
     * @param target {Element} DOM element which is the target of this event
     */
    onevent : qx.event.GlobalError.observeMethod(function(target) {

      // Fire navigate event when actual URL diverges from stored URL
      var currentUrl = qx.bom.Iframe.queryCurrentUrl(target);

      if (currentUrl !== target.$$url) {
        qx.event.Registration.fireEvent(target, "navigate", qx.event.type.Data, [currentUrl]);
        target.$$url = currentUrl;
      }

      // Always fire load event
      qx.event.Registration.fireEvent(target, "load");
    })
  },





  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {
    /*
    ---------------------------------------------------------------------------
      EVENT HANDLER INTERFACE
    ---------------------------------------------------------------------------
    */

    // interface implementation
    canHandleEvent : function(target, type) {
      return target.tagName.toLowerCase() === "iframe"
    },


    // interface implementation
    registerEvent : function(target, type, capture) {
      // Nothing needs to be done here
    },


    // interface implementation
    unregisterEvent : function(target, type, capture) {
      // Nothing needs to be done here
    }


  },





  /*
  *****************************************************************************
     DEFER
  *****************************************************************************
  */

  defer : function(statics) {
    qx.event.Registration.addHandler(statics);
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Jonathan Weiß (jonathan_rass)
     * Christian Hagendorn (Chris_schmidt)

************************************************************************ */

/**
 * Cross browser abstractions to work with iframes.
 *
 * @require(qx.event.handler.Iframe)
 */
qx.Class.define("qx.bom.Iframe",
{
  /*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

  statics :
  {
    /**
     * @type {Map} Default attributes for creation {@link #create}.
     */
    DEFAULT_ATTRIBUTES :
    {
      onload : "qx.event.handler.Iframe.onevent(this)",
      frameBorder: 0,
      frameSpacing: 0,
      marginWidth: 0,
      marginHeight: 0,
      hspace: 0,
      vspace: 0,
      border: 0,
      allowTransparency: true
    },

    /**
     * Creates an DOM element.
     *
     * Attributes may be given directly with this call. This is critical
     * for some attributes e.g. name, type, ... in many clients.
     *
     * @param attributes {Map?null} Map of attributes to apply
     * @param win {Window?null} Window to create the element for
     * @return {Element} The created iframe node
     */
    create : function(attributes, win)
    {
      // Work on a copy to not modify given attributes map
      var attributes = attributes ? qx.lang.Object.clone(attributes) : {};
      var initValues = qx.bom.Iframe.DEFAULT_ATTRIBUTES;

      for (var key in initValues)
      {
        if (attributes[key] == null) {
          attributes[key] = initValues[key];
        }
      }

      return qx.dom.Element.create("iframe", attributes, win);
    },


    /**
     * Get the DOM window object of an iframe.
     *
     * @param iframe {Element} DOM element of the iframe.
     * @return {Window?null} The DOM window object of the iframe or null.
     * @signature function(iframe)
     */
    getWindow : function(iframe)
    {
      try {
        return iframe.contentWindow;
      } catch(ex) {
        return null;
      }
    },


    /**
     * Get the DOM document object of an iframe.
     *
     * @param iframe {Element} DOM element of the iframe.
     * @return {Document} The DOM document object of the iframe.
     */
    getDocument : function(iframe)
    {
      if ("contentDocument" in iframe) {
        try {
          return iframe.contentDocument;
        } catch(ex) {
          return null;
        }
      }

      try {
        var win = this.getWindow(iframe);
        return win ? win.document : null;
      } catch(ex) {
        return null;
      }
    },


    /**
     * Get the HTML body element of the iframe.
     *
     * @param iframe {Element} DOM element of the iframe.
     * @return {Element} The DOM node of the <code>body</code> element of the iframe.
     */
    getBody : function(iframe)
    {
      try
      {
        var doc = this.getDocument(iframe);
        return doc ? doc.getElementsByTagName("body")[0] : null;
      }
      catch(ex)
      {
        return null
      }
    },


    /**
     * Sets iframe's source attribute to given value
     *
     * @param iframe {Element} DOM element of the iframe.
     * @param source {String} URL to be set.
     * @signature function(iframe, source)
     */
    setSource : function(iframe, source)
    {
      try
      {
        // the guru says ...
        // it is better to use 'replace' than 'src'-attribute, since 'replace'
        // does not interfere with the history (which is taken care of by the
        // history manager), but there has to be a loaded document
        if (this.getWindow(iframe) && qx.dom.Hierarchy.isRendered(iframe))
        {
          /*
            Some gecko users might have an exception here:
            Exception... "Component returned failure code: 0x805e000a
            [nsIDOMLocation.replace]"  nsresult: "0x805e000a (<unknown>)"
          */
          try
          {
            // Webkit on Mac can't set the source when the iframe is still
            // loading its current page
            if ((qx.core.Environment.get("engine.name") == "webkit") &&
                qx.core.Environment.get("os.name") == "osx")
            {
              var contentWindow = this.getWindow(iframe);
              if (contentWindow) {
                contentWindow.stop();
              }
            }
            this.getWindow(iframe).location.replace(source);
          }
          catch(ex)
          {
            iframe.src = source;
          }
        }
        else
        {
          iframe.src = source;
        }

      // This is a programmer provided source. Remember URL for this source
      // for later comparison with current URL. The current URL can diverge
      // if the end-user navigates in the Iframe.
      this.__rememberUrl(iframe);

      }
      catch(ex) {
        qx.log.Logger.warn("Iframe source could not be set!");
      }
    },


    /**
     * Returns the current (served) URL inside the iframe
     *
     * @param iframe {Element} DOM element of the iframe.
     * @return {String} Returns the location href or null (if a query is not possible/allowed)
     */
    queryCurrentUrl : function(iframe)
    {
      var doc = this.getDocument(iframe);

      try
      {
        if (doc && doc.location) {
          return doc.location.href;
        }
      }
      catch(ex) {};

      return "";
    },


    /**
    * Remember actual URL of iframe.
    *
    * @param iframe {Element} DOM element of the iframe.
    */
    __rememberUrl: function(iframe)
    {

      // URL can only be detected after load. Retrieve and store URL once.
      var callback = function() {
        qx.bom.Event.removeNativeListener(iframe, "load", callback);
        iframe.$$url = qx.bom.Iframe.queryCurrentUrl(iframe);
      }

      qx.bom.Event.addNativeListener(iframe, "load", callback);
    }

  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Fabian Jakobs (fjakobs)
     * Mustafa Sak (msak)

************************************************************************ */

/**
 * Implements an iFrame based history manager for IE 6/7/8.
 *
 * Creates a hidden iFrame and uses document.write to store entries in the
 * history browser's stack.
 *
 * @internal
 */
qx.Class.define("qx.bom.IframeHistory",
{
  extend : qx.bom.History,


  construct : function()
  {
    this.base(arguments);
    this.__initTimer();
  },


  members :
  {
    __iframe : null,
    __iframeReady : false,
    __writeStateTimner : null,
    __dontApplyState : null,
    __locationState : null,


    // overridden
    _setInitialState : function()
    {
      this.base(arguments);
      this.__locationState = this._getHash();
    },


    //overridden
    _setHash : function(value)
    {
      this.base(arguments, value);
      this.__locationState = this._encode(value);
    },


    //overridden
    addToHistory : function(state, newTitle)
    {
      if (!qx.lang.Type.isString(state)) {
        state = state + "";
      }

      if (qx.lang.Type.isString(newTitle))
      {
        this.setTitle(newTitle);
        this._titles[state] = newTitle;
      }

      if (this.getState() !== state) {
        this.setState(state);
      }
      this.fireDataEvent("request", state);
    },


    //overridden
    _onHistoryLoad : function(state)
    {
      this._setState(state);
      this.fireDataEvent("request", state);
      if (this._titles[state] != null) {
        this.setTitle(this._titles[state]);
      }
    },


    /**
     * Helper function to set state property. This will only be called
     * by _onHistoryLoad. It determines, that no apply of state will be called.
     * @param state {String} State loaded from history
     */
    _setState : function(state)
    {
      this.__dontApplyState = true;
      this.setState(state);
      this.__dontApplyState = false;
    },


    //overridden
    _applyState : function(value, old)
    {
      if (this.__dontApplyState){
        return;
      }
      this._writeState(value);
    },


    /**
     * Get state from the iframe
     *
     * @return {String} current state of the browser history
     */
    _readState : function()
    {
      if (!this.__iframeReady) {
        return this._decode(this._getHash());
      }

      var doc = this.__iframe.contentWindow.document;
      var elem = doc.getElementById("state");
      return elem ? this._decode(elem.innerText) : "";
    },


    /**
     * Store state to the iframe
     *
     * @param state {String} state to save
     */
    _writeState : function(state)
    {
      if (!this.__iframeReady) {
        this.__clearWriteSateTimer();
        this.__writeStateTimner = qx.event.Timer.once(function(){this._writeState(state);}, this, 50);
        return;
      }
      this.__clearWriteSateTimer();

      var state = this._encode(state);

      // IE8 is sometimes recognizing a hash change as history entry. Cause of sporadic surface of this behavior, we have to prevent setting hash.
      if (qx.core.Environment.get("engine.name") == "mshtml" && qx.core.Environment.get("browser.version") != 8){
        this._setHash(state);
      }

      var doc = this.__iframe.contentWindow.document;
      doc.open();
      doc.write('<html><body><div id="state">' + state + '</div></body></html>');
      doc.close();
    },


    /**
     * Helper function to clear the write state timer.
     */
    __clearWriteSateTimer : function()
    {
      if (this.__writeStateTimner){
        this.__writeStateTimner.stop();
        this.__writeStateTimner.dispose();
      }
    },


    /**
     * Initialize the polling timer
     */
    __initTimer : function()
    {
      this.__initIframe(function () {
        qx.event.Idle.getInstance().addListener("interval", this.__onHashChange, this);
      });
    },


    /**
     * Hash change listener.
     *
     * @param e {qx.event.type.Event} event instance
     */
    __onHashChange : function(e)
    {
      // the location only changes if the user manually changes the fragment
      // identifier.
      var currentState = null;
      var locationState = this._getHash();

      if (!this.__isCurrentLocationState(locationState)) {
        currentState = this.__storeLocationState(locationState);
      } else {
        currentState = this._readState();
      }
      if (qx.lang.Type.isString(currentState) && currentState != this.getState()) {
        this._onHistoryLoad(currentState);
      }
    },


    /**
     * Stores the given location state.
     *
     * @param locationState {String} location state
     * @return {String}
     */
    __storeLocationState : function (locationState)
    {
      locationState = this._decode(locationState);
      this._writeState(locationState);

      return locationState;
    },


    /**
     * Checks whether the given location state is the current one.
     *
     * @param locationState {String} location state to check
     * @return {Boolean}
     */
    __isCurrentLocationState : function (locationState) {
      return qx.lang.Type.isString(locationState) && locationState == this.__locationState;
    },


    /**
     * Initializes the iframe
     *
     * @param handler {Function?null} if given this callback is executed after iframe is ready to use
     */
    __initIframe : function(handler)
    {
      this.__iframe = this.__createIframe();
      document.body.appendChild(this.__iframe);

      this.__waitForIFrame(function()
      {
        this._writeState(this.getState());

        if (handler) {
          handler.call(this);
        }
      }, this);
    },


    /**
     * IMPORTANT NOTE FOR IE:
     * Setting the source before adding the iframe to the document.
     * Otherwise IE will bring up a "Unsecure items ..." warning in SSL mode
     *
     * @return {Iframe}
     */
    __createIframe : function ()
    {
      var iframe = qx.bom.Iframe.create({
        src : qx.util.ResourceManager.getInstance().toUri(qx.core.Environment.get("qx.blankpage"))
      });

      iframe.style.visibility = "hidden";
      iframe.style.position = "absolute";
      iframe.style.left = "-1000px";
      iframe.style.top = "-1000px";

      return iframe;
    },


    /**
     * Waits for the IFrame being loaded. Once the IFrame is loaded
     * the callback is called with the provided context.
     *
     * @param callback {Function} This function will be called once the iframe is loaded
     * @param context {Object?window} The context for the callback.
     * @param retry {Integer} number of tries to initialize the iframe
     */
    __waitForIFrame : function(callback, context, retry)
    {
      if (typeof retry === "undefined") {
        retry = 0;
      }

      if ( !this.__iframe.contentWindow || !this.__iframe.contentWindow.document )
      {
        if (retry > 20) {
          throw new Error("can't initialize iframe");
        }

        qx.event.Timer.once(function() {
          this.__waitForIFrame(callback, context, ++retry);
        }, this, 10);

        return;
      }

      this.__iframeReady = true;
      callback.call(context || window);
    }
  },


  destruct : function()
  {
    this.__iframe = null;
    if (this.__writeStateTimner){
      this.__writeStateTimner.dispose();
      this.__writeStateTimner = null;
    }
    qx.event.Idle.getInstance().removeListener("interval", this.__onHashChange, this);
  }
});
/* ************************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2004-2008 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Andreas Ecker (ecker)
     * Fabian Jakobs (fjakobs)

************************************************************************ */

/**
 * Default history manager implementation. Either polls for URL fragment
 * identifier (hash) changes or uses the native "hashchange" event.
 *
 * @internal
 */
qx.Class.define("qx.bom.NativeHistory",
{
  extend : qx.bom.History,

  construct : function()
  {
    this.base(arguments);
    this.__attachListeners();
  },


  members :
  {
    __checkOnHashChange : null,


    /**
     * Attach hash change listeners
     */
    __attachListeners : function()
    {
      if (qx.bom.History.SUPPORTS_HASH_CHANGE_EVENT)
      {
        var boundFunc = qx.lang.Function.bind(this.__onHashChange, this);
        this.__checkOnHashChange = qx.event.GlobalError.observeMethod(boundFunc);
        qx.bom.Event.addNativeListener(window, "hashchange", this.__checkOnHashChange);
      }
      else
      {
        qx.event.Idle.getInstance().addListener("interval", this.__onHashChange, this);
      }
    },


    /**
     * Remove hash change listeners
     */
    __detatchListeners : function()
    {
      if (qx.bom.History.SUPPORTS_HASH_CHANGE_EVENT) {
        qx.bom.Event.removeNativeListener(window, "hashchange", this.__checkOnHashChange);
      } else {
        qx.event.Idle.getInstance().removeListener("interval", this.__onHashChange, this);
      }
    },


    /**
     * hash change event handler
     */
    __onHashChange : function()
    {
      var currentState = this._readState();

      if (qx.lang.Type.isString(currentState) && currentState != this.getState()) {
        this._onHistoryLoad(currentState);
      }
    },


    /**
     * Browser dependent function to read the current state of the history
     *
     * @return {String} current state of the browser history
     */
    _readState : function() {
      return this._decode(this._getHash());
    },


    /**
     * Save a state into the browser history.
     *
     * @param state {String} state to save
     */
    _writeState : qx.core.Environment.select("engine.name",
    {
      "opera" : function(state)
      {
        qx.event.Timer.once(function()
        {
          this._setHash(this._encode(state));
        }, this, 0);
      },

      "default" : function (state) {
        this._setHash(this._encode(state));
      }
    })
  },


  destruct : function() {
    this.__detatchListeners();
  }
});
