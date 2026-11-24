package io.github.rohimahajan6.parabank.guard;

/** Thrown when a test tries to create or change data somewhere it must not. */
public class WriteBlockedException extends IllegalStateException {

    public WriteBlockedException(String message) {
        super(message);
    }
}
