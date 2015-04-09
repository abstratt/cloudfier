var cloudfier = cloudfier || {};

cloudfier.corelib = {
    reloadPage : function (url) {
        window.location = url || window.location;
    }, 
    camelCaseToSentence : function (camelCase) {
        return qx.lang.String.capitalize(qx.lang.String.hyphenate(camelCase).replace(/-/g, ' ').replace(/^\\s\\s*/, '').replace(/\\s\\s*\$/, ''));
    },
    
    isLoggedIn: function() {
        var currentUser = cloudfier.currentUser;
        return currentUser && currentUser.username && "guest" !== currentUser.username;
    },
    
    refreshCurrentUser: function () {
        var currentUserReq = new qx.io.request.Xhr(cloudfier.apiBase, "GET");
        currentUserReq.addListenerOnce("success", function(e) {
            cloudfier.applicationName = currentUserReq.getResponse().applicationName;
            cloudfier.currentUser = currentUserReq.getResponse().currentUser;
            cloudfier.application.fireDataEvent("currentUserChanged", cloudfier.currentUser);
            
        }, this);
        currentUserReq.addListenerOnce("statusError", function(e) {
            cloudfier.currentUser = undefined;
            cloudfier.applicationName = "Error";
            cloudfier.application.fireDataEvent("currentUserChanged", cloudfier.currentUser);
        }, this);
        currentUserReq.send();
    },
    
    logout: function() {
        var req = new qx.io.request.Xhr(cloudfier.apiBase + "logout", "GET");
        req.send();
        req.addListener("success", function(e) {
            this.refreshCurrentUser(); 
            cloudfier.corelib.reloadPage();
        }, this);
        req.addListener("statusError", function(e) {
            if (e.getTarget().getStatus() != 401)
                cloudfier.lib.handleError(e.getTarget());
            this.refreshCurrentUser();
            cloudfier.corelib.reloadPage();
        }, this);        
    },

    login: function (username, password, errorHandler, successHandler) {
        var req = new qx.io.request.Xhr(cloudfier.apiBase + "login", "POST");
        req.setRequestData("login="+username+"&password="+password);
        req.send();
        req.addListener("success", function(e) {
            var newLocation = window.location; 
            if (window.location.search) {
                var params = window.location.search.slice(1).split("&");
                for (var i = 0; i < params.length; i++)
                {
                    var tmp = params[i].split("=");
                    if (tmp[0] === 'source' && tmp.length == 2 && tmp[1]) {
                        newLocation = unescape(tmp[1]);
                        break;
                    }
                }
            }
            successHandler && successHandler();
            this.refreshCurrentUser();
            cloudfier.corelib.reloadPage(newLocation);
        }, this);
        req.addListener("statusError", function(e) {
            errorHandler && errorHandler(e);
            
            if (e.getTarget().getStatus() != 401)
                cloudfier.lib.handleError(e.getTarget());
        });
    }
};   
