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
    private int resident_right;
    private int resident_down;
    private int factory_left;
    private int factory_up;

    public void init() { // function is called once at the beginning before play is called
        this.gen = new Random();
        this.road_cells = new HashSet<Cell>();
        this.resident_right = 5;
        this.resident_down = 5;
        //TODO Land.side
        this.factory_left =  50 - 5;
        this.factory_up = 50 - 5;
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
        System.out.println("road: ");
        for(Cell p:m.road){
            System.out.print(p.toString());
        }
        System.out.println();
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
                if(!bounded(p,request.type == Building.Type.FACTORY)) continue;
                //System.out.println(p.toString());
                Building[] rotations = request.rotations();
                for (int ri = 0 ; ri < rotations.length ; ri++) {
                    Building b = rotations[ri];
                    if (land.buildable(b, p)){
                        search(m,b,land,p,ri,request);
                    }
                }
            }
        }
        if(m.accept){
            road_cells.addAll(m.road);
            updateBoundary(m);
            print(m);
        } 
        return m;
    }

    private boolean bounded(Cell c,boolean isFactory){
        if(isFactory){
            return c.i >= this.factory_up && c.j >= this.factory_left;
        }
        return c.i <= this.resident_down && c.j <= this.resident_right;
    }

    private void updateBoundary(Move m){
        Set<Cell> b = buildingToSet(m.request.rotations()[m.rotation],m.location);
        boolean isFactory = m.request.type == Building.Type.FACTORY;
        if(isFactory){
            for(Cell c:b){
                this.factory_up = Math.min(this.factory_up,c.i);
                this.factory_left = Math.min(this.factory_left,c.j);
            }
        }
        else{
            for(Cell c:b){
                this.resident_down = Math.max(this.resident_down,c.i);
                this.resident_right= Math.max(this.resident_right,c.j);
            }
        }
    }

    //return abosulte coordinates of a building in cell
    private Set<Cell> buildingToSet(Building building,Cell p){
        Set<Cell> b = new HashSet<Cell>();
        Iterator<Cell> itr = building.iterator();
        while(itr.hasNext()){
            b.add(convert(itr.next(),p));
        }
        return b;
    }

    private void search(Move m,Building b, Land land, Cell p, int ri, Building request){
        Set<Cell> waters = findShortestWater(buildingToSet(b,p),land,new HashSet<Cell>(),new HashSet<Cell>());
        checkOptimal(land, b, m, p, waters, new HashSet<Cell>(), request, ri);
        if(!m.accept) return;
        Set<Cell> new_roads = findShortestRoad(buildingToSet(b,p),land,waters,new HashSet<Cell>());
        m.road = new_roads;
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
            for(Cell start:b.neighbors()){
                if(!hitSide(start,side) && land.unoccupied(start) && !building.contains(start) && !waters.contains(start) && !parks.contains(start)){
                    queue.offer(start);
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
            output.add(end);
            end = end.previous;
        }
        return output;
    }



    // all cells here use absolute coordinates
    // build shortest sequence of road cells to connect to a set of cells b
    private Set<Cell> findShortestWater(Set<Cell> building, Land land, Set<Cell> waters, Set<Cell> parks) {
        Set<Cell> output = new HashSet<Cell>();
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
        int side = land.side;
        for(Cell b:building){ 
            for(Cell start:b.neighbors()){
                if(land.unoccupied(start) && !building.contains(start)){
                    queue.offer(start);
                }
            }
        }
        
        boolean stop = false;
        Cell end = null;
        Cell marker = null;
        int level = 1;
        queue.add(marker);
        while (!queue.isEmpty() && !stop) {
            Cell curr = queue.poll();
            if(curr == null){
                level++;
                queue.offer(null);
                //System.out.println(level);
                continue;
            }
            checked[curr.i][curr.j] = true;
            
            for (Cell n : curr.neighbors()) { 
                if(checked[n.i][n.j]) continue;
                n.previous = curr;    
                if(level >= 4 || n.isType(Cell.Type.WATER)){
                    end = curr;
                    stop = true;
                    break;
                }  
                if (land.unoccupied(n) && !building.contains(n)) {
                    queue.offer(n);
                }
            }
        }
        
        while(end != null){
            output.add(end);
            end = end.previous;
        }
        //System.out.println("here");
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
