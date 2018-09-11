package formulation.ongoingwork;

import java.io.IOException;

import exception.InvalidPCenterInputFile;
import exception.UnknownCommandException;
import formulation.AbstractFormulation;
import formulation.LPReader;

public class TestLPTrain extends LPReader{


	public TestLPTrain(String lpfile)
			throws IOException, InvalidPCenterInputFile, UnknownCommandException, InterruptedException {
		super(lpfile);
	}

	public static void main(String[] args){

		try {
			String inputFile = "./data/sncf3.lp";
			String outputFile = "./data/sncf3_facets.ieq";
			TestLPTrain formulation = new TestLPTrain(inputFile);

			String initialPOIFile = "./data/sncf3lp.poi";
			String trafOutputFile = initialPOIFile.replace(".poi", ".poi.ieq");
			formulation.generateFormulation();
			String output = AbstractFormulation.dim(initialPOIFile);

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
