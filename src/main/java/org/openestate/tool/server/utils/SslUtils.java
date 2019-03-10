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
package org.openestate.tool.server.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for SSL connections.
 *
 * @author Andreas Rudolph
 */
public class SslUtils {
    @SuppressWarnings("unused")
    private final static Logger LOGGER = LoggerFactory.getLogger(SslUtils.class);
    private static SSLContext context;

    public static void installLooseSslSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new LooseTrustManager()}, null);
        SSLContext.setDefault(context);
        Security.setProperty(
                "ssl.SocketFactory.provider",
                LooseSslSocketFactory.class.getName());
    }

    public static class LooseSslSocketFactory extends SSLSocketFactory {
        @Override
        public String[] getDefaultCipherSuites() {
            return SslUtils.context.getSocketFactory().getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return SslUtils.context.getSocketFactory().getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return SslUtils.context.getSocketFactory().createSocket(s, host, port, autoClose);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return SslUtils.context.getSocketFactory().createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return SslUtils.context.getSocketFactory().createSocket(host, port, localHost, localPort);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return SslUtils.context.getSocketFactory().createSocket(host, port);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return SslUtils.context.getSocketFactory().createSocket(address, port, localAddress, localPort);
        }
    }

    private static class LooseTrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }
}
