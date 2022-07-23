/*
 * Copyright 2009-2022 OpenEstate.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openestate.tool.server.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods to create database dumps.
 *
 * @author Andreas Rudolph
 */
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class DumpUtils {
    @SuppressWarnings("unused")
    private final static Logger LOGGER = LoggerFactory.getLogger(DumpUtils.class);
    private static long stamp = System.currentTimeMillis();

    /**
     * Create a database dump.
     *
     * @param c         database connection
     * @param directory target directory
     * @throws IOException  if files can't be written
     * @throws SQLException if communication with the database failed
     */
    public static void dump(Connection c, File directory) throws IOException, SQLException {
        dump(c, directory, null);
    }

    /**
     * Create a database dump.
     *
     * @param c         database connection
     * @param dbName    database name used in the generated files
     * @param directory target directory
     * @throws IOException  if files can't be written
     * @throws SQLException if communication with the database failed
     */
    public static void dump(Connection c, File directory, String dbName) throws IOException, SQLException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Can't create directory: " + directory.getAbsolutePath());
        }
        File lobsDirectory = new File(directory, "lobs");
        if (!lobsDirectory.isDirectory() && !lobsDirectory.mkdirs()) {
            throw new IOException("Can't create directory: " + lobsDirectory.getAbsolutePath());
        }

        // dump schema and non LOB data
        dbName = ObjectUtils.defaultIfNull(StringUtils.trimToNull(dbName), "dump");
        File dumpFile = new File(directory, dbName + ".script");
        dumpSchema(c, dumpFile);

        // dump LOB's into separate files
        File lobsFile = new File(directory, "lobs.sql");
        dumpLobs(c, lobsFile, lobsDirectory);

        // remove empty lobs.sql file
        if (lobsFile.isFile() && lobsFile.length() < 1) {
            FileUtils.deleteQuietly(lobsFile);
        }
    }

    /**
     * Dump LOB's into separate files.
     *
     * @param c        database connection
     * @param lobsFile SQL file for a later import of LOB files
     * @param lobsDir  directory to store exported LOB files
     * @throws IOException  if files can't be written
     * @throws SQLException if communication with the database failed
     */
    private static void dumpLobs(Connection c, File lobsFile, File lobsDir) throws IOException, SQLException {
        PreparedStatement statement = null;
        ResultSet result = null;
        int i;

        boolean append = lobsFile.exists();
        try (Writer lobsWriter = new FileWriterWithEncoding(lobsFile, StandardCharsets.UTF_8, append)) {
            // get LOB fields from the database
            statement = c.prepareStatement("SELECT * "
                    + "FROM information_schema.system_columns "
                    + "WHERE type_name IN (?, ?) "
                    + "AND table_cat IN (?) "
                    + "AND table_name IN (SELECT table_name FROM information_schema.system_tables WHERE table_type=?);");
            i = 0;
            statement.setString(++i, "BLOB");
            statement.setString(++i, "CLOB");
            statement.setString(++i, "PUBLIC");
            statement.setString(++i, "TABLE");
            result = statement.executeQuery();
            while (result.next()) {
                String tableCat = result.getString("table_cat");
                String tableScheme = result.getString("table_schem");
                String tableName = result.getString("table_name");
                String columnName = result.getString("column_name");
                String columnType = result.getString("type_name");

                // export LOB column
                dumpLobsFromColumn(c, lobsWriter, lobsDir, tableCat, tableScheme, tableName, columnName, columnType);
            }
        } finally {
            if (statement != null) statement.close();
            if (result != null) result.close();
        }
    }

    /**
     * Dump LOB value of a certain table column.
     *
     * @param c           database connection
     * @param lobsWriter  writer of the SQL file for a later import of LOB files
     * @param lobsDir     directory to store exported LOB files
     * @param tableCat    table catalog
     * @param tableScheme table schema
     * @param tableName   table name
     * @param columnName  column name
     * @param columnType  column type
     * @throws SQLException if communication with the database failed
     */
    private static void dumpLobsFromColumn(Connection c, Writer lobsWriter, File lobsDir, String tableCat, String tableScheme, String tableName, String columnName, String columnType) throws SQLException {
        if (!"BLOB".equalsIgnoreCase(columnType) && !"CLOB".equalsIgnoreCase(columnType)) {
            LOGGER.warn("Column '" + tableScheme + "." + tableName + "." + columnName + "' is neither a BLOB nor a CLOB!");
            return;
        }

        // get primary key columns from the LOB table
        String[] tablePK = getPrimaryKeys(c, tableCat, tableScheme, tableName);
        if (ArrayUtils.isEmpty(tablePK)) {
            LOGGER.warn("Can't export LOBS from '" + tableScheme + "." + tableName + "." + columnName + "' without primary key!");
            return;
        }

        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            List<String> columns = new ArrayList<>(Arrays.asList(tablePK));
            columns.add(columnName);

            statement = c.prepareStatement("SELECT " + StringUtils.join(columns, ",") + " "
                    + "FROM " + tableName + " "
                    + "ORDER BY " + StringUtils.join(tablePK, ", ") + ";");

            result = statement.executeQuery();
            while (result.next()) {
                // get primary key value for the table row
                List<String> pkConditions = new ArrayList<>();
                boolean pkIsInvalid = false;
                for (String pkCol : tablePK) {
                    Object pkValue = result.getObject(pkCol);
                    if (pkValue instanceof Number) {
                        pkConditions.add(pkCol + " = " + pkValue.toString());
                    } else if (pkValue instanceof String) {
                        pkConditions.add(pkCol + " = '" + pkValue.toString() + "'");
                    } else {
                        if (pkValue == null)
                            LOGGER.warn("Empty primary key in '" + tableScheme + "." + tableName + "." + pkCol + "'");
                        else
                            LOGGER.warn("Unsupported type of primary key in '" + tableScheme + "." + tableName + "." + pkCol + "' (" + pkValue.getClass().getName() + ")");
                        pkIsInvalid = true;
                        break;
                    }
                }
                if (pkIsInvalid) continue;

                try {
                    String lobFileLookup = null;

                    // get BLOB value
                    if ("BLOB".equalsIgnoreCase(columnType)) {
                        Blob blob = null;
                        try {
                            blob = result.getBlob(columnName);
                            if (blob != null) {
                                File file = writeBlob(blob, lobsDir);
                                String relativePath = file.getAbsolutePath().substring(lobsDir.getParentFile().getAbsolutePath().length() + 1);
                                lobFileLookup = "LOAD_FILE('" + relativePath + "')";
                            }
                        } finally {
                            if (blob != null) blob.free();
                        }
                    }

                    // get CLOB value
                    else if ("CLOB".equalsIgnoreCase(columnType)) {
                        Clob clob = null;
                        try {
                            clob = result.getClob(columnName);
                            if (clob != null) {
                                File file = writeClob(clob, lobsDir);
                                String relativePath = file.getAbsolutePath().substring(lobsDir.getParentFile().getAbsolutePath().length() + 1);
                                lobFileLookup = "LOAD_FILE('" + relativePath + "', '" + StandardCharsets.UTF_8.name() + "')";
                            }
                        } finally {
                            if (clob != null) clob.free();
                        }
                    } else {
                        continue;
                    }

                    if (lobFileLookup != null) {
                        lobsWriter.write("UPDATE " + tableScheme + "." + tableName + " "
                                + "SET " + columnName + " = " + lobFileLookup + " "
                                + "WHERE " + StringUtils.join(pkConditions, " AND ") + ";");
                        lobsWriter.write(System.lineSeparator());
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Can't write LOB!");
                    LOGGER.warn("> table       : " + tableScheme + "." + tableName);
                    LOGGER.warn("> lob column  : " + columnName);
                    LOGGER.warn("> primary key : " + StringUtils.join(pkConditions, ", "));
                    LOGGER.warn("> " + ex.getLocalizedMessage(), ex);
                    //continue;
                }
            }
        } finally {
            if (statement != null) statement.close();
            if (result != null) result.close();
        }
    }

    /**
     * Dump schema and non LOB data.
     *
     * @param c          database connection
     * @param schemaFile SQL file to store the schema and non LOB data
     * @throws SQLException if communication with the database failed
     */
    private static void dumpSchema(Connection c, File schemaFile) throws SQLException {
        try (Statement statement = c.createStatement()) {
            statement.execute("SCRIPT '" + StringUtils.replace(schemaFile.getAbsolutePath(), "'", "\\'") + "'");
        }
    }

    /**
     * Get primary keys of a certain table.
     *
     * @param c           database connection
     * @param tableCat    table catalog
     * @param tableScheme table schema
     * @param tableName   table name
     * @return names of primary key columns
     * @throws SQLException if communication with the database failed
     */
    private static String[] getPrimaryKeys(Connection c, String tableCat, String tableScheme, String tableName) throws SQLException {
        PreparedStatement statement = null;
        ResultSet result = null;
        List<String> keys = new ArrayList<>();
        try {
            statement = c.prepareStatement("SELECT column_name "
                    + "FROM information_schema.system_primarykeys "
                    + "WHERE table_cat = ? AND table_schem = ? AND table_name = ? "
                    + "ORDER BY key_seq ASC;");
            int i = 0;
            statement.setString(++i, tableCat);
            statement.setString(++i, tableScheme);
            statement.setString(++i, tableName);
            result = statement.executeQuery();
            while (result.next()) {
                keys.add(result.getString("column_name"));
            }
            return keys.toArray(new String[0]);
        } finally {
            if (statement != null) statement.close();
            if (result != null) result.close();
        }
    }

    /**
     * Write a BLOB value into a file.
     *
     * @param blob    BLOB value
     * @param lobsDir directory to store exported LOB files
     * @return created file in the provided directory
     * @throws IOException  if files can't be written
     * @throws SQLException if communication with the database failed
     */
    private static File writeBlob(Blob blob, File lobsDir) throws IOException, SQLException {
        final long id;
        synchronized (DumpUtils.class) {
            id = ++stamp;
        }
        File blobFile = new File(lobsDir, id + ".blob");
        try (OutputStream output = new FileOutputStream(blobFile)) {
            IOUtils.copy(blob.getBinaryStream(), output);
            output.flush();
            return blobFile;
        } catch (IOException | SQLException ex) {
            FileUtils.deleteQuietly(blobFile);
            throw ex;
        }
    }

    /**
     * Write a CLOB value into a file.
     *
     * @param clob    CLOB value
     * @param lobsDir directory to store exported LOB files
     * @return created file in the provided directory
     * @throws IOException  if files can't be written
     * @throws SQLException if communication with the database failed
     */
    private static File writeClob(Clob clob, File lobsDir) throws IOException, SQLException {
        final long id;
        synchronized (DumpUtils.class) {
            id = ++stamp;
        }
        File clobFile = new File(lobsDir, id + ".clob");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(clobFile), StandardCharsets.UTF_8)) {
            IOUtils.copy(clob.getCharacterStream(), writer);
            writer.flush();
            return clobFile;
        } catch (IOException | SQLException ex) {
            FileUtils.deleteQuietly(clobFile);
            throw ex;
        }
    }
}
