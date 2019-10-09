package fr.neamar.kiss.utils;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class CompletedFuture <T> implements Future<T> {
	private final T object;

	public CompletedFuture(T object) {
		this.object = object;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public T get() {
		return object;
	}

	@Override
	public T get(long timeout, TimeUnit unit) {
		return get();
	}
}