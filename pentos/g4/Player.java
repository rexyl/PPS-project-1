package pentos.g4;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

    private Random gen;
    private Set<Cell> road_cells;
    private int min;
    private int max;

    public void init() { // function is called once at the beginning before play is called
        this.gen = new Random();
        road_cells = new HashSet<Cell>();
    }
    
    public Move play(Building request, Land land) {
        // find all valid building locations and orientations
        Move m = new Move(false);
        System.out.println("play started");
        System.out.println("request: " + request.toString());
        this.min = Integer.MAX_VALUE;
        this.max= Integer.MIN_VALUE;
        for (int i = 0 ; i < land.side ; i++){
            for (int j = 0 ; j < land.side ; j++) {
                Cell p = new Cell(i, j);
                //System.out.println(p.toString());
                Building[] rotations = request.rotations();
                for (int ri = 0 ; ri < rotations.length ; ri++) {
                    Building b = rotations[ri];
                    if (land.buildable(b, p)){
                        DFS(land,b,m,p,new HashSet<Cell>(),new HashSet<Cell>(),request,ri);
                    }
                }
            }
        }
        if(!m.accept) return m;
        //System.out.println("reached here");
        Set<Cell> b = new HashSet<Cell>();
        Iterator<Cell> itr = m.request.rotations()[m.rotation].iterator();
        while(itr.hasNext()){
            b.add(convert(itr.next(),m.location));
        }
        Set<Cell> new_roads = findShortestRoad(b,land,m.water,m.park);
        if(new_roads == null){
            m.road = new HashSet<Cell>();
        }
        else{
            m.road = new_roads;
            road_cells.addAll(m.road);
            //System.out.println("road built");
        }
        return m;
    }
    
    private boolean hitSide(Cell b,int side){
        return (b.i == 0 || b.i == side -1 || b.j == 0 || b.j == side - 1);
    }


    // all cells here use absolute coordinates
    // build shortest sequence of road cells to connect to a set of cells b
    private Set<Cell> findShortestRoad(Set<Cell> building, Land land, Set<Cell> waters, Set<Cell> parks) {
        Set<Cell> output = new HashSet<Cell>();
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
        int side = land.side;
        for(Cell b:building){ 
            //System.out.println("b: " + b.toString());
            for(Cell start:b.neighbors()){
                if(!hitSide(start,side) && land.unoccupied(start) && !building.contains(start) && !waters.contains(start) && !parks.contains(start)){
                    //start.previous = null;
                    //System.out.println("added start " + start.toString());
                    queue.add(start);
                }
            }
        }
        
        boolean stop = false;
        Cell end = null;
        while (!queue.isEmpty() && !stop) {
            Cell curr = queue.poll();
            checked[curr.i][curr.j] = true;
            if(road_cells.contains(curr)){
                end = curr.previous;
                break;
            }
            else if(hitSide(curr,side)){
                end = curr;
                stop = true;
                break;
            }
            for (Cell n : curr.neighbors()) { 
                if(checked[n.i][n.j]) continue;
                n.previous = curr;    
                if(road_cells.contains(n)){
                    queue.offer(n);
                }  
                else if (land.unoccupied(n) && !building.contains(n) && !waters.contains(n) && !parks.contains(n)) {
                    queue.offer(n);
                }
            }
        }
        while(end != null){
            //System.out.println("add road");
            output.add(end);
            end = end.previous;
        }
        return output;
    }

     private int calcSum(Building b, Cell p,Set<Cell> waters,Set<Cell> parks){
        int sum = 0;
        for(Cell building_cell:b){
            sum += (building_cell.i + p.i);
            sum += (building_cell.j + p.j);
        }

        Iterator<Cell> itr = waters.iterator();
        while(itr.hasNext()){
            Cell c = itr.next();
            sum += c.i;
            sum += c.j;
        }

        itr= parks.iterator();
        while(itr.hasNext()){
            Cell c = itr.next();
            sum += c.i;
            sum += c.j;
        }
        return sum;
    }

    
     //search all valid placement of 4 cell water and parks
    //find the placement that returns optimal solution 
    private void DFS(Land land,Building b,Move m,Cell p,Set<Cell> waters,Set<Cell> parks,Building request,int ri){
        /*
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
                        if(land.unoccupied(n) && !waters.contains(n) && !parks.contains(n)){
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
                        if(land.unoccupied(n) && !waters.contains(n) && !parks.contains(n)){
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
        */
        checkOptimal(land,b,m,p,waters,parks,request,ri);
    }

    private void checkOptimal(Land land, Building b, Move m, Cell p, Set<Cell> waters, Set<Cell> parks, Building request, int ri){      
        int sum = calcSum(b,p,waters,parks);
        if ( (request.type == Building.Type.RESIDENCE && sum < min) || 
         (request.type == Building.Type.FACTORY && sum > max) ) {
            if(request.type == Building.Type.RESIDENCE) min = sum;
            if(request.type == Building.Type.FACTORY) max = sum;

            m.accept = true;
            m.location = p;
            m.request = request;
            m.water = new HashSet<Cell>(waters);
            m.park = new HashSet<Cell>(parks);
            m.rotation = ri;
            return;
        }
    }

    //convert cell b into absolute coordinates
    private Cell convert(Cell b,Cell p){
        //System.out.println("b.i: " + b.i);
        //System.out.println("b.j: " + b.j);
        //System.out.println("p.i: " + p.i);
        //System.out.println("p.j: " + p.j);
        return new Cell(b.i+p.i,b.j+p.j);
    }
}
