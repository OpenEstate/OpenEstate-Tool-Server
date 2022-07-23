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
package org.openestate.tool.server.manager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hsqldb.lib.RCData;
import org.hsqldb.lib.tar.TarGenerator;
import org.hsqldb.lib.tar.TarMalformatException;
import org.openestate.tool.server.ServerUtils;
import org.openestate.tool.server.utils.DumpUtils;
import org.openestate.tool.server.utils.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created automated backups of the HSQLDB database.
 * <p>
 * This application connects to a currently running HSQLDB server and creates backups of its provided databases.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
@SuppressWarnings("SqlNoDataSourceInspection")
public class ManagerBackup {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(ManagerBackup.class);
    private static final String HELP_OPTION = "help";
    private static final String CONF_OPTION = "conf";
    private static final String ID_OPTION = "id";
    private static final String DIR_OPTION = "dir";
    private static final String LIMIT_OPTION = "limit";
    private static final String DUMP_OPTION = "dump";
    private static final String DELAY_OPTION = "delay";
    private static final String WAIT_OPTION = "wait";

    static {
        ServerUtils.init();

        // Create the logger instance after initialization. This makes sure, that logging environment is properly
        // configured before the logger is actually created.
        LOGGER = LoggerFactory.getLogger(ManagerBackup.class);
    }

    /**
     * Create a database backup.
     *
     * @param c         database connection
     * @param backupDir directory, where backups are stored
     * @throws SQLException if communication with the database failed
     */
    private static synchronized void doBackup(Connection c, File backupDir) throws SQLException {
        String path = StringUtils.replace(backupDir.getAbsolutePath(), "'", "\\'");
        Statement q = c.createStatement();
        q.execute("BACKUP DATABASE TO '" + path + "/' BLOCKING;");
    }

    /**
     * Create a database dump.
     *
     * @param c         database connection
     * @param backupDir directory, where backups are stored
     * @throws IOException           if files can't be written
     * @throws SQLException          if communication with the database failed
     * @throws TarMalformatException if the tar.gz archive can't be written
     */
    private static synchronized void doDump(Connection c, File backupDir) throws IOException, SQLException, TarMalformatException {
        final DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        final File tempDir = new File(backupDir, "temp");
        final File archiveFile = new File(backupDir, "db-" + format.format(new Date()) + ".tar.gz");
        try {
            if (tempDir.exists())
                FileUtils.deleteQuietly(tempDir);
            if (!tempDir.exists() && !tempDir.mkdirs())
                throw new IOException("Can't create temporary backup directory!");
            if (!tempDir.isDirectory())
                throw new IOException("The temporary backup directory is invalid!");

            // save dump into temporary directory
            DumpUtils.dump(c, tempDir, "db");

            // create tar.gz archive of the temporary directory
            TarGenerator generator = new TarGenerator(archiveFile, true, null);
            Iterator<File> dumpedFiles = FileUtils.iterateFiles(tempDir, null, true);
            while (dumpedFiles.hasNext()) {
                File f = dumpedFiles.next();
                if (f.isDirectory()) continue;
                String tarPath = f.getAbsolutePath().substring(
                        tempDir.getAbsolutePath().length() + 1);
                generator.queueEntry(tarPath, f);
            }

            // TarGenerator writes stuff to System.err, that we like to ignore.
            final PrintStream err = System.err;
            try {
                System.setErr(new PrintStream(new NullOutputStream()));
                generator.write();
            } finally {
                System.setErr(err);
            }
        } finally {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    /**
     * Start backup application.
     *
     * @param args command line arguments
     */
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        final Options options = new Options()
                .addOption(
                        Option.builder(HELP_OPTION)
                                .longOpt("help")
                                .desc("Show usage information.")
                                .build()
                )
                .addOption(
                        Option.builder(CONF_OPTION)
                                .longOpt("config")
                                .hasArg()
                                .argName("file")
                                .desc("Configuration file with connection settings. By default manager.conf from the etc directory is used.")
                                .build()
                )
                .addOption(
                        Option.builder(ID_OPTION)
                                .longOpt("urlid")
                                .hasArgs()
                                .argName("urlid")
                                .desc("The connection ID of the database to backup defined in the provided configuration file. By default ALL connections from the configuration file are used. You may provide multiple ID's separated by a space - e.g.:" + System.lineSeparator() + "-id database1 database2")
                                .build()
                )
                .addOption(
                        Option.builder(DIR_OPTION)
                                .longOpt("targetDir")
                                .hasArg()
                                .argName("path")
                                .desc("The Path to the directory, where backups are stored. By default backups are stored in the backups subfolder of the var directory.")
                                .build()
                )
                .addOption(
                        Option.builder(LIMIT_OPTION)
                                .longOpt("limit")
                                .hasArg()
                                .argName("number")
                                .desc("Set the maximum number of backups to keep per database. Oldest backup files are removed if the limit is exceeded. Set this value to 0 in order to disable automatic removal. By default 5 backups are kept per database.")
                                .build()
                )
                .addOption(
                        Option.builder(DUMP_OPTION)
                                .longOpt("dump")
                                .desc("Create a database dump instead of copying the raw database files.")
                                .build()
                )
                .addOption(
                        Option.builder(DELAY_OPTION)
                                .longOpt("delay")
                                .hasArg()
                                .argName("seconds")
                                .desc("Delay execution for the specified amount of seconds.")
                                .build()
                )
                .addOption(
                        Option.builder(WAIT_OPTION)
                                .longOpt("wait")
                                .desc("Wait for user input before application shutdown.")
                                .build()
                );

        final CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args, false);
        } catch (ParseException ex) {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("Invalid command line arguments!");
            System.err.println(ex.getLocalizedMessage());
            printHelp(options);
            System.exit(1);
            return;
        }

        // detect wait
        final boolean wait = commandLine.hasOption(WAIT_OPTION);

        if (commandLine.hasOption(HELP_OPTION)) {
            printHelp(options);
            if (wait) waitForEnter(false);
            System.exit(0);
            return;
        }

        // detect delay
        if (commandLine.hasOption(DELAY_OPTION)) {
            final int delay;
            try {
                delay = Integer.parseInt(StringUtils.trimToEmpty(commandLine.getOptionValue(DELAY_OPTION)));
            } catch (NumberFormatException ex) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("The provided delay is invalid!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
            if (delay > 0) {
                LOGGER.info("Waiting for " + delay + " seconds...");
                try {
                    Thread.sleep(delay * 1000);
                } catch (InterruptedException ex) {
                    LOGGER.warn("Sleep was interrupted!", ex);
                }
            }
        }

        // detect connection configuration
        final File rcFile;
        if (commandLine.hasOption(CONF_OPTION)) {
            // use the configuration file provided from the command line
            rcFile = new File(StringUtils.trimToEmpty(commandLine.getOptionValue(CONF_OPTION)));
            if (!rcFile.isFile()) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("The provided configuration file was not found!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
        } else {
            // use the default configuration file
            try {
                rcFile = new File(ServerUtils.getEtcDir(), "manager.conf");
            } catch (IOException ex) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Can't find the default configuration file!");
                System.err.println(ex.getLocalizedMessage());
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
        }

        // get list of available connection id's
        final Collection<String> urlIds;
        try {
            urlIds = ManagerUtils.getUrlIds(rcFile);
        } catch (IOException ex) {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("The configuration file is not readable!");
            System.err.println(ex.getLocalizedMessage());
            if (wait) waitForEnter(true);
            System.exit(1);
            return;
        }
        if (urlIds.isEmpty()) {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("The configuration file does not contain any connections!");
            if (wait) waitForEnter(true);
            System.exit(1);
            return;
        }

        // get target directory
        final File targetDir;
        if (commandLine.hasOption(DIR_OPTION)) {
            // use the target directory provided from the command line
            String path = StringUtils.trimToNull(commandLine.getOptionValue(DIR_OPTION));
            if (path == null) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("No target directory was specified!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
            targetDir = new File(path);
            if (!targetDir.isDirectory()) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("The provided target directory does not exist!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
        } else {
            // use the default backup directory
            try {
                targetDir = new File(ServerUtils.getVarDir(), "backups");
            } catch (IOException ex) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Can't find the default backup directory!");
                System.err.println(ex.getLocalizedMessage());
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
        }

        // detect connection ID's
        final List<String> urlIdsToBackup = new ArrayList<>();
        if (commandLine.hasOption(ID_OPTION)) {
            for (String urlId : commandLine.getOptionValues(ID_OPTION)) {
                urlId = StringUtils.trimToNull(urlId);
                if (urlId == null) continue;
                if (!urlIds.contains(urlId)) {
                    System.err.println("The connection ID '" + urlId + "' is not available in the configuration file.");
                    continue;
                }
                urlIdsToBackup.add(urlId);
            }
        } else {
            urlIdsToBackup.addAll(urlIds);
        }
        if (urlIdsToBackup.isEmpty()) {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("No databases were found for backup!");
            if (wait) waitForEnter(true);
            System.exit(1);
            return;
        }

        // detect backup limit
        final int limit;
        if (commandLine.hasOption(LIMIT_OPTION)) {
            try {
                limit = Integer.parseInt(StringUtils.trimToEmpty(commandLine.getOptionValue(LIMIT_OPTION)));
            } catch (NumberFormatException ex) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("The provided limit is invalid!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
        } else {
            limit = 5;
        }

        // detect dump
        final boolean dump = commandLine.hasOption(DUMP_OPTION);

        // trust all certificates
        try {
            SslUtils.installLooseSslSocketFactory();
        } catch (Exception ex) {
            LOGGER.warn("Can't setup SSL context!", ex);
        }

        // process database backup
        int count = 0;
        for (String urlId : urlIdsToBackup) {
            if (dump)
                LOGGER.info("Creating dump of '{}' database...", urlId);
            else
                LOGGER.info("Creating backup of '{}' database...", urlId);

            // get connection settings
            final RCData rcData;
            try {
                rcData = new RCData(rcFile, urlId);
            } catch (Exception ex) {
                LOGGER.error("Can't read connection configuration!", ex);
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Can't read connection configuration (" + ex.getLocalizedMessage() + ")!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }

            // init backup directory
            final File backupDir = new File(targetDir, urlId);
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                LOGGER.error("Can't create backup directory at '{}'!", backupDir.getAbsolutePath());
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Can't create backup directory at '" + backupDir.getAbsolutePath() + "'!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
            if (!backupDir.isDirectory()) {
                LOGGER.error("Invalid backup directory at '{}'!", backupDir.getAbsolutePath());
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Invalid backup directory at '" + backupDir.getAbsolutePath() + "'!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }

            // execute backup process
            try (Connection c = rcData.getConnection()) {
                if (dump)
                    doDump(c, backupDir);
                else
                    doBackup(c, backupDir);

                count++;
            } catch (Exception ex) {
                LOGGER.error("Backup failed for '" + urlId + "' database!", ex);
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Backup failed for '" + urlId + "' (" + ex.getLocalizedMessage() + ")!");
                if (wait) waitForEnter(true);
                System.exit(1);
            }

            // remove outdated backup files
            if (limit > 0) {
                TreeMap<Long, File> backupFiles = new TreeMap<>();
                for (File f : ObjectUtils.defaultIfNull(backupDir.listFiles(), new File[]{})) {
                    if (!f.isFile()) continue;
                    long stamp = f.lastModified();
                    while (backupFiles.containsKey(stamp)) {
                        stamp++;
                    }
                    backupFiles.put(stamp, f);
                }
                while (backupFiles.size() > limit) {
                    Map.Entry<Long, File> entry = backupFiles.firstEntry();
                    File f = entry.getValue();
                    LOGGER.info("Removing outdated backup for '{}' database at '{}'...", urlId, f.getAbsolutePath());
                    FileUtils.deleteQuietly(f);
                    backupFiles.remove(entry.getKey());
                }
            }
        }

        if (count == 1)
            LOGGER.info("One backup was saved at '" + targetDir.getAbsolutePath() + "'.");
        else
            LOGGER.info(count + " backups were saved at '" + targetDir.getAbsolutePath() + "'.");

        if (wait) waitForEnter(true);
    }

    /**
     * Print usage information to System.out.
     *
     * @param options command line options
     */
    private static void printHelp(Options options) {
        final String commandLine;
        if (SystemUtils.IS_OS_WINDOWS)
            commandLine = "ManagerBackup.bat / ManagerBackup.exe";
        else
            commandLine = "ManagerBackup.sh";

        System.out.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
        new HelpFormatter().printHelp(
                commandLine + " [OPTION]...",
                "Create automated backups of the currently running HSQLDB server. You might use the following custom settings:" + System.lineSeparator() + StringUtils.SPACE,
                options,
                StringUtils.SPACE + System.lineSeparator() + "See https://manual.openestate.org for more information."
        );
        System.out.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
    }

    /**
     * Wait for the user to press ENTER before continue.
     *
     * @param printSeparator print a separator before the message shown to the user
     */
    private static void waitForEnter(boolean printSeparator) {
        if (printSeparator) {
            System.out.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
        }
        System.out.println("Press ENTER to close this application.");
        System.console().readLine();
    }
}
