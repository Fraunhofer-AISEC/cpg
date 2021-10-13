package de.fraunhofer.aisec.cpg.graph.statements;

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConditionalBranchStatement extends Statement {
    private LabelStatement defaultTargetLabel;
    private List<Pair<Expression, LabelStatement>> conditionalTargets = new ArrayList<>();

    public LabelStatement setDefaultTargetLabel() {
        return defaultTargetLabel;
    }

    public void setDefaultTargetLabel(LabelStatement defaultTargetLabel) {
        this.defaultTargetLabel = defaultTargetLabel;
    }

    public List<Pair<Expression, LabelStatement>> getConditionalTargets() {
        return conditionalTargets;
    }

    public void addConditionalTarget(Expression condition, LabelStatement label) {
        conditionalTargets.add(new Pair<>(condition, label));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConditionalBranchStatement)) {
            return false;
        }
        ConditionalBranchStatement that = (ConditionalBranchStatement) o;
        return super.equals(that)
                && Objects.equals(conditionalTargets, that.conditionalTargets)
                && Objects.equals(defaultTargetLabel, that.defaultTargetLabel);
    }
}
