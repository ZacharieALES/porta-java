package formulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import exception.InvalidIEQFileFormatException;
import exception.UnknownCommandException;
import exception.UnknownVariableName;
import utils.Command;

/**
 * Representation of a polytope by its formulation
 * @author zach
 *
 */
public abstract class AbstractFormulation extends AbstractPolytope{

	public String sTmpIEQFile = sTmpFolder + "/" + sTmpFileCanonicName + ".ieq";
	public String sTmpConvertedIntegerPointsFile =  sTmpPOIFile + "_converted";

	/**
	 * At the creation of a formulation, check that the required commands are visible
	 * @throws UnknownCommandException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public AbstractFormulation() throws UnknownCommandException, IOException, InterruptedException{
		super();
	}

	public abstract String getConstraints() throws UnknownVariableName;
	
	/**
	 * Generate the formulation in the default file
	 * @throws UnknownVariableName
	 */
	public void generateFormulation() throws UnknownVariableName{
		//		System.out.println("Generate formulation in : "+ sTmpIEQFile);
		generateFormulation(sTmpIEQFile);
	}

	/** Generate the formulation in a specified file
	 * @param ieqFile The considered file
	 * @throws UnknownVariableName
	 */
	public void generateFormulation(String ieqFile) throws UnknownVariableName{

		File tmpFile = new File(ieqFile);

		String parentPath = tmpFile.getParent();
		File tmpFolder = new File(parentPath);

		/* Create the temporary folder if necessary */
		if(!tmpFolder.exists())
			tmpFolder.mkdir();

		initializeVariables();

		/* Create the porta ieq file */ 
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

	/** 
	 * Get the variables lower bound in the format that porta wants.
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
	 * Get the variables upper bound in the format that porta wants.
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


	/**
	 * Extract the facets associated to this formulation.
	 * @return
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException
	 * @throws IOException
	 */
	public String getFacets() throws UnknownVariableName, InvalidIEQFileFormatException, IOException {

		String results = "";

		extractFacets(sTmpConvertedFacetsFile);
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
	 * Use porta to get the facets of the integer polytope associated to this formulation and write them in a file
	 * @param generator The generator associated to the considered formulation
	 * @param outputFile The file in which the facets will be added; null if an error occurred
	 * @throws UnknownVariableName 
	 * @throws InvalidIEQFileFormatException 
	 * @throws IOException 
	 */
	public void extractFacets(String outputFile) throws UnknownVariableName, InvalidIEQFileFormatException, IOException{

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
