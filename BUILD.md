# Building WakMeter

This document describes how to build the WakMeter application for different platforms.

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

## Building the JAR file

To build a standalone JAR file that includes all dependencies:

```bash
mvn clean package
```

This will create:
- `target/WakMeter-1.0.0-SNAPSHOT.jar` - A self-contained JAR with all dependencies

## Building the Windows Executable

The project is configured to automatically create a Windows executable (`.exe`) file during the standard Maven build process.

### From Any Platform (Linux, macOS, Windows)

Simply run the standard Maven build command:

```bash
mvn clean package
```

This will create:
- `target/WakMeter.exe` - A Windows executable that wraps the JAR file

The executable is created using Launch4j, which allows cross-platform building of Windows executables.

### Running the Windows Executable

On a Windows machine with Java 21+ installed:

1. Double-click `WakMeter.exe`, or
2. Run from command line: `WakMeter.exe`

The executable will use the Java installation found in `%JAVA_HOME%` or `%PATH%`.

### Executable Features

- **Memory**: Maximum heap size of 512MB (`-Xmx512m`)
- **Java Version**: Requires Java 21 or higher
- **Icon**: Uses the custom icon from `src/main/resources/assets/icon.ico`
- **Type**: GUI application (no console window)

## Running from JAR

If you prefer to run the JAR directly without the Windows executable:

```bash
java -jar target/WakMeter-1.0.0-SNAPSHOT.jar
```

## Alternative: Native Installer with jpackage (Windows Only)

The project also includes a `jpackage` Maven profile for creating a native Windows installer on Windows machines. This approach creates a fully self-contained application with its own JRE.

**Note:** This can only be run on a Windows machine with Java 21+ installed.

```bash
mvn clean package -Pjpackage
```

This will create:
- `target/installer/WakMeter-1.0.0-SNAPSHOT.exe` - A Windows installer

The jpackage installer includes:
- Custom Java runtime (via jlink)
- No external Java installation required
- Native Windows installer

## Skipping Tests

To skip tests during build:

```bash
mvn clean package -DskipTests
```

## Troubleshooting

### "Invalid target release" error

Make sure you're using Java 21 or higher:

```bash
java -version
```

### Launch4j warnings

The warning "Sign the executable to minimize antivirus false positives" is normal. For distribution, consider code signing the executable.

### jpackage profile fails

The jpackage profile requires:
1. Running on Windows
2. Windows SDK tools available
3. Proper JavaFX SDK path configuration in pom.xml
