package sjdb;

import java.util.ArrayList;
import java.util.Stack;

public class StatsVisitor implements PlanVisitor {

    ArrayList<Select> selects = new ArrayList<>();

    Stack<NamedRelation> relations = new Stack<>();

    ArrayList<Attribute> attributes = new ArrayList<>();

    Stack<Operator> operators = new Stack<>();

    Estimator estimator;

    public StatsVisitor(Estimator estimator) {
        this.estimator = estimator;
    }

    @Override
    public void visit(Scan op) {
        Scan newScan = new Scan((NamedRelation) op.getRelation());
        estimator.visit(newScan);
        operators.push(newScan);

        relations.add((NamedRelation) op.getRelation());
    }

    @Override
    public void visit(Project op) {
        Operator previous = operators.pop();
        Project newProject = new Project(previous, op.getAttributes());
        estimator.visit(newProject);
        operators.push(newProject);

        attributes.addAll(op.getAttributes());
    }

    @Override
    public void visit(Select op) {
        Select newSelect = new Select(op.getInput(), op.getPredicate());
        estimator.visit(newSelect);
        operators.push(newSelect);

        selects.add(op);

        if (op.getPredicate().getLeftAttribute() != null) {
            attributes.add(op.getPredicate().getLeftAttribute());
        }
        if (op.getPredicate().getRightAttribute() != null) {
            attributes.add(op.getPredicate().getRightAttribute());
        }
    }

    @Override
    public void visit(Product op) {
        Operator right = operators.pop();
        Operator left = operators.pop();
        Product newProduct = new Product(left, right);
        estimator.visit(newProduct);
        operators.push(newProduct);
    }

    @Override
    public void visit(Join op) {
        Join newJoin = new Join(op.getLeft(), op.getRight(), op.getPredicate());
        estimator.visit(newJoin);
        Attribute leftPred = op.getPredicate().getLeftAttribute();
        Attribute rightPred = op.getPredicate().getRightAttribute();
        attributes.add(leftPred);
        attributes.add(rightPred);
        operators.push(newJoin);

    }

    public ArrayList<Select> getSelects() {
        return selects;
    }

    public Stack<NamedRelation> getRelations() {
        return relations;
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public Operator getRoot() {
        return operators.peek();
    }
}
