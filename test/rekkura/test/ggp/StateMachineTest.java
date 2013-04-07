package rekkura.test.ggp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rekkura.fmt.StandardFormat;
import rekkura.ggp.machina.GgpStateMachine;
import rekkura.model.Dob;
import rekkura.model.StateMachine;
import rekkura.util.Colut;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
		
		ListMultimap<Dob, Dob> joint = machine.getActions(initial);
		Map<Dob, Dob> actions = Maps.newHashMap();
		
		for (Dob role : joint.keySet()) {
			actions.put(role, Colut.any(joint.get(role)));
		}
		
		Set<Dob> next = machine.nextState(initial, actions);
		Assert.assertEquals(10, next.size());
	}
	
	@Test
	public void ticTacToeGoals() {
		StateMachine<Set<Dob>, Dob> machine = factory.create(SimpleGames.getTicTacToe());

		List<String> rawActions = Lists.newArrayList(
				"((does)(o)((mark)(1)(2)))",
				"((does)(o)((mark)(2)(1)))",
				"((does)(o)((mark)(2)(3)))",
				"((does)(o)((mark)(2)(2)))");
		
		List<Dob> actions = StandardFormat.inst.dobsFromStrings(rawActions);
		
		Set<Dob> state = machine.getInitial();
		for (Dob action : actions) {
			Map<Dob, Dob> map = Colut.asMap(null, action);
			state = machine.nextState(state, map);
		}
		
		Assert.assertTrue(machine.isTerminal(state));
	}
	
}
