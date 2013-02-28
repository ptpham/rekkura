package rekkura.util;

import java.util.Collection;
import java.util.List;

/**
 * (Collection Utilities)
 * @author ptpham
 *
 */
public class Colut {
	
	public static <T> void addAll(Collection<T> target, Collection<T> other) {
		if (other == null) return;
		target.addAll(other);
	}
	
	public static <T> boolean contains(Collection<T> s, T t) {
		return (s != null && s.contains(t));
	}
	
	public static <T> boolean nonEmpty(Collection<T> s) {
		return s != null && s.size() > 0;
	}
	
	public static <T> boolean empty(Collection<T> s) {
		return !nonEmpty(s);
	}
	
	public static <T> T any(Collection<T> s) {
		if (empty(s)) return null;
		return s.iterator().next();
	}
	
	public static <T> T end(List<T> list) {
		if (nonEmpty(list)) return list.get(list.size() - 1);
		return null;
	}
	
	public static Character end(String s) {
		if (s == null || s.length() == 0) return null;
		return s.charAt(s.length() - 1);
	}
}
