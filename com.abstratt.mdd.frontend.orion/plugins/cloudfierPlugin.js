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

var buildProjectPath = function (args, context) {
    var path = args.application.file ? args.application.file.path : args.application.path;
    var current = context.cwd;

    current = current.replace('.', '/');
    path = path.replace('.', '/');
    
    if (current.indexOf('/workspace/') == 0)
        // workaround for Orion bug #420829
        return path;
    // the extra path separator is not always required
    return (current+'/'+path).replace(/\/+/g,'/');
};

var checkSyntax = function(title, contents) {
    return dojo.xhrGet({
         handleAs: 'json',
         url: "/services/builder" + title,
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
         url: "/services/formatter/?fileName=" + resource,
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

var locationToWorkspace = function(location, file) {
    return location.replace('/file/','').replace('/' + file,'').replace(/\/$/g, '').replace(/\//g, '-');
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
        try { 
            var parsedMessage = JSON.parse(error.responseText).message;
        } catch (e) {
            console.log("Error parsing error message: " + e);
            parsedMessage = "no further detail";
        }
        return "Unexpected error: " + parsedMessage + " (" + error.status + ")";
    };
};

var formatProblem = function(problem, projectPath) {
    return problem.severity.toUpperCase() + ": " + problem.reason + " - [" + problem.file + " : " + problem.line + "](http:/edit/edit.html#" + projectPath + problem.file + ",line=" + problem.line + ")"
};

var formatTestResult = function(testResult, projectPath) {
     var j, location, string = "";
     var passed = testResult.testStatus == 'Pass';
     var symbol = passed ? "\u2714": "\u2718";
     string += symbol + " " + testResult.testCaseName + "\n";
     if (!passed) {
         if (testResult.testMessage) {
             string += "\tCause: " + testResult.testMessage + "\n";
         }
         if (testResult.errorLocation) {
	         for (j in testResult.errorLocation) {
	             location = testResult.errorLocation[j];
 	             string += "\t[" + location.frameName + " (" + location.filename + ":" + location.lineNumber + ")](http:/edit/edit.html#" + projectPath + location.filename + ",line=" + location.lineNumber + ")" + "\n"    
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
             return message + showLinks(projectPath);
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
        'mdd.application.allowAnonymous=true'
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

var showLinks = function(projectPath) {
    var applicationName = locationToWorkspace(projectPath);
    var uiUrl = "http:/services/ui/" + applicationName + "/";
    var mobileUiUrl = "http:/services/ui/" + applicationName + "/mobile/source/";
    var apiUrl = "http:/services/api/" + applicationName + "/";
    return "\nStart [desktop UI](" + uiUrl + ")" +
        "\nStart [mobile UI](" + mobileUiUrl + ")" +
        "\nBrowse [REST API](" + apiUrl + ") (make sure to log in via a UI first)"; 
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
    contentTypes: [{  id: "application/vnd-json-data",
             name: "JSON-encoded dataset",
             filename: ["data.json"],
             extends: "application/javascript"
    }] 
});

provider.registerServiceProvider("orion.page.link.related", {}, {
    id: "orion.cloudfier.content.examples",
    tooltip: "Check out existing projects from the Cloudfier example repository.",
    name: "Get Cloudfier examples",
    uriTemplate: "../git/git-repository.html#,cloneGitRepository=https://bitbucket.org/abstratt/cloudfier-examples.git"
});

provider.registerServiceProvider("orion.page.link.related", {}, {
    id: "orion.cloudfier.content.support",
    tooltip: "Report an issue, make a suggestion, or ask for help.",
    name: "Report a problem",
    uriTemplate: "http://cloudfier.uservoice.com/"
});

provider.registerServiceProvider("orion.page.link.related", {}, {
    id: "orion.cloudfier.content.documentation",
    tooltip: "Learn how to write Cloudfier programs.",
    name: "Cloudfier documentation",
    uriTemplate: "http://doc.cloudfier.com/"
});

provider.registerServiceProvider("orion.edit.validator", { checkSyntax: checkSyntax }, { contentType: ["text/uml", "application/vnd-json-data"] });

provider.registerServiceProvider("orion.edit.outliner", { computeOutline: computeOutline }, { contentType: ["text/uml"], id: "com.abstratt.textuml.outliner", name: "TextUML outliner" });

provider.registerServiceProvider("orion.edit.command", {
    run : autoFormat
}, {
    name : "Format (^M)",
    key : [ "m", true ],
    contentType: ["text/uml"]
});

provider.registerServiceProvider("orion.edit.command", {
    run : takeDatabaseSnapshot
}, {
    name : "Load database (^L)",
    key : [ "l", true ],
    contentType: ["application/vnd-json-data"]
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
             match: '\\b(abstract|access|aggregation|alias|and|any|apply|association|as|attribute|begin|broadcast|by|class|component|composition|connector|constant|datatype|dependency|derived|destroy|do|else|elseif|end|entry|enumeration|exit|extends|external|function|id|if|implements|import|interface|in|initial|inout|invariant|is|link|model|navigable|new|nonunique|not|on|operation|or|ordered|out|package|port|postcondition|precondition|private|primitive|profile|property|protected|provided|public|query|raise|raises|readonly|reception|reference|required|return|role|self|send|signal|specializes|state|statemachine|static|stereotype|subsets|terminate|to|transition|type|unique|unlink|unordered|var|when)\\b'
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
