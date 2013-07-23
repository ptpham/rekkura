package rekkura.ggp.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import org.ggp.base.util.http.HttpReader;
import org.ggp.base.util.http.HttpWriter;

import rekkura.ggp.milleu.Player;
import rekkura.ggp.net.GgpProtocol;
import rekkura.ggp.net.ServerHarness;

import com.sun.net.httpserver.HttpServer;

public class PlayerServer {

	public static HttpServer runWith(Class<? extends Player> player, String name, int port) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 32);
		
		server.createContext("/", new ServerHarness(player, name));
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		return server;
	}
	
	public static void runWithGgpBaseHttp(Class<? extends Player> player, String name, int port) throws IOException {
		ServerSocket server = new ServerSocket(port);
		GgpProtocol.PlayerDemuxer demux = GgpProtocol.createDefaultPlayerDemuxer(player, name);
		
		while (true) {
			try { ggpBaseHttpInternal(server, demux); }
			catch (Exception e) { e.printStackTrace(); }
		}
	}

	private static void ggpBaseHttpInternal(ServerSocket server,
			GgpProtocol.PlayerDemuxer demux) throws IOException {
		Socket socket = server.accept();
		String message = HttpReader.readAsServer(socket);
		String response = demux.handleMessage(message);
		HttpWriter.writeAsServer(socket, response);
		socket.close();
	}
	
	public static void main(String args[]) throws IOException {
		runWith(Player.Legal.class, "Rekkura-Legal", 9148);
	}
}
