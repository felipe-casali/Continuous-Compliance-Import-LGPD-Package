/*
 * Copyright (c) 2021 by Delphix. All rights reserved.
 */
package sample.masking.driverSupport;

import com.delphix.masking.api.driverSupport.DriverSupport;
import com.delphix.masking.api.driverSupport.Task;
import java.util.ArrayList;
import java.util.List;
import sample.masking.driverSupport.tasks.DisableConstraints;
import sample.masking.driverSupport.tasks.DisableTriggers;
import sample.masking.driverSupport.tasks.DropIndexes;

/*
 * This class will be used to store and retrieve all instances of Tasks.
 */
public class MSSQLDriverSupport implements DriverSupport {
    /**
     * This method serves as a directory Task objects provided by this plugin.
     *
     * @return an ordered list of tasks. The order that tasks are added to the returning list is the
     *     order that they will be executed in.
     */
    @Override
    public List<Task> getTasks() {
        List<Task> mssqlTasks = new ArrayList<>();
        mssqlTasks.add(new DisableConstraints());
        mssqlTasks.add(new DisableTriggers());
        mssqlTasks.add(new DropIndexes());

        return mssqlTasks;
    }
}
