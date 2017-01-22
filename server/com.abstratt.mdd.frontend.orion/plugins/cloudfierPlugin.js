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
            proposal: "package package_name;\n\n/* add classes here */\n\nend.",
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
            proposal: "\tattribute status2 : SM1;\n\toperation action1();\n\toperation action2();\n\toperation action3();\n\tstatemachine SM1\n\t\tinitial state State0\n\t\t\ttransition on call(action1) to State1;\n\t\tend;\n\t\tstate State1\n\t\t\ttransition on call(action1) to State1\n\t\t\ttransition on call(action2) to State2;\n\t\tend;\n\t\tstate State2\n\t\t\ttransition  on call(action1) to State1\n\t\t\ttransition on call(action3) to State3;\n\t\tend;\n\t\tterminate state State3;\n\tend;\n\t\tend;\n",
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

var formatProblem = function(problem, projectPath) {
    return problem.severity.toUpperCase() + ": " + problem.reason + " - [" + problem.file + " : " + problem.line + "](http:/edit/edit.html#" + buildOrionContentLocation(projectPath) + problem.file + ",line=" + problem.line + ")"
};

var formatTestResult = function(testResult, projectPath) {
     var j, location, string = "";
     var passed = testResult.testStatus == 'Pass';
     var symbol = passed ? "\u2714": "\u2718";
     var linkToOperation
     if (testResult.testSourceLocation !== undefined) {     
         linkToOperation = "http:/edit/edit.html#" + buildOrionContentLocation(projectPath) + testResult.testSourceLocation.filename + ",line=" + testResult.testSourceLocation.lineNumber;
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
 	             string += "\t[" + location.frameName + " (" + location.filename + ":" + location.lineNumber + ")](http:/edit/edit.html#" + buildOrionContentLocation(projectPath) + location.filename + ",line=" + location.lineNumber + ")" + "\n"    
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




var shellCreateProject = function(args, context) {
    var propertiesFile = [
        '#This file configures a Cloudfier project',
        'mdd.modelWeaver=kirraWeaver',
        'mdd.enableExtensions=true',
        'mdd.enableLibraries=true',
        'mdd.enableTypes=true',
        'mdd.extendBaseObject=true',
        'mdd.application.allowAnonymous=true',
        'mdd.application.loginRequired=false'
    ];
    var result = {path: "mdd.properties", isDirectory: false, blob: new Blob(insertNewLines(propertiesFile))};
    console.log(result);
    return result;
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
    var validator = /\[^a-bA-B0-9_]/g;
    if (entity.replace(validator, '') !== entity) {
        throw Error("Invalid entity name '" + entity + "' - must have only letters/digits/underscore");
    }
    if (namespace.replace(validator, '') !== namespace) {
        throw Error("Invalid namespace name '" + namespace + "' - must have only letters/digits/underscore");
    }
    if (/[0-9]/.test(entity[0])) {
        throw Error("Invalid entity name '" + entity + "' - first character cannot be a digit");
    }
    if (/[0-9]/.test(namespace[0])) {
        throw Error("Invalid namespace name '" + namespace + "' - first character cannot be a digit");
    }
    var simpleModel = [
        'package ' + namespace + ';',
        'class ' + entity,
        '    /* Add your own attributes */',
        '    attribute name : String;',
        'end;',
        'end.',
    ];
    var result = {path: entity + ".tuml", blob: new Blob(insertNewLines(simpleModel))};
    console.log(result);
    return result;
};

var shellCreateNamespace = function(args, context) {
    var namespace = args.namespace;
    var baseFileName = args.file ? args.file.trim() : null;
    baseFileName = (!baseFileName || args.file == '<namespace>') ? namespace : baseFileName ;
    var validator = /\[^a-bA-B0-9_]/g;
    if (namespace.replace(validator, '') !== namespace) {
        throw Error("Invalid namespace name '" + namespace + "' - must have only letters/digits/underscore");
    }
    if (/[0-9]/.test(namespace[0])) {
        throw Error("Invalid namespace name '" + namespace + "' - first character cannot be a digit");
    }
    if (baseFileName.replace(validator, '') !== baseFileName) {
        throw Error("Invalid base file name '" + baseFileName + "' - must have only letters/digits/underscore");
    }
    var simpleModel = [
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
    var mobileUiUrl = "http:/kirra-api/kirra_qooxdoo/build/?app-path=/services/api-v2/" + applicationName + "/";
    var newUiUrl = "http:/kirra-api/kirra-ng/?theme=&app-path=/services/api-v2/" + applicationName + "/";
    var api2Url = "http:/services/api-v2/" + applicationName + "/";
    var appInfo = "\nStart [desktop browser UI](" + newUiUrl + ")" +
        "\nStart [mobile browser UI (work in progress)](" + mobileUiUrl + ")" +        
        "\nBrowse [REST API (v2)](" + api2Url + ")";
    for (var i in packages) {
        appInfo += "\nClass diagrams for package [" + packages[i] + "](http:/services/diagram/" + applicationName + "/package/" + packages[i] + ".uml?showClasses=true&showOperations=true&showAttributes=true&showEnumerations=true&showDataTypes=true&showSignals=true)"; 
        appInfo += "\nStatechart diagrams for package [" + packages[i] + "](http:/services/diagram/" + applicationName + "/package/" + packages[i] + ".uml?showStateMachines=true)";
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



provider.registerServiceProvider("orion.shell.command", { callback: shellCreateProject }, {   
    name: "cloudfier init-project",
    description: "Initializes the current directory as a Cloudfier project",
    returnType: "file"
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
        imageDataURI: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4QEWDDAvJFOxugAAABl0RVh0Q29tbWVudABDcmVhdGVkIHdpdGggR0lNUFeBDhcAAAS+SURBVFjDrZc/aFRPEMc/++edh0TQO0lCMCiHxiaNfyoJmGuEgJDSIhCsgggpA4KtqSzVFBZpBEEQEWJnEQQRhKQICiZBRBFt9CQEhTvu7a5FnOW9vLt4P/Ob5r23u2925jvfmdlVzrnAH/HeA6C1Jjumtc7NpWmKMYa7d+9y6NAhpqen4xqlFEopQgi5p3MOYwxZ8d6jQwg55bK5jCmlaDabuXFrLUoptra2aDQaaK1xzuUM//HjR9wcyG0ue2qt0UoptNY0m83cpMitW7e4ffs2aZrm0JE17XY755EYvLCwwOLiIkD81zlHmqbInt57tCgqlUrRAEHCe8/w8DBaa54/f14IDUCSJIQQMMZEJO7duwfA6OgoANbaiELWgT+G7MQ0WvTnKWPT09N471lZWaHRaERvRKmIcw6A79+/02q1ADh37lxcn6Zp1ClIhhB2OGCtLUxaa6NB58+fR2vN06dP49qsKKUwxpCmKffv38c5x8zMDNbaqEfesyHRWmOFKNZanHO5ECil8N4zMTHB2toaX79+5cOHD9RqNQBu3ryZy5D19fUYloGBgQLzQwg456IT1lpUu90OomR+fh6tNZOTkzF+gsLm5iZfvnyhXq/n0i27Jk1TlpaWmJycjF7L3PLyMi9fvmR4eJirV6/GceWcC/Lx+PFj3r17F724fv06fX19UbkofPLkCRsbG3jvMcbgnGNsbIzx8fGImhj36dMnHj58GDkyNjZGvV6PKaparVYQxQA/f/5kYWEhptfhw4eZnZ3Fe8+bN2949uxZrm5kU1ZrzbVr16hWqwDcuXOHra0tAPr7+5mamqKvry+iFkLYQSCbFlK1Pn/+zIMHD7hy5QonT57k/fv3PHr0iF5kbm4Oay1LS0u8ffuWmZkZBgYGclkmhFftdjvsLr/CVGNMhHJ+fj7nbScERJIk4caNG7mwZf/J6tfZopMVay0h7LSJV69eddyoU++Q6ri9vZ2rFVL90jTNlXOdZTEQS7LUhTRNWV1d3RPy7P9S8VZXV3MGSonPIuK93zFAPAUol8s5r6y1/Pr1q+BlNzSkKjYajdgNd89nC5/e3ak6MTxJkoKXu7mwW4Q/Wc9Fp1RfAL2XMnk/fvx4wYu/EfHMmTOFFi/fQuyOBnSC9vLly1097URErTXHjh3Lhbab/NUArTWlUomhoaGOnnbiwIULFwrd8p8NkNhPTU0VYtzN4Hq9HlNv3waIEeVymaGhoQi11Pbdcvbs2cIZYd8hEKJJJ9xr7aVLl2K/74bSfzJA0kgpRa1WI0mSrtlijMEYE4vN/0JCOaCIIYODgwXSyfvo6Gh8z56I982BrKdHjx7tOj8yMhLD1Qv8PXMgeyEZHx/vWrRGRkbi/WAvruSaXi+LRKm1loMHD3Lx4kW2t7dj4yqXy1Qqlei5975wbNuXAVk4lVJUKhUOHDiAMYYkSWg2mxw5ciSH1t+aV88GyMnWORd7+uvXr/n27VucL5fLVKtVTp06FVEIIUQC78sA8V42B6jValQqlZiS7XabarUa7weyvpcssL1WQiEhwOnTp/n48WM8cgGcOHGicDjZq4X/UxrK7aa/v59Wq8WLFy9YXl6mVCoxODiY2zB709pLfgObpt4MFVx58gAAAABJRU5ErkJggg==", 
        order: 1000
});

provider.registerService("orion.page.link.category", null, {
        name: "Cloudfier Examples",
        id: "cloudfier-examples",
        imageDataURI: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4QEWDDggfDUmIwAAABl0RVh0Q29tbWVudABDcmVhdGVkIHdpdGggR0lNUFeBDhcAAAFMSURBVDjLtdM/S9ZxFAXwTw9CODVlFEQNOVTgEhVOLr0Kp0sUGEJDSTQ5VouBEEFgdRbpFbRKTUKDLT0vIGiU9oS0oRs8/bAQort97z3n/jv3yz/asT8Fquoa5vo5TrJ9pARVtYgNTGMX+5jBHpaSvJ7EjwbkZ9jEY0wlOZnkVONW8aqqXh7aQVfexJUkO1V1CQ8b8zTJx6q6jE+4nWRj2MEGVpt8HGNcxAXsVNXpJGOs4MVvI/TCpvGo/fP4kuRqkvn23YAkaxhV1QJMdXAOu0m+N+gdznbytca8nej2c3PeT46wf4gid3AP15N8nQgdDFUYY6aqhrJ+w/0kHwb+8835maCPZA8PBsATmB10dbc5W0MVlvCkpfplZ3BugjyLdSwfeol9JDex0tseVl7HmySLfzvlW63zqLd90DPDcpLnR/1MC4PPtOV/2A9dBnkGZjrpFAAAAABJRU5ErkJggg==",
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
    uriTemplate: "http://doc.cloudfier.com/"
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
             match: '\\b(abstract|access|actor|aggregation|alias|all|allow|and|any|apply|association|as|attribute|begin|broadcast|by|call|catch|class|component|composition|connector|create|datatype|delete|deny|dependency|derived|destroy|do|else|elseif|end|entry|enumeration|exit|extends|extent|external|false|final|finally|function|id|if|implements|import|in|initial|inout|interface|invariant|is|link|literal|load|model|navigable|new|none|nonunique|not|null|on|operation|opposite|or|ordered|out|package|port|postcondition|precondition|private|primitive|profile|property|protected|provided|public|query|raise|raises|read|readonly|reception|reference|repeat|required|return|role|self|send|signal|specializes|state|statemachine|static|stereotype|subsets|terminate|then|to|transition|true|try|type|unique|unlink|unordered|until|update|var|when|where|while)\\b'
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
