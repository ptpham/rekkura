package rekkura.util;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class CartesianWalker<U> {
		public List<U> current;
		
		Stack<Iterator<U>> ongoing = new Stack<Iterator<U>>();
		List<Iterable<U>> candidates;
		
		@SuppressWarnings("unchecked")
		public CartesianWalker(List<Iterable<U>> candidates) {
			for (Iterable<U> iterable : candidates) {
				Preconditions.checkArgument(iterable.iterator().hasNext(), 
						"Each candidate iterable must have at least one candidate.");
			}

			this.candidates = Lists.newArrayList(candidates);
			if (this.candidates.size() == 0) this.candidates.add(Lists.<U>newArrayList((U)null));
			this.current = Lists.newArrayListWithCapacity(candidates.size());
			replenish();
		}

		private void replenish() {
			while (this.ongoing.size() < this.candidates.size()) {
				int curSize = this.ongoing.size();
				this.ongoing.push(this.candidates.get(curSize).iterator());
			}
		}
		
		public boolean nextAssignment() {
			// Remove expended iterators
			while (ongoing.size() > 0 && !ongoing.peek().hasNext()) {
				this.ongoing.pop();
				while (current.size() > ongoing.size()) Colut.removeEnd(current);
			}	
			int depletedSize = ongoing.size();
			if (ongoing.size() == 0) return false;
			replenish();
				
			int begin = Math.min(current.size(), depletedSize - 1);
			for (int i = begin; i < this.candidates.size(); i++) {
				U u = ongoing.get(i).next();
				Colut.addAt(this.current, i, u);
			}
			
			return true;
		}
	}
