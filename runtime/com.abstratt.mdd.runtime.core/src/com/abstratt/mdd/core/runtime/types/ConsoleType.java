package com.abstratt.mdd.core.runtime.types;

import com.abstratt.mdd.core.runtime.ExecutionContext;

public class ConsoleType extends BuiltInClass {
    public static void writeln(ExecutionContext context, StringType value) {
    	System.out.println(value);
    }

    public static void write(ExecutionContext context, StringType value) {
    	System.out.print(value);
    }
    
    private ConsoleType() {
    }

    @Override
    public String getClassifierName() {
        return "mdd_console::Console";
    }

}