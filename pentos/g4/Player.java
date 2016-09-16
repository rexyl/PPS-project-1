package pentos.g4;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

	//put factories on top, residences on bottom;
	//bulid a wall after leaving room of 5 cells on each side
	//for each round iterate through all possible placements near left side
	//find the placement that leaves the fewest space

	private int factory_level;
	private int residence_level;
    private int left_min;
    private boolean road_built;
    private Set<Cell> road_neighbors;
    private Set<Cell> roads;

	public void init() { // function is called once at the beginning before play is called
		this.factory_level = 0;
		this.residence_level = 0;
        this.left_min = Integer.MAX_VALUE;
        this.road_built = false;
        this.road_neighbors = new HashSet<Cell>();
    }

    public void print(Move m){
        System.out.println("location: " + m.location.toString());
        System.out.println("building: " + m.request.toString());
        System.out.println("water: ");
        for(Cell w:m.water){
            System.out.print(w.toString());
        }
        System.out.println("park: ");
        for(Cell p:m.park){
            System.out.print(p.toString());
        }
    }


    public Move play(Building request, Land land) {
    	Move m = new Move(false);
        if(!road_built){
            buildRoad(land.side);
        }
    	List<Cell> startCoors = getStartCoors(land,request.type == Building.Type.FACTORY);
        //for(Cell s:startCoors) System.out.println(s.toString());
        this.left_min = Integer.MAX_VALUE;
        pickMove(startCoors,m,land,request);
        if(m.accept){
            //System.out.println("returned here");
            print(m);
            return m;
        }
    	//this level is full; build roads for next level;
        //update level information
		if(request.type == Building.Type.FACTORY){
			factory_level++;	
		}
		else{
			residence_level++;
		}

		startCoors = getStartCoors(land,request.type == Building.Type.FACTORY);
		this.left_min = Integer.MAX_VALUE;
		pickMove(startCoors,m,land,request);
    	return m;
    }



    //build the roads based on size of land
    private void buildRoad(int side){
        roads = new HashSet<Cell>();
        int lo = 5;
        int hi = side - 6;
        while(lo<hi){
            for(int j = 0; j < side; j++){
                Cell road_cell1 = new Cell(lo,j);
                Cell road_cell2 = new Cell(hi,j);
                //no room for roads; land must be full
                roads.add(road_cell1);
                roads.add(road_cell2);
                if(lo-1>=0) road_neighbors.add(new Cell(lo-1,j));
                if(lo+1<=side-1) road_neighbors.add(new Cell(lo+1,j));
                if(hi-1>=0) road_neighbors.add(new Cell(hi-1,j));
                if(hi+1<=side-1) road_neighbors.add(new Cell(hi+1,j));
            }
            lo += 6;
            hi -= 6;
        }
    }


    //for residence, pick the comb of water and park s.t available left space is the fewest
    //for factory just search all possible placements at starting coordinates
    private void pickMove(List<Cell> startCoors,Move m,Land land,Building request){
        Building[] rotations = request.rotations();  
        for (int ri = 0 ; ri < rotations.length ; ri++) {
            Building b = rotations[ri];
            for(Cell p:startCoors){
                if (land.buildable(b, p) && connected(land,b,p) && !hitRoad(b)){
                    Set<Cell> waters = new HashSet<Cell>();
                    Set<Cell> parks = new HashSet<Cell>();
                    //System.out.println("DFS");
                    //System.out.println(b.toString());
                    DFS(land,b,m,p,waters,parks);
                }
            }
        }
    }      

    //check if current Building is connected to a road or boundary
    private boolean connected(Land land,Building b,Cell p){
        Iterator<Cell> itr = b.iterator();
        int side = land.side;
        while(itr.hasNext()){
            Cell c = itr.next();
            if(c.i == 0 || c.i == side-1 || c.j == 0 || c.j == side-1) return true;
            if(road_neighbors.contains(c)) return true;
        }
        return false;
    }

    private boolean hitRoad(Building b){
        Iterator<Cell> itr = b.iterator();
        while(itr.hasNext()){
            Cell c = itr.next();
            if(roads.contains(c)) return true;
        }
        return false;
    }


    //search all valid placement of 4 cell water and parks
    //find the placement that returns the smallest leftRemainingCell
    private void DFS(Land land,Building b,Move m,Cell p,Set<Cell> waters,Set<Cell> parks){
        if(true || b.type == Building.Type.FACTORY){
            checkOptimal(land,b,m,p,waters,parks);
        }
        else{
            //search water first
            if(waters.size()<4){
                Iterator<Cell> itr = waters.iterator();
                boolean available = false;
                while(itr.hasNext()){
                    Cell w = itr.next();
                    for(Cell n:w.neighbors()){
                        if(unoccupied(land,n,p,b,waters,parks)){
                            available = true;
                            waters.add(n);
                            DFS(land,b,m,p,waters,parks);
                            waters.remove(n);
                        }
                    }
                }
                //waters are less than 4 but no more cells can be placed
                //remove water and check optimality
                if(!available){
                    checkOptimal(land,b,m,p,new HashSet<Cell>(),parks);
                }
            }
            //then park
            else if(parks.size()<4){
                Iterator<Cell> itr = parks.iterator();
                boolean available = false;
                while(itr.hasNext()){
                    Cell k = itr.next();
                    for(Cell n:k.neighbors()){
                        if(unoccupied(land,n,p,b,waters,parks)){
                            available = true;
                            parks.add(k);
                            DFS(land,b,m,p,waters,parks);
                            parks.remove(k);
                        }
                    }
                }
                //parks are less than 4 but no more cells can be placed
                //remove parks and check optimality
                if(!available){
                    checkOptimal(land,b,m,p,waters,new HashSet<Cell>());
                }
            }
            //both water and park are filled; check leftRemainingCells of this placement
            else{
                checkOptimal(land,b,m,p,waters,parks);
            }
        }
    }


    //see if current placement is optimal, if so modify move and curr_min
    private void checkOptimal(Land land,Building b,Move m,Cell p,Set<Cell> waters,Set<Cell> parks){
        int curr = leftRemainingCells(land,b,p,waters,parks);
       //System.out.println("optimal checked for" + b.toString());
        if(curr<left_min){
            m.accept = true;
            m.location = p;
            m.request = b;
            if(!road_built){
                m.road = roads;
                road_built = true;
            } 
            m.water = new HashSet<Cell>(waters);
            m.park = new HashSet<Cell>(parks);
            //find building rotation
            Building[] rotations = b.rotations();
            for(int ri = 0; ri < rotations.length; ri++){
                if(rotations[ri].equals(b)) m.rotation = ri;
            }
            this.left_min = curr;
            //System.out.println("current optimal is: " + b.toString());
        }
    }

    //scan from left to right, stop at the column with all unoccupied cells
    //count the number of unoccupied cells before that column
    private int leftRemainingCells(Land land,Building b,Cell p,Set<Cell> waters,Set<Cell> parks){
        boolean isFactory = b.type == Building.Type.FACTORY;
        int side = land.side;
        int row_lo = isFactory? side-1-(factory_level*6+5) : residence_level*6;
        int row_hi = isFactory? side-1-(factory_level*6) : residence_level*6+5;
        
        int res = 0;
        int j = 0;
        while(j < side){
            int unoccupied = 0;
            for(int i = row_lo; i < row_hi; i++){
                if(unoccupied(land,new Cell(i,j),p,b,waters,parks)){
                    unoccupied++;
                }
            }
            if(unoccupied == 5){
                break;
            } 
            res += unoccupied;
            j++;
        }
        return res;
    }


    private boolean unoccupied(Land land,Cell c,Cell p,Building b,Set<Cell> waters,Set<Cell> parks){
        if(!land.unoccupied(c) || waters.contains(c) || parks.contains(c) || c.i%6 == 5
            || (land.side-1-c.i)%6==5 ) return false;

        //check if this cell conflicts with building
        Iterator<Cell> itr = b.iterator();
        while(itr.hasNext()){
            Cell tmp = itr.next();
            if(tmp.equals(c)) return false;
            Cell target_lo = new Cell(tmp.i+p.i,tmp.j+p.j);
            if(target_lo.equals(c)) return false;
        }   
        return true;
    }

    //return all possible starting placement coors
    private List<Cell> getStartCoors(Land land,boolean isFactory){
    	int side = land.side;
    	int row_lo = isFactory? side-1-(factory_level*6+5) : residence_level*6;
    	int row_hi = isFactory? side-1-(factory_level*6) : residence_level*6+5;

    	List<Cell> res = new ArrayList<Cell>();
    	//go through rows within range, find rightmost available spots;
    	for(int i = row_lo; i < row_hi; i++){
    		int rightmost = Integer.MIN_VALUE;
    		for(int j = 0; j < side; j++){
    			if(!land.unoccupied(new Cell(i,j))){
    				rightmost = Math.max(rightmost,j);
    			}
    		}
            if(rightmost == Integer.MIN_VALUE) res.add(new Cell(i,0));
    		else if(rightmost != side) res.add(new Cell(i,rightmost+1));
    	}
    	return res;
    }
}