package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat
{
	static BoatGrader bg;
	public static int ActualNumChildOnOahu, ActualNumAdultOnOahu, numPeopleOnMolokai = 0;
	public static String boatLocation = "Oahu";
	public static Lock riderLock = new Lock();
	public static Lock pilotLock = new Lock();
	public static Condition rider =  new Condition(riderLock);
	public static Condition pilot =  new Condition(pilotLock);
	public static boolean childIsPilot;

	public static Lock finishLock = new Lock();
	public static Condition finishCondition = new Condition(finishLock);

	public static Alarm a = new Alarm();

	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		//System.out.println("\n ***Testing Boats with only 2 children***");
		//begin(0, 2, b); 

		//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		//  	begin(1, 2, b);

		//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		//  	begin(3, 3, b);

		/*
		 * OUR CUSTOM TESTS
		 */
		System.out.println("Tests for Boat");
		begin(1, 2, b); 
		/*
		int numTests = 0;
		for(int numChild=2; numChild<5; numChild++) {
			for(int numAdult=0; numAdult<8; numAdult++) {
				numTests++;
				System.out.println("\n ***Test " + numTests + "/24" + " with " + numAdult + " adults, " + numChild + " children***");
				begin(numAdult, numChild, b);
			}
		}
		*/
	}

	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		finishLock.acquire();
		for(int i=0; i<children; i++) {
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread n = new KThread(r);
			n.setName(""+i);
			n.fork();
		}
		for(int i=children; i<children+adults; i++) {
			Runnable s = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread n = new KThread(s);
			n.setName(""+i);
			n.fork();
		}
		while(numPeopleOnMolokai!=adults+children) {
			finishCondition.wake();
			finishCondition.sleep();
		}
		System.out.println("Simulation has finished");
		KThread.currentThread().finish(); 
	}

	static void AdultItinerary()
	{
		ActualNumAdultOnOahu++;
		int numChildOnOahu = ActualNumChildOnOahu;
		int numAdultOnOahu = ActualNumAdultOnOahu;

		pilotLock.acquire();
		pilot.wake();
		pilot.sleep();
		while(numChildOnOahu != 1 || !boatLocation.equals("Oahu")) {
			numChildOnOahu = ActualNumChildOnOahu;
			pilot.wake();
			pilot.sleep();
		}
		bg.AdultRowToMolokai();
		ActualNumAdultOnOahu--;
		numPeopleOnMolokai++;
		boatLocation = "Molokai";
		pilot.wake();
		pilotLock.release();
		boolean intStatus = Machine.interrupt().disable();	
		KThread.currentThread().sleep();
		Machine.interrupt().restore(intStatus); 
		System.out.println("Thread Finished\n");
	}

	static void ChildItinerary()
	{
		//Variable Declarations
		String currentIsland = new String("Molokai");
		ActualNumChildOnOahu++; //"checks in" to island
		System.out.println(ActualNumChildOnOahu);
		//Sees the number of people on the island
		int numChildOnOahu = ActualNumChildOnOahu;
		int numAdultOnOahu = ActualNumAdultOnOahu;
		pilotLock.acquire();
		while(!currentIsland.equals("Oahu")) {
			pilot.wake();
			pilot.sleep();

			//waits for boat to get to Oahu
			while(!boatLocation.equals("Oahu")) {
				pilot.sleep();
			}
			//Case: Thread is pilot
			if(!childIsPilot) {
				childIsPilot = true;
				//Updates the numbers. Checks into island, sees number of people on Island
				bg.ChildRowToMolokai();
				pilot.wake(); 
				pilot.sleep();
				bg.ChildRowToOahu();
				boatLocation = "Oahu";
				currentIsland = "Oahu";
			}
			//Case: Thread is rider
			else {
				riderLock.acquire();
				bg.ChildRideToMolokai();
				ActualNumChildOnOahu--;
				numPeopleOnMolokai++;
				numChildOnOahu = ActualNumChildOnOahu;
				numAdultOnOahu = ActualNumAdultOnOahu;
				boatLocation = "Molokai";
				currentIsland = "Molokai";
				boolean intStatus = Machine.interrupt().disable();	
				childIsPilot = false;
				riderLock.release();
				pilot.wake();
				pilotLock.release();
				Machine.interrupt().restore(intStatus); 
				KThread.currentThread().finish();
				System.out.println("Thread Finished\n");
			}
		}
		//at this point, either child is done and sleeping on Molokai or waiting on Oahu
		//End Sequence
		pilot.wake();
		pilot.sleep();
		while(true) {
			if(numChildOnOahu + numAdultOnOahu==1) {
				//tryToFinish();
				//go into endsequence
				bg.ChildRowToMolokai();
				ActualNumChildOnOahu--;
				numPeopleOnMolokai++;
				numChildOnOahu = ActualNumChildOnOahu;
				numAdultOnOahu = ActualNumAdultOnOahu;
				boatLocation = "Molokai";
				currentIsland = "Molokai";
				finishLock.acquire();
				finishCondition.wake();
				finishCondition.sleep();
				pilot.wake();
				pilot.sleep();
				finishLock.release();
			}
			else {
				bg.ChildRowToOahu();
				ActualNumChildOnOahu++;
				numPeopleOnMolokai--;
				numChildOnOahu = ActualNumChildOnOahu;
				numAdultOnOahu = ActualNumAdultOnOahu;
				boatLocation = "Oahu";
				currentIsland = "Oahu";
				pilotLock.release();
				//pilot.sleep();
				a.waitUntil(30);

				//using Alarm, wait for x
			}
		}
	}

	static void SampleItinerary()
	{
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}


}
