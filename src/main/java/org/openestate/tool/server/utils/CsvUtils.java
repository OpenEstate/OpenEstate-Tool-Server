/*
 * Copyright 2009-2019 OpenEstate.org.
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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for importing & exporting the database in CSV format.
 *
 * @author Andreas Rudolph
 */
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class CsvUtils {
    @SuppressWarnings("unused")
    private final static Logger LOGGER = LoggerFactory.getLogger(CsvUtils.class);
    private static long stamp = System.currentTimeMillis();

    private static CSVFormat createFormat() {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withAllowDuplicateHeaderNames(false)
                .withQuoteMode(QuoteMode.MINIMAL)
                .withQuote('"');
    }

    /**
     * Create a database dump.
     *
     * @param c         database connection
     * @param directory target directory
     * @throws IOException  if files can't be written
     * @throws SQLException if communication with the database failed
     */
    public static void dump(Connection c, File directory, boolean withLobs) throws IOException, SQLException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Can't create directory: " + directory.getAbsolutePath());
        }

        // dump table data without LOB's
        dumpData(c, directory);

        // dump LOB's into separate files
        if (withLobs) {
            File lobsDirectory = new File(directory, "lobs");
            if (!lobsDirectory.isDirectory() && !lobsDirectory.mkdirs()) {
                throw new IOException("Can't create directory: " + lobsDirectory.getAbsolutePath());
            }

            File lobsFile = new File(directory, "lobs.sql");
            dumpLobs(c, lobsFile, lobsDirectory);

            // remove empty lobs.sql file
            if (lobsFile.isFile() && lobsFile.length() < 1) {
                FileUtils.deleteQuietly(lobsFile);
            }
        }
    }

    /**
     * Dump table data without LOB's.
     *
     * @param c         database connection
     * @param directory target directory to store table data
     * @throws SQLException if communication with the database failed
     */
    private static void dumpData(Connection c, File directory) throws SQLException, IOException {
        final String query = "SELECT table_name " +
                "FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE table_schema='PUBLIC' " +
                "AND table_type='BASE TABLE' " +
                "ORDER BY table_name ASC;";

        try (Statement statement = c.createStatement();
             ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                String tableName = result.getString("table_name");
                dumpTable(c, tableName, new File(directory, tableName + ".csv"));
            }
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
    @SuppressWarnings("Duplicates")
    private static void dumpLobs(Connection c, File lobsFile, File lobsDir) throws IOException, SQLException {
        final String query = "SELECT * "
                + "FROM information_schema.system_columns "
                + "WHERE type_name IN (?, ?) "
                + "AND table_cat IN (?) "
                + "AND table_name IN (SELECT table_name FROM information_schema.system_tables WHERE table_type=?);";

        int i;
        boolean append = lobsFile.exists();
        ResultSet result = null;
        try (PreparedStatement statement = c.prepareStatement(query);
             Writer lobsWriter = new FileWriterWithEncoding(lobsFile, StandardCharsets.UTF_8, append)) {

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
    @SuppressWarnings("Duplicates")
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
     * Write table rows into a CSV file.
     *
     * @param c         database connection
     * @param tableName table name
     * @param csvFile   CSV file
     * @throws SQLException if communication with the database failed
     * @throws IOException  if files can't be written
     */
    private static void dumpTable(Connection c, String tableName, File csvFile) throws SQLException, IOException {
        LOGGER.info("Dumping table {}...", tableName);
        final String query = "SELECT * FROM " + tableName + ";";

        try (Statement statement = c.createStatement();
             ResultSet result = statement.executeQuery(query);
             CSVPrinter printer = createFormat().withHeader(result)
                     .print(csvFile, StandardCharsets.UTF_8)) {

            final int columnCount = result.getMetaData().getColumnCount();
            while (result.next()) {
                for (int i = 1; i <= columnCount; ++i) {
                    Object object = result.getObject(i);
                    if (object instanceof Blob)
                        printer.print(null); // TODO: ggf. Lob's als Base64 exportieren
                    else if (object instanceof Clob)
                        printer.print(((Clob) object).getCharacterStream());
                    else if (object != null)
                        printer.print(object);
                    else
                        printer.print("NULL");
                }
                printer.println();
            }
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
    @SuppressWarnings("Duplicates")
    private static String[] getPrimaryKeys(Connection c, String tableCat, String tableScheme, String tableName) throws SQLException {
        final String query = "SELECT column_name "
                + "FROM information_schema.system_primarykeys "
                + "WHERE table_cat = ? AND table_schem = ? AND table_name = ? "
                + "ORDER BY key_seq ASC;";

        ResultSet result = null;
        try (PreparedStatement statement = c.prepareStatement(query)) {
            int i = 0;
            statement.setString(++i, tableCat);
            statement.setString(++i, tableScheme);
            statement.setString(++i, tableName);
            result = statement.executeQuery();

            List<String> keys = new ArrayList<>();
            while (result.next()) {
                keys.add(result.getString("column_name"));
            }
            return keys.toArray(new String[0]);
        } finally {
            if (result != null) result.close();
        }
    }

    public static void restore(Connection c, File directory) throws SQLException, IOException {
        ResultSet sequences = null;
        ResultSet lastId = null;
        try (Statement statement = c.createStatement()) {
            try {
                statement.execute("SET DATABASE REFERENTIAL INTEGRITY FALSE;");

                final File[] csvFiles = directory.listFiles((FileFilter) new SuffixFileFilter(".csv", IOCase.INSENSITIVE));
                final Map<String, File> csvFilesMap = new TreeMap<>();
                if (csvFiles != null && ArrayUtils.isNotEmpty(csvFiles)) {
                    for (File csvFile : csvFiles) {
                        String tableName = FilenameUtils.getBaseName(csvFile.getName());
                        csvFilesMap.put(tableName, csvFile);
                    }
                }

                // restore tables
                for (Map.Entry<String, File> e : csvFilesMap.entrySet()) {
                    restoreTable(c, e.getKey(), e.getValue());
                }

                // reload sequences
                sequences = statement.executeQuery("SELECT sequence_name " +
                        "FROM INFORMATION_SCHEMA.SEQUENCES " +
                        "WHERE sequence_schema='PUBLIC' " +
                        "ORDER BY sequence_name ASC;");
                while (sequences.next()) {
                    final String sequenceName = StringUtils.trimToEmpty(sequences.getString("sequence_name"));
                    if (!sequenceName.startsWith("SEQ_")) continue;

                    final String tableName = sequenceName.substring(4);
                    final String[] tablePks = getPrimaryKeys(c, "PUBLIC", "PUBLIC", tableName);
                    if (tablePks.length != 1) {
                        LOGGER.warn("Can't update primary key for table {}!", tableName);
                        continue;
                    }

                    lastId = statement.executeQuery("SELECT " + tablePks[0] + " " +
                            "FROM " + tableName + " " +
                            "ORDER BY " + tablePks[0] + " DESC " +
                            "LIMIT 1;");

                    long nextId = 1L;
                    if (lastId.next()) {
                        nextId = lastId.getLong(tablePks[0]) + 1;
                    }

                    LOGGER.info("Setting sequence {} to {}...", sequenceName, nextId);
                    statement.execute("ALTER SEQUENCE " + sequenceName + " RESTART WITH " + nextId + ";");
                }
            } finally {
                statement.execute("SET DATABASE REFERENTIAL INTEGRITY TRUE;");
                statement.execute("CHECKPOINT DEFRAG");
                if (sequences!=null) sequences.close();
            }
        }
    }

    private static void restoreTable(Connection c, String tableName, File csvFile) throws SQLException, IOException {
        final String query = "SELECT * FROM " + tableName + " LIMIT 1;";
        LOGGER.info("Restoring table {}...", tableName);

        try (Statement statement = c.createStatement();
             ResultSet meta = statement.executeQuery(query);
             CSVParser parser = createFormat().parse(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

            final List<String> csvHeader = parser.getHeaderNames();
            //LOGGER.debug("CSV-HEADER: {}", StringUtils.join(csvHeader, ", "));

            final ResultSetMetaData metaData = meta.getMetaData();
            final Map<String, Integer> columnTypes = new HashMap<>();
            for (final String columnName : csvHeader) {
                int columnType = 0;
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    String n = metaData.getColumnName(i + 1);
                    if (n.equalsIgnoreCase(columnName)) {
                        columnType = metaData.getColumnType(i + 1);
                    }
                }
                columnTypes.put(columnName, columnType);
            }

            for (CSVRecord csvRecord : parser) {
                //LOGGER.info("Importing record {} at line {}...", parser.getRecordNumber(), parser.getCurrentLineNumber());
                final DateFormat dbTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                final DateFormat dbTimestampTzFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSXXX");

                final Map<String, String> row = new LinkedHashMap<>();
                for (int col = 0; col < csvHeader.size(); col++) {
                    final String columnName = csvHeader.get(col);
                    final int columnType = columnTypes.getOrDefault(columnName, Types.NULL);
                    final String csvValue = csvRecord.get(col);

                    String columnValue;
                    if ("NULL".equalsIgnoreCase(csvValue)) {
                        columnValue = "NULL";
                    } else {
                        switch (columnType) {
                            case Types.BLOB:
                            case Types.NULL:
                                columnValue = "NULL";
                                break;

                            case Types.DATE:
                                columnValue = "'" + csvValue + "'";
                                break;

                            case Types.TIME:
                            case Types.TIME_WITH_TIMEZONE:
                                columnValue = "'" + csvValue + "'";
                                break;

                            case Types.TIMESTAMP:
                                try {
                                    Date date = DateUtils.parseDateStrictly(
                                            csvValue,
                                            "yyyy-MM-dd'T'HH:mm:ss.SSS",
                                            "yyyy-MM-dd'T'HH:mm:ss",
                                            "yyyy-MM-dd'T'HH:mm");

                                    columnValue = "'" + dbTimestampFormat.format(date) + "'";
                                } catch (ParseException ex) {
                                    LOGGER.warn("Can't convert timestamp '{}'!", csvValue);
                                    columnValue = "NULL";
                                }

                                break;

                            case Types.TIMESTAMP_WITH_TIMEZONE:
                                try {
                                    Date date = DateUtils.parseDateStrictly(
                                            csvValue,
                                            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                                            "yyyy-MM-dd'T'HH:mm:ssXXX",
                                            "yyyy-MM-dd'T'HH:mmXXX");

                                    columnValue = "'" + dbTimestampTzFormat.format(date) + "'";
                                } catch (ParseException ex) {
                                    LOGGER.warn("Can't convert timestamp with timezone '{}'!", csvValue);
                                    columnValue = "NULL";
                                }
                                break;

                            case Types.CHAR:
                            case Types.CLOB:
                            case Types.LONGNVARCHAR:
                            case Types.LONGVARCHAR:
                            case Types.NCHAR:
                            case Types.NCLOB:
                            case Types.NVARCHAR:
                            case Types.SQLXML:
                            case Types.VARCHAR:
                                columnValue = "'" + StringUtils.replace(csvRecord.get(col), "'", "''") + "'";
                                break;
                            default:
                                columnValue = csvValue;
                        }
                    }

                    if (columnValue != null) {
                        row.put(columnName, columnValue);
                    }
                }

                String insertQuery = "INSERT INTO "
                        + tableName + " (" + StringUtils.join(row.keySet(), ", ") + ") " +
                        "VALUES (" + StringUtils.join(row.values(), ", ") + ");";

                try {
                    statement.execute(insertQuery);
                } catch (SQLException ex) {
                    LOGGER.error("INSERT QUERY FAILED: {}", insertQuery);
                    throw ex;
                }

            }
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
    @SuppressWarnings("Duplicates")
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
    @SuppressWarnings("Duplicates")
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
