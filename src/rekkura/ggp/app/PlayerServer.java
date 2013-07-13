package rekkura.ggp.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import rekkura.ggp.milleu.Player;
import rekkura.ggp.net.ServerHarness;

import com.sun.net.httpserver.HttpServer;

public class PlayerServer {

	public static void runWith(Class<? extends Player> player, String name, int port) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 32);
		
		server.createContext("/", new ServerHarness(player, name));
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();

	}
	
	public static void main(String args[]) throws IOException {
		runWith(Player.Legal.class, "Rekkura-Legal", 9148);
	}
	
}
