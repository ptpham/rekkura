package rekkura.test.logic.algorithm;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import rekkura.logic.algorithm.Merge;
import rekkura.logic.algorithm.Merges;
import rekkura.logic.model.Rule;
import rekkura.logic.structure.Pool;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class MergeTest {

	@Test
	public void posSubGatherSource() {
		String srcRaw = "{|<((q)(a)),true>:-<((r)(a)),true><((r)(b)),true>}";
		String dstRaw = "{(x)|<((r)(x)),true>:-<((z)(x)),true>}";
		List<String> expectedRaw = Lists.newArrayList(
			"{|<((q)(a)),true>:-<((r)(b)),true><((z)(a)),true>}",
			"{|<((q)(a)),true>:-<((r)(a)),true><((z)(b)),true>}");
		run(srcRaw, dstRaw, expectedRaw, Merges.POSITIVE_SUBSTITUTION);
	}
	
	@Test
	public void posSubIgnoreNegative() {
		String srcRaw = "{|<((q)(a)),true>:-<((r)(a)),false><((r)(b)),false>}";
		String dstRaw = "{(x)|<((r)(x)),true>:-<((z)(x)),true>}";
		List<String> expectedRaw = Lists.newArrayList();
		run(srcRaw, dstRaw, expectedRaw, Merges.POSITIVE_SUBSTITUTION);
	}

	protected void run(String srcRaw, String dstRaw, List<String> expectedRaw, Merge.Operation merge) {
		Pool pool = new Pool();
		Rule dst = pool.rules.submergeString(srcRaw);
		Rule src = pool.rules.submergeString(dstRaw);
		Set<Rule> expected = Sets.newHashSet(pool.rules.submergeStrings(expectedRaw));
		Set<Rule> result = Sets.newHashSet(Merge.applyOperation(src, dst, merge, pool));
		Assert.assertEquals(expected, result);
	}
	
}
