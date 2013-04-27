package rekkura.test.logic;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Lists;

import rekkura.logic.Comprender;
import rekkura.logic.Pool;
import rekkura.model.Rule;

public class ComprenderTest {

	@Test
	public void merge() {
		String firstRaw = "{(X)(Y)(W)|<((f)(X)(Y)(a)(W)),true>:-<((g)(X)(Y)(W)),true>}";
		String secondRaw = "{(X)(Y)(Z)(W)|<((h)(Y)(X)(Z)),true>:-<((f)(Y)(X)(Z)(W)),true>}";
		String expectedRaw = "{(X)(Y)(W)|<((h)(Y)(X)(a)),true>:-<((g)(Y)(X)(W)),true>}";
		
		Pool pool = new Pool();
		Rule first = pool.rules.submerge(firstRaw);
		Rule second = pool.rules.submerge(secondRaw);
		Rule expected = pool.rules.submerge(expectedRaw);
		
		Assert.assertEquals(Lists.newArrayList(expected), 
			pool.rules.submerge(Comprender.mergeRules(first, second, pool)));
	}
}
