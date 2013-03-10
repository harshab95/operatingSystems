package nachos.userprog;

/**
 * This is a class designed to contain all of the constants for Project 2.
 * @author japhethwong
 *
 */
public class Constants {
	// System Call Constants  (all in UserProcess.java)
	public static final int EXEC_ERROR_CODE = -1;
	public static final int JOIN_SUCCESS_CODE = 1;
	public static final int JOIN_ERROR_CODE = 0;
	public static final int MAX_ARG_LENGTH = 256;
	public static final int NO_PROCESS_ID = -1;
	public static final int STATUS_EXITED = 0;
	public static final int STATUS_READY = 1;
	
	// FileDescriptorManager Constants
	public static final int INVALID_DESCRIPTOR = -1;
}