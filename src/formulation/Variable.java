package formulation;

public class Variable {
	
	public String originalName;
	public String originalLatexName = null;
	
	/** Lowest possible value */
	int lowerBoundNumerator, lowerBoundDenominator = 1;
	
	/** Highest possible value */
	int upperBoundNumerator, upperBoundDenominator = 1;
	
	public Variable(String originalName, int lowerBound, int upperBound){
		this.originalName = originalName;
		this.lowerBoundNumerator = lowerBound;
		this.upperBoundNumerator = upperBound;
	}
	
	public Variable(String originalName){
		this.originalName = originalName;
		
		/* Default bounds used in CPLEX LP files */
		this.lowerBoundNumerator = 0;
		this.upperBoundNumerator = 8;
	}

	public Variable(String originalName, int lowerBound, int upperBound, String originalLatexName){
		this(originalName, lowerBound, upperBound);
		this.originalLatexName = originalLatexName;
	}

}
