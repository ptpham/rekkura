package rekkura.ggp.players;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import rekkura.ggp.milleu.Game;
import rekkura.ggp.milleu.Player;
import rekkura.model.Dob;

import com.google.common.collect.ListMultimap;

public class ConsolePlayer extends Player.ProverBased {
	@Override protected void plan() { queryHumanMove(); }
	@Override protected void move() { queryHumanMove(); }
	
	@Override protected void reflect() { 
		System.out.println("Goals: " + machine.getGoals(getTurn().state));
	}
	
	private void queryHumanMove() {
		setDecision(anyDecision());
		Game.Turn turn = getTurn();
		
		// Print out what happened last turn
		if (turn.turn > 0) {
			int lastTurn = turn.turn - 1;
			System.out.println("Moves on turn " + lastTurn + ": " + getMemory(lastTurn));
		}
		
		ListMultimap<Dob, Dob> actions = machine.getActions(turn.state);
		List<Dob> available = actions.get(role);
		
		for (int i = 0; i < available.size(); i++) {
			System.out.println(i + ") " + available.get(i));
		}
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			int selection = Integer.parseInt(reader.readLine());
			setDecision(turn.turn, available.get(selection));
		} catch (Throwable e) { setDecision(anyDecision()); }
	}
}
