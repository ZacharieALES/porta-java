package formulation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import exception.InvalidPCenterInputFile;
import exception.UnknownCommandException;
import exception.UnknownVariableName;
import lpterms.AbstractTerm;
import lpterms.CoefficientTerm;
import lpterms.OperatorTerm;
import lpterms.VariableTerm;

/**
 * Restrictions:
 * - the expressions do not include parenthesis
 * - each constraint has a name
 * - there are no coefficients expressed using character '/'
 * - the letter "e" or "E" for exponential entries are not taking into account
 * - the use of "+/- infinity/inf" is not taking into account (possible in the bound section) 
 * - the use of the term "free" is not taking into account (possible in the bounds section) 
 * - the syntax of the BOUNDS section of the .lp file is not checked (we just add a constraint for each operator found and check if each operator is between a variable and a coefficient)
 * - '<' and '>' operators are respectively converted to "<=" and ">=" 
 * 
 * Remark:
 * - the default bound for a variable in a CPLEX LP file is [0, 8]. These bounds are used if a variable bounds are not specified.
 * 
 * Possible extension:
 * - test if all the variables used are declared
 * - add the constraints name in the .ieq file
 * 
 * @author zach
 *
 */
public class LPReader extends AbstractFormulationGenerator{

	String constraints = "";
	public enum Section{
		BINARIES, GENERALS, CONSTRAINTS, OBJECTIVE, NONE, END, BOUNDS
	}

	public String inputFile;

	@Override
	protected void createVariables() {
		try {
			readLPFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The constraints are created in the constructor
	 */
	@Override
	public String getInequalities() throws UnknownVariableName {
		return constraints;
	}

	public LPReader(String lpfile) throws IOException, InvalidPCenterInputFile, UnknownCommandException, InterruptedException {
		super();
		this.inputFile = lpfile;
	}

	/**
	 * Test if a term corresponds to a new section
	 * @param term
	 * @return The type of the new section if any; Section.NONE otherwise
	 */
	public Section detectNewSection(String expression) {

		String lowerExpression = expression.toLowerCase();
		
		Section newSection = Section.NONE;

		if("subject to".equals(lowerExpression)
				|| "st".equals(lowerExpression)
				|| "s.t.".equals(lowerExpression))
			newSection = Section.CONSTRAINTS;
		else if("min".equals(lowerExpression)
				|| "max".equals(lowerExpression)
				|| "maximize".equals(lowerExpression)
				|| "maximum".equals(lowerExpression)
				|| "minimize".equals(lowerExpression)
				|| "minimum".equals(lowerExpression))
			newSection = Section.OBJECTIVE;
		else if("general".equals(lowerExpression)
				|| "generals".equals(lowerExpression)
				|| "gen".equals(lowerExpression))
			newSection = Section.GENERALS;
		else if("end".equals(lowerExpression))
			newSection = Section.END;
		else if("bound".equals(lowerExpression)
				|| "bounds".equals(lowerExpression))
			newSection = Section.BOUNDS;
		else if("binary".equals(lowerExpression)
				|| "binaries".equals(lowerExpression)
				|| "bin".equals(lowerExpression))
			newSection = Section.BINARIES;
		
		return newSection;
	}

	public boolean isAnOperator(String expression) {

		return "+".equals(expression)
				|| "-".equals(expression)
				|| "<".equals(expression)
				|| ">".equals(expression)
				|| "<=".equals(expression)
				|| ">=".equals(expression)
				|| "=".equals(expression);

	}

	public void readLPFile() throws IOException {

		constraints = "";

		InputStream ips=new FileInputStream(inputFile); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);

		/* Read the first line */
		String line;		

		Section cSection = Section.NONE;

		List<AbstractTerm> currentExpression = null;

		/* While there are line and the problem definition is not over */
		while ((line=br.readLine())!=null && cSection != Section.END){
			
//			System.out.println("Reading line: " + line);

			/* Preprocess and split by spaces the line */
			line = preprocessLine(line.trim());

			String[] sLine = line.split(" ");

			/* If it is not empty */
			if(sLine.length > 0) {

				int currentTermId = 0;
				String currentTerm = sLine[currentTermId];

				/* Test if the line is the beginning of a new section */
				Section newSection = detectNewSection(currentTerm);

				/* If it is */
				if(newSection != Section.NONE) {

					/* Process the previous expression and add the result (if any) to the constraints */ 
					constraints += processExpression(currentExpression, cSection) + "\n";

					/* The next expression is currently empty */
					currentExpression = null;
					currentTermId++;

					cSection = newSection;
				}

				/* If the current line is in an interesting section */
				if(cSection == Section.BINARIES
						|| cSection == Section.GENERALS
						|| cSection == Section.BOUNDS
						|| cSection == Section.CONSTRAINTS) {

					/* If it is a new constraint */
					int colonIndex = line.indexOf(":");

					/* i.e., if the line contains ":" */
					if(colonIndex != -1) {

						/* Process the previous constraint */
						constraints += processExpression(currentExpression, cSection) + "\n";

						/* The next expression is currently empty */
						currentExpression = null;

						/* Remove the part before the ":" in the line and split it again */
						line = line.split(":")[1];
						sLine = line.trim().split(" ");

						currentTermId = 0;

					}

					/* For each term on this line */
					while(currentTermId < sLine.length) {

						currentTerm = sLine[currentTermId];

						if(currentTerm.length() > 0) {
							if(currentExpression == null)
								currentExpression = new ArrayList<>();

							/* Test if it is an operator */
							if(isAnOperator(currentTerm))
								currentExpression.add(new OperatorTerm(currentTerm));

							/* Otherwise try to convert the term into a float (test if it is a coefficient) */
							else {
								try {
									Float.parseFloat(currentTerm);
									currentExpression.add(new CoefficientTerm(currentTerm));
								}
								catch(NumberFormatException e2) {

									/* Otherwise, it corresponds to a variable name */
									currentExpression.add(new VariableTerm(currentTerm, this));

								}
							}
						}

						currentTermId ++;

					}
				}
				else if(cSection == Section.END)
					constraints += processExpression(currentExpression, cSection);
			}
		}

		br.close();

	}

	private String processExpression(List<AbstractTerm> currentExpression, Section cSection) {

		String result = "";

		if(currentExpression != null) {

			if(cSection == Section.CONSTRAINTS) {

				boolean isValid = isConstraintValid(currentExpression);

				if(isValid) {
					for(AbstractTerm at: currentExpression)
						result += at.expression + " ";
					
//					System.out.println("Add constraint \"" + currentExpression);
				}
				
				

			}

			else if(cSection == Section.BINARIES) {

				/* The binary section must only contain variables (no operator or coefficient) */
				boolean isValid = true;

				int id = 0;

				while(isValid && id < currentExpression.size()) {
					if(!(currentExpression.get(id) instanceof VariableTerm)) { 
						isValid = false;
						System.err.println("Error: the BINARY section of the file contains a term which is not a variable name: " + currentExpression.get(id).expression);
					}

					id++;
				}

				if(isValid)
					for(AbstractTerm at: currentExpression) {
						VariableTerm vt = (VariableTerm)at;

						this.setLBound(vt.originalExpression, 0);
						this.setUBound(vt.originalExpression, 1);
//						System.out.println("Variable \"" + vt.originalExpression + "\" is set to binary");
					}
			}

			else if(cSection == Section.BOUNDS) {

				/* For each term of the BOUNDS section */
				for(int i = 1; i < currentExpression.size() - 1; i++) {
					
					AbstractTerm cTerm = currentExpression.get(i);
					
					/* For each operator term of the BOUNDS section */
					if(cTerm instanceof OperatorTerm) {
						
						AbstractTerm previousTerm = currentExpression.get(i-1);
						AbstractTerm nextTerm = currentExpression.get(i+1);
						
						OperatorTerm operator = (OperatorTerm)cTerm;
						VariableTerm variable = null;
						CoefficientTerm coefficient = null;
						boolean isUpperBound = true;
						
						/* If the operator is between a variable and a coefficient in the following order: var operator coefficient */
						if(previousTerm instanceof VariableTerm && nextTerm instanceof CoefficientTerm) {
							
							/* Set the variable, the coefficient and the direction */ 
							variable = (VariableTerm) previousTerm;
							coefficient = (CoefficientTerm)nextTerm;
							
							if(!operator.expression.contains("<"))
								isUpperBound = false;
						}
						/* If the operator is between a variable and a coefficient in the following order: coefficient operator var */
						else if(nextTerm instanceof VariableTerm && previousTerm instanceof CoefficientTerm) {

							/* Set the variable, the coefficient and the direction */
							variable = (VariableTerm) nextTerm;
							coefficient = (CoefficientTerm)previousTerm;
							
							if(operator.expression.contains("<"))
								isUpperBound = false;
						}
						
						/* If the operator is not between a variable and a coefficient */
						if(variable == null) {
							System.err.println("Error: expression \"" + previousTerm.expression + " " + operator.expression + " " + nextTerm.expression + "\" is not valid.\n"
									+ "Operator \"" + operator.expression + "\" should be between exactly one variable and one coefficient.");
						}
						
						/* If the operator is between a variable and a coefficient */
						else {
							
							if(isUpperBound) {
								this.setUBound(variable.originalExpression, Integer.parseInt(coefficient.expression));
//								System.out.println("Upper bound of variable \"" + variable.originalExpression + "\" is set to " + coefficient.expression);
							}
							else {
								this.setLBound(variable.originalExpression, Integer.parseInt(coefficient.expression));
//								System.out.println("Lower bound of variable \"" + variable.originalExpression + "\" is set to " + coefficient.expression);
							}
								
						}
					}
				}


			}
		}

		return result;
	}

	private boolean isConstraintValid(List<AbstractTerm> currentExpression) {

		boolean isValid = true;

		if(currentExpression.size() > 2) {

			/* Check that there are not two consecutive variables or coefficients or operators in the constraint */
			int id1 = 0;
			int id2 = 1;

			while(id2 < currentExpression.size() && isValid) {
				AbstractTerm t1 = currentExpression.get(id1);
				AbstractTerm t2 = currentExpression.get(id2);

				if(t1 instanceof OperatorTerm && t2 instanceof OperatorTerm
						|| t1 instanceof VariableTerm && t2 instanceof VariableTerm
						|| t1 instanceof CoefficientTerm && t2 instanceof CoefficientTerm)
					isValid = false;

				id1++;
				id2++;

			}

			id1 = 0;
			id2 = 1;
			int id3 = 2;

			while(id3 < currentExpression.size() && isValid) {

				AbstractTerm t1 = currentExpression.get(id1);
				AbstractTerm t2 = currentExpression.get(id2);
				AbstractTerm t3 = currentExpression.get(id3);

				if(!(t1 instanceof OperatorTerm) && !(t2 instanceof OperatorTerm) && !(t3 instanceof OperatorTerm))
					isValid = false;
				
				id1++;
				id2++;
				id3++;
			}
		}
		else
			isValid = false;

		if(!isValid)
			System.err.println("Error: constraint \"" + currentExpression + "\" is not valid.\n"
					+ "Possible reasons are :\n"
					+ "- two consecutive operators, variables or coefficients\n;"
					+ "- three consecutive terms without any operator.\n"
					);

		return isValid;
	}

	private String preprocessLine(String line) {

		/* First remove the potential comments */
		int index = line.indexOf("\\");

		if(index != -1)
			line = line.substring(0, index);

		/* Then, treat the special case of "subject to" which is the only keyword with a space... */
		index = line.toLowerCase().indexOf("subject to");

		if(index == 0) {
			if(line.length() >= 11)
				line = "st" + line.substring(11);
			else
				line = "st";
				
		}

		/* Add spaces before and after operators to ease the parsing */
		line = line.replace("+", " + ");
		line = line.replace("-", " - ");
		line = line.replace("<=", " <= ");
		line = line.replace(">=", " >= ");

		if(!line.contains(">=") && !line.contains("<=")) {
			line = line.replace("<", " < ");
			line = line.replace(">", " > ");
			line = line.replace("=", " = ");
		}

		/* Ensure that all the white spaces in the line are single spaces */
		line = line.replace("\t", " ");

		int length = line.length();
		int previousLength = 0;

		while(length != previousLength) {
			line = line.replace("  ", " ");
			previousLength = length;
			length = line.length();
		}

		return line.trim();
	}

	public static void main(String[] args){

		try {
			String inputFile = "./data/sncf3.lp";
			String outputFile = "./data/sncf3_facets.ieq";
			LPReader formulation = new LPReader(inputFile);

			String initialPOIFile = "./data/sncf3lp.poi";
			String trafOutputFile = initialPOIFile.replace(".poi", ".poi.ieq");
			formulation.generateFormulation();
			String output = dim(initialPOIFile);

			output = formulation.replacePortaVariablesInString(output);
			
			System.out.println("===== Print dim output: \n" + output);
			
			System.out.println("=== Get the facets (input: " + initialPOIFile + ", output: " + trafOutputFile + ")");
			traf(initialPOIFile);

			System.out.println("=== Convert facets (input: " + trafOutputFile + ", output: " + outputFile + ")");
			formulation.convertIEQFile(trafOutputFile, outputFile, true);
			
////			System.exit(0);
//			System.out.println(formulation.getDimension());
//
//			formulation.getFacets("./.tmp/facet.ieq");
//
//			formulation.convertPOIFile("./.tmp/tmp.poi", "./.tmp/" + inputFile + "_ordered_converted_integer_points.poi");
//			formulation.convertIEQFile("./.tmp/tmp.ieq", "./.tmsncf1.lpp/" + inputFile + "_ordered_converted_formulation.ieq", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}
