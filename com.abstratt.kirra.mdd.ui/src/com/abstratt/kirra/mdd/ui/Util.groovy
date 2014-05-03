package com.abstratt.kirra.mdd.ui

import org.eclipse.uml2.uml.*
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Package

class Util extends KirraUIHelper {

	static String buildRequest(actionUriVar, modelVar, methodExpr) {
	"""
	(function() {
	    var req = new qx.io.request.Xhr($actionUriVar, $methodExpr);
	    req.setRequestHeader("Content-Type", "application/json");
	    req.setRequestData(qx.util.Serializer.toJson($modelVar, (function() {}), cloudfier.qooxdooISODateFormat));
	    return req;
	})();
	"""
	}

    static String getDomainURL(String recordVar, Property property) {
         "(${recordVar} && ${recordVar}.getValues() && ${recordVar}.getValues().get${getSymbol(property).capitalize()} && ${recordVar}.getValues().get${getSymbol(property).capitalize()}() && ${recordVar}.getValues().get${getSymbol(property).capitalize()}().getDomainUri) ? ${recordVar}.getValues().get${getSymbol(property).capitalize()}().getDomainUri() : ${getInstancesURL(property.type)}"
    }
    
    static String getDomainURL(String recordVar, Parameter parameter) {
         "(${recordVar} && ${recordVar}.getActions() && ${recordVar}.getActions().get${getSymbol(parameter.operation).capitalize()} && ${recordVar}.getActions().get${getSymbol(parameter.operation).capitalize()}().getParameters().get${getSymbol(parameter).capitalize()}() && ${recordVar}.getActions().get${getSymbol(parameter.operation).capitalize()}().getParameters().get${getSymbol(parameter).capitalize()}().getDomainUri) ? ${recordVar}.getActions().get${getSymbol(parameter.operation).capitalize()}().getParameters().get${getSymbol(parameter).capitalize()}().getDomainUri() : ${getInstancesURL(parameter.type)}"
    }

    static String getInstancesURL(entity) {
        "cloudfier.apiBase + 'instances/${entity.qualifiedName.replaceAll('::', '.')}/'"
    }
    
    static getFinderURL(entity, finder) {
	    "cloudfier.apiBase + 'queries/${entity.qualifiedName.replaceAll('::', '.')}/${getName(finder)}'"
	}

	static getStaticActionURL(entity, action) {
	    "cloudfier.apiBase + 'actions/${entity.qualifiedName.replaceAll('::', '.')}/${getName(action)}'"
	}

	static convertPropertyToJS(recordExpression, property, converter) {
	"""
	if (${recordExpression}.values['${getName(property)}'] != null) {
	    ${recordExpression}.values['${getName(property)}'] = ${converter("${recordExpression}.values['${getName(property)}']")};
	}
	"""}
	
	static convertDateFromStringToJS(recordExpression, property) {
	    return convertPropertyToJS(recordExpression, property, { expression -> "cloudfier.qooxdooISODateFormat.parse(${expression})" })
	}
	
	static convertLinkToShorthand(recordExpression, property) {
	    return convertPropertyToJS(recordExpression, property, { expression -> "${expression} ? ${expression}.shorthand : null" })
	}
	
	static convertLinkToUri(recordExpression, property) {
	    return convertPropertyToJS(recordExpression, property, { expression -> "${expression} ? ${expression}.uri : null" })
	}
	
	static convertToString(recordExpression, property) {
	    return convertPropertyToJS(recordExpression, property, { expression -> "'' + ${expression}" })
	}
	
	static convertCamelCaseToTitleCase(recordExpression, property) {
	    return convertPropertyToJS(recordExpression, property, { expression -> "cloudfier.corelib.camelCaseToSentence($expression)" })
	}
	
	static recordConverter(entity, recordExpression, editable, creation = false) {
	"""
	        if (${recordExpression}.values) {
	            ${  
	                def statements = ['// converting ']
	                getFormFields(entity).each {
	                    if (it.type.name == 'Date' && editable && isEditable(it, creation))
	                        statements << convertDateFromStringToJS(recordExpression, it)
	                    else if (isEntity(it.type))
	                        statements << ((editable && !isReadOnly(it, creation)) ? convertLinkToUri(recordExpression, it) : convertLinkToShorthand(recordExpression, it))
	                    else if (it.type.name == 'Integer' && (!editable || isReadOnly(it, creation)))
	                        statements << convertToString(recordExpression, it)
	                    else if (it.type.name == 'Double')
	                        // use a text field for doubles
	                        statements << convertToString(recordExpression, it)
	                    else if (it.type instanceof StateMachine)
	                        statements << convertCamelCaseToTitleCase(recordExpression, it);
	                    // ensure all properties are defined, even if the JSON was missing some (Qooxdoo would throw an exception if we try to get their values later)
                        statements << """ 	                        
                    	if (${recordExpression}.values['${getName(it)}'] === undefined) {
	                        ${recordExpression}.values['${getName(it)}'] = null;
	                    }
	                    """
	                }
	                return statements.join("\n")
	            }
	        } 
	"""
	}
	
	static createDataManipulator(entity, creation = true) {
	"""
	function(data) {
	    for (var i = 0; i < data.length; i++) {
	        ${ recordConverter(entity, 'data[i]', false, creation) }
	    }
	    return data;
	}
	"""
	}
	
	static createEntityStore(entity) {
	"""
	var ${getSymbol(entity)}DataManipulator = ${createDataManipulator(entity)}; 
	
	var ${getSymbol(entity)}Store = new cloudfier.store.JsonStore(null, { manipulateData: ${getSymbol(entity)}DataManipulator });
	${getSymbol(entity)}Store.addListener("error", function(e) {
	    if (e.getData().getStatus() == 401) {
	        cloudfier.lib.showLoginWindow();
	    } else {
	        cloudfier.lib.handleError(e.getTarget());        
	    }
	});
	
	cloudfier.registry['${getName(entity)}'].store = ${getSymbol(entity)}Store; 
	"""
	}

	static splitOnCamelCase(string) {
	    return string.collect{ ((it as char).upperCase ? ' ' : '') + it }.join()-' ' 
	}
	
	static modelToScreen(string) {
	    return splitOnCamelCase(string.capitalize()) 
	}
	
	static entityObject(Class entity) {
	    return "cloudfier.registry['${getSymbol(entity)}']"
    }
    
    static current() {
        return "cloudfier.registry._current_"
    }
    
    static buildTasker() {
	"""
		{
		    /** The task currently running (null if no task is currently running). */
		    running : null,	
		    /** Queue of scheduled tasks pending execution. */
		    pending : [],
		    /** Runs the next pending task if no task is currently running . Normally not invoked directly by client code. */
		    runNext : function () {
		        console.log("running next");
		        var me = this;
		        if (me.running !== null) {
		            console.log("Cannot run next, task still in progress: " + me.running.uri);
		            return;
		        }
		        if (me.pending.length == 0) {
		            console.log("Cannot run next, nothing left to run");
		            return;
		        }
		        var next = me.pending.shift();
		        me.running = next;
		        console.log("Running " + me.running.uri);
		        me.running.run(function () {
		            console.log("Completed " + (me.running && me.running.uri));
		            me.running = null;
		            me.runNext();    
		        }); 
		    },        
		    /** 
		     * Schedules a task. A task is an object with the following slots:
		     *     - run: The task behavior. A function that takes a 'next' function as parameter, which must be invoked at completion of the task.  
		     *     - uri: Not necessarily a URI, this task id. Two task objects with the same uri ar considered to be the same task. Rescheduling a task
		     *                that is currently running does nothing. Rescheduling a task that is currently rescheduled moved it to the end of the queue. 
		     *     - context: The task's context, if any. Scheduling a task with a context will unschedule any pending tasks for a different context. If a
		     * task with a different context is currently running, it will no longer be tracked. 
		     */
		    schedule : function (task) {
		        console.log("Requested scheduling of task " + task.uri + " for context " + task.context);
		        for (var i = this.pending.length - 1; i >= 0; i--) {
		            if (this.pending[i].context && task.context && this.pending[i].context !== task.context) {
		                // found a pending task with a stale context, get rid of it
		                console.log("Removing stale task " + this.pending[i].uri + " for context " + this.pending[i].context);
		                this.pending.splice(i, 1);
		            } else if (this.pending[i].uri === task.uri) {
		                // already scheduled, just move it back to the end of the line
		                this.pending.splice(i, 1);
		                console.log("Removing previously scheduled " + task.uri);
		            }
		        }
		        if (this.running) {
		            if (this.running.context && task.context && this.running.context !== task.context) {
		                console.log("Forgetting running task " + this.running.uri + " for context " + this.running.context);
		                this.running = null;
		            } else if (this.running.uri === task.uri) { 
		                console.log("Already running, not rescheduled " + task.uri);
		                return;
		            }
		        }
		        this.pending.push(task);
		        console.log("Scheduled " + task.uri);
		        this.runNext();
		    }
		}
	"""
	}
	
	static buildCoreLib() {
	""" 
	{
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
                var currentUser = currentUserReq.getResponse().currentUser;
                cloudfier.currentUser = currentUser;
                cloudfier.application.fireDataEvent("currentUserChanged", currentUser);
                
            }, this);
            currentUserReq.addListenerOnce("statusError", function(e) {
                cloudfier.currentUser = undefined;
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
    } 
	"""
	}
	
	static buildRegistry(namespaces) {
	"""
	{
        ${
	        getEntities(namespaces).findAll { 
	            isConcreteEntity(it)
	        }.collect {
	            """
	                '${getName(it)}': { 
	                    name: '${getName(it)}',
	                    loaded: false,
	                    store: undefined,
	                    page: undefined,
	                    relatedStores: [
	                        // objects in the form: {store: ..., relationship: ... }
	                    ],
	                    reloadStore : function () {
                            if (this.loaded) {
					            this.store.reload();
					        } else {
					            console.log("Loading store at: " + ${getInstancesURL(it)});
					            this.store.setUrl(${getInstancesURL(it)});
					        }
					        this.loaded = true;
	                    },
	                    pageSelected : function () {
	                        // do nothing, proper implementation to be installed by entity-specific script
	                    } 
	                }
	            """
	        }.join(',\n\t\t')
	    }
	}
	"""
	}
	
	static createStores(namespaces) {
        getEntities(namespaces).findAll { 
            isConcreteEntity(it)
        }.collect {
            createEntityStore(it)
        }.join('\n')
	}
	
	static defineJsonStore() {
	"""
        qx.Class.define("cloudfier.store.JsonStore", {
            extend : qx.data.store.Json,
        events:
           {
             "recordSelected" : "qx.event.type.Data"
           }
        });
	"""
	}
}