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

import java.io.InputStream;
import org.hsqldb.server.ServerConfiguration;
import org.hsqldb.server.ServerConstants;
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

    final ServerProperties props;
    try
    {
      InputStream propsStream = Server.class.getResourceAsStream( "/server.properties" );
      if (propsStream==null)
      {
        LOGGER.error( "Can't find server configuration!" );
        return;
      }
      props = ServerProperties.create( ServerConstants.SC_PROTOCOL_HSQL, propsStream );
    }
    catch (Exception ex)
    {
      LOGGER.error( "Can't load server configuration!" );
      LOGGER.error( "> " + ex.getLocalizedMessage(), ex );
      return;
    }

    ServerConfiguration.translateDefaultDatabaseProperty( props );

    // Standard behaviour when started from the command line
    // is to halt the VM when the server shuts down. This may, of
    // course, be overridden by whatever, if any, security policy
    // is in place.
    ServerConfiguration.translateDefaultNoSystemExitProperty( props );
    ServerConfiguration.translateAddressProperty( props );

    server = new Server();
    try
    {
      server.setProperties( props );
    }
    catch (Exception e)
    {
      server.printError( "Failed to set properties!" );
      server.printStackTrace( e );
      return;
    }

    server.start();
  }

  @Override
  protected void print( String msg )
  {
    //super.print( msg );
    LOGGER.info( "[" + this.getServerId() + "]: " + msg );
  }

  @Override
  protected void printError( String msg )
  {
    //super.printError( msg );
    LOGGER.error( msg );
  }

  @Override
  protected void printStackTrace( Throwable t )
  {
    //super.printStackTrace( t );
    LOGGER.error( t.getLocalizedMessage(), t );
  }

  @Override
  protected void printWithThread( String msg )
  {
    super.printWithThread( msg );
  }
}