package com.se.bds.core;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    ApplicationModules modules = ApplicationModules.of(CoreMacroserviceApplication.class);

    @Test
    void verifyModularStructure() {
        // This single line scans your entire codebase and enforces the
        // allowedDependencies rules defined in your package-info.java files.
        modules.verify();
    }

    @Test
    void createModuleDocumentation() {
        // This generates visual architecture diagrams (PlantUML) based on your actual code!
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}