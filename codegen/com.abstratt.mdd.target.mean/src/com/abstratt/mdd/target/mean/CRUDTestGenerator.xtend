package com.abstratt.mdd.target.mean

import com.abstratt.kirra.TypeRef
import com.abstratt.mdd.core.IRepository
import org.eclipse.uml2.uml.Class

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*

class CRUDTestGenerator {
    
    IRepository repository
    
    Iterable<Class> entities
    
    String applicationName
    
    new(IRepository repository) {
        this.repository = repository
        val appPackages = repository.getTopLevelPackages(null).applicationPackages
        this.applicationName = repository.getApplicationName(appPackages)
        this.entities = appPackages.entities.filter[topLevel].toList.topologicalSort
    }
    
    def CharSequence generateTests() {
        
        '''
        require('../server.js');
        var HttpClient = require("../http-client.js");
        var helpers = require('../helpers.js');
        var util = require('util');
        var q = require('q');

        var assert = require("assert");
        var folder = process.env.KIRRA_FOLDER || 'cloudfier-examples';

        var kirraBaseUrl = process.env.KIRRA_BASE_URL || "http://localhost:48084";
        var kirraApiUrl = process.env.KIRRA_API_URL || (kirraBaseUrl);
        var httpClient = new HttpClient(kirraApiUrl);
        
        var getExactEntity = function (entityName) {
            var entityUri = kirraApiUrl + '/entities/' + entityName;
            return httpClient.performRequestOnURL(entityUri, null, 200);
        };

        var createInstance = function(entityName, values) {
            var entity;
            return getExactEntity(entityName).then(function(fetchedEntity) {
                entity = fetchedEntity;
                return httpClient.performRequestOnURL(entity.templateUri, null, 200);
            }).then(function(template) {
                var toCreate = helpers.merge(template, values);
                return httpClient.performRequestOnURL(entity.extentUri, 'POST', 201, toCreate);
            });
        };
        
        «entities.map[ entity |
            val fullName = TypeRef.sanitize(entity.qualifiedName)
            val requiredRelationships = entity.entityRelationships.filter[ r |
                    r.required && r.defaultValue == null
                ];
            '''
            var create«entity.name» = function (values) {
                var toCreate = {};
                values = values || {};
                // set required properties
                «entity.properties.filter[ p | 
                    p.required && p.defaultValue == null
                ].map[ required |
                    '''toCreate.«required.name» = values.«required.name» || «new JSGenerator().generateDefaultValue(required.type) ?: '''"«required.name»-value"'''»;'''
                ].join('\n')»
                «IF requiredRelationships.empty»
                return createInstance('«fullName»', toCreate);
                «ELSE»
                // set required relationships (via callbacks)
                var promise = q();
                «requiredRelationships.map[ required |
                    '''
                    promise = promise.then(function() {
                        return create«required.type.name»().then(function(requiredInstance) {
                            toCreate.«required.name» = { objectId: requiredInstance.objectId };
                        });
                    });'''
                ].join('\n')»
                promise = promise.then(function() {
                    return createInstance('«fullName»', toCreate);
                });
                return promise;
                «ENDIF»
            };
            '''
         ].join('\n')»

        suite('«applicationName» CRUD tests', function() {
            this.timeout(10000);

            var checkStatus = function(m, expected) {
                assert.equal(m.status, expected, JSON.stringify(m.error || ''));
            };
        
        
            «entities.map[generateEntityCRUDTests(it)].join('\n\n')»
            
        });    
        '''        
    }
    
    def generateEntityCRUDTests(Class entity) {
        val fullName = TypeRef.sanitize(entity.qualifiedName)
        '''
            suite('«entity.name»', function() {
                var entity;
                test('GET entity', function(done) {
                    getExactEntity('«fullName»').then(function(fetched) {
                        entity = fetched; 
                        assert.equal(fetched.fullName, "«fullName»");
                        assert.ok(fetched.extentUri);
                        «IF entity.instantiable»
                        assert.ok(fetched.templateUri);
                        «ELSE»
                        assert.ok(fetched.templateUri === undefined);
                        «ENDIF»
                    }).then(done, done);
                });
                test('GET extent', function(done) {
                    httpClient.performRequestOnURL(entity.extentUri, null, 200).then(function(instances) {
                        assert.ok(typeof instances.length === 'number');
                        assert.ok(instances.length >= 0); 
                    }).then(done, done);
                });
                «IF entity.instantiable»
                test('POST', function(done) {
                    create«entity.name»().then(function(created) {
                        assert.ok(created);
                        assert.ok(created.uri);
                    }).then(done, done);
                });
                test('GET one', function(done) {
                    var created;
                    create«entity.name»().then(function(result) {
                        created = result;
                        return httpClient.performRequestOnURL(created.uri, null, 200);
                    }).then(function(retrieved) {
                        assert.ok(retrieved);
                        assert.ok(retrieved.uri);
                    }).then(done, done);
                });
                
                test('PUT', function(done) {
                    var created;
                    create«entity.name»().then(function(result) {
                        created = result;
                        return httpClient.performRequestOnURL(created.uri, 'PUT', 200, created);
                    }).then(function(updated) {
                        assert.ok(updated);
                        assert.ok(updated.uri);
                    }).then(done, done);
                });
                «ENDIF»
                
            });
        '''
    }
}
