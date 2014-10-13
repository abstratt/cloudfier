package com.abstratt.mdd.target.mean

import org.eclipse.uml2.uml.Class

class RouteGenerator {
    
    def CharSequence generateRoute(Class clazz) {
        '''
        self.app.get("/entities/«clazz.name»", function(req, res) {
            res.json({
                extentUri: self.resolveUrl('/entities/«clazz.name»/instances')
            });
        });
        self.app.get("/entities/«clazz.name»/instances", function(req, res) {
            return mongoose.model('«clazz.name»').find().then(function(block) {
                res.json({
                    offset: 0,
                    length: block.size(),
                    contents: block
                });
            });
        });
        self.app.get("/entities/«clazz.name»/template", function(req, res) {
            res.json(new «clazz.name»());
        });
        self.app.post("/entities/«clazz.name»/instances", function(req, res) {
            var instanceData = req.body;
            var new«clazz.name» = new «clazz.name»();
            for (var p in instanceData) {
                new«clazz.name»[p] = instanceData[p]; 
            }
            new«clazz.name».save(function(err, created) {
                if (err) {
                    respondWithError(req, err);
                } else {
                    res.json(created);    
                }
            });
        });
        '''
    }
    
}
