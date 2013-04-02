package rekkura.ggp.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import rekkura.fmt.LogicFormat;
import rekkura.ggp.milleu.Player;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This wrapper will expose a player to the internet as a server
 * according to the given logical format.
 * @author "ptpham"
 *
 */
public class ServerHarness implements HttpHandler {

	public final GgpProtocol.PlayerDemuxer demuxer;
	
	public ServerHarness(Player player, LogicFormat fmt) { 
		this(GgpProtocol.createDefaultPlayerDemuxer(player, fmt)); 
	}
	public ServerHarness(GgpProtocol.PlayerDemuxer demuxer) {
		this.demuxer = demuxer;
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		InputStream in = exchange.getRequestBody();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		
		String message = reader.readLine();
		String response = this.demuxer.handleMessage(message);
		if (response == null || response.isEmpty()) response = "Invalid Protocol Exception";
		in.close();

		exchange.sendResponseHeaders(200, response.length());
		
		OutputStream out = exchange.getResponseBody();
		PrintWriter writer = new PrintWriter(out);
		writer.write(response);
		writer.flush();
		out.close();
	}
	
}
