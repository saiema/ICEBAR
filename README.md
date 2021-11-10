# ICEBAR
### Iterative CounterExample Based Alloy Repair

ICEBAR is a technique based on [ARepair](https://github.com/kaiyuanw/ARepair) that takes a buggy model and a property based oracle (both written in Alloy). It uses the oracle to check the model and strengthen a test suite from counterexamples when found. The buggy model alongside a test suite is passed to ARepair, if a fix is found, this is checked against the property based oracle to check if it's a correct fix, if not, then the process repeats until a given bound.

## Test generation

Though generating tests from counterexamples is easy, violations can ocurr from expected instances not being produced. This is the case of predicates and `run pred expect 1` commands.
