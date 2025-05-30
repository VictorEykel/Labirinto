package models;

import maze.Maze;

public class Land extends Entity{

    public int index;

    public Land(int position_y, int position_x, int index, Maze maze) {
        super(position_y, position_x, maze);
        this.index = index;
    }

}
