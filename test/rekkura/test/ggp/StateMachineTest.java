package rekkura.test.ggp;

import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.ggp.machina.ProverStateMachine;
import rekkura.model.Dob;
import rekkura.util.Colut;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class StateMachineTest {

	@Test
	public void ticTacToe() {
		ProverStateMachine machine = ProverStateMachine.createWithStratifiedBackward(SimpleGames.getTicTacToe());
		Set<Dob> initial = machine.getInitial();
		
		Multimap<Dob, Dob> joint = machine.getActions(initial);
		Map<Dob, Dob> actions = Maps.newHashMap();
		
		for (Dob role : joint.keySet()) {
			actions.put(role, Colut.any(joint.get(role)));
		}
		
		Set<Dob> next = machine.nextState(initial, actions);
		Assert.assertEquals(10, next.size());
	}
	
}
