package rekkura.test.logic.prover;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.logic.prover.StratifiedForward;
import rekkura.model.Dob;
import rekkura.model.Rule;

public class StratifiedForwardTest {

	@Test
	public void basic() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> }"
		};
		
		String[] rawDobs = {
			"((Q)(X))"
		};
		
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> dobs = fmt.dobsFromStrings(Arrays.asList(rawDobs));
		
		StratifiedForward prover = new StratifiedForward(rules);
		prover.queueTruth(dobs.get(0));
		Assert.assertTrue("Prover should have something to prove!", prover.hasMore());
		
		
	}

	
}
