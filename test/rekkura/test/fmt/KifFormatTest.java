package rekkura.test.fmt;

import junit.framework.Assert;

import org.junit.Test;

import rekkura.fmt.KifFormat;
import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

public class KifFormatTest {

	private static KifFormat fmt = new KifFormat();
	
	@Test
	public void dob() {
		String raw = "(a (b c) d)";
		Dob dob = fmt.dobFromString(raw);
		Assert.assertEquals(raw, fmt.toString(dob));
	}

	@Test
	public void atom() {
		String raw = "(not (a e (b (c f)) d))";
		Atom atom = fmt.atomFromString(raw);
		Assert.assertEquals(raw, fmt.toString(atom));
	}
	
	@Test
	public void rule() {
		String raw = "(<= e f ?g (not (a (b c) d)))";
		Rule rule = fmt.ruleFromString(raw);
		Assert.assertEquals(raw, fmt.toString(rule));
		Assert.assertEquals(1, rule.vars.size());
	}
	
	@Test
	public void distinct() {
		String raw = "(<= e f g (not (a (b c) d)) " + 
						"(distinct a b) (distinct ?x ?y))";
		Rule rule = fmt.ruleFromString(raw);
		Assert.assertEquals(2, rule.distinct.size());
		Assert.assertEquals(raw, fmt.toString(rule));
	}
	
	
}
