package rekkura.ggp.app;

import java.io.IOException;
import java.net.InetSocketAddress;

import rekkura.fmt.KifFormat;
import rekkura.ggp.milleu.Player;
import rekkura.ggp.net.ServerHarness;

import com.sun.net.httpserver.HttpServer;

public class PlayerServer {

	public static void main(String args[]) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(9147), 16);
		server.createContext("./", new ServerHarness(new Player.Legal(), new KifFormat()));
		server.setExecutor(null);
		server.start();
	}
	
}
