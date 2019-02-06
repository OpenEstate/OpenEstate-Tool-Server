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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Helper methods for the server environment.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
@SuppressWarnings("WeakerAccess")
public class ServerUtils {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(ServerUtils.class);

    /**
     * Name of the server.
     */
    public static final String TITLE = "OpenEstate-ImmoServer";

    /**
     * Name of the system property, that contains the application name.
     */
    public static final String APP_PROPERTY = "openestate.server.app";

    /**
     * Name of the system property, that points to the etc directory of the server.
     */
    public static final String ETC_DIR_PROPERTY = "openestate.server.etcDir";

    /**
     * Name of the system property, that points to the log directory of the server.
     */
    public static final String LOG_DIR_PROPERTY = "openestate.server.logDir";

    /**
     * Name of the system property, that points to the var directory of the server.
     */
    public static final String VAR_DIR_PROPERTY = "openestate.server.varDir";

    /**
     * Name of the system property, that enables / disables the system tray icon.
     */
    public static final String SYSTEM_TRAY_PROPERTY = "openestate.server.systemTray";

    private static File ETC_DIR = null;
    private static File LOG_DIR = null;
    private static File VAR_DIR = null;
    private static boolean INITIALIZED = false;

    static {
        init();
        LOGGER = LoggerFactory.getLogger(ServerUtils.class);
    }

    private ServerUtils() {
        super();
    }

    /**
     * Get internal application name.
     *
     * @return internal application name
     */
    public static String getApplicationName() {
        String app = StringUtils.trimToNull(System.getProperty(APP_PROPERTY));
        if (app == null) {
            synchronized (ServerUtils.class) {
                app = "default";
                System.setProperty(APP_PROPERTY, app);
            }
        }
        return app;
    }

    /**
     * Get etc directory used by the server.
     *
     * @return etc directory
     */
    public static File getEtcDir() throws IOException {
        if (ETC_DIR != null) return ETC_DIR;

        synchronized (ServerUtils.class) {
            final String path = StringUtils.trimToNull(System.getProperty(ETC_DIR_PROPERTY));
            ETC_DIR = (path != null) ?
                    getCanonicalOrAbsoluteFile(path) :
                    getCanonicalOrAbsoluteFile("etc");

            if (!ETC_DIR.exists() && !ETC_DIR.mkdirs())
                throw new IOException("Can't create etc directory at '" + path + "'!");

            if (!ETC_DIR.isDirectory())
                throw new IOException("The etc directory at '" + path + "' is invalid!");

            // register etc directory as system property, if not already set
            if (path == null)
                System.setProperty(ETC_DIR_PROPERTY, ETC_DIR.getPath());

            return ETC_DIR;
        }
    }

    /**
     * Convert a file path to a canonical or absolute {@link File}.
     *
     * @param path file path to convert
     * @return canonical or absolute {@link File}
     */
    public static File getCanonicalOrAbsoluteFile(String path) {
        return getCanonicalOrAbsoluteFile(new File(path));
    }

    /**
     * Convert a {@link File} to a canonical or absolute {@link File}.
     *
     * @param file file to convert
     * @return canonical or absolute {@link File}
     */
    public static File getCanonicalOrAbsoluteFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            return file.getAbsoluteFile();
        }
    }

    /**
     * Get log directory used by the server.
     *
     * @return log directory
     */
    public static File getLogDir() throws IOException {
        if (LOG_DIR != null) return LOG_DIR;

        synchronized (ServerUtils.class) {
            final String path = StringUtils.trimToNull(System.getProperty(LOG_DIR_PROPERTY));
            LOG_DIR = (path != null) ?
                    getCanonicalOrAbsoluteFile(path) :
                    getCanonicalOrAbsoluteFile(new File(getVarDir(), "log"));

            if (!LOG_DIR.exists() && !LOG_DIR.mkdirs())
                throw new IOException("Can't create log directory at '" + path + "'!");

            if (!LOG_DIR.isDirectory())
                throw new IOException("The log directory at '" + path + "' is invalid!");

            // register log directory as system property, if not already set
            if (path == null)
                System.setProperty(LOG_DIR_PROPERTY, LOG_DIR.getPath());

            return LOG_DIR;
        }
    }

    public static InputStream getResource(String name) {
        return ServerUtils.class.getClassLoader().getResourceAsStream(
                "org/openestate/tool/server/resources/" + name);
    }

    /**
     * Get var directory used by the server.
     *
     * @return var directory
     */
    public static File getVarDir() throws IOException {
        if (VAR_DIR != null) return VAR_DIR;

        synchronized (ServerUtils.class) {
            final String path = StringUtils.trimToNull(System.getProperty(VAR_DIR_PROPERTY));
            VAR_DIR = (path != null) ?
                    getCanonicalOrAbsoluteFile(path) :
                    getCanonicalOrAbsoluteFile("var");

            if (!VAR_DIR.exists() && !VAR_DIR.mkdirs())
                throw new IOException("Can't create var directory at '" + path + "'!");

            if (!VAR_DIR.isDirectory())
                throw new IOException("The var directory at '" + path + "' is invalid!");

            // register var directory as system property, if not already set
            if (path == null)
                System.setProperty(VAR_DIR_PROPERTY, VAR_DIR.getPath());

            return VAR_DIR;
        }
    }

    /**
     * Initialize the server environment.
     */
    public static void init() {
        if (INITIALIZED) return;

        synchronized (ServerUtils.class) {
            try {
                System.out.println("Initializing " + getApplicationName() + " application...");

                // replace variables in system properties
                Enumeration e = System.getProperties().keys();
                while (e.hasMoreElements()) {
                    String key = e.nextElement().toString();
                    String value = System.getProperty(key);
                    if (value.contains("${")) {
                        System.setProperty(key, StringSubstitutor.replaceSystemProperties(value));
                    }
                }

                // init etc directory
                initEtc();

                // init logging
                initLogging();
            } finally {
                INITIALIZED = true;
            }
        }
    }

    /**
     * Initialize the etc directory.
     * <p>
     * Files from the default etc directory are copied to the configured etc directory,
     * if they are not already present.
     */
    private static void initEtc() {
        try {
            File etcDir = getEtcDir();
            File defaultEtcDir = getCanonicalOrAbsoluteFile("etc");

            // don't do anything, if the default etc directory is configured
            if (etcDir.getPath().equals(defaultEtcDir.getPath()))
                return;

            // copy files from default etc directory
            for (File defaultEtcFile : ObjectUtils.defaultIfNull(defaultEtcDir.listFiles(), new File[]{})) {
                File etcFile = new File(etcDir, defaultEtcFile.getName());
                if (etcFile.isFile()) continue;
                FileUtils.copyFile(defaultEtcFile, etcFile);
            }
        } catch (IOException ex) {
            System.out.println("ERROR: Can't init etc directory!");
            ex.printStackTrace(System.out);
            System.exit(1);
        }
    }

    /**
     * Initialize the logging environment.
     */
    private static void initLogging() {
        // init and check log directory
        try {
            if (!getLogDir().isDirectory())
                throw new IOException("Log directory does not exist!");
        } catch (IOException ex) {
            System.out.println("ERROR: Can't access log directory!");
            ex.printStackTrace(System.out);
            System.exit(1);
            return;
        }

        // get external log4j configuration file
        File log4jProperties;
        try {
            log4jProperties = new File(getEtcDir(), "log4j.properties");
        } catch (IOException ex) {
            System.out.println("ERROR: Can't access etc directory!");
            ex.printStackTrace(System.out);
            System.exit(1);
            return;
        }

        // fallback to default log4j configuration file
        if (!log4jProperties.isFile()) {
            log4jProperties = new File(new File("etc"), "log4j.properties");
        }

        // init logging from external log4j configuration file
        if (log4jProperties.isFile()) {
            try (InputStream input = new FileInputStream(log4jProperties)) {
                PropertyConfigurator.configure(input);
                return;
            } catch (IOException ex) {
                System.out.println("ERROR: Can't init logging from external configuration!");
                ex.printStackTrace(System.out);
            }
        }

        // fallback to internal log4j configuration file
        try (InputStream input = getResource("log4j.properties")) {
            PropertyConfigurator.configure(input);
        } catch (IOException ex) {
            System.out.println("ERROR: Can't init logging from internal configuration!");
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Test, if the system tray icon is usable / enabled.
     *
     * @return true, if the system tray icon is usable / enabled
     */
    public static boolean isSystemTrayEnabled() {
        final String property = StringUtils.trimToNull(StringUtils.lowerCase(
                System.getProperty(SYSTEM_TRAY_PROPERTY, "false")));

        return "1".equals(property) || "true".equals(property);
    }
}
