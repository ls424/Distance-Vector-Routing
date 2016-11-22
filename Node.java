import java.io.*;

/**
 * This is the class that students need to implement. The code skeleton is provided.
 * Students need to implement rtinit(), rtupdate() and linkhandler().
 * printdt() is provided to pretty print a table of the current costs for reaching
 * other nodes in the network.
 */ 
public class Node { 
    
    public static final int INFINITY = 9999;
    
    int[] lkcost;		/*The link cost between this node and other nodes*/
    int[][] costs;  		/*Define distance table, [destination][neighbor]*/
    int nodename;               /*Name of this node*/
    // int num;

    /* Class constructor */
    public Node() { 
    	lkcost = new int[4];
    	costs = new int[4][4];
    	// num = 4;
    }
    
    /* students to write the following two routines, and maybe some others */
    void rtinit(int nodename, int[] initial_lkcost) { 

    	this.nodename = nodename;
    	System.out.println();
    	System.out.println("Initialize Node:" + this.nodename);
    	int num = initial_lkcost.length;

        //update lkcost
    	for (int i = 0; i < num; i++) {
    		lkcost[i] = initial_lkcost[i];
    	}


    	//send distance vector Dx(y) for y in N to neighbor
    	for (int i = 0; i < num; i++) {

    		//update Dx(y) for all destinations y in N
    		for (int t = 0; t < initial_lkcost.length; t++) {
    			if (t == nodename) {
    				costs[i][t] = initial_lkcost[i]; //i:to destination node i, t:through node t
    				// sndcost[i] = costs[i][t];
    			} else {
    				costs[i][t] = INFINITY;
    			}
    		} 

    		//check if the node is its neighbor, then send the pkt
    		if (i != nodename && initial_lkcost[i] != INFINITY) {
    			Packet p = new Packet(nodename, i, lkcost);
    			System.out.println("Sending distance vector from " + nodename + "to" + i);
    			NetworkSimulator.tolayer2(p);
    		}
    	}
    	System.out.println("Initialization complete, print current distance table" );
    	printdt();
        System.out.println();
    }    
    
    void rtupdate(Packet rcvdpkt) {  

    	int src = rcvdpkt.sourceid;
    	int dst = rcvdpkt.destid;
    	int[] mincost = rcvdpkt.mincost;
    	System.out.printf("Update Node %d From Node %d\n", nodename, src);

    	//record the old costs to make preparation for costs table change
    	int num = this.costs.length;
    	int[][] oldCosts = new int[num][num];

    	for (int i = 0; i < num; i++) {
    		for (int j = 0; j < num; j++) {
    			oldCosts[i][j] = this.costs[i][j];
    		}
    	}

    	//update its distance table
    	for (int i = 0; i < lkcost.length; i++) {
    		if (mincost[i] == INFINITY) {
                this.costs[i][src] = INFINITY;
            } else {
                this.costs[i][src] = this.lkcost[src] + mincost[i];
            }
    	}


    	//if minimum cost changed, send poison
    	if (isChanged(oldCosts, this.costs)) {

			//send poison
    		for (int i = 0; i < num; i++) {
    			//only send poison to neighbor
    			if (i != this.nodename && lkcost[i] != INFINITY) {

                    //make posion
                    int[] poisons = new int[num];
                    for (int j = 0; j < num; j++) {
                        if (this.costs[j][i] < lkcost[j]) {
                            poisons[j] = INFINITY;
                        } else {
                            poisons[j] = getMin(costs[j]);
                    }
                }
    				Packet p = new Packet(this.nodename, i, poisons);
    				NetworkSimulator.tolayer2(p);
    			}
    		}
    	} 
    	printdt();
    }
    
    //check if the mincost to any other node has changed 
    boolean isChanged(int[][] oldcost, int[][] newcost) {
		int num = this.lkcost.length;
		for (int i = 0; i < num; i++) {
			if (i != this.nodename && getMin(oldcost[i]) != getMin(newcost[i])) {
				return true;
			}
		}
		return false;
    }

    //get the mincost for one row in distance table
    int getMin(int[] row) {
    	int min = INFINITY;
    	for (int i = 0; i < row.length; i++) {
    		min = Math.min(min, row[i]);
    	}
    	return min;
    }
    
    /* called when cost from the node to linkid changes from current value to newcost*/
    void linkhandler(int linkid, int newcost) {  
    	System.out.printf("\nCost from Node %d through linkeid %d changed to %d\n", nodename, linkid, newcost);
    	this.lkcost[linkid] = newcost;
    	costs[linkid][linkid] = newcost;
    	int num = lkcost.length;

    	// get new mincost
        int[] newMincost = new int[num];
        for (int j = 0; j < num; j++) {
            newMincost[j] = getMin(this.costs[j]);
        }

        for (int i = 0; i < this.lkcost.length; i++) {

        	//only send to neighbors
            if (i != this.nodename && lkcost[i] != INFINITY) {

                // update mincost according to split horizon with poison reverse
				for (int j = 0; j < num; j++) {
					//if i is the next hop to get to destination 
					if (costs[j][i] < newMincost[j]) {
						newMincost[j] = INFINITY;
					}
				}
                
                //send pkt
                Packet p = new Packet(this.nodename, i, newMincost);
                NetworkSimulator.tolayer2(p);
            }
        }
    }    


    /* Prints the current costs to reaching other nodes in the network */
    void printdt() {
        switch(nodename) {
	
	case 0:
	    System.out.printf("                via     \n");
	    System.out.printf("   D0 |    1     2 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     1|  %3d   %3d \n",costs[1][1], costs[1][2]);
	    System.out.printf("dest 2|  %3d   %3d \n",costs[2][1], costs[2][2]);
	    System.out.printf("     3|  %3d   %3d \n",costs[3][1], costs[3][2]);
	    break;
	case 1:
	    System.out.printf("                via     \n");
	    System.out.printf("   D1 |    0     2    3 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][2],costs[0][3]);
	    System.out.printf("dest 2|  %3d   %3d   %3d\n",costs[2][0], costs[2][2],costs[2][3]);
	    System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][2],costs[3][3]);
	    break;    
	case 2:
	    System.out.printf("                via     \n");
	    System.out.printf("   D2 |    0     1    3 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][1],costs[0][3]);
	    System.out.printf("dest 1|  %3d   %3d   %3d\n",costs[1][0], costs[1][1],costs[1][3]);
	    System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][1],costs[3][3]);
	    break;
	case 3:
	    System.out.printf("                via     \n");
	    System.out.printf("   D3 |    1     2 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d\n",costs[0][1],costs[0][2]);
	    System.out.printf("dest 1|  %3d   %3d\n",costs[1][1],costs[1][2]);
	    System.out.printf("     2|  %3d   %3d\n",costs[2][1],costs[2][2]);
	    break;
        }
    }
    
}
