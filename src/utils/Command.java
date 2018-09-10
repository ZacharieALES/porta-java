package utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import exception.UnknownVariableName;
import formulation.AbstractFormulationGenerator;


public class Command {
	
	/**
	 * Execute a command
	 * @param command The text of the command
	 * @return The command output; null if an error occurred
	 */
	public static String execute(String command){
		String result = "";
        
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            
            String s = null;    
            while ((s = br.readLine()) != null)
            	result += s + "\n";
                
            p.waitFor();
            p.destroy();
        } catch (Exception e) {result = null;}
        
        return result;
	}
	
	/**
	 * Execute a command
	 * @param command The text of the command
	 * @param outputFile The name of the file in which the command output must be written
	 * @return The file in which the result was output; null if an error occurred
	 */
	public static File execute(String command, String outputFile){
		String s = null;

        File f = new File(outputFile);
        String sParentPath = f.getParent();
		File fFolder = new File(sParentPath);
		
		if(!fFolder.exists())
			fFolder.mkdir();
		
		
        Process p;
        try {
        	
        	FileWriter fw = new FileWriter(f, false);
        	BufferedWriter bw = new BufferedWriter(fw);
        	
        	
            p = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            while ((s = br.readLine()) != null){
            	bw.write(s);
            	bw.flush();
            }
                
            p.waitFor();
            bw.write("exit: " + p.exitValue());
            bw.flush();
            bw.close();
            
            p.destroy();
        } catch (Exception e) {
        	f = null;
        }
        
        return f;
	}
	
}
