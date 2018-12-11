package formulation.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import exception.UnknownCommandException;
import exception.UnknownVariableName;
import formulation.AbstractIntegerPoints;
import formulation.IntegerPoint;
import formulation.Variable;

/**
 * Definition of the polytope associated to a knapsack problem thanks to its feasible solutions (i.e., integer points)
 * 
 * All the feasible solutions are enumerated in method @{code createIntegerPoints} through dynamic programming.
 * 
 * @author zach
 *
 */
public class KnapsackIntegerPoints extends AbstractIntegerPoints{

	/** Number of items **/
	int n;

	/** Weight of the items */
	int[] w;

	/** Value of the items */
	int[] p;

	/** Maximal weight of the knapsack */
	int K;

	/**
	 * Create a Knapsack object
	 * @param n
	 * @param w
	 * @param p
	 * @throws UnknownCommandException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public KnapsackIntegerPoints(int n, int K, int[] w, int[] p) throws UnknownCommandException, IOException, InterruptedException {

		super();
		this.n = n;
		this.K = K;
		this.w = w;
		this.p = p;
	}

	@Override
	public void createIntegerPoints() throws UnknownVariableName {

		/* Add the solution associated to an empty knapsack */
		IntegerPoint emptyKnapsack = new IntegerPoint(this);
		addIntegerPoint(emptyKnapsack);

		/* Recursively find all the other solutions */
		findFeasibleSolutions(K, n, new ArrayList<Integer>());
	}

	/**
	 * Find all the knapsack feasible integer solutions which:
	 * - contain the objects in {@code objectsInTheKnapsack}
	 * - contain objects of id between 0 and {@code nextObject} - 1
	 * 
	 * @param remainingSpace Remaining available space in the knapsack
	 * @param nextObject Id of the next object that we will want to add in the knapsack (between 1 and n)
	 * @param objectsInTheKnapsack Objects already in the knapsack
	 * @throws UnknownVariableName
	 */
	private void findFeasibleSolutions(int remainingSpace, int nextObject, List<Integer> objectsInTheKnapsack) throws UnknownVariableName 
	{ 

		/* If finding a new solution is still possible */
		if (nextObject != 0 && remainingSpace != 0) {

			/* Find the solutions in which the object nextObject is not included */
			findFeasibleSolutions(remainingSpace, nextObject-1, objectsInTheKnapsack); 

			/* If there is space for the object nextObject */
			if (w[nextObject-1] <= remainingSpace) { 

				/* Add the object to the knapsack*/
				objectsInTheKnapsack.add(nextObject);

				/* Add the corresponding integer solution */
				IntegerPoint point = new IntegerPoint(this);

				for(Integer objectId: objectsInTheKnapsack)
					point.setVariable("x" + objectId, 1);

				addIntegerPoint(point);
				
				System.out.println(point);

				/* Find other solutions in which the object nextObject is included */
				findFeasibleSolutions(remainingSpace-w[nextObject-1], nextObject-1, objectsInTheKnapsack);

				objectsInTheKnapsack.remove(Integer.valueOf(nextObject));

			}	
		}
	} 

	@Override
	protected void createVariables() {

		/* Register the knapsack formulation variables */
		for(int i = 1; i <= n; ++i)
			this.registerVariable(new Variable("x" + i, 0, 1));
	}

	
	/* Possible uses of this software */
	public enum Use{
		
		/* Get the dimension of the integer polytope of a formulation, also returns the hyperplans which include the polytope */
		DIMENSION,

		/* Get feasible integer solutions of the problem */
		INTEGER_POINTS, 

		/* Get the facets of the problem */
		FACETS
	}


	public static void main(String[] args) {

		try {

			KnapsackIntegerPoints polytope = new KnapsackIntegerPoints(6, 12, new int[] {1, 3, 2, 4, 6, 4}, new int[] {1, 5, 3, 5, 2, 3});

			Use whatToDo = Use.FACETS;
		
			switch(whatToDo) {
			case DIMENSION: System.out.println(polytope.getDimension()); break;
			case FACETS: System.out.println(polytope.getFacets()); break;
			case INTEGER_POINTS: System.out.println(polytope.getIntegerPoints()); break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
