var cloudfier = cloudfier || {};

cloudfier.tasker = {
    /** The task currently running (null if no task is currently running). */
    running : null,	
    /** Queue of scheduled tasks pending execution. */
    pending : [],
    /** Runs the next pending task if no task is currently running . Normally not invoked directly by client code. */
    runNext : function () {
        console.log("running next");
        var me = this;
        if (me.running !== null) {
            console.log("Cannot run next, task still in progress: " + me.running.uri);
            return;
        }
        if (me.pending.length == 0) {
            console.log("Cannot run next, nothing left to run");
            return;
        }
        var next = me.pending.shift();
        me.running = next;
        console.log("Running " + me.running.uri);
        me.running.run(function () {
            console.log("Completed " + (me.running && me.running.uri));
            me.running = null;
            me.runNext();    
        }); 
    },        
    /** 
     * Schedules a task. A task is an object with the following slots:
     *     - run: The task behavior. A function that takes a 'next' function as parameter, which must be invoked at completion of the task.  
     *     - uri: Not necessarily a URI, this task id. Two task objects with the same uri ar considered to be the same task. Rescheduling a task
     *                that is currently running does nothing. Rescheduling a task that is currently rescheduled moved it to the end of the queue. 
     *     - context: The task's context, if any. Scheduling a task with a context will unschedule any pending tasks for a different context. If a
     * task with a different context is currently running, it will no longer be tracked. 
     */
    schedule : function (task) {
        console.log("Requested scheduling of task " + task.uri + " for context " + task.context);
        for (var i = this.pending.length - 1; i >= 0; i--) {
            if (this.pending[i].context && task.context && this.pending[i].context !== task.context) {
                // found a pending task with a stale context, get rid of it
                console.log("Removing stale task " + this.pending[i].uri + " for context " + this.pending[i].context);
                this.pending.splice(i, 1);
            } else if (this.pending[i].uri === task.uri) {
                // already scheduled, just move it back to the end of the line
                this.pending.splice(i, 1);
                console.log("Removing previously scheduled " + task.uri);
            }
        }
        if (this.running) {
            if (this.running.context && task.context && this.running.context !== task.context) {
                console.log("Forgetting running task " + this.running.uri + " for context " + this.running.context);
                this.running = null;
            } else if (this.running.uri === task.uri) { 
                console.log("Already running, not rescheduled " + task.uri);
                return;
            }
        }
        this.pending.push(task);
        console.log("Scheduled " + task.uri);
        this.runNext();
    }
};

