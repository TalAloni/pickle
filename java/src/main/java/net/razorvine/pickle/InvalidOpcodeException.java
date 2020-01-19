package net.razorvine.pickle;

/**
 * Exception thrown when the unpickler encounters an invalid opcode.
 *
 * @author Irmen de Jong (irmen@razorvine.net)
 */
public class InvalidOpcodeException extends PickleException {

	private static final long serialVersionUID = -7691944009311968713L;

	public InvalidOpcodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidOpcodeException(String message) {
		super(message);
	}

}
