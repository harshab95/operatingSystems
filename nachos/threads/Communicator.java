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
	private Condition2 okToSpeak, okToListen;

	public final int NO_MESSAGE = -1;

	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		message = NO_MESSAGE;
		numSpeakers = 0;
		numListeners = 0;
		communicatorLock = new Lock();
		okToSpeak = new Condition2(communicatorLock);
		okToListen = new Condition2(communicatorLock);
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
		if (numListeners == 0 || numSpeakers > 0) {	
			// TODO Is if here sufficient?  If we use while, thread will just go back to sleep.
			boolean intStatus = Machine.interrupt().disable();		
			okToSpeak.sleep();
			Machine.interrupt().restore(intStatus); 				//Enable interrupts
		
		}

		// When this point is reached, our speaker has found a listener.
		transmitMessage(word);
		numSpeakers--;
		okToListen.wake();
		
		boolean intStatus = Machine.interrupt().disable();		
		okToSpeak.sleep();	// Need to sleep so we return AFTER message is received.
		Machine.interrupt().restore(intStatus); 				//Enable interrupts

		// When this point is reached, control is returned to this thread.  Listener should have listened.
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
		if (numSpeakers == 0 || numListeners > 0) {	// TODO Same question as if statement in speak().
			boolean intStatus = Machine.interrupt().disable();		
			okToListen.sleep();
			Machine.interrupt().restore(intStatus); 				//Enable interrupts
		}

		// At this point, our listener has found a speaker.
		okToSpeak.wake();	// Tell speaker to transmit.  We sleep while transmission occurs.
		
		boolean intStatus = Machine.interrupt().disable();		
		okToListen.sleep();
		Machine.interrupt().restore(intStatus); 				//Enable interrupts
		// At this point, the paired speaker has transmitted the message.  Listener can listen.
		int toReturn = retrieveMessage();
		numListeners--;
		okToSpeak.wake();
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
		int toReturn = message;
		message = NO_MESSAGE;
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


