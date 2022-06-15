# Docker config POC
I made this POC in order to show code that allows to build
docker images and select the application conf and java parameters
at container creation time in a manner that escalates well with
Kubernetes.

## The project
The POC is an sbt multi-project, I used that in order to keep the two
versions of the POC in the same git repo.
<br>
Is fine if you are not familiar with multi-project, just consider
the sbt command that builds the docker container has a small change
and each sub-project has the code contained in its own folder
(internal or external)

## Internal
This version of the POC shows how we can, with one same image, choose
the configuration file in the repo as well as the java parameters
at container creation time. This way we can use the same image to
run in any predefined environment.
<br>
For this scenario we assume we will have all the files in the git repo.

### Run the internal POC
1) Build the image. It requires to have docker installed in your local.
   Also consider that is possible you see normal output of the image being
   built as [error] if your machine is not Linux
```
$ sbt internal/Docker/publishLocal
```
2) Check that the image is built
```
$ docker images
REPOSITORY        TAG                   IMAGE ID       CREATED          SIZE
internal          0.1                   60b117a3ca57   12 seconds ago   279MB
```
3) We build a container that works with staging and one that works
   for production
```
$ docker run -d --env SERVER_ENV=stg --name=dockerTest1 internal:0.1
...
$ docker run -d --env SERVER_ENV=prod --name=dockerTest2 internal:0.1
```
4) We check that the app has access to different configuration values
   with the logs
```
$ docker logs dockerTest1
This is the docker config poc app (internal version)
We can access values from the staging config file
$ docker logs dockerTest2
This is the docker config poc app (internal version)
We can access values from the production config file
```

### How does the internal POC work?
In sbt file you'll see the parameters used for the docker plugin:
```
//dockerBaseImage := "eclipse-temurin:8u312-b07-jdk-focal"
dockerBaseImage := "openjdk:8-jre",
dockerExposedPorts ++= Seq(9990, 8888),
bashScriptConfigLocation := Some("${app_home}/../conf/application.${SERVER_ENV}.ini")
```
- The `docker image` is just one image with java development kit
  version 8 installed. Another strong option is commented.
- We also see that we can choose the `ports` to expose.
- We specify `bashScriptConfigLocation` setting: Since is an
  instruction to be embedded in a bash script, it will pick
  `SERVER_ENV` environment variable which we pass at
  container creation time.

We just do as is specified in the [official docs](https://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/customize.html#via-application-ini)
<br>
The application....ini file contains the java parameters that we want
for each specific environment and with that we specify the conf file with the
config.resource system prpoerty.

We load the conf file relying on that system property:
```
val config:Config = ConfigFactory.load()
```

## External
This version of the POC covers the case where we want the config
file to contain values not specified by the developers of the app
but external configuration. This way we are able to build images that
serve the needs of clients external to the organization without
adding their information to the git repo of the app.
<br>
For this scenario we assume that part of the configuration is in the
repo. Externally we have another file with the rest of the
configuration and also a file the java parameters to run the
application. We have access to these two external files at
container creation time.

### Security note about the external files
Those files' information can come from the client and we would end up
building volumes or plain files depending on the specifics of our
deployment. However, in case we decide to have some API interface that
automatically provides those files with the client input, we should
be very careful with the security checks at that API. After all
these values could potentially override the native configuration
of an app that accesses internal resources of the organization.

Let's consider as well that regardless of the security concerns
we cannot let the client send us the complete file since it
needs to comply a format specific to the application and we can't
let the client have any responsibility for a malfunction on
the app.

If we have any doubt about it at all, it should be ok we keep a
manual procedure to produce and validate those external files
with the client input.

### Run the external POC
1) Build the image. It requires to have docker installed in your local.
   Also consider that is possible you see normal output of the image being
   built as [error] if your machine is not Linux
```
$ sbt external/Docker/publishLocal
```
2) Check that the image is built
```
$ docker images
REPOSITORY        TAG                   IMAGE ID       CREATED          SIZE
external          0.1                   4e7e95f2b8d0   10 seconds ago   279MB
```
3) We build an image using the files in the folder clientModule in the
   repo. Even if these files are in the git repo, they do not participate
   in the build of the image and could be as well local files in the
   host machine. You can test by copy&pasting the folder somewhere else
   and change the command accordingly. These files could be in a volume
   when using kubernetes but for this poc we use the `bind` option
   in the mount parameter which creates a link between some mounted
   folder in the container and your local folder.
   <br><b>To run this command you need to be at the root of
   the cloned repo</b>
```
$ docker run -d --name=dockertest_ext --mount type=bind,source="$(pwd)"/clientModule,target=/opt/conf/ext  external:0.1
```
4) We check that the app has access to the configuration values
   with the logs.
```
$ docker logs dockertest_ext
This is the docker config poc app (external version)
We can access values from the external config file
We can access values from the base config file
```

### How does the external POC work?
In sbt file you'll see the parameters used for the docker plugin:
```
//dockerBaseImage := "eclipse-temurin:8u312-b07-jdk-focal"
dockerBaseImage := "openjdk:8-jre",
dockerExposedPorts ++= Seq(9990, 8888),
bashScriptConfigLocation := Some("/opt/conf/ext/application.external.ini")
```
- The other parameters stay the same values and meaning as with the
  internal version but regarding `bashScriptConfigLocation` we specify
  a file to run the java parameters that is in a folder mounted at
  the container at creation time.
- Note that in this case the file `application.external.ini` points to 
  a file mounted inside the container but external to the jar. It does 
  so via the config.file system property.

We load the conf file relying on that system property:
```
  val config = ConfigFactory.load()
```

Note that this conf file starts with:
```
include "application.base.conf"
```

This is why we can still load conf values in the resource folder that
is inside the repo, specifically the file `application.base.conf`.
