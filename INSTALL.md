# Installing ICEBAR

## Requirements

As of version 2.8.1, ICEBAR requirements are the following:

1. Java 8 (newer versions may work).
2. [ARepair^](https://github.com/kaiyuanw/ARepair), used for test generation. For convenience, ARepair is included in our [2.8.1 release](https://github.com/saiema/ICEBAR/releases/tag/2.8.1).
3. [BeAFix 2.12.1](https://github.com/saiema/BeAFix/releases/tag/2.12.1), also used for test generation. For convenience, BeAFix 2.12.1 is also included in  our [2.8.1 release](https://github.com/saiema/ICEBAR/releases/tag/2.8.1).
4. Bash if using GNU/Linux, or zsh on macOS (^^).

_^ ARepair has a bug that causes null pointer exceptions on some inputs (model + test suite). ICEBAR provides an option that can be set in the .properties file to deal with these exceptions. A fix to ARepair, dealing with this issue, is provided in our included version of this tool. This fix will be submitted as a pull request to ARepair's repository._

_^^ We tried both ICEBAR and our scripts in GNU/Linux, and macOS, in macOS we had issues when the bash intepreter was changed from the default (zsh) to another one._

## Installing / Compiling

ICEBAR is provided as a jar file containing its binaries. It can also be compiled with the following command:

`javac ar/edu/unrc/exa/dc/icebar/ICEBAR.java` 

from inside the `src` folder.

## Configuration

ICEBAR uses `.properties` files to configure its behavior. Some examples of properties files are provided in the repository and [release 2.8.1](https://github.com/saiema/ICEBAR/releases/tag/2.8.1). All properties are defined and documented in the provided sample files.

Inside both `.properties` files, and in any new one, the property `icebar.tools.arepair.root=Tools/ARepair` must be edited to have the full path to that directory, e.g.: for a user `bob` who downloaded the replication package inside his `Download` folder, the property should be changed to `icebar.tools.arepair.root=/home/bob/Downloads/ICEBAR-2.8.1/Tools/ARepair/`.

## Running ICEBAR

ICEBAR is run from the command line, using the provided jar, or compiling and running the sources.

### Running from JAR

To run ICEBAR using the provided jar file, consider command: 

`java -jar ICEBAR-2.8.1.jar --help` 

It will provide all the necessary information on how to use ICEBAR.

### Running from compiled sources

After compiling ICEBAR's code one can use the following command:

`java ar.edu.unrc.exa.dc.icebar.ICEBAR --help` 

to get information on how to use ICEBAR.
