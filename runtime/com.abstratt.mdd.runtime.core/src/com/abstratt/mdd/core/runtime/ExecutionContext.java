package com.abstratt.mdd.core.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.StructuredActivityNode;
import org.eclipse.uml2.uml.Variable;

import com.abstratt.mdd.core.runtime.types.BasicType;
import com.abstratt.mdd.core.util.ActivityUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.nodestore.INodeKey;
import com.abstratt.nodestore.NodeReference;

/**
 * An execution context is a thread.
 */
public class ExecutionContext {
    /**
     * For a frame, what line it was executing when the next frame was created.
     */
    public static class CallSite {
        private String frameName;
        private String sourceFile;
        private Integer lineNumber;

        public CallSite(String frameName, String sourceFile, Integer lineNumber) {
            this.frameName = frameName;
            this.sourceFile = sourceFile;
            this.lineNumber = lineNumber;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CallSite other = (CallSite) obj;
            if (lineNumber == null) {
                if (other.lineNumber != null)
                    return false;
            } else if (!lineNumber.equals(other.lineNumber))
                return false;
            if (sourceFile == null) {
                if (other.sourceFile != null)
                    return false;
            } else if (!sourceFile.equals(other.sourceFile))
                return false;
            return true;
        }

        public String getFrameName() {
            return frameName;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        public String getSourceFile() {
            return sourceFile;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (lineNumber == null ? 0 : lineNumber.hashCode());
            result = prime * result + (sourceFile == null ? 0 : sourceFile.hashCode());
            return result;
        }
    }

    public class Frame {
        private Scope currentScope;

        private String name;

        private Frame previous;

        private RuntimeObject self;

        private Activity activity;

        private Action lastCallSite;

        public Frame(Activity activity, RuntimeObject target, String name, Frame previous) {
            this.self = target;
            this.name = name;
            this.previous = previous;
            this.activity = activity;
            newScope(ActivityUtils.getBodyNode(activity));
        }

        public Scope currentScope() {
            return currentScope;
        }

        public void dropScope() {
            currentScope = currentScope.ancestor();
        }

        public Activity getActivity() {
            return activity;
        }

        public CallSite getCallSite() {
            if (lastCallSite == null)
                return null;
            String source = MDDExtensionUtils.getSource(lastCallSite);
            Integer lineNumber = MDDExtensionUtils.getLineNumber(lastCallSite);
            return new CallSite(name, source, lineNumber);
        }

        public String getName() {
            return name;
        }

        public RuntimeObject getSelf() {
            return self;
        }

        public RuntimeVariable getVariable(Variable variable) {
            return currentScope.getVariable(variable);
        }

        public Collection<RuntimeVariable> getVariables() {
            return currentScope.getVariables();
        }

        public void newScope(StructuredActivityNode block) {
            newScope(block, currentScope);
        }

        public void recordCallSite(Action action) {
            this.lastCallSite = action;
        }

        @Override
        public String toString() {
            return getName();
        }

        private Scope findScope(StructuredActivityNode block) {
            Scope found = null;
            if (currentScope != null)
                found = currentScope.findScope(block);
            if (found == null && previous != null)
                found = previous.findScope(block);
            return found;
        }

        private void newScope(StructuredActivityNode block, Scope parentScope) {
            // for closures, the parent scope will be in a different frame
            // NB: we usually don't call ActivityNode#getActivity directly as it only works
            // for top-level nodes, but here is exactly what we want
            Activity parentActivity = ActivityUtils.getParentAsActivity(block);
            if (parentActivity != null && MDDExtensionUtils.isClosure(parentActivity)) {
                assert parentScope == null;
                parentScope = findScope(MDDExtensionUtils.getClosureContext(parentActivity));
            }
            currentScope = new Scope(block, parentScope);
        }
    }

    public class Scope {
        private Scope ancestor;
        // Variable -> RuntimeVariable
        private Map<Variable, RuntimeVariable> variables = new HashMap<Variable, RuntimeVariable>();
        private StructuredActivityNode block;

        public Scope(StructuredActivityNode block, Scope ancestor) {
            this.ancestor = ancestor;
            this.block = block;
        }

        public Scope ancestor() {
            return ancestor;
        }

        public void declareVariable(Variable variable) {
            variables.put(variable, new RuntimeVariable(variable));
        }

        public Scope findScope(StructuredActivityNode block) {
            if (this.block == block)
                return this;
            return ancestor == null ? null : ancestor.findScope(block);
        }

        public RuntimeVariable getVariable(Variable variable) {
            RuntimeVariable var = variables.get(variable);
            if (var != null)
                return var;
            if (ancestor != null)
                var = ancestor.getVariable(variable);
            if (var != null)
                return var;
            // fallback to outer scope
            return var;
        }

        public String[] getVariableNames() {
            return variables.keySet().toArray(new String[variables.keySet().size()]);
        }

        public Collection<RuntimeVariable> getVariables() {
            return variables.values();
        }

        public BasicType getVariableValue(Variable variable) {
            final RuntimeVariable runtimeVariable = getVariable(variable);
            if (runtimeVariable == null)
                throw new IllegalArgumentException("Unknown variable " + variable.getName());
            return runtimeVariable.getValue();
        }

        public void setVariableValue(Variable variable, BasicType value) {
            final RuntimeVariable runtimeVariable = getVariable(variable);
            if (runtimeVariable == null)
                throw new IllegalArgumentException("Unknown variable '" + variable.getName() + "'");
            runtimeVariable.setValue(value);
        }
    }

    private static int globalId;

    private List<Frame> frames = new ArrayList<Frame>();

    private int id;

    private Runtime runtime;

    /**
     * The set of objects modified during this context and that need invariants
     * checked.
     */
    private Map<NodeReference, RuntimeObject> workingSet = new LinkedHashMap<>();

    private List<RuntimeEvent> events = new ArrayList<RuntimeEvent>();

    private boolean dirty;

    private boolean saveChanges = true;

    private int level = 0;

    private Boolean readOnly;

    public ExecutionContext(Runtime runtime) {
        this.runtime = runtime;
        this.id = ExecutionContext.globalId++;
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }

    public void addToWorkingSet(RuntimeObject runtimeObject) {
        Assert.isLegal(runtimeObject.nodeReference() != null);
        if (this.workingSet != null) {
            RuntimeObject existing = installIntoWorkingSet(runtimeObject);
            Assert.isLegal(existing == null || existing == runtimeObject);
        }
    }

    public void checkConstraints(Collection<RuntimeObject> objectsToCheck) {
        for (RuntimeObject toCheck : new ArrayList<RuntimeObject>(objectsToCheck))
            toCheck.ensureValid();
    }

    public void clearWorkingSet() {
        workingSet = new LinkedHashMap<>();
    }

    public void commitWorkingSet() {
        if (saveChanges)
            commitWorkingSet(this.workingSet.values());
    }

    public void commitWorkingSet(Collection<RuntimeObject> objectsToCommit) {
        for (RuntimeObject toCommit : objectsToCommit)
            toCommit.save();
    }

    public String computeStackTrace() {
        StringBuffer result = new StringBuffer();
        for (Frame currentFrame : this.frames) {
            result.append('\t');
            result.append(currentFrame);
            result.append('\n');
        }
        return result.toString();
    }

    public Frame currentFrame() {
        return frames.get(0);
    }

    public void declareVariable(Variable variable) {
        currentFrame().currentScope().declareVariable(variable);
    }

    public void dropFrame() {
        frames.remove(0);
    }

    public void dropScope() {
        currentFrame().dropScope();
    }

    public boolean enter(boolean readOnly) {
        boolean newTransaction = level++ == 0;
        if (newTransaction) {
            Assert.isTrue(this.events.isEmpty(), "Event backlog is not empty");
            Assert.isTrue(this.workingSet.isEmpty(), "Working set is not empty");
            this.readOnly = readOnly;
        } else {
            Assert.isTrue(readOnly || !this.readOnly, "Attempt to change the context from R/O to R/W");
        }
        System.out.println("entered context level: " + level);
        if (newTransaction)
            // even for read-only contexts we use transactions, for consistent reads
            runtime.getNodeStoreCatalog().beginTransaction();
        return newTransaction;
    }

    public List<CallSite> getCallSites() {
        Collection<CallSite> result = new LinkedHashSet<CallSite>();
        for (Frame frame : this.frames)
            if (frame.getCallSite() != null)
                result.add(frame.getCallSite());
        return new ArrayList<CallSite>(result);
    }

    public Frame[] getFrames() {
        return frames.toArray(new Frame[frames.size()]);
    }

    public String getName() {
        return "context " + id;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    /**
     * @return
     */
    public Object getSelf() {
        return currentFrame().getSelf();
    }

    /**
     * @param attribute
     * @return
     */
    public BasicType getVariableValue(Variable variable) {
        return currentFrame().currentScope().getVariableValue(variable);
    }

    public RuntimeObject getWorkingObject(NodeReference key) {
        return workingSet.get(key);
    }

    public Collection<RuntimeObject> getWorkingObjects(RuntimeClass runtimeClass) {
        Collection<RuntimeObject> result = new LinkedHashSet<RuntimeObject>();
        for (RuntimeObject current : workingSet.values())
            if (current.isActive() && current.getMetaClass().equals(runtimeClass))
                result.add(current);
        return result;
    }

    public Collection<RuntimeObject> getWorkingSet() {
        return new HashSet<RuntimeObject>(workingSet.values());
    }

    public boolean hasFrames() {
        return !frames.isEmpty();
    }

    public void leave(boolean operationSucceeded) {
        System.out.println("left context level: " + level);
        boolean success = false;
        try {
            if (operationSucceeded) {
                if (!readOnly)
                    saveContext(false);
                success = true;
            }
        } finally {
            runtime.getNodeStoreCatalog().clearCaches();
            level--;
            if (level == 0) {
                boolean rollback = !success || readOnly;
                // only set while a context is active
                readOnly = null;
                clearWorkingSet();
                clearEventQueue();
                if (!rollback)
                    runtime.getNodeStoreCatalog().commitTransaction();
                else
                    runtime.getNodeStoreCatalog().abortTransaction();
            }
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Creates a new frame under this execution context.
     * 
     * @param activity
     *            the activity this frame corresponds to (1-1)
     * @param target
     *            the current target object (a.k.a. 'self')
     * @param name
     *            the name of this frame, for presentation purposes
     * @param annotations
     *            any runtime annotations in effect
     */
    public void newFrame(Activity activity, RuntimeObject target, String name) {
        Frame previous = null;
        if (!frames.isEmpty())
            previous = frames.get(0);
        frames.add(0, new Frame(activity, target, name, previous));
    }

    public void newScope(StructuredActivityNode block) {
        currentFrame().newScope(block);
    }

    public void processPendingEvents() {
        while (!this.events.isEmpty()) {
            List<RuntimeEvent> eventsToProcess = this.events;
            clearEventQueue();
            processEvents(eventsToProcess);
        }
    }

    public void publishEvent(RuntimeEvent event) {
        // event triggering is disabled while events are being handled
        if (this.events != null)
            this.events.add(event);
    }

    public void removeFromWorkingSet(RuntimeObject runtimeObject) {
        if (this.workingSet != null)
            this.workingSet.remove(runtimeObject.nodeReference());
    }

    public void runWithAdvices(RuntimeAction action, RuntimeRunnable actionBehavior) {
        actionBehavior.run();
    }

    /**
     * Commits all objects in the current working set.
     * 
     * @param preserve whether objects currently in the context should be in the new working set
     */
    public void saveContext(boolean preserve) {
        boolean originalSaveChanges = saveChanges;
        Collection<RuntimeObject> objectsToCheck = this.workingSet.values();
        try {
            processPendingEvents();
            // so we don't try to add objects to the working set as a result of
            // checking constraints
            this.saveChanges = false;
            // in case we erroneously end up generating events during constraint
            // validation
            this.events = null;
            if (dirty) {
                // only validate/flush if objects were actually changed
                checkConstraints(objectsToCheck);
                commitWorkingSet(objectsToCheck);
                runtime.getNodeStoreCatalog().validateConstraints();
            }
        } finally {
            clearEventQueue();
            clearWorkingSet();
            this.saveChanges = originalSaveChanges;
            if (preserve)
                for (RuntimeObject original : objectsToCheck)
                    Assert.isTrue(null == installIntoWorkingSet(original));
        }
    }

    public void setVariableValue(Variable variable, BasicType value) {
        currentFrame().currentScope().setVariableValue(variable, value);
    }

    @Override
    public String toString() {
        return getName();
    }

    private List<RuntimeEvent> clearEventQueue() {
        return this.events = new ArrayList<RuntimeEvent>();
    }

    private RuntimeObject installIntoWorkingSet(RuntimeObject runtimeObject) {
        return this.workingSet.put(runtimeObject.nodeReference(), runtimeObject);
    }

    private void processEvents(List<RuntimeEvent> eventsToProcess) {
        for (RuntimeEvent runtimeEvent : eventsToProcess)
            runtimeEvent.getTarget().getMetaClass().handleEvent(runtimeEvent);
    }
}