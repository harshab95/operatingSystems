package nachos.threads;

import java.util.Comparator;
import java.util.PriorityQueue;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.ThreadState;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p><b>Note</b>: Nachos will not function correctly with more than one
	 * alarm.
	 */
	public Alarm() {
		//TODO check if intiailization goes here before the Machine call
		threadsWaiting = new PriorityQueue<ThreadAlarmTime>(threadsWaitingInitialCapacity);
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() { timerInterrupt(); }
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread
	 * that should be run.
	 */
	public void timerInterrupt() {
		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks,
	 * waking it up in the timer interrupt handler. The thread must be
	 * woken up (placed in the scheduler ready set) during the first timer
	 * interrupt where
	 *
	 * <p><blockquote>
	 * (current time) >= (WaitUntil called time)+(x)
	 * </blockquote>
	 *
	 * @param	x	the minimum number of clock ticks to wait.
	 *
	 * @see	nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		
		boolean intStatus = Machine.interrupt().disable();
		threadsWaiting.add(e)
	}
	
	/* Implemented a LowPriorityComparator to pop lowest priority threads first 
	 *  
	 */
	private class LowPriorityComparator implements Comparator<ThreadAlarmTime> {
		public int compare(ThreadAlarmTime threadAlarm1, ThreadAlarmTime threadAlarm2) {
			assert threadAlarm1.wakeTimeSet == threadAlarm2.wakeTimeSet == true;
			if (threadAlarm1.wakeTime == threadAlarm2.wakeTime) {
				return 0;
			} 
			else if (threadAlarm1.wakeTime < threadAlarm2.wakeTime) {
				return 1;
			}
			else {
				return -1;
			}
		}

		public boolean equals(Object o) {
			return o.equals(this);
		}
	}
	
	protected class ThreadAlarmTime {
		KThread threadPointer = null;
		boolean wakeTimeSet = false;
		long wakeTime;
		ThreadAlarmTime(KThread t, long wakeUpTime) {
			this.threadPointer = t;
			this.wakeTime = wakeUpTime;
			this.wakeTimeSet = true;
		}
	}

	PriorityQueue<ThreadAlarmTime> threadsWaiting = null;
	int threadsWaitingInitialCapacity = 10;
}

