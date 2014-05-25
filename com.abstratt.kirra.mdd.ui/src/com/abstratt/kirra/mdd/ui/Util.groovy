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
	                    else if (it.type.name == 'Double' || it.type.name == 'Integer')
	                        // use a text field for integers and doubles
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