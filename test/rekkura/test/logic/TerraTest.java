package rekkura.test.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

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
	public void applyPositive() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),true><((h)(Y)),true>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList("((g)(a))", "((h)(b))");
		Map<String, String> rawExpected = ImmutableMap.of("(X)", "(a)", "(Y)", "(b)");
		checkApply(rawRule, rawCandidates, rawTruths, rawExpected);
	}
	
	@Test
	public void applyNegative() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),false><((h)(Y)),false>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList();
		Map<String, String> rawExpected = ImmutableMap.of("(X)", "(a)", "(Y)", "(b)");
		checkApply(rawRule, rawCandidates, rawTruths, rawExpected);
	}
	
	@Test
	public void applyPositiveFailure() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),false><((h)(Y)),true>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList("((g)(a))", "((h)(b))");
		Map<String, String> rawExpected = null;
		checkApply(rawRule, rawCandidates, rawTruths, rawExpected);
	}
	
	@Test
	public void applyNegativeFailure() {
		String rawRule = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)),false><((h)(Y)),true>}";
		List<String> rawCandidates = Lists.newArrayList("(a)", "(b)");
		List<String> rawTruths = Lists.newArrayList("((h)(c))");
		Map<String, String> rawExpected = null;
		checkApply(rawRule, rawCandidates, rawTruths, rawExpected);
	}

	private void checkApply(String rawRule, List<String> rawCandidates,
			List<String> rawTruths, Map<String, String> rawExpected) {
		Pool pool = new Pool();

		Rule rule = pool.rules.submergeString(rawRule);
		List<Dob> candidates = pool.dobs.submergeStrings(rawCandidates);
		Set<Dob> truths = Sets.newHashSet(pool.dobs.submergeStrings(rawTruths));
		
		Map<Dob, Dob> assignment = Terra.applyVariables(rule, candidates, truths, pool);
		if (rawExpected == null) {
			Assert.assertNull(assignment);
		} else {
			Assert.assertNotNull(assignment);
			Map<Dob, Dob> expected = pool.submergeUnifyStrings(rawExpected);
			Assert.assertEquals(expected, assignment);
		}
	}
}
