The Pianola agent instruments a running Java Swing desktop application.

When the application starts, the agent opens a port (default 26060) and begins
listening for requests. Keep in mind that, as of now, the connection is
**completely unsecured**!

The endpoint offers a REST API.

    - POST to /snapshots creates a "snapshot" resource containing the state of
      the gui at that moment.

    - GET to /snapshots/snapshotId gets a JSON representation of the snapshot.

Building the agent 
==================

Building the agent requires Maven. 

Invoke

> mvn package 

If the compilation is successful, the **pianola-driver** jar should appear in the **target** folder. 

Configuring the Application Under Test
======================================

When starting the AUT an argument of the following form must be passed to the JVM:

> -javaagent:path_to_pianola_agent_jar

There's no need to add the jar to the classpath as well.

See the documentation of the
[java.lang.instrument](http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html)
packgage for details on how to start agents from the command line.

If configured correctly, the agent should start automatically with the application.



