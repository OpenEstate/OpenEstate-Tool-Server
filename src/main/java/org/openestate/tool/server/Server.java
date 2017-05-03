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
package org.openestate.tool.server;

import org.hsqldb.lib.FileUtil;
import org.hsqldb.server.ServerConfiguration;
import org.hsqldb.server.ServerConstants;
import org.hsqldb.server.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of OpenEstate-Server.
 *
 * @since 1.0
 * @author Andreas Rudolph
 */
public class Server extends org.hsqldb.Server
{
  private final static Logger LOGGER = LoggerFactory.getLogger( Server.class );
  private static Server server = null;

  public Server()
  {
    super();
  }

  public static Server get()
  {
    return server;
  }

  public static void main( String[] args )
  {
    //org.hsqldb.Server.main( args );
    server = new Server();

    String propsPath = "server";
    String propsExtension = ".properties";

    propsPath = FileUtil.getFileUtil().canonicalOrAbsolutePath( propsPath );

    ServerProperties props = ServerConfiguration.getPropertiesFromFile(
      ServerConstants.SC_PROTOCOL_HSQL, propsPath, propsExtension);

    if (props==null)
    {
      server.printError( "Can't find server configuration!" );
      return;
    }

    ServerConfiguration.translateDefaultDatabaseProperty( props );

    // Standard behaviour when started from the command line
    // is to halt the VM when the server shuts down. This may, of
    // course, be overridden by whatever, if any, security policy
    // is in place.
    ServerConfiguration.translateDefaultNoSystemExitProperty( props );
    ServerConfiguration.translateAddressProperty( props );

    try
    {
      server.setProperties(props);
    }
    catch (Exception e)
    {
      server.printError( "Failed to set properties" );
      server.printStackTrace( e );
      return;
    }

    // now messages go to the channel specified in properties
    server.print( "Startup sequence initiated from main() method" );
    server.print( "Loaded properties from [" + propsPath + propsExtension + "]" );
    server.start();
  }

  @Override
  protected void print( String msg )
  {
    super.print( msg );
  }

  @Override
  protected void printError( String msg )
  {
    super.printError( msg );
  }

  @Override
  protected void printStackTrace( Throwable t )
  {
    super.printStackTrace( t );
  }

  @Override
  protected void printWithThread( String msg )
  {
    super.printWithThread( msg );
  }
}