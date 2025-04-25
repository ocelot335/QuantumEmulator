package org.example.model.gate.oracle;

import lombok.Getter;

import java.io.Serializable;
import java.util.Set;

@Getter
public class OracleDefinition implements Serializable {
    private final String name;
    private final Set<Integer> statesWhereOutputIsOne;

    public OracleDefinition(String name, Set<Integer> statesWhereOutputIsOne) {
        this.name = name;
        this.statesWhereOutputIsOne = statesWhereOutputIsOne;
    }

}