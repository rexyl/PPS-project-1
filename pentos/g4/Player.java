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
        this.road_cells = new HashSet<Cell>();
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
        this.max = Integer.MIN_VALUE;
        for (int i = 0 ; i < land.side ; i++){
            for (int j = 0 ; j < land.side ; j++) {
                Cell p = new Cell(i, j);
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
            //updateBoundary(m);
            print(m);
        } 
        return m;
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
        Set<Cell> building = buildingToSet(b,p);
        Set<Cell> waters = new HashSet<Cell>();
        if(request.type == Building.Type.RESIDENCE) {
            waters = findShortestWater(building,land,new HashSet<Cell>(),new HashSet<Cell>());
        }
        checkOptimal(land, b, m, p, waters, new HashSet<Cell>(), request, ri);
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
            if(hitSide(b,land.side)) return new HashSet<Cell>();
            for(Cell start:b.neighbors()){
                if(road_cells.contains(start)) return new HashSet<Cell>();
                if(land.unoccupied(start) && !building.contains(start) && !waters.contains(start) && !parks.contains(start)){
                    queue.offer(start);
                    //System.out.println("start: " + start.toString());
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
                stop = true;
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
        if(!stop) return null;
        while(end != null){
            output.add(end);
            end = end.previous;
        }
        return output;
    }



    // all cells here use absolute coordinates
    // build shortest sequence of water cells to connect to a set of cells b
    // if there are more than 4 water cells, take first four
    private Set<Cell> findShortestWater(Set<Cell> building, Land land, Set<Cell> waters, Set<Cell> parks) {
        Set<Cell> output = new HashSet<Cell>();
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
        int side = land.side;
        for(Cell b:building){
            for(Cell start:b.neighbors()){
                if(start.isWater()) return output;
                if(land.unoccupied(start) && !building.contains(start)){
                    queue.offer(start);
                }
            }
        }

        //System.out.println("queue size before: " + queue.size() );
        //System.out.println("there");
        Cell end = null;
        boolean stop = false;
        Cell marker = new Cell(land.side+1,land.side+1);
        queue.offer(marker);
        int level = 1;
        while (!queue.isEmpty() && !stop){
            //System.out.println(level);
            Cell curr = queue.poll();
            if(curr.equals(marker)){
                level++;
                if(!queue.isEmpty()) queue.offer(marker);
                continue;
            }
            checked[curr.i][curr.j] = true;
            for (Cell n : curr.neighbors()) { 
                if(checked[n.i][n.j]){
                    continue;
                } 
                if(n.isWater() || level>=4){
                    end = curr;
                    stop = true;
                    break;
                }
                checked[n.i][n.j] = true;
                n.previous = curr;                    
                if (land.unoccupied(n) && !building.contains(n)) {
                    queue.offer(n);
                }
            }
        }

        //if(end!=null) System.out.println("here");
        while(end != null && level>0){
            level--;
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



    private void checkOptimal(Land land, Building b, Move m, Cell p, Set<Cell> waters, Set<Cell> parks, Building request, int ri){      
        int sum = calcSum(b,p,waters,parks);
        boolean isFactory = request.type == Building.Type.FACTORY;
        if(!isFactory){
            sum +=  buildingToSet(b,p).size() * waters.size();
            sum += buildingToSet(b,p).size() * parks.size();
        }
        if ( (!isFactory && sum <= min) || (isFactory && sum >= max) ) {
            Set<Cell> new_roads = findShortestRoad(buildingToSet(b,p),land,waters,new HashSet<Cell>());
            if(new_roads == null){
                return;
            }
            m.road = new_roads;
            m.accept = true;
            m.location = p;
            m.request = request;
            m.water = new HashSet<Cell>(waters);
            m.park = new HashSet<Cell>(parks);
            m.rotation = ri;
            if(!isFactory) min = sum;
            else max = sum;
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
