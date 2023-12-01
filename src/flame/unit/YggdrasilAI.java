package flame.unit;

import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class YggdrasilAI extends AIController{
    boolean canMoveToward = false;
    float moveScanTime = 0f;

    static Vec2 mv = new Vec2();

    @Override
    public void updateTargeting(){
        boolean ret = retarget();

        if(ret){
            target = findMainTarget(unit.x, unit.y, unit.range() * 2, true, true);
            //Log.info(unit.range());
        }

        if(invalid(target)){
            target = null;
        }

        if(target != null){
            unit.isShooting = true;

            float dx = 0f;
            float dy = 0f;
            if(target instanceof Hitboxc h){
                dx += h.deltaX();
                dy += h.deltaY();
            }

            Vec2 to = Predict.intercept(unit.x, unit.y, target.x(), target.y(), dx, dy, 20f);

            unit.aimX = to.x;
            unit.aimY = to.y;
        }else{
            unit.isShooting = false;
        }
        
        //Log.info(target);
    }

    @Override
    public void updateMovement(){
        boolean move = true;
        Building core = unit.closestEnemyCore();

        if(!(target instanceof CoreBuild) && target != null){
            move = false;
        }
        moveScanTime -= Time.delta;

        if(!move){
            if(moveScanTime <= 0f){
                canMoveToward = true;
                World.raycastEachWorld(unit.x, unit.y, target.x(), target.y(), (x, y) -> {
                    Tile tile = world.tile(x, y);

                    boolean f = tile == null || tile.legSolid() || tile.dangerous();
                    if(f){
                        canMoveToward = false;
                    }

                    return f;
                });
                moveScanTime = 5f;
            }
            
            boolean near = unit.within(target, unit.range() / 3f);

            if(canMoveToward && !unit.within(target, unit.range()) && !near){
                mv.set(target.x(), target.y()).sub(unit.x, unit.y).limit(unit.type.speed);
                unit.move(mv);
            }
            if(canMoveToward && near){
                mv.set(unit.x, unit.y).sub(target.x(), target.y()).limit(unit.type.speed);
                unit.move(mv);
            }

            if(canMoveToward){
                faceTarget();
                return;
            }else{
                move = true;
            }
        }

        if(core != null && unit.within(core, unit.range() / 1.3f + core.block.size * tilesize / 2f)){
            target = core;
        }

        if((core == null || !unit.within(core, unit.type.range * 0.5f))){
            if(state.rules.waves && unit.team == state.rules.defaultTeam){
                Tile spawner = getClosestSpawner();
                if(spawner != null && unit.within(spawner, state.rules.dropZoneRadius + 120f)) move = false;
                if(spawner == null && core == null) move = false;
            }

            if(core == null && (!state.rules.waves || getClosestSpawner() == null)){
                move = false;
            }

            if(move) pathfind(Pathfinder.fieldCore);
        }

        faceTarget();
    }

    @Override
    public void faceTarget(){
        if(target != null){
            unit.lookAt(target);
        }else if(unit.moving()){
            unit.lookAt(unit.vel().angle());
        }
    }
}
