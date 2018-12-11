The software [porta](http://porta.zib.de/) is used to analyze polytopes and polyhedra. One of its drawback is that the variables in the formulation used to define the polytopes must be named x1, x2, x3, ... This is not user-friendly when you consider a formulation which contains different variables names with potentially several indices. It makes it difficult to:
* write your formulation so that porta understands it;
* understand the outputs returned by porta.

The aim of the porta-java project is to alleviate these drawbacks.

For a given polytope P, this project currently enables to:
* find the integer points in P;
* find the dimension and the hyperplans which include P;
* find the [facets](https://en.wikipedia.org/wiki/Face_(geometry)#Facet_or_(n-1)-face) of P.

In the context of this project, a polytope corresponds to the convex hull of a set of integer points.

# How to use porta-java?

## Prerequisites
* It currently does not work on windows (contributions on that part or another are welcome).
* Porta commands must be in your path.
 
## How to define a polytope? 
1. Provide a linear formulation: the polytope will correspond to the convex hull of its feasible integer solutions.

&nbsp;&nbsp;Advantage: usually easier to write;

2. Provide a set of integer points: the polytope will directly correspond to their convex hull.

&nbsp;&nbsp;Advantage: usually quicker (it may be long for porta to find the integer points associated to a formulation).
  
### How to define a polytope by providing a linear formulation?
There are two ways of providing a linear formulation
 
#### 1/2 - Providing a formulation from an LP file
The easiest way to provide a formulation is to import a [CPLEX lp file](http://lpsolve.sourceforge.net/5.1/CPLEX-format.htm) using the LPReader class :

	LPReader formulation = new LPReader("myformulation.lp");

&nbsp;&nbsp;Drawback: it may not be convenient to generate an lp file for each instance you want to study. In that case you can use an *AbstractFormulation* that will directly read your input files (or in which your instances are hard coded).
 
#### 2/2 - Providing a formulation by extending the class AbstractFormulation
To extend AbstractFormulation, you need to implement two abstract methods :

1. *createVariables()*: register all the variables used in your formulation.

    ```
    /* Example of a createVariables() method implementation for the knapsack problem. 
     * (https://en.wikipedia.org/wiki/Knapsack_problem#Definition)
     */
    protected void createVariables() {
		
      for(int i = 1; i <= n; ++i)
    
        /* Add the variable named "xi" which takes integer values between 0 and 1 */
        this.registerVariable(new Variable("x" + i, 0, 1));
    }
    ```


2. *getConstraints()*: create a String which contains all your formulation constraints.

    ```
    /* Example of getConstraints() method implementation for the knapsack problem. 
    * Here there is only one constraint: sum_i wi xi <= K
    */
    public String getConstraints() throws UnknownVariableName {
	
      /* Remarks: 
       * - the weight of item i is stored in position i-1 of array w[];
       * - do not use '*' to multiply a variable and its coefficient;
       * - the constraints must be separated by '\n' (here there is only one constraint) 
       * - portaName() returns the name associated to your variable for porta
       */
      String constraint = w[1 - 1] + " " + portaName("x" + 1);
		
      /* For each item */
      for(int i = 2; i <= n; ++i)
        constraint += " + " + w[i - 1] + " " + portaName("x" + i);
    	
      constraint += " <= " + K;
    		
      return constraint;
    }
    ```
  
### How to define a polytope by providing integer points?

To define a polytope by providing integer points, create a class which extends AbstractIntegerPoints and implements two abstract methods:

1. *createVariables()*: register all the variables (an integer point will be defined by assigning a value to each variable).

2. *createIntegerPoints()*: register all the integer points through the class IntegerPoint and the method AbstractIntegerPoints.addIntegerPoint(IntegerPoint).

	```
	/* Example of createIntegerPoints() method implementation for the knapsack problem */  	
	public void createIntegerPoints() throws UnknownVariableName {
	
		/* First, add the solutions associated to an empty knapsack
		 * (an integer point is created with all its variables equal to 0) 
		 */
		IntegerPoint emptyKnapsack = new IntegerPoint(this);
		addIntegerPoint(emptyKnapsack);

		/* Recursively find all the other solutions */
		findFeasibleSolutions(K, n, new ArrayList<Integer>());
		
		/* Remark: to change the value of a variable, use the method IntegerPoint.setVariable(String variableName, int value);
		 * Example: 
		 *   IntegerPoint point = new IntegerPoint(this);
		 *   point.setVariable("x1", 1);
		 */
	}
	```
## How to analyze a polytope once it is defined?
   
### Get its integer points
  
    System.out.println(polytope.getIntegerPoints());
      
### Get its dimension
  
    System.out.println(polytope.getDimension());
      
### Get its facets 
  
    System.out.println(polytope.getFacets());
      
## The knapsack example
  The classes *KnapsackFormulation* and *KnapsackIntegerPoints* from the package "example" are good entry points to see how to use the software.
  
# Feedbacks are welcome
We are in the early stage of this project so bugs and limitations should be expected. 

I would be most grateful if you could report to me any problem, comment or suggestion that you might have (through the github issue system or directly by email at zacharie.ales[at]ensta[dot]fr).
  
# Considered new features
* Better handling of the .lp format (in particular keywords "free" and "infinity" are  not currently taken into account);
* Manage more porta features and options;
* Get the results in latex;
* Simplification of the facets expression;
* Comptatibility with windows.