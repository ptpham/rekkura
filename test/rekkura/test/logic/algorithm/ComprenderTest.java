package rekkura.test.logic.algorithm;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import rekkura.logic.algorithm.Comprender;
import rekkura.logic.algorithm.Merge;
import rekkura.logic.algorithm.Merges;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ComprenderTest {
	
	@Test
	public void mergeNoVars() {
		String firstRaw = "{|<(f),true>:-<(g),true><(p),true>}";
		String secondRaw = "{|<(h),true>:-<(f),true>}";
		String expectedRaw = "{|<(h),true>:-<(g),true><(p),true>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), Sets.newHashSet(expectedRaw));
	}
	
	@Test
	public void mergeNoMatch() {
		String firstRaw = "{|<(z),true>:-<(g),true><(p),true>}";
		String secondRaw = "{|<(h),true>:-<(f),true>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), Sets.<String>newHashSet());
	}
	
	@Test
	public void mergeVarUnion() {
		String firstRaw = "{(X)|<((z)(X)),true>:-<(X),true><(p),true>}";
		String secondRaw = "{(X)(Y)|<((h)(Y)),true>:-<((z)(X)),true>}";
		String expectedRaw = "{(X)(Y)|<((h)(Y)),true>:-<(X),true><(p),true>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), Sets.newHashSet(expectedRaw));
	}
	
	@Test
	public void mergeVarBinding() {
		String firstRaw = "{(X)|<((z)(X)),true>:-<(X),true><(p),true>}";
		String secondRaw = "{|<((h)(a)),true>:-<((z)(a)),true>}";
		String expectedRaw = "{|<((h)(a)),true>:-<(a),true><(p),true>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), Sets.newHashSet(expectedRaw));
	}
	
	@Test
	public void mergeSwapVars() {
		String firstRaw = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)(Y)),true>}";
		String secondRaw = "{(X)(Y)|<((h)(X)(Y)),true>:-<((f)(Y)(X)),true>}";
		String expectedRaw = "{(X)(Y)|<((h)(X)(Y)),true>:-<((g)(Y)(X)),true>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), Sets.newHashSet(expectedRaw));
	}
	
	@Test
	public void variableConflict() {
		String firstRaw = "{(X)(Y)|<((f)(X)),true>:-<((g)(X)(Y)),true>}";
		String secondRaw = "{(X)(Y)|<((h)(X)(Y)),true>:-<((f)(X)),true>}";
		
		Pool pool = new Pool();
		List<Rule> rules = pool.rules.submergeStrings(Lists.newArrayList(firstRaw, secondRaw));
		List<Rule> generated = Comprender.mergeAll(rules, pool, Merges.posSub);
		assertEquals(1, generated.size());
		
		Rule rule = generated.get(0);
		assertEquals(3, rule.vars.size());
	}
	
	@Test
	public void combination() {
		String firstRaw = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)(Y)),true><((g)(Y)(X)),true>}";
		String secondRaw = "{(X)(Y)|<((h)(X)(Y)),true>:-<((f)(Y)(X)),false>}";
		String expectedFirst = "{(X)(Y)|<((h)(X)(Y)),true>:-<((g)(Y)(X)),false>}";
		String expectedSecond = "{(X)(Y)|<((h)(X)(Y)),true>:-<((g)(X)(Y)),false>}";
		
		Merge.Operation op = Merge.combine(Merges.posSub, Merges.negSplit);
		runMerge(Lists.newArrayList(firstRaw, secondRaw), 
			Sets.newHashSet(expectedFirst, expectedSecond), op);
	}
	
	@Test
	public void mergeComplex() {
		String firstRaw = "{(X)(Y)(W)|<((f)(X)(Y)(a)(W)),true>:-<((g)(X)(Y)(W)),true>}";
		String secondRaw = "{(X)(Y)(Z)(W)|<((h)(Y)(X)(Z)),true>:-<((f)(Y)(X)(Z)(W)),true>}";
		String expectedRaw = "{(X)(Y)(W)|<((h)(Y)(X)(a)),true>:-<((g)(Y)(X)(W)),true>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), Sets.newHashSet(expectedRaw));
	}
	
	private void runMerge(List<String> rawRules, Set<String> rawExpected) {
		runMerge(rawRules, rawExpected, Merges.posSub);
	}
	
	private void runMerge(List<String> rawRules, Set<String> rawExpected, Merge.Operation op) {
		Pool pool = new Pool();
		List<Rule> rules = pool.rules.submergeStrings(rawRules);
		
		Set<Rule> expected = Sets.newHashSet(pool.rules.submergeStrings(rawExpected));
		Set<Rule> actual = Sets.newHashSet(pool.rules.submerge(Comprender.mergeAll(rules, pool, op)));
		assertEquals(expected, actual);
	}
}
