package utils;

public class Utility {
	
	public static final double TOL = 1E-6;
	
	public static boolean isInteger(Double d) {
		return Math.abs(d - Math.round(d)) < TOL;
	}

}
