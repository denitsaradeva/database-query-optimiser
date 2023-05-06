package sjdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Optimiser {

    private Catalogue catalogue;

    private Estimator estimator;

    private ArrayList<Select> selects;

    private Stack<NamedRelation> relations;

    private ArrayList<Attribute> operators;

    public Optimiser(Catalogue catalogue) {
        this.catalogue = catalogue;
        this.estimator = new Estimator();
    }

    public Operator optimise(Operator root) {

        root.accept(estimator);

        //Getting all relations, selects, and attributes
        StatsVisitor statsVisitor = new StatsVisitor(estimator);
        root.accept(statsVisitor);
        selects = statsVisitor.getSelects();
        determineRelationOrder(statsVisitor);
        operators = new ArrayList<>(statsVisitor.getAttributes());

//        generatePermutations(relations, new Stack<>(), permutations);

        //pushes selects down the tree
        SelectVisitor selectVisitor = new SelectVisitor(relations, selects, estimator);
        statsVisitor.getRoot().accept(selectVisitor);

        //introduces joins
        JoinVisitor joinVisitor = new JoinVisitor(estimator, catalogue);
        selectVisitor.getRoot().accept(joinVisitor);

        //gathering all attributes after the new changes
        joinVisitor.getRoot().accept(statsVisitor);

        //pushes projects down the tree
        ProjectVisitor projectVisitor = new ProjectVisitor(operators, estimator);
        joinVisitor.getRoot().accept(projectVisitor);


        return projectVisitor.getRoot();
    }

    private void determineRelationOrder(StatsVisitor statsVisitor) {
        relations = statsVisitor.getRelations();

        ArrayList<NamedRelation> relationList = new ArrayList<>(relations);
        relationList.sort((a, b) -> b.getTupleCount() - a.getTupleCount());

        relations.clear();
        relations.addAll(relationList);
    }


//    private static void generatePermutations(List<NamedRelation> relations, List<NamedRelation> currentPermutation,
//                                             List<List<NamedRelation>> permutations) {
//        if (currentPermutation.size() == relations.size()) {
//            permutations.add(new ArrayList<>(currentPermutation));
//            return;
//        }
//
//        for (NamedRelation relation : relations) {
//            if (currentPermutation.contains(relation)) {
//                continue;
//            }
//            currentPermutation.add(relation);
//            generatePermutations(relations, currentPermutation, permutations);
//            currentPermutation.remove(currentPermutation.size() - 1);
//        }
//    }
}
