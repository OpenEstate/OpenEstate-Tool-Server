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
package org.openestate.tool.server;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
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

/**
 * Implementation of OpenEstate-Server.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
@SuppressWarnings("WeakerAccess")
public class Server extends org.hsqldb.Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private static final I18n I18N = I18nFactory.getI18n(Server.class);
    public static final String TITLE = "OpenEstate-ImmoServer";
    private static final String SYSTEM_TRAY_PROPERTY = "openestate.server.systemTray";
    private static Server server = null;
    private static TrayIcon systemTrayIcon = null;

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
        if (!isSystemTrayEnabled()) {
            //LOGGER.debug( "The system tray is disabled." );
            return;
        }
        if (!SystemTray.isSupported()) {
            LOGGER.warn("The operating system does not support system tray.");
            return;
        }

        final Image trayIconImage;
        try {
            trayIconImage = ImageIO.read(Server.class.getResourceAsStream(
                    "/org/openestate/tool/server/resources/ImmoServer.png"));
        } catch (Exception ex) {
            LOGGER.error("Can't load icon for system tray!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            return;
        }

        final PopupMenu popup = new PopupMenu();
        final MenuItem stopItem = new MenuItem(I18N.tr("shutdown {0}", TITLE));
        stopItem.addActionListener(e -> {
            stopItem.setEnabled(false);
            server.stop();
        });
        popup.add(stopItem);

        systemTrayIcon = new TrayIcon(trayIconImage, TITLE, popup);
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
     * Test, if the system tray icon is usable / enabled.
     * @return true, if the system tray icon is usable / enabled
     */
    public static boolean isSystemTrayEnabled() {
        final String property = StringUtils.trimToNull(StringUtils.lowerCase(
                System.getProperty(SYSTEM_TRAY_PROPERTY, "false")));

        return "1".equals(property) || "true".equals(property);
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
        final ServerProperties props;
        try {
            InputStream propsStream = Server.class.getResourceAsStream("/server.properties");
            if (propsStream == null) {
                LOGGER.error("Can't find server configuration!");
                return;
            }
            props = ServerProperties.create(ServerConstants.SC_PROTOCOL_HSQL, propsStream);
        } catch (Exception ex) {
            LOGGER.error("Can't load server configuration!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            return;
        }

        ServerConfiguration.translateDefaultDatabaseProperty(props);

        // Standard behaviour when started from the command line
        // is to halt the VM when the server shuts down. This may, of
        // course, be overridden by whatever, if any, security policy
        // is in place.
        ServerConfiguration.translateDefaultNoSystemExitProperty(props);
        ServerConfiguration.translateAddressProperty(props);

        // create the database server
        server = new Server();
        try {
            server.setProperties(props);
        } catch (Exception ex) {
            server.printError("Failed to set properties!");
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

        // start the database server
        server.start();
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
                            TITLE, I18N.tr("{0} is available for incoming connections.", TITLE), TrayIcon.MessageType.INFO);
                    break;

                case ServerConstants.SERVER_STATE_CLOSING:
                    systemTrayIcon.displayMessage(
                            TITLE, I18N.tr("{0} is shutting down.", TITLE), TrayIcon.MessageType.INFO);
                    break;

                case ServerConstants.SERVER_STATE_OPENING:
                    systemTrayIcon.displayMessage(
                            TITLE, I18N.tr("{0} is starting up.", TITLE), TrayIcon.MessageType.INFO);
                    break;

                case ServerConstants.SERVER_STATE_SHUTDOWN:
                    systemTrayIcon.displayMessage(
                            TITLE, I18N.tr("{0} has been closed and is not available anymore.", TITLE), TrayIcon.MessageType.INFO);
                    break;

                default:
                    break;
            }
        }

        super.setState(state);
    }
}
