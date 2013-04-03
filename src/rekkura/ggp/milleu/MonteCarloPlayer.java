package rekkura.ggp.milleu;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import rekkura.alg.DepthCharge;
import rekkura.ggp.milleu.Player.ProverBased;
import rekkura.model.Dob;
import rekkura.util.BestPair;
import rekkura.util.Colut;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

public class MonteCarloPlayer extends ProverBased {
	@Override protected void plan() { explore(); }
	@Override protected void move() { explore(); }
	@Override protected void reflect() { }

	private Random rand = new Random();
	private void explore() {
		setAction(anyMove());
		
		Game.Turn current = getTurn();
		Set<Dob> state = current.state;
		
		ListMultimap<Dob, Dob> actions = machine.getActions(state);
		List<Dob> playerActions = actions.get(role);
		
		Multiset<Dob> goals = HashMultiset.create();
		
		int charges = 0;
		BestPair<Float, Dob> best = BestPair.createReverseNatural(-Float.MAX_VALUE, null);
		while (validState()) {
			if (!chargePerAction(playerActions, goals)) break;
			charges++;
			
			// See if we need to update the move we want to make
			for (Dob action : goals.elementSet()) {
				float value = ((float) goals.count(action))/charges;
				if (best.consider(value, action)) setAction(current.turn, best.getCarry());
			}
		}
	}
	
	/**
	 * This method attempts to perform a single charge per action.
	 * It will bail if time has run out.
	 * 
	 * @param actions the set of moves we want to consider from the current state
	 * @param goals running sum of goal values
	 * @return returns true if all actions were considered and false otherwise.
	 */
	private boolean chargePerAction(Collection<Dob> actions, Multiset<Dob> goals) {
		Set<Dob> state = this.getTurn().state;
		
		for (Dob action : actions) {
			if (!validState()) return false;
			Map<Dob, Dob> fixed = Maps.newHashMap();
			fixed.put(role, action);
			
			List<Set<Dob>> charge = DepthCharge.fire(state, machine, fixed, rand);
			
			Set<Dob> terminal = Colut.end(charge);
			int goal = machine.getGoals(terminal).count(role);
			goals.add(action, goal);
		}
		
		return true;
	}
}