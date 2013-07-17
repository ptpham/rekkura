package rekkura.ggp.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import rekkura.ggp.milleu.Player;
import rekkura.util.Stremut;

import com.google.common.base.Joiner;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * This wrapper will expose a {@link Player} to the internet as a server
 * according to the given logical format.
 * @author "ptpham"
 *
 */
public class ServerHarness implements HttpHandler {

	public final GgpProtocol.PlayerDemuxer demuxer;
	
	public <U extends Player> ServerHarness(Class<U> player, String name) { 
		this(GgpProtocol.createDefaultPlayerDemuxer(player, name)); 
	}
	
	public ServerHarness(GgpProtocol.PlayerDemuxer demuxer) {
		this.demuxer = demuxer;
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			handleInternal(exchange);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	private void handleInternal(HttpExchange exchange) throws IOException {
		InputStream in = exchange.getRequestBody();
		String message = Joiner.on(" ").join(Stremut.readAll(in));
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
