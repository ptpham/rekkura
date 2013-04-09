package rekkura.test.ggp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rekkura.ggp.machina.GgpStateMachine;
import rekkura.model.Dob;
import rekkura.model.StateMachine;
import rekkura.util.Colut;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

@RunWith(Parameterized.class)
public class StateMachineTest {

	private GgpStateMachine.Factory<?> factory;
	public StateMachineTest(GgpStateMachine.Factory<?> factory) { this.factory = factory; }
	
	@Parameters
	public static Collection<Object[]> paramters() {
		return Arrays.asList(new Object[][] {
			{ GgpStateMachine.GENERIC_FORWARD_PROVER },
			{ GgpStateMachine.GENERIC_BACKWARD_PROVER },
			{ GgpStateMachine.BACKWARD_PROVER }
		});
	}
	
	@Test
	public void ticTacToe() {
		StateMachine<Set<Dob>, Dob> machine = factory.create(SimpleGames.getTicTacToe());
		Set<Dob> initial = machine.getInitial();
		Assert.assertEquals(10, initial.size());
		
		Multimap<Dob, Dob> joint = machine.getActions(initial);
		Map<Dob, Dob> actions = Maps.newHashMap();
		Assert.assertEquals(10, joint.size());
		
		for (Dob role : joint.keySet()) {
			actions.put(role, Colut.any(joint.get(role)));
		}
		
		Set<Dob> next = machine.nextState(initial, actions);
		Assert.assertEquals(10, next.size());
	}
	
}
