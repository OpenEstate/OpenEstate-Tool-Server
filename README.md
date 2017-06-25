OpenEstate-Tool-Server 1.0-SNAPSHOT
===================================

*OpenEstate-Tool-Server* (also called *OpenEstate-ImmoServer*) provides a
[*HSQLDB* server](http://hsqldb.org/), that may be used for multi user
installations of *OpenEstate-ImmoTool*.

This project

-   extends the default [*HSQLDB* server](http://hsqldb.org/) with some
    features.
-   provides several startup scripts / executables / application bundles for
    *Windows*, *macOS* and *Linux*.
-   bundles [*yajsw*](http://yajsw.sourceforge.net/) in order to run the
    database as a *Windows* service.
-   provides scripts to run the database as a
    [*systemd*](https://fedoraproject.org/wiki/Features/systemd) service on
    *Linux* systems.
-   provides scripts to run the database as a
    [*launchd*](https://en.wikipedia.org/wiki/Launchd) agent on *macOS* systems.
-   provides an application to create a keystore for *SSL* encrypted database
    access.


Dependencies
------------

-   Java 7 or newer
-   [Bouncy Castle 1.57](https://bouncycastle.org/)
    (optional; only required by
    [`SslGenerator.java`](src/main/java/org/openestate/tool/server/utils/SslGenerator.java))
-   [commons-io 2.5](http://commons.apache.org/proper/commons-io/)
-   [commons-lang 3.6](http://commons.apache.org/proper/commons-lang/)
-   [gettext-commons 0.9.8](https://code.google.com/archive/p/gettext-commons/)
-   [hsqldb 2.3.5](http://hsqldb.org/)
-   [log4j 1.2.17](http://logging.apache.org/log4j/1.2/)
    (optional; may be replaced by another logging system
    [via SLF4J](http://www.slf4j.org/manual.html))
-   [SLF4J 1.7.25](http://www.slf4j.org/)


Changelog
---------

Take a look at [`CHANGELOG.md`](CHANGELOG.md) for the full changelog.


License
-------

This library is licensed under the terms of
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
Take a look at
[`LICENSE.txt`](https://github.com/OpenEstate/OpenEstate-Tool-Server/blob/develop/LICENSE.txt)
for the license text.


Todo
----

-   improve javadoc comments


Further informations
--------------------

-   [*OpenEstate-Tool-Server* at GitHub](https://github.com/OpenEstate/OpenEstate-Tool-Server)
-   [Releases of *OpenEstate-Tool-Server*](https://github.com/OpenEstate/OpenEstate-Tool-Server/releases)
-   [Changelog of *OpenEstate-Tool-Server*](https://github.com/OpenEstate/OpenEstate-Tool-Server/blob/develop/CHANGELOG.md)
-   [Javadocs of *OpenEstate-Tool-Server*](http://manual.openestate.org/OpenEstate-Tool-Server/)
