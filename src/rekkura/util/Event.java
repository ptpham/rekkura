package rekkura.util;

import com.google.common.eventbus.EventBus;

public class Event {
	public static void post(EventBus bus, Object o) {
		if (bus != null) bus.post(o);
	}
	
	public static void register(EventBus bus, Object o) {
		if (bus != null) bus.register(o);
	}
	
	public static void unregister(EventBus bus, Object o) {
		if (bus != null) bus.unregister(o);
	}
}
