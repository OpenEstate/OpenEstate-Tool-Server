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
package org.openestate.tool.server.manager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collection;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hsqldb.lib.RCData;
import org.hsqldb.lib.tar.TarMalformatException;
import org.hsqldb.lib.tar.TarReader;
import org.openestate.tool.server.ServerUtils;
import org.openestate.tool.server.utils.CsvUtils;
import org.openestate.tool.server.utils.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Load a backup into the HSQLDB database.
 * <p>
 * This application connects to a currently running HSQLDB server and imports a backup.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlDialectInspection"})
public class ManagerRestore {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(ManagerBackup.class);
    private static final String HELP_OPTION = "help";
    private static final String CONF_OPTION = "conf";
    private static final String ID_OPTION = "id";
    private static final String FILE_OPTION = "file";
    private static final String DELAY_OPTION = "delay";
    private static final String WAIT_OPTION = "wait";

    static {
        ServerUtils.init();

        // Create the logger instance after initialization. This makes sure, that logging environment is properly
        // configured before the logger is actually created.
        LOGGER = LoggerFactory.getLogger(ManagerRestore.class);
    }

    private static void doUncompress(File archive, File targetDir) throws IOException, TarMalformatException {
        new TarReader(archive, TarReader.OVERWRITE_MODE, null, null, targetDir)
                .read();
    }

    /**
     * Start restore application.
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
                        Option.builder(FILE_OPTION)
                                .longOpt("srcFile")
                                .hasArg()
                                .argName("path")
                                .desc("Path to the backup file to import.")
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

        // detect backup file to restore
        final File archive;
        if (commandLine.hasOption(FILE_OPTION)) {
            archive = new File(commandLine.getOptionValue(FILE_OPTION));
            if (!archive.isFile() || !archive.getName().toLowerCase().endsWith(".tar.gz")) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("An invalid backup file was provided!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }
        } else {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("No backup file was provided!");
            if (wait) waitForEnter(true);
            System.exit(1);
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

        // detect connection ID
        final String urlId;
        if (commandLine.hasOption(ID_OPTION)) {
            urlId = commandLine.getOptionValue(ID_OPTION);
        } else {
            urlId = urlIds.iterator().next();
        }
        if (!urlIds.contains(urlId)) {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("Database ID '" + urlId + "' is unknown!");
            if (wait) waitForEnter(true);
            System.exit(1);
            return;
        }

        // trust all certificates
        try {
            SslUtils.installLooseSslSocketFactory();
        } catch (Exception ex) {
            LOGGER.warn("Can't setup SSL context!", ex);
        }

        // process database restore
        File tempDir = null;
        try {
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


            // uncompress archive
            try {
                //tempDir = Files.createTempDirectory("restore").toFile();
                tempDir = new File("restore");
                tempDir.mkdirs();
                doUncompress(archive, tempDir);
            } catch (Exception ex) {
                LOGGER.error("Can't uncompress the archive!", ex);
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Can't uncompress the archive (" + ex.getLocalizedMessage() + ")!");
                if (wait) waitForEnter(true);
                System.exit(1);
                return;
            }

            // import archive
            try (Connection c = rcData.getConnection()) {
                try (Statement s = c.createStatement()) {
                    s.execute("TRUNCATE SCHEMA PUBLIC AND COMMIT NO CHECK;");
                }

                CsvUtils.restore(c, tempDir);
            } catch (Exception ex) {
                LOGGER.error("Restore failed for '" + urlId + "' database!", ex);
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("Restore failed for '" + urlId + "' (" + ex.getLocalizedMessage() + ")!");
                if (wait) waitForEnter(true);
                System.exit(1);
            }

        } finally {
            //if (tempDir != null && tempDir.isDirectory())
            //    FileUtils.deleteQuietly(tempDir);
        }


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
