Alfresco System Usage Statistics
=============================================

This module is sponsored by Redpill Linpro AB - http://www.redpill-linpro.com.

Description
-----------
This project contains some basic statistics gathering of your Alfresco system.

Compatible with Alfresco 7.4.x, built with Alfresco SDK 4.6.

Structure
------------

The project consists of a platform (repository) module and a share module packaged as jar files, built using the Alfresco SDK 4.x AIO structure with Docker support.

Building & Installation
------------
The build produces several jar files. Attach them to your own maven project using dependencies or put them under tomcat/shared/lib.

Platform (repository) dependency:
```xml
<dependency>
    <groupId>org.redpill-linpro.alfresco.statistics</groupId>
    <artifactId>alfresco-system-usage-statistics-platform</artifactId>
    <version>1.2.0</version>
</dependency>
```

Share dependency:
```xml
<dependency>
    <groupId>org.redpill-linpro.alfresco.statistics</groupId>
    <artifactId>alfresco-system-usage-statistics-share</artifactId>
    <version>1.2.0</version>
</dependency>
```

Maven repository:
```xml
<repository>
  <id>redpill-public</id>
  <url>https://maven.redpill-linpro.com/nexus/content/groups/public</url>
</repository>
```

The jar files are also downloadable from: https://maven.redpill-linpro.com/nexus/index.html#nexus-search;quick~alfresco-system-usage-statistics


License
-------

This application is licensed under the LGPLv3 License. See the [LICENSE file](LICENSE) for details.

Authors
-------

Marcus Svartmark - Redpill Linpro AB
