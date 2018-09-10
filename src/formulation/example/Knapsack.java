package formulation.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import exception.UnknownVariableName;
import formulation.AbstractFormulationGenerator;
import formulation.LPReader;
import formulation.Variable;

public class Knapsack extends AbstractFormulationGenerator{

	/** Number of items **/
	int n;

	/** Weight of the items */
	int[] w;

	/** Value of the items */
	int[] p;
	
	/** Maximal weight of the knapsack */
	int K;
	
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

			/* A formulation can be defined from an input file */
			Knapsack formulation = new Knapsack("./data/knapsack.txt");

			/* or from an LP file */
			LPReader formulation2 = new LPReader("./data/knapsack.lp");

			/* or by directly giving its attributes */
			Knapsack formulation3 = new Knapsack(6, 12, new int[] {1, 3, 2, 4, 6, 4}, new int[] {1, 5, 3, 5, 2, 3});

			Use whatToDo = Use.INTEGER_POINTS;
		
			switch(whatToDo) {
			case DIMENSION: System.out.println(formulation.getDimension()); break;
			case FACETS: System.out.println(formulation.getFacets()); break;
			case INTEGER_POINTS: System.out.println(formulation.getIntegerPoints()); break;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Create a Knapsack object (direct attributes affectation)
	 * @param n
	 * @param w
	 * @param p
	 */
	public Knapsack(int n, int K, int[] w, int[] p) {
		this.n = n;
		this.K = K;
		this.w = w;
		this.p = p;
	}

	/**
	 * Create a Knapsack by reading its attributes values from an input file .
	 * 
	 * The file must contains:
	 * - a line which contains "n = X" with X an integer;
	 * - a line which contains "w = x1 x2 ... xn" with xi an integer;
	 * - a line which contains "p = x1 x2 ... xn" with xi an integer;
	 * 
	 * All the interval between words in these lines must be spaces.
	 * The line which contains the value of n must appear before the two others.
	 * Lines which do not contain "n =", "w =" or "p =" will be ignored.
	 * @param inputFile
	 */
	public Knapsack(String inputFile) {

		try{
			InputStream ips=new FileInputStream(inputFile);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;

			this.n = -1;
			this.K = -1;
			this.w = null;
			this.p = null;

			while ((line=br.readLine())!=null){

				/* If the line contains the value of n */
				if(line.contains("n = ")) {
					String[] sLine = line.split("=");

					if(sLine.length != 2)
						System.err.println("Error the input file \"" + inputFile + "\" is not properly formated. The line \"" + line + "\" should be as follows: \"n = X\" with X an integer");
					else {
						try {
							this.n = Integer.parseInt(sLine[1].trim());
							this.w = new int[n];
							this.p = new int[n];
						}catch(NumberFormatException e) {
							System.err.println("The line \"" + line + "\" must contain an integer after the \"=\" symbol. The program failed at converting the value to an integer.");
							e.printStackTrace();
						}
					}
				}

				/* If the line contains the value of w */
				if(line.contains("w = ") && this.w != null) {

					String[] sLine = line.split("= ");
					
					if(sLine.length != 2)
						System.err.println("Error the input file \"" + inputFile + "\" is not properly formated. The line \"" + line + "\" should be as follows: \"w = x1 x2 ... xn\" with xi integers");
					else {

						String[] sValues = sLine[1].trim().split(" ");

						if(sValues.length != n)
							System.err.println("Error the input file \"" + inputFile + "\" is not properly formated (it should contains a line with \"w = x1 x2 ... xn\" with xi integers.");
						else {

							try {
								for(int i = 0; i < n; ++i) 
									w[i] = Integer.parseInt(sValues[i].trim());
							}catch(NumberFormatException e) {
								System.err.println("The line \"" + line + "\" must contain " + n + " integers after the \"=\" symbol. The program failed at converting one of the value to an integer.");
								e.printStackTrace();
							}
						}
					}
				}

				/* If the line contains the value of p */
				if(line.contains("p = ") && this.p != null) {

					String[] sLine = line.split("= ");

					if(sLine.length != 2)
						System.err.println("Error the input file \"" + inputFile + "\" is not properly formated. The line \"" + line + "\" should be as follows: \"p = x1 x2 ... xn\" with xi integers");
					else {

						String[] sValues = sLine[1].trim().split(" ");

						if(sValues.length != n)
							System.err.println("Error the input file \"" + inputFile + "\" is not properly formated (it should contains a line with \"p = x1 x2 ... xn\" with xi integers).");
						else {

							try {
								for(int i = 0; i < n; ++i) 
									p[i] = Integer.parseInt(sValues[i].trim());
							}catch(NumberFormatException e) {
								System.err.println("The line \"" + line + "\" must contain " + n + " integers after the \"=\" symbol. The program failed at converting one of the value to an integer.");
								e.printStackTrace();
							}
						}
					}
				}

				/* If the line contains the value of K */
				if(line.contains("K = ")) {
					String[] sLine = line.split("=");

					if(sLine.length != 2)
						System.err.println("Error the input file \"" + inputFile + "\" is not properly formated. The line \"" + line + "\" should be as follows: \"K = X\" with X an integer");
					else {
						try {
							this.K = Integer.parseInt(sLine[1].trim());
						}catch(NumberFormatException e) {
							System.err.println("The line \"" + line + "\" must contain an integer after the \"=\" symbol. The program failed at converting the value to an integer.");
							e.printStackTrace();
						}
					}
				}
			}
				br.close();
			
			
			if(n == -1 || K == -1 || p == null || w == null)
				throw new InvalidKnapsackInputFile(n == -1, K == -1, p == null, w == null);
			
		}catch(InvalidKnapsackInputFile e){
			System.err.println("Invalid Knapsack input file");

			if(n == -1)
				System.err.println("n is not correctly defined");
			if(K == -1)
				System.err.println("K is not correctly defined");
			if(p == null)
				System.err.println("p is not correctly defined");
			if(w == null)
				System.err.println("w is not correctly defined");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		

	}

	@Override
	protected void createVariables() {
		
		/* Register the knapsack formulation variables */
		for(int i = 1; i <= n; ++i)
			this.addVariable(new Variable("x" + i, 0, 1));
	}

	@Override
	public String getInequalities() throws UnknownVariableName {
		
		/* Create the constraint 
		 * 
		 * Remarks: 
		 * - the weight of item i is stored in position i-1 of array w[];
		 * - do not use '*' to multiply a variable and its coefficient;
		 * - the constraints must be separated by \n (here there is only one constraint) */
		String constraint = w[1 - 1] + " " + portaName("x" + 1);
		
		/* For each item */
		for(int i = 2; i <= n; ++i)
			constraint += " + " + w[i - 1] + " " + portaName("x" + i);
		
		constraint += " <= " + K;
			
		return constraint;
	}
	
	@SuppressWarnings("serial")
	public class InvalidKnapsackInputFile extends Exception{
		
		boolean isNCorrectlyDefined;
		boolean isKCorrectlyDefined;
		boolean isWCorrectlyDefined;
		boolean isPCorrectlyDefined;
		
		public InvalidKnapsackInputFile(boolean isNCorrectlyDefined, boolean isKCorrectlyDefined, boolean isWCorrectlyDefined, boolean isPCorrectlyDefined) {
			this.isNCorrectlyDefined = isNCorrectlyDefined;
			this.isKCorrectlyDefined = isKCorrectlyDefined;
			this.isWCorrectlyDefined = isWCorrectlyDefined;
			this.isPCorrectlyDefined = isPCorrectlyDefined;
		}
	}

}
