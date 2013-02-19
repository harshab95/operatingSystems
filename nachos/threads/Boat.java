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
	
	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b); 

		//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		//  	begin(1, 2, b);

		//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		//  	begin(3, 3, b);
	}

	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
/*
		Runnable r = new Runnable() {
			public void run() {
				SampleItinerary();
			}
		};
		KThread t = new KThread(r);
		t.setName("Sample Boat Thread");
		t.fork();
*/
		
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
	    //while(numPeopleOnMolokai!=adults+children)
	    //    KThread.currentThread().sleep();
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
		
		/*
	    numAdultOnOahu++; //as soon as it starts running, it "checks in" to Oahu
	    acquire pilotLock
	    while(numChildOnOahu != 1 or boatLocation!="Oahu") sleep; //we only want to leave when there's only 1 child on Oahu.
	    AdultRowToMolokai();
	    adultUpdate()
	    release lock
	    sleep() go to sleep forever

	    define adultUpdate() {
	        numAdultOnOahu--;
	        numPeopleOnMolokai++;
	        boatLocation = "Molokai"
	        }
	    */
		pilotLock.release();
		KThread.currentThread().sleep();
	}

	static void ChildItinerary()
	{
		String currentIsland = new String("Molokai");
		ActualNumChildOnOahu++;
		int numChildOnOahu = ActualNumChildOnOahu;
		int numAdultOnOahu = ActualNumAdultOnOahu;
		
		pilotLock.acquire();
		while(!boatLocation.equals("Oahu")) {
			KThread.currentThread().sleep();
		}
		
		if(!childIsPilot) {
			childIsPilot = true;
			if(currentIsland.equals("Oahu")) {
				bg.ChildRowToMolokai();
				ActualNumChildOnOahu--;
				numPeopleOnMolokai++;
				numChildOnOahu = ActualNumChildOnOahu;
				
				boatLocation = "Molokai";
				pilot.wake();
				pilotLock.release();
			}
		}
		else {
			riderLock.acquire();
			if(currentIsland == "Oahu") {
				bg.ChildRideToMolokai();
				ActualNumChildOnOahu--;
				numPeopleOnMolokai++;
				numChildOnOahu = ActualNumChildOnOahu;
			}
			childIsPilot = false;
			riderLock.release();
			pilotLock.release();
		}
		/*
		private String currentLocation = "Molokai";    
   		RealNumChildOnOahu++; //as soon as it starts running, it "checks in" to Oahu
   		numChildAtOahu = RealNumChildOnOahu;

   		acquire pilotLock
   		while(boatLocation!="Oahu") sleep();
   		gettingToMolokai();
   		EndChecker();

   
   		void gettingToMolokai() {
       		if (childPilot is false) {
           		childIsPilot = true;
           	}
           	if(currentLocation==”Oahu”) {
               	then ChildRowToMolokai();
           		RealNumChildAtOahu--;
           		numChildAtOahu = RealNumChildAtOahu;
           		boatLocation = "Molokai";
           		wake();
           		release lock;
           	}
       		else {
           		acquire riderlock;
           		if(currentIsland == "Oahu")
               		ChildRideToMolokai();    
               		RealNumChildAtOahu--;
               		numChildAtOahu = RealNumChildAtOahu;
           		childIsPilot = false;
       		release pilotlock, riderlock;
   			}
   		}

   		EndChecker() {
       	if(numPeopleAtOahu==0) {
       	 	try to finish();
       	}
       	else
           	go back to Oahu();
           
	pilotlock.release();
	*/
		if(numPeopleOnMolokai==0) {
			//try to finish();
		}
		else {
			bg.ChildRowToOahu();
			ActualNumChildOnOahu++;
			numChildOnOahu = ActualNumChildOnOahu;
			numPeopleOnMolokai--;
		}
		pilotLock.release();
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
