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
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * ServerProperties.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
public class ServerProperties extends org.hsqldb.server.ServerProperties {
    private final static Logger LOGGER = LoggerFactory.getLogger(ServerProperties.class);
    private static final I18n I18N = I18nFactory.getI18n(ServerProperties.class);

    public ServerProperties(int protocol, File file) throws IOException {
        super(protocol, file);
    }

    public static ServerProperties create(int protocol, InputStream props) throws IOException {
        File tempFile = File.createTempFile("openestate-immoserver-", ".properties");
        try {
            FileUtils.copyInputStreamToFile(props, tempFile);
            return new ServerProperties(protocol, tempFile);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }
}