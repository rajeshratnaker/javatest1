package com.thisisnoble.javatest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.thisisnoble.javatest.Event;
import com.thisisnoble.javatest.Orchestrator;
import com.thisisnoble.javatest.Processor;
import com.thisisnoble.javatest.Publisher;

public class OrchestratorImpl implements Orchestrator {

	private Publisher publisher;
	private List<Processor> processors = new ArrayList<Processor>();
	private CompositeEvent resultantEvent;
	private AtomicInteger expected = new AtomicInteger(0);

	@Override
	synchronized public void register(Processor processor) {
		processors.add(processor);
	}

	@Override
	public void receive(Event event) {
		if (resultantEvent == null) {
			resultantEvent = new CompositeEvent(event);
			for (Processor p : processors) {
				if (p.interestedIn(event)) {
					p.process(event);
					expected.incrementAndGet();
				}
			}
		} else {
			synchronized (resultantEvent) {
				resultantEvent.addChild(event);
				if (resultantEvent.size() == expected.get()) {
					publisher.publish(resultantEvent);
					expected.set(0);
					resultantEvent = null;
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
