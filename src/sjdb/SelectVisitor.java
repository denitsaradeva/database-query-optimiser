package sjdb;

import java.util.*;

public class SelectVisitor implements PlanVisitor {
    List<Select> selects;
    Stack<Operator> operators;
    Stack<NamedRelation> relationOrder;
    Estimator estimator;

    public SelectVisitor(Stack<NamedRelation> relationOrder, ArrayList<Select> selects, Estimator estimator) {
        this.selects = new ArrayList<>(selects);
        this.operators = new Stack<>();
        this.relationOrder = relationOrder;
        this.estimator = estimator;
    }

    @Override
    public void visit(Scan op) {
//        System.out.println("VISIT SCAN:");
        NamedRelation relation = relationOrder.pop();
        Scan newScan = new Scan(relation);
        estimator.visit(newScan);

        Iterator<Select> iter = selects.iterator();
        List<Attribute> attributes = relation.getAttributes();

        boolean pushSelect = false;
        ArrayList<Integer> indices = new ArrayList<>();
        int count = 0;
        while (iter.hasNext()) {
            Select currentSelect = iter.next();
            if (currentSelect.getPredicate().equalsValue()) {
                if (attributes.contains(currentSelect.getPredicate().getLeftAttribute())) {
                    Select newSelect = new Select(newScan, new Predicate(currentSelect.getPredicate().getLeftAttribute()
                            , currentSelect.getPredicate().getRightValue()));
                    estimator.visit(newSelect);
                    operators.push(newSelect);
                    pushSelect = true;
                    indices.add(count);
                }
            } else {
                if (attributes.contains(currentSelect.getPredicate().getLeftAttribute())
                        && attributes.contains(currentSelect.getPredicate().getRightAttribute())) {
                    Select newSelect = new Select(newScan, new Predicate(currentSelect.getPredicate().getLeftAttribute()
                            , currentSelect.getPredicate().getRightAttribute()));
                    estimator.visit(newSelect);
                    operators.push(newSelect);
                    pushSelect = true;
                    indices.add(count);
                }
            }
            count++;
        }
        if (!pushSelect) {
            operators.push(newScan);
        }

        for (int i = indices.size() - 1; i >= 0; i--) {
            selects.remove((int) indices.get(i));
        }

//        System.out.println(operators);
    }

    @Override
    public void visit(Project op) {
//        System.out.println("VISIT PROJECT:");
        Operator previous = operators.pop();
        Project newProject = new Project(previous, op.getAttributes());
        estimator.visit(newProject);
        operators.push(newProject);
//        System.out.println(operators);
    }

    @Override
    public void visit(Select op) {
        //ignoring selects
//        System.out.println("VISIT SELECT:");
//        System.out.println("--No change");
    }

    @Override
    public void visit(Product op) {
//        System.out.println("VISIT PRODUCT:");
        Operator right = operators.pop();
        Operator left = operators.pop();
        Product newProduct = new Product(left, right);
        estimator.visit(newProduct);
        operators.push(newProduct);

        Iterator<Select> iter = selects.iterator();
        ArrayList<Attribute> attributes = new ArrayList<>(newProduct.getLeft().getOutput().getAttributes());


        ArrayList<Integer> indices = new ArrayList<>();
        int count = 0;
        while (iter.hasNext()) {
            Select currentSelect = iter.next();

            if (currentSelect.getPredicate().equalsValue()) {
                if (attributes.contains(currentSelect.getPredicate().getLeftAttribute())) {
                    Select newSelect = new Select(newProduct
                            , new Predicate(currentSelect.getPredicate().getLeftAttribute()
                            , currentSelect.getPredicate().getRightValue()));
                    estimator.visit(newSelect);
                    operators.pop();
                    operators.push(newSelect);
                    indices.add(count);
                }
            } else {
                attributes.addAll(newProduct.getRight().getOutput().getAttributes());
                if (attributes.contains(currentSelect.getPredicate().getLeftAttribute())
                        && attributes.contains(currentSelect.getPredicate().getRightAttribute())) {
                    Select newSelect = new Select(newProduct
                            , new Predicate(currentSelect.getPredicate().getLeftAttribute()
                            , currentSelect.getPredicate().getRightAttribute()));
                    estimator.visit(newSelect);
                    operators.pop();
                    operators.push(newSelect);
                    indices.add(count);
                }
            }

            count++;
        }

        for (int i = indices.size() - 1; i >= 0; i--) {
            selects.remove((int) indices.get(i));
        }
//        System.out.println(operators);
    }

    @Override
    public void visit(Join op) {
        //not applicable here
//        System.out.println("VISIT JOIN:");
//        System.out.println("--No change");
    }

    public Operator getRoot() {
        return this.operators.peek();
    }
}
