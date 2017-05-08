/*
 * Copyright 2009-2017 OpenEstate.org.
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

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
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
import org.openestate.tool.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * SslGenerator.
 *
 * @since 1.0
 * @author Andreas Rudolph
 */
public class SslGenerator
{
  private final static Logger LOGGER = LoggerFactory.getLogger( SslGenerator.class );
  private static final I18n I18N = I18nFactory.getI18n( SslGenerator.class );
  private final static String ALIAS = "OpenEstate-ImmoServer";
  private final static String PROVIDER = "BC";
  private final static String KEY_ALGORITHM = "RSA";
  private final static int KEY_LENGTH = 4096;
  private final static String SIGNATURE_ALGORITHM = "SHA256withRSA";

  private SslGenerator()
  {
  }

  private static ContentSigner createSigner( PrivateKey privateKey )
  {
    try
    {
      AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find( SIGNATURE_ALGORITHM );
      AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find( sigAlgId );

      return new BcRSAContentSignerBuilder( sigAlgId, digAlgId )
        .build( PrivateKeyFactory.createKey( privateKey.getEncoded() ) );
    }
    catch (Exception e)
    {
      throw new RuntimeException( "Could not create content signer.", e );
    }
  }

  public static void main( String[] args )
  {
    final Console console = System.console();
    final String line = StringUtils.repeat( "-", 75 );

    console.writer().println( line );
    console.writer().println( I18N.tr( "Generate RSA keypair and certificate for SSL encryption." ) );
    console.writer().println( line );
    console.writer().println( StringUtils.EMPTY );

    // register bouncy castle provider
    Security.addProvider( new BouncyCastleProvider() );

    // get common name
    String commonName = (args.length>0)? StringUtils.trimToNull( args[0] ): null;
    while (commonName==null)
    {
      //LOGGER.error( "No common name was specified as first command line argument!" );
      //System.exit( 1 );
      //return;

      console.writer().print( I18N.tr( "Enter the ip-address / hostname of this server:" ) + StringUtils.SPACE );
      console.writer().flush();
      commonName = StringUtils.trimToNull( console.readLine() );
    }

    // get password
    char[] password = (args.length>1)? StringUtils.trimToEmpty( args[1] ).toCharArray(): null;
    if (password==null)
    {
      //LOGGER.error( "No password was specified as second command line argument!" );
      //System.exit( 1 );
      //return;

      while (password==null)
      {
        console.writer().print( I18N.tr( "Enter the password to access the keystore:" ) + StringUtils.SPACE );
        console.writer().flush();
        password = console.readPassword();
      }

      console.writer().print( I18N.tr( "Enter the password to access the keystore again:" ) + StringUtils.SPACE );
      console.writer().flush();
      char[] password2 = console.readPassword();
      if (!StringUtils.equals( String.valueOf( password ), String.valueOf( password2 ) ))
      {
        console.writer().println( I18N.tr( "Error!" ) );
        console.writer().println( I18N.tr( "The provided passwords do not match." ) );
        System.exit( 1 );
      }
    }

    console.writer().println( StringUtils.EMPTY );
    console.writer().println( line );
    console.writer().println( I18N.tr( "Creating files for SSL encryption..." ) );
    console.writer().println( line );
    console.writer().println( StringUtils.EMPTY );

    final File sslDir = new File( new File( "etc" ), "ssl" );
    if (!sslDir.exists() && !sslDir.mkdirs())
    {
      LOGGER.error( "Can't create ssl directory at '" + sslDir.getAbsolutePath() + "'!" );
      System.exit( 1 );
      return;
    }

    OutputStream output = null;
    PemWriter pemWriter = null;

    // create random number generator
    final SecureRandom random = new SecureRandom();

    // create key generator
    final KeyPairGenerator keyGen;
    try
    {
      keyGen = KeyPairGenerator.getInstance( KEY_ALGORITHM, PROVIDER );
      keyGen.initialize( KEY_LENGTH, random );
    }
    catch (Exception ex)
    {
      LOGGER.error( "Can't initialize key generator!" );
      LOGGER.error( "> " + ex.getLocalizedMessage(), ex );
      System.exit( 1 );
      return;
    }

    // generate a keypair
    final KeyPair pair = keyGen.generateKeyPair();

    // export private key
    final PrivateKey priv = pair.getPrivate();
    try
    {
      File f = new File( sslDir, "private.key" );
      FileUtils.deleteQuietly( f );
      console.writer().println( I18N.tr( "Writing private key to {0}.", "'" + f.getAbsolutePath() + "'" ) );
      pemWriter = new PemWriter( new FileWriterWithEncoding( f, "UTF-8" ) );
      pemWriter.writeObject( new PemObject( "OpenEstate-ImmoServer / Private Key", priv.getEncoded() ) );
      pemWriter.flush();
    }
    catch (Exception ex)
    {
      LOGGER.error( "Can't export private key!" );
      LOGGER.error( "> " + ex.getLocalizedMessage(), ex );
      System.exit( 1 );
      return;
    }
    finally
    {
      IOUtils.closeQuietly( pemWriter );
    }

    // export public key
    final PublicKey pub = pair.getPublic();
    try
    {
      File f = new File( sslDir, "public.key" );
      FileUtils.deleteQuietly( f );
      console.writer().println( I18N.tr( "Writing public key to {0}.", "'" + f.getAbsolutePath() + "'" ) );
      pemWriter = new PemWriter( new FileWriterWithEncoding( f, "UTF-8" ) );
      pemWriter.writeObject( new PemObject( "OpenEstate-ImmoServer / Public Key", pub.getEncoded() ) );
      pemWriter.flush();
    }
    catch (Exception ex)
    {
      LOGGER.error( "Can't export public key!" );
      LOGGER.error( "> " + ex.getLocalizedMessage(), ex );
      System.exit( 1 );
      return;
    }
    finally
    {
      IOUtils.closeQuietly( pemWriter );
    }

    // generate certificate
    final X509Certificate cert;
    try
    {
      Date startDate = new Date();
      Date expiryDate = DateUtils.addYears( startDate, 10 );
      X500Name subject = new X500Name( "CN=" + commonName );
      SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance( pub.getEncoded() );
      BigInteger serial = BigInteger.valueOf( random.nextLong() ).abs();

      //X509v1CertificateBuilder builder = new X509v1CertificateBuilder( subject, serial, startDate, expiryDate, subject, publicKeyInfo );
      //X509CertificateHolder holder = builder.build( createSigner( priv ) );
      //cert = new JcaX509CertificateConverter().getCertificate( holder );

      X509v3CertificateBuilder builder = new X509v3CertificateBuilder( subject, serial, startDate, expiryDate, subject, publicKeyInfo );

      X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(
        new BcDigestCalculatorProvider().get( new AlgorithmIdentifier( OIWObjectIdentifiers.idSHA1 ) ) );
      builder.addExtension( Extension.subjectKeyIdentifier, false, x509ExtensionUtils.createSubjectKeyIdentifier( publicKeyInfo ) );

      X509CertificateHolder holder = builder.build( createSigner( priv ) );
      cert = new JcaX509CertificateConverter().getCertificate( holder );

      // export certificate
      File f = new File( sslDir, "private.crt" );
      FileUtils.deleteQuietly( f );
      console.writer().println( I18N.tr( "Writing certificate to {0}.", "'" + f.getAbsolutePath() + "'" ) );
      pemWriter = new PemWriter( new FileWriterWithEncoding( f, "UTF-8" ) );
      pemWriter.writeObject( new PemObject( "OpenEstate-ImmoServer / Certificate", cert.getEncoded() ) );
      pemWriter.flush();
    }
    catch (Exception ex)
    {
      LOGGER.error( "Can't create certificate!" );
      LOGGER.error( "> " + ex.getLocalizedMessage(), ex );
      System.exit( 1 );
      return;
    }
    finally
    {
      IOUtils.closeQuietly( pemWriter );
    }

    // create keystore
    try
    {
      KeyStore store = KeyStore.getInstance( "jks" );
      store.load( null, password );
      store.setKeyEntry( ALIAS, priv, password, new Certificate[]{cert} );

      File f = new File( sslDir, "keystore.jks" );
      FileUtils.deleteQuietly( f );
      console.writer().println( I18N.tr( "Writing keystore to {0}.", "'" + f.getAbsolutePath() + "'" ) );
      output = new FileOutputStream( f );
      store.store( output, password );
      output.flush();
    }
    catch (Exception ex)
    {
      LOGGER.error( "Can't create keystore!" );
      LOGGER.error( "> " + ex.getLocalizedMessage(), ex );
      System.exit( 1 );
      return;
    }
    finally
    {
      IOUtils.closeQuietly( output );
    }

    console.writer().println( StringUtils.EMPTY );
    console.writer().println( line );
    console.writer().println( I18N.tr( "SSL encryption was successfully prepared!" ) );
    console.writer().println( line );
    console.writer().println( StringUtils.EMPTY );
    console.writer().println( I18N.tr( "Follow these steps in order to enable SSL encryption." ) );
    console.writer().println( StringUtils.EMPTY );
    console.writer().println( "(1) " +  I18N.tr( "Open the following configuration file with a text editor:" ) );
    console.writer().println( StringUtils.EMPTY );
    console.writer().println( new File( "etc", "server.properies" ).getAbsolutePath() );
    console.writer().println( StringUtils.EMPTY );
    console.writer().println( "(2) " +  I18N.tr( "Change the following values in the configuration file:" ) );
    console.writer().println( StringUtils.EMPTY );
    console.writer().println( "server.tls=true" );
    console.writer().println( "system.javax.net.ssl.keyStore=./etc/ssl/keystore.jks" );
    console.writer().println( "system.javax.net.ssl.keyStorePassword=" + String.valueOf( password ) );
    console.writer().println( StringUtils.EMPTY );
    console.writer().println( "(3) " +  I18N.tr( "Restart {0}.", Server.TITLE ) );
    console.writer().println( StringUtils.EMPTY );
    console.writer().println( line );
    console.writer().println( StringUtils.EMPTY );
  }
}