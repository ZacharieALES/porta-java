package formulation.ongoingwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import exception.UnknownCommandException;
import exception.UnknownVariableName;
import formulation.AbstractFormulation;
import formulation.Variable;
import utils.Dates;

/**
 * Generate the mediation cluster formulation
 * @author zach
 *
 */
public class MediationCluster extends AbstractFormulation{

	int n = -1;
	double[][] d;
	String inputFile;
	double alpha, beta;

	public MediationCluster(String inputFile, double alpha, double beta) throws UnknownCommandException, IOException, InterruptedException {

		super();
		
		this.inputFile = inputFile;

		this.alpha = alpha;
		this.beta = beta;

		InputStream ips;
		try {
			ips = new FileInputStream(inputFile);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);

			/* Read the first line */
			String ligne;

			d = null;

			/* Read the next lines */
			while ((ligne=br.readLine())!=null){

				//				System.out.println(ligne);

				if(n == -1) {
					if(ligne.contains("n") && ligne.contains("=")) {
						n = Integer.parseInt(ligne.split("=")[1]);
					}
				}
				else {
					if(d == null) {
						if(ligne.contains("=") && ligne.contains("[")) {
							ligne = ligne.split("\\[")[1];
							d = new double[n][n];						
						}
					}
					if(d != null){

						ligne = ligne.split(";")[0].split("]")[0];

						String[] values = ligne.trim().split(" ");

						if(values.length > 2) {
							int i = Integer.parseInt(values[0]);
							int j = Integer.parseInt(values[1]);
							double v = Double.parseDouble(values[2]);

							d[i][j] = v;
							d[j][i] = v;

							//							System.out.println("d[" + i + "," + j + "] = " + d[i][j]);
						}
					}
				}
			}
			br.close();
			ips.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 

	}

	@Override
	protected void createVariables() {
		for(int i = 0; i < n; i++) {

			addVariable(new Variable("m" + i, 0, 1));

			for(int j = i+1; j < n; j++) {
				addVariable(new Variable(xName(i,j), 0, 1));

				if(d[i][j] != 0) {
					addVariable(new Variable(tName(i,j), 0, 1));
					addVariable(new Variable(zName(i,j), 0, 1));
				}
				else {
					System.out.println(i + ", " + j);
				}
			}
		}
	}

	@Override
	public String getConstraints() throws UnknownVariableName {

		String output = "";

		/* Triangle inequalities */
		for(int i = 0; i < n; i++)
			for(int j = i+1; j < n; j++)
				for(int k = 0; k < n; k++)
					if(k != i && k != j) {
						output += x(i,k) + " + ";
						output += x(j,k) + " >= ";
						output += x(i,j) + "\n";
					}

		/* x_ij >= mi */
		for(int i = 0; i < n; i++)
			for(int j = i+1; j < n; ++j)
				if(i != j) {
					output += x(i,j) + " >= " + m(i) + "\n";
					output += x(i,j) + " >= " + m(j) + "\n";
				}

		/* tij >= mi + mj - 1
		 * tij <= mi
		 * tij <= mj */
		for(int i = 0; i < n; i++)
			for(int j = i+1; j < n; ++j)
				if(i != j && d[i][j] != 0) {
					output += t(i, j) + " >= " + m(i) + " + " + m(j) + " - 1\n";
					output += t(i,j) + " <= " + m(i) + "\n";
					output += t(i,j) + " <= " + m(j) + "\n";
				}

		/* sum_{i mediator} w-(i,mediators) <= alpha w+(i, mediators)) */
		String wm ="";
		String wp = "";
		for(int i = 0; i < n; i++)
			for(int j = i+1; j < n; ++j)
				if(i != j) {

					if(d[i][j] > 0) 
						wp += "+ " + (int)(100*alpha*Math.abs(Math.pow(10, 4) * d[i][j])) + " " + t(i, j) + " ";
					else if(d[i][j] < 0)
						wm += " + " + (int)(100*Math.abs(Math.pow(10, 4) * d[i][j])) + " " + t(i, j) + " ";

				}

		output += wm + " <= " + wp + "\n";

		/* sum_{i mediator} w-(i,!mediators) <= beta w+(i, !mediators)) */
		wm ="";
		wp = "";
		for(int i = 0; i < n; i++)
			for(int j = i+1; j < n; ++j)
				if(i != j) {

					if(d[i][j] > 0) {
						int coef = (int)(beta*Math.abs(Math.pow(10, 4) * d[i][j]));
						wp += "+" + coef + " " + z(i, j) + " - " + coef + " " + t(i, j) + " ";
					}
					else if(d[i][j] < 0)
						wm += "+" + (int)(Math.abs(Math.pow(10, 4) * d[i][j])) + " " + z(i, j) + " - " + (int)Math.abs(Math.pow(10, 4) * d[i][j]) + " " + t(i, j) + " ";

				}

		output += wm + " <= " + wp + "\n";

		/* zij >= mi
		 * zij >= mj
		 * zij <= mi + mj
		 */
		for(int i = 0; i < n; i++)
			for(int j = i+1; j < n; ++j)
				if(i != j && d[i][j] != 0) {
					output += z(i, j) + " >= " + m(i) + "\n";
					output += z(i, j) + " >= " + m(j) + "\n";
					output += z(i, j) + " <= " + m(i) + " + " + m(j) + "\n";
				}

		return output;
	}

	public String m(int i) throws UnknownVariableName {
		return portaName("m" + i);
	}

	public String x(int i, int j) throws UnknownVariableName {
		return portaName(xName(i, j));
	}

	public String z(int i, int j) throws UnknownVariableName {
		return portaName(zName(i, j));
	}

	public String t(int i, int j) throws UnknownVariableName {
		return portaName(tName(i, j));
	}

	public String xName(int i, int j) {
		return "x" + Math.min(i, j) + "_" + Math.max(i,  j);
	}

	public String zName(int i, int j) {
		return "z" + Math.min(i, j) + "_" + Math.max(i,  j);
	}

	public String tName(int i, int j) {
		return "t" + Math.min(i, j) + "_" + Math.max(i,  j);
	}



	public static void main(String[] args){

		try {
			String inputFile = "Section01_reduced_3";
			double alpha = 1.0;
			String date = Dates.date();
			String prefix = date + "_mediation_" + inputFile + "_";
			String folderPath = "./res/" + date + "_mediation_alpha" + alpha+ "_" + inputFile + "/"; 

			File f = new File(folderPath);

			if(!f.exists())
				f.mkdir();
			


			MediationCluster formulation = new MediationCluster("./data/" + inputFile + ".txt", alpha, alpha);

			formulation.getFacets(folderPath + "_facets_" + prefix + ".ieq");

			formulation.convertPOIFile("./.tmp/tmp.poi", folderPath + "_integer_points_" + prefix + ".poi");
			formulation.convertIEQFile("./.tmp/tmp.ieq", folderPath + "_formulation_" + prefix + ".ieq", false);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
