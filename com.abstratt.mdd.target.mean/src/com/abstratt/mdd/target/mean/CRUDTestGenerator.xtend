package com.abstratt.mdd.target.mean

import com.abstratt.kirra.TypeRef
import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.mdd.core.IRepository
import java.util.List
import org.eclipse.uml2.uml.Class

class CRUDTestGenerator {
    
    IRepository repository
    
    List<Class> entities
    
    String applicationName
    
    new(IRepository repository) {
        this.repository = repository
        val appPackages = KirraHelper.getApplicationPackages(repository.getTopLevelPackages(null))
        this.applicationName = KirraHelper.getApplicationName(repository, appPackages)
        this.entities = KirraHelper.getEntities(appPackages)
    }
    
    def CharSequence generateTests() {
        
        '''
        var Kirra = require("./kirra-client.js");
        var helpers = require('./helpers.js');
        var util = require('util');

        var assert = require("assert");
        var user = process.env.KIRRA_USER || 'test';
        var folder = process.env.KIRRA_FOLDER || 'cloudfier-examples';

        suite('«applicationName» CRUD tests', function() {
            var kirraBaseUrl = process.env.KIRRA_BASE_URL || "http://localhost:48084";
            var kirraApiUrl = process.env.KIRRA_API_URL || (kirraBaseUrl);
            var kirra = new Kirra(kirraApiUrl);
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
                    kirra.getExactEntity('«fullName»').then(function(fetched) {
                        entity = fetched; 
                        assert.equal(fetched.fullName, "«fullName»");
                        assert.ok(fetched.extentUri);
                        assert.ok(fetched.templateUri);
                    }).then(done, done);
                });
                test('GET extent', function(done) {
                    kirra.performRequestOnURL(entity.extentUri, null, 200).then(function(instances) {
                        assert.ok(typeof instances.length === 'number');
                        assert.ok(instances.length >= 0); 
                    }).then(done, done);
                });
                var template;
                test('GET template', function(done) {
                    kirra.performRequestOnURL(entity.templateUri, null, 200).then(function(fetched) {
                        assert.ok(fetched); 
                        template = fetched;
                    }).then(done, done);
                });
                test('POST', function(done) {
                    kirra.performRequestOnURL(entity.extentUri, 'POST', 201, template).then(function(created) {
                        assert.ok(created);
                        assert.ok(created.uri);
                    }).then(done, done);
                });
            });
        '''
    }
    
}
