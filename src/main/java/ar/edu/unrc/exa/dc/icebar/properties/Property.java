package ar.edu.unrc.exa.dc.icebar.properties;

@SuppressWarnings("SpellCheckingInspection")
public enum Property {

        ICEBAR_USE_DEFAULT_ON_INVALID_VALUE {
            @Override
            public String getKey() {
                return ICEBAR_PREFIX + ".usedefaultoninvalidvalue";
            }

            @Override
            public String getDescription() {
                return "[BOOLEAN] When true, if a properties value is invalid, the default value (when available) will be used instead.";
            }
        },
        BEAFIX_JAR {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".jar";
            }
            @Override
            public String getDescription() {
                return "[PATH] BeAFix jar location (CLI version of BeAFix, please verify minimum required version).";
            }
        },
        BEAFIX_TESTS {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".tests";
            }

            @Override
            public String getDescription() {
                return "[POSITIVE INTEGER] How many tests will BeAFix try to generate, this defines an upper bound as it limits how many instances to use.";
            }
        },
        BEAFIX_MODEL_OVERRIDES_FOLDER {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".modeloverridesfolder";
            }

            @Override
            public String getDescription() {
                return "[PATH] Defines the location for model overrides' folder (used by BeAFix to swap signature/field initialization with function initialization), no value means no overriding." +
                        "\n\tSee BeAFix's documentation for more information (execute `java -jar <BeAFixCLI jar> --help TESTS`).";
            }
        },
        BEAFIX_INSTANCE_TESTS {
            @Override
            public String getKey() {
                return BEAFIX_PREFIX + ".instancetests";
            }

            @Override
            public String getDescription() {
                return "[BOOLEAN] Defines if BeAFix generate tests from instances (obtained by run commands with expect 1).";
            }
        },
        BEAFIX_NO_INSTANCE_TEST_FOR_NEGATIVE_TEST_WHEN_NO_FACTS {
            @Override
            public String getKey() { return BEAFIX_PREFIX + ".noinstancetestsfornegativetestwhennofacts"; }

            @Override
            public String getDescription() {
                return "[BOOLEAN] When set to true, this will prevent generation of tests like '<INSTANCE> expect 1' for '<INSTANCE> && <PRED> expect 0' tests when the model has no facts.";
            }
        },
        AREPAIR_ROOT {
            @Override
            public String getKey() {
                return AREPAIR_PREFIX + ".root";
            }

            @Override
            public String getDescription() {
                return "[ABSOLUTE PATH] The root folder for ARepair, the following restrictions apply:" +
                        "\n\t1) A sat-solvers folder must exist inside the root folder containing minisat libraries" +
                        "\n\t2) A libs folder inside root containing both alloy.jar and aparser-1.0.jar" +
                        "\n\t3) A target folder inside root containing arepair-1.0-jar-with-dependencies.jar";
            }
        },
        AREPAIR_TREAT_PARTIAL_REPAIRS_AS_FIXES {
            @Override
            public String getKey() { return AREPAIR_PREFIX + ".partialrepairasfixes";}

            @Override
            public String getDescription() {
                return "[BOOLEAN] When true, partial fixes found by ARepair will be treated as fixes. We suggest to enable this option.";
            }
        },
        ICEBAR_LAPS {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".laps"; }

            @Override
            public String getDescription() {
                return "[NON NEGATIVE INTEGER] Number of iterations done by ICEBAR until a fix is found." +
                        "\n\tA value of `0` is equivalent to run ARepair with the initial tests and check if the fix found satisfies the property-based oracle.";
            }
        },
        ICEBAR_PRIORIZATION {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".priorization"; }

            @Override
            public String getDescription() {
                return "[BOOLEAN] Candidates with less violated properties will be given priority when this option is enabled.";
            }
        },
        ICEBAR_SEARCH {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".search"; }

            @Override
            public String getDescription() {
                return "[DFS|BFS] Base search algorithm, either Depth First Search (DFS) or Breadth First Search (BFS).";
            }
        },
        ICEBAR_ENABLE_RELAXEDFACTS_GENERATION {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".allowrelaxedfacts"; }

            @Override
            public String getDescription() {
                return "[BOOLEAN] Will call BeAFix Test generation with relaxed facts (incrementally remove facts until an instance can be generated)." +
                        "\n\tThis will only be used when no tests where produced by more trusted methods, see ICEBAR documentation";
            }
        },
        ICEBAR_ENABLE_FORCE_ASSERTION_TESTS {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".forceassertiontests"; }

            @Override
            public String getDescription() {
                return "[BOOLEAN] Will call BeAFix Test generation forcing assertion tests (this will generate predicates from where to try and generate tests)." +
                        "\n\tThis will only be used when no tests where produced by more trusted methods, see ICEBAR documentation";
            }
        },
        ICEBAR_GLOBAL_TRUSTED_TESTS {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".globaltrustedtests"; }

            @Override
            public String getDescription() {
                return "[BOOLEAN] When true, trusted tests will be shared by all candidates, when disabled, trusted tests will be local to each candidate (except from trusted tests from the original candidate).";
            }
        },
        ICEBAR_TIMEOUT {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".timeout"; }

            @Override
            public String getDescription() {
                return "[NON NEGATIVE INTEGER] Timeout (in minutes) for ICEBAR, a `0` value means no timeout.";
            }
        },
        ICEBAR_UPDATE_AREPAIR_SCOPE_FROM_ORACLE {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".updatescopefromoracle"; }

            @Override
            public String getDescription() {
                return "[BOOLEAN] When true, ARepair scope will be updated from the maximum scope used in the oracle (when false, this will only be done for initial tests).";
            }
        },
        ICEBAR_KEEP_GOING_ON_AREPAIR_NPE {
            @Override
            public String getKey() { return ICEBAR_PREFIX + ".keepgoingarepairnpe"; }

            @Override
            public String getDescription() {
                return "[BOOLEAN] When true, ICEBAR will treat ARepair NullPointerExceptions errors as no repair, and continue the search." +
                        "\n\tThis option only affect the original version of ARepair, if a patched version (the one accompanying ICEBAR for example) is used, this option is unnecessary.";
            }
        },
        ICEBAR_LOGGING_CONSOLE_VERBOSITY {
            @Override
            public String getKey() {
                return ICEBAR_PREFIX + ".logging.console.verbosity";
            }

            @Override
            public String getDescription() {
                return "[OFF|INFO|FINE] Establish the level of console verbosity:" +
                        "\n\tOFF: no logging" +
                        "\n\tINFO: only main process steps are logged" +
                        "\n\tFINE: everything is logged";
            }
        },
        ICEBAR_LOGGING_FILE_VERBOSITY {
            @Override
            public String getKey() {
                return ICEBAR_PREFIX + ".logging.file.verbosity";
            }

            @Override
            public String getDescription() {
                return "[OFF|INFO|FINE] Establish the level of file verbosity:" +
                        "\n\tOFF: no logging" +
                        "\n\tINFO: only main process steps are logged" +
                        "\n\tFINE: everything is logged";
            }
        },
        ICEBAR_SAVE_ALL_TEST_SUITES {
            @Override
            public String getKey() {
                return ICEBAR_SEARCH_PREFIX + ".savealltestsuites";
            }

            @Override
            public String getDescription() {
                return "[BOOLEAN] When true, all used test suites will be saved to files, not only the one that lead to a proper fix.";
            }
        }
        ;
        private static final String ICEBAR_PREFIX = "icebar";
        private static final String ICEBAR_SEARCH_PREFIX = ICEBAR_PREFIX + ".search";
        private static final String BEAFIX_PREFIX = ICEBAR_PREFIX + ".tools.beafix";
        private static final String AREPAIR_PREFIX = ICEBAR_PREFIX + ".tools.arepair";
        public abstract String getKey();
        public abstract String getDescription();
}
