/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.lib.jdbc;

import java.lang.reflect.Constructor;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;

import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.ConcreteClassGenerator;


/**
 * Wrapper around an existing connection. Subclasses can override the
 * methods whose behavior they mean to change. The <code>equals</code> and
 * <code>hashCode</code> methods pass through to the base underlying data
 * store connection.
 *
 * @author Abe White
 */
public abstract class DelegatingConnection implements Connection, Closeable {

    static final Constructor<DelegatingConnection> concreteImpl;
    static {
        try {
            concreteImpl = ConcreteClassGenerator.getConcreteConstructor(DelegatingConnection.class, Connection.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Connection _conn;
    private final DelegatingConnection _del;

    public DelegatingConnection(Connection conn) {
        _conn = conn;
        if (conn instanceof DelegatingConnection)
            _del = (DelegatingConnection) _conn;
        else
            _del = null;
    }

    /** 
     *  Constructor for the concrete implementation of this abstract class.
     */
    public static DelegatingConnection newInstance(Connection conn) {
        return ConcreteClassGenerator.newInstance(concreteImpl, conn);
    }

    /** 
     *  Marker to enforce that subclasses of this class are abstract.
     */
    protected abstract void enforceAbstract();

    /**
     * Return the wrapped connection.
     */
    public Connection getDelegate() {
        return _conn;
    }

    /**
     * Return the base underlying data store connection.
     */
    public Connection getInnermostDelegate() {
        return (_del == null) ? _conn : _del.getInnermostDelegate();
    }

    public int hashCode() {
        return getInnermostDelegate().hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other instanceof DelegatingConnection)
            other = ((DelegatingConnection) other).getInnermostDelegate();
        return getInnermostDelegate().equals(other);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("conn ").append(hashCode());
        appendInfo(buf);
        return buf.toString();
    }

    protected void appendInfo(StringBuffer buf) {
        if (_del != null)
            _del.appendInfo(buf);
    }

    public Statement createStatement() throws SQLException {
        return createStatement(true);
    }

    /**
     * Create a statement, with the option of not wrapping it in a
     * {@link DelegatingStatement}, which is the default.
     */
    protected Statement createStatement(boolean wrap) throws SQLException {
        Statement stmnt;
        if (_del != null)
            stmnt = _del.createStatement(false);
        else
            stmnt = _conn.createStatement();
        if (wrap)
            stmnt = DelegatingStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String str) throws SQLException {
        return prepareStatement(str, true);
    }

    /**
     * Prepare a statement, with the option of not wrapping it in a
     * {@link DelegatingPreparedStatement}, which is the default.
     */
    protected PreparedStatement prepareStatement(String str, boolean wrap)
        throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(str, false);
        else
            stmnt = _conn.prepareStatement(str, ResultSet.TYPE_FORWARD_ONLY, 
                ResultSet.CONCUR_READ_ONLY);
        if (wrap)
            stmnt = DelegatingPreparedStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public CallableStatement prepareCall(String str) throws SQLException {
        return prepareCall(str, true);
    }

    /**
     * Prepare a call, with the option of not wrapping it in a
     * {@link DelegatingCallableStatement}, which is the default.
     */
    protected CallableStatement prepareCall(String str, boolean wrap)
        throws SQLException {
        CallableStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareCall(str, false);
        else
            stmnt = _conn.prepareCall(str);
        if (wrap)
            stmnt = DelegatingCallableStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public String nativeSQL(String str) throws SQLException {
        return _conn.nativeSQL(str);
    }

    public void setAutoCommit(boolean bool) throws SQLException {
        _conn.setAutoCommit(bool);
    }

    public boolean getAutoCommit() throws SQLException {
        return _conn.getAutoCommit();
    }

    public void commit() throws SQLException {
        _conn.commit();
    }

    public void rollback() throws SQLException {
        _conn.rollback();
    }

    public void close() throws SQLException {
        _conn.close();
    }

    public boolean isClosed() throws SQLException {
        return _conn.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return getMetaData(true);
    }

    /**
     * Return the metadata, with the option of not wrapping it in a
     * {@link DelegatingDatabaseMetaData}, which is the default.
     */
    protected DatabaseMetaData getMetaData(boolean wrap) throws SQLException {
        DatabaseMetaData meta;
        if (_del != null)
            meta = _del.getMetaData(false);
        else
            meta = _conn.getMetaData();
        if (wrap)
            meta = DelegatingDatabaseMetaData.newInstance(meta, this);
        return meta;
    }

    public void setReadOnly(boolean bool) throws SQLException {
        _conn.setReadOnly(bool);
    }

    public boolean isReadOnly() throws SQLException {
        return _conn.isReadOnly();
    }

    public void setCatalog(String str) throws SQLException {
        _conn.setCatalog(str);
    }

    public String getCatalog() throws SQLException {
        return _conn.getCatalog();
    }

    public void setTransactionIsolation(int i) throws SQLException {
        _conn.setTransactionIsolation(i);
    }

    public int getTransactionIsolation() throws SQLException {
        return _conn.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return _conn.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        _conn.clearWarnings();
    }

    public Statement createStatement(int type, int concur) throws SQLException {
        return createStatement(type, concur, true);
    }

    /**
     * Create a statement, with the option of not wrapping it in a
     * {@link DelegatingStatement}, which is the default.
     */
    protected Statement createStatement(int type, int concur, boolean wrap)
        throws SQLException {
        Statement stmnt;
        if (_del != null)
            stmnt = _del.createStatement(type, concur, false);
        else
            stmnt = _conn.createStatement(type, concur);
        if (wrap)
            stmnt = DelegatingStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String str, int type, int concur)
        throws SQLException {
        return prepareStatement(str, type, concur, true);
    }

    /**
     * Prepare a statement, with the option of not wrapping it in a
     * {@link DelegatingPreparedStatement}, which is the default.
     */
    protected PreparedStatement prepareStatement(String str, int type,
        int concur, boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(str, type, concur, false);
        else
            stmnt = _conn.prepareStatement(str, type, concur);
        if (wrap)
            stmnt = DelegatingPreparedStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public CallableStatement prepareCall(String str, int type, int concur)
        throws SQLException {
        return prepareCall(str, type, concur, true);
    }

    /**
     * Prepare a call, with the option of not wrapping it in a
     * {@link DelegatingCallableStatement}, which is the default.
     */
    protected CallableStatement prepareCall(String str, int type, int concur,
        boolean wrap) throws SQLException {
        CallableStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareCall(str, type, concur, false);
        else
            stmnt = _conn.prepareCall(str, type, concur);
        if (wrap)
            stmnt = DelegatingCallableStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return _conn.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        _conn.setTypeMap(map);
    }

    // JDBC 3.0 methods follow.

    public void setHoldability(int holdability) throws SQLException {
        _conn.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return _conn.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return _conn.setSavepoint();
    }

    public Savepoint setSavepoint(String savepoint) throws SQLException {
        return _conn.setSavepoint(savepoint);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        _conn.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        _conn.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        return createStatement(resultSetType, resultSetConcurrency,
            resultSetHoldability, true);
    }

    protected Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability, boolean wrap)
        throws SQLException {
        Statement stmnt;
        if (_del != null)
            stmnt = _del.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability, false);
        else {
            stmnt = _conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        if (wrap)
            stmnt = DelegatingStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql,
        int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        return prepareStatement(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        int resultSetType, int resultSetConcurrency, int resultSetHoldability,
        boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability, false);
        else {
            stmnt = _conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        if (wrap)
            stmnt = DelegatingPreparedStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public CallableStatement prepareCall(String sql,
        int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        return prepareCall(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability, true);
    }

    protected CallableStatement prepareCall(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability, boolean wrap)
        throws SQLException {
        CallableStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareCall(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability, false);
        else {
            stmnt = _conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        if (wrap)
            stmnt = DelegatingCallableStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
        return prepareStatement(sql, autoGeneratedKeys, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        int autoGeneratedKeys, boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, autoGeneratedKeys);
        else {
            stmnt = _conn.prepareStatement(sql, autoGeneratedKeys);
        }
        if (wrap)
            stmnt = DelegatingPreparedStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException {
        return prepareStatement(sql, columnIndexes, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        int[] columnIndexes, boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, columnIndexes, wrap);
        else {
            stmnt = _conn.prepareStatement(sql, columnIndexes);
        }
        if (wrap)
            stmnt = DelegatingPreparedStatement.newInstance(stmnt, this);
        return stmnt;
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException {
        return prepareStatement(sql, columnNames, true);
    }

    protected PreparedStatement prepareStatement(String sql,
        String[] columnNames, boolean wrap) throws SQLException {
        PreparedStatement stmnt;
        if (_del != null)
            stmnt = _del.prepareStatement(sql, columnNames, wrap);
        else {
            stmnt = _conn.prepareStatement(sql, columnNames);
        }
        if (wrap)
            stmnt = DelegatingPreparedStatement.newInstance(stmnt, this);
        return stmnt;
    }

    // java.sql.Wrapper implementation (JDBC 4)
    public boolean isWrapperFor(Class iface) {
        return iface.isAssignableFrom(getDelegate().getClass());
    }

    public Object unwrap(Class iface) {
        if (isWrapperFor(iface))
            return getDelegate();
        else
            return null;
    }
}
