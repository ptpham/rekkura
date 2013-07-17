package rekkura.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.collect.Lists;

public class Stremut {

	public static List<String> readAll(InputStream stream) throws IOException {
		List<String> result = Lists.newArrayList();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		
		String line;
		while ((line = reader.readLine()) != null) result.add(line);
		return result;
	}

	
}
