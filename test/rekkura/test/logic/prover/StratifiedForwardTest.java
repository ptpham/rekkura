package rekkura.test.logic.prover;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Lists;

import rekkura.fmt.LogicFormat;
import rekkura.fmt.StandardFormat;
import rekkura.logic.prover.StratifiedForward;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;

public class StratifiedForwardTest {

	@Test
	public void basic() {
		String[] rawRules = { 
			"{(X) | <((Q)(X)),true> :- <((P)(X)),true> }",
			"{(X) | <((R)(X)),true> :- <((Q)(X)),true> }"
		};
		
		String[] rawDobs = {
			"((P)(X))",
			"((Q)(X))",
			"((R)(X))"
		};
		
		LogicFormat fmt = new StandardFormat();
		List<Rule> rules = fmt.rulesFromStrings(Arrays.asList(rawRules));
		List<Dob> dobs = fmt.dobsFromStrings(Arrays.asList(rawDobs));
		
		StratifiedForward prover = new StratifiedForward(rules);
		prover.reset(Lists.newArrayList(dobs.get(0)));
		Assert.assertTrue("Prover should have something to prove!", prover.hasMore());
		
		Set<Dob> proven = prover.proveNext();
		Assert.assertTrue(proven.size() == 1);
		Assert.assertEquals(rawDobs[1], fmt.toString(Colut.any(proven)));
	}

	
}
