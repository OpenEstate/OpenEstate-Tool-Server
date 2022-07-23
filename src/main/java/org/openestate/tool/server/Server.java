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
package org.openestate.tool.server;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.hsqldb.server.ServerConfiguration;
import org.hsqldb.server.ServerConstants;
import org.openestate.tool.server.utils.MigrationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Implementation of OpenEstate-ImmoServer.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
public class Server extends org.hsqldb.Server {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(Server.class);

    /**
     * Current server instance.
     */
    private static Server server = null;

    /**
     * System tray icon used by the server instance.
     */
    private static TrayIcon systemTrayIcon = null;

    /**
     * This variable is set to true,
     * if the shutdown hook for this server was triggered.
     */
    private static boolean shutdownHookTriggered = false;

    static {
        ServerUtils.init();

        // Create the logger instance after initialization. This makes sure, that logging environment is properly
        // configured before the logger is actually created.
        LOGGER = LoggerFactory.getLogger(Server.class);
    }

    /**
     * Create server instance.
     */
    protected Server() {
        super();
    }

    /**
     * Get current server instance.
     *
     * @return server instance
     */
    public static Server get() {
        return server;
    }

    /**
     * Load system tray icon for the server instance.
     */
    private static void initSystemTray() {
        //LOGGER.debug( "init system tray" );
        if (!ServerUtils.isSystemTrayEnabled()) {
            //LOGGER.debug( "The system tray is disabled." );
            return;
        }
        if (!SystemTray.isSupported()) {
            LOGGER.info("The operating system does not support system tray.");
            return;
        }

        final Image trayIconImage;
        try {
            trayIconImage = ImageIO.read(ServerUtils.getResource("ImmoServer.png"));
        } catch (Exception ex) {
            LOGGER.error("Can't load icon for system tray!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            return;
        }

        final PopupMenu popup = new PopupMenu();
        final MenuItem stopItem = new MenuItem(I18N.tr("shutdown {0}", ServerUtils.TITLE));
        stopItem.addActionListener(e -> {
            stopItem.setEnabled(false);
            server.shutdown();
        });
        popup.add(stopItem);

        systemTrayIcon = new TrayIcon(trayIconImage, ServerUtils.TITLE, popup);
        systemTrayIcon.setImageAutoSize(true);

        final SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(systemTrayIcon);
        } catch (AWTException ex) {
            LOGGER.error("Can't add icon to system tray!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Create and start the server instance.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        //org.hsqldb.Server.main( args );

        if (!SystemUtils.isJavaAwtHeadless()) {
            initSystemTray();
        }

        // load server configuration
        final ServerProperties serverProperties;
        try {
            final File propertiesFile = new File(ServerUtils.getEtcDir(), "server.properties");
            if (!propertiesFile.isFile()) {
                throw new IOException("Can't find server configuration at '" + propertiesFile.getPath() + "'!");
            }
            serverProperties = new ServerProperties(ServerConstants.SC_PROTOCOL_HSQL, propertiesFile);
        } catch (IOException ex) {
            LOGGER.error("Can't load server configuration!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            System.exit(1);
            return;
        }

        ServerConfiguration.translateDefaultDatabaseProperty(serverProperties);

        // Standard behaviour when started from the command line
        // is to halt the VM when the server shuts down. This may, of
        // course, be overridden by whatever, if any, security policy
        // is in place.
        ServerConfiguration.translateDefaultNoSystemExitProperty(serverProperties);
        ServerConfiguration.translateAddressProperty(serverProperties);

        // create the database server
        server = new Server();
        try {
            server.setProperties(serverProperties);
        } catch (Exception ex) {
            server.printError("Failed to set server properties!");
            server.printStackTrace(ex);
            return;
        }

        // init databases before the server is started
        for (int i = 0; ; i++) {
            String path = server.getDatabasePath(i, true);
            if (path == null) break;
            if (!path.startsWith("file:")) continue;

            File dbDir = new File(FilenameUtils.separatorsToSystem(StringUtils.substringAfter(path, "file:"))).getParentFile();
            String dbName = StringUtils.substringAfterLast(path, "/");
            LOGGER.info("Initializing database '" + dbDir.getAbsolutePath() + "'.");
            try {
                MigrationUtils.migrateFromOldDatabase(dbDir, dbName);
            } catch (Exception ex) {
                LOGGER.warn("Can't migrate database at '" + dbDir.getAbsolutePath() + "'!");
                LOGGER.warn("> " + ex.getLocalizedMessage(), ex);
            }
        }

        // properly shutdown the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (shutdownHookTriggered) return;
            shutdownHookTriggered = true;
            if (Server.server == null) return;

            final int state = Server.server.getState();
            switch (state) {
                case ServerConstants.SERVER_STATE_ONLINE:
                case ServerConstants.SERVER_STATE_OPENING:
                case ServerConstants.SERVER_STATE_CLOSING:
                    LOGGER.info("Starting shutdown sequence.");
                    Server.server.shutdown();
                    Server.server = null;
            }
        }));

        // start the database server
        server.start();
    }

    @Override
    public boolean isNoSystemExit() {
        return shutdownHookTriggered || super.isNoSystemExit();
    }

    @Override
    protected void print(String msg) {
        //super.print( msg );
        LOGGER.info("[" + this.getServerId() + "]: " + msg);
    }

    @Override
    protected void printError(String msg) {
        //super.printError( msg );
        LOGGER.error(msg);
    }

    @Override
    protected void printStackTrace(Throwable t) {
        //super.printStackTrace( t );
        LOGGER.error(t.getLocalizedMessage(), t);
    }

    @Override
    protected synchronized void setState(int state) {
        //LOGGER.debug( "set server state: " + state );

        if (systemTrayIcon != null && this.getState() != state) {
            switch (state) {
                case ServerConstants.SERVER_STATE_ONLINE:
                    systemTrayIcon.displayMessage(
                            ServerUtils.TITLE,
                            I18N.tr("{0} is available for incoming connections.", ServerUtils.TITLE),
                            TrayIcon.MessageType.INFO);
                    break;

                case ServerConstants.SERVER_STATE_CLOSING:
                    systemTrayIcon.displayMessage(
                            ServerUtils.TITLE,
                            I18N.tr("{0} is shutting down.", ServerUtils.TITLE),
                            TrayIcon.MessageType.INFO);
                    break;

                case ServerConstants.SERVER_STATE_OPENING:
                    systemTrayIcon.displayMessage(
                            ServerUtils.TITLE,
                            I18N.tr("{0} is starting up.", ServerUtils.TITLE),
                            TrayIcon.MessageType.INFO);
                    break;

                case ServerConstants.SERVER_STATE_SHUTDOWN:
                    systemTrayIcon.displayMessage(
                            ServerUtils.TITLE,
                            I18N.tr("{0} has been closed and is not available anymore.", ServerUtils.TITLE),
                            TrayIcon.MessageType.INFO);
                    break;

                default:
                    break;
            }
        }

        super.setState(state);
    }
}
