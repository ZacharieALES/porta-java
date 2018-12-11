package formulation.ongoingwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import exception.InvalidPCenterInputFile;
import exception.UnknownCommandException;
import exception.UnknownVariableName;
import formulation.AbstractFormulation;
import formulation.Variable;
import utils.Dates;

/**
 * Generate an adaptation of the  p-center formulation from the article:
 * "A new formulation and resolution method for the p-centers problem"
 * ~2002
 * - Sourour Elloumi
 * - Martine Labbé
 * - Yves Pochet
 * 
 * in which all zk variables are removed and replaced by a unique variable kStar (which is defined such that the optimal radius is equal to D^kStar)
 *  
 * @author zach
 *
 */
public class PCenterPCRad extends AbstractFormulation {

	String inputFile;


	/** Number of clients */
	int N;

	/** Number of potential sites */
	int M;

	/** Distance between the sites (lines) and the factories (columns) */
	double[][] d;

	/** Ordered value of the existing distances in <d> */
	TreeSet<Double> D;

	/** Maximal number of selected factories */
	int p;

	/**
	 * Create a p-center formulation from an input file.
	 * The input file format is:
	 * - The first line contains 3 integers separated by a space, they respectively correspond to:
	 * 		- The number of client N
	 * 		- The number of factories M
	 * 		- The value of p (I guess...)
	 * - The N next lines contain M values such that the value on line i and column j is the distance between the client number i and the factory number j.
	 * These values must be separated by spaces
	 * @param inputFile
	 * @throws IOException 
	 * @throws InvalidPCenterInputFile 
	 * @throws UnknownCommandException 
	 * @throws InterruptedException 
	 */
	public PCenterPCRad(String inputFile) throws IOException, InvalidPCenterInputFile, UnknownCommandException, InterruptedException{

		super();
		
		this.inputFile = inputFile;

		InputStream ips=new FileInputStream(inputFile); 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);

		/* Read the first line */
		String ligne = br.readLine();
		String[] sTemp = ligne.split("\\s+");

		if(sTemp.length < 3){
			br.close();
			ips.close();
			throw new InvalidPCenterInputFile(inputFile, "The first line contains less than three values.");
		}

		N = Integer.parseInt(sTemp[0]);
		M = Integer.parseInt(sTemp[1]);
		p = Integer.parseInt(sTemp[2]);

		d = new double[N][M];
		D = new TreeSet<>();
		int clientNb = 1;

		/* Read the next lines */
		while ((ligne=br.readLine())!=null && clientNb <= M){
			sTemp = ligne.split("\\s+");

			if(sTemp.length < M){
				br.close();
				ips.close();
				throw new InvalidPCenterInputFile(inputFile, "Line n°" + clientNb + " contains less than " + M + " values separated by spaces.");
			}

			for(int j = 0 ; j < M ; j++){
				double cDouble = Double.parseDouble(sTemp[j]);
				d[clientNb - 1][j] = cDouble;
				D.add(cDouble);
			}

			clientNb++;			  

		}

		br.close();
		ips.close();

		if(clientNb - 1 < M)
			throw new InvalidPCenterInputFile(inputFile, "The file only contains " + (clientNb-1) + " distances lines instead of " + M);

//		System.out./ln(this);

	}

	/** Number of different values in the matrix -1 
	 * (i.e., there are K+1 values in the distance matrix)
	 */
	public int K(){return D == null ? 0: D.size()-1;}

	@Override
	public void createVariables() {

		Iterator<Double> it = D.iterator();
		it.next();

		registerVariable(new Variable("kStar", 0, K()));

		for(int i = 1 ; i <= M ; i++)
			registerVariable(new Variable("y" + i, 0, 1));

	}

	@Override
	public String getConstraints() throws UnknownVariableName {

		String output = "";

		/* At least one factory */
		for(int i = 1 ; i < M ; i++)
			output += portaName("y" + i) + " + "; 

		output += portaName("y" + M) + " >= 1\n";

		/* At most M factories */
		for(int i = 1 ; i < M ; i++)
			output += portaName("y" + i) + " + "; 

		output += portaName("y" + M) + " == " + p + "\n";
//		output += portaName("y" + M) + " <= " + p + "\n";

		/* Link between kStar and y variables */	
		for(int i = 1 ; i <= N ; i++){

			//			System.out.println("Client n°" + i );

			Iterator<Double> it = D.iterator();

			/* The first D value is not used in the formulation (it is just a constant in the objective) */
			it.next();

			List<String> previousYVariables = null;

			for(int k = 1 ; k <= K() ; k++){

				List<String> currentYVariables = new ArrayList<>();

				/* Get the radius associated to variable zk */
				Double currentD = it.next();

				//				System.out.println("CurrentD: " + currentD);

				for(int j = 1 ; j <= M ; j++){

					if(d[i-1][j-1] < currentD)
						currentYVariables.add("y" + j);
				}

				//				if(k != 1){
				//					System.out.println("--\ncurrent y var: " + currentYVariables);
				//					System.out.println("previous y var: " + previousYVariables);
				//				}

				/* If the previous K contained less y variables */
				if(k != 1 && currentYVariables.size() > previousYVariables.size()){

					for(String yVar: previousYVariables)
						output += (k-1) + " " + portaName(yVar) + " + ";

					output += portaName("kStar") + " >= " + (k-1) + "\n";	
					
				}
				//				else if(k != 1)
				//					System.out.println("z" + (k-1) + " >= 1 <- not added\n");

				previousYVariables = currentYVariables;

			}	

			/* Add the last k inequality */
			for(String yVar: previousYVariables)
				output += K() + " " + portaName(yVar) + " + ";

			//			System.out.println("--\n" + previousYVariables);
			//			System.out.println("z" + K() + " >= 1 <- added\n");
			output += portaName("kStar") + " >= " + K() + "\n";	
			
		}



		return output;
	}

	@Override
	public String toString(){
		String result = N + " " + M + " " + p + "\n";

		for(int i = 0 ; i < N ; i++){
			for(int j = 0 ; j < M ; j++)
				result += d[i][j] + " ";
			result += "\n";
		}

		return result;
	}

	public static void main(String[] args){

		try {
			
//			int pMax = 5;
			String date = Dates.date();
			String folderPath = "./res/" + date + "/"; 
			File f = new File(folderPath);
			
			if(!f.exists())
				f.mkdir();
			

			String inputFile = "pc3_2_2";//"pc_n5_p2_i_6";
			PCenterPCRad formulation = new PCenterPCRad("./data/" + inputFile + ".dat");

			String prefix = date + "_kstar_" + inputFile + "_";

			formulation.extractFacets(folderPath + prefix + "facets.ieq");

			formulation.convertPOIFile("./.tmp/tmp.poi", folderPath + prefix + inputFile + "_integer_points.poi");
			formulation.convertIEQFile("./.tmp/tmp.ieq", folderPath + prefix + inputFile + "_formulation.ieq", false);
			
//			for(int p = 1; p <= pMax; p++) {
//				String inputFile = "pc5_" + p + "_prime";//"pc5_3"; //"pc20_5";
//				PCenterPCRad formulation = new PCenterPCRad("./data/" + inputFile + ".dat");
//
//				String prefix = date + "_kstar_" + inputFile + "_";
//
//				formulation.getFacets(folderPath + prefix + "facets.ieq");
//
//				formulation.convertPOIFile("./.tmp/tmp.poi", folderPath + prefix + inputFile + "_integer_points.poi");
//				formulation.convertIEQFile("./.tmp/tmp.ieq", folderPath + prefix + inputFile + "_formulation.ieq", false);
//				//			formulation.convertIEQFile("./.tmp/tmp.poi.ieq", "./.tmp/converted_facets.ieq");
//			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
