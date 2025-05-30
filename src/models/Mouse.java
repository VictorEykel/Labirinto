package models;

import maze.Maze;

public class Mouse extends Entity{

    public Mouse(int position_x, int position_y, Maze maze) {
        super(position_x, position_y,maze);
    }

    public Entity getDirectionY(int args){
        int x = this.position_x;
        int y = this.position_y + args;
        return this.maze.getEntity(x,y);
    }

    public Entity getDirectionX(int args){
        int x = this.position_x + args;
        int y = this.position_y;
        return this.maze.getEntity(x,y);
    }
}
