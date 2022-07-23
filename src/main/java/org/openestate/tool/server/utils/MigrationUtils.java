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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.StringUtils;
import org.hsqldb.persist.HsqlDatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for database migration.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
public final class MigrationUtils {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationUtils.class);
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(MigrationUtils.class);

    private MigrationUtils() {
        super();
    }

    /**
     * Migrate from an older database version.
     *
     * @param dbDir database directory
     * @param name  database name
     * @throws IOException if migration failed
     */
    public static void migrateFromOldDatabase(File dbDir, String name) throws IOException {
        if (dbDir == null || !dbDir.isDirectory()) return;

        // look for database files
        final File dbScriptFile = new File(dbDir, name + ".script");
        final File dbPropsFile = new File(dbDir, name + ".properties");
        if (!dbPropsFile.isFile() || !dbScriptFile.isFile()) return;

        // load database parameters
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(dbPropsFile)) {
            props.load(input);
        }

        final String dbVersion = StringUtils.trimToEmpty(props.getProperty("version"));

        // upgrade database structures from HSQLDB 2.2.x
        if (dbVersion.startsWith("2.2.")) {
            LOGGER.info("Migrating database '" + dbDir.getAbsolutePath() + "' from " + dbVersion + " to " + HsqlDatabaseProperties.THIS_VERSION + ".");

            File dbScriptFileNew = new File(dbDir, dbScriptFile.getName() + ".new");
            File dbScriptFileOld = new File(dbDir, dbScriptFile.getName() + ".old");
            //noinspection RegExpRedundantEscape
            Pattern pattern = Pattern.compile(
                    "SELECT ([\\w]*) INTO ([\\w]*) FROM ([\\w\\.]*) WHERE ([\\w]*)\\s?=\\s?CURRENT VALUE FOR ([\\w\\.]*);");

            try (Writer output = new FileWriterWithEncoding(dbScriptFileNew, "UTF-8")) {
                for (String line : FileUtils.readLines(dbScriptFile, "UTF-8")) {
                    Matcher m = pattern.matcher(line);
                    while (m.find()) {
                        line = StringUtils.replace(
                                line, m.group(0), "SET " + m.group(2) + " = IDENTITY();");
                    }
                    output.write(line);
                    output.write(System.lineSeparator());
                }
                output.flush();
            }

            FileUtils.copyFile(dbScriptFile, dbScriptFileOld);
            FileUtils.copyFile(dbScriptFileNew, dbScriptFile);
        }
    }
}
