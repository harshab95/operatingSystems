package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param	conditionLock	the lock associated with this condition
	 *				variable. The current thread must hold this
	 *				lock whenever it uses <tt>sleep()</tt>,
	 *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;

		/* 
		 * TODO for now just assuming that we will be using our own PriorityQueue
		 * 1) Why errors?
		 * 2) What should transfer priority be?
		 * 3) This correct type of Queue to use?
		 */
		boolean transferPriority = true;

		// tempScheduler only for getting access to a PriorityQueue
		RoundRobinScheduler tempScheduler = new RoundRobinScheduler();
		this.waitQueue = (RoundRobinScheduler.FifoQueue) tempScheduler.newThreadQueue(transferPriority);
	}

	public static void selfTest() {
		System.out.println("Tests for Condition Variables");
	}
	
	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The
	 * current thread must hold the associated lock. The thread will
	 * automatically reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		//TODO check if it's the correct way to get the current thread.
		boolean intStatus = Machine.interrupt().disable();		
		waitQueue.waitForAccess(KThread.currentThread());
		conditionLock.release();	
		KThread.sleep();
		//TODO check if this is correct... to acquire.
		conditionLock.acquire();
		Machine.interrupt().restore(intStatus); 				//Enable interrupts
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		boolean interrupts = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		//TODO can i rely on the isEmpty() method
		if (!waitQueue.isEmpty()) {
			KThread t = waitQueue.nextThread();
			t.ready();
		}
		Machine.interrupt().restore(interrupts);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		while (!waitQueue.isEmpty()) {
			wake();
		}
	}

	private Lock conditionLock;

	private RoundRobinScheduler.FifoQueue waitQueue;
}
