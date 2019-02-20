package formulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import exception.InvalidIEQFileFormatException;
import exception.UnknownCommandException;
import exception.UnknownVariableName;
import utils.Command;
import utils.Utility;

/**
 * Corresponds to the representation of a polytope.
 * The polytope can be represented by:
 * - its linear formulation (class AbstractFormulation)
 * - its integer points (class AbstractIntegerPoints)
 * @author zach
 *
 */
public abstract class AbstractPolytope {

	/** Hashmap which contains all the variables indexed by their id in porta */
	protected HashMap<Integer, Variable> variables = new HashMap<>();

	/** Hashmap which contains all id in porta indexed by their original name */
	protected HashMap<String, Integer> variablesBis = new HashMap<>();

	String sTmpFolder = "./.tmp";
	String sTmpFileCanonicName = "tmp";
	public String sTmpPOIFile = sTmpFolder + "/" + sTmpFileCanonicName + ".poi";
	public String sTmpConvertedPOIFile = sTmpPOIFile + "_converted";
	public String sTmpConvertedFacetsFile = sTmpFolder + "/" + sTmpFileCanonicName + ".poi.ieq_converted";
	public String sTmpConvertedIntegerPointsFile =  sTmpPOIFile + "_converted";
	public String sTmpIEQFile = sTmpFolder + "/" + sTmpFileCanonicName + ".ieq";
	
	NumberFormat nf = new DecimalFormat("#0.00");

	
	public AbstractPolytope() throws UnknownCommandException, IOException, InterruptedException {
		Command.checkCommand("traf");
		Command.checkCommand("vint");
		Command.checkCommand("dim");
	}

	/* Create the variables if necessary */
	protected void initializeVariables(){

		if(variables.size() == 0)
			createVariables();

	}

	/** Fill the <variables> hashmap. The key of each entry is its index in porta, the value coresponds to the name of the variable */
	protected abstract void createVariables();

	/**
	 * Generate the integer points contained in the polytope. The result must be written in sTmpPOIFile
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException
	 */
	public abstract void generateIntegerPoints() throws UnknownVariableName, InvalidIEQFileFormatException;

	/**
	 * Generate P formulation. The result must be written in sTmpPOIFile
	 * @return 
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException
	 */
	public abstract String generateFormulation() throws UnknownVariableName, InvalidIEQFileFormatException;

	/**
	 * Convert an ieq file in which the variables have porta names into a file in which the variable have the user name
	 * @param inputIEQFile The path of the input ieq file
	 * @param convertedIEQFile The path of the output file
	 * @param removeMinuses True if the variables which appear with a minus '-' in a constraint are put on the other side of the equation (useful to ease the reading of porta output file, less useful when applied on user generated files as it may present the constraints in a way the user is not used to)
	 * @throws IOException
	 * @throws UnknownVariableName
	 */
	public void convertIEQFile(String inputIEQFile, String convertedIEQFile, boolean removeMinuses) throws IOException, UnknownVariableName{

		initializeVariables();

		InputStream ips=new FileInputStream(inputIEQFile); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);

		FileWriter fw = new FileWriter(convertedIEQFile);
		BufferedWriter bw = new BufferedWriter(fw);

		String ligne;

		while ((ligne=br.readLine())!=null){
			String ligne2 = ligne;

			if(removeMinuses)
				ligne2 = removeMinusesInConstraints(ligne);
			else {
				ligne2.replaceAll("\\+", " + ");
				ligne2.replaceAll("-", " - ");
				ligne2.replaceAll("<", " <");
				ligne2.replaceAll(">", " >");
				ligne2.replaceAll("==", " ==");

				ligne2.replaceAll("  ", " ");
				ligne2.replaceAll("  ", " ");
			}
			
			bw.write(replacePortaVariablesInString(ligne2) + "\n");
			bw.flush();
		}

		bw.close();
		br.close();

	}
	

	/**
	 * To ease the readibility of the constraints, change the side of the constraint terms which have a negative coefficient 
	 * @param s String which contains the constraint
	 * @return
	 */
	private String removeMinusesInConstraints(String s) {

		/* Position of the character '-' or '+' of the current variable */
		int currentSignPosition = -2;
		boolean isCurrentVariablePositive = false;

		int indexOfBracket = s.indexOf(")");
		String result = s;

		if(s.length() > 0) {

			List<String> positiveVar = new ArrayList<>();
			List<String> negativeVar = new ArrayList<>();
			int i = Math.max(0, indexOfBracket);
			char c = s.charAt(i);

			while(c != '<' && c != '=' && c != '>' && i < s.length()){

				/* If this is the beginning of a new variable 
				 * (i.e., <c> contains a sign or, if this is the first variable, <c> contains 'x') */
				if(c == '-' || c == '+' || (c == 'x' || Character.isDigit(c)) && currentSignPosition == -2) {

					/* If it is not the first variable */
					if(currentSignPosition != -2) {

						/* Get the string associated to the new variable */
						String variable = s.substring(currentSignPosition+1, i).replace(" ", "");

						if(isCurrentVariablePositive)
							positiveVar.add(variable);
						else
							negativeVar.add(variable);

					}

					isCurrentVariablePositive = c != '-';

					if(c != 'x' && !Character.isDigit(c))
						currentSignPosition = i;
					else
						currentSignPosition = i-1;
				}

				i++;

				if(i < s.length())
					c = s.charAt(i);

			}

			/* If:
			 * - the end of the string has not been reached;
			 * - at least one variable has been found in the LHS;
			 * - no variable appear in the RHS.
			 */
			if(i < s.length() && currentSignPosition != -2 && !s.substring(i).contains("x")) {

				/* Add the last variable */

				/* Get the string associated to the new variable */
				String variable = s.substring(currentSignPosition+1, i).replace(" ", "");

				if(isCurrentVariablePositive)
					positiveVar.add(variable);
				else
					negativeVar.add(variable);

				result = new String();

				boolean isSHNegative = s.substring(i).contains("-");
				boolean isEquality = s.contains("==");
				boolean isGT = s.contains(">=");
				boolean isSHNull = s.contains(" 0");

				int posVarFirstId = 0;

				/* If the second-hand coefficient is negative, add it on the left part */
				if(isSHNegative || isSHNull && positiveVar.size() == 0)

					if(isSHNull)
						result = " 0";
					else
						/* +2 to remove the two characters associated to the sign ("<=" or "==") */
						result = " " + s.substring(i+2).trim().replace("-", "");

				/* Otherwise, the sign of the first positive variable must not appear */
				else {
					posVarFirstId = 1;

					if(positiveVar.size() > 0)
						result += " " + positiveVar.get(0);
				}

				for(int id = posVarFirstId ; id < positiveVar.size() ; id++)
					result += " + " + positiveVar.get(id);

				if(isEquality)
					result += " ==";
				else if(isGT)
					result += " >=";
				else
					result += " <=";


				int negativeVarFirstId = 0;

				if(!isSHNegative && !isSHNull || isSHNull && negativeVar.size() == 0)
					result += " " + s.substring(i+2).trim();

				/* Otherwise, the sign of the first negative variable must not appear */
				else {
					negativeVarFirstId = 1;

					if(negativeVar.size() > 0)
						result += " " + negativeVar.get(0);
				}


				for(int id = negativeVarFirstId ; id < negativeVar.size() ; id++)
					result += " + " + negativeVar.get(id); 

				/* Add the inequality number if any */
				int bracketIndex = s.indexOf(')');

				if(bracketIndex != -1)
					result = s.substring(0, bracketIndex+1) + result;

			}
		}


		return result;
	}


	/**
	 * Replace the variable porta name into user name in a String 
	 * @param s The considered String
	 * @return A string in which the porta names are converted
	 * @throws UnknownVariableName
	 */
	protected String replacePortaVariablesInString(String s) throws UnknownVariableName{

		/* Add spaces before and after operators to ease the parsing */
		s = s.replace("+", " + ");
		s = s.replace("-", " - ");
		s = s.replace("<=", " <= ");
		s = s.replace(">=", " >= ");

		if(!s.contains(">=") && !s.contains("<=")) {
			s = s.replace("<", " < ");
			s = s.replace(">", " > ");
			s = s.replace("==", " = ");
		}

		/* Ensure that all the white spaces in the s are single spaces */
		s = s.replace("\t", " ");

		int length = s.length();
		int previousLength = 0;

		while(length != previousLength) {
			s = s.replace("\t", "  ");
			s = s.replace("  ", " ");
			previousLength = length;
			length = s.length();
		}

		/* Position of the character 'x' which denotes a porta variable; -1 if we are not currently browsing a porta variable */
		int currentXPosition = -1;

		/* Add an empty space at the end to detect a potential variable at the end of the string */
		s += " ";
		String result = new String(s);

		for(int i = 0 ; i < s.length() ; i++){

			/* If we are not currently browsing the characters of a porta variable */
			if(currentXPosition == -1){

				/* If we find the beginning of a porta variable */
				if(s.charAt(i) == 'x')
					currentXPosition = i;
			}
			/* If we reach the end of a porta variable name */
			else if(!Character.isDigit(s.charAt(i))){
				String sInitial = s.substring(currentXPosition, i);

				if(sInitial.length() > 1){
					Variable v = variables.get(Integer.parseInt(sInitial.substring(1)));

					if(v == null)
						throw new UnknownVariableName(sInitial.substring(1));

					String sNew = v.originalName;
					result = result.replaceAll(sInitial + " ", sNew + " ");
				}

				currentXPosition = -1;
			}
		}

		return result;
	}
	

	/**
	 * Convert a poi file in which the variable have porta names into a file in which the variable have the user name
	 * @param inputPOIFile
	 * @param convertedPOIFile
	 * @throws IOException
	 * @throws UnknownVariableName
	 */
	public void convertPOIFile(String inputPOIFile, String convertedPOIFile) throws IOException, UnknownVariableName{

		initializeVariables();

		InputStream ips=new FileInputStream(inputPOIFile); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);

		FileWriter fw = new FileWriter(convertedPOIFile);
		BufferedWriter bw = new BufferedWriter(fw);

		String line;


		String convSection = "CONV_SECTION";
		String coneSection = "CONE_SECTION";
		String endSection = "END";
		boolean isInConvSection = false;
		boolean isInConeSection = false;
		
		/* Type of the object read (vector or point) */
		String objectType ="";
		int objectNb = 0;
		
		while ((line=br.readLine())!=null){

			line = line.trim();
			
			if(line.contains(convSection)) {
				isInConvSection = true;
				isInConeSection = false;
				objectType = "Point";
				objectNb = 1;
			}
			if(line.contains(coneSection)) {
				isInConeSection = true;
				isInConvSection = false;
				objectType = "Vector";
				objectNb = 1;
			}
			else if(line.contains(endSection)) {
				isInConvSection = false;
				isInConeSection = false;
			}
			else if(isInConvSection || isInConeSection) {

				String []sTemp = line.split("\\)");

				String point = "";

				/* If the line contains ')' (e.g., if it starts by "( 1)") */
				if(sTemp.length > 1)
					point = sTemp[1];
				else
					point = sTemp[0];
				
				point = point.trim().replace("- ", "-");
				int pSize;
				
				do {
					pSize = point.length();
					point = point.replace("  ", " ");
					
				}while(pSize != point.length());
				
				sTemp = point.split(" ");

				/* If the line corresponds to a point */
				if(sTemp.length == variables.size()){

					/* Map which contains for each value in the solution, the name of the variables with this value */
					HashMap<Double, List<String>> variablesByValue = new HashMap<>();

					for(int i = 0 ; i < variables.size() ; i++){
						
						Double value;
						
						if(sTemp[i].contains("/")) {
							String[] sTemp2 = sTemp[i].split("/");
							value = Double.parseDouble(sTemp2[0]) / Double.parseDouble(sTemp2[1]);
						}
						else
							value = Double.parseDouble(sTemp[i]);

						List<String> list = variablesByValue.get(value);

						if(list == null){
							list = new ArrayList<>();
							variablesByValue.put(value, list);
						}

						/* +1 since the variables in porta are indexed starting from 1 */
						list.add(variables.get(i+1).originalName);

					}

					bw.write(objectType  +" n°" + objectNb + "\n");
					objectNb++;

					for(Entry<Double, List<String>> entry: variablesByValue.entrySet()){
						if(entry.getKey() != 0 || variablesByValue.entrySet().size() == 1)
							if(Utility.isInteger(entry.getKey()))
								bw.write(entry.getValue() + " = " + entry.getKey().intValue() + "\n");
							else
								bw.write(entry.getValue() + " = " + nf.format(entry.getKey()) + "\n");
					}
					bw.write("\n");

				}
				bw.flush();

			}
		}

		bw.close();
		br.close();

	}

	/**
	 * Register a new variable used in the formulation
	 * @param var
	 */
	public void registerVariable(Variable var){
		int newId = variables.size()+1;
		variables.put(newId, var);
		variablesBis.put(var.originalName, newId);
	}
	

	/**
	 * Check if a variable is registered
	 * @param variableOriginalName
	 * @return
	 */
	public boolean isRegistered(String variableOriginalName) {
		return variablesBis.get(variableOriginalName) != null;
	}


	/**
	 * Returns a string which contains the correspondence between the variables porta name and initial name. 
	 * @return
	 */
	protected String getvariablesindex() {

		String result = "\n\n===\nVariable correspondence\n===\n"
				+ "Porta name\tOriginal name\n---\n";

		for(int i = 0; i < variables.size(); i++) {

			if(variables.get(i) != null)
				result += "x" + i + "\t\t" + variables.get(i).originalName + "\n";
		}
		result += "---";

		return result;
	}

	/**
	 * Get the name of a variable in porta
	 * @param variableOriginalName Original name of the variable
	 * @return The name of the variable in porta
	 * @throws UnknownVariableName 
	 */
	public String portaName(String variableOriginalName) throws UnknownVariableName{

		Integer id = variablesBis.get(variableOriginalName);

		if(id == null)
			throw new UnknownVariableName(variableOriginalName);

		String result = "x" + id;

		return result;

	}
	
	/**
	 * Compute the dimension and the hyperplanes which contains the convex hull of the integer points included in the poi file at <inputFile>.
	 * @param inputFile Path to the .poi file which contains the formulation
	 * @return The porta output which includes the dimension and the including hyperplanes
	 */
	protected static String dim(String inputFile){

		String result = Command.execute("dim " + inputFile);

		String[] sResult = result.split("DIMENSION OF THE POLYHEDRON");

		if(sResult.length > 1)
			result = "DIMENSION OF THE INTEGER POLYHEDRON" + sResult[1];

		return result;
	}

	/**
	 * Compute the facets of the convex hull of the integer points included in the poi file at path <inputFile>.
	 * @param inputFile Path to the .poi file which contains the formulation
	 */
	public static void traf(String inputFile){
		Command.execute("traf " + inputFile);
	}
	
	/**
	 * Use porta to get the facets of the integer polytope associated to this formulation and write them in a file
	 * @param generator The generator associated to the considered formulation
	 * @param outputFile The file in which the facets will be added; null if an error occurred
	 * @throws UnknownVariableName 
	 * @throws InvalidIEQFileFormatException 
	 * @throws IOException  
	 */
	public void writeFacetsInFile(String outputFile) throws UnknownVariableName, InvalidIEQFileFormatException, IOException{

		generateIntegerPoints();
	
		String outputTrafFile = sTmpPOIFile.replace(".poi", ".poi.ieq");

		System.out.println("=== Get the facets (input: " + sTmpPOIFile + ", output: " + outputTrafFile + ")");
		traf(sTmpPOIFile);

		System.out.println("=== Convert facets (input: " + outputTrafFile + ", output: " + outputFile + ")");
		convertIEQFile(outputTrafFile, outputFile, true);

	}
	
	/**
	 * Extract the facets associated to this formulation.
	 * @return
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException
	 * @throws IOException
	 */
	public String getIPFacets() throws UnknownVariableName, InvalidIEQFileFormatException, IOException{

		String results = "";

		writeFacetsInFile(sTmpConvertedFacetsFile);
		try{
			InputStream ips=new FileInputStream(sTmpConvertedFacetsFile);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;
			String facetsSection = "INEQUALITIES_SECTION";
			String endSection = "END";
			boolean isInFacetsSection = false;

			while ((line=br.readLine())!=null){

				if(line.contains(facetsSection))
					isInFacetsSection = true;
				else if(line.contains(endSection))
					isInFacetsSection = false;
				else if(isInFacetsSection)
					results += line + "\n";

			}
			br.close();
		}catch(Exception e){
			System.out.println(e.toString());
		}

		return results;

	}

	/**
	 * Use porta to get the dimension and the hyperplanes which include the convex hull of the integer points of the polytope
	 * @return The dimension and the hyperplanes which include porta; null if an error occurred
	 * @throws UnknownVariableName 
	 * @throws InvalidIEQFileFormatException 
	 */
	public String getIPDimension() throws UnknownVariableName, InvalidIEQFileFormatException{

		String output = null;

		generateIntegerPoints();
		
		System.out.println("=== Get the dimension");
		System.out.println("INITIAL DIMENSION : " + variables.size());
		output = dim(this.sTmpPOIFile);

		output = replacePortaVariablesInString(output);

		return output;

	}
	

	public String getIntegerPoints() throws UnknownVariableName, InvalidIEQFileFormatException, IOException {

		String results = "";

		generateIntegerPoints();

		convertPOIFile(sTmpPOIFile, sTmpConvertedIntegerPointsFile);

		try{
			InputStream ips=new FileInputStream(sTmpConvertedIntegerPointsFile);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;

			while ((line=br.readLine())!=null)
				results += line + "\n";

			br.close();
		}catch(Exception e){
			System.out.println(e.toString());
		}

		return results;
	}
	

	

	/**
	 * Use porta to get the extreme points of a polytope associated to this formulation and write them in a file
	 * @param outputFile The file in which the facets will be added; null if an error occurred
	 * @param isIntegerPoltyope True if we consider the integer polytope; false if we consider the relaxation polytope
	 * @throws UnknownVariableName 
	 * @throws InvalidIEQFileFormatException 
	 * @throws IOException 
	 */
	public void writeExtremePointsInFile(String outputFile, boolean isIntegerPolytope) throws UnknownVariableName, InvalidIEQFileFormatException, IOException{

		String formulationFile;
		
		if(isIntegerPolytope)
			formulationFile = generateIPFormulation();
		else
			formulationFile = generateFormulation();
			
		String lastTrafOutputFile = formulationFile + ".poi";
		System.out.println("=== Get the extreme points (input: " + formulationFile + ", output: " + lastTrafOutputFile + ")");
		traf(formulationFile);

		System.out.println("=== Convert extreme points (input: " + lastTrafOutputFile + ", output: " + outputFile + ")");
		convertPOIFile(lastTrafOutputFile, outputFile);
	}
	
	public String getIPExtremePoints() throws UnknownVariableName, InvalidIEQFileFormatException, IOException {
		return getExtremePoints(true);
	}

	public String getExtremePoints() throws UnknownVariableName, InvalidIEQFileFormatException, IOException {
		return getExtremePoints(false);
	}


	/**
	 * Extract the facets associated to this formulation.
	 * @return
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException
	 * @throws IOException
	 */
	public String getExtremePoints(boolean isIntegerPolytope) throws UnknownVariableName, InvalidIEQFileFormatException, IOException {

		String results = "";

		writeExtremePointsInFile(sTmpPOIFile, isIntegerPolytope);
		
		try{
			InputStream ips=new FileInputStream(sTmpPOIFile);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;

			while ((line=br.readLine())!=null)
					results += line + "\n";

			br.close();
		}catch(Exception e){
			System.out.println(e.toString());
		}

		return results;

	}
	
	/**
	 * Generate I(P) formulation. The result must be written in sTmpPOIFile
	 * @return Output file in which the formulation has been generated
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException
	 */
	public String generateIPFormulation() throws UnknownVariableName, InvalidIEQFileFormatException{

		generateIntegerPoints();

		String output = sTmpPOIFile + ".ieq";
		System.out.println("=== Generate the integer polytope (output: " + output + ")");
		traf(sTmpPOIFile);
		
		return output;
		
	}
	
}
