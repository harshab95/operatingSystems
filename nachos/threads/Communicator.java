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
	private Condition okToSpeak, okToListen;

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
		System.out.println("\nspeak(): Entered method.");
		communicatorLock.acquire();
		numSpeakers++;
		System.out.println("speak(): Acquired lock and incremented numSpeakers.\n" +
				"\tnumSpeakers: " + numSpeakers + "   numListeners: " + numListeners);
		if (numListeners == 0 || numSpeakers > 1) {	
			// TODO Is if here sufficient?  If we use while, thread will just go back to sleep.
			// boolean intStatus = Machine.interrupt().disable();		
			System.out.println("speak(): Entered if statement, preparing to okToSpeak.sleep()");
			okToSpeak.sleep();
			// Machine.interrupt().restore(intStatus); 				//Enable interrupts
		
		}

		// When this point is reached, our speaker has found a listener.
		System.out.println("speak(): Preparing to transmitMessage().\n" +
				"\tmessage: " + word + "  this.message: " + this.message);
		transmitMessage(word);
		numSpeakers--;
		System.out.println("speak(): Decremented numSpeakers, preparing to signal okToListen.\n" +
				"\tnumSpeakers: " + numSpeakers + "   numListeners: " + numListeners);
		okToListen.wake();
		
		// boolean intStatus = Machine.interrupt().disable();		
		System.out.println("speak(): Preparing to okToSpeak.sleep()");
		okToSpeak.sleep();	// Need to sleep so we return AFTER message is received.
		// Machine.interrupt().restore(intStatus); 				//Enable interrupts

		// When this point is reached, control is returned to this thread.  Listener should have listened.
		communicatorLock.release();
		System.out.println("speak(): Released lock.");
		return;
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */    
	public int listen() {
		System.out.println("\nlisten(): Entered method.");
		communicatorLock.acquire();
		numListeners++;
		System.out.println("listen(): Acquried communicatorLock and incremented numListeners.\n" +
				"\tnumSpeakers: " + numSpeakers + "   numListeners: " + numListeners);
		if (numSpeakers == 0 || numListeners > 1) {	// TODO Same question as if statement in speak().
			// boolean intStatus = Machine.interrupt().disable();
			System.out.println("listen(): Entered if statement and disabled interrupts.  Preparing to sleep().");
			okToListen.sleep();
			// Machine.interrupt().restore(intStatus); 				//Enable interrupts
		}

		// At this point, our listener has found a speaker.
		System.out.println("listen(): Preparing to signal okToSpeak.");
		okToSpeak.wake();	// Tell speaker to transmit.  We sleep while transmission occurs.
		
		// boolean intStatus = Machine.interrupt().disable();		
		System.out.println("listen(): Preparing to okToListen.sleep()");
		okToListen.sleep();
		// Machine.interrupt().restore(intStatus); 				//Enable interrupts
		// At this point, the paired speaker has transmitted the message.  Listener can listen.
		System.out.println("listen(): Preparing to retrieveMessage().\n" +
				"\tthis.message: " + this.message);
		int toReturn = retrieveMessage();
		System.out.println("listen(): Retrieved message; decrementing numListeners and okToSpeak.wake().");
		numListeners--;
		okToSpeak.wake();
		communicatorLock.release();
		System.out.println("listen(): okToSpeak.wake() and lock released.\n" +
				"\tnumSpeakers: " + numSpeakers + "   numListeners: " + numListeners);
		return toReturn;
	}

	/**
	 * <tt>transmitMessage()</tt> encapsulates the logic for a speaker to transmit a 
	 * message out.
	 * @param message is the message being passed
	 */
	private void transmitMessage(int message) {
		System.out.println("transmitMessage(): Entered method with message: " + message);	
		Lib.assertTrue(this.message == NO_MESSAGE);
		System.out.println("transmitMessage(): Passed assertion test.");
		this.message = message;
		System.out.println("transmitMessage(): Wrote message " + message + " to message field.  New value: " + this.message);
	}

	/**
	 * <tt>retrieveMessage()</tt> encapsulates the logic for a listener to read the 
	 * transmitted message, clean up any shared memory (for the next transmission), 
	 * and return the received message.
	 * @return the message transmitted by the speaker.
	 */
	private int retrieveMessage() {
		System.out.println("retrieveMessage(): Entered method.");
		Lib.assertTrue(this.message != NO_MESSAGE);	// TODO Will NOT work if the message is actually -1.
		System.out.println("retrieveMessage(): Passed assertion test.");
		int toReturn = this.message;
		this.message = NO_MESSAGE;
		System.out.println("retrieveMessage(): Retrieved message and reset message field.\n\ttoReturn: " + toReturn + "   this.message: " + this.message);
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


