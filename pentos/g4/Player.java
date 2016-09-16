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

	public void init() { // function is called once at the beginning before play is called
		this.factory_level = 0;
		this.residence_level = 0;
        this.left_min = Integer.MAX_VALUE;
        this.road_built = false;
        this.road_neighbors = new HashSet<Cell>();
    }

    public Move play(Building request, Land land) {
    	Move m = new Move(false);
        if(road_built = false){
            buildRoad(m,land.side);
            road_built = true;
        }

    	List<Cell> startCoors = getStartCoors(land,request.type == FACTORY);
        this.left_min = Integer.MAX_VALUE;
        pickMove(startCoors,m,request.type == FACTORY);
        if(m.accept) return m;

    	//this level is full; build roads for next level;
        //update level information
		Set<Cell> road_cells = new HashSet<Cell>();
		int row = 0;	
		if(request.type == FACTORY){
			factory_level++;	
			road_row = side - 1 - (factory_level*6-1);	
		}
		else{
			residence_level++;
			road_row = residence_level*6-1;
		}

		m.road = road_cells;
		startCoors = getStartCoors(land,request.type == FACTORY);
		this.left_min = Integer.MAX_VALUE;
		pickMove(startCoors,m,land,request);
    	return m;
    }



    //build the roads based on size of land
    private void buildRoad(Move m,int side){
        Set<Cell> roads = new HashSet<Cell>();
        int lo = 5;
        int hi = side - 6;
        while(lo<hi){
            for(int j = 0; j < land.side; j++){
                Cell road_cell = new Cell(lo,j));
                Cell road_cell = new Cell(hi,j));
                //no room for roads; land must be full
                roads.add(road_cell);
            }
            lo += 6;
            hi -= 6;
        }
        m.roads = roads;
    }


    //for residence, pick the comb of water and park s.t available left space is the fewest
    //for factory just search all possible placements at starting coordinates
    private void pickMove(List<Cell> startCoors,Move m,Land land,Building request){
        Building[] rotations = m.request.rotations;  
        for (int ri = 0 ; ri < rotations.length ; ri++) {
            Building b = rotations[ri];
            for(Cell p:startCoors){
                if (land.buildable(b, p) && connected(land,b,p)){
                    Set<Cell> waters = new HashSet<Cell>();
                    Set<Cell> parks = new HashSet<Cell>();
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

    //search all valid placement of 4 cell water and parks
    //find the placement that returns the smallest leftRemainingCell
    private void DFS(Land land,Building b,Move m,Cell p,Set<Cell> waters,Set<Cell> parks){
        if(request.type == FACTORY){
            checkOptimal(land,b,m,p,waters,parks,ri);
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
                            DFS(land,b,m,p,waters,parks,ri);
                            waters.remove(n);
                        }
                    }
                }
                //waters are less than 4 but no more cells can be placed
                //remove water and check optimality
                if(!available){
                    checkOptimal(land,b,m,p,new HashSet<Cell>(),parks,ri);
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
                            searched = true;
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
        if(curr<left_min){
            m.accept = true;
            m.location = p;
            m.road = new HashSet<Cell>();
            m.waters = waters;
            m.parks = parks;
            //find building rotation
            Building[] rotations = m.request.rotations;
            for(int ri = 0; ri < rotations.length; ri++){
                if(rotations[ri].equals(b)) m.rotation = ri;
            }
            this.left_min = curr;
        }
    }

    //scan from left to right, stop at the column with all unoccupied cells
    //count the number of unoccupied cells before that column
    private int leftRemainingCells(Land land,Building b,Cell p,Set<Cell> waters,Set<Cell> parks){
        boolean isFactory = request.type == FACTORY;
        int row_lo = isFactory? side-1-(factory_level*6+5) : residence_level*6;
        int row_hi = isFactory? side-1-(factory_level*6) : residence_level*6+5;
        int side = land.side;
        
        int res = 0;
        int j = 0;
        while(j < side){
            for(int i = row_lo; i < row_hi; i++){
                Cell dummy = new Cell(i,j);
                int unoccupied = 0;
                if(unoccupied(land,dummy,p,b,waters,parks)){
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
        Iterator<Cell> itr = request.iterator();
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
    		if(rightmost!= Integer.MIN_VALUE && rightmost != side) res.add(new Cell(i,rightmost+1));
    	}
    	return res;
    }
}