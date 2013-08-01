package rekkura.ggp.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
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
		GgpProtocol.PlayerDemuxer demux = GgpProtocol.createDefaultPlayerDemuxer(player, name);
		ExecutorService service = Executors.newFixedThreadPool(32);
		ServerSocket server = new ServerSocket(port);
		while (true) {
			final Socket socket = server.accept();
			ggpBaseExecuteRequest(demux, socket, service);
		}
	}

	private static void ggpBaseExecuteRequest(
			final GgpProtocol.PlayerDemuxer demux, final Socket socket, ExecutorService service) {
		service.execute(new Runnable() { public void run() { ggpBaseHandleRequest(demux, socket); } });
	}

	private static void ggpBaseHandleRequest(GgpProtocol.PlayerDemuxer demux, Socket socket) {
		try {
			String message = HttpReader.readAsServer(socket);
			String response = demux.handleMessage(message);
			HttpWriter.writeAsServer(socket, response);
			socket.close(); 
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void main(String args[]) throws IOException {
		runWith(Player.Legal.class, "Rekkura-Legal", 9148);
	}
}
