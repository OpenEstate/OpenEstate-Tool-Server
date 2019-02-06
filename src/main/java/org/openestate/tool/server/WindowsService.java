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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Handler for Windows services used by <a href="https://commons.apache.org/daemon/">commons-daemon</a>.
 *
 * @author Andreas Rudolph
 * @see <a href="https://stackoverflow.com/a/32352529">inspired by this example</a>
 * @since 1.0
 */
@SuppressWarnings("unused")
public class WindowsService {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(WindowsService.class);

    static {
        ServerUtils.init();
        LOGGER = LoggerFactory.getLogger(WindowsService.class);
    }

    /**
     * Invoke service start.
     *
     * @param args command line arguments
     */
    public static void start(final String[] args) {
        LOGGER.info("Starting Windows service...");
        if (!ArrayUtils.isEmpty(args))
            LOGGER.debug("with {}", StringUtils.join(args, ", "));

        try {
            Server.main(args);
        } catch (Exception e) {
            LOGGER.error("Windows Service was terminated externally.", e);
        }
    }

    /**
     * Invoke service stop
     *
     * @param args command line arguments
     */
    public static void stop(final String[] args) {
        LOGGER.info("Stopping Windows service...");
        if (!ArrayUtils.isEmpty(args)) {
            LOGGER.debug("with {}", StringUtils.join(args, ", "));
        }

        synchronized (WindowsService.class) {
            Server server = Server.get();
            if (server != null) {
                server.shutdown();
            }
        }
    }
}
