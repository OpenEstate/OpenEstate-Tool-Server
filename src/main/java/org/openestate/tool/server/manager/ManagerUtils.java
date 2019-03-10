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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Helper methods for the management tools.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
@SuppressWarnings("WeakerAccess")
public class ManagerUtils {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerUtils.class);
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(ManagerUtils.class);

    private ManagerUtils() {
        super();
    }

    /**
     * Extract connection ID's from a rc file (like manager.conf).
     *
     * @param rcFile rc file to parse
     * @return list of configured connection ID's
     * @see <a href="http://www.hsqldb.org/doc/2.0/util-guide/sqltool-chapt.html#sqltool_auth-sect">Sample RC File</a>
     */
    public static Collection<String> getUrlIds(File rcFile) throws IOException {
        List<String> urlIds = new ArrayList<>();
        for (String line : FileUtils.readLines(rcFile, StandardCharsets.UTF_8)) {
            line = StringUtils.trimToNull(line);
            if (line == null || line.startsWith("#")) continue;
            if (!line.toLowerCase().startsWith("urlid ")) continue;

            String[] values = StringUtils.split(line, " ", 2);
            if (values.length == 2) urlIds.add(values[1]);
        }
        return Collections.unmodifiableList(urlIds);
    }
}
