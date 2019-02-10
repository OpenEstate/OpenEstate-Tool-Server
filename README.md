OpenEstate-Tool-Server 1.0.0-SNAPSHOT
=====================================

*OpenEstate-Tool-Server* (also called *OpenEstate-ImmoServer*) provides a [*HSQLDB* server](http://hsqldb.org/), that may be used for multi user installations of *OpenEstate-ImmoTool*.

This project

-   extends the default [*HSQLDB* server](http://hsqldb.org/) with some features.
-   provides several startup scripts / executables / application bundles for *Windows*, *macOS* and *Linux*.
-   bundles [*commons-daemon*](https://commons.apache.org/daemon/) to run the database as a *Windows* service.
-   provides scripts to run the database as a [*systemd*](https://en.wikipedia.org/wiki/Systemd) service on *Linux* systems.
-   provides scripts to run the database as a [*launchd*](https://en.wikipedia.org/wiki/Launchd) agent on *macOS* systems.
-   provides an application to create a keystore for *SSL* encrypted database access.
-   provides a GUI & console application for database administration.


Dependencies
------------

-   Java 8 or newer
-   [Bouncy Castle 1.60](https://bouncycastle.org/) 
    (optional; only required by [`SslGenerator.java`](src/main/java/org/openestate/tool/server/utils/SslGenerator.java))
-   [commons-cli 1.4](https://commons.apache.org/cli/)
    (optional; used for [management applications](src/main/java/org/openestate/tool/server/manager))
-   [commons-daemon 1.1.0](https://commons.apache.org/daemon/)
    (optional; used for installing a *Windows* service)
-   [commons-io 2.6](https://commons.apache.org/io/)
-   [commons-lang 3.8.1](https://commons.apache.org/lang)
-   [commons-text 1.6](https://commons.apache.org/text)
-   [gettext-commons 0.9.8](https://code.google.com/archive/p/gettext-commons/)
-   [hsqldb 2.3.5](http://hsqldb.org/)
-   [log4j 1.2.17](https://logging.apache.org/log4j/1.2/)
-   [SLF4J 1.7.25](https://www.slf4j.org/)


Changelog
---------

Take a look at [`CHANGELOG.md`](CHANGELOG.md) for the full changelog.


License
-------

This library is licensed under the terms of [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Take a look at [`LICENSE.txt`](https://github.com/OpenEstate/OpenEstate-Tool-Server/blob/develop/LICENSE.txt) for the license text.


Further information
-------------------

-   [*OpenEstate-Tool-Server* at GitHub](https://github.com/OpenEstate/OpenEstate-Tool-Server)
-   [Releases of *OpenEstate-Tool-Server*](https://github.com/OpenEstate/OpenEstate-Tool-Server/releases)
-   [Changelog of *OpenEstate-Tool-Server*](https://github.com/OpenEstate/OpenEstate-Tool-Server/blob/develop/CHANGELOG.md)
-   [Javadocs of *OpenEstate-Tool-Server*](https://media.openestate.org/apidocs/OpenEstate-Tool-Server/)
