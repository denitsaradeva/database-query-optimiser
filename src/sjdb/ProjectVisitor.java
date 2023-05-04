package sjdb;

import java.util.*;

public class ProjectVisitor implements PlanVisitor {

    private ArrayList<Attribute> attributes;

    private Stack<Operator> operators;

    private Estimator estimator;

    private Stack<Project> projects;

    public ProjectVisitor(ArrayList<Attribute> attributes, Estimator estimator) {
        this.attributes = attributes;
        this.estimator = estimator;
        this.operators = new Stack<>();
        this.projects = new Stack<>();
    }

    @Override
    public void visit(Scan op) {
        System.out.println("VISIT SCAN:");
        NamedRelation relation = (NamedRelation) op.getRelation();
        Scan newScan = new Scan(relation);
        estimator.visit(newScan);

        ArrayList<Attribute> neededAttrs = new ArrayList<>();

        for (Attribute attribute : newScan.getOutput().getAttributes()) {
            if (attributes.contains(attribute)) {
                neededAttrs.add(attribute);
            }
        }

        if (neededAttrs.size() > 0) {
            attributes.removeAll(neededAttrs);
            Project newProject = new Project(newScan, neededAttrs);
            operators.push(newProject);
            projects.push(newProject);
        } else {
            operators.push(newScan);
        }

        System.out.println(operators);
    }

    @Override
    public void visit(Project op) {
        System.out.println("VISIT PROJECT:");
        projects.push(op);
        Project newProject = new Project(op.getInput(), op.getAttributes());
        estimator.visit(newProject);
        operators.push(newProject);

        System.out.println(operators);
    }

    @Override
    public void visit(Select op) {
        System.out.println("VISIT SELECT:");
        Select newSelect = new Select(op.getInput(), op.getPredicate());
        estimator.visit(newSelect);

        Attribute leftAttribute = op.getPredicate().getLeftAttribute();
        Attribute rightAttribute = op.getPredicate().getRightAttribute();

        if (projects.size() > 0) {
            Project project = projects.peek();
            List<Attribute> attrs = project.getAttributes();

            if (op.getPredicate().equalsValue()) {
                if (attrs.contains(leftAttribute)) {
                    attrs.remove(leftAttribute);
                }
            } else {
                if (attrs.contains(leftAttribute) && attrs.contains(rightAttribute)) {
                    attrs.remove(leftAttribute);
                    attrs.remove(rightAttribute);
                }
            }

            Project newProject = new Project(newSelect, attrs);
            estimator.visit(newProject);
            projects.push(newProject);
            operators.push(newSelect);
            operators.push(newProject);
        } else {
            System.out.println("ugh");
        }

        System.out.println(operators);
    }

    @Override
    public void visit(Product op) {
        System.out.println("VISIT PRODUCT:");
        Product newProduct = new Product(op.getLeft(), op.getRight());
        estimator.visit(newProduct);

        List<Attribute> newAttrs = op.getLeft().getOutput().getAttributes();
        newAttrs.addAll(op.getRight().getOutput().getAttributes());
        Project newProject = new Project(newProduct, newAttrs);
        estimator.visit(newProject);

        operators.push(newProduct);
        operators.push(newProject);

        System.out.println(operators);
    }

    @Override
    public void visit(Join op) {
        System.out.println("VISIT JOIN:");
        Join newJoin = new Join(op.getLeft(), op.getRight(), op.getPredicate());
        estimator.visit(newJoin);

        Attribute leftAttribute = op.getPredicate().getLeftAttribute();
        Attribute rightAttribute = op.getPredicate().getRightAttribute();

        Project rightProject = projects.pop();
        Project leftProject = projects.pop();
        List<Attribute> rightAttrs = rightProject.getAttributes();
        List<Attribute> leftAttrs = leftProject.getAttributes();

        if (rightAttrs.contains(leftAttribute) && leftAttrs.contains(rightAttribute)) {
            rightAttrs.remove(leftAttribute);
            leftAttrs.remove(rightAttribute);
        } else if (leftAttrs.contains(leftAttribute) && rightAttrs.contains(rightAttribute)) {
            leftAttrs.remove(leftAttribute);
            rightAttrs.remove(rightAttribute);
        }

        ArrayList<Attribute> combinedAttrs = new ArrayList<>(leftAttrs);
        combinedAttrs.addAll(rightAttrs);

        Project newProject = new Project(newJoin, combinedAttrs);
        estimator.visit(newProject);

        projects.push(newProject);
//        operators.push(newJoin);
        operators.push(newProject);

        System.out.println(operators);
    }

    public Operator getRoot(){
        return operators.peek();
    }
}
