package com.abstratt.mdd.target.jee

import com.abstratt.mdd.core.IRepository

class EntityMapper extends com.abstratt.mdd.target.jse.EntityMapper {
    override EntityGenerator createEntityGenerator(IRepository repository) {
        new EntityGenerator(repository)
    }
}
