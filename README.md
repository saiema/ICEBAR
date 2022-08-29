# ICEBAR
### Iterative CounterExample Based Alloy Repair

ICEBAR is a technique based on [ARepair](https://github.com/kaiyuanw/ARepair) that takes a buggy model and a property based oracle (both written in Alloy). It uses the oracle to check the model and strengthen a test suite from counterexamples when found. The buggy model alongside a test suite is passed to ARepair, if a fix is found, this is checked against the property based oracle to check if it's a correct fix, if not, then the process repeats until a given bound.

ICEBAR's repository can be found at https://github.com/saiema/ICEBAR.

ICEBAR's replication package site can be found at https://sites.google.com/view/icebar-evaluation/home/replication-package.

# Overview of the technique

The technique starts from a given faulty specification (with property-based oracles included) and a, potentially empty, test suite, it then runs ARepair to attempt to produce a patch. If a patch is found, we know that is passes the test suite; we take this candidate and contrast it against the property-based oracle in the specification. If, again, the specification meets this oracle, we consider the patch a proper fix. If, on the other hand, some property fails in the candidate patch, we use Alloy to produce instances (assertion counterexamples, violations to predicates), which are in turn translated into new unit tests, to complement the original test suite and start over the ARepair process. Eventually, a fix is found, that passes the corresponding test suite and property-based oracle, and is returned to the user.

## Test generation

When generating a test from a counterexample **CE**, the generated test represents an undesired behaviour:

* **CE** should not be a valid instance of the model, e.g.: facts should be modified to filter that instance.
* **CE** is a valid instance and then it should satisfy the property defined by the assertion.

This two cases lead to generate the test `CE and (not facts or P)` where **P** is the property associated with the assertion.

When a counterexample is not available and the model is buggy, i.e.: no instance is generated for a predicate **P** for which instances are expected, instances can be obtained in two possible ways:

* Using another predicate **Q** for which instances can be produced (primary option).
* By temporarily and incrementally relaxing the model's facts to try to produce an instance for predicate **P** (secondary option).

In both cases, the oracle problem arises: given an instance **I** that satisfies predicate **P**, is this the expected behaviour or not? To solve this issue *ICEBAR* uses branches, in one branch the assumption that `I and P` is the expected behaviour; in the other the opossite asumption is made `I and not P`. 

# Requirements and Installation

For instalation instructions please refer to the **INSTALL.md** file.

# Folder structure and files

After downloading and extracting ICEBAR's zip file (release [2.8.1](https://github.com/saiema/ICEBAR/releases/tag/2.8.1) and above), there will be the following folders and files:

 * `experiments` folder contains ARepair's and Alloy4Fun's benchmarks.
 * `Tools` folder contains both ARepair and BeAFix, the second one is used by ICEBAR to generate tests.
 * `ICEBAR-<version>.jar`, this can be used by executing `java -jar ICEBAR-<version>.jar <arguments>`, use `--help` to get information about the             arguments to use. The `<version>` will be `2.8.1` or above, and it depends on the downloaded release.
 * `icebar.properties`, a properties file with the configuration used in our experiments. You can use this file as a template for you own configurations.
 * `icebar_ARepairMode.properties`, a properties file to run ARepair once and check the fix against a property-based oracle. The main difference of this     configuration with respect to the previous one is the amount of laps (iterations) used, in this case we used **0** laps, this is equivalent to just       running ARepair.
 * `icebar-run.sh`, this script runs all ICEBAR experiments.
 * `icebar-run-arepair.sh`, this script runs all ARepair experiments.
 * `modelsA4F.sh`, this script defines all Alloy4Fun's models to use. Variable `A4F_CASES` can be modified to remove specific cases by prefixing them        with the `#` symbol.
 * `modelsARepair.sh`, this script defines all ARepair's models to use. Variable `AREPAIR_CASES` can be modified to remove specific cases by prefixing      them with the `#` symbol.

# Usage
### Running ICEBAR

ICEBAR uses a mixture of `.properties` files to configure ICEBAR's behaviour, and a set of command line arguments. There are two `.properties` files available with the 2.8.1 release as well as part of the repository, these properties have each property documented. And for help with the command line the following command is available:

```
java -jar ICEBAR-2.8.1.jar --help
```

The same command, changing `--help` with the appropiate arguments, can be used to run ICEBAR on a specific model.

## ICEBAR's configuration

ICEBAR relies on a few arguments: a model to repair, an optional initial test suite to use, a property based oracle for repair acceptance and test generation, and a `.properties` file. Although the provided properties files have each property documented, here we will document some of the more important ones.

 * `icebar.tools.beafix.jar=Tools/BeAFixCLI-2.12.1.jar`: this property defines where to find BeAFix's CLI jar, it's value can be a relative path, and the     jar is provided within the release (_2.8.1 and above_).
 * `icebar.tools.beafix.tests=1`: how many tests to generate when a repair candidate is found to be spurious. This number is related to generated             instances rather than generated tests, since untrusted instances (those generated from a predicate) will result in two tests, a positive and a           negative test.
 * `icebar.tools.beafix.modeloverridesfolder=Tools/modelOverrides`: this is a property that comes from BeAFix, the tool used for test generation, this is     already provided. This is used to ignore signature, and to be able to use functions instead of fields when generating tests.
 * `icebar.tools.beafix.instancetests=true`: when `true`, test generation will be able to generate tests from predicates.
 * `icebar.tools.beafix.compat.relaxed=true`: this must always be set to `true`. ARepair need tests with certain restrictions that are not usually used       by BeAFix.
 * `icebar.tools.beafix.noinstancetestsfornegativetestwhennofacts=true`: on a negative test, i.e.: `INSTANCE && PRED expect 0`, the tests may be             satisfied by preventing `INSTANCE` for being generated. A second test can be added to ensure this instance IS generated, i.e.: `INSTANCE expect 1`.       When the model does not have any fact, this second test can be discarded, this property will allow this behaviour. 
 * `icebar.tools.arepair.root=Tools/ARepair`: this property defines the root folder for ARepair, it cannot be a relative path. For a user `bob` in a         GNU/Linux OS who downloaded our release (2.8.1 and above) in the `Download` folder, the path would be `/home/bob/Downloads/ICEBAR-2.8.1/Tools/ARepair/`.
 * `icebar.tools.arepair.partialrepairasfixes=true`: sometimes ARepair will not return a repair, although it did make some modifications to the original     model, when this option is set to `true`, this partial repairs will be used as full repairs, otherwise they will be ignored.
 * `icebar.laps=30`: how many `AREPAIR -> REPAIR CHECK -> TEST GENERATION` laps will be used to try and find a repair. Using a value of 0 would be           equivalent to run ARepair and then check the produced repair against the property based oracle.
 * `icebar.timeout=60`: the timeout, in minutes, that ICEBAR will used, a value of 0 means no timeout.
 * `icebar.priorization=false`: when set to `true`, candidates (test suites) that lead to satisfying more property based oracles will be used first.
 * `icebar.search=BFS`: the search algorithm to use, either Depth First Search (DFS) or Breadth First Search (BFS).
 * `icebar.allowrelaxedfacts=true`: will try to produce instances from a predicate by relaxing the model's facts.
 * `icebar.forceassertiontests=true`: this will transform an assertion (that do not produce any counterexample) into a predicate to try to produce an         instance.
 * `icebar.globaltrustedtests=false`: a trusted test generated from a candidate that includes untrusted tests will be used globaly when this property is     set to `true`, otherwise it will use these tests locally.
 * `icebar.updatescopefromoracle=true`: when `true`, ARepair scope will be updated from the maximum scope used in the oracle (when `false`, this will         only be done for initial tests).

_The rest of the properties are either no longer used or only used for debugging purposes_

## ICEBAR's output

ICEBAR will generate three files:

 * `icebar.info` will contain **status(^)**;**laps**;**tests(^^)**;**test Gen And Model Check Time(ms)(^^^)**;**ARepair time (ms)**;**ARepair calls**
    _(^) This can be: **correct** for a fixed model; **timeout**; **exhausted** when no more tests are available; **ARepair once Spurious** when running only ARepair once and the repair was found but spurious; **ARepair once No Fix Found** when running only ARepair once and no repair was found; and any other value represent an error in either ICEBAR, BeAFix, or ARepair_
 * `icebar_arepair.info` will contain information about all calls made to ARepair
 * `Repair.log` this will contain the same output as the one shown in the terminal when running ICEBAR, information that will appear in this log will include:
   * Running ARepair: this may end with no fix found, a fix found, no tests available _(this is treated as a fix found, used to bootstrap the test generation)_, or an error ocurred while running ARepair.
   * Checking a model: this may end with `CHECK FAILED` indicating that the repair produced by ARepair was spurious; `CHECK SUCCEEDED` if the repair produced by ARepair is a proper fix; or an error if there was one while running BeAFix.
   * Test generation: this will either state that no tests were generated; tests were generated and which ones; an error ocurred while generating tests.
   * The result of running ICEBAR: if a fix is found, a path to this fix will be shown.

# Replicating ICEBAR's experiments

We provided scripts to replicate our experiments (in release [2.8.1](https://github.com/saiema/ICEBAR/releases/tag/2.8.1)).

Inside both `.properties` files, and in any new one, the property `icebar.tools.arepair.root=Tools/ARepair` must be edited to have the full path to that directory, e.g.: for a user `bob` who downloaded the replication package inside his `Download` folder, the property should be changed to `icebar.tools.arepair.root=/home/bob/Downloads/ICEBAR-2.8.1/Tools/ARepair/`.

## Using Docker

We provide a docker image as an alternative for the replication of our experiments. For using our docker image please follow these instructions:

 1. Install Docker, by either installing Docker Desktop for [Windows](https://docs.docker.com/desktop/install/windows-install/) or [macOS](https://docs.docker.com/desktop/install/mac-install/); or by installing [Docker Engine](https://docs.docker.com/engine/install/) (not available for Windows).
 2. Pull our docker image `drstein/icebar:2.8.1` by executing `docker push drstein/icebar:2.8.1` or by using Docker Desktop.
 3. Create and run a docker container from our image by executing the command `docker run -it drstein/icebar:2.8.1`.
  
The folder structure is the same as the one mentioned [above](#folder-structure-and-files), and the instructions are the same as for replicating our experiments natively.

## Experiments replication

The script for running these experiments is `icebar-run.sh` which can take two possible inputs: `--run-ARepair` to use the ARepair's bechmark; and `--run-A4F` to use the Alloy4Fun's benchmark. The cases for each benchmark can be disabled by commenting (prepending `#`) unwanted cases inside `AREPAIR_CASES` in `modelsARepair.sh`, or `A4F_CASES` in `modelsA4F.sh` for ARepair and Alloy4Fun benchmarks respectively.

## Running ICEBAR on ARepair's benchmark
```
./icebar-run.sh --run-ARepair
```

## Running ICEBAR on Alloy4Fun's benchmark
```
./icebar-run.sh --run-A4F
```

## Running ARepair experiments (given a model and 4 test suites, run ARepair once per test suite). The used test suites are an automatic generated one (using AUnit), and three randomly generated test suites.

### Running ARepair on ARepair's benchmark
```
./icebar-run-arepair.sh --run-ARepair
```

### Running ARepair on Alloy4Fun's benchmark
```
./icebar-run-arepair.sh --run-A4F
```

## Scripts output

The results of running either `icebar-run-arepair.sh` or `icebar-run.sh` are summarized in a `.csv` file at the experiment's root folder. For example: if the ICEBAR experiments are executed over the Alloy4Fun benchmark (`./icebar-run.sh --run-A4F`), file `summary-A4F-ICEBAR.csv` will be created by the script, containing the following information:

**model case name** | **repair result** _( correct, not fix found^, exhausted^, spurious^, or timeout)_ | **ICEBAR iterations** _(0 in case of ARepair mode)_ | **# of test generated** | **checking and test generation time (ms)** | **Repair time (ms)** | **# of calls to the ARepair tool**

_(^) not fix found and spurious are reported when running in ARepair mode (0 max iterations); exhausted is used when running with at least one iteration._

# Publications and experiments replication

A research paper for **ICEBAR** tittled *"ICEBAR: Feedback-Driven Iterative Repair of Alloy Specifications"* was accepted at **ASE 2022**. The version used in this research paper was **2.8.1** which can be found in the releases section. Release [2.8.1](https://github.com/saiema/ICEBAR/releases/tag/2.8.1) also contains the whole replication package, including benchmarks and scripts. 
