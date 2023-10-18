package flame.entities;

import arc.math.geom.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

public class DrawEntity extends BasicEntity implements Drawc{
    public float x, y;

    @Override
    public void draw(){
        //
    }

    @Override
    public float clipSize(){
        return Float.MAX_VALUE;
    }

    @Override
    public Floor floorOn(){
        Tile tile = tileOn();
        return tile == null || tile.block() != Blocks.air ? (Floor)Blocks.air : tile.floor();
    }

    @Override
    public Block blockOn(){
        Tile tile = tileOn();
        return tile == null ? Blocks.air : tile.block();
    }

    @Override
    public Building buildOn(){
        return Vars.world.buildWorld(x, y);
    }

    @Override
    public Tile tileOn(){
        return Vars.world.tileWorld(x, y);
    }

    @Override
    public boolean onSolid(){
        Tile tile = tileOn();
        return tile == null || tile.solid();
    }

    @Override
    public float getX(){
        return x;
    }

    @Override
    public float getY(){
        return y;
    }

    @Override
    public int tileX(){
        return World.toTile(x);
    }

    @Override
    public int tileY(){
        return World.toTile(y);
    }

    @Override
    public void set(Position p){
        set(p.getX(), p.getY());
    }

    @Override
    public void set(float x, float y){
        this.x = x;
        this.y = y;
    }

    @Override
    public void trns(Position p){
        trns(p.getX(), p.getY());
    }

    @Override
    public void trns(float x, float y){
        this.x += x;
        this.y += y;
    }

    @Override
    public void x(float v){
        x = v;
    }

    @Override
    public float x(){
        return x;
    }

    @Override
    public void y(float v){
        y = v;
    }

    @Override
    public float y(){
        return y;
    }

    @Override
    protected void addGroup(){
        super.addGroup();
        Groups.draw.add(this);
    }

    @Override
    protected void removeGroup(){
        super.removeGroup();
        Groups.draw.remove(this);
    }
}
