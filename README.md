# porta-java

## Introduction
The software [porta](http://porta.zib.de/) is used to analyze polytopes and polyhedra. One of its drawback is that the variables in the formulation used to define the polytopes must be named x1, x2, x3, ... This is not really user-friendly when you consider a formulation which contains different variables names with potentially several indices. It makes it difficult to:
* write your formulation in a way that porta understands it;
* understand the outputs returned by porta.

The aim of the porta-java project is to alleviate these drawbacks.

For a given polytope P, this project currently enables to:
* find the integer points in P;
* find the dimension and the hyperplans which include P;
* find the [facets](https://en.wikipedia.org/wiki/Face_(geometry)#Facet_or_(n-1)-face) of P.

 ## How to use porta-java?

### Prerequisites
* It currently does not work on windows (contributions on that part or another are welcome).
* Porta commands must be in your path.
 
 ### How to define a formulation?
 Currently the only way to define an integer polytope is by providing a linear formulation.
 
 #### Defining a formulation from an LP file
The easiest way to define a formulation is to import a [CPLEX lp file](http://lpsolve.sourceforge.net/5.1/CPLEX-format.htm) using the LPReader class :

	LPReader formulation = new LPReader("myformulation.lp");

#### Defining a formulation by extending the class AbstractFormulationGenerator
A more flexible way to define formulations is to create a class for your problem which extends AbstractFormulationGenerator. In that case, you need to implement two abstract methods :

1. AbstractFormulationGenerator.createVariables(): this method is used to register all the variables used in your formulation thanks to the method AbstractFormulationGenerator.addVariable().

    ```
    /* Example of createVariables() method implementation for the knapsack problem 
     * (https://en.wikipedia.org/wiki/Knapsack_problem#Definition)
     */
    protected void createVariables() {
		
      for(int i = 1; i <= n; ++i)
    
        /* Add the variable named "xi" which takes integer values between 0 and 1 */
        this.addVariable(new Variable("x" + i, 0, 1));
    }
    ```


2. String getConstraints(): this method is used to create a String which contains all the formulation constraints thanks to the method AbstractFormulationGenerator.getConstraints().

    ```
    /* Example of getConstraints() method implementation for the knapsack problem */
    public String getConstraints() throws UnknownVariableName {
	
      /* Remarks: 
       * - the weight of item i is stored in position i-1 of array w[];
       * - do not use '*' to multiply a variable and its coefficient;
       * - the constraints must be separated by \n (here there is only one constraint) 
       * - the method portaName() enables to get the name of your variable for porta
       */
      String constraint = w[1 - 1] + " " + portaName("x" + 1);
		
      /* For each item */
      for(int i = 2; i <= n; ++i)
        constraint += " + " + w[i - 1] + " " + portaName("x" + i);
    	
      constraint += " <= " + K;
    		
      return constraint;
    }
    ```
  
  ### How to use porta features once a formulation is defined?
  
  Once an AbstractFormulation object is defined, the associated integer polytope can be analyzed as follows.
  
  #### How to get the list of the integer points in the polytope?
  Use the AbstractFormulation.getIntegerPoints() method:
  
      System.out.println(formulation.getIntegerPoints());
      
  #### How to get the dimension of the polytope?
  Use the AbstractFormulation.getDimension() method:
  
      System.out.println(formulation.getDimension());
      
  #### How to get the facets of the polytope?
  Use the AbstractFormulation.getFacets() method:
  
      System.out.println(formulation.getFacets());
      
  
  ### The knapsack example
  The class Knapsack in the package "example" is a good entry point to see how to use the software on the [knapsack problem](https://en.wikipedia.org/wiki/Knapsack_problem#Definition).
  
  ## Feedbacks are welcome
  We are in the early stage of this project so bugs and limitations are to be expected. 
  I would be most grateful if you could report to me any bug, comment or suggestion that you might have (through the github issue system or directly by mail zacharie.ales[at]ensta[dot]fr).
  
  ## Considered new features
  * Definition of polytopes through their integer points rather than their formulations (useful when porta takes time to compute the integer solutions from the formulation);
  * Better handling of the .lp format (in particular keywords "free" and "infinity" are  not currently taken into account);
  * Manage more porta features and options.
