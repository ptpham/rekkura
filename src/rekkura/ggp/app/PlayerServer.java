package rekkura.ggp.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import rekkura.ggp.milleu.Player;
import rekkura.ggp.net.ServerHarness;

import com.sun.net.httpserver.HttpServer;

public class PlayerServer {

	public static void main(String args[]) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(9148), 32);
		
		server.createContext("/", new ServerHarness(Player.Legal.class));
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
	}
	
}
