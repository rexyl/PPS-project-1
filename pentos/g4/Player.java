package pentos.g4;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

    private Random gen;
    private Set<Cell> road_cells;
    private Set<Cell> water_cells;
    private Set<Cell> park_cells;
    private int min;
    private int max;
    public enum Type {PARK, WATER, ROAD};

    public void init() { // function is called once at the beginning before play is called
        this.gen = new Random();
        this.road_cells = new HashSet<Cell>();
        this.water_cells = new HashSet<Cell>();
        this.park_cells = new HashSet<Cell>();
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
        System.out.println();
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
                        search(m,buildingToSet(b,p),land,p,ri,request);
                    }
                }
            }
        }
        if(m.accept){
            road_cells.addAll(m.road);
            water_cells.addAll(m.water);
            park_cells.addAll(m.park);
            //print(m);
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

    private void search(Move m,Set<Cell> b, Land land, Cell p, int ri, Building request){
        Set<Cell> waters = new HashSet<Cell>();
        Set<Cell> parks = new HashSet<Cell>();
        if(request.type == Building.Type.RESIDENCE) {
            waters = findShortest(b,land,new HashSet<Cell>(),new HashSet<Cell>(),new HashSet<Cell>(),Type.WATER,4);
            if(waters == null) waters = new HashSet<Cell>();
            parks = findShortest(b,land,waters,parks,new HashSet<Cell>(),Type.PARK,4);
            if(parks == null) parks = new HashSet<Cell>();
        }
        checkOptimal(land, b, m, p, waters, parks, request, ri);
    }
    
    private boolean hitSide(Cell b,int side){
        return (b.i == 0 || b.i == side -1 || b.j == 0 || b.j == side - 1);
    }

    //return true if current cell hits the target we want to search
    private boolean hitTarget(Cell c,Type type,int side){
        if(type == Type.ROAD){
            if(hitSide(c,side)) return true;
            for(Cell r:road_cells){
                for(Cell n:r.neighbors()){
                    if(n.equals(c)) return true;
                }
            }
        }
        else if(type == Type.WATER){
            for(Cell w:water_cells){
                for(Cell n:w.neighbors()){
                    if(n.equals(c)) return true;
                }
            }
        }
        else{
            for(Cell p:park_cells){
                for(Cell n:p.neighbors()){
                    if(n.equals(c)) return true;
                }
            }
        }
        return false;
    }



    private Set<Cell> findShortest(Set<Cell> building, Land land, Set<Cell> waters, Set<Cell> parks, Set<Cell> roads,
        Type type, int depth){
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
        int side = land.side;
        for(Cell b:building){
            //if building is direct neighbor to targets, return empty set;
            if(hitTarget(b,type,side)) return new HashSet<Cell>();
            for(Cell start:b.neighbors()){
                //else push all direct neighbor of building to queue
                if(land.unoccupied(start) && !building.contains(start) && !waters.contains(start)
                    && !parks.contains(start) && !roads.contains(start)){
                    queue.offer(start);
                    //System.out.println("start");
                }
            }
        }

        //System.out.println("queue size before: " + queue.size() );
        //System.out.println("there");
        Cell end = null;
        Cell marker = new Cell(side+1,side+1);
        queue.offer(marker);
        int level = 1;
        while (!queue.isEmpty()){
            Cell curr = queue.poll();
            if(hitTarget(curr,type,side) || level>=depth){
                end = curr;
                break;
            }
            else if(curr.equals(marker)){
                level++;
                if(!queue.isEmpty()) queue.offer(marker);
            }
            else{
                for(Cell n : curr.neighbors()) { 
                    if(!checked[n.i][n.j]){
                        checked[n.i][n.j] = true;
                        n.previous = curr;            
                        if (land.unoccupied(n) && !building.contains(n) && !waters.contains(n) && !parks.contains(n) && !roads.contains(n)) {
                            queue.offer(n);
                        }
                    }
                }
            }
        }
        //System.out.println("level: " + level);
        if(end==null){
            return null;
        }
        Set<Cell> output = new HashSet<Cell>();
        while(end != null && level > 0){
            level--;
            output.add(end);
            end = end.previous;
        }
        return output;
    }


     private int calcSum(Set<Cell> b, Cell p,Set<Cell> waters,Set<Cell> parks){
        int sum = 0;
        for(Cell building_cell:b){
            sum += building_cell.i;
            sum += building_cell.j;
        }

        Iterator<Cell> itr = waters.iterator();
        while(itr.hasNext()){
            Cell c = itr.next();
            sum += c.i;
            sum += c.j;
        }

        itr = parks.iterator();
        while( itr.hasNext() ){
            Cell c = itr.next();
            sum += c.i;
            sum += c.j;
        }
        return sum;
    }



    private void checkOptimal(Land land, Set<Cell> b, Move m, Cell p, Set<Cell> waters, Set<Cell> parks, Building request, int ri){      
        int sum = calcSum(b,p,waters,parks);
        boolean isFactory = request.type == Building.Type.FACTORY;
        if(!isFactory){
            sum +=  b.size() * waters.size() * 2;
            sum += b.size() * parks.size() * 2;
        }
        if ( (!isFactory && sum <= min) || (isFactory && sum >= max) ) {
            Set<Cell> new_roads = findShortest(b,land,waters,parks,new HashSet<Cell>(),Type.ROAD,land.side);
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
    private Cell convert(Cell c,Cell p){
        return new Cell(c.i+p.i,c.j+p.j);
    }
}
