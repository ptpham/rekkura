package rekkura.ggp.app;

import java.io.IOException;
import java.net.InetSocketAddress;

import rekkura.ggp.milleu.Player;
import rekkura.ggp.net.ServerHarness;

import com.sun.net.httpserver.HttpServer;

public class PlayerServer {

	public static void main(String args[]) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(9148), 32);
		
		Player player = new Player.Legal();
		Thread thread = new Thread(player);
		thread.start();
		
		server.createContext("/", new ServerHarness(player));
		server.setExecutor(null);
		server.start();
	}
	
}
