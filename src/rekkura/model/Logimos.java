package rekkura.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This holds a collection of "mini models".
 * @author ptpham
 *
 */
public class Logimos {
	
	public static class BodyAssignment {
		public final Dob ground;
		public final int position;
		public final Rule rule;
		
		public BodyAssignment(Dob ground, int position, Rule rule) {
			this.position = position;
			this.ground = ground;
			this.rule = rule;
		}
	}
	
	/**
	 * This class represents for each subtree in the given
	 * dob the set of things that have unified against it.
	 * @author ptpham
	 *
	 */
	public static class DobSpace {
		public final Dob form;
		public final Multimap<Dob, Dob> replacements;
		
		public DobSpace(Dob form) {
			this.form = form;
			this.replacements = HashMultimap.create();
		}
		
		@Override
		public String toString() {
			return "[" + this.form + ", " + replacements + "]";
		}
	}
	
	
}
