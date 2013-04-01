package rekkura.ggp.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
		this.demuxer = GgpProtocol.createDefaultPlayerDemuxer(player, fmt); 
	}
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		InputStream in = exchange.getRequestBody();
		DataInputStream din = new DataInputStream(in);
		
		String message = din.readUTF();
		String response = this.demuxer.handleMessage(message);

		OutputStream out = exchange.getResponseBody();
		DataOutputStream dout = new DataOutputStream(out);
		dout.writeUTF(response);
	}
	
}
