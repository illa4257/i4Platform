# Java Executable Wrapper
A simple C++ script for running JAR programs with the flexibility to change the JRE and arguments.

### Features
- Passes all arguments to a new process.
- Allows configuring the command for running Java and JVM arguments via a config file in the executable's directory.
- Uses INI files for easy configuration.

### Config
The script does not create the config file automatically; you should create it yourself. The config file should be an INI file placed in the same directory as the executable.

Example, if `CONFIG_FILENAME` is `test app`, then `test app.ini` should look like this:
```ini
# Command for launching java
command=java

# JVM args
java_start_args=-Xms128M -Xmx256M
```

### Building
To build the launcher on Windows (with `CONFIG_FILENAME` set to `test app`), use the following command:
```sh
gcc -O3 "-DCONFIG_FILENAME=\"test app\"" "-DARGS=\"-jar MyApp.jar\"" src/launcher.cpp -lstdc++ -o build/Launcher.exe
```