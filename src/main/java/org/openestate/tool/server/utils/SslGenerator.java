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
package org.openestate.tool.server.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.openestate.tool.server.ServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Generate a key pair and certificate for SSL encrypted connections.
 *
 * @author Andreas Rudolph
 * @since 1.0
 */
public class SslGenerator {
    @SuppressWarnings("unused")
    private static final Logger LOGGER;
    @SuppressWarnings("unused")
    private static final I18n I18N = I18nFactory.getI18n(SslGenerator.class);
    private static final String ALIAS = ServerUtils.TITLE;
    private static final String PROVIDER = "BC";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_LENGTH = 4096;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    static {
        ServerUtils.init();

        // Create the logger instance after initialization. This makes sure, that logging environment is properly
        // configured before the logger is actually created.
        LOGGER = LoggerFactory.getLogger(SslGenerator.class);
    }

    private SslGenerator() {
        super();
    }

    private static ContentSigner createSigner(PrivateKey privateKey) {
        try {
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(SIGNATURE_ALGORITHM);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            return new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                    .build(PrivateKeyFactory.createKey(privateKey.getEncoded()));
        } catch (Exception ex) {
            throw new RuntimeException("Can't create content signer.", ex);
        }
    }

    /**
     * Start SSL initialization.
     *
     * @param args command line arguments,
     *             the first argument might contain the common name,
     *             the second argument might contain the keystore password
     */
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        final Console console = System.console();
        final String line = StringUtils.repeat("-", 75);

        console.writer().println(StringUtils.EMPTY);
        console.writer().println(line);
        console.writer().println(I18N.tr("Generate RSA key pair and certificate for SSL encryption."));
        console.writer().println(line);
        console.writer().println(StringUtils.EMPTY);

        // register bouncy castle provider
        Security.addProvider(new BouncyCastleProvider());

        // get common name
        String commonName = (args.length > 0) ? StringUtils.trimToNull(args[0]) : null;
        while (commonName == null) {
            //LOGGER.error( "No common name was specified as first command line argument!" );
            //System.exit( 1 );
            //return;

            console.writer().print(I18N.tr("Enter the ip-address / hostname of this server:") + StringUtils.SPACE);
            console.writer().flush();
            commonName = StringUtils.trimToNull(console.readLine());
        }

        // get password
        char[] password = (args.length > 1) ? StringUtils.trimToEmpty(args[1]).toCharArray() : null;
        if (password == null) {
            //LOGGER.error( "No password was specified as second command line argument!" );
            //System.exit( 1 );
            //return;

            while (password == null) {
                console.writer().print(I18N.tr("Enter the password to access the keystore:") + StringUtils.SPACE);
                console.writer().flush();
                password = console.readPassword();
            }

            console.writer().print(I18N.tr("Enter the password to access the keystore again:") + StringUtils.SPACE);
            console.writer().flush();
            char[] password2 = console.readPassword();
            if (!StringUtils.equals(String.valueOf(password), String.valueOf(password2))) {
                console.writer().println(StringUtils.capitalize(I18N.tr("error")) + "!");
                console.writer().println(I18N.tr("The provided passwords do not match."));
                System.exit(1);
            }
        }

        console.writer().println(StringUtils.EMPTY);
        console.writer().println(line);
        console.writer().println(I18N.tr("Creating files for SSL encryption..."));
        console.writer().println(line);
        console.writer().println(StringUtils.EMPTY);

        final File etcDir;
        final File sslDir;
        try {
            etcDir = ServerUtils.getEtcDir();
            sslDir = new File(etcDir, "ssl");
        } catch (IOException ex) {
            LOGGER.error("Can't locate ssl directory!");
            System.exit(1);
            return;
        }
        if (!sslDir.exists() && !sslDir.mkdirs()) {
            LOGGER.error("Can't create ssl directory at '" + sslDir.getAbsolutePath() + "'!");
            System.exit(1);
            return;
        }

        // create random number generator
        final SecureRandom random = new SecureRandom();

        // create key generator
        final KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM, PROVIDER);
            keyGen.initialize(KEY_LENGTH, random);
        } catch (Exception ex) {
            LOGGER.error("Can't initialize key generator!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            System.exit(1);
            return;
        }

        // generate a key pair
        final KeyPair pair = keyGen.generateKeyPair();

        // export private key
        final PrivateKey privateKey = pair.getPrivate();
        final File privateKeyFile = new File(sslDir, "private.key");
        FileUtils.deleteQuietly(privateKeyFile);
        console.writer().println(I18N.tr("Writing private key to {0}.", "'" + privateKeyFile.getAbsolutePath() + "'"));
        try (PemWriter pemWriter = new PemWriter(new FileWriterWithEncoding(privateKeyFile, "UTF-8"))) {
            pemWriter.writeObject(new PemObject(
                    ALIAS + " / Private Key",
                    privateKey.getEncoded()));
            pemWriter.flush();
        } catch (Exception ex) {
            LOGGER.error("Can't export private key!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            System.exit(1);
            return;
        }

        // export public key
        final PublicKey publicKey = pair.getPublic();
        final File publicKeyFile = new File(sslDir, "public.key");
        FileUtils.deleteQuietly(publicKeyFile);
        console.writer().println(I18N.tr("Writing public key to {0}.", "'" + publicKeyFile.getAbsolutePath() + "'"));
        try (PemWriter pemWriter = new PemWriter(new FileWriterWithEncoding(publicKeyFile, "UTF-8"))) {
            pemWriter.writeObject(new PemObject(
                    ALIAS + " / Public Key",
                    publicKey.getEncoded()));
            pemWriter.flush();
        } catch (Exception ex) {
            LOGGER.error("Can't export public key!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            System.exit(1);
            return;
        }

        // generate certificate
        final X509Certificate cert;
        try {
            Date startDate = new Date();
            Date expiryDate = DateUtils.addYears(startDate, 10);
            X500Name subject = new X500Name("CN=" + commonName);
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
            BigInteger serial = BigInteger.valueOf(random.nextLong()).abs();

            //X509v1CertificateBuilder builder = new X509v1CertificateBuilder( subject, serial, startDate, expiryDate, subject, publicKeyInfo );
            //X509CertificateHolder holder = builder.build( createSigner( privateKey ) );
            //cert = new JcaX509CertificateConverter().getCertificate( holder );

            X509v3CertificateBuilder builder = new X509v3CertificateBuilder(subject, serial, startDate, expiryDate, subject, publicKeyInfo);

            X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(
                    new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1)));
            builder.addExtension(Extension.subjectKeyIdentifier, false, x509ExtensionUtils.createSubjectKeyIdentifier(publicKeyInfo));

            X509CertificateHolder holder = builder.build(createSigner(privateKey));
            cert = new JcaX509CertificateConverter().getCertificate(holder);

            // export certificate
            File f = new File(sslDir, "private.crt");
            FileUtils.deleteQuietly(f);
            console.writer().println(I18N.tr("Writing certificate to {0}.", "'" + f.getAbsolutePath() + "'"));
            try (PemWriter pemWriter = new PemWriter(new FileWriterWithEncoding(f, "UTF-8"))) {
                pemWriter.writeObject(new PemObject(
                        ALIAS + " / Certificate",
                        cert.getEncoded()));
                pemWriter.flush();
            }
        } catch (Exception ex) {
            LOGGER.error("Can't create certificate!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            System.exit(1);
            return;
        }

        // create keystore
        try {
            KeyStore store = KeyStore.getInstance("jks");
            store.load(null, password);
            store.setKeyEntry(ALIAS, privateKey, password, new Certificate[]{cert});

            File f = new File(sslDir, "keystore.jks");
            FileUtils.deleteQuietly(f);
            console.writer().println(I18N.tr("Writing keystore to {0}.", "'" + f.getAbsolutePath() + "'"));

            try (OutputStream output = new FileOutputStream(f)) {
                store.store(output, password);
                output.flush();
            }
        } catch (Exception ex) {
            LOGGER.error("Can't create keystore!");
            LOGGER.error("> " + ex.getLocalizedMessage(), ex);
            System.exit(1);
            return;
        }

        console.writer().println(StringUtils.EMPTY);
        console.writer().println(line);
        console.writer().println(I18N.tr("SSL encryption was successfully prepared!"));
        console.writer().println(line);
        console.writer().println(StringUtils.EMPTY);
        console.writer().println(I18N.tr("Follow these steps in order to enable SSL encryption."));
        console.writer().println(StringUtils.EMPTY);
        console.writer().println("(1) " + I18N.tr("Open the following configuration file with a text editor:"));
        console.writer().println(StringUtils.EMPTY);
        console.writer().println(new File(etcDir, "server.properties").getAbsolutePath());
        console.writer().println(StringUtils.EMPTY);
        console.writer().println("(2) " + I18N.tr("Change the following values in the configuration file:"));
        console.writer().println(StringUtils.EMPTY);
        console.writer().println("server.tls=true");
        //console.writer().println("system.javax.net.ssl.keyStore=./etc/ssl/keystore.jks");
        console.writer().println("system.javax.net.ssl.keyStorePassword=" + String.valueOf(password));
        console.writer().println(StringUtils.EMPTY);
        console.writer().println("(3) " + I18N.tr("Restart {0}.", ServerUtils.TITLE));
        console.writer().println(StringUtils.EMPTY);
        console.writer().println(line);
        console.writer().println(StringUtils.EMPTY);

        console.writer().println(I18N.tr("Press ENTER to exit this application."));
        try {
            console.readLine();
        } catch (Exception ignored) {
        }
    }
}
