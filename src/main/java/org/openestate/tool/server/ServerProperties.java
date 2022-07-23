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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Properties for the HSQLDB Server.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
@SuppressWarnings("WeakerAccess")
public class ServerProperties extends org.hsqldb.server.ServerProperties {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerProperties.class);
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(ServerProperties.class);

    /**
     * Create server properties instance.
     *
     * @param protocol type of server ({@link org.hsqldb.server.ServerConstants#SC_PROTOCOL_HTTP}, {@link org.hsqldb.server.ServerConstants#SC_PROTOCOL_HSQL} or {@link org.hsqldb.server.ServerConstants#SC_PROTOCOL_BER})
     * @param file     server properties file
     * @throws IOException if properties are not readable
     */
    public ServerProperties(int protocol, File file) throws IOException {
        super(protocol, file);
        this.init();
    }

    /**
     * Create server properties instance from an {@link InputStream}.
     *
     * @param protocol type of server ({@link org.hsqldb.server.ServerConstants#SC_PROTOCOL_HTTP}, {@link org.hsqldb.server.ServerConstants#SC_PROTOCOL_HSQL} or {@link org.hsqldb.server.ServerConstants#SC_PROTOCOL_BER})
     * @param props    server properties
     * @return ServerProperties created server properties
     * @throws IOException if properties are not readable
     */
    public static ServerProperties create(int protocol, InputStream props) throws IOException {
        File tempFile = File.createTempFile("openestate-immoserver-", ".properties");
        try {
            FileUtils.copyInputStreamToFile(props, tempFile);
            return new ServerProperties(protocol, tempFile);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    /**
     * Replace system properties in server configuration.
     */
    private void init() {
        Enumeration<?> e = this.stringProps.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = this.stringProps.getProperty(key);
            if (value.contains("${")) {
                this.stringProps.setProperty(key, StringSubstitutor.replaceSystemProperties(value));
            }
        }
    }
}
