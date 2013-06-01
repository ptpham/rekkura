package rekkura.ggp.milleu;

import java.util.List;

import rekkura.ggp.milleu.Game.Config;
import rekkura.ggp.player.RemotePlayer;
import rekkura.util.Colut;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

public class Match {
	public final Game.Config config;
	public final String id, game;
	
	private Match(String id, String game, Game.Config config) {
		this.config = config;
		this.game = game;
		
		if (Colut.empty(id)) {
			id = "rekkura-default-match-id-";
			id += Long.toString(Math.round(Math.random()));
		}
		this.id = id;
	}

	public static class Builder {
		public String match, game;
		public Game.Config config;
		public Match build() { return new Match(match, game, config); }
	}
	
	public static Builder newBuilder(Config config) {
		Builder result = new Builder();
		result.config = config;
		return result;
	}
	
	public RemotePlayer getRemote(String url) {
		return new RemotePlayer(id, url);
	}
	
	public MatchRunnable newRunnable(Player... players) {
		return newRunnable(Lists.newArrayList(players));
	}
	
	public MatchRunnable newRunnable(List<Player> players) {
		return newRunnable(players, null);
	}
	
	public MatchRunnable newRunnable(List<Player> players, EventBus bus) {
		MatchRunnable result = new MatchRunnable(this, bus);
		result.players.addAll(players);
		return result;
	}
}
