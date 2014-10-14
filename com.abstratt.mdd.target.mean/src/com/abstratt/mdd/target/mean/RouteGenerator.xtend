package com.abstratt.mdd.target.mean

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class
import java.util.Collection
import org.eclipse.uml2.uml.Package
import java.util.List

class RouteGenerator {
    
    private IRepository repository
    
    private Collection<Package> appPackages
    
    List<Class> entities
    
    new(IRepository repository) {
        this.repository = repository
        this.appPackages = KirraHelper.getApplicationPackages(repository.getTopLevelPackages(null))
        this.entities = KirraHelper.getEntities(appPackages)
    }
    
    def CharSequence generateRoutes() {
        '''
            var mongoose = require('mongoose');
            
            «entities.map[entity | 
                '''var «entity.name» = require('./models/«entity.name».js');'''
            ].join('\n')»
            
            var exports = module.exports = { 
                build: function (app, resolveUrl) {
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
        app.get("/entities/«fullName»/instances", function(req, res) {
            return mongoose.model('«entity.name»').find().exec(function(error, contents) {
                if (error) {
                    console.log(error);
                    res.status(400).json({ message: error.message });
                } else {
                    res.json({
                        uri: resolveUrl('entities/«fullName»/instances'),
                        length: contents.length,
                        contents: contents
                    });
                }
            });
        });
        app.get("/entities/«fullName»/template", function(req, res) {
            res.json(new «entity.name»());
        });
        app.post("/entities/«fullName»/instances", function(req, res) {
            var instanceData = req.body;
            var new«entity.name» = new «entity.name»();
            for (var p in instanceData) {
                new«entity.name»[p] = instanceData[p]; 
            }
            new«entity.name».save(function(err, doc) {
                if (err) {
                    console.log(err);
                    res.status(400).json({message: err.message});
                } else {
                    var created = doc.toObject();
                    created.uri = resolveUrl('entities/«fullName»/instances/' + created._id);
                    delete created._id;
                    delete created.__v;
                    console.log(created);
                    res.status(201).json(created);    
                }
            });
        });
        '''
    }
    
    def generateIndex() {
        '''
        app.get("/", function(req, res) {
            res.json({
                applicationName : "«KirraHelper.getApplicationName(repository, appPackages)»",
                entities : resolveUrl("entities")
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
        '''
        {
           fullName : "«fullName»",
           extentUri : resolveUrl("entities/«fullName»/instances"),
           templateUri : resolveUrl("entities/«fullName»/template"),
           uri : resolveUrl("entities/«fullName»")
        }'''        
    }
}
