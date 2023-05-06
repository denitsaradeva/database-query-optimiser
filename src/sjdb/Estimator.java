package sjdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Stack;

public class Estimator implements PlanVisitor {

    int cost = 0;

    int prevCost = 0;

    public Estimator() {
        // empty constructor
    }

    /*
     * Create output relation on Scan operator
     *
     * Example implementation of visit method for Scan operators.
     */
    public void visit(Scan op) {
        Relation input = op.getRelation();
        Relation output = new Relation(input.getTupleCount());

        Iterator<Attribute> iter = input.getAttributes().iterator();
        while (iter.hasNext()) {
            output.addAttribute(new Attribute(iter.next()));
        }

        op.setOutput(output);
    }

    public void visit(Project op) {
        List<Attribute> input = op.getAttributes();
        Relation output = new Relation(op.getInput().getOutput().getTupleCount());

        Iterator<Attribute> iter = input.iterator();
        while (iter.hasNext()) {
            Attribute current = iter.next();

//            System.out.println(current);
//            System.out.println("-- " + op.getInput().getOutput().getAttributes());
            output.addAttribute(new Attribute(current.getName(),
                    op.getInput().getOutput().getAttribute(current).getValueCount()));
        }

        op.setOutput(output);

    }

    public void visit(Select op) {
        Predicate predicate = op.getPredicate();
        Relation output = op.getInput().getOutput();
        Relation updatedOutput;


        Attribute leftAttr = op.getPredicate().getLeftAttribute();
        Attribute rightAttr = op.getPredicate().getRightAttribute();
        Attribute newLeft;
        Attribute newRight;
        if (predicate.equalsValue()) {
            newLeft = new Attribute(leftAttr.getName(), 1);
            updatedOutput = new Relation(output.getTupleCount()
                    / (output.getAttribute(leftAttr).getValueCount()));

            Iterator<Attribute> iter = output.getAttributes().iterator();
            while (iter.hasNext()) {
                Attribute current = iter.next();
                if (current.getName().equals(leftAttr.getName())) {
                    updatedOutput.addAttribute(new Attribute(newLeft));
                } else {
                    updatedOutput.addAttribute(new Attribute(current));
                }
            }
        } else {
//            System.out.println(rightAttr);
//            System.out.println(leftAttr);
//            System.out.println(output.getAttributes());
            newRight = new Attribute(rightAttr.getName()
                    , Math.min(output.getAttribute(rightAttr).getValueCount(), output.getAttribute(leftAttr).getValueCount()));

            newLeft = new Attribute(leftAttr.getName()
                    , Math.min(output.getAttribute(rightAttr).getValueCount(), output.getAttribute(leftAttr).getValueCount()));

            updatedOutput = new Relation(output.getTupleCount()
                    / Math.max(output.getAttribute(rightAttr).getValueCount(), output.getAttribute(leftAttr).getValueCount()));

            Iterator<Attribute> iter = output.getAttributes().iterator();
            while (iter.hasNext()) {
                Attribute current = iter.next();
                if (current.getName().equals(rightAttr.getName())) {
                    updatedOutput.addAttribute(new Attribute(newRight));
                } else if (current.getName().equals(leftAttr.getName())) {
                    updatedOutput.addAttribute(new Attribute(newLeft));
                } else {
                    updatedOutput.addAttribute(new Attribute(current));
                }
            }
        }

        cost += updatedOutput.getTupleCount();
        prevCost = updatedOutput.getTupleCount();
        op.setOutput(updatedOutput);
    }

    public void visit(Product op) {
        Relation output = new Relation((op.getInputs().get(0).getOutput().getTupleCount())
                * (op.getInputs().get(1).getOutput().getTupleCount()));

        Iterator<Attribute> left = op.getInputs().get(0).getOutput().getAttributes().iterator();
        while (left.hasNext()) {
            output.addAttribute(new Attribute(left.next()));
        }
        Iterator<Attribute> right = op.getInputs().get(1).getOutput().getAttributes().iterator();
        while (right.hasNext()) {
            output.addAttribute(new Attribute(right.next()));
        }

        cost += output.getTupleCount();
        prevCost = output.getTupleCount();
        op.setOutput(output);
    }

    public void visit(Join op) {
        Attribute leftPred = op.getPredicate().getLeftAttribute();
        Attribute rightPred = op.getPredicate().getRightAttribute();

        Relation output = new Relation(((op.getInputs().get(0).getOutput().getTupleCount())
                * (op.getInputs().get(1).getOutput().getTupleCount())
                / Math.max(op.getInputs().get(1).getOutput().getAttribute(leftPred).getValueCount()
                , op.getInputs().get(0).getOutput().getAttribute(rightPred).getValueCount())));

        Iterator<Attribute> left = op.getInputs().get(0).getOutput().getAttributes().iterator();
        while (left.hasNext()) {
            Attribute current = left.next();

            Attribute updatedAttr = new Attribute(current.getName(),
                    Math.min(op.getInputs().get(0).getOutput().getAttribute(rightPred).getValueCount()
                            , op.getInputs().get(1).getOutput().getAttribute(leftPred).getValueCount()));
            output.addAttribute(updatedAttr);
        }

        Iterator<Attribute> right = op.getInputs().get(1).getOutput().getAttributes().iterator();
        while (right.hasNext()) {
            Attribute current = right.next();

            Attribute updatedAttr = new Attribute(current.getName(),
                    Math.min(op.getInputs().get(0).getOutput().getAttribute(rightPred).getValueCount()
                            , op.getInputs().get(1).getOutput().getAttribute(leftPred).getValueCount()));
            output.addAttribute(updatedAttr);
        }

        cost += output.getTupleCount();
        prevCost = output.getTupleCount();

        op.setOutput(output);

    }

    public int getValue() {
        return cost - prevCost;
    }


}
