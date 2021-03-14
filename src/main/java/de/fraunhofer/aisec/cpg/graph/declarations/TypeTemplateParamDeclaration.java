package de.fraunhofer.aisec.cpg.graph.declarations;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TemplateParameter;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

public class TypeTemplateParamDeclaration extends Declaration implements TemplateParameter<ParameterizedType> {

    @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
    @SubGraph("AST")
    protected List<PropertyEdge<ParameterizedType>> possibleInitializations = new ArrayList<>();

    @Relationship(value = "DEFAULT", direction = "OUTGOING")
    @SubGraph("AST")
    private ParameterizedType defaultType;

    public List<ParameterizedType> getPossibleInitializations() {
        return unwrap(this.possibleInitializations);
    }

    public List<PropertyEdge<ParameterizedType>> getPossibleInitializationsPropertyEdge() {
        return this.possibleInitializations;
    }

    public void addPossibleInitialization(ParameterizedType parameterizedType) {
        PropertyEdge<ParameterizedType> propertyEdge = new PropertyEdge<>(this, parameterizedType);
        propertyEdge.addProperty(Properties.INDEX, this.possibleInitializations.size());
        this.possibleInitializations.add(propertyEdge);
    }

    public ParameterizedType getDefault() {
        return defaultType;
    }

    public void setDefault(ParameterizedType defaultType) {
        this.defaultType = defaultType;
    }
}
