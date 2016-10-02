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
    private Set<Cell> prev_cells;
    private int min;
    private int max;
    public enum Type {PARK, WATER, ROAD};

    public void init() { // function is called once at the beginning before play is called
        this.gen = new Random();
        this.road_cells = new HashSet<Cell>();
        this.water_cells = new HashSet<Cell>();
        this.park_cells = new HashSet<Cell>();
        this.prev_cells = new HashSet<Cell>();
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
        if(prev_cells.size() == 0){
            for(int i  = 0; i < land.side; i++){
                prev_cells.add(new Cell(0,i));
                prev_cells.add(new Cell(land.side-1,i));
            }
            for(int j  =0; j < land.side; j++){
                prev_cells.add(new Cell(j,0));
                prev_cells.add(new Cell(j,land.side-1));
            }

        }

        // find all valid building locations and orientations
        ArrayList<Move> possibleMoves = new ArrayList<Move>();
        this.min = Integer.MAX_VALUE;
        this.max = Integer.MIN_VALUE;
        boolean isFactory = request.type == Building.Type.FACTORY;
        for (int i = 0 ; i < land.side ; i++){
            for (int j = 0 ; j < land.side ; j++) {
                Cell p = new Cell(i, j);
                Building[] rotations = request.rotations();
                for (int ri = 0 ; ri < rotations.length ; ri++) {
                    Building b = rotations[ri];
                    if (land.buildable(b, p) && isBounded(buildingToSet(b,p)) ){
                        Move temp_m = search(buildingToSet(b,p),land,p,ri,request);
                        if(temp_m.accept){
                            possibleMoves.add(temp_m);
                        }
                    }
                }
            }
        }
        Move move = scoreMove(possibleMoves, isFactory, land);
        if(move.accept){
            road_cells.addAll(move.road);
            water_cells.addAll(move.water);
            park_cells.addAll(move.park);
            updateBoundary(move);
        }
        return move;
    }

    private boolean isBounded(Set<Cell> b){
        //return true;
        
        for(Cell building_cell:b){
            for(Cell n:building_cell.neighbors()){
                if(prev_cells.contains(n)){
                    return true;
                }
            }
        }
        return false;
        
    }

    private void updateBoundary(Move m){
        prev_cells.addAll(buildingToSet(m.request.rotations()[m.rotation], m.location) );
        prev_cells.addAll(m.water);
        prev_cells.addAll(m.park);
        prev_cells.addAll(m.road);
    }

    //score the move
    private Move scoreMove(ArrayList<Move> possibleMoves, boolean isFactory, Land land){
        Move bestMove = new Move(false);
        int highestPerimeter = Integer.MIN_VALUE;
        int highest_ij = Integer.MIN_VALUE;
        int cur_Perimeter = highestPerimeter;
        int cur_ij = highest_ij;

        for (Move m : possibleMoves) {
            //System.out.println("location: " + m.location.toString());
            cur_Perimeter = calcPerimeter(m,land);
            cur_ij = calcIJ(m,land);
            if(isFactory) cur_ij = -cur_ij;
            if (cur_ij > highest_ij){//|| (cur_ij == highest_ij &&
                //cur_Perimeter > highestPerimeter)) {
                bestMove = m;
            }
            /*
            if (cur_Perimeter > highestPerimeter || (cur_Perimeter == highestPerimeter &&
                cur_ij > highest_ij)) {
                bestMove = m;
            }
            */
            highestPerimeter = Math.max(highestPerimeter,cur_Perimeter);
            highest_ij = Math.max(highest_ij,cur_ij);
        }
        return bestMove;
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

    private Move search(Set<Cell> b, Land land, Cell p, int ri, Building request){
        Move m = new Move(false);
        Set<Cell> waters = new HashSet<Cell>();
        Set<Cell> parks = new HashSet<Cell>();
        
        
        if(request.type == Building.Type.RESIDENCE) {
            waters = findShortest(b,land,new HashSet<Cell>(),new HashSet<Cell>(),new HashSet<Cell>(),Type.WATER,4);
            if(waters == null) waters = new HashSet<Cell>();
            //parks = findShortest(b,land,waters,parks,new HashSet<Cell>(),Type.PARK,4);
            //if(parks == null) parks = new HashSet<Cell>();
        }
        
        
        Set<Cell> new_roads = findShortest(b,land,waters,parks,new HashSet<Cell>(),Type.ROAD,land.side);
        if(new_roads == null){
            m.accept = false;
            return m;
        }
        m.road = new_roads;
        m.accept = true;
        m.location = p;
        m.request = request;
        m.water = new HashSet<Cell>(waters);
        m.park = new HashSet<Cell>(parks);
        m.rotation = ri;
        return m;
        //checkOptimal(land, b, m, p, waters, parks, request, ri);
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
        List<Cell> starts = new ArrayList<Cell>();
        for(Cell b:building){
            //if building is direct neighbor to targets, return empty set;
            if(hitTarget(b,type,side)) return new HashSet<Cell>();
            Cell[] neighbors = b.neighbors();
            //perimeter_sort(neighbors,land,building);
            for(Cell start:neighbors){
                //else push all direct neighbor of building to queue
                if(land.unoccupied(start) && !building.contains(start) && !waters.contains(start)
                    && !parks.contains(start) && !roads.contains(start)){
                    starts.add(start);
                    //System.out.println("start");
                }
            }
        }

        Cell[] s = (Cell[])starts.toArray(new Cell[0]);
        perimeter_sort(s,land,building);
        for(Cell c:s) queue.offer(c);

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
                Cell[] neighbors = curr.neighbors();
                perimeter_sort(neighbors,land,building);
                for(Cell n : neighbors) { 
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

    //return how many of c's neighbors are occupied
    private int count_neighbor(Cell c, Land land, Set<Cell> b){
        int res = 0;
        for(Cell n:c.neighbors()){
            if(!land.unoccupied(n) || b.contains(n)) res++;
        }
        if(c.i == 0 || c.i == land.side) res++;
        if(c.j == 0 || c.j == land.side) res++;
        return res;
    }

    private int calcPerimeter(Move m, Land land){
        int sum = 0;
        Set<Cell> b = buildingToSet(m.request.rotations()[m.rotation], m.location);
        for(Cell building_cell : b){
            sum += count_neighbor(building_cell,land,new HashSet<Cell>());
        }

        for(Cell w:m.water){
            sum += count_neighbor(w,land,b);
        }

        for(Cell p:m.park){
            sum += count_neighbor(p,land,b);
        }
        return sum;
    }

    private int calcIJ(Move m, Land land){
        int sum = 0;
        for(Cell building_cell : buildingToSet(m.request.rotations()[m.rotation], m.location) ){
            sum += building_cell.i;
            sum += building_cell.j;
        }
        /*
        for(Cell w:m.water){
            sum += w.i;
            sum += w.j;
        }

        for(Cell p:m.park){
            sum += p.i;
            sum += p.j;
        }

        /*
        for(Cell r:m.road){
            sum += r.i;
            sum += r.j;
        }
        */
        return sum;
    }

    //convert cell b into absolute coordinates
    private Cell convert(Cell c,Cell p){
        return new Cell(c.i+p.i,c.j+p.j);
    }


    private void perimeter_sort(Cell[] neighbors,Land land,Set<Cell> b){
        Cell[] copy = Arrays.copyOf(neighbors,neighbors.length);
        boolean[] marked = new boolean[neighbors.length];
        for(int i = 0; i < copy.length; i++){
            int max = Integer.MIN_VALUE;
            int m = -1;
            for(int j = 0; j < copy.length; j++){
                if(marked[j]) continue;
                if(count_neighbor(copy[j],land,b) > max){
                    max = count_neighbor(copy[j],land,b);
                    m = j;
                }
            }
            marked[m] = true;
            neighbors[i] = copy[m];
        }
        /*
        for(Cell n:neighbors){
            System.out.print(count_neighbor(n,land,b) + ",");
        }
        System.out.println();
        System.out.println("========================");
        */
    }
}
