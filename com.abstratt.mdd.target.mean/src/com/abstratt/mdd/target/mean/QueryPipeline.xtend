package com.abstratt.mdd.target.mean

import java.util.List
import org.eclipse.uml2.uml.Action
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.CallOperationAction
import org.eclipse.uml2.uml.ReadExtentAction

import static extension com.abstratt.mdd.core.util.ActivityUtils.*

/**
 * A query pipeline is a sequence of stages involving generation and consumption of collections of objects.
 * 
 * The first stage is always a collection producing stage, 
 * whereas all stages thereafter are both collection producing and consuming ones
 * (so they can be chained),
 * the last stage optionally not producing a collection (an aggregation stage).
 * 
 * A query pipeline should be built whenever a target object of a call operation action
 * has a multiplicity > 1.
 * 
 * The actual code generation is then based off the query that is built.
 */
class QueryPipeline {
    public final List<QueryStage> stages

    private new(List<QueryStage> stages) {
        this.stages = stages
    }

    def static build(Action first) {
        val stages = newLinkedList()
        addStage(first, stages)
        return new QueryPipeline(stages)
    }

    def static void addStage(Action current, List<QueryStage> stages) {
        val stage = createStage(current)
        stage.action = current
        if (current instanceof CallOperationAction)
            addStage(current.target.sourceAction, stages)
        stages.add(stage)
    }

    def static QueryStage createStage(Action action) {
        if (action instanceof ReadExtentAction)
            return new ExtentStage
        else if (action instanceof CallOperationAction)
            return switch (action.operation.name) {
                case 'select' : new FilterStage(action.arguments.head.sourceAction.resolveBehaviorReference as Activity)
                case 'collect' : new MappingStage(action.arguments.head.sourceAction.resolveBehaviorReference as Activity)
                default: throw new UnsupportedOperationException("Unsupported query action: " + action)
            }
        else
            throw new UnsupportedOperationException("Unsupported query action: " + action)
    }

    def QueryStage getFirst() {
        return stages.head
    }

    def long getStageCount() {
        return stages.size
    }

    def QueryStage previousStage(QueryStage current) {
        val foundAt = stages.indexOf(current)
        return if (foundAt > 0)
            stages.get(foundAt - 1)
        else
            null
    }

    def QueryStage nextStage(QueryStage current) {
        val foundAt = stages.indexOf(current)
        return if (foundAt >= 0 && foundAt < stages.size - 1)
            stages.get(foundAt + 1)
        else
            null
    }

    def getStage(int i) {
        return stages.get(i)
    }

}

/**
 * Each stage is anchored by an action.
 */
class QueryStage {
    public Action action
}

/** 
 * Produces a set of all instances that exist for the target class.
 */
class ExtentStage extends QueryStage {
}

/**
 * Produces a set which is the subset of the input set that satisfies the
 * filter criteria.
 */
class FilterStage extends QueryStage {
    public final Activity condition
    new (Activity condition) {
        this.condition = condition
    }
}

/**
 * Produces a set of objects corresponding to each of the objects in the input set
 * according to the mapping function. 
 */
class MappingStage extends QueryStage {
    public final Activity mapping
    new (Activity mapping) {
        this.mapping = mapping
    }
}

/**
 * Produces a set of groups, where each group has the objects in the input 
 */
class GroupingStage extends QueryStage {
    public final Activity grouping
    new (Activity grouping) {
        this.grouping = grouping
    }
}

