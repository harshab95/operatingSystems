package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat
{
	static BoatGrader bg;
	public static int ActualNumChildOnOahu, ActualNumChildOnMolokai, ActualNumAdultOnOahu, ActualNumAdultOnMolokai = 0;
	public static String boatLocation = "Oahu";
	public static Lock riderLock = new Lock();
	public static Lock pilotLock = new Lock();
	public static Condition rider =  new Condition(riderLock);
	public static Condition pilot =  new Condition(pilotLock);
	public static boolean childIsPilot = false;
	public static boolean boatHasChildAsRider = false;

	public static Lock finishLock = new Lock();
	public static Condition finishCondition = new Condition(finishLock);

	public static boolean finished = false;

	public static Alarm alarm = new Alarm();

	public static Lock runLock = new Lock();
	public static Condition runCondition = new Condition(runLock);

	public static void selfTest1() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 0 Adults and 2 Children");
		begin(0, 2, b); 
	}
	
	public static void selfTest2() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 0 Adults and 3 Children");
		begin(0, 3, b); 
	}
	
	public static void selfTest3() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 0 Adults and 6 Children");
		begin(0, 6, b); 
	}
	
	public static void selfTest4() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 0 Adults and 7 Children");
		begin(0, 7, b); 
	}
	
	public static void selfTest5() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 0 Adults and 100 Children");
		begin(0, 100, b); 
	}
	
	public static void selfTest6() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 1 Adults and 2 Children");
		begin(1, 2, b); 
	}
	public static void selfTest7() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 2 Adults and 2 Children");
		begin(2, 2, b); 
	}
	public static void selfTest8() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 3 Adults and 2 Children");
		begin(3, 2, b); 
	}
	public static void selfTest9() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 1 Adult and 3 Children");
		begin(1, 3, b); 
	}
	public static void selfTest10() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 10 Adults and 3 Children");
		begin(10, 3, b); 
	}
	
	public static void selfTest11() {
		BoatGrader b = new BoatGrader();
		System.out.println("Test for Boat: testing with 100 Adults and 100 Children");
		begin(100, 100, b); 
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
		for(int i=0; i<adults; i++) {
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread n = new KThread(r);
			n.setName(""+i);
			n.fork();
		}
		for(int i=adults; i<children+adults; i++) {
			Runnable s = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread n = new KThread(s);
			n.setName(""+i);
			n.fork();
		}

		while(ActualNumAdultOnMolokai + ActualNumChildOnMolokai != adults + children) {
			finishCondition.wake();
			finishCondition.sleep();
		}
		finished = true;
		System.out.println("Simulation has finished");
	}


	static void AdultItinerary()
	{
		ActualNumAdultOnOahu++;
		int numChildOnOahu = ActualNumChildOnOahu;
		int numAdultOnOahu = ActualNumAdultOnOahu;
		int numChildOnMolokai = ActualNumChildOnMolokai;
		int numAdultOnMolokai = ActualNumAdultOnMolokai;
		String currentIsland = "Oahu";
		pilotLock.acquire();
		while(!finished) {
			
			if(currentIsland.equals("Oahu")) {
				numChildOnOahu = ActualNumChildOnOahu;
				numAdultOnOahu = ActualNumAdultOnOahu;
				while(numChildOnOahu != 1 || !boatLocation.equals("Oahu")) {
					pilot.wake();
					pilot.sleep();
					numChildOnOahu = ActualNumChildOnOahu;
					numAdultOnOahu = ActualNumAdultOnOahu;
				}
				bg.AdultRowToMolokai();
				ActualNumAdultOnOahu--;
				ActualNumAdultOnMolokai++;
				numChildOnOahu = ActualNumChildOnOahu;
				numAdultOnOahu = ActualNumAdultOnOahu;
				numChildOnMolokai = ActualNumChildOnMolokai;
				numAdultOnMolokai = ActualNumAdultOnMolokai;
				boatLocation = "Molokai";
				currentIsland = "Molokai";
			}
			else { //currentIsland is Molokai
				numChildOnMolokai = ActualNumChildOnMolokai;
				numAdultOnMolokai = ActualNumAdultOnMolokai;
				while(!boatLocation.equals("Molokai")) {
					pilot.wake();
					pilot.sleep();
					numChildOnMolokai = ActualNumChildOnMolokai;
					numAdultOnMolokai = ActualNumAdultOnMolokai;
				}
				if(numChildOnMolokai==0) {
					bg.AdultRowToOahu();
					ActualNumAdultOnOahu++;
					ActualNumAdultOnMolokai--;
					numChildOnOahu = ActualNumChildOnOahu;
					numAdultOnOahu = ActualNumAdultOnOahu;
					numChildOnMolokai = ActualNumChildOnMolokai;
					numAdultOnMolokai = ActualNumAdultOnMolokai;
					boatLocation = "Oahu";
					currentIsland = "Oahu";
				}
			}
			pilot.wake();
			pilot.sleep();
		}
		pilot.sleep();

	}

	static void ChildItinerary() {
		//Variable Declarations
		String currentIsland = new String("Oahu");
		ActualNumChildOnOahu++; //"checks in" to island
		//Sees the number of people on the island
		int numChildOnOahu = ActualNumChildOnOahu;
		int numAdultOnOahu = ActualNumAdultOnOahu;
		int numChildOnMolokai = ActualNumChildOnMolokai;
		int numAdultOnMolokai = ActualNumAdultOnMolokai;
		pilotLock.acquire();
		pilot.wake();
		pilot.sleep();

		while(!finished) {

			if(currentIsland.equals("Oahu")) {
				numChildOnOahu = ActualNumChildOnOahu;
				numAdultOnOahu = ActualNumAdultOnOahu;
				if(boatLocation.equals("Oahu")) {
					//Case: Thread is pilot
					pilot.wake();
					pilot.sleep();

					numChildOnOahu = ActualNumChildOnOahu;
					numAdultOnOahu = ActualNumAdultOnOahu;
					if(numChildOnOahu>1) { //if there are other children on the island
						
						if(!childIsPilot) { //there is no child pilot yet
							childIsPilot = true; //then become the pilot
							while(!boatHasChildAsRider) { //then wait for a rider
								pilot.wake();
								pilot.sleep();
							}
							//boat now has 2 children on it
							bg.ChildRowToMolokai();
							ActualNumChildOnOahu--;
							ActualNumChildOnMolokai++;
							boatLocation = "Molokai";
							currentIsland = "Molokai";
							childIsPilot=false;
							//update stuff accordingly, then
							pilot.wake(); //wakes up the rider
							pilot.sleep();
							//rider has gone to Molokai
							numChildOnOahu = ActualNumChildOnOahu;
							numAdultOnOahu = ActualNumAdultOnOahu;
							numChildOnMolokai = ActualNumChildOnMolokai;
							numAdultOnMolokai = ActualNumAdultOnMolokai;

						}
						else { //there is already a child pilot
							if(!boatHasChildAsRider) {
								boatHasChildAsRider=true; //become the rider
								pilot.wake(); //wake up the pilot
								pilot.sleep();
								//pilot has rowed the boat to Molokai
								bg.ChildRideToMolokai();
								ActualNumChildOnOahu--;
								ActualNumChildOnMolokai++;
								numChildOnOahu = ActualNumChildOnOahu;
								numAdultOnOahu = ActualNumAdultOnOahu;
								numChildOnMolokai = ActualNumChildOnMolokai;
								numAdultOnMolokai = ActualNumAdultOnMolokai;
								boatLocation = "Molokai";
								currentIsland = "Molokai";
								//update stuff accordingly
								boatHasChildAsRider=false;
							}
						}
					}
					else if(numAdultOnOahu>0) {
						numChildOnOahu = ActualNumChildOnOahu;
						numAdultOnOahu = ActualNumAdultOnOahu;
						pilot.wakeAll();
						pilot.sleep();
					}
					else /*if(numChildOnOahu + numAdultOnOahu==1)*/ {
						System.out.println("EndSequence started");
						//tryToFinish();
						//go into endsequence
						bg.ChildRowToMolokai();
						ActualNumChildOnOahu--;
						ActualNumChildOnMolokai++;
						numChildOnOahu = ActualNumChildOnOahu;
						numAdultOnOahu = ActualNumAdultOnOahu;
						numChildOnMolokai = ActualNumChildOnMolokai;
						numAdultOnMolokai = ActualNumAdultOnMolokai;
						boatLocation = "Molokai";
						currentIsland = "Molokai";
						finishLock.acquire();
						finishCondition.wake();
						finishCondition.sleep();
						/*
						pilot.wake();
						pilot.sleep();
						finishLock.release();
						*/
					}
				}
			}


			else { //Child is on Molokai
				numChildOnMolokai = ActualNumChildOnMolokai;
				numAdultOnMolokai = ActualNumAdultOnMolokai;
				if(boatLocation.equals("Molokai") && !boatHasChildAsRider) {
					bg.ChildRowToOahu();
					ActualNumChildOnOahu++;
					ActualNumChildOnMolokai--;
					numChildOnOahu = ActualNumChildOnOahu;
					numAdultOnOahu = ActualNumAdultOnOahu;
					numChildOnMolokai = ActualNumChildOnMolokai;
					numAdultOnMolokai = ActualNumAdultOnMolokai;
					boatLocation = "Oahu";
					currentIsland = "Oahu";
					//error is happening because this thread goes to Oahu before rider comes back?
				}
			}
			pilot.wake();
			pilot.sleep();
		}
		pilot.sleep();
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
