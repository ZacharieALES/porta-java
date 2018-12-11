package formulation.ongoingwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import exception.InvalidPCenterInputFile;
import exception.UnknownCommandException;
import formulation.AbstractFormulation;
import formulation.LPReader;
import formulation.Variable;

public class TestLPTrain extends LPReader{


	public TestLPTrain(String lpfile)
			throws IOException, InvalidPCenterInputFile, UnknownCommandException, InterruptedException {
		super(lpfile);
	}

	String inputFile ="inputFile.txt";

	public enum InputType{
		POI_FILE,
		INTEGER_POINTS,
		LP_FILE
	}
	
	public static void main(String[] args){

		try {
			InputType it = InputType.INTEGER_POINTS;
			
			switch(it) {
			case INTEGER_POINTS: 
				
//				String inputFile = "./data/sncf/retestcplex64bits/modeleNominalSimplifie4rames.lp.solEntieres"; // 60 solutions
				String inputFile = "./data/sncf/retestcplex64bits/modeleNominalSimplifie4ramesSansCyclicite.lp.solEntieres"; // > 400 solutions
//				String inputFile = "./data/sncf/retestcplex64bits/modeleNominalSimplifieSansCyclicite.lp.solEntieres"; // 30 solutions
//				String inputFile = "./data/modeleNominalNonSimplifie.lp.solEntieres";				
//				String inputFile = "./data/modeleNominalSimplifie.lp.solEntieres";
				TestLPTrain formulation = new TestLPTrain(inputFile);
				formulation.createPOIFile(inputFile);
				String trafOutputFile = formulation.sTmpPOIFile +  ".ieq";
				String outputFile =  trafOutputFile + "_converted";

				System.out.println("=== Get the facets (input: " + formulation.sTmpPOIFile + ", output: " + trafOutputFile + ")");
				traf(formulation.sTmpPOIFile);

				System.out.println("=== Convert facets (input: " + trafOutputFile + ", output: " + outputFile + ")");
				formulation.convertIEQFile(trafOutputFile, outputFile, true);
				
				break;
			case POI_FILE: 
				
				outputFile = "./data/sncf3_facets.ieq";
				inputFile = "./data/sncf3.lp";
				formulation = new TestLPTrain(inputFile);

				String initialPOIFile = "./data/sncf3lp.poi";
				trafOutputFile = initialPOIFile.replace(".poi", ".poi.ieq");
				formulation.generateFormulation();
				String output = AbstractFormulation.dim(initialPOIFile);

				output = formulation.replacePortaVariablesInString(output);

				System.out.println("===== Print dim output: \n" + output);

				System.out.println("=== Get the facets (input: " + initialPOIFile + ", output: " + trafOutputFile + ")");
				traf(initialPOIFile);

				System.out.println("=== Convert facets (input: " + trafOutputFile + ", output: " + outputFile + ")");
				formulation.convertIEQFile(trafOutputFile, outputFile, true);
				break;
			case LP_FILE: 

				inputFile = "./data/sncf3.lp";
				formulation = new TestLPTrain(inputFile);

				System.out.println(formulation.getDimension());

				formulation.extractFacets("./.tmp/facet.ieq");

				formulation.convertPOIFile("./.tmp/tmp.poi", "./.tmp/" + inputFile + "_ordered_converted_integer_points.poi");
				formulation.convertIEQFile("./.tmp/tmp.ieq", "./.tmsncf1.lpp/" + inputFile + "_ordered_converted_formulation.ieq", false);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the POI file from an input file which contains:
	 * - the list of all the variables separated by spaces on the first line
	 * - the list of all the integer points (each line correspond to the value of the variables in an integer point)
	 *    
	 * @param inputFile
	 */
	public void createPOIFile(String inputFile) {

		try{
			InputStream ips=new FileInputStream(inputFile);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);

			System.out.println("=== Convert integer points in POI file (input: " + inputFile + ", output: " + sTmpPOIFile + ")");
			FileWriter fw = new FileWriter(sTmpPOIFile, false); // True if the text is appened at the end of the file, false if the content of the file is removed prior to write in it
			BufferedWriter output = new BufferedWriter(fw);

			String line;

			while ((line=br.readLine())!=null){

				/* Ensure that all the white spaces in the s are single spaces */
				line = line.trim();
				line = line.replace("\t", " ");

				int length = line.length();
				int previousLength = 0;

				while(length != previousLength) {
					line = line.replace("\t", "  ");
					line = line.replace("  ", " ");
					previousLength = length;
					length = line.length();
				}

				String[] sLine = line.split(" ");

				/* If this is the first line */
				if(variables.size() == 0) {

					for(int i = 0; i < sLine.length; ++i)
						registerVariable(new Variable(sLine[i]));

					output.write("DIM = " + variables.size() + "\n\nCONV_SECTION\n");
				}
				else {
					output.write(line + "\n");
					System.out.println(line);
				}
			}

			output.write("\nEND\n");
			output.flush();
			output.close();
			br.close();
		}catch(Exception e){
			System.out.println(e.toString());
		}


	}
}
