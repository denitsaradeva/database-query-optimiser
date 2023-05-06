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
        NamedRelation relation = (NamedRelation) op.getRelation();
        Scan newScan = new Scan(relation);
        estimator.visit(newScan);

        ArrayList<Attribute> neededAttrs = new ArrayList<>();

        for (Attribute attribute : newScan.getOutput().getAttributes()) {
            if (attributes.contains(attribute)) {
                neededAttrs.add(attribute);
            }
        }

        attributes.removeAll(neededAttrs);
        Project newProject = new Project(newScan, neededAttrs);
        estimator.visit(newProject);
        operators.push(newProject);
        projects.push(newProject);
    }

    @Override
    public void visit(Project op) {
        //ignoring projects for this step
    }

    @Override
    public void visit(Select op) {
        Operator lastProject =  operators.pop();

        Select newSelect = new Select(lastProject, op.getPredicate());
        estimator.visit(newSelect);

        Attribute leftAttribute = op.getPredicate().getLeftAttribute();
        Attribute rightAttribute = op.getPredicate().getRightAttribute();

        List<Attribute> newAttrs = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        if (projects.size() > 0) {
            Project project = projects.peek();
            List<Attribute> attrs = project.getAttributes();

            if (op.getPredicate().equalsValue()) {
                if (attrs.contains(leftAttribute)) {
                    int index = attrs.indexOf(leftAttribute);
                    indices.add(index);
                }
            } else {
                if (attrs.contains(leftAttribute) && attrs.contains(rightAttribute)) {
                    int leftIndex = attrs.indexOf(leftAttribute);
                    indices.add(leftIndex);
                    int rightIndex = attrs.indexOf(rightAttribute);
                    indices.add(rightIndex);
                }
            }

            for (int i = 0; i < attrs.size(); i++) {
                if (!indices.contains(i)) {
                    newAttrs.add(attrs.get(i));
                }
            }

            Project newProject = new Project(newSelect, newAttrs);
            estimator.visit(newProject);
            projects.push(newProject);
            operators.push(newProject);
        } else {
            System.out.println("ugh");
        }
    }

    @Override
    public void visit(Product op) {
        Operator rightProject = operators.pop();
        Operator leftProject = operators.pop();
        Product newProduct = new Product(leftProject, rightProject);
        estimator.visit(newProduct);

        List<Attribute> newAttrs = leftProject.getOutput().getAttributes();
        newAttrs.addAll(rightProject.getOutput().getAttributes());
        Project newProject = new Project(newProduct, newAttrs);
        estimator.visit(newProject);
        operators.push(newProject);
    }

    @Override
    public void visit(Join op) {
        Attribute leftAttribute = op.getPredicate().getLeftAttribute();
        Attribute rightAttribute = op.getPredicate().getRightAttribute();

        Operator rightProject = operators.pop();
        Operator leftProject = operators.pop();

        Join newJoin = new Join(leftProject, rightProject, op.getPredicate());
        estimator.visit(newJoin);

        List<Attribute> rightAttrs = rightProject.getOutput().getAttributes();
        List<Attribute> leftAttrs = leftProject.getOutput().getAttributes();


        ArrayList<Attribute> newAttrs = new ArrayList<>();
        ArrayList<Integer> leftIndices = new ArrayList<>();
        ArrayList<Integer> rightIndices = new ArrayList<>();


        if (rightAttrs.contains(leftAttribute) && leftAttrs.contains(rightAttribute)) {
            rightIndices.add(rightAttrs.indexOf(leftAttribute));
            leftIndices.add(leftAttrs.indexOf(rightAttribute));
        } else if (leftAttrs.contains(leftAttribute) && rightAttrs.contains(rightAttribute)) {
            rightIndices.add(rightAttrs.indexOf(rightAttribute));
            leftIndices.add(leftAttrs.indexOf(leftAttribute));
        }

        for (int i = 0; i < leftAttrs.size(); i++) {
            if (!leftIndices.contains(i)) {
                newAttrs.add(leftAttrs.get(i));
            }
        }

        for (int i = 0; i < rightAttrs.size(); i++) {
            if (!rightIndices.contains(i)) {
                newAttrs.add(rightAttrs.get(i));
            }
        }

        Project newProject = new Project(newJoin, newAttrs);
        estimator.visit(newProject);

        projects.push(newProject);
        operators.push(newProject);
    }

    public Operator getRoot() {
        return operators.peek();
    }
}
