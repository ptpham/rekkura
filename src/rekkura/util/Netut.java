package rekkura.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Network utilities.
 * @author ptpham
 *
 */
public class Netut {

	public static URL lightUrl(String url) {
		try { return new URL(url); }
		catch (MalformedURLException e) { throw new IllegalArgumentException(e); }
	}
	
	public static String lightExchange(String message, URL url) {
		try { return exchange(message, url); }
		catch (IOException e) { e.printStackTrace(); }
		return null;
	}

	public static String exchange(String message, URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
		writer.write(message);
		writer.flush();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		return reader.readLine();
	}
}
