package rekkura.ggp.net;

import java.net.URL;
import java.util.List;
import java.util.Map;

import rekkura.fmt.KifFormat;
import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.model.Dob;
import rekkura.util.Netut;
import rekkura.util.Synchron;

import com.google.common.collect.Maps;


public class RemotePlayer extends Player {
	private URL url;
	private String match;
	
	public RemotePlayer(String match, URL url) {
		this.url = url;
		this.match = match;
	}
	
	@Override
	public void run() {
		KifFormat fmt = KifFormat.inst;
		
		// Send the match details to the remote address
		List<Dob> roles = Game.getRoles(config.rules);
		GgpProtocol.Start start = new GgpProtocol.Start(config, role, match);
		String message = fmt.toString(GgpProtocol.fromStart(start));
		Netut.lightExchange(message, url);
		System.out.println("Starting");
		
		// Wait the start time and then ask for the first move
		Synchron.lightSleep(GgpProtocol.getGgpStartClock(config));
		while (!isComplete()) {
			List<Dob> moves = getLastMoves(roles);
			System.out.println(moves);
			
			GgpProtocol.Play play = new GgpProtocol.Play(match, moves);
			message = fmt.toString(GgpProtocol.fromPlay(play));
			String response = Netut.lightExchange(message, url);
			
			Dob move = fmt.dobFromString(response);
			setDecision(getHistoryExtent(), Game.convertMoveToAction(role, move));
			
			waitForInput();
		}
		
		// Send a stop message on the last turn
		List<Dob> moves = getLastMoves(roles);
		GgpProtocol.Stop stop = new GgpProtocol.Stop(match, moves);
		message = fmt.toString(GgpProtocol.fromStop(stop));
		Netut.lightExchange(message, url);
	}
	
	private List<Dob> getLastMoves(List<Dob> roles) {
		Map<Dob, Dob> memory = Maps.newHashMap();
		if (getHistoryExtent() > 0) memory = getMemory(getHistoryExtent() - 1);
		List<Dob> moves = Game.convertActionMapToMoves(roles, memory);
		return moves;
	}

}
