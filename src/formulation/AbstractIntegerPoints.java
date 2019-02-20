package formulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	
	public void generateIntegerPoints() throws UnknownVariableName {

		System.out.println("=== Generate the integer points (output: " + sTmpPOIFile + ")");
		writeIntegerPointsInDefaultFile();
			
	}
	
	/**
	 * Generate the formulation in the default file
	 * @throws UnknownVariableName
	 * @throws InvalidIEQFileFormatException 
	 */
	@Override
	public String generateFormulation() throws UnknownVariableName, InvalidIEQFileFormatException{

		/* When considering integer points, P = I(P) */
		return generateIPFormulation();
	}
}
