package sjdb;
import java.util.ArrayList;
public class Test {
	private Catalogue catalogue;

	public Test() {
	}

	public static void main(String[] args) throws Exception {
		Catalogue catalogue = createCatalogue();
		Inspector inspector = new Inspector();
		Estimator estimator = new Estimator();
		Operator plan = query(catalogue);
		plan.accept(estimator);
		plan.accept(inspector);
//		Optimiser optimiser = new Optimiser(catalogue);
//		Operator planopt = optimiser.optimise(plan);
//		planopt.accept(estimator);
//		planopt.accept(inspector);
	}

	public static Catalogue createCatalogue() {
		Catalogue cat = new Catalogue();
		cat.createRelation("A", 100);
		cat.createAttribute("A", "a1", 100);
		cat.createAttribute("A", "a2", 15);
		cat.createRelation("B", 150);
		cat.createAttribute("B", "b1", 150);
		cat.createAttribute("B", "b2", 100);
		cat.createAttribute("B", "b3", 5);
		cat.createRelation("C", 50);
		cat.createAttribute("C", "c1", 50);
		cat.createAttribute("C", "c2", 10);
		cat.createAttribute("C", "c3", 5);
		return cat;
	}

	public static Operator query(Catalogue cat) throws Exception {
		Scan a = new Scan(cat.getRelation("A"));
		Scan b = new Scan(cat.getRelation("B"));
		Scan c = new Scan(cat.getRelation("C"));
		Product p1 = new Product(a, b);
		Select s1 = new Select(p1, new Predicate(new Attribute("a2"), new Attribute("b3")));
		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		atts.add(new Attribute("a2"));
		atts.add(new Attribute("b1"));
		Join j = new Join(c, s1, new Predicate(new Attribute("c1"), new Attribute("b1")));
		Project plan = new Project(j, atts);
		return plan;
	}
}