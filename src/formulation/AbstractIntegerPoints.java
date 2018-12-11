package formulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import exception.InvalidIEQFileFormatException;
import exception.UnknownCommandException;
import exception.UnknownVariableName;

/**
 * Representation of a polytope by its integer points (i.e., feasible integer solutions)
 * @author zach
 *
 */
public abstract class AbstractIntegerPoints extends AbstractPolytope{

	public AbstractIntegerPoints() throws UnknownCommandException, IOException, InterruptedException {
		super();
		integerPoints = new ArrayList<>();
	}
	
	private List<IntegerPoint> integerPoints;

	
	protected void addIntegerPoint(IntegerPoint point) {
		this.integerPoints.add(point);
	}
	
	/**
	 * Function which fills the list {@code integerPoints}
	 * @throws UnknownVariableName 
	 */
	public abstract void createIntegerPoints() throws UnknownVariableName;
	
	public String getIntegerPoints() throws UnknownVariableName, IOException {

		String results = "";

		writeIntegerPointsInFile(sTmpPOIFile);
		convertPOIFile(sTmpPOIFile, sTmpConvertedPOIFile);
		
		try{
			InputStream ips=new FileInputStream(sTmpConvertedPOIFile);
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
	 * Generate the integer points file in the default location
	 * @throws UnknownVariableName
	 */
	public void writeIntegerPointsInDefaultFile() throws UnknownVariableName{
		writeIntegerPointsInFile(sTmpPOIFile);
	}

	/** Generate the integer points file in a specified location
	 * @param ieqFile The considered file
	 * @throws UnknownVariableName
	 */
	public void writeIntegerPointsInFile(String ieqFile) throws UnknownVariableName{

		File tmpFile = new File(ieqFile);

		String parentPath = tmpFile.getParent();
		File tmpFolder = null;
		
		try{
			tmpFolder = new File(parentPath);
		}
		catch(NullPointerException e) {
			tmpFolder = new File("./");
		}

		/* Create the temporary folder if necessary */
		if(!tmpFolder.exists())
			tmpFolder.mkdir();

		initializeVariables();
		createIntegerPoints();

		/* Create the porta ieq file */ 
		FileWriter fw;
		try {
			fw = new FileWriter(tmpFile);
			BufferedWriter bw = new BufferedWriter(fw);

			bw.write("DIM=" + variables.size() + "\n\n");
			
			bw.write("CONV_SECTION\n");
			
			for(IntegerPoint p: this.integerPoints)
				bw.write(p + "\n");

			bw.write("\n\nEND\n");

			bw.write(getvariablesindex());
			bw.flush();
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
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

		System.out.println("=== Generate the integer points");
		writeIntegerPointsInDefaultFile();

		System.out.println("=== Get the dimension");
		System.out.println("INITIAL DIMENSION : " + variables.size());
		output = dim(this.sTmpPOIFile);

		output = replacePortaVariablesInString(output);

		return output;

	}



	/**
	 * Extract the facets associated to this formulation.
	 * @return
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException
	 * @throws IOException
	 */
	public String getFacets() throws UnknownVariableName, InvalidIEQFileFormatException, IOException{

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
	 * Use porta to get the facets of the integer polytope associated to this formulation and write them in a file
	 * @param generator The generator associated to the considered formulation
	 * @param outputFile The file in which the facets will be added; null if an error occurred
	 * @throws UnknownVariableName 
	 * @throws InvalidIEQFileFormatException 
	 * @throws IOException  
	 */
	public void writeFacetsInFile(String outputFile) throws UnknownVariableName, InvalidIEQFileFormatException, IOException{

		System.out.println("=== Generate the integer points (output: " + sTmpPOIFile + ")");
		writeIntegerPointsInDefaultFile();
	
		String outputTrafFile = sTmpPOIFile.replace(".poi", ".poi.ieq");

		System.out.println("=== Get the facets (input: " + sTmpPOIFile + ", output: " + outputTrafFile + ")");
		traf(sTmpPOIFile);

		System.out.println("=== Convert facets (input: " + outputTrafFile + ", output: " + outputFile + ")");
		convertIEQFile(outputTrafFile, outputFile, true);

	}
}
