package com.abstratt.mdd.target.mean

import com.abstratt.kirra.TypeRef
import com.abstratt.mdd.core.IRepository
import java.util.Collection
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Package

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class RouteGenerator {
    
    private IRepository repository
    
    private Collection<Package> appPackages
    
    Iterable<Class> entities
    
    new(IRepository repository) {
        this.repository = repository
        this.appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.entities = appPackages.entities.filter[topLevel]
    }
    
    def CharSequence generateRoutes() {
        '''
            var mongoose = require('mongoose');
            var cls = require('continuation-local-storage');
            
            «entities.map[entity | 
                '''var «entity.name» = require('./models/«entity.name».js');'''
            ].join('\n')»
            
            var exports = module.exports = { 
                build: function (app, resolveUrl) {
                                
                    // helps with removing internal metadata            
                    var renderInstance = function (entityName, instance) {
                        instance.objectId = instance._id;
                        delete instance._id;
                        delete instance.__v;
                        instance.uri = resolveUrl('entities/'+ entityName + '/instances/' + instance.objectId);
                        instance.entityUri = resolveUrl('entities/'+ entityName);
                        return instance;
                    };
                    
                    «generateIndex()»
                    «entities.map[generateRoute].join("\n\n")»
                }
            };
        '''
    }
    
    def CharSequence generateRoute(Class entity) {
        val fullName = TypeRef.sanitize(entity.qualifiedName)
        '''
        // routes for «fullName»
        app.get("/entities/«fullName»", function(req, res) {
            res.json(«generateEntityLink(entity)»);
        });
        app.get("/entities/«fullName»/instances/:objectId", function(req, res) {
            return mongoose.model('«entity.name»').where({ _id: req.params.objectId}).findOne().lean().exec(function(error, found) {
                if (error) {
                    console.log(error);
                    res.status(400).json({ message: error.message });
                } else {
                    res.json(renderInstance('«fullName»', found));
                }
            });
        });
        app.get("/entities/«fullName»/instances", function(req, res) {
            return mongoose.model('«entity.name»').find().lean().exec(function(error, documents) {
                var contents = [];
                if (error) {
                    console.log(error);
                    res.status(400).json({ message: error.message });
                } else {
                    documents.forEach(function(each) {
                        contents.push(renderInstance('«fullName»', each));
                    });
                    res.json({
                        uri: resolveUrl('entities/«fullName»/instances'),
                        length: contents.length,
                        contents: contents
                    });
                }
            });
        });
        «IF entity.instantiable»
        app.get("/entities/«fullName»/template", function(req, res) {
            var template = new «entity.name»().toObject();
            «entity.attributes.filter[!it.derived && it.defaultValue != null].map[
                '''template.«it.name» = «new ModelGenerator().generateValue(it.defaultValue)»;'''
            ].join('\n')»
            res.json(renderInstance('«fullName»', template));
        });
        «ENDIF»
        «IF entity.instantiable»
        app.post("/entities/«fullName»/instances", function(req, res) {
            var instanceData = req.body;
            var new«entity.name» = new «entity.name»();
            «entity.properties.filter[!it.derived].map[
                '''new«entity.name».«it.name» = instanceData.«it.name»;'''
            ].join('\n')»
            «entity.entityRelationships.filter[!it.derived && !it.multivalued].map[
                '''new«entity.name».«it.name» = instanceData.«it.name» && instanceData.«it.name».objectId;'''
            ].join('\n')»
            new«entity.name».save(function(err, doc) {
                if (err) {
                    console.log(err);
                    res.status(400).json({message: err.message});
                } else {
                    var created = doc.toObject();
                    created.objectId = created._id;
                    delete created._id;
                    delete created.__v;
                    created.uri = resolveUrl('entities/«fullName»/instances/' + created.objectId);
                    res.status(201).json(created);    
                }
            });
        });
        «ENDIF»
        app.put("/entities/«fullName»/instances/:objectId", function(req, res) {
            var instanceData = req.body;
            return mongoose.model('«entity.name»').findByIdAndUpdate(req.params.objectId, instanceData).lean().exec(function(error, found) {
                if (error) {
                    console.log(error);
                    res.status(400).json({ message: error.message });
                } else {
                    res.json(renderInstance('«fullName»', found));
                }
            });
        });
        
        '''
    }
    
    def generateIndex() {
        '''
        app.get("/", function(req, res) {
            cls.getNamespace('session').run(function(context) {
                res.json({
                    applicationName : "«repository.getApplicationName(appPackages)»",
                    entities : resolveUrl("entities"),
                    currentUser : context.username 
                });
            });
        });
        
        app.get("/entities", function(req, res) {
            res.json([
                «entities.map[generateEntityLink].join(',\n ')»
            ]);
        });'''
    }
    
    def generateEntityLink(Class entity) {
        val fullName = TypeRef.sanitize(entity.qualifiedName)
        val link = newLinkedHashMap(
            'fullName' -> '''"«fullName»"''',
            'label' -> '''"«entity.label»"''',
            'description' -> '''"«entity.description.replaceAll('\\n', ' ')»"''', 
            'uri' -> '''resolveUrl("entities/«fullName»")''',
            'extentUri' -> '''resolveUrl("entities/«fullName»/instances")''',
            'user' -> entity.user.toString,
            'concrete' -> entity.concrete.toString,
            'standalone' -> entity.standalone.toString
        )
        if (entity.instantiable) {
            link.put('templateUri', '''resolveUrl("entities/«fullName»/template")''')
        }
        new ModelGenerator().generatePrimitiveValue(link)
    }
}
