package rekkura.test.fmt;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import rekkura.fmt.KifFormat;
import rekkura.model.Rule;

public class KifFormatTest {

	KifFormat fmt = new KifFormat();
	
	@Test
	public void negation() {
		String raw = "(<= blarg (not(something(something))))";
		Rule rule = fmt.ruleFromString(raw);
		Assert.assertEquals(1, rule.body.size());
		Assert.assertTrue(!rule.body.get(0).truth);
	}
	
	@Test
	public void deor() {
		String raw = "(<= (connfour ?x) (or(col ?x)(row ?x)(diag1 ?x)(diag2 ?x)))";
		List<Rule> rules = fmt.deor(fmt.ruleFromString(raw));
		Assert.assertEquals(4, rules.size());
	}
	
	@Test
	public void deorCrash() {
		String raw = "(<= terminal (not (boardOpen)))";
		Rule rule = fmt.ruleFromString(raw);
		System.out.println(fmt.deor(rule));
	}
}
