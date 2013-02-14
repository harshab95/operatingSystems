package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum &&
				priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;    

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/* Implemented a PriorityComparator for our PriorityThreadQueue, a
	 *  subclass of ThreadQueue. 
	 *  */
	private class PriorityComparator implements Comparator<KThread> {
		public int compare(KThread kt1, KThread kt2) {
			int kt1p= kt1.schedulingState.getEffectivePriority();
			int kt2p = kt2.schedulingState.getEffectivePriority();
			if (kt1p == kt2p) {
				return 0;
			} 
			else if (kt1p < kt2p) {
				return -1;
			}
			else {
				return 1;
			}
		}

		public boolean equals(Object o) {
			return o.equals(this);
		}
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		private KThread currentThread = null;
		private PriorityQueue<KThread> localThreads = null;
		private KThread randomChosenNextThread = null;
		private Hashtable queueEntryTimes = null;

		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			localThreads = new PriorityQueue<KThreads>;
			localThreads.comparator = new PriorityComparator();
			currentThread = null;
			queueEntryTimes = new HashTable();
		}

		/*
		 * Getter for the currentThread. used to construct the parent / child tree
		 * of a ThreadState which as a system, helps set and update the effective 
		 * priorities of Queues
		 */
		public KThread getCurrentThread()  {
			return currentThread;
		}
		
		/**
		 * Puts a thread on the waitQueue so it can wait for access to be the current running / active 
		 * thread. Note that this may be for either a lock, or a ready (scheduling) Queue.
		 * Once a thread is waiting, the currentThread must have its priority updated, in the case
		 * that @param thread 's priority is higher than its current effective priority.
		 *  
		 * @param thread is the thread that is waiting for access.
		 */
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());

			//To refresh the priority queue, must remove and readd
			if (currentThread != null) {
				localThreads.remove(currentThread);
				curThreadState = getThreadState(currentThread); 
				curThreadState.parents.add(thread);
				curThreadState.calculateEffectivenessPriority();
				localThreads.add(currentThread);
			}	
			queueEntryTimes.put(thread, Machine.timer.getTime() );
			localThreads.add(thread);
			// So the threadState can access it's priorities.
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			//TODO Make sure no thread currently running ?
			assert localThreads.size() == 0;
			assert currentThread == null;
			localThreads.add(thread);
			currentThread = thread;
			
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me

			if (localThreads.peek() == null) {
				return null;
			}
			
			KThread t;
			if (randomChosenNextThread != null) {
				t = randomChosenThread;
			} else {
				t = pickNextThread();
			}
			// Remove the next thread from the parents list of child (currentThread)
			currentThread.scheduledState.parents.remove(t);
			getThreadState(currentThread).calculateEffectivePriority();
			
			currentThread = t;
			randomChosenThread = null;
			return t;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			
			//TODO disableIterrupts ?
			
			// Catch1: No more threads in queue, return
			if (localThreads.peek() == null) {
				return null;
			}
			// Catch2: Already set a thread to return
			if (randomChosenThread != null) {
				return randomChosenThread;
			}
			
			// Must have at least 1 KThread in queue and have not picked the next thread
			int highestPriority = getThreadEffectivePriority(localThreads.peek());
			ArrayCollection<KThread> highestPriorityThreads = new ArrayCollection<KThread>();
			while (localThreads.peek() != null) {
				t = localThreads.poll();
				if (getThreadEffectivePriority(t) == highestPriority) {
					highestPriorityThreads.add(t);
				}
			}
			if (highestPriorityThreads.size() > 1) {
				randomChosenNextThread = chooseThreadLongestWaitTime(queueEntryTimes);
			} else {
				// Only 1 KThread with highest priority
				randomChosenNextThread = highestPriorityThreads.remove(0);
			}	
			return randomChosenNextThread;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		protected int effectivePriority = -1;
		protected ArrayCollection<ThreadState> parents = null;
		protected KThread child = null;
		protected PriorityQueue targetQueue = null;
		
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			//Should be one line as it just needs to returned the cached value
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			if (child != null) {
				getThreadState(child).calculateEffectivePriority();
			}
			// implement me
		}

		/** 
		 * JE  to recalculate the effective priority of a thread
		 */
		public void calculateEffectivePriority() {
			highestPriority = -infinity;
			for thread in parents {
				if (getThreadState(thread).getEffectivePriority()) > highestPriority) {
					highestPriority = getThreadState(thread).getEffectivePriority()
				}
			}
			this.effectivePriority= highestPriority;
			
			if (child != null) {
				( (ThreadState) child.scheduledState).calculateEffectivePriority();
			}
			return;
		}
		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			targetQueue = waitQueue;
			child = waitQueue.getCurrentThread();
			
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			targetQueue = waitQueue;
			if (child != null) {
				ThreadState childThreadState = (getThreadState(thread).child);
				childThreadState.parent.remove(thread);
				child.calculateEffectivePriority();
			}	
		}	

		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
	}
}
