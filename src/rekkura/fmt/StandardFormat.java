package rekkura.fmt;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class StandardFormat extends LogicFormat {
	
	@Override
	public String toString(Dob dob) {
		if (dob.isTerminal()) return "(" + dob.name + ")";
		return "(" + dobsToString(dob.childIterable()) + ")";
	}

	private String dobsToString(Iterable<Dob> iterator) {
		StringBuilder builder = new StringBuilder();
		for (Dob dob : iterator) { builder.append(toString(dob)); }
		return builder.toString();
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
		int endIdx = s.length() - 1;
		String parts[] = s.split(",");
		if (s.length() < 3 || s.charAt(0) != '<'
				|| s.charAt(endIdx) != '>'
				|| parts.length != 2) {
			throw new Error("Atom parse error: " + s);
		}
		
		return new Atom(dobFromString(parts[0].replace("<", "")), 
				Boolean.parseBoolean(parts[1].replace(">", "").trim()));
	}
	
	@Override
	public String toString(Rule rule) {
		return "{" + dobsToString(rule.vars) + "|" + 
			toString(rule.head) + ":-" + atomsToString(rule.body) + "}";
	}
	
	public List<Atom> atomListFromString(String s) {
		List<Atom> result = Lists.newArrayList();
		for (String part : s.split("<|>")) {
			part = part.trim();
			if (part.length() > 0) {
				result.add(atomFromString("<" + part + ">"));
			}
		}
		return result;
	}
	
	private Set<Dob> dobSetFromString(String s) {
		return Sets.newHashSet(dobFromString("(" + s + ")").childIterable());
	}

	@Override
	public Rule ruleFromString(String s) {
		s = s.trim();
		int endIdx = s.length() - 1;
		String parts[] = s.split("\\||:-");
		if (s.length() < 5 || s.charAt(0) != '{'
			|| s.charAt(endIdx) != '}' || parts.length != 3) {
			throw new Error("Rule parse error: " + s);
		}
		
		Rule rule = new Rule();
		rule.head = atomFromString(parts[1]);
		rule.body = atomListFromString(parts[2].replace("}", ""));
		rule.vars = dobSetFromString(parts[0].replace("{", ""));	
		return rule;
	}
}
