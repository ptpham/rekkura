package rekkura.logic;

import java.util.List;
import java.util.Map;

import rekkura.model.Atom;
import rekkura.model.Dob;
import rekkura.model.Rule;
import rekkura.util.Colut;

import com.google.common.collect.Lists;

/**
 * This class is responsible for working with paths between rules.
 * One query of interest is the way in which a dob is transformed
 * from the body of one rule to the head of another.
 * @author ptpham
 *
 */
public class Comprender {

	/**
	 * This returns the unifications from the head of the source 
	 * rule against the bodies of the destination rule.
	 * @param src
	 * @param dst
	 * @return
	 */
	public static List<Map<Dob, Dob>> unificationEdges(Rule src, Rule dst) {
		return Colut.filterNulls(bodyUnifications(src.head.dob, dst));
	}
	
	/**
	 * This method returns the unifications of the target against the body
	 * terms of the given rule.
	 * @param target
	 * @param rule
	 * @return
	 */
	public static List<Map<Dob, Dob>> bodyUnifications(Dob target, Rule rule) {
		List<Map<Dob, Dob>> result = Lists.newArrayList();
		for (Atom atom : rule.body) {
			result.add(Unifier.unifyVars(atom.dob, target, rule.vars));
		}
		return result;
	}
}
