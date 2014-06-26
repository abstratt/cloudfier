package com.abstratt.nodestore.jdbc;

import java.util.List;

public class SQLStatement {
    String string;
    boolean changeExpected;
    List<Object> parameters;

    public SQLStatement(String string, boolean changeExpected) {
        this.string = string;
        this.changeExpected = changeExpected;
    }

    @Override
    public String toString() {
        return string + " - " + parameters;
    }
}