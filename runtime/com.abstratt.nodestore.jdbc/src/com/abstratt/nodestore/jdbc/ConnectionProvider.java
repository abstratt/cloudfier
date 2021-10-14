package com.abstratt.nodestore.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.commons.lang.Validate;
import org.postgresql.ds.PGSimpleDataSource;

import com.abstratt.pluginutils.ConfigUtils;

/**
 * Provides connections from a datasource. If a connection is required multiple
 * times in a thread, all requests end up with the same connection (only one
 * connection in use per thread).
 *
 * Connections must be held for as little as possible, and be returned when no
 * longer in use.
 */
public class ConnectionProvider {
    private Connection connection;
    private AtomicInteger level = new AtomicInteger();
    private DataSource dataSource;
    private String databaseName;
    private boolean readOnly;

    public ConnectionProvider() {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        // TODO need a way to pass properties into node store factory
        databaseName = ConfigUtils.get("KIRRA_DATABASE_NAME", "cloudfier");
        pgDataSource.setDatabaseName(databaseName);
        String username = ConfigUtils.get("KIRRA_DATABASE_USERNAME", "cloudfier");
        pgDataSource.setUser(username);
        String password = ConfigUtils.get("KIRRA_DATABASE_PASSWORD", "cloudfier");
		pgDataSource.setPassword(password);
		String host = ConfigUtils.get("KIRRA_DATABASE_HOST", "localhost");
		pgDataSource.setServerName(host);
		int port = Integer.parseInt(ConfigUtils.get("KIRRA_DATABASE_PORT", "5432"));
		pgDataSource.setPortNumber(port);
        this.dataSource = pgDataSource;
    }

    public Connection acquireConnection() throws SQLException {
        // System.out.println("acquireConnection - " + level.get());
        boolean firstRequest = level.getAndIncrement() == 0;
        Validate.isTrue(connection == null == firstRequest, "First? " + firstRequest);
        if (firstRequest) {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connection.setReadOnly(readOnly);
        }
        return connection;
    }

    public void commit() throws SQLException {
        Validate.isTrue(this.connection != null);
        JDBCNodeStore.logSQLStatement(databaseName + " - committing ");
        this.connection.commit();
    }

    public boolean hasConnection() {
        return level.get() > 0;
    }

    public void releaseConnection(boolean success) throws SQLException {
        // System.out.println("releaseConnection - " + level.get());
        if (level.get() == 0) {
            Validate.isTrue(this.connection == null);
            // someone being overly zealous
            return;
        }
        Validate.isTrue(level.get() > 0);
        boolean lastRelease = level.decrementAndGet() == 0;
        if (lastRelease) {
            try {
                if (success) {
                    commit();
                } else {
                    rollback();
                }
            } finally {
                Connection tmpConnection = this.connection;
                this.connection = null;
                tmpConnection.close();
            }
        }
    }

    public void rollback() throws SQLException {
        Validate.isTrue(this.connection != null);
        JDBCNodeStore.logSQLStatement(databaseName + " - rolling back");
        this.connection.rollback();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

}