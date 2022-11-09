# Masking Extensibility Development Toolkit

This toolkit helps in developing Masking extensible plugins that can be consumed by the Delphix Masking Engine.

## Prerequisites
1. Familiarity with Core Java
2. Gradle build tool
3. Install Java8 JDK
4. Setup JAVA_HOME environment variable to point to the JDK installation.


## Development
The easiest way to get started is to do all development under the corresponding sample projects directory.
The steps to create a plugin for each plugin type are listed below.

### Algorithm
1. Create a class under path `algorithmSamples/src/main/java/<package>/`
2. The class should implement `com.delphix.masking.sdk.algorithm.MaskingAlgorithm` interface
3. Implement mask method
4. Run command `./gradlew :algorithm:jar` to generate the jar file. The jar is generated under path `algorithm/build/libs/`

### Driver Support
1. Create a class under path `driverSupportSamples/src/main/java/<package>/`
2. The class should implement `com.delphix.masking.api.driverSupport.DriverSupport` interface
3. Implement Task classes and `DriverSupport#getTasks` method.
4. Run command `./gradlew :driverSupport:jar` to generate the jar file. The jar is generated under path `driverSupport/build/libs/`

### Adding External Maven Dependencies
In order to add any dependency on any external maven dependency, please add the dependency in the appropriate project's `build.gradle` file under `dependencies`.

### Create Plugin Outside the SDK Project
1. Create a directory for the plugin.
2. Initialize the directory

   `$ ./sdkTools/bin/maskScript init -d <path to the plugin directory> -n <plugin name> -a <plugin author name> -v <plugin version> -t <project type: algorithm or driverSupport>`.

   This will initialize the directory with following items:
   1. Gradle build files
       1. build.gradle
       2. gradle/
       3. gradle.properties
       4. gradlew
       5. gradlew.bat
       6. settings.gradle
   2. **docs/**: It has javadocs for the masking extensible plugin APIs
   3. **libs/**: It contains the necessary JARs needed for the masking extensible plugin development
   4. **src/**: It contains the JAVA code for the algorithm. A sample project of that project's type is generated under `src/main/java/com/sample/`.
3. To add more JAVA files, use the Skeleton Generator.
4. To build the masking extensible plugin jar, run `./gradlew clean build` from the project's directory.
5. The jar is generated at location `<project directory>/build/libs/`

## Testing

There are three ways to test a masking extensible plugin,
1. Unittest (for algorithm plugins only)
2. CLI Application
3. Script

#### Unittest
This method will test the algorithms only if it is part of the
internal algorithm subproject. This will not test algorithms developed
anywhere else.
`./gradlew test`
If the build is failing due to formatting error, please run `./gradlew spotlessApply` to fix the formatting, then run the `test` task again.

#### CLI Application For Masking

1. Create the application executable, `./gradlew installDist`
2. Start the application, `./sdkTools/bin/maskApp`
3. `MASKING-APP:> mask`
4. If you want to use a jar other than the one in the `algorithm` project, then run the command with `-j` option,

    `MASKING-APP:> mask -j <Location of the jar file>`
5. It will list all the compatible algorithm names.
6. Select the one that you want to test.
7. The algorithm name can be provided at the command itself,

   `MASKING-APP:> mask -j <Location of the jar file> -f <framework name>`
8. Type the value that you want to mask.
9. Type `doneMasking` to stop the masking.
10. `exit` to exit the CLI application.

NOTE:
The **maskApp** doesn't handle backslash(\) in path correctly. User
should use forward slash(/) as path separator on windows environment.

#### Script
1. Create the script executable, `./gradlew installDist`

##### Algorithm
2. Run the script,
  1. `./sdkTools/bin/maskScript mask -i <algorithm instance name> --algojar <Location of the jar file> [input1][input2]...`
  2. `echo "hello" | ./sdkTools/bin/maskScript mask --algo <algorithm name> `
  
##### Driver Support
2. Run the script,
  1. `./sdkTools/bin/maskScript taskExecute -n <task name> -j <location of driver support jar file> -c <location of config file> -l <location of JDBC driver jar file>`

### Skeleton Generator
There is a helper script to generate the boilerplate code for writing a masking extensible plugin. Steps to use the script are:
1. Create the generator executable, `./gradlew installDist`
2. The script can be used in two ways,
  1. Interactive way,
     Each input like package name, class name, etc. will be asked from user.

     `./sdkTools/bin/maskScript generate [project type] -i`

     The script will ask for each missing parameter. The script can be used by passing partial parameters too. E.g.

     `./sdkTools/bin/maskScript generate [project type] -i -c <className>`

     Note: The path should exclude the package directories. The script will generate the package directories relative to the location given.
  2. Non-interactive way,
     Pass all the required inputs while running the script.

     `./sdkTools/bin/maskScript generate [project type] -c <className> -p <packageName> -v <valueType> -s <Location where the Java file will be generated>`

     Note 1: The path should exclude the package directories. The script will generate the package directories relative to the location given.
     Note 2: For internally (within the SDK) created algorithms - the algorithm info (for Service Discovery feature, allowing fast algorithm classes loading)  is added by default to the /META-INF/services/<implemented-interface-name> file. For externally created algorithms (using -s option, outside of the SDK project) the similar /META-INF/services/<implemented-interface-name> file is created in the chosen external directory. There is an option to omit that file, by using the `-n` flag (no argument is required for that option).

### Plugin Installation
The plugin jar can be installed in the masking engine using the REST APIs. It is a two step process.
1. Upload the jar using the fileUpload endpoint.
2. Copy the fileReferenceId from step 1, and use it in the plugin create endpoint.

There is a tool in this SDK to help in that.

`./sdkTools/bin/maskScript install -j <jarPath> -H <masking server host> -P <masking server port> -u <masking server username> -n <name of the plugin>`

The tool either create a new plugin or update an existing plugin if a plugin with same name exists.
The password can be passed as environment variable
MASKING_ENGINE_PASSWORD or interactively.
The tool uses HTTPS to connect to the masking engine by default. If
you want http, then pass `-k` flag to the command.

### Debug

In order to debug algorithms, remote debugging should be used. The `maskScript` and the `maskApp` executables can be run with DBEUG mode on. In order to enable debug mode
1. For `maskScript`, set the environment variable **MASK_SCRIPT_OPTS** with value "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
2. For `maskApp`, set the environment variable **MASK_APP_OPTS** with value "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

The above environment variable would make the applications to open the port 5005 to debug. In any IDE that supports Remote JVM debug, create a Remote Debugging configuration and use the port 5005 in it.

To disable the debug mode, unset the above environment variables.
