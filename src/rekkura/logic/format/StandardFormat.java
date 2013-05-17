package rekkura.logic.format;

import java.util.List;
import java.util.Stack;

import rekkura.logic.model.Atom;
import rekkura.logic.model.Dob;
import rekkura.logic.model.Rule;

import com.google.common.collect.Lists;

/**
 * Apologies for the grossness. 
 * 
 * Dob Example: ((f)((g)(X)))
 * Atom Example: <((f)(X)),true>
 * Rule Example: {(X)(Y)|<((p)(X)),true>:-<((q)(X)(Y)),true><(X)!=(Y)>}
 * 
 * The rule reads "p(X) being true is entailed if there exists 
 * variables X and Y such such that q(X)(Y) is true and that
 * X is not equal to Y".
 * 
 * @author ptpham
 *
 */
public class StandardFormat extends LogicFormat {
	
	public static final String NOT_EQUAL = "!=";

	@Override
	public String toString(Dob dob) {
		StringBuilder builder = new StringBuilder();
		appendDobToString(dob, builder);
		return builder.toString();
	}
	
	private String dobsToString(Iterable<Dob> iterator) {
		StringBuilder builder = new StringBuilder();
		for (Dob dob : iterator) { appendDobToString(dob, builder); }
		return builder.toString();
	}
	
	protected void appendDobToString(Dob dob, StringBuilder builder) {
		builder.append("(");
		if (dob.isTerminal()) builder.append(dob.name);
		else appendDobsToString(dob.childIterable(), builder);
		builder.append(")");
	}
	
	protected void appendDobsToString(Iterable<Dob> iterator, StringBuilder builder) {
		for (Dob dob : iterator) { appendDobToString(dob, builder); }
	}
	
	@Override
	public Dob dobFromString(String s) {
		try { return dobParse(s); }
		catch (Exception e) { dobParseError(s); }
		return null;
	}

	protected void dobParseError(String s) {
		new Error("Dob parse error: " + s);
	}

	private Dob dobParse(String s) {
		s = s.trim();
		if (!s.startsWith("(")) return new Dob(s);
		
		Stack<List<Dob>> dobs = new Stack<List<Dob>>();
		StringBuilder builder = new StringBuilder();
		
		dobs.push(Lists.<Dob>newArrayList());
		for (int i = 0; i < s.length(); i++) {
			char curChar = s.charAt(i);
			if (curChar == '(') {
				dobs.push(Lists.<Dob>newArrayList());
			} else if (curChar == ')') {
				String name = builder.toString();
				Dob created = null;
				List<Dob> curList = dobs.pop();
				if (name.length() > 0) created = new Dob(name);
				else created = new Dob(curList);
				dobs.peek().add(created);
				builder = new StringBuilder();
			} else if (Character.isLetterOrDigit(curChar)) {
				builder.append(curChar);
			}
		}
		
		if (dobs.size() != 1) dobParseError(s);
		List<Dob> result = dobs.pop();
		
		if (result.size() != 1) dobParseError(s);
		return result.get(0);
	}
	
	@Override
	public String toString(Atom atom) {
		return "<" + toString(atom.dob) + "," + atom.truth + ">";
	}
	
	private String atomsToString(Iterable<Atom> atoms) {
		StringBuilder builder = new StringBuilder();
		for (Atom atom : atoms) { builder.append(toString(atom)); }
		return builder.toString();
	}
	
	@Override
	public Atom atomFromString(String s) {
		s = s.trim();
		String parts[] = s.split(",");
		if (s.length() < 3 || s.charAt(0) != '<'
				|| !s.endsWith(">")
				|| parts.length != 2) {
			throw new Error("Atom parse error: " + s);
		}
		
		return new Atom(dobFromString(parts[0].replace("<", "")), 
				Boolean.parseBoolean(parts[1].replace(">", "").trim()));
	}
	
	private Rule.Distinct distinctFromString(String string) {
		String[] split = string.split(NOT_EQUAL);
		if (split.length != 2 || split[0].charAt(0) != '<' ||
				!split[1].endsWith(">")) {
			throw new Error("Distinct parse error: " + string);
		}
		
		String first = split[0].replace("<", "");
		String second = split[1].replace(">", "");
		
		return new Rule.Distinct(dobFromString(first), dobFromString(second));
	}
	
	@Override
	public String toString(Rule rule) {
		StringBuilder distinct = new StringBuilder();
		for (Rule.Distinct entry : rule.distinct) {
			appendDistinct(distinct, entry);			
		}
		
		return "{" + dobsToString(rule.vars) + "|" + 
			toString(rule.head) + ":-" + atomsToString(rule.body) +
			distinct.toString() + "}";
	}
	
	@Override
	public String toString(Rule.Distinct distinct) {
		StringBuilder builder = new StringBuilder();
		appendDistinct(builder, distinct);
		return builder.toString();
	}

	private void appendDistinct(StringBuilder distinct, Rule.Distinct entry) {
		distinct.append("<");
		distinct.append(toString(entry.first));
		distinct.append("!=");
		distinct.append(toString(entry.second));
		distinct.append(">");
	}
	
	public List<Atom> atomListFromString(String s) {
		List<Atom> result = Lists.newArrayList();
		for (String part : s.split("<|>")) {
			if (part.contains(NOT_EQUAL)) continue;
			
			part = part.trim();
			if (part.length() > 0) {
				result.add(atomFromString("<" + part + ">"));
			}
		}
		return result;
	}
	
	public List<Rule.Distinct> distinctListFromString(String s) {
		List<Rule.Distinct> result = Lists.newArrayList();
		for (String part : s.split("<|>")) {
			if (!part.contains(NOT_EQUAL)) continue;
			
			part = part.trim();
			if (part.length() > 0) {
				result.add(distinctFromString("<" + part + ">"));
			}
		}
		return result;
	}
	
	private List<Dob> dobListFromString(String s) {
		return dobFromString("(" + s + ")").childCopy();
	}

	@Override
	public Rule ruleFromString(String s) {
		s = s.trim();
		String parts[] = s.split("\\||:-");
		if (checkRuleValidity(s, parts)) {
			throw new Error("Rule parse error: " + s);
		}
		
		String constraints = parts[2].replace("}", "");
		
		Atom head = atomFromString(parts[1]);
		List<Atom> body = atomListFromString(constraints);
		List<Dob> vars = dobListFromString(parts[0].replace("{", ""));
		List<Rule.Distinct> distinct = distinctListFromString(constraints);
		Rule rule = new Rule(head, body, vars, distinct);
		
		return rule;
	}
	
	private boolean checkRuleValidity(String s, String[] parts) {
		return s.length() < 5 || s.charAt(0) != '{'
			|| !s.endsWith("}") || parts.length != 3;
	}
	
	public static final StandardFormat inst = new StandardFormat();

}
