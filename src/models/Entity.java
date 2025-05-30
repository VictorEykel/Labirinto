package models;

import maze.Maze;

public abstract class Entity {

    public int position_y;
    public int position_x;
    public Maze maze;

    public Entity(int position_y, int position_x, Maze maze) {
        this.position_y = position_y;
        this.position_x = position_x;
        this.maze = maze;
    }

    public Integer orden(){
        return (this.position_y + this.position_x );
    }

    public boolean isEnded() {
        return this instanceof End;
    }

    @Override
    public String toString() {
        return "x="+this.position_x + ",y=" + this.position_y;
    }
}
