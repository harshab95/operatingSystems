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
	private Lock communicatorLock;
	private Condition okToSpeak, okToListen, okToFinish;

	public final int NO_MESSAGE = -1;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		message = NO_MESSAGE;
		numSpeakers = 0;
		numListeners = 0;
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
		numSpeakers++;

		if (numListeners == 0 || numSpeakers > 1) {
			okToSpeak.sleep();
		}
		okToListen.wake();

		// Extra check: if transmitted message hasn't been received, don't transmit new one.
		while (this.message != NO_MESSAGE) {
			okToSpeak.sleep();
		}

		// When this point is reached, our speaker has found a listener.
		transmitMessage(word);
		numSpeakers--;
		okToFinish.wake();
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
			okToListen.sleep();
		}
		
		okToSpeak.wake();
		okToFinish.sleep();

		// Reach this point ONLY when okToFinish.wake() was called - which occurs 
		// only in speak().  Thus, we KNOW that a message is available now.
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
		Lib.assertTrue(this.message == NO_MESSAGE);
		this.message = message;
	}

	/**
	 * <tt>retrieveMessage()</tt> encapsulates the logic for a listener to read the 
	 * transmitted message, clean up any shared memory (for the next transmission), 
	 * and return the received message.
	 * @return the message transmitted by the speaker.
	 */
	private int retrieveMessage() {
		Lib.assertTrue(this.message != NO_MESSAGE);	// TODO Will NOT work if the message is actually -1.
		int toReturn = this.message;
		this.message = NO_MESSAGE;
		okToSpeak.wake();	// Wake up a speaker who had to sleep because message hadn't been read yet.
		return toReturn;
	}

	/**
	 * This method is to test our:
	 * 1) Communicators Part 4
	 * 2) Condition2.java Part 2
	 * @return true if it passes the test, false if it doesn't (or if it doesn't return at all)
	 */
	static int i;
	public static boolean selfTest() {
		KThread[] speakers, listeners;
		int numTest = 10;
		//Needs to be declared to be final in order for inner run() { } to access comm 
		final Communicator comm = new Communicator();

		System.out.println("Tests for Communicator");
		/*
		 * Initializing variables
		 */
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
		System.out.println("finished initializing");

		/*
		 * Forking to run
		 */
		for (i = 0; i < numTest; i++) {
			speakers[i].fork();
			System.out.println("Speaker "+i+" forked");
		}
		System.out.println("speakers finished forking");
		for (i = 0; i < numTest; i++) {
			listeners[i].fork();
			System.out.println("Listener "+i+" forked");
		}
		System.out.println("Listeners finished forking");


		for (i = 0; i < numTest; i++) {
			speakers[i].join();
			System.out.println("Speaker "+i+" joined");
		}
		System.out.println("speakers finished joining");

		for (i = 0; i < numTest; i++) {
			listeners[i].join();
			System.out.println("Listener "+i+" joined");
		}
		System.out.println("listeners finished joining");

		return true;
	}
}


