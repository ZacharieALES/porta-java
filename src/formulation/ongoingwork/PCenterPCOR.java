package formulation.ongoingwork;

import java.io.IOException;

import exception.InvalidPCenterInputFile;
import exception.UnknownCommandException;
import exception.UnknownVariableName;
import formulation.Variable;

public class PCenterPCOR extends PCenterPCSCZOrdered{

	public PCenterPCOR(String inputFile) throws IOException,
	InvalidPCenterInputFile, UnknownCommandException, InterruptedException {
		super(inputFile);
	}

	@Override
	public void createVariables() {

		super.createVariables();
		addVariable(new Variable("kStar", 0, K()));

	}



	@Override
	public String getInequalities() throws UnknownVariableName {

		super.getInequalities();
		
		String output = "";

		output += " - " + portaName("kStar");
		
		for(int k = 1 ; k <= K() ; k++)
			output += " + " + portaName("z" + k);
		
		output += "== 0";
		
		return output;
	}

	public static void main(String[] args){

		try {
			String inputFile = "pc5_3"; //"pc20_5"; //"pc3_2";//
			PCenterPCOR formulation = new PCenterPCOR("./data/" + inputFile + ".dat");
			//
			System.out.println(formulation.getDimension());
			
			formulation.getFacets("./.tmp/facet.ieq");
			
			//			Porta.getFacets(formulation, "./.tmp/" + inputFile + "_ordered_facets.ieq");

			formulation.convertPOIFile("./.tmp/tmp.poi", "./.tmp/" + inputFile + "_ordered_converted_integer_points.poi");
			formulation.convertIEQFile("./.tmp/tmp.ieq", "./.tmp/" + inputFile + "_ordered_converted_formulation.ieq", false);
			//			formulation.convertIEQFile("./.tmp/tmp.poi.ieq", "./.tmp/" + inputFile + "_ordered_converted_facets.ieq");
			//			formulation.convertIEQFile("./.tmp/tmp.poi.ieq", "./.tmp/converted_facets.ieq");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


}
