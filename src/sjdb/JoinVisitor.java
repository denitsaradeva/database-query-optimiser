package sjdb;

import java.util.Stack;

public class JoinVisitor implements PlanVisitor {
    Stack<Operator> operators;

    Estimator estimator;

    Catalogue catalogue;

    public JoinVisitor(Estimator estimator, Catalogue catalogue) {
        this.operators = new Stack<>();
        this.estimator = estimator;
        this.catalogue = catalogue;
    }

    @Override
    public void visit(Scan op) {
//        System.out.println("VISIT SCAN:");
//        System.out.println("--no change");
    }

    @Override
    public void visit(Project op) {
        //ignore for this step
//        System.out.println("VISIT PROJECT:");
        Operator previous = operators.pop();
        Project newProject = new Project(previous, op.getAttributes());
        estimator.visit(newProject);
        operators.push(newProject);
//        System.out.println(operators);
    }

    @Override
    public void visit(Select op) {
//        System.out.println("VISIT SELECT:");
        try {
            Product product = (Product) op.getInput();
            Join newJoin;
            if (operators.size() > 1) {
                Operator right = operators.pop();
                Operator left = operators.pop();
                newJoin = new Join(left, right, op.getPredicate());
            } else {
                newJoin = new Join(product.getLeft(), product.getRight(), op.getPredicate());
            }
            estimator.visit(newJoin);
            operators.push(newJoin);
        } catch (Exception e) {
            Select newSelect = new Select(op.getInput(), op.getPredicate());
            estimator.visit(newSelect);
            operators.push(newSelect);
        }
//        System.out.println(operators);
    }

    @Override
    public void visit(Product op) {
//        System.out.println("VISIT PRODUCT:");
//        System.out.println("--no change");
    }

    @Override
    public void visit(Join op) {
        //ignore for this step
//        System.out.println("VISIT JOIN:");
//        System.out.println("--no change");
    }

    public Operator getRoot() {
        return operators.peek();
    }
}
