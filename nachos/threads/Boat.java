package nachos.threads;
import nachos.ag.BoatGrader;

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

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b); 

		/*
		 * OUR CUSTOM TESTS
		 */
		int numTests = 0;
		for(int numChild=2; numChild<5; numChild++) {
			for(int numAdult=0; numAdult<8; numAdult++) {
				numTests++;
				System.out.println("\n ***Test " + numTests + "/24" + "with " + numAdult + " adults, " + numChild + " children***");
				begin(numAdult, numChild, b);
			}
		}
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
	        finishCondition.sleep();
	    	finishCondition.wake();
	    }
	    KThread.currentThread().finish(); 

	}

	static void AdultItinerary()
	{
		ActualNumAdultOnOahu++;
		int numChildOnOahu = ActualNumChildOnOahu;
		int numAdultOnOahu = ActualNumAdultOnOahu;
		
		pilotLock.acquire();
		while(numChildOnOahu != 1 || !boatLocation.equals("Oahu")) {
			numChildOnOahu = ActualNumChildOnOahu;
			pilot.sleep();
		}
		bg.AdultRowToMolokai();
		ActualNumAdultOnOahu--;
		numPeopleOnMolokai++;
		boatLocation = "Molokai";
		
		pilotLock.release();
		KThread.currentThread().sleep();
	}

	static void ChildItinerary()
	{
		//Variable Declarations
		String currentIsland = new String("Molokai");
		ActualNumChildOnOahu++; //"checks in" to island
		//Sees the number of people on the island
		int numChildOnOahu = ActualNumChildOnOahu;
		int numAdultOnOahu = ActualNumAdultOnOahu;
		
		pilotLock.acquire();
		//waits for boat to get to Oahu
		while(!boatLocation.equals("Oahu")) {
			pilot.sleep();
		}
		//Case: Thread is pilot
		if(!childIsPilot) {
			childIsPilot = true;
			//Updates the numbers. Checks into island, sees number of people on Island
			ActualNumChildOnOahu--;
			numPeopleOnMolokai++;
			numChildOnOahu = ActualNumChildOnOahu;
			numAdultOnOahu = ActualNumAdultOnOahu;
			bg.ChildRowToMolokai();
			boatLocation = "Molokai";
			pilot.wake(); 
			pilot.sleep();
		}
		//Case: Thread is rider
		else {
			riderLock.acquire();
			ActualNumChildOnOahu--;
			numPeopleOnMolokai++;
			numChildOnOahu = ActualNumChildOnOahu;
			numAdultOnOahu = ActualNumAdultOnOahu;
			bg.ChildRideToMolokai();
			boatLocation = "Molokai";
			childIsPilot = false;
			riderLock.release();
			pilot.wake();
			pilot.sleep();
		}
		//End Sequence
		while(true) {
			if(numChildOnOahu + numAdultOnOahu==0) {
				//tryToFinish();
				//go into endsequence
				finishLock.acquire();
				finishCondition.wake();
				finishCondition.sleep();
				pilotLock.release();
				pilot.sleep();
			}
			else {
				bg.ChildRowToOahu();
				ActualNumChildOnOahu++;
				numChildOnOahu = ActualNumChildOnOahu;
				numAdultOnOahu = ActualNumAdultOnOahu;
				numPeopleOnMolokai--;
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
