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
		while (numListeners == 0 || numSpeakers > 0) {
			okToSpeak.sleep();
		}
		
		// When this point is reached, our speaker has found a listener.
		transmitMessage(word);
		numSpeakers--;
		okToListen.wake();
		okToSpeak.sleep();	// Need to sleep so we return AFTER message is received.
		
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
		while (numSpeakers == 0 || numListeners > 0) {
			okToListen.sleep();
		}
		
		// At this point, our listener has found a speaker.
		okToSpeak.wake();	// Tell speaker to transmit.  We sleep while transmission occurs.
		okToListen.sleep();
		
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
}


