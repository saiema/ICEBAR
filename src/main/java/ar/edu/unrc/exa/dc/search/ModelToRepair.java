package ar.edu.unrc.exa.dc.search;

import java.nio.file.Path;

public class ModelToRepair {

    private final Path modelToRepair;
    private final String modelToRepairName;
    private final Path oracle;

    public ModelToRepair(Path modelToRepair, Path oracle) {
        this.modelToRepair = modelToRepair;
        this.modelToRepairName = modelToRepair.getFileName().toString().replace(".als","");
        this.oracle = oracle;
    }

    public Path path() {
        return this.modelToRepair;
    }

    public String name() {
        return this.modelToRepairName;
    }

    public Path oraclePath() {
        return this.oracle;
    }

}
