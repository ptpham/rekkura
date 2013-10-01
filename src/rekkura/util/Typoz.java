package rekkura.util;

public class Typoz {

	public static Integer lightParseInt(String value) {
		try { return Integer.parseInt(value); } 
		catch (Exception e) { return null; }
	}
}
