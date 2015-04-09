package com.abstratt.kirra.populator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.abstratt.kirra.Entity;
import com.abstratt.kirra.Relationship;
import com.abstratt.kirra.TypeRef;
import com.abstratt.pluginutils.NodeSorter;

public class EntitySorter {
    public static void sort(List<Entity> toSort) {
        Collections.sort(toSort);
        final Map<TypeRef, Entity> typeRefToEntity = new HashMap<TypeRef, Entity>();
        List<TypeRef> sortedRefs = new ArrayList<TypeRef>();
        for (Entity entity : toSort) {
            sortedRefs.add(entity.getTypeRef());
            typeRefToEntity.put(entity.getTypeRef(), entity);
        }
        NodeSorter.NodeHandler<TypeRef> walker = new NodeSorter.NodeHandler<TypeRef>() {
            @Override
            public Collection<TypeRef> next(TypeRef vertex) {
                Collection<TypeRef> result = new HashSet<TypeRef>();
                Entity entity = typeRefToEntity.get(vertex);
                for (Relationship rel : entity.getRelationships())
                    if (!rel.isDerived() && rel.isPrimary())
                        result.add(rel.getTypeRef());
                return result;
            }
        };
        try {
            sortedRefs = NodeSorter.sort(sortedRefs, walker);
        } catch (IllegalArgumentException e) {
            // too bad
        }
        toSort.clear();
        for (TypeRef typeRef : sortedRefs)
            toSort.add(0, typeRefToEntity.get(typeRef));
    }
}
