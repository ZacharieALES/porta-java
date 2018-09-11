package formulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import exception.InvalidIEQFileFormatException;
import exception.UnknownCommandException;
import exception.UnknownVariableName;
import utils.Command;


public abstract class AbstractFormulation {

	String sTmpFolder = "./.tmp";
	String sTmpFileCanonicName = "tmp";
	public String sTmpIEQFile = sTmpFolder + "/" + sTmpFileCanonicName + ".ieq";
	public String sTmpPOIFile = sTmpFolder + "/" + sTmpFileCanonicName + ".poi";
	public String sTmpConvertedFacetsFile = sTmpFolder + "/" + sTmpFileCanonicName + ".poi.ieq_converted";
	public String sTmpConvertedIntegerPointsFile =  sTmpPOIFile + "_converted";

	public AbstractFormulation() throws UnknownCommandException, IOException, InterruptedException{
		checkCommand("traf");
		checkCommand("vint");
		checkCommand("dim");
	}
	
	/**
	 * Test if a linux command exists on the system and throw an exception otherwise
	 * @param command The name of the command
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private void checkCommand(String command) throws UnknownCommandException, IOException, InterruptedException{
			
		boolean existsInPath = Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
		        .map(Paths::get)
		        .anyMatch(path -> Files.exists(path.resolve(command)));
		
		if(!existsInPath) {
			throw new UnknownCommandException(command);
		}
	}
	
	private void initializeVariables(){

		if(variables.size() == 0)
			createVariables();

	}

	/** Hashmap which contains all the variables indexed by their id in porta */
	private HashMap<Integer, Variable> variables = new HashMap<>();

	/** Hashmap which contains all id in porta indexed by their original name */
	private HashMap<String, Integer> variablesBis = new HashMap<>();

	public void generateFormulation() throws UnknownVariableName{
//		System.out.println("Generate formulation in : "+ sTmpIEQFile);
		generateFormulation(sTmpIEQFile);
	}

	public void generateFormulation(String sTmpIEQFile) throws UnknownVariableName{

		File tmpFile = new File(sTmpIEQFile);

		String parentPath = tmpFile.getParent();
		File tmpFolder = new File(parentPath);

		if(!tmpFolder.exists())
			tmpFolder.mkdir();

		initializeVariables();

		FileWriter fw;
		try {
			fw = new FileWriter(tmpFile);
			BufferedWriter bw = new BufferedWriter(fw);

			bw.write("DIM=" + variables.size());
			//			bw.write("\n\nVALID\n");
			//			bw.write(getValidPoint());
			bw.write("\n\nLOWER_BOUNDS\n");
			bw.write(getLowerBound());
			bw.write("\n\nUPPER_BOUNDS\n");
			bw.write(getUpperBound());
			bw.write("\n\nINEQUALITIES_SECTION\n");
			bw.write(getConstraints());
			bw.write("\n\nEND\n");
			
			bw.write(getvariablesindex());
			bw.flush();
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private String getvariablesindex() {
		
		String result = "\n\n===\nVariable correspondence\n===\n"
				+ "Porta name\tOriginal name\n---\n";
		
		for(int i = 0; i < variables.size(); i++) {
//			System.out.println(variables.get(i) == null);
			
			if(variables.get(i) != null)
				result += "x" + i + "\t\t" + variables.get(i).originalName + "\n";
		}
		result += "---";
			
		return result;
	}

	/**
	 * Convert an ieq file in which the variable have porta names into a file in which the variable have the user name
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
				ligne2 = removeMinusesInInequalities(ligne);
			else {
				ligne2.replaceAll("\\+", " + ");
				ligne2.replaceAll("-", " - ");
				ligne2.replaceAll("<", " <");
				ligne2.replaceAll(">", " >");
				ligne2.replaceAll("==", " ==");

				ligne2.replaceAll("  ", " ");
				ligne2.replaceAll("  ", " ");
			}
				

//			if(!ligne.equals(ligne2)) 
//			{
//				System.out.println("---");
//				System.out.println(ligne);
//				System.out.println(ligne2);
//				System.out.println(replacePortaVariablesInString(ligne2));
//			}
			bw.write(replacePortaVariablesInString(ligne2) + "\n");
			//			bw.write(replacePortaVariablesInString(ligne) + "\n");
			bw.flush();
		}

		bw.close();
		br.close();

	}

	public void convertPOIFile(String inputPOIFile, String convertedPOIFile) throws IOException, UnknownVariableName{

		initializeVariables();

		InputStream ips=new FileInputStream(inputPOIFile); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);

		FileWriter fw = new FileWriter(convertedPOIFile);
		BufferedWriter bw = new BufferedWriter(fw);

		String ligne;

		int solutionNb = 1;

		while ((ligne=br.readLine())!=null){
			String []sTemp = ligne.split("\\)");

			/* If the line corresponds to a point */
			if(sTemp.length > 1){

				sTemp = sTemp[1].split(" ");

				if(sTemp.length == variables.size() + 1){

					HashMap<Integer, List<String>> variablesByValue = new HashMap<>();

					for(int i = 1 ; i <= variables.size() ; i++){
						Integer value = Integer.parseInt(sTemp[i]);

						List<String> list = variablesByValue.get(value);

						if(list == null){
							list = new ArrayList<>();
							variablesByValue.put(value, list);
						}

						list.add(variables.get(i).originalName);

					}

					bw.write("Solution n°" + solutionNb + "\n");
					solutionNb++;

					for(Entry<Integer, List<String>> entry: variablesByValue.entrySet()){
						if(entry.getKey() != 0 || variablesByValue.entrySet().size() == 1)
							bw.write("= " + entry.getKey() + ": " + entry.getValue() + "\n");
					}
					bw.write("\n");

				}
				else{
					System.out.println("The lines seems to contain an integer solution but it contains " + (sTemp.length-1) + " integer values instead of " + variables.size()+ "\nThe line: " + ligne);
				}
				bw.flush();

			}
		}

		bw.close();
		br.close();

	}

	private String removeMinusesInInequalities(String s) {

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

	/** Fill the <variables> hashmap. The key of each entry is its index in porta, the value coresponds to the name of the variable */
	protected abstract void createVariables();

	/** 
	 * Get a valid point.
	 * Ex:
	 * 0 1 1 0 2
	 * @return Valid point represented by a String which contains the value of each variable ordered by their index in porta
	 */
	public String getValidPoint(){
		String result = "";

		// TODO Récupérer valeur dans le fichier poi

		return result;

	}

	/** 
	 * Get an upper bound.
	 * Ex:
	 * 0 0 0 0 0 
	 * @return Lower bound of each variable ordered by porta index
	 */
	public String getLowerBound(){

		String result = "";

		for(Entry<Integer, Variable> entry: variables.entrySet()) {
			
			if(entry.getValue().lowerBoundDenominator != 1) {
				
				int lb = entry.getValue().lowerBoundNumerator / entry.getValue().lowerBoundDenominator;
				
				if(Math.abs(lb - (entry.getValue().lowerBoundNumerator / entry.getValue().lowerBoundDenominator)) > 0.0001) {				
					System.err.println("Error: AbstractFormulationGeneration.getLowerBound: this software is currently unable to consider non integer variable bounds");
					System.err.println("The lower bound of variable \"" + entry.getValue().originalName + "\" is set to " + lb + " instead of " + entry.getValue().lowerBoundNumerator + "/" + entry.getValue().lowerBoundDenominator);
				}
				
			}
			else
				result += entry.getValue().lowerBoundNumerator + " ";
		}

		return result;

	}

	/** 
	 * Get an upper bound.
	 * Ex:
	 * 1 1 1 1 3
	 * @return Upper bound of each variable ordered by porta index
	 */
	public String getUpperBound(){

		String result = "";

		for(Entry<Integer, Variable> entry: variables.entrySet()) {
			
			if(entry.getValue().upperBoundDenominator != 1) {
				
				int ub = entry.getValue().upperBoundNumerator / entry.getValue().upperBoundDenominator;

				if(Math.abs(ub - (entry.getValue().upperBoundNumerator / entry.getValue().upperBoundDenominator)) > 0.0001) {	
					System.err.println("Error: AbstractFormulationGeneration.getUpperBound: this software is currently unable to consider non integer variable bounds");
					System.err.println("The upper bound of variable \"" + entry.getValue().originalName + "\" is set to " + ub + " instead of " + entry.getValue().upperBoundNumerator + "/" + entry.getValue().upperBoundDenominator);
				}
				
			}
			else
				result += entry.getValue().upperBoundNumerator + " ";
		}

		return result;

	}

	/**
	 * Get the constraints that represents the formulation such that:
	 * - Each constraint must be on one line.
	 * - The left-hand side contains the variables.
	 * - The right-hand side contains a numerical value.
	 * - The operator is either: >=, <= or ==.
	 * - A variable and its coefficients are separated by spaces not by '*'. 
	 * 
	 * Ex:
	 * (2) 2 x9  +  x4 - x1 - x2 - x3  <= 0
	 * (2) x6  + x1 <= 1
	 * (2) x10  + x1 <= 1
	 * 
	 * @return A string which contains the constraints of the formulation
	 * @throws UnknownVariableName 
	 */
	public abstract String getConstraints() throws UnknownVariableName;

	public void addVariable(Variable var){
		int newId = variables.size()+1;
		variables.put(newId, var);
		variablesBis.put(var.originalName, newId);
		
//		System.out.println("var " + var.originalName + "  " + newId );
	}

	/**
	 * Get the name of a variable in porta
	 * @param variableOriginalName Original name of the variable
	 * @return The name of the variable in porta
	 * @throws UnknownVariableName 
	 */
	public String portaName(String variableOriginalName) throws UnknownVariableName{

		Integer id = variablesBis.get(variableOriginalName);
		//		String result = null;
		//		
		//		Iterator<Entry<Integer, Variable>> it = variables.entrySet().iterator();
		//		
		//		while(result == null && it.hasNext()){
		//			Entry<Integer, Variable> entry = it.next();
		//			
		//			if(variableOriginalName.equals(entry.getValue().originalName))
		//				result = "x" + entry.getKey();
		//		}

		if(id == null)
			throw new UnknownVariableName(variableOriginalName);

		String result = "x" + id;

		return result;

	}
	
	public boolean isDefined(String variableOriginalName) {
		return variablesBis.get(variableOriginalName) != null;
	}
	

	public void setLBound(String varOriginalName, Integer lbound) {
		setLBound(varOriginalName, lbound, 1);
	}
	
	public void setLBound(String varOriginalName, Integer lboundNumerator, Integer lboundDenominator) {
		Integer portaId = variablesBis.get(varOriginalName);
		
		if(portaId != null) {
			variables.get(portaId).lowerBoundNumerator = lboundNumerator;
			variables.get(portaId).lowerBoundDenominator = lboundDenominator;
		}
	}

	public void setUBound(String varOriginalName, Integer ubound) {
		setUBound(varOriginalName, ubound, 1);
	}
	
	public void setUBound(String varOriginalName, Integer uboundNumerator, Integer uboundDenominator) {
		Integer portaId = variablesBis.get(varOriginalName);
		
		if(portaId != null) {
			variables.get(portaId).upperBoundNumerator = uboundNumerator;
			variables.get(portaId).upperBoundDenominator = uboundDenominator;
		}
	}

	/**
	 * Use porta to get the dimension and the hyperplanes which include the convex hull of the integer points from a given formulation
	 * @param generator The generator associated to the considered formulation
	 * @return The dimension and the hyperplanes which include porta; null if an error occurred
	 * @throws UnknownVariableName 
	 * @throws InvalidIEQFileFormatException 
	 */
	public String getDimension() throws UnknownVariableName, InvalidIEQFileFormatException{

		String output = null;

		System.out.println("=== Generate the formulation");
		this.generateFormulation();

		System.out.println("=== Extract the integer points");
		vint(this.sTmpIEQFile);

		System.out.println("=== Get the dimension");
		System.out.println("INITIAL DIMENSION : " + variables.size());
		output = dim(this.sTmpPOIFile);

		output = replacePortaVariablesInString(output);

		return output;

	}
	
	public String getIntegerPoints() throws UnknownVariableName, InvalidIEQFileFormatException, IOException {

		String results = "";

		System.out.println("=== Generate the formulation (output: " + sTmpIEQFile + ")");
		generateFormulation(sTmpIEQFile);

		System.out.println("=== Extract the integer points (input: " + sTmpIEQFile + ", output: " + sTmpPOIFile + ")");
		vint(sTmpIEQFile);

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
	
	public String getFacets() throws UnknownVariableName, InvalidIEQFileFormatException, IOException {
		
		String results = "";
		
		getFacets(sTmpConvertedFacetsFile);
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
	 * Use porta to get the facets of the integer polytope associated to a given formulation
	 * @param generator The generator associated to the considered formulation
	 * @param outputFile The file in which the facets will be added; null if an error occurred
	 * @throws UnknownVariableName 
	 * @throws InvalidIEQFileFormatException 
	 * @throws IOException 
	 */
	public void getFacets(String outputFile) throws UnknownVariableName, InvalidIEQFileFormatException, IOException{

			System.out.println("=== Generate the formulation (output: " + sTmpIEQFile + ")");
			generateFormulation(sTmpIEQFile);

			System.out.println("=== Extract the integer points (input: " + sTmpIEQFile + ", output: " + sTmpPOIFile + ")");
			vint(sTmpIEQFile);
			
			String outputTrafFile = sTmpPOIFile.replace(".poi", ".poi.ieq");

			System.out.println("=== Get the facets (input: " + sTmpPOIFile + ", output: " + outputTrafFile + ")");
			traf(sTmpPOIFile);

			System.out.println("=== Convert facets (input: " + outputTrafFile + ", output: " + outputFile + ")");
			convertIEQFile(outputTrafFile, outputFile, true);


	}

	/**
	 * Compute the facets of the convex hull of the integer points included in the poi file at path <inputFile>.
	 * @param inputFile Path to the .poi file which contains the formulation
	 */
	public static void traf(String inputFile){
		Command.execute("traf " + inputFile);
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
	 * Compute all the integers points for the formulation contained in <inputFile>.
	 * @param inputFile Path to the .ieq file which contains the formulation
	 * @throws InvalidIEQFileFormatException 
	 */
	private static void vint(String inputFile) throws InvalidIEQFileFormatException{
		String result = Command.execute("vint " + inputFile);
		
		String[] sResult = result.split("number of valid integral points");
		
		if(sResult.length > 1)
			System.out.println("number of valid integral points" + sResult[1].split("\n")[0].trim());

		String error = null;
		if(result.contains("invalid format"))
			error = "invalid format";
			
		else if(result.contains("line too long"))
			error = "line too long";
			
		if(error != null) {
			String[] sInvalid = result.split(error);
			
			/* Get the first part of the message */
			String[] lines = sInvalid[0].split("\n");
			throw new InvalidIEQFileFormatException(lines[lines.length-1] + error + sInvalid[1].split("\n")[0]);
			
		}
	}

}
