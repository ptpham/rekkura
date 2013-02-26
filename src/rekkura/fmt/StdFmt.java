package rekkura.fmt;

import java.util.List;
import java.util.Set;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class StdFmt {
	
	public String toString(Dob dob) {
		if (dob.isTerminal()) return "(" + dob.name + ")";
		return "(" + dobsToString(dob) + ")";
	}

	private String dobsToString(Iterable<Dob> iterator) {
		StringBuilder builder = new StringBuilder();
		for (Dob dob : iterator) { builder.append(toString(dob)); }
		return builder.toString();
	}
	
	public Dob dobFromString(String s) {
		s = s.trim();
		int endIdx = s.length() - 1;
		if (s.length() < 2 || s.charAt(0) != '(' || s.charAt(endIdx) != ')') {
			throw new Error("Dob parse error: " + s);
		}
		
		String inner = s.substring(1, endIdx);
		if (!inner.contains("(") && !inner.contains(")")) {
			return new Dob(inner);
		}
		
		List<Dob> dobs = Lists.newArrayList();
		int layer = 0;
		int lastIdx = 0;
		for (int i = 0; i < s.length(); i++) {
			char curChar = s.charAt(i);
			if (curChar == '(') layer++;
			else if (curChar == ')') {
				layer--;
				if (layer == 1) {
					Dob child = dobFromString(s.substring(lastIdx + 1, i + 1));
					dobs.add(child);
					lastIdx = i;
				}
			}
		}
		
		Dob result = new Dob(dobs);
		return result;
	}
	
	public String toString(Atom atom) {
		return "<" + toString(atom.dob) + "," + atom.truth + ">";
	}
	
	private String atomsToString(Iterable<Atom> atoms) {
		StringBuilder builder = new StringBuilder();
		for (Atom atom : atoms) { builder.append(toString(atom)); }
		return builder.toString();
	}
	
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
				Boolean.parseBoolean(parts[1].replace(">", "")));
	}
	
	public String toString(Rule rule) {
		return "{" + dobsToString(rule.vars) + "|" + 
			toString(rule.head) + ":-" + atomsToString(rule.body) + "}";
	}
	
	public List<Atom> atomListFromString(String s) {
		List<Atom> result = Lists.newArrayList();
		for (String part : s.split("<|>")) {
			if (part.length() > 0) {
				result.add(atomFromString("<" + s + ">"));
			}
		}
		return result;
	}
	
	private Set<Dob> dobSetFromString(String s) {
		return Sets.newHashSet(dobFromString("(" + s + ")"));
	}

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
