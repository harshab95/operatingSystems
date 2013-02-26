package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {

	private int message;
	private int numSpeakers, numListeners;
	private boolean messageFieldInUse;
	private Lock communicatorLock;
	private Condition okToSpeak, okToListen, okToFinish;	// TODO Switch back to Condition2.

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		numSpeakers = 0;
		numListeners = 0;
		messageFieldInUse = false;
		communicatorLock = new Lock();
		okToSpeak = new Condition(communicatorLock);
		okToListen = new Condition(communicatorLock);
		okToFinish = new Condition(communicatorLock);
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param	word	the integer to transfer.
	 */
	public void speak(int word) {
		communicatorLock.acquire();
		remainingSpeakers++;	// TODO For testing purposes only.
		numSpeakers++;

		if (numListeners == 0 || numSpeakers > 1) {
			System.out.println("Speaker about to sleep.");	// TODO
			okToSpeak.sleep();
		}

		// At this point, a speaker-listener pair exists.  Wake up the listener and transmit the message.
		System.out.println("Speaker woke up, signalling listener.");//TODO
		okToListen.wake();

		// Extra check: if transmitted message hasn't been received, don't transmit new one.
		while (messageFieldInUse) {
			System.out.println("Speaker waiting to transmit message."); // TODO
			okToSpeak.sleep();
		}

		// When this point is reached, our speaker has found a listener.
		transmitMessage(word);
		System.out.println("Speaker transmitted message"); // TODO
		numSpeakers--;
		okToFinish.wake();	// Ready to read the transmitted message.
		remainingSpeakers--;	// TODO Testing purposes only.
		communicatorLock.release();
		return;
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */   
	public int listen() {
		communicatorLock.acquire();
		remainingListeners++;	// TODO Testing purposes only.
		numListeners++;

		if (numSpeakers == 0 || numListeners > 1) {
			System.out.println("Listener preparing to sleep");	// TODO
			okToListen.sleep();
		}
		// At this point, a listener-speaker pairing exists.  Wake up the speaker and wait for message to be written.
		System.out.println("Listener calling okToSpeak.wake()");
		okToSpeak.wake();

		while (!messageFieldInUse) {
			System.out.println("Listener waiting to retrieve a message, but no message, okToFinish.sleep()"); 	// TODO
			okToFinish.sleep();
		}

		// Reach this point ONLY when okToFinish.wake() was called - which occurs 
		// only in speak().  Thus, we KNOW that a message is available now.
		System.out.println("Finish woke up and is about to retrieve message");	//TODO
		int toReturn = retrieveMessage();
		numListeners--;
		remainingListeners--;	// TODO Testing purposes only.
		communicatorLock.release();
		return toReturn;
	}

	/**
	 * <tt>transmitMessage()</tt> encapsulates the logic for a speaker to transmit a 
	 * message out.
	 * @param message is the message being passed
	 */
	private void transmitMessage(int message) {
		Lib.assertTrue(!messageFieldInUse);
		messageFieldInUse = true;
		this.message = message;
		System.out.println("Transmitted message.");	// TODO
	}

	/**
	 * <tt>retrieveMessage()</tt> encapsulates the logic for a listener to read the 
	 * transmitted message, clean up any shared memory (for the next transmission), 
	 * and return the received message.
	 * @return the message transmitted by the speaker.
	 */
	private int retrieveMessage() {
		Lib.assertTrue(messageFieldInUse);
		int toReturn = this.message;
		messageFieldInUse = false;
		okToSpeak.wake();	// Wake up a speaker who had to sleep because message hadn't been read yet.

		System.out.println("***Retrieving message.");	//TODO
		return toReturn;
	}

	/// Testing methods and fields below this point.
	static int i;
	public static int remainingSpeakers = 0, remainingListeners = 0;

	/**
	 * selfTest() (2-parameters) tests Communicator with any number of speakers and listeners 
	 * supplied by the user.  It prints debugging information to the terminal and notifies user 
	 * if test was passed.
	 * @param numTestSpeakers is the number of speakers you wish to create
	 * @param numTestListeners is the number of listeners you wish to create
	 * @return true if the test passes, false otherwise
	 */
	public static boolean selfTest(int numTestSpeakers, int numTestListeners) {
		System.out.println("\n== Testing Communicator.java with " + numTestSpeakers + 
				" speakers and " + numTestListeners + " listeners. ==");

		remainingSpeakers = 0;
		remainingListeners = 0;
		KThread[] speakers = new KThread[numTestSpeakers];
		KThread[] listeners = new KThread[numTestListeners];
		boolean testResult = false;
		final Communicator comm = new Communicator();	// Needs to be declared final for inner run() { } to access comm.
		String testMessageHeader = "[Communicator: (S: " + numTestSpeakers + ", L: " + numTestListeners + ")] ";

		StringBuilder sb = new StringBuilder("");	// Used for message formatting.
		for (int i = 0; i < testMessageHeader.length(); i++) {
			sb.append(" ");
		}
		String testMessageSpace = sb.toString();

		// Initialize variables.
		speakers = new KThread[numTestSpeakers];
		listeners = new KThread[numTestListeners];
		for (i = 0; i < numTestSpeakers; i++) {
			speakers[i] = new KThread(new Runnable() {
				public void run() {
					comm.speak(i);
				}
			});
		}
		for (i = 0; i < numTestListeners; i++) {
			listeners[i] = new KThread(new Runnable() {
				public void run() {
					comm.listen();
				}
			});
		}
		System.out.println(testMessageHeader + "Finished initializing.");

		// Forking to run
		for (i = 0; i < numTestSpeakers; i++) {
			speakers[i].fork();
			System.out.println(testMessageHeader + "Speaker "+i+" forked.");
		}
		System.out.println(testMessageHeader + "Speakers finished forking.");
		for (i = 0; i < numTestListeners; i++) {
			listeners[i].fork();
			System.out.println(testMessageHeader + "Listener "+i+" forked.");
		}
		System.out.println(testMessageHeader + "Listeners finished forking.");

		// Joining.
		// TODO Seems like if we don't join() the threads, speak()/listen() will not run.  But if you do, then we wait indefinitely when calling 2nd test (because thread sleeps, and no speaker wakes it up).
		for (i = 0; i < numTestSpeakers; i++) {
			speakers[i].join();
			System.out.println(testMessageHeader + "Speaker "+i+" joined.");
		}
		System.out.println(testMessageHeader + "Speakers finished joining.");

		for (i = 0; i < numTestListeners; i++) {
			listeners[i].join();
			System.out.println(testMessageHeader + "Listener "+i+" joined.");
		}
		System.out.println(testMessageHeader + "Listeners finished joining.");

		// Print error messages as necessary, then return.
		int numExpectedPairs = Math.min(numTestSpeakers, numTestListeners);
		int numExpectedRemainders = Math.max(numTestSpeakers, numTestListeners) - numExpectedPairs;
		if (numTestSpeakers < numTestListeners) {
			System.out.println(testMessageHeader + "Expected remaining speakers: 0,  remaining listeners: " + numExpectedRemainders + "\n" +
					testMessageSpace + "Actual   remaining speakers: " + remainingSpeakers + ",  remaining listeners: " + remainingListeners);
			testResult = (remainingSpeakers == 0) && (remainingListeners == numExpectedRemainders);
		} else {
			System.out.println(testMessageHeader + "Expected remaining speakers: " + numExpectedRemainders + ",  remaining listeners: 0\n" +
					testMessageSpace + "Actual   remaining speakers: " + remainingSpeakers + ",  remaining listeners: " + remainingListeners);
			testResult = (remainingSpeakers == numExpectedRemainders) && (remainingListeners == 0);
		}

		if (testResult) {
			System.out.println("\n> " + testMessageHeader + "Test passed.");
		} else {
			System.out.println("\n> " + testMessageHeader + "***ERROR: Test failed!");
		}
		return testResult;
	}

	/**
	 * selfTest() (no parameters) runs each individual test for Communicator.java.
	 * @return true if it passes all tests, false if at least one test fails OR throws an exception.
	 */
	public static boolean selfTest() {
		boolean allTestsPassed = true;
		System.out.println("== Testing Communicator.java. ==");
		try {
			allTestsPassed = selfTest(10, 10) && allTestsPassed;
			allTestsPassed = selfTest(1, 2) && allTestsPassed;
		} catch (Exception e) {
			System.out.println("***ERROR: Exception thrown; test failed.  selfTest() will NOT continue executing tests.");
			allTestsPassed = false;
		}
		return allTestsPassed;
	}
}


