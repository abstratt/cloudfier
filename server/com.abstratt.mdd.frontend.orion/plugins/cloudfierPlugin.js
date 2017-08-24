/*******************************************************************************
 * @license
 * Copyright (c) 2012 Abstratt Technologies.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Rafael Chaves - initial API and implementation
 *******************************************************************************/
window.onload = function() {

var xhr;

require(["dojo/request/xhr"], function(ref) {
    console.log("Loaded");
    console.log(ref);
    xhr = ref;
});

var buildOrionContentLocation = function(original) {
    var components = original.match(/^\/file\/([^\/]*)\/(.*)$/);
    var user = components[1];
    return "/file/"+ user + "-OrionContent/" + components[2];
}

var buildProjectPath = function (args, context, slot) {
    slot = slot || 'application';
    var path = args[slot].file ? args[slot].file.path : args[slot].path;
    var current = context.cwd;
    
    if (path === '.') {
        path = '';
    } else if (path.startsWith('./') && path.length > 2) {
        path = path.substring(2); 
    }
    current = current.replace('-OrionContent', '');
    path = path.replace('-OrionContent', '');
    if (current.indexOf('/workspace/') == 0)
        // workaround for Orion bug #420829
        return path;
    // the extra path separator is not always required
    return (current+'/'+path).replace(/\/+/g,'/');
};

var checkSyntax = function(title, contents) {
    return dojo.xhrGet({
         handleAs: 'json',
         url: "/services/builder" + title.replace('-OrionContent', ''),
         load: function(result) {
             return result
         },
         error: function(error) {
             return {
                 problems: [{
                      reason: "Error accessing validation server (" + error.message + ")",
                      severity: "error",
                      end: 1,
                      character: 1,
                      line: 1
                 }]
             };
         }
    });
};

var computeOutline = function(editorContext, options) {
    var result = editorContext.getText().then(function(text) {
        return dojo.xhrPost({
             postData: text,
             handleAs: 'json',
             url: "/services/analyzer/?defaultExtension=tuml",
             load: function(result) {
                 return result;
             }
        });
    });
    return result;
};

var autoFormat = function(selectedText, text, selection, resource) {
    return dojo.xhrPost({
         postData: text,
         handleAs: 'text',
         url: "/services/formatter/?fileName=" + resource.replace('-OrionContent', ''),
         load: function(result) {
             return { text: result, selection: null };
         }
    });
}; 

var computeProposals = function(prefix, buffer, selection) {
    return [
        {
            proposal: "[Application]\npackage package_name;\n\n/* add classes here */\n\nend.",
            description: 'New package' 
        },
        {
            proposal: "class class_name\n/* add attributes and operations here */\nend;",
            description: 'New class' 
        },
        { 
            proposal: "attribute attribute_name : String;",
            description: 'New attribute' 
        },
        { 
            proposal: "operation operation_name(param1 : String, param2 : Integer) : Boolean;\nbegin\n    /* IMPLEMENT ME */\n    return false;\nend;",
            description: 'New operation' 
        },
        { 
            proposal: "\tattribute status2 : SM1;\n\toperation action1();\n\toperation action2();\n\toperation action3();\n\tstatemachine SM1\n\t\tinitial state State0\n\t\t\ttransition on call(action1) to State1;\n\t\tend;\n\t\tstate State1\n\t\t\ttransition on call(action1) to State1;\n\t\t\ttransition on call(action2) to State2;\n\t\tend;\n\t\tstate State2\n\t\t\ttransition on call(action1) to State1;\n\t\t\ttransition on call(action3) to State3;\n\t\tend;\n\t\tterminate state State3;\n\tend;\n\t\tend;\n",
            description: 'New state machine' 
        }
    ];
};

var locationToWorkspace = function(location) {
    return removeOrionPathPrefix(location).replace(/\/$/g, '').replace(/\//g, '-');
};

var removeOrionPathPrefix = function(location) {
    return location.replace('/file/','').replace('-OrionContent','')
};

/*** SHELL commands */

var standardApplicationError = function(projectPath) {
    var application = projectPath;
    return function (error) {
        if (error.status == 404) {
	        return "Application not found: '" + application + "'. An application is a directory containing a mdd.properties file.";
        } else if (error.status == 400) {
            return JSON.parse(error.responseText).message;
        }
        var parsedMessage;
        try {
            var parsedErrorResponse = JSON.parse(error.responseText);
            parsedMessage = parsedErrorResponse.message;
        } catch (e) {
            console.log("Error parsing error message: " + e);
            console.log("Original error message: " + error.responseText);
            parsedMessage = error.responseText;
        } 
        return "Unexpected error: " + parsedMessage + " (" + error.status + ")";
    };
};

var currentProtocol = function () {
    return window.location.protocol;
};

var formatProblem = function(problem, projectPath) {
    
    return problem.severity.toUpperCase() + ": " + problem.reason + " - [" + problem.file + " : " + problem.line + "](" + currentProtocol() + "/edit/edit.html#" + buildOrionContentLocation(projectPath) + problem.file + ",line=" + problem.line + ")"
};

var formatTestResult = function(testResult, projectPath) {
     var j, location, string = "";
     var passed = testResult.testStatus == 'Pass';
     var symbol = passed ? "\u2714": "\u2718";
     var linkToOperation
     if (testResult.testSourceLocation !== undefined) {     
         linkToOperation = currentProtocol() + "/edit/edit.html#" + buildOrionContentLocation(projectPath) + testResult.testSourceLocation.filename + ",line=" + testResult.testSourceLocation.lineNumber;
         string += "[" + symbol + "](" + linkToOperation + ")";
     } else {
         string += symbol;
     }
     string += " " + testResult.testClassName + "." + testResult.testCaseName + "\n";
     if (!passed) {
         if (testResult.testMessage) {
             string += "\tCause: " + testResult.testMessage + "\n";
         }
         if (testResult.errorLocation) {
	         for (j in testResult.errorLocation) {
	             location = testResult.errorLocation[j];
 	             string += "\t[" + location.frameName + " (" + location.filename + ":" + location.lineNumber + ")](" + currentProtocol() + "/edit/edit.html#" + buildOrionContentLocation(projectPath) + location.filename + ",line=" + location.lineNumber + ")" + "\n"    
	         }  
         }
     }
     return string;
};

var shellRunTests  = function(args, context) {
    var projectPath = buildProjectPath(args, context);
    var appName = locationToWorkspace(projectPath);
    var url = "/services/api/" + appName + "/tests";
    var deferred;
    var testCases;
    deferred = dojo.xhrGet({
         handleAs: 'json',
         url: "/services/api/" + appName + "/tests",
         load: function(result) {
             testCases = result.testCases;
             return 0;
         },
         error: standardApplicationError(projectPath)
    });
    var testRunner;
    var index = 0;
    var results = [];
    
    var generateResult = function() {
         if (results.length == 0) {
             return "No tests to run" 
         }
		 var message = "";
		 var resultMessage;
		 var i, j;
		 for (i in results) {
		     message += formatTestResult(results[i], projectPath);
		 }
		 return message;
    };
    
    testRunner = function(result) {
        if (!testCases) {
            return result;
        }
        var current = index++
        if (current >= testCases.length) {
            return generateResult();
        }
        deferred.progress("Running test " + current + " " + testCases[current].testClass + " - " + testCases[current].testCase);
        return dojo.xhrPost({
	         postData: '',
	         handleAs: 'json',
	         url: testCases[current].testCaseUri,
	         failOk: true,
	         load: function(result) {
	             deferred.progress("Completed " + testCases[current].testCase);
	             results.push(result);
	             return deferred.then(testRunner);
	         },
	         error: function (error) {
	             var message = (standardApplicationError(projectPath))(error);
	             results.push({testCaseName: testCases[current].testCase, testMessage: message, testStatus: 'Fail'}); 
	             return deferred.then(testRunner);
	         }
	    });
    };
    return deferred.then(testRunner);
};

var shellFullDeploy = function(args, context) {
    var projectPath = buildProjectPath(args, context);
    var deferred;
    deferred = dojo.xhrPost({
         postData: '',
         handleAs: 'json',
         url: "/services/deployer/?path=" + projectPath,
         failOk: true,
         load: function(result) {
             var problems = result.problems;
             for (i in problems) {
                 if (problems[i].severity == 'error') {
                     return "Deploy failed: " + formatProblem(problems[i], projectPath)
                 } else if (problems[i].severity == 'warning') {
                     continue
                 } else {
                     deferred.progress("Application successfully deployed, resetting database...");                            
                     return shellDbDeploy(args, context, problems[i].reason)
                 }
             }
             return ""
         },
         error: standardApplicationError(projectPath)
    });
    return deferred;
};

var shellAppInfo = function(args, context) {
    var projectPath = buildProjectPath(args, context);
    return dojo.xhrGet({
         url: "/services/deployer/?path=" + projectPath,
         failOk: true,
         handleAs: 'json',
         load: function(result) {
             var message = "";
             for (name in result) {
                 message += name + " = " + result[name] + "\n"
             }
             return message + showLinks(projectPath, result.packages);
         },
         error: function(error) {
             if (error.status == 404) {
                 return "No application is deployed yet at \"" + projectPath + "\". Use 'cloudfier full-deploy <application-dir>' to deploy it first";
             }
             return "Unexpected error: " + JSON.parse(error.responseText).message + " (" + error.status + ")";
         }
    });
};

var shellAppUndeploy = function(args, context) {
    var projectPath = buildProjectPath(args, context);
    return dojo.xhrDelete({
         url: "/services/deployer/?path=" + projectPath,
         failOk: true,
         load: function(result) {
             return "Application successfully undeployed";
         },
         error: function(error) {
             if (error.status == 404) {
                 return "Nothing to undeploy at " + projectPath;
             }
             return "Unexpected error: " + JSON.parse(error.responseText).message + " (" + error.status + ")";
         }
    });
};

var shellGenerate = function(args, context) {
    var projectPath = buildProjectPath(args, context);
    var appName = locationToWorkspace(projectPath);
    var platform = args.platform;
    var url = "/services/generator/" + appName + "/platform/" + platform;
    return xhr.get(url, {
         handleAs: 'arraybuffer',
         headers: { "Accept" : "application/zip" }, 
    }).then(unzip('gen'), function(error) {
         if (error.status == 404) {
             return "No application is deployed yet at \"" + projectPath + "\". Use 'cloudfier full-deploy <application-dir>' to deploy it first";
         }
         return "Unexpected error: " + JSON.parse(error.responseText).message + " (" + error.status + ")";
    });
};

var unzip = function(baseDir) {
    return function(result) {
	    var zip = new JSZip(result);
	    var files = zip.files;
	    var newFiles = [];
	    for (var name in files) {
	        if (isDirPath(name)) {
	            /* represents a directory */
	            newFiles.push({path: asDirPath(baseDir) + name.substring(0, name.length - 1), isDirectory: true});
	        } else {
	            var file = zip.file(name);
	            newFiles.push({path: asDirPath(baseDir) + name, blob: file.asArrayBuffer()});
	        }
	    };
	    return newFiles;
    };
};

var asDirPath = function(path) {
    if (isDirPath(path)) {
        return path;
    }
    return path + "/";
};

var isDirPath = function(path) {
    return path && (path.lastIndexOf("/") === (path.length - 1));
};

var shellSchemaCrawlerImport = function(args, context) {
    var cwd = context.cwd;
    var projectPath = buildProjectPath(args, context);
    var snapshotPath = removeOrionPathPrefix(buildProjectPath(args, context, 'schema-crawler-snapshot'));
    var appName = locationToWorkspace(projectPath);
    var url = "/services/importer/" + appName + "/source/schema-crawler?snapshot=" + snapshotPath;
    return xhr.get(url, {
         handleAs: 'arraybuffer',
         headers: { "Accept" : "application/zip" }, 
    }).then(unzip('.'), standardApplicationError(cwd));
};

var insertNewLines = function(lines) {
    var contents = [];
    for(var i = 0;i < lines.length;i++) {
        contents.push(lines[i] + "\n");
    }
    return contents;    
};


var getProjectProperties = function(applicationName) {
    var propertiesFile = [
        '#This file configures a Cloudfier project',
        'mdd.modelWeaver=kirraWeaver',
        'mdd.enableExtensions=true',
        'mdd.enableLibraries=true',
        'mdd.enableTypes=true',
        'mdd.extendBaseObject=true',
        'mdd.application.allowAnonymous=true',
        'mdd.application.loginAllowed=true',
        'mdd.application.name=' + applicationName
    ];
    return insertNewLines(propertiesFile);
};

var shellInitProject = function(args, context) {
    var applicationName = args.applicationName || 'app'; 
    var result = {path: "mdd.properties", isDirectory: false, blob: new Blob(getProjectProperties(applicationName))};
    console.log(result);
    return result;
};

var shellCreateProject = function(args, context) {
    var applicationName = args.applicationName;
    var projectPath = asDirPath(applicationName);
    var entityNames = args.entities.split(',')
    var newFiles = [];
    newFiles.push({path: projectPath + 'mdd.properties', isDirectory: false, blob: new Blob(getProjectProperties(applicationName))});
    newFiles.push({path: projectPath + applicationName + ".tuml", blob: new Blob(insertNewLines(getSimpleEntities(applicationName, entityNames)))});
    console.log(newFiles);
    return newFiles;
};

var shellDBSnapshot = function(args, context) {
    var cwd = context.cwd;
    if (!cwd.indexOf('/file/') == 0) {
        throw Error("Cannot run from the root. CD into a project directory before running this command.");
    }
    var url = ("/services/api/" + locationToWorkspace(cwd) + "/data").replace(/\/+/g,'/');
    return dojo.xhrGet({
         handleAs: 'text',
         url: url,
         load: function(result) {
             var result = {path: "data.json", isDirectory: false, blob: new Blob([result.toString()])};
             console.log(result);
             return result;
         },
         error: standardApplicationError(cwd)
    });
};

var shellCreateEntity = function(args, context) {
    var namespace = args.namespace;
    var entity = args.entity;
    var simpleModel = getSimpleEntity(namespace, entity);
    var result = {path: entity + ".tuml", blob: new Blob(insertNewLines(simpleModel))};
    console.log(result);
    return result;
};

var sanitizeIdentifier = function(toSanitize) {
    var cleaner = /[^A-Za-z0-9]/g;
    return (toSanitize && toSanitize.replace(cleaner, '_')) || toSanitize;
};

var getSimpleEntity = function(namespace, entity) {
    entity = sanitizeIdentifier(entity);
    namespace = sanitizeIdentifier(namespace);
    if (/[0-9]/.test(entity[0])) {
        throw Error("Invalid entity name '" + entity + "' - first character cannot be a digit");
    }
    if (/[0-9]/.test(namespace[0])) {
        throw Error("Invalid namespace name '" + namespace + "' - first character cannot be a digit");
    }
    var simpleModel = [
        '[Application]',
        'package ' + namespace + ';',
        'class ' + entity,
        '    /* Add your own attributes */',
        '    attribute name : String;',
        'end;',
        'end.',
    ];
    return simpleModel;
};

var getSimpleEntities = function(namespace, entityNames) {
    var i;
    for (i in entityNames) {
        entityNames[i] = sanitizeIdentifier(entityNames[i]);
        if (/[0-9]/.test(entityNames[i][0])) {
            throw Error("Invalid entity name '" + entityNames[i] + "' - first character cannot be a digit");
        }
    }
    namespace = sanitizeIdentifier(namespace);
    if (/[0-9]/.test(namespace[0])) {
        throw Error("Invalid namespace name '" + namespace + "' - first character cannot be a digit");
    }
    var simpleModel = [];
    simpleModel.push('[Application]');
    simpleModel.push('package ' + namespace + ';');
    for (i in entityNames) {
        simpleModel.push('class ' + entityNames[i]);
        simpleModel.push('    /* Add your own attributes */');
        simpleModel.push('    attribute name : String;');
        simpleModel.push('end;');
        simpleModel.push('');
    }
    simpleModel.push('end.');
    return simpleModel;
};

var shellCreateNamespace = function(args, context) {
    var namespace = args.namespace;
    var baseFileName = args.file ? args.file.trim() : null;
    baseFileName = (!baseFileName || args.file == '<namespace>') ? namespace : baseFileName ;
    namespace = sanitizeIdentifier(namespace);
    if (/[0-9]/.test(namespace[0])) {
        throw Error("Invalid namespace name '" + namespace + "' - first character cannot be a digit");
    }
    if (baseFileName.replace(validator, '') !== baseFileName) {
        throw Error("Invalid base file name '" + baseFileName + "' - must have only letters/digits/underscore");
    }
    var simpleModel = [
        '[Application]',
        'package ' + namespace + ';',
        '',
        '    /* Add your own classes here */',
        '',
        'end.',
    ];
    var result = {path: baseFileName + ".tuml", blob: new Blob(insertNewLines(simpleModel))};
    console.log(result);
    return result;
};


var shellAppDeploy = function(args, context) {
    var projectPath = buildProjectPath(args, context);
    return dojo.xhrPost({
         postData: '',
         handleAs: 'json',
         url: "/services/deployer/?path=" + projectPath,
         failOk: true,
         load: function(result) {
             var problems = result.problems;
             for (i in problems) {
                 if (problems[i].severity == 'error') {
                     return "Deploy failed: " + formatProblem(problems[i], projectPath); 
                 } else if (problems[i].severity == 'warning') {
                     continue
                 } else {                            
                     return problems[i].reason
                 }
             }
             return ""
         },
         error: standardApplicationError(projectPath)
    });
};

var shellDbDeploy = function(args, context, message) {
    var projectPath = buildProjectPath(args, context);
    message = (message || "") + "\nDatabase was reset\n"
    var appName = locationToWorkspace(projectPath)
    var url = "/services/api/" + appName + "/data";
    return dojo.xhrPost({
         postData: '',
         handleAs: 'json',
         url: url, 
         failOk: true,
         load: function(result) {
             return message + showLinks(projectPath) 
         },
         error: standardApplicationError(appName)
    });
};

var showLinks = function(projectPath, packages) {
    var applicationName = locationToWorkspace(projectPath);
    packages = packages || [];
    var uiUrl = "" + currentProtocol() + "/kirra-angular/?theme=&app-path=/services/api-v2/" + applicationName + "/";
    var api2Url = "" + currentProtocol() + "/services/api-v2/" + applicationName + "/";
    var appInfo = "\nStart [browser UI](" + uiUrl + ")" +
        "\nBrowse [REST API (v2)](" + api2Url + ")";
    for (var i in packages) {
        appInfo += "\nClass diagrams for package [" + packages[i] + "](" + currentProtocol() + "/services/diagram/" + applicationName + "/package/" + packages[i] + ".uml?showClasses=true&showOperations=true&showAttributes=true&showEnumerations=true&showDataTypes=true&showSignals=true)"; 
        appInfo += "\nStatechart diagrams for package [" + packages[i] + "](" + currentProtocol() + "/services/diagram/" + applicationName + "/package/" + packages[i] + ".uml?showStateMachines=true)";
    }    
    return appInfo     
}

var takeDatabaseSnapshot = function(selectedText, text, selection, resource) {
    var dirPath = resource.substring(0, resource.lastIndexOf('/'))
    var url = "/services/api/" + locationToWorkspace(dirPath) + "/data";
    return dojo.xhrGet({
         handleAs: 'text',
         url: url,
         load: function(result) {
             return { text: result.toString(), selection: null };
         }
    });
};

var headers = { name: "Cloudfier Plugin", version: "0.9", description: "Cloudfier features for Orion." };
var provider = new orion.PluginProvider(headers);

provider.registerServiceProvider("orion.shell.command", {}, 
    {   
        name: "cloudfier",
        description: "Cloudfier commands"
    }
);


provider.registerServiceProvider("orion.shell.command", { callback: shellFullDeploy }, 
    {   
        name: "cloudfier full-deploy",
        description: "Deploys a Cloudfier application and database",
        parameters: [{
            name: "application",
            type: "file",
            description: "Application to deploy"
        }],
        returnType: "string"
    }
);

provider.registerServiceProvider("orion.shell.command", { callback: shellRunTests }, 
    {   
        name: "cloudfier run-tests",
        description: "Runs all tests",
        parameters: [{
            name: "application",
            type: "file",
            description: "Application to run tests for"
        }],
        returnType: "string"
    }
);

provider.registerServiceProvider("orion.shell.command", { callback: shellAppInfo }, {   
	name: "cloudfier info",
	description: "Shows information about a Cloudfier application and database",
	parameters: [{
	    name: "application",
	    type: "file",
	    description: "Application to get obtain information for"
	}],
	returnType: "string"
});

provider.registerServiceProvider("orion.shell.command", { callback: shellAppUndeploy }, 
    {   
        name: "cloudfier undeploy",
        description: "Undeploys the given application",
        parameters: [{
            name: "application",
            type: "file",
            description: "Application to undeploy"
        }],
        returnType: "string"
    }
);

provider.registerServiceProvider("orion.shell.command", { callback: shellGenerate }, 
    {   
        name: "cloudfier generate",
        description: "Generates code for the given application and platform",
        parameters: [{
            name: "platform",
            type: "string",
            description: "Platform to generate for"
        },{
            name: "application",
            type: "file",
            description: "Application to generate"
        }],
        returnType: "[file]"
    }
);

provider.registerServiceProvider("orion.shell.command", { callback: shellSchemaCrawlerImport }, 
    {   
        name: "cloudfier import-schema-crawler",
        description: "Imports a database schema as Cloudfier application",
        parameters: [{
            name: "application",
            type: "file",
            description: "Application to import into"
        },{
            name: "schema-crawler-snapshot",
            type: "file",
            description: "Database snapshot to import (schema snapshot file generated with SchemaCrawler)"
        }],
        returnType: "[file]"
    }
);



provider.registerServiceProvider("orion.shell.command", { callback: shellInitProject }, {   
    name: "cloudfier init-project",
    description: "Initializes the current directory as a Cloudfier project",
    parameters: [{
        name: "applicationName",
        type: {name: "string"},
        description: "Name of the application to be initialized"
    }],
    returnType: "file"
});

provider.registerServiceProvider("orion.shell.command", { callback: shellCreateProject }, {   
    name: "cloudfier create-project",
    description: "Creates a Cloudfier project",
    parameters: [{
        name: "applicationName",
        type: {name: "string"},
        description: "Name of the application to be created"
    }, {
        name: "entities",
        type: {name: "string"},
        description: "Name of the entity classes to be created (comma separated)",
        option: true,
        defaultValue: 'SimpleEntity' 
    }],
    returnType: "[file]"
});

provider.registerServiceProvider("orion.shell.command", { callback: shellDBSnapshot }, {   
    name: "cloudfier db-snapshot",
    description: "Fetches a snapshot of the current application's database and stores it in as a data.json file in the current directory",
    returnType: "file"
});

provider.registerServiceProvider("orion.shell.command", { callback: shellCreateEntity }, {   
    name: "cloudfier add-entity",
    description: "Adds a new entity with the given name to the current directory",
    parameters: [{
        name: "namespace",
        type: {name: "string"},
        description: "Name of the namespace (package) for the entity (class)"
    },
    {
        name: "entity",
        type: {name: "string"},
        description: "Name of the entity (class) to create"
    }],
    returnType: "file"
});

provider.registerServiceProvider("orion.shell.command", { callback: shellCreateNamespace }, {   
    name: "cloudfier add-namespace",
    description: "Adds a new empty source file to the current directory",
    parameters: [{
        name: "namespace",
        type: {name: "string"},
        description: "Name of the namespace (package). Examples: invoicing, payment, elevator etc"
    },
    {
        name: "file",
        type: {name: "string"},
        description: "Base name of the source file to create - does not include a file extension (optional, default: <namespace>)",
        defaultValue: "<namespace>"
    }],
    returnType: "file"
});


provider.registerServiceProvider("orion.shell.command", { callback: shellAppDeploy }, 
    {   
        name: "cloudfier app-deploy",
        description: "Deploys a Cloudfier application",
        parameters: [{
            name: "application",
            type: "file",
            description: "Application to deploy"
        }],
        returnType: "string"
    }
);

provider.registerServiceProvider("orion.shell.command", { callback: shellDbDeploy }, 
    {   
        name: "cloudfier db-deploy",
        description: "Deploys a Cloudfier application's database",
        parameters: [{
            name: "application",
            type: "file",
            description: "Application to reset the database for"
        }],
        returnType: "string"
    }
);

provider.registerServiceProvider("orion.core.contenttype", {}, {
    contentTypes: [{  id: "text/uml",
             name: "TextUML",
             extension: ["tuml"],
             extends: "text/plain"
    }]
});
    
provider.registerServiceProvider("orion.core.contenttype", {}, {
    contentTypes: [{  id: "cloudfier/data-snapshot",
             name: "Cloudfier JSON-encoded dataset",
             filename: ["data.json"],
             extends: "application/json"
    }] 
});

provider.registerService("orion.page.link.category", null, {
        name: "Cloudfier Documentation",
        id: "cloudfier-documentation",
        imageDataURI: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4QEWDDggfDUmIwAAABl0RVh0Q29tbWVudABDcmVhdGVkIHdpdGggR0lNUFeBDhcAAAFMSURBVDjLtdM/S9ZxFAXwTw9CODVlFEQNOVTgEhVOLr0Kp0sUGEJDSTQ5VouBEEFgdRbpFbRKTUKDLT0vIGiU9oS0oRs8/bAQort97z3n/jv3yz/asT8Fquoa5vo5TrJ9pARVtYgNTGMX+5jBHpaSvJ7EjwbkZ9jEY0wlOZnkVONW8aqqXh7aQVfexJUkO1V1CQ8b8zTJx6q6jE+4nWRj2MEGVpt8HGNcxAXsVNXpJGOs4MVvI/TCpvGo/fP4kuRqkvn23YAkaxhV1QJMdXAOu0m+N+gdznbytca8nej2c3PeT46wf4gid3AP15N8nQgdDFUYY6aqhrJ+w/0kHwb+8835maCPZA8PBsATmB10dbc5W0MVlvCkpfplZ3BugjyLdSwfeol9JDex0tseVl7HmySLfzvlW63zqLd90DPDcpLnR/1MC4PPtOV/2A9dBnkGZjrpFAAAAABJRU5ErkJggg==",
        order: 1000
});

provider.registerService("orion.page.link.category", null, {
        name: "Cloudfier Examples",
        id: "cloudfier-examples",
        imageDataURI: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4QIUEQMPEgF2OgAAABl0RVh0Q29tbWVudABDcmVhdGVkIHdpdGggR0lNUFeBDhcAAAODSURBVFjDtZdPiJVVGMZ/9851RscUwaTGRcwiYiAFFWcRFLZz00wURG6CyvLPCyKE44wwYlA5GJOIyDPOOFHQsiKJZtNqKheCRnoLlBbFjEmKrlQGxlFvm/fA4Ztz7/d9mgcu5zvnvP/O+z7nOedCTjOzSmYc+i4zu2Fmt82sO15rpptqtTwBSQ0zWweMAoOSLvpSFVgGLAUqLksku19SPc9+lWLtE2Cr92FnN4CngVXA1Wi3H7vs4SKGa3npl9QA9gLtwIGQFeCu/7JtGFjiOrGNZKu0cBrGVUkPovGLwFtAH9Dl09eAH4CvJP3cQndRMIsA5nV8HhgB9kr6OxgDJoDtOVn9EnhP0n3X6waOA8OS6sFHEgPRwqjvcCxyfi5yfgroBTochJs9OIC3gXNm1ubjE25rNOOjJQiHPKUDPp4ANgF3gM2Sdkg67xiYl/SrpJ3ARpfZCEy67iAwBQxkj2myBNkovea/+LDXHROnMvO9AfjN5bdkMEEqC4sCiEnEzMbNrGFmpwoeWcxszHU+zyOkWgKAa33qpqf4FR+fzO42tQFfGwd2ee0BlpjZk/79L9AINmoZPDwALnlp1noAIaDfWzmPmLACBLZcE5X6sn+vBhZaEdHSJgCtSbpbsAptCax15FFxIIwe/837+K8AwNSFkyhBA3jBp2Yj28HuvSQGPH1ExBOAcxr4AHgH+CmSa1Z/gHe9/877hWD3UY7hLeA5SddzTsBTwJ/ASuAlSWda2a+lmNDM1gMfAUOSzpjZNPAysBv4MMXp0dwedz7tuj3AEeAgUC/KhJ8CrwIWUTPOZjV/IyyqvZl1ROw5EpaBfuCz1AmqNiGiAafPY56ZKeA80AkciuUzoDzk1/ZZST/63FGn9X15IG72BAuM2O/sNmNmnQm9J8zsisv0tbJFCUrNjuvu4I1wS/pNiZm96WsXWtko9SIKRy4C6GlgPfA68HX82ABe8/77QhdPiUdpvItvHM3bzGxbE5VvE+8LCj/JcsrR7gy5kNBt+FtwuaS5onarZTDhd0GnU+qzmV8P0ClpLhfpZUqQuC3PAt3A/cT6P8A6z07j/w4gtHb/Q5IKoKOsscpDBICZPZO91YA2SVfK2qqWcBrOex2Y8at2JvqeNbM/yhLOw5RgeZS9rKMV0VrjcZZgHNiRmZ6U9H5ZW7WSPBDaLg9+u/dfhICKMmBo/wFxUqCKGdyOFgAAAABJRU5ErkJggg==", 
        order: 1001
});

provider.registerServiceProvider("orion.page.link", null, {
    id: "orion.cloudfier.content.examples",
    tooltip: "Check out existing projects from the Cloudfier example repository.",
    name: "Get Cloudfier examples",
    category: "cloudfier-examples",
    uriTemplate: "{+OrionHome}/git/git-repository.html#,cloneGitRepository=https://github.com/abstratt/cloudfier-examples.git"
});

provider.registerServiceProvider("orion.page.link", null, {
    id: "orion.cloudfier.content.documentation",
    tooltip: "Learn how to write Cloudfier programs.",
    name: "Cloudfier documentation",
    category: "cloudfier-documentation",
    uriTemplate: "https://doc.cloudfier.com/"
});

provider.registerServiceProvider("orion.edit.validator", { checkSyntax: checkSyntax }, { contentType: ["text/uml", "cloudfier/data-snapshot"] });

provider.registerServiceProvider("orion.edit.outliner", { computeOutline: computeOutline }, { contentType: ["text/uml"], id: "com.abstratt.textuml.outliner", name: "TextUML outliner" });

provider.registerServiceProvider("orion.edit.command", {
    run : autoFormat
}, {
    name : "Format (^Y)",
    key : [ "y", true ],
    contentType: ["text/uml"]
});

provider.registerServiceProvider("orion.edit.command", {
    run : takeDatabaseSnapshot
}, {
    name : "Load database (^B)",
    key : [ "b", true ],
    contentType: ["cloudfier/data-snapshot"]
});

/* Registers a highlighter service. */    
provider.registerServiceProvider("orion.edit.highlighter",
  {
    // "grammar" provider is purely declarative. No service methods.
  }, {
    type : "grammar",
    contentType: ["text/uml"],
    grammar: {
      patterns: [
          {  
             end: '"',
             begin: '"',
             name: 'string.quoted.double.textuml',
          },
          {  begin: "\\(\\*", 
             end: "\\*\\)",
             name: "comment.model.textuml"
          },
          {  
             begin: "/\\*", 
             end: "\\*/",
             name: "comment.ignored.textuml"
          },
          {  
             name: 'keyword.control.untitled',
             match: '\\b(abstract|access|actor|aggregation|alias|all|allow|and|any|apply|association|as|attribute|begin|broadcast|by|call|catch|class|component|composition|connector|constructor|create|datatype|delete|deny|dependency|derived|destroy|do|else|elseif|end|entry|enumeration|exit|extends|extent|external|false|final|finally|function|id|if|implements|import|in|initial|inout|interface|invariant|is|link|literal|load|model|navigable|new|none|nonunique|not|null|on|operation|opposite|or|ordered|out|package|port|postcondition|precondition|private|primitive|profile|property|protected|provided|public|query|raise|raises|read|readonly|reception|reference|repeat|required|return|role|self|send|signal|specializes|state|statemachine|static|stereotype|subsets|terminate|then|to|transition|true|try|type|unique|unlink|unordered|until|update|var|when|where|while)\\b'
          },
          {
            "match": "([a-zA-Z_][a-zA-Z0-9_]*)",
            "name": "variable.other.textuml"
          },                  
          {
            "match": "<|>|<=|>=|=|==|\\*|/|-|\\+",
            "name": "keyword.other.textuml"
          },
          {
            "match": ";",
            "name": "punctuation.textuml"
          }
        ]
    }
});
  
provider.registerServiceProvider("orion.edit.contentAssist",
    {
        computeProposals: computeProposals
    },
    {
        name: "TextUML content assist",
        contentType: ["text/uml"]
    }
);

provider.connect();

};
