package rekkura.fmt;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
	
	private Map<Dob, Dob> distinctFromString(String string) {
		String[] split = string.split(NOT_EQUAL);
		if (split.length != 2 || split[0].charAt(0) != '<' ||
				!split[1].endsWith(">")) {
			throw new Error("Distinct parse error: " + string);
		}
		
		String first = split[0].replace("<", "");
		String second = split[1].replace(">", "");
		
		Map<Dob, Dob> result = Maps.newHashMap();
		result.put(dobFromString(first), dobFromString(second));
		return result;
	}
	
	@Override
	public String toString(Rule rule) {
		StringBuilder distinct = new StringBuilder();
		for (Map.Entry<Dob, Dob> entry : rule.distinct.entrySet()) {
			distinct.append("<");
			distinct.append(toString(entry.getKey()));
			distinct.append("!=");
			distinct.append(toString(entry.getValue()));
			distinct.append(">");			
		}
		
		return "{" + dobsToString(rule.vars) + "|" + 
			toString(rule.head) + ":-" + atomsToString(rule.body) +
			distinct.toString() + "}";
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
	
	public Map<Dob, Dob> distinctListFromString(String s) {
		Map<Dob, Dob> result = Maps.newHashMap();
		for (String part : s.split("<|>")) {
			if (!part.contains(NOT_EQUAL)) continue;
			
			part = part.trim();
			if (part.length() > 0) {
				result.putAll(distinctFromString("<" + part + ">"));
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
		Rule rule = new Rule();
		rule.head = atomFromString(parts[1]);
		rule.body = atomListFromString(constraints);
		rule.vars = dobListFromString(parts[0].replace("{", ""));
		rule.distinct = distinctListFromString(constraints);
		
		return rule;
	}
	
	private boolean checkRuleValidity(String s, String[] parts) {
		return s.length() < 5 || s.charAt(0) != '{'
			|| !s.endsWith("}") || parts.length != 3;
	}
	
	public static final StandardFormat inst = new StandardFormat();
}
