package fr.neamar.kiss.utils.calculator;

import androidx.annotation.NonNull;

public final class Result<T> {
	static <T> Result<T> syntacticalError() {
		return new Result<>(true);
	}

	static <T> Result<T> arithmeticalError() {
		return new Result<>(false);
	}

	static <T> Result<T> result(@NonNull T result) {
		return new Result<>(result);
	}


	public final T result;
	public final boolean syntacticalError;
	public final boolean arithmeticalError;

	private Result(boolean isSyntactical) {
		this.syntacticalError = isSyntactical;
		this.arithmeticalError = !isSyntactical;
		this.result = null;
	}

	private Result(@NonNull T result) {
		this.result = result;
		this.syntacticalError = this.arithmeticalError = false;
	}
}