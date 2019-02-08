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
import org.hsqldb.util.DatabaseManagerSwing;
import org.openestate.tool.server.ServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Graphical tool for management of a HSQLDB database.
 * <p>
 * This application basically starts {@link DatabaseManagerSwing} provided by HSQLDB.
 * It uses custom command line arguments for a better integration into the server environment.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
public class ManagerTool extends DatabaseManagerSwing {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(ManagerTool.class);
    private static final String HELP_OPTION = "help";
    private static final String CONF_OPTION = "conf";
    private static final String ID_OPTION = "id";
    private static final String SCRIPT_OPTION = "script";

    static {
        ServerUtils.init();
        LOGGER = LoggerFactory.getLogger(ManagerTool.class);
    }

    /**
     * Start graphical application for database management.
     *
     * @param args command line arguments
     */
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {

        // Options provided by DatabaseManagerSwing
        //
        // Usage: java DatabaseManagerSwing [--options]
        // where options include:
        // --help                show this message
        // --driver <classname>  jdbc driver class
        // --url <name>          jdbc url
        // --user <name>         username used for connection
        // --password <password> password for this user
        // --urlid <urlid>       use url/user/password/driver in rc file
        // --rcfile <file>       (defaults to 'dbmanager.rc' in home dir)
        // --dir <path>          default directory
        // --script <file>       reads from script file
        // --noexit              do not call system.exit()

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
                        Option.builder(SCRIPT_OPTION)
                                .longOpt("script")
                                .hasArg()
                                .argName("file")
                                .desc("SQL file to load on startup.")
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

        final List<String> arguments = new ArrayList<>();

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
        arguments.add("--rcfile");
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
        arguments.add("--urlid");
        arguments.add(urlId);

        // detect script file
        if (commandLine.hasOption(SCRIPT_OPTION)) {
            File scriptFile = new File(StringUtils.trimToEmpty(commandLine.getOptionValue(SCRIPT_OPTION)));
            if (!scriptFile.isFile()) {
                System.err.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
                System.err.println("The provided script file was not found!");
                System.exit(1);
                return;
            }
            arguments.add("--script");
            arguments.add(scriptFile.getAbsolutePath());
        }

        LOGGER.debug("Launching DatabaseManagerSwing with '{}'...",
                StringUtils.join(arguments, StringUtils.SPACE));
        DatabaseManagerSwing.main(arguments.toArray(new String[]{}));
    }

    /**
     * Print usage information to System.out.
     *
     * @param options command line options
     */
    private static void printHelp(Options options) {
        final String commandLine;
        if (SystemUtils.IS_OS_WINDOWS)
            commandLine = "ManagerTool.bat / ManagerTool.exe";
        else
            commandLine = "ManagerTool.sh";

        System.out.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
        new HelpFormatter().printHelp(
                commandLine + " [OPTION]...",
                "Open a graphical application for database administration. You might use the following custom settings:" + System.lineSeparator() + StringUtils.SPACE,
                options,
                StringUtils.SPACE + System.lineSeparator() + "See https://manual.openestate.org for more information."
        );
        System.out.println(StringUtils.repeat('-', HelpFormatter.DEFAULT_WIDTH));
    }
}
