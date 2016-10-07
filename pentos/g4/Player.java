package pentos.g4;
//reference g1 version 9/26
import java.util.*;
import java.io.*;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

public class Player implements pentos.sim.Player {

     //                        empty residence factory p/w side  road  firstroad  firstpark/first water
    private int[] factory_to = {0,   -10,       2,      0,   0,    1,         5};
    private int[] residence_to = {0,    5,        -10,    1,   0,    1,         8,                 10};
    private int[] waterpark_to = {0,   1,        0,      1,   -10,  -5};
    private Random gen = new Random();
    final int ITERATION_COUNT = 200;
    final int side = 50;
    private boolean[][] isDisconnected;
    private Set<Cell> road_cells = new HashSet<Cell>();
    static int count = 0;
    public void init() {
        isDisconnected = new boolean[side][side];
        //getParameters();
    }

    public void getParameters(){
        try{
         FileReader fileReader = new FileReader("/Users/luyang/Documents/eclipse_workspace/parameters");
         BufferedReader bufferedReader = new BufferedReader(fileReader);
         String line = null;
         line = bufferedReader.readLine();
         String[] arr = line.split(",");
         for (int i = 0; i < arr.length ;i++ ) {
             this.factory_to[i] = Integer.parseInt(arr[i]);
         }
         line = bufferedReader.readLine();
         arr = line.split(",");
         for (int i = 0; i < arr.length ;i++ ) {
             this.residence_to[i] = Integer.parseInt(arr[i]);
         }
         line = bufferedReader.readLine();
         arr = line.split(",");
         for (int i = 0; i < arr.length ;i++ ) {
             this.waterpark_to[i] = Integer.parseInt(arr[i]);
         }
         bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file");                  
            // Or we could just do this: 
            // ex.printStackTrace();
        }
    }

    public Move getBestMove(Building request, Land land) {
        Building[] rotations = request.rotations();
        int best_i = land.side + 1,
            best_j = land.side + 1;
        int best_perimeter = -1;
        Move best_move = null;
        
        //find first free location row by row
        if(request.type == Building.Type.RESIDENCE) {
            best_i = land.side + 1;
            best_j = land.side + 1;
            
            for (int i = 0; i < land.side; ++i) 
            for (int j = 0; j < land.side; ++j) {
                Cell p = new Cell(i,j);
                for (int ri = 0; ri < rotations.length; ++ri)
                if(land.buildable(rotations[ri], p)) {
                    Move temp = new Move(true, 
                                request, 
                                p, 
                                ri, 
                                new HashSet<Cell>(), 
                                new HashSet<Cell>(), 
                                new HashSet<Cell>());
                    boolean disconnected = false;
                    Set<Cell> shiftedCells = new HashSet<Cell>();
                    for (Cell x : temp.request.rotations()[temp.rotation]) {
                        shiftedCells.add(
                                new Cell(x.i+temp.location.i,
                                        x.j+temp.location.j));
                        disconnected |= 
                                isDisconnected[x.i + temp.location.i][x.j + temp.location.j];
                    }
                    
                    int perimeter = 0;
                    boolean first_resident_or_wp = true;
                    for(Cell x : shiftedCells) {
                        for(Cell y : x.neighbors()) {
                            if (shiftedCells.contains(y)) {
                                continue;
                            }
                            // if (land.land[y.i][y.j].isFactory()) {
                            //  perimeter+=2;
                            // }
                            Cell.Type t = land.getCellType(y.i,y.j);
                            if (t == Cell.Type.ROAD) {
                                perimeter+=1;
                            }
                            if (t == Cell.Type.RESIDENCE) {
                                perimeter+=2;
                            }
                            if (t == Cell.Type.WATER || t == Cell.Type.PARK) {
                                perimeter+=2;
                            }
                            if(first_resident_or_wp && (t == Cell.Type.WATER || t == Cell.Type.PARK || t == Cell.Type.RESIDENCE) ){
                                if (first_resident_or_wp) {
                                    first_resident_or_wp= false;
                                    perimeter+=5;
                                }
                            }
                        }
                        if(x.i == 0 || x.i == land.side - 1) perimeter+=2;
                        if(x.j == 0 || x.j == land.side - 1) perimeter+=2;                      
                    }
                    // builda road to connect this building to perimeter
                    
                    if(!disconnected && ((perimeter > best_perimeter)
                            ||(perimeter==best_perimeter && (i  + j) < best_i +  best_j)
                            || (perimeter==best_perimeter && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j))) {
                        Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
                        if(roadCells != null) {
                            best_move = temp;
                            best_i = i;
                            best_j = j;
                            best_perimeter = perimeter;
                        } else {
                            for(Cell x : shiftedCells) {
                                isDisconnected[x.i][x.j] = true;
                            }
                        }
                    }               
                }
            }
        //find closest free location to end
        } else if(request.type == Building.Type.FACTORY) {
            best_i = -1;
            best_j = -1;
            best_perimeter = -1;
            for (int i = land.side - 1; i >= 0; --i) 
            for ( int j = land.side - 1; j >= 0; --j) {
                Cell p = new Cell(i,j);
                for (int ri = 0; ri < rotations.length; ++ri)
                if(land.buildable(rotations[ri], p)) {
                    Move temp = new Move(true, 
                                request, 
                                p, 
                                ri, 
                                new HashSet<Cell>(), 
                                new HashSet<Cell>(), 
                                new HashSet<Cell>());
                    
                    boolean disconnected = false;
                    Set<Cell> shiftedCells = new HashSet<Cell>();
                    for (Cell x : temp.request.rotations()[temp.rotation]) {
                        shiftedCells.add(
                                new Cell(x.i+temp.location.i,
                                        x.j+temp.location.j));
                        disconnected |= 
                                isDisconnected[x.i + temp.location.i][x.j + temp.location.j];
                    }
                    
                    int perimeter = 0;
                    boolean first_factory = true;
                    for(Cell x : shiftedCells) {
                        for(Cell y : x.neighbors()) {
                            if (shiftedCells.contains(y)) {
                                continue;
                            }
                            Cell.Type t = land.getCellType(y.i,y.j);
                            if (t == Cell.Type.FACTORY) {
                                perimeter+=2;
                                if(first_factory){
                                    first_factory =false;
                                    perimeter+=10;
                                }
                            }
                            if (t == Cell.Type.ROAD) {
                                perimeter+=2;
                            }
                            // if (land.land[y.i][y.j].isResidence()) {
                            //  perimeter+=2;
                            // }
                            // if (land.land[y.i][y.j].isWater()) {
                            //  perimeter+=2;
                            // }
                            // if (land.land[y.i][y.j].isPark()) {
                            //  perimeter+=2;
                            // }

                        }
                        if(x.i == 0 || x.i == land.side - 1) perimeter+=2;
                        if(x.j == 0 || x.j == land.side - 1) perimeter+=2;                      
                    }
                    // builda road to connect this building to perimeter
                    if(!disconnected && (perimeter > best_perimeter 
                            || (perimeter==best_perimeter && (i  + j) >  best_i + best_j))
                            ||(perimeter==best_perimeter && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j)) {
                        Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
                        if(roadCells != null) {
                            best_move = temp;
                            best_i = i;
                            best_j = j;
                            best_perimeter = perimeter;
                        } else {
                            for(Cell x : shiftedCells) {
                                isDisconnected[x.i][x.j] = true;
                            }
                        }
                    }                   
                }
            }    
        }
        return best_move;
    }
    
    private static int num = 0;
    static Set<Cell> prev_water=null;
    public Move play(Building request, Land land) {
        Move best_move = getBestMove(request, land);
        if (best_move == null) {
            //System.out.println("no moves");
            return new Move(false);
        }
        // get coordinates of building placement (position plus local building cell coordinates)
        Set<Cell> shiftedCells = new HashSet<Cell>();
        for (Cell x : best_move.request.rotations()[best_move.rotation]) {
            shiftedCells.add(
                    new Cell(x.i+best_move.location.i,
                            x.j+best_move.location.j));
            //System.out.println(x.i + " " + x.j + " shifted to " + (x.i+best_move.location.i) + " " + (x.j + best_move.location.j));
        }
        // builda road to connect this building to perimeter
        Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
        if (roadCells != null) {
            best_move.road = roadCells;
            road_cells.addAll(roadCells);
            //int x = gen.nextInt();
            if(request.type == request.type.RESIDENCE) {
                
                Set<Cell> markedForConstruction = new HashSet<Cell>();
                markedForConstruction.addAll(roadCells);
                Set<Cell> best_park = new HashSet<Cell>();
                int best_perimeter = 100;
                int best_size = 100;
                for(int i = 0; i < ITERATION_COUNT; ++i) {
                    int perimeter = 0;
                    int size = 0;
                    Set<Cell> park_option = randomWalk(shiftedCells, markedForConstruction, land, 4, 1);
                    size = park_option.size()>0?park_option.size():110;
                    for(Cell x : park_option) {
                        for(Cell y: x.neighbors()) {
                            if (park_option.contains(y)) continue;
                            Cell.Type t = land.getCellType(y.i,y.j);
                            if (t == Cell.Type.FACTORY) {
                                perimeter+=2;
                            }
                            // if (land.land[y.i][y.j].isRoad()) {
                            //  perimeter+=2;
                            // }
                            if (t == Cell.Type.RESIDENCE) {
                                perimeter+=2;
                            }
                            if (t == Cell.Type.PARK || t == Cell.Type.WATER) {
                                perimeter+=2;
                            }
                        }
                    }
                    if(size < best_size || (size == best_size && perimeter < best_perimeter)) {
                        best_perimeter = perimeter;
                        best_park = park_option;
                        best_size = size;
                        //if(size != 4 && size != 0) System.out.println("hi");
                    }
                }

                best_move.park = best_park;
                markedForConstruction.addAll(best_move.park);
                
                best_perimeter = 100;
                best_park = new HashSet<Cell>();
                best_size = 100;
                for(int i = 0; i < ITERATION_COUNT; ++i) {
                    int perimeter = 0;
                    int size = 0;
                    Set<Cell> park_option = randomWalk(shiftedCells, markedForConstruction, land, 4, 2);
                    size = park_option.size()>0?park_option.size():110;
                    for(Cell x : park_option) {
                        for(Cell y: x.neighbors()) {
                            if (park_option.contains(y)) continue;
                            Cell.Type t = land.getCellType(y.i,y.j);
                            if (t == Cell.Type.FACTORY) {
                                perimeter+=2;
                            }
                            // if (land.land[y.i][y.j].isRoad()) {
                            //  perimeter+=2;
                            // }
                            if (t == Cell.Type.RESIDENCE) {
                                perimeter+=2;
                            }
                            if (t == Cell.Type.PARK || t == Cell.Type.WATER) {
                                perimeter+=2;
                            }
                        }
                    }
                    if(size < best_size || (size == best_size && perimeter < best_perimeter)) {
                        best_perimeter = perimeter;
                        best_park = park_option;
                        best_size = size;
                        //if(size != 4 && size != 0) System.out.println("hi");
                    }
                }
                best_move.water = best_park;
                prev_water = best_move.water;
            }
            return best_move;
        }
        else {
            return new Move(false);    
        }
    }
    
    // build shortest sequence of road cells to connect to a set of cells b
    private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
        Set<Cell> output = new HashSet<Cell>();
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
        // add border cells that don't have a road currently
        Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
        for(Cell x : b) {
            if(x.i==0 || x.i==land.side-1 || x.j==0 || x.j==land.side-1) return new HashSet<Cell>();
            for(Cell y : x.neighbors()) {
                if(road_cells.contains(y)) {
                    return new HashSet<Cell>();
                }
            }
        }
        for (int z=0; z<land.side; z++) {
            if (b.contains(new Cell(0,z)) || b.contains(new Cell(z,0)) || b.contains(new Cell(land.side-1,z)) || b.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
            return output;
            if (land.unoccupied(0,z))
            queue.add(new Cell(0,z,source));
            if (land.unoccupied(z,0))
            queue.add(new Cell(z,0,source));
            if (land.unoccupied(z,land.side-1))
            queue.add(new Cell(z,land.side-1,source));
            if (land.unoccupied(land.side-1,z))
            queue.add(new Cell(land.side-1,z,source));
        }
        // add cells adjacent to current road cells
        
        for (Cell p : road_cells) {
            for (Cell q : p.neighbors()) {
            if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q)) 
                queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
            }
        }   
        while (!queue.isEmpty()) {
            Cell p = queue.remove();
            checked[p.i][p.j] = true;
            for (Cell x : p.neighbors()) {      
            if (b.contains(x)) { // trace back through search tree to find path
                Cell tail = p;
                while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
                output.add(new Cell(tail.i,tail.j));
                tail = tail.previous;
                }
                if (!output.isEmpty())
                return output;
            }
            else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
                x.previous = p;
                checked[x.i][x.j] = true;
                queue.add(x);
            } 

            }
        }
        if (output.isEmpty() && queue.isEmpty())
            return null;
        else
            return output;
    }

    // walk n consecutive cells starting from a building. Used to build a random field or pond. 
    private Set<Cell> randomWalk(Set<Cell> b, Set<Cell> marked, Land land, int n, int type) {
        ArrayList<Cell> adjCells = new ArrayList<Cell>();
        Set<Cell> output = new HashSet<Cell>();
        for (Cell p : b) {
            for (Cell q : p.neighbors()) {
            if (land.isField(q) || land.isPond(q))
                return new HashSet<Cell>();
            if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q) 
                &&!(q.i == 0||q.i == land.side - 1||q.j == 0||q.j == land.side - 1))
                adjCells.add(q);
            }
        }
        if (adjCells.isEmpty())
            return new HashSet<Cell>();
        Cell tail = adjCells.get(gen.nextInt(adjCells.size()));
        for (int ii=0; ii<n; ii++) {
            ArrayList<Cell> walk_cells = new ArrayList<Cell>();
            for (Cell p : tail.neighbors()) {
                if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p)
                    &&!(p.i == 0||p.i == land.side - 1||p.j == 0||p.j == land.side - 1))
                    walk_cells.add(p);
                if((land.isField(p) && type == 1) || (land.isPond(p) && type == 2)) {
                //  System.out.println("hi");
                    output.add(tail);
                    return output;
                }
            }
            if (walk_cells.isEmpty()) {
            //return output; //if you want to build it anyway
            return new HashSet<Cell>();
            }
            output.add(tail);       
            tail = walk_cells.get(gen.nextInt(walk_cells.size()));
        }
        return output;
    }
}

