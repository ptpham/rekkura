package rekkura.test.ggp;

import org.junit.Assert;
import org.junit.Test;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.ggp.net.GgpProtocol;
import rekkura.ggp.net.GgpProtocol.DefaultPlayerHandler;
import rekkura.ggp.net.GgpProtocol.GgpState;
import rekkura.ggp.net.GgpProtocol.PlayerDemuxer;
import rekkura.model.Dob;

import com.google.common.collect.Lists;

public class GgpProtocolTest {

	@Test
	public void demuxStartAndStop() {
		PlayerDemuxer demux = GgpProtocol.createDefaultPlayerDemuxer(Player.Legal.class);
		
		String start = "(start match_id 1000 ((<= hello)) 2000)";
		demux.handleMessage(start);
		
		String stop = "(stop match_id ())";
		demux.handleMessage(stop);
	}
	
	@Test
	public void handlerStartAndStop() {
		DefaultPlayerHandler<Player.Legal> handler = 
				GgpProtocol.createDefaultPlayerHandler(Player.Legal.class);
		
		String match = "match";
		Dob role = new Dob("role");
		Game.Config config = GgpTestUtil.createBlitzConfig(SimpleGames.getTicTacToe());
		handler.handleStart(match, role, config);
		Assert.assertEquals(1, handler.players.size());
		
		GgpState state = handler.players.get(match);
		Assert.assertEquals(GgpProtocol.getGgpStartClock(config), state.ggpStartClock);
		Assert.assertEquals(GgpProtocol.getGgpPlayClock(config), state.ggpPlayClock);
		Assert.assertEquals(Player.Legal.class, state.player.getClass());
		Assert.assertNotNull(state.player);
		
		handler.handleStop(match, Lists.newArrayList(new Dob("(x)"), new Dob("o")));
		Assert.assertEquals(0, handler.players.size());
	}
	
	
}
