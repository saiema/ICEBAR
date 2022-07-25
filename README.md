# ICEBAR
### Iterative CounterExample Based Alloy Repair

ICEBAR is a technique based on [ARepair](https://github.com/kaiyuanw/ARepair) that takes a buggy model and a property based oracle (both written in Alloy). It uses the oracle to check the model and strengthen a test suite from counterexamples when found. The buggy model alongside a test suite is passed to ARepair, if a fix is found, this is checked against the property based oracle to check if it's a correct fix, if not, then the process repeats until a given bound.

# Overview of the technique

The technique starts from a given faulty specification (with property-based oracles included) and a, potentially empty, test suite, it then runs ARepair to attempt to produce a patch. If a patch is found, we know that is passes the test suite; we take this candidate and contrast it against the property-based oracle in the specification. If, again, the specification meets this oracle, we consider the patch a proper fix. If, on the other hand, some property fails in the candidate patch, we use Alloy to produce instances (assertion counterexamples, violations to predicates), which are in turn translated into new unit tests, to complement the original test suite and start over the ARepair process. Eventually, a fix is found, that passes the corresponding test suite and property-based oracle, and is returned to the user.

# Test generation

When generating a test from a counterexample **CE**, the generated test represents an undesired behaviour:

* **CE** should not be a valid instance of the model, e.g.: facts should be modified to filter that instance.
* **CE** is a valid instance and then it should satisfy the property defined by the assertion.

This two cases lead to generate the test `CE and (not facts or P)` where **P** is the property associated with the assertion.

When a counterexample is not available and the model is buggy, i.e.: no instance is generated for a predicate **P** for which instances are expected, instances can be obtained in two possible ways:

* Using another predicate **Q** for which instances can be produced (primary option).
* By temporarily and incrementally relaxing the model's facts to try to produce an instance for predicate **P** (secondary option).

In both cases, the oracle problem arises: given an instance **I** that satisfies predicate **P**, is this the expected behaviour or not? To solve this issue *ICEBAR* uses branches, in one branch the assumption that `I and P` is the expected behaviour; in the other the opossite asumption is made `I and not P`. 

# Requirements

**ICEBAR** has the following requirements:

1. Java 8+
2. [ARepair](https://github.com/kaiyuanw/ARepair), we provide ARepair as a jar in each [release](https://github.com/saiema/ICEBAR/releases).
3. [BeAFix](https://github.com/saiema/BeAFix), this is used for test generation.

# Instalation

For instalation instructions please refer to the **INSTALL.md** file.

# Publications and experiments replication

A research paper for **ICEBAR** tittled *"ICEBAR: Feedback-Driven Iterative Repair of Alloy Specifications"* was accepted at **ASE 2022**. The version used in this research paper was **2.8.1** which can be found in the releases section. Release [2.8.1](https://github.com/saiema/ICEBAR/releases/tag/2.8.1) also contains the whole replication package, including benchmarks and scripts. 
