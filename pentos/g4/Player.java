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
    private int min_right_cells_num;
    private int min_right_index;
    private Cell cur_best;
    private boolean road_built;
    private Set<Cell> road_neighbors;
    private Set<Cell> roads;

	public void init() { // function is called once at the beginning before play is called
		this.factory_level = 0;
		this.residence_level = 0;
        this.min_right_cells_num = Integer.MAX_VALUE;
        this.min_right_index = Integer.MAX_VALUE;
        this.cur_best = null;
        this.road_built = false;
        this.road_neighbors = new HashSet<Cell>();
    }

    public void print(Move m){
        System.out.println("location: " + m.location.toString());
        System.out.println("building: " + m.request.rotations()[m.rotation].toString());
        System.out.println("rotation: " + Integer.toString(m.rotation));
        System.out.println("water: ");
        for(Cell w:m.water){
            System.out.print(w.toString());
        }
        System.out.println();
        System.out.println("park: ");
        for(Cell p:m.park){
            System.out.print(p.toString());
        }
        System.out.println();
    }


    public Move play(Building request, Land land) {
        System.out.println("Play started");
        System.out.println("====================");
        System.out.println("factory? " + (request.type == Building.Type.FACTORY));
    	Move m = new Move(false);
        if(!road_built){
            buildRoad(land.side);
            m.road = roads;
            road_built = true;
        }
        else{
            m.road = new HashSet<Cell>();
        }

        /*
        System.out.println("In this play roads are: ");
        for(Cell p:m.road){
           System.out.print(p.toString());
        }
        System.out.println("");
        */
    	List<Cell> startCoors = getStartCoors(land,request.type == Building.Type.FACTORY);
        //for(Cell s:startCoors) System.out.println(s.toString());
        this.min_right_cells_num = Integer.MAX_VALUE;
        this.min_right_index = Integer.MAX_VALUE;
        this.cur_best = null;
        pickMove(startCoors,m,land,request);
        System.out.println("startCoors size:" + startCoors.size());
        if(m.accept){
            //System.out.println("returned here");
            print(m);
            System.out.println("====================");
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
        System.out.println("2 startCoors size:" + startCoors.size());
		this.min_right_cells_num = Integer.MAX_VALUE;
        this.min_right_index = Integer.MAX_VALUE;
        this.cur_best = null;
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
                if (land.buildable(b, p) && connected(land,b,p) && !hitRoad(p,b)){
                    //System.out.println("check building: " + b.toString());
                    System.out.println("location: " + p.toString() + " is buildable");
                    DFS(land,b,m,p,new HashSet<Cell>(),new HashSet<Cell>(),request,ri);
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
            Cell loc = new Cell(c.i+p.i,c.j+p.j);
            if(loc.i == 0 || loc.i == side-1 || loc.j == 0 || loc.j == side-1) return true;
            if(road_neighbors.contains(loc)) return true;
        }
        return false;
    }

    private boolean hitRoad(Cell location, Building b){
        // System.out.println("In hitRoad, roads are: ");
        // for(Cell r:roads){
        //     System.out.print(r.toString());
        // }
        Iterator<Cell> itr = b.iterator();
        while(itr.hasNext()){
            Cell c = itr.next();
            Cell check_cell = new Cell(c.i+location.i, c.j+location.j);
            if(roads.contains(check_cell)) return true;
        }
        return false;
    }


    //convert cell b into absolute coordinates
    private Cell convert(Cell b,Cell p){
        return new Cell(b.i+p.i,b.j+p.j);
    }

    //search all valid placement of 4 cell water and parks
    //find the placement that returns the smallest leftRemainingCell
    private void DFS(Land land,Building b,Move m,Cell p,Set<Cell> waters,Set<Cell> parks,Building request,int ri){
        if(b.type == Building.Type.FACTORY){
            checkOptimal(land,b,m,p,waters,parks,request,ri);
        }
        else{
            //search water first
            if(waters.size()<4){
                Set<Cell> copy = new HashSet<Cell>(waters);
                Iterator<Cell> itr = waters.size() == 0? b.iterator():copy.iterator();
                boolean available = false;
                while(itr.hasNext()){
                    Cell next = itr.next();
                    Cell curr = waters.size() == 0? convert(next,p) : next;
                    for(Cell n:curr.neighbors()){
                        if(unoccupied(land,n,p,b,waters,parks)){
                            available = true;
                            waters.add(n);
                            DFS(land,b,m,p,waters,parks,request,ri);
                            waters.remove(n);
                        }
                    }
                }
                //waters are less than 4 but no more cells can be placed
                //remove water and check optimality
                if(!available){
                    checkOptimal(land,b,m,p,new HashSet<Cell>(),parks,request,ri);
                }
            }
            //then park
            else if(parks.size()<4){
                Set<Cell> copy = new HashSet<Cell>(parks);
                Iterator<Cell> itr = parks.size() == 0? b.iterator():copy.iterator();
                boolean available = false;
                while(itr.hasNext()){
                    Cell next = itr.next();
                    Cell curr = parks.size() == 0? convert(next,p) : next;
                    for(Cell n:curr.neighbors()){
                        if(unoccupied(land,n,p,b,waters,parks)){
                            available = true;
                            parks.add(n);
                            DFS(land,b,m,p,waters,parks,request,ri);
                            parks.remove(n);
                        }
                    }
                }
                //parks are less than 4 but no more cells can be placed
                //remove parks and check optimality
                if(!available){
                    checkOptimal(land,b,m,p,waters,new HashSet<Cell>(),request,ri);
                }
            }
            //both water and park are filled; check leftRemainingCells of this placement
            else{
                checkOptimal(land,b,m,p,waters,parks,request,ri);
            }
        }
    }

    private int[] findRightMostBuildingCellIndex(Building b, Cell p){
        int right_most_ind = 0, right_cells_num = 0;
        for(Cell building_cell:b){
            if (building_cell.j+p.j > right_most_ind) {
                right_most_ind = building_cell.j+p.j;
            }
        }
        for(Cell building_cell:b){
            if (building_cell.j+p.j == right_most_ind) {
                right_cells_num++;
            }
        }
        return new int[]{right_most_ind, right_cells_num};
    }

    //see if current placement is optimal, if so modify move and curr_min
    private void checkOptimal(Land land,Building b,Move m,Cell p,Set<Cell> waters,Set<Cell> parks,Building request,int ri){
        //int curr = leftRemainingCells(land,b,p,waters,parks);
       //System.out.println("optimal checked for" + b.toString());
        //System.out.println(left_min);    
        int[] temp = findRightMostBuildingCellIndex(b,p);
        int right_most_ind = temp[0];
        int right_cells_num = temp[1];
        if (right_most_ind < min_right_index || (right_most_ind == min_right_index && right_cells_num < min_right_cells_num) ) {
            cur_best = p;
            min_right_index = right_most_ind;
            min_right_cells_num = right_cells_num;

            m.accept = true;
            m.location = p;
            m.request = request;
            m.water = new HashSet<Cell>(waters);
            m.park = new HashSet<Cell>(parks);
            m.rotation = ri;
            return;
        }
        // if(curr<left_min){
        //     m.accept = true;
        //     m.location = p;
        //     m.request = request;
        //     m.water = new HashSet<Cell>(waters);
        //     m.park = new HashSet<Cell>(parks);
        //     m.rotation = ri;
            //find building rotation
            /*
            Building[] rotations = request.rotations();
            for(int ri = 0; ri < rotations.length; ri++){
                if(rotations[ri].equals(b)) m.rotation = ri;
            }
            */
            //this.left_min = curr;
            //System.out.println("current optimal is: " + b.toString());
        
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


    //c is using abosulte coordinates
    private boolean unoccupied(Land land,Cell c,Cell p,Building b,Set<Cell> waters,Set<Cell> parks){
        boolean res = true;
        if(!land.unoccupied(c) || waters.contains(c) || parks.contains(c)){
            return false;
        }

        boolean isFactory = b.type == Building.Type.FACTORY;
        if(isFactory && (land.side-1-c.i)%6==5) return false;
        if(!isFactory && c.i%6 == 5) return false;
        
        //check if this cell conflicts with building
        Iterator<Cell> itr = b.iterator();
        while(itr.hasNext()){
            Cell tmp = itr.next();
            Cell target_lo = new Cell(tmp.i+p.i,tmp.j+p.j);
            if(target_lo.equals(c)) {
                return false;
            }
        }

        return true;
    }

    //return all possible starting placement coors
    private List<Cell> getStartCoors(Land land,boolean isFactory){
    	int side = land.side;

        List<Cell> res = new ArrayList<Cell>();
        
        if(factory_level > side/12 && isFactory) return res;
        if(residence_level > side/12 && !isFactory) return res;
        if(isFactory){
            int level = factory_level;
            while(level>=0){
                res.addAll(getCellInRows(land,side-(level*6+5),side-(level*6)));
                level--;
            }
        }
        else{
            int level = residence_level;
            while(level>=0){
                res.addAll(getCellInRows(land,level*6,level*6+5));
                level--;
            }
        }

     //    int row_low = isFactory? side-1-(factory_level*6+5) : residence_level*6;
     //    int row_high = isFactory? side-1-(factory_level*6) : residence_level*6+5;
     //    int row_lo = row_low, row_hi = row_high;
     //    int right_lane = 0;       
    	// //go through rows within range, find rightmost available spots;
     //    while(row_lo>=0 && row_lo<side && row_hi>=0 && row_hi<side){
     //    	for(int i = row_lo; i < row_hi; i++){
     //    		for(int j = 0; j < side; j++){
     //    			if(!land.unoccupied(new Cell(i,j))){
     //                    right_lane = Math.max(j,right_lane);
     //    			}
     //    		}
     //    	}
     //        for(int i = row_lo; i < row_hi; i++){
     //            for(int j = 0; j < right_lane+1; j++){
     //                if(land.unoccupied(new Cell(i,j))){
     //                    res.add(new Cell(i,j));
     //                }
     //            }
     //        }
     //        if(isFactory){
     //            row_lo += 5;
     //            row_hi += 5;
     //        }
     //        else{
     //            row_lo -= 5;
     //            row_hi -= 5;                
     //        }
     //    }

        for(Cell c:res){
            System.out.print("start cell: " + c.toString());
        }
        System.out.println();

    	return res;
    }

    
    private List<Cell> getCellInRows(Land land,int row_lo,int row_hi){
        List<Cell> res = new ArrayList<Cell>();
        int side = land.side;
        int right_lane = 0;
        for(int i = row_lo; i < row_hi; i++){
            for(int j = 0; j < side; j++){
                if(!land.unoccupied(new Cell(i,j))){
                    right_lane = Math.max(j,right_lane);
                }
            }
        }
        if (right_lane>0 && right_lane+1<side) {
            right_lane++;
        }
        for(int i = row_lo; i < row_hi; i++){
            for(int j = 0; j < right_lane+1 && j < side; j++){
                if(land.unoccupied(new Cell(i,j))){
                    res.add(new Cell(i,j));
                }
             }
        }
        return res;
    }
    
}