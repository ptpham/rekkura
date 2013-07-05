package rekkura.test.ggp.machina;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.logic.model.Dob;
import rekkura.state.model.StateMachine;
import rekkura.test.ggp.SimpleGames;
import rekkura.util.Colut;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public abstract class StateMachineTest {
	protected abstract GgpStateMachine.Factory<?> getFactory();
	
	@Test
	public void ticTacToe() {
		StateMachine<Set<Dob>, Dob> machine = getFactory().create(SimpleGames.getTicTacToe());
		Set<Dob> initial = machine.getInitial();
		assertEquals(10, initial.size());
		
		Multimap<Dob, Dob> joint = machine.getActions(initial);
		Map<Dob, Dob> actions = Maps.newHashMap();
		assertEquals(10, joint.size());
		
		for (Dob role : joint.keySet()) {
			actions.put(role, Colut.any(joint.get(role)));
		}
		
		Set<Dob> next = machine.nextState(initial, actions);
		assertEquals(10, next.size());
	}
	
	@Test
	public void connectFour() {
		StateMachine<Set<Dob>, Dob> machine = getFactory().create(SimpleGames.getConnectFour());
		Set<Dob> initial = machine.getInitial();
		assertEquals(43, initial.size());
		
		ListMultimap<Dob, Dob> actions = machine.getActions(initial);
		assertEquals(8, actions.size());
	}
}
