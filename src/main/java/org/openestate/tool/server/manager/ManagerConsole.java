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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hsqldb.cmdline.SqlTool;
import org.openestate.tool.server.ServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Console tool for management of a HSQLDB database.
 * <p>
 * This application basically starts {@link SqlTool} provided by HSQLDB.
 * It uses custom command line arguments for a better integration into the server environment.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
public class ManagerConsole extends SqlTool {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(ManagerConsole.class);
    private static final String HELP_OPTION = "help";
    private static final String CONF_OPTION = "conf";
    private static final String ID_OPTION = "id";
    private static final String AUTO_COMMIT_OPTION = "autoCommit";
    private static final String CONTINUE_ON_ERROR_OPTION = "noError";
    private static final String DEBUG_OPTION = "debug";
    private static final String SQL_OPTION = "sql";
    private static final String SET_OPTION = "set";
    private static final String DELAY_OPTION = "delay";

    static {
        ServerUtils.init();
        LOGGER = LoggerFactory.getLogger(ManagerConsole.class);
    }

    /**
     * Start console application for database management.
     *
     * @param args command line arguments
     */
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {

        // Options provided by SqlTool
        //
        // Syntax: java org.hsqldb.cmdline.SqlTool [--opt[=optval]...] urlid [file1.sql...]
        // urlid                    ID of connection settings in rcfile.
        // '-' means to not connect to any data source
        // file1.sql...             SQL files to be executed [stdin]
        // '-' means non-interactive stdin.
        // OR: java org.hsqldb.cmdline.SqlTool [--opt[=optval]...]
        //                          (Precisely equivalent to the first case, with urlid of '-' and no SQL files)
        // Options:
        // --help                   Displays this message
        // --autoCommit             Auto-commit JDBC DML commands
        // --continueOnErr=true|false  Continue (if true) or Abort (false) upon errors
        // --debug                  Print Debug info to stderr
        // --driver=a.b.c.Driver    JDBC driver class [org.hsqldb.jdbc.JDBCDriver]
        // --inlineRc=url=val1,user=val2[,asetting=val3...][,password=]
        //                          Inline RC variables (use --driver for driver)
        // --list                   List urlids in the rc file
        // --noAutoFile             Do not execute auto.sql from home dir
        // --noInput                Do not read stdin (default if sql file given or --sql switch used).
        // --rcFile=/file/path.rc   Connect Info File [$HOME/sqltool.rc]
        // --setVar=NAME1=val1[,NAME2=val2...]   PL variables. May use multiple instances of this switch.
        // -p NAME=value            Assign a single PL variable. May use multiple instances of this switch.
        // --sql="SQL; Statements;" Execute given SQL instead of stdin (before SQL files if any are specified) where "SQL" consists of SQL command(s).  See the Guide.
        // --stdInput               Read stdin IN ADDITION to sql files/--sql input
        //
        // Values set with inlineRc or setVar may use \ to escape commas inside of values.

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
                                .hasArg()
                                .argName("urlid")
                                .desc("The connection ID defined in the provided configuration file. By default the first connection from the configuration file is used.")
                                .build()
                )
                .addOption(
                        Option.builder(AUTO_COMMIT_OPTION)
                                .longOpt("autoCommit")
                                .desc("Commit JDBC DML commands automatically.")
                                .build()
                )
                .addOption(
                        Option.builder(CONTINUE_ON_ERROR_OPTION)
                                .longOpt("continueOnError")
                                .desc("Continue execution in case of errors.")
                                .build()
                )
                .addOption(
                        Option.builder(DEBUG_OPTION)
                                .longOpt("debug")
                                .desc("Print debugging information to stderr.")
                                .build()
                )
                .addOption(
                        Option.builder(SQL_OPTION)
                                .longOpt("sql")
                                .hasArg()
                                .argName("sql-query")
                                .desc("SQL queries, that are executed by the application. Multiple queries can be separated by a semicolon - e.g.:" + System.lineSeparator() + "-sql \"QUERY1; QUERY2;\"")
                                .build()
                )
                .addOption(
                        Option.builder(SET_OPTION)
                                .longOpt("setVar")
                                .hasArgs()
                                .argName("KEY=VALUE")
                                .desc("Assign PL variables. You may add multiple key-value pairs after this switch separated by a space - e.g.:" + System.lineSeparator() + "-set \"KEY1=value1\" \"KEY2=value2\"")
                                .build()
                )
                .addOption(
                        Option.builder(DELAY_OPTION)
                                .longOpt("delay")
                                .hasArg()
                                .argName("seconds")
                                .desc("Delay execution for the specified amount of seconds.")
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

        if (commandLine.hasOption(HELP_OPTION)) {
            printHelp(options);
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

        final List<String> arguments = new ArrayList<>();

        // Never execute auto.sql from home dir.
        arguments.add("--noAutoFile");

        // add auto commit option
        if (commandLine.hasOption(AUTO_COMMIT_OPTION)) {
            arguments.add("--autoCommit");
        }

        // add continue on error option
        if (commandLine.hasOption(CONTINUE_ON_ERROR_OPTION)) {
            arguments.add("--continueOnErr");
        }

        // add debug option
        if (commandLine.hasOption(DEBUG_OPTION)) {
            arguments.add("--debug");
        }

        // add sql option
        if (commandLine.hasOption(SQL_OPTION)) {
            arguments.add("--sql");
            arguments.add(StringUtils.trimToEmpty(commandLine.getOptionValue(SQL_OPTION)));
        }

        // add set option
        if (commandLine.hasOption(SET_OPTION)) {
            for (String option : commandLine.getOptionValues(SET_OPTION)) {
                option = StringUtils.trimToNull(option);
                if (option == null) continue;
                arguments.add("-p");
                arguments.add(option);
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
                System.exit(1);
                return;
            }
        }
        arguments.add("--rcFile");
        arguments.add(rcFile.getAbsolutePath());

        // get list of available connection id's
        final Collection<String> urlIds;
        try {
            urlIds = ManagerUtils.getUrlIds(rcFile);
        } catch (IOException ex) {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("The configuration file is not readable!");
            System.err.println(ex.getLocalizedMessage());
            System.exit(1);
            return;
        }
        if (urlIds.isEmpty()) {
            System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
            System.err.println("The configuration file does not contain any connections!");
            System.exit(1);
            return;
        }

        // detect connection id
        final String urlId;
        if (commandLine.hasOption(ID_OPTION)) {
            // use the connection ID provided from the command line
            urlId = StringUtils.trimToNull(commandLine.getOptionValue(ID_OPTION));
            if (urlId == null) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("The provided connection ID is invalid!");
                System.exit(1);
                return;
            } else if (!urlIds.contains(urlId)) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("The configuration file does not contain a connection with urlid '" + urlId + "'!");
                System.exit(1);
                return;
            }
        } else {
            // use the first configured connection ID
            urlId = urlIds.iterator().next();
        }
        arguments.add(urlId);

        // add other arguments
        for (String arg : commandLine.getArgList()) {
            File f = new File(arg);
            if (!f.isFile()) {
                System.err.println("The provided sql file '" + arg + "' was not found!");
                System.exit(1);
            }
            arguments.add(f.getAbsolutePath());
        }

        LOGGER.debug("Launching SqlTool with '{}'...",
                StringUtils.join(arguments, StringUtils.SPACE));
        SqlTool.main(arguments.toArray(new String[]{}));
    }

    /**
     * Print usage information to System.out.
     *
     * @param options command line options
     */
    private static void printHelp(Options options) {
        final String commandLine;
        if (SystemUtils.IS_OS_WINDOWS)
            commandLine = "ManagerConsole.bat / ManagerConsole.exe";
        else
            commandLine = "ManagerConsole.sh";

        System.out.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
        new HelpFormatter().printHelp(
                commandLine + " [OPTION] ... [file1.sql] [file2.sql] ...",
                "Open a console application for database administration. You might use the following custom settings:" + System.lineSeparator() + StringUtils.SPACE,
                options,
                StringUtils.SPACE + System.lineSeparator() + "You might append one ore more SQL files after the options. These files are automatically executed. " + System.lineSeparator() + System.lineSeparator()
                        + "If no SQL files were appended and the -sql switch is not used, the application reads SQL statements from stdin." + System.lineSeparator() + System.lineSeparator()
                        + "If no SQL statements are provided (neither via -sql switch, appended sql files or stdin), the application will start in interactive mode." + System.lineSeparator() + System.lineSeparator()
                        + "See https://manual.openestate.org for more information."
        );
        System.out.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
    }
}
