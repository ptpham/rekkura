package rekkura.test.logic;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.logic.Pool;
import rekkura.model.Rule;

public class PoolTest {

	@Test
	public void rule() {
		String firstRaw = "{(X)(Y)(W)|<((f)(X)(Y)(a)(W)),true>:-<((g)(X)(Y)(W)),true>}";
		String secondRaw = "{(X)(W)(Y)|<((f)(X)(Y)(a)(W)),true>:-<((g)(X)(Y)(W)),true>}";
		
		Pool pool = new Pool();
		Rule first = pool.rules.submergeString(firstRaw);
		Rule second = pool.rules.submergeString(secondRaw);
		
		Assert.assertEquals(first, second);
	}
	
}
