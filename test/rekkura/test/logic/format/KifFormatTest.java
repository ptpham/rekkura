package rekkura.test.logic.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rekkura.logic.format.KifFormat;
import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;

public class KifFormatTest {

	private static KifFormat fmt = new KifFormat();
	
	@Test
	public void dob() {
		String raw = "(a (b c) d)";
		Dob dob = fmt.dobFromString(raw);
		assertEquals(raw, fmt.toString(dob));
	}

	@Test
	public void atom() {
		String raw = "(not (a e (b (c f)) d))";
		Atom atom = fmt.atomFromString(raw);
		assertEquals(raw, fmt.toString(atom));
	}
	
	@Test
	public void rule() {
		String raw = "(<= e ?g f (not (a (b c) d)))";
		Rule rule = fmt.ruleFromString(raw);
		assertEquals(raw, fmt.toString(rule));
		assertEquals(1, rule.vars.size());
	}
	
	@Test
	public void distinct() {
		String raw = "(<= e f g (not (a (b c) d)) " + 
						"(distinct a b) (distinct ?x ?y))";
		Rule rule = fmt.ruleFromString(raw);
		assertEquals(2, rule.distinct.size());
		assertEquals(raw, fmt.toString(rule));
	}
	
	@Test
	public void atomCompression() {
		String raw = "((proposition))";
		Atom proposition = fmt.atomFromString(raw);
		assertTrue(proposition.dob.isTerminal());
	}
	
	@Test
	public void dobSingleParen() {
		String raw = "(proposition)";
		Dob proposition = fmt.dobFromString(raw);
		assertEquals(raw, proposition.toString());
	}
	
	@Test
	public void dobDoubleParen() {
		String raw = "((proposition))";
		Dob proposition = fmt.dobFromString(raw);
		assertEquals(raw, proposition.toString());
	}

}
