# Installing ICEBAR

## Requirements

As of version 2.8.1, ICEBAR requirements are the following:

1. Java 8 (newer versions may work).
2. [ARepair*](https://github.com/kaiyuanw/ARepair), used for test generation, ARepair is provided as part of our [2.8.1 release](https://github.com/saiema/ICEBAR/releases/tag/2.8.1).
3. [BeAFix 2.12.1](https://github.com/saiema/BeAFix/releases/tag/2.12.1), this is used for test generation, BeAFix 2.12.1 is provided as part of our [2.8.1 release](https://github.com/saiema/ICEBAR/releases/tag/2.8.1).
4. Bash, or zsh on macOS.

_* ARepair has one bug that would cause a NullPointerException on some inputs (model + test suite), even though ICEBAR has an option that can be set in the .properties file to deal with this exceptions, a fix was done in the provided ARepair version. This fix will be soon proposed as a PR in ARepair's repository._

## Installation / Compiling

ICEBAR is provided as a jar file, it can also be compiled with `javac ar/edu/unrc/exa/dc/icebar/ICEBAR.java` from inside the `src` folder.

## Configuration

ICEBAR uses `.properties` files to configure it's behaviour, inside the repository and [release 2.8.1](https://github.com/saiema/ICEBAR/releases/tag/2.8.1) there are some examples of these that can be used to create new ones. All properties are defined and documented inside the provided files.

## Running ICEBAR

To run ICEBAR one can use either the provided jar or the compiled classes.

### Running from JAR

To run ICEBAR using the provided jar file the command `java -jar ICEBAR-2.8.1.jar --help` can be used which will give all necessary information on how to use ICEBAR.

### Running from compiled code

After compiling ICEBAR's code one can use the command `java ar.edu.unrc.exa.dc.icebar.ICEBAR --help` to get information on how to use ICEBAR.
