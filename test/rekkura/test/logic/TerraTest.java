package rekkura.test.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import rekkura.logic.algorithm.Terra;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TerraTest {
	
	@Test
	public void applyVarsPositive() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),true><((h)(Y)),true>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList("((g)(a))", "((h)(b))");
		Map<String, String> rawExpected = ImmutableMap.of("(X)", "(a)", "(Y)", "(b)");
		checkApplyVars(rawRule, rawCandidates, rawTruths, rawExpected);
	}
	
	@Test
	public void applyVarsNegative() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),false><((h)(Y)),false>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList();
		Map<String, String> rawExpected = ImmutableMap.of("(X)", "(a)", "(Y)", "(b)");
		checkApplyVars(rawRule, rawCandidates, rawTruths, rawExpected);
	}
	
	@Test
	public void applyVarsPositiveFailure() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),false><((h)(Y)),true>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList("((g)(a))", "((h)(b))");
		Map<String, String> rawExpected = null;
		checkApplyVars(rawRule, rawCandidates, rawTruths, rawExpected);
	}
	
	@Test
	public void applyVarsNegativeFailure() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),false><((h)(Y)),true>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList("((h)(c))");
		Map<String, String> rawExpected = null;
		checkApplyVars(rawRule, rawCandidates, rawTruths, rawExpected);
	}
	
	@Test
	public void applyVarsMissingVars() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),false><((h)(Y)),true>}";
		List<String> rawCandidates = Lists.newArrayList("(a)");
		List<String> rawTruths = Lists.newArrayList("((h)(c))");
		Map<String, String> rawExpected = null;
		checkApplyVars(rawRule, rawCandidates, rawTruths, rawExpected);
	}

	private void checkApplyVars(String rawRule, List<String> rawCandidates,
			List<String> rawTruths, Map<String, String> rawExpected) {
		Pool pool = new Pool();

		Rule rule = pool.rules.submergeString(rawRule);
		List<Dob> candidates = pool.dobs.submergeStrings(rawCandidates);
		Set<Dob> truths = Sets.newHashSet(pool.dobs.submergeStrings(rawTruths));
		
		Map<Dob, Dob> assignment = Terra.applyVars(rule, candidates, truths, pool);
		if (rawExpected == null) {
			assertNull(assignment);
		} else {
			assertNotNull(assignment);
			Map<Dob, Dob> expected = pool.submergeUnifyStrings(rawExpected);
			assertEquals(expected, assignment);
		}
	}
}
