package nachos.ag;
// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.



import nachos.machine.*;
import nachos.security.*;
import nachos.threads.*;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The default autograder. Loads the kernel, and then tests it using
 * <tt>Kernel.selfTest()</tt>.
 */
public class AutoGrader {
	/**
	 * Allocate a new autograder.
	 */
	public String testName, testNumber;
	public AutoGrader() {
		
	}

	/**
	 * Start this autograder. Extract the <tt>-#</tt> arguments, call
	 * <tt>init()</tt>, load and initialize the kernel, and call
	 * <tt>run()</tt>.
	 *
	 * @param	privilege      	encapsulates privileged access to the Nachos
	 * 				machine.
	 */
	public void start(Privilege privilege, String test) {
		Lib.assertTrue(this.privilege == null,
				"start() called multiple times");
		this.privilege = privilege;
		testName = test;

		String[] args = Machine.getCommandLineArguments();
		
		extractArguments(args);
		System.out.print(" grader");

		init();

		System.out.print("\n");	

		kernel =
				(Kernel) Lib.constructObject(Config.getString("Kernel.kernel"));
		kernel.initialize(args);
		
		//TODO redundant line
		this.testName = test;
		runTests();
		run();
	}

	private boolean runTests() {
		/*
		 * parse(testParams)
		 * if (test = "p1T"){
		 * 		KThread.selfTest();
		 * ) {
		 * 	
		 */
		if (testName == null) {
			return false;
		}
		
		
		
		System.out.println("\n---------------- Starting runTests() ");
		System.out.println("** Test Name   : " + testName);
		System.out.println("*  Test Number : " + testNumber + "\n");
		
		KThread kt = new KThread();
		if(testName.equals("join"))
			kt.selfTest();
		else if(testName.equals("conditional variables"))
			Condition2.selfTest();
		else if(testName.equals("alarm"))
			Alarm.selfTest();
		else if(testName.equals("communicator"))
			Communicator.selfTest();
		else if(testName.equals("priority scheduler"))
			PriorityScheduler.selfTest();
		else if(testName.equals("boat")) {
			Integer testNum = Integer.parseInt(testNumber);
			switch(testNum) {
			case 1: Boat.selfTest1();
			break;
			case 2: Boat.selfTest2();
			break;
			case 3: Boat.selfTest3();
			break;
			case 4: Boat.selfTest4();
			break;
			case 5: Boat.selfTest5();
			break;
			case 6: Boat.selfTest6();
			break;
			case 7: Boat.selfTest7();
			break;
			case 8: Boat.selfTest8();
			break;
			case 9: Boat.selfTest9();
			break;
			case 10: Boat.selfTest10();
			break;
			case 11: Boat.selfTest11();
			}
		}
		else {
			System.out.println("####ERROR#### No test run! Check your parameters to nachos");
		}
		
		//Communicator.selfTest();
		//Boat.selfTest();
		return true;
	}

	private void extractArguments(String[] args) {
		String testArgsString = Config.getString("AutoGrader.testArgs");
		if (testArgsString == null) {
			testArgsString = "";
		}

		for (int i=0; i<args.length; ) {
			String arg = args[i++];
			if (arg.length() > 0 && arg.charAt(0) == '-') {
				if (arg.equals("-#")) {
					Lib.assertTrue(i < args.length,
							"-# switch missing argument");
					testArgsString = args[i++];
				}
			}
		}

		StringTokenizer st = new StringTokenizer(testArgsString, ",\n\t\f\r");

		while (st.hasMoreTokens()) {
			StringTokenizer pair = new StringTokenizer(st.nextToken(), "=");

			Lib.assertTrue(pair.hasMoreTokens(),
					"test argument missing key");
			String key = pair.nextToken();

			Lib.assertTrue(pair.hasMoreTokens(),
					"test argument missing value");
			String value = pair.nextToken();

			testArgs.put(key, value);
		}	
	}

	String getStringArgument(String key) {
		String value = (String) testArgs.get(key);
		Lib.assertTrue(value != null,
				"getStringArgument(" + key + ") failed to find key");
		return value;
	}

	int getIntegerArgument(String key) {
		try {
			return Integer.parseInt(getStringArgument(key));
		}
		catch (NumberFormatException e) {
			Lib.assertNotReached("getIntegerArgument(" + key + ") failed: " +
					"value is not an integer");
			return 0;
		}
	}

	boolean getBooleanArgument(String key) {
		String value = getStringArgument(key);

		if (value.equals("1") || value.toLowerCase().equals("true")) {
			return true;
		}
		else if (value.equals("0") || value.toLowerCase().equals("false")) {
			return false;
		}
		else {
			Lib.assertNotReached("getBooleanArgument(" + key + ") failed: " +
					"value is not a boolean");
			return false;
		}	
	}

	long getTime() {
		return privilege.stats.totalTicks;
	}

	void targetLevel(int targetLevel) {
		this.targetLevel = targetLevel;
	}

	void level(int level) {
		this.level++;	
		Lib.assertTrue(level == this.level,
				"level() advanced more than one step: test jumped ahead");

		if (level == targetLevel)
			done();
	}

	private int level = 0, targetLevel = 0;

	void done() {
		System.out.print("\nsuccess\n");
		privilege.exit(162);
	}

	private Hashtable<String, String> testArgs = new Hashtable<String, String>();

	void init() {
	}

	void run() {
		//kernel.selfTest();
		kernel.run();
		kernel.terminate();
	}

	Privilege privilege = null;
	Kernel kernel;

	/**
	 * Notify the autograder that the specified thread is the idle thread.
	 * <tt>KThread.createIdleThread()</tt> <i>must</i> call this method before
	 * forking the idle thread.
	 *
	 * @param	idleThread	the idle thread.
	 */
	public void setIdleThread(KThread idleThread) {
	}

	/**
	 * Notify the autograder that the specified thread has moved to the ready
	 * state. <tt>KThread.ready()</tt> <i>must</i> call this method before
	 * returning.
	 *
	 * @param	thread	the thread that has been added to the ready set.
	 */
	public void readyThread(KThread thread) {
	}

	/**
	 * Notify the autograder that the specified thread is now running.
	 * <tt>KThread.restoreState()</tt> <i>must</i> call this method before
	 * returning.
	 *
	 * @param	thread	the thread that is now running.
	 */
	public void runningThread(KThread thread) {
		privilege.tcb.associateThread(thread);
		currentThread = thread;
	}

	/**
	 * Notify the autograder that the current thread has finished.
	 * <tt>KThread.finish()</tt> <i>must</i> call this method before putting
	 * the thread to sleep and scheduling its TCB to be destroyed.
	 */
	public void finishingCurrentThread() {
		privilege.tcb.authorizeDestroy(currentThread);
	}

	/**
	 * Notify the autograder that a timer interrupt occurred and was handled by
	 * software if a timer interrupt handler was installed. Called by the
	 * hardware timer.
	 *
	 * @param	privilege	proves the authenticity of this call.
	 * @param	time	the actual time at which the timer interrupt was
	 *			issued.
	 */
	public void timerInterrupt(Privilege privilege, long time) {
		Lib.assertTrue(privilege == this.privilege,
				"security violation");
	}

	/**
	 * Notify the autograder that a user program executed a syscall
	 * instruction.
	 *
	 * @param	privilege	proves the authenticity of this call.
	 * @return	<tt>true</tt> if the kernel exception handler should be called.
	 */
	public boolean exceptionHandler(Privilege privilege) {
		Lib.assertTrue(privilege == this.privilege,
				"security violation");
		return true;
	}

	/**
	 * Notify the autograder that <tt>Processor.run()</tt> was invoked. This
	 * can be used to simulate user programs.
	 *
	 * @param	privilege	proves the authenticity of this call.
	 */
	public void runProcessor(Privilege privilege) {
		Lib.assertTrue(privilege == this.privilege,
				"security violation");
	}

	/**
	 * Notify the autograder that a COFF loader is being constructed for the
	 * specified file. The autograder can use this to provide its own COFF
	 * loader, or return <tt>null</tt> to use the default loader.
	 *
	 * @param	file	the executable file being loaded.
	 * @return	a loader to use in loading the file, or <tt>null</tt> to use
	 *		the default.
	 */
	public Coff createLoader(OpenFile file) {
		return null;
	}

	/**
	 * Request permission to send a packet. The autograder can use this to drop
	 * packets very selectively.
	 *
	 * @param	privilege	proves the authenticity of this call.
	 * @return	<tt>true</tt> if the packet should be sent.
	 */
	public boolean canSendPacket(Privilege privilege) {
		Lib.assertTrue(privilege == this.privilege,
				"security violation");
		return true;
	}

	/**
	 * Request permission to receive a packet. The autograder can use this to
	 * drop packets very selectively.
	 *
	 * @param	privilege	proves the authenticity of this call.
	 * @return	<tt>true</tt> if the packet should be delivered to the kernel.
	 */
	public boolean canReceivePacket(Privilege privilege) {
		Lib.assertTrue(privilege == this.privilege,
				"security violation");
		return true;
	}

	private KThread currentThread;
}
