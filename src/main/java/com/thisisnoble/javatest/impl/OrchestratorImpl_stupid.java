package com.thisisnoble.javatest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.thisisnoble.javatest.Event;
import com.thisisnoble.javatest.Orchestrator;
import com.thisisnoble.javatest.Processor;
import com.thisisnoble.javatest.Publisher;

public class OrchestratorImpl_stupid implements Orchestrator {

	private Publisher publisher;
	private List<Processor> processors = new ArrayList<Processor>();
	private ConcurrentHashMap<Event, CompositeEvent> eventsToProcessMap = new ConcurrentHashMap<Event, CompositeEvent>();
	private ConcurrentHashMap<Event, AtomicInteger> resultStatus = new ConcurrentHashMap<Event, AtomicInteger>();

	@Override
	synchronized public void register(Processor processor) {
		processors.add(processor);
	}

	@Override
	public void receive(Event event) {
		CompositeEvent compositeEvent = eventsToProcessMap.putIfAbsent(event, new CompositeEvent(event));
		AtomicInteger processorsCount = resultStatus.putIfAbsent(event, new AtomicInteger(0));
		if (compositeEvent == null) {
			processorsCount = resultStatus.get(event);
			for (Processor p : processors) {
				if (p.interestedIn(event)) {
					p.process(event);
					processorsCount.incrementAndGet();
				}
			}
		} else {
			synchronized (compositeEvent) {
				compositeEvent.addChild(event);
				if (compositeEvent.size() == processorsCount.get()) {
					publisher.publish(compositeEvent);
					eventsToProcessMap.remove(event);
					resultStatus.remove(event);
				}
			}
		}
	}

	@Override
	synchronized public void setup(Publisher publisher) {

		if (publisher == null) {
			throw new IllegalArgumentException("Publisher can't be null.");
		}
		this.publisher = publisher;
	}

}
