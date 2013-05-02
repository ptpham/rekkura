package rekkura.test.logic;

import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.logic.Comprender;
import rekkura.logic.Pool;
import rekkura.model.Rule;

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
	public void mergeNegation() {
		String firstRaw = "{(X)(Y)|<((f)(X)(Y)),true>:-<((g)(X)(Y)),true><((g)(Y)(X)),true>}";
		String secondRaw = "{(X)(Y)|<((h)(X)(Y)),true>:-<((f)(Y)(X)),false>}";
		String expectedFirst = "{(X)(Y)|<((h)(X)(Y)),true>:-<((g)(Y)(X)),false>}";
		String expectedSecond = "{(X)(Y)|<((h)(X)(Y)),true>:-<((g)(X)(Y)),false>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), 
			Sets.newHashSet(expectedFirst, expectedSecond));
	}
	
	@Test
	public void mergeComplex() {
		String firstRaw = "{(X)(Y)(W)|<((f)(X)(Y)(a)(W)),true>:-<((g)(X)(Y)(W)),true>}";
		String secondRaw = "{(X)(Y)(Z)(W)|<((h)(Y)(X)(Z)),true>:-<((f)(Y)(X)(Z)(W)),true>}";
		String expectedRaw = "{(X)(Y)(W)|<((h)(Y)(X)(a)),true>:-<((g)(Y)(X)(W)),true>}";
		
		runMerge(Lists.newArrayList(firstRaw, secondRaw), Sets.newHashSet(expectedRaw));
	}
	
	// TODO: Reordering test
	
	// TODO: Polish up de Morgan ... right now atoms may lose context when split
	
	private void runMerge(List<String> rawRules, Set<String> rawExpected) {
		Pool pool = new Pool();
		List<Rule> rules = pool.rules.submergeStrings(rawRules);
		
		Set<Rule> expected = Sets.newHashSet(pool.rules.submergeStrings(rawExpected));
		Set<Rule> actual = Sets.newHashSet(pool.rules.submerge(Comprender.mergeAll(rules, pool)));
		Assert.assertEquals(expected, actual);
	}
}
