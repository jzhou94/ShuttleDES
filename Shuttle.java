/* CS350HW8 Problem 2, 3, 4
 * Joe Zhou
 * jzhou94@bu.edu
 */

import java.util.*;
import java.util.concurrent.Semaphore;
public class Shuttle extends Thread {
  
  int loopCounter = 1;                                             // Number of times passengers return to airport
  private int i;                                                   // ID of shuttle or passenger
  private int type;                                                // Either Passenger (0) or Shuttle (1)
  public int travelingTime;                                        // Traveling time between terminals for shuttle
  public int minTravelingTime = 15;                                // Minimum traveling time
  public int maxTravelingTime = 25;                                // Maximum traveling time
  public static int passengersServed = 0;                          // Number of people that have reached destination
  int N = 10;                                                      // Maximum number of passengers allowed in shuttles
  public static int K = 6;                                         // Number of terminals
  public static int M = 4;                                         // Number of shuttles
  public static int P = 50;                                        // Number of passengers in airport
  public static int[] numPassengers = new int[M];                  // Number of passengers in each shuttle
  public static int[] numWaiting = new int[K];                     // Number of passengers waiting on each platform
  public static int[][] numLeaving = new int[M][K];                // Number of Passengers leaving shuttle at terminal
  public static int[] atStation = new int[K];                      // Shuttle that's currently in terminal un/boarding
  public static Semaphore[] platforms = new Semaphore[K];          // Prevents passengers from rushing to shuttles
  public static Semaphore[] boarding = new Semaphore[K];           // Prevents shuttles from leaving while boarding
  public static Semaphore[] shuttlesA = new Semaphore[K];          // Prevents people from entering unavailable shuttle

  // Problem 3 - Prevents multiple shuttles from boarding/unboarding at single terminal
  public static Semaphore[] terminals = new Semaphore[K];
  
  // Problem 4 - Prevents people from leaving shuttle outside terminals. Separated from boarding semaphore shuttlesA
  //             so passengers can leave before recharge and new passengers are blocked from entering during recharge
  public static Semaphore[] shuttlesD = new Semaphore[K];
  public static int[] recharge = new int[M];                       // Amount of time traveled for each car
  public int rechargeThreshhold = 100;                             // Time before recharge is required for shuttle
  public int rechargeTime = 100;                                   // Time shuttle will recharge for
  
  public Shuttle(int id, int t) {
    i = id;
    type = t;
  }
  
  // Function for shuttles traveling between terminals
  public void traverseTerminals() {
    Random r = new Random();
    int current = r.nextInt(K);                                    // Have shuttles start from intial terminal
    while (passengersServed != (P * loopCounter)) {                // Run shuttles until all passengers are served
 
      System.out.println("Entering:  Terminal " + current + "    Number of Passengers: " + numPassengers[i] + "    Shuttle - " + i);
      
      try {
        terminals[current].acquire();                              // Wait until no other shuttles are un/boarding here
      } catch(InterruptedException e){}
      
      try {
        platforms[current].acquire();                              // Passengers just arriving board next shuttle
      } catch(InterruptedException e){}
      System.out.println("Entering:  Platform " + current + "    Waiting on Platform:  " + numWaiting[current] + "    Shuttle - " + i);
      atStation[current] = i;                                      // Tells terminal that this shuttle is un/boarding
      
      if (numLeaving[i][current] != 0) {                           // There are passengers that wants to leave
        System.out.println("Unboard:   Terminal " + current + "    Passengers Leaving:   " + numLeaving[i][current] + "    Shuttle - " + i);
        shuttlesD[current].release();                              // Allows passengers to disembark
        try {
          boarding[current].acquire();                             // Waits for passengers to leave
        } catch(InterruptedException e){}
      }
      
      if (recharge[i] >= rechargeThreshhold) {                     // Shuttle needs to recharge
        platforms[current].release();                              // People prevented from rushing can get on next
        terminals[current].release();                              // Allow next shuttle to board/unboard
        System.out.println("Entering: ChargeStation" + "  Charging for Time:  " + rechargeTime + "    Shuttle - " + i);
        try {
          Thread.sleep(rechargeTime);
        } catch(InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        recharge[i] = 0;                                           // Shuttle fully recharged
        
        try {
          terminals[current].acquire();                            // Wait until no other shuttles are un/boarding here
        } catch(InterruptedException e){}
        
        try {
          platforms[current].acquire();                            // Passengers just arriving board next shuttle
        } catch(InterruptedException e){}
        System.out.println("Leaving:  ChargeStation" + "  Heading to Terminal:  " + current + "    Shuttle - " + i);
        atStation[current] = i;                                    // Tells terminal that this shuttle is boarding
      }
      
      // Passengers on shuttle less than max and there are passengers on platform waiting to enter
      if (numPassengers[i] < N && numWaiting[current] != 0) {
        System.out.println("Boarding:  Terminal " + current + "    Number of Passengers: " + numPassengers[i] + "    Shuttle - " + i);
        shuttlesA[current].release();                              // Lets passengers enter
        try {
          boarding[current].acquire();                             // Wait until passengers have entered before leaving
        } catch(InterruptedException e){}
      }
      
      System.out.println("Leaving:   Terminal " + current + "    Number of Passengers: " + numPassengers[i] + "    Shuttle - " + i);
      atStation[current] = -1;                                     // No shuttles are un/boarding at this terminal
      
      platforms[current].release();                                // People prevented from rushing can get on next
      terminals[current].release();                                // Allow next shuttle to board/unboard
      current++;                                                   // Shuttle decides next terminal
      if (current == K) {                                          // Shuttle loops terminals
        current = 0;
      }
      
      Random rT = new Random();
      travelingTime = rT.nextInt(maxTravelingTime - minTravelingTime) + minTravelingTime;
      
      try {                                                        // Shuttle traveling between terminals
        Thread.sleep(travelingTime);
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      recharge[i] += travelingTime;                                // Shuttle recharge timer increased
    }
  }
  
  // Function for passengers going from arriving terminal to destionation
  public void waitOnShuttle() {
    
    while (loopCounter > 0) {
      loopCounter--;
      Random r = new Random();
      int arrival = r.nextInt(K);                                   // Initial arrival terminal
      int destination = r.nextInt(K);                               // Destination terminal
      while (destination == arrival) {                              // Make sure the two aren't equal
        destination = r.nextInt(K);
      }
      
      System.out.println("Entering:  Terminal " + arrival + "    Destination: Terminal " + destination + "    Passenger - " + i);
      try {
        platforms[arrival].acquire();                               // Tries to get on platform for shuttle
      } catch(InterruptedException e){}                             // Can't if shuttle is currently boarding/unboarding
      numWaiting[arrival]++;                                        // Increase number of passengers on platform
      System.out.println("Entering:  Platform " + arrival + "    Destination: Terminal " + destination + "    Passenger - " + i);
      // No shuttles boarding/unboarding currently, subsequent passengers can go on platform
      platforms[arrival].release();
      
      
      try {
        shuttlesA[arrival].acquire();                               // Wait for shuttle with less than max passengers
      } catch(InterruptedException e){}
      numWaiting[arrival]--;                                        // Decrease number of passengers on platform
      System.out.println("Entering:   Shuttle " + atStation[arrival] + "    Destination: Terminal " + destination + "    Passenger - " + i);
      numPassengers[atStation[arrival]]++;                          // Increase number of passengers in shuttle
      numLeaving[atStation[arrival]][destination]++;                // Increase number of people leaving at destination
      if (numWaiting[arrival] != 0 && numPassengers[atStation[arrival]] < N) {
        shuttlesA[arrival].release();                               // Shuttle still available and more wants to enter
      } else {
        boarding[arrival].release();                                // Shuttle leaves, max passenger or no more boarding
      }
      
      try {
        shuttlesD[destination].acquire();                           // Wait for shuttle to arrive at destination
      } catch(InterruptedException e){}
      System.out.println("Exiting:    Shuttle " + atStation[destination] + "    Destination: Terminal " + destination + "    Passenger - " + i);
      numPassengers[atStation[destination]]--;                      // Decrease number of people in shuttle
      passengersServed++;                                           // Increase number of people reaching destination
      numLeaving[atStation[destination]][destination]--;            // Decrease number of people exiting at terminal
      if (numLeaving[atStation[destination]][destination] != 0) {
        shuttlesD[destination].release();                           // More people want to disembark
      } else if (passengersServed != P){
        boarding[destination].release();                            // No more want to disembark, can recharge or board
      }
    }
  }
  
  public void run() {   
    if (type == 0) {
      waitOnShuttle();                                             // Thread is passenger
    } else {
      traverseTerminals();                                         // Thread is shuttle
    }
  }

  
  public static void main(String[] args) { 
    
    // Initialization for Semaphores and arrays
    for (int x = 0; x < K; x++) {
      terminals[x] = new Semaphore(1, false);
    }
    for (int x = 0; x < K; x++) {
      shuttlesA[x] = new Semaphore(0, false);
    }
    for (int x = 0; x < K; x++) {
      shuttlesD[x] = new Semaphore(0, false);
    }
    for (int x = 0; x < K; x++) {
      platforms[x] = new Semaphore(1, false);
    }
    for (int x = 0; x < K; x++) {
      boarding[x] = new Semaphore(0, false);
    }

    for (int x = 0; x < M; x++) {
      numPassengers[x] = 0;
    }
    for (int x = 0; x < K; x++) {
      numWaiting[x] = 0;
    }
    for (int x = 0; x < K; x++) {
      atStation[x] = -1;
    }
    for (int x = 0; x < M; x++) {
      for (int y = 0; y < K; y++) {
        numLeaving[x][y] = 0;
      }
    }
    for (int x = 0; x < M; x++) {
      recharge[x] = 0;
    }
    
    // Initialization for shuttle and passenger threads
    Shuttle[] s = new Shuttle[K];
    Shuttle[] p = new Shuttle[P];
    for (int x = 0; x < P; x++) {
      p[x] = new Shuttle(x, 0);
      p[x].start();
    }
    
    for (int x = 0; x < M; x++) {
      s[x] = new Shuttle(x, 1);
      s[x].start();
    }
  }
  
}