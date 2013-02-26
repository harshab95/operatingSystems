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
	private Condition2 okToSpeak, okToListen, okToFinish;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		numSpeakers = 0;
		numListeners = 0;
		messageFieldInUse = false;
		communicatorLock = new Lock();
		okToSpeak = new Condition2(communicatorLock);
		okToListen = new Condition2(communicatorLock);
		okToFinish = new Condition2(communicatorLock);
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
		numListeners++;

		if (numSpeakers == 0 || numListeners > 1) {
			System.out.println("Listener preparing to sleep");	// TODO
			okToListen.sleep();
		}
		// At this point, a listener-speaker pairing exists.  Wake up the speaker and wait for message to be written.
		okToSpeak.wake();
		
		while (!messageFieldInUse) {
			okToFinish.sleep();
		}

		// Reach this point ONLY when okToFinish.wake() was called - which occurs 
		// only in speak().  Thus, we KNOW that a message is available now.
		System.out.println("Finish woke up and is about to retrieve message");	//TODO
		int toReturn = retrieveMessage();
		numListeners--;

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
	public static final int TOTAL_TESTS = 2;
	static int i;

	private static final int statusNew = 0;
	private static final int statusReady = 1;
	private static final int statusRunning = 2;
	private static final int statusBlocked = 3;
	private static final int statusFinished = 4;
	/**
	 * This method is used to run a specific test.  Tests here will test:
	 * <ol>
	 * <li>Communicators (Part 4)</li>
	 * <li>Condition2.java (Part 2)</li>
	 * <li>Alarm.java (Part 3)</li>
	 * </ol>
	 * <p>Test Details:</p>
	 * <ol>
	 * <li><b>Test 0: </b>Sanity check test.  Fork 10 speakers, then 10 listeners.</li>
	 * <li><b>Test 1: </b>Fork 2 listeners, then 1 speaker.  Check that 1 thread listener is still blocked.</li>
	 * </ol>
	 * 
	 * @param testNumber the test number to run
	 * @return true if it passes the test, false if it doesn't (or if it doesn't return at all)
	 */
	public static boolean selfTest(int testNumber) {
		System.out.println("\n== Testing Communicator.java with Test " + testNumber + " ==");
		KThread[] speakers, listeners;
		int numTest;
		final Communicator comm = new Communicator();	// Needs to be declared final for inner run() { } to access comm.
		String testMessageHeader = "[Communicator: Test " + testNumber + "] "; 

		switch (testNumber) {
		case 0:
			numTest = 10; 

			// Initialize variables.
			speakers = new KThread[numTest];
			listeners = new KThread[numTest];
			for (i = 0; i < numTest; i++) {
				speakers[i] = new KThread(new Runnable() {
					public void run() {
						comm.speak(i);
					}
				});
			}
			for (i = 0; i < numTest; i++) {
				listeners[i] = new KThread(new Runnable() {
					public void run() {
						comm.listen();
					}
				});
			}
			System.out.println(testMessageHeader + "Finished initializing.");

			// Forking to run
			for (i = 0; i < numTest; i++) {
				speakers[i].fork();
				System.out.println(testMessageHeader + "Speaker "+i+" forked.");
			}
			System.out.println(testMessageHeader + "Speakers finished forking.");
			for (i = 0; i < numTest; i++) {
				listeners[i].fork();
				System.out.println(testMessageHeader + "Listener "+i+" forked.");
			}
			System.out.println(testMessageHeader + "Listeners finished forking.");


			for (i = 0; i < numTest; i++) {
				speakers[i].join();
				System.out.println(testMessageHeader + "Speaker "+i+" joined.");
			}
			System.out.println(testMessageHeader + "Speakers finished joining.");

			for (i = 0; i < numTest; i++) {
				listeners[i].join();
				System.out.println(testMessageHeader + "Listener "+i+" joined.");
			}
			System.out.println(testMessageHeader + "Listeners finished joining.");

			return true;
		case 1:
			// Initialize variables.
			int[] threadStatus = new int[3];
			speakers = new KThread[1];
			listeners = new KThread[2];

			// threadA.listen()
			listeners[0] = new KThread(new Runnable() {
				public void run() {
					comm.listen();
				}
			});

			listeners[0].fork();
			System.out.println(testMessageHeader + "listenerA forked.");
			/*
			threadStatus[0] = listeners[0].getThreadStatus(); 
			if (threadStatus[0] != statusBlocked) {
				System.out.println(testMessageHeader + "listenerA invoked listen() with no speakers available, should be blocked.\n" +
						"\tExpected status: " + statusBlocked + "  Actual status: " + threadStatus + "\n" +
						"\tnumSpeakers: " + comm.numSpeakers + "  numListeners: " + comm.numListeners);
				return false;
			}*/

			// threadB.listen()
			listeners[1] = new KThread(new Runnable() {
				public void run() {
					comm.listen();
				}
			});

			listeners[1].fork();
			System.out.println(testMessageHeader + "listenerB forked.");
			/*
			threadStatus[0] = listeners[0].getThreadStatus();
			threadStatus[1] = listeners[1].getThreadStatus();
			if (threadStatus[0] != statusBlocked || threadStatus[1] != statusBlocked) {
				System.out.println(testMessageHeader + "listenerB invoked listen() with no speakers available, both listeners should be blocked.\n" +
						"\tA: Expected status: " + statusBlocked + "  Actual status: " + threadStatus[0] + "\n" +
						"\tB: Expected status: " + statusBlocked + "  Actual status: " + threadStatus[1] + "\n" +
						"\tnumSpeakers: " + comm.numSpeakers + "  numListeners: " + comm.numListeners);
				return false;
			}*/

			// threadC.speak()
			speakers[0] = new KThread(new Runnable() {
				public void run() {
					comm.speak(i++);
				}
			});

			speakers[0].fork();
			System.out.println(testMessageHeader + "speaker forked.");

			threadStatus[0] = listeners[0].getThreadStatus();
			threadStatus[1] = listeners[1].getThreadStatus();
			threadStatus[2] = speakers[0].getThreadStatus();
			/*
			Alarm a = new Alarm();
			a.waitUntil(1000);

			if (threadStatus[2] == statusBlocked) {
				System.out.println(testMessageHeader + "speaker invoked speak() with two waiting listeners, should NOT be blocked.\n" +
						"\tExpected status: NOT " + statusBlocked + "  Actual status: " + threadStatus + "\n" +
						"\tnumSpeakers: " + comm.numSpeakers + "  numListeners: " + comm.numListeners);
				return false;
			}

			if (threadStatus[0] != statusBlocked || threadStatus[1] != statusBlocked) {
				System.out.println(testMessageHeader + "speaker invoked speak() with two waiting listeners, only ONE listener should be blocked.\n" +
						"\tstatusBlocked: " + statusBlocked + "\n" + 
						"\tlistenerA: " + threadStatus[0] + "\n" +
						"\tlistenerB: " + threadStatus[1] + "\n" +
						"\tnumSpeakers: " + comm.numSpeakers + "  numListeners: " + comm.numListeners);
				return false;
			}
			 */
			// Joining threads.
			for (i = 0; i < 2; i++) {
				// for (i = 1; i >= 0; i--) {
				listeners[i].join();
				System.out.println(testMessageHeader + "Listener "+i+" joined.");
			}

			speakers[0].join();
			System.out.println(testMessageHeader + "Speaker joined.");
			System.out.println(testMessageHeader + "Speakers finished joining.");

			// listeners[1].join();
			/*
			System.out.println("okToSpeak is empty: " + comm.okToSpeak.hasNoWaitingThreads());
			System.out.println("okToListen is empty: " + comm.okToListen.hasNoWaitingThreads());
			System.out.println("okToFinish is empty: " + comm.okToFinish.hasNoWaitingThreads());*/
			return true;

		default:
			System.out.println(testMessageHeader + "***ERROR: No such test exists.  Requested test: " + testNumber);
			return false;
		}
	}

	/**
	 * selfTest() (no parameters) runs each individual test for Communicator.java.
	 * @return true if it passes all tests, false if at least one test fails OR throws an exception.
	 */
	public static boolean selfTest() {
		boolean allTestsPassed = TOTAL_TESTS == 0 ? true : false;
		System.out.println("== Testing Communicator.java.  Total Tests to run: " + TOTAL_TESTS + " ==");

		for (int testNum = 0; testNum < TOTAL_TESTS; testNum++) {
			String testResultHeader = "\n> [Communicator: Test " + testNum + "] ";
			try {

				if (selfTest(testNum)) {
					System.out.println(testResultHeader  + "Test passed.");
				} else {
					System.out.println(testResultHeader + "***ERROR: Test failed.");
					allTestsPassed = false;
				}
			} catch (Exception e) {
				System.out.println(testResultHeader + "***ERROR: Exception thrown; test failed.");
				allTestsPassed = false;
			}
		}
		return allTestsPassed;
	}
}


