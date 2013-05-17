package rekkura.test.logic;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

public class PoolTest {

	@Test
	public void duplicateVars() {
		String firstRaw = "{(Y)(X)(Y)(X)|<((f)(X)(Y)(a)),true>:-<((g)(X)(Y)),true>}";
		String secondRaw = "{(X)(Y)|<((f)(X)(Y)(a)),true>:-<((g)(X)(Y)),true>}";
		comparePair(firstRaw, secondRaw);
	}

	@Test
	public void canonizeVars() {
		String firstRaw = "{(Y)(X)(W)|<((f)(X)(Y)(a)(W)),true>:-<((g)(X)(Y)(W)),true>}";
		String secondRaw = "{(W)(X)(Y)|<((f)(X)(Y)(a)(W)),true>:-<((g)(X)(Y)(W)),true>}";
		comparePair(firstRaw, secondRaw);
	}
	
	@Test
	public void canonizeDistinct() {
		String firstRaw = "{(X)(Y)(Q)|<(f),true>:-<((z)(Q))!=(Y)><(X)!=(Q)>}";
		String secondRaw = "{(Q)(X)(Y)|<(f),true>:-<(Q)!=(X)><(Y)!=((z)(Q))>}";
		comparePair(firstRaw, secondRaw);
	}
	
	@Test
	public void canonizeBody() {
		String firstRaw = "{(X)(Y)(Q)|<(f),true>:-<(Y),false><(t),false><((z)((w)(X))),true><((z)(Q)),true><(X),true>}";
		String secondRaw = "{(Q)(X)(Y)|<(f),true>:-<(X),true><((z)(Q)),true><((z)((w)(X))),true><(Y),false><(t),false>}";
		comparePair(firstRaw, secondRaw);
	}

	private void comparePair(String firstRaw, String canon) {
		Pool pool = new Pool();
		Rule first = pool.rules.submergeString(firstRaw);
		Rule second = pool.rules.submergeString(canon);
		
		Assert.assertEquals(first, second);
		Assert.assertEquals(canon, first.toString());
	}	
}
