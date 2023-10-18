package flame.entities;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.effects.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;

public class ParriedEntity extends BasicEntity implements Poolable{
    float x, y, size;
    int targetId, pierceLast = -1;
    Posc target;
    Team team;

    public static void create(Posc target, Team team){
        ParriedEntity e = Pools.obtain(ParriedEntity.class, ParriedEntity::new);
        e.size = 200f;
        e.x = target.x();
        e.y = target.y();
        e.target = target;
        e.targetId = target.id();
        e.team = team;

        if(target instanceof Bullet b){
            if(!b.collided.isEmpty()) e.pierceLast = b.collided.get(b.collided.size - 1);
        }

        e.add();
    }

    void explode(){
        Damage.damage(team, x, y, size, 1000f, true);
        FlameFX.empathyParryExplosion.at(x, y, size);
        Rect r = Tmp.r1.setCentered(x, y, size * 2);
        Groups.bullet.intersect(r.x, r.y, r.width, r.height, b -> {
            if(b.team != team && b.within(x, y, size + b.hitSize / 2) && b.vel.len() > 1f){
                b.team = team;
                b.rotation(b.angleTo(x, y) + 180);
            }
        });
        Sounds.largeExplosion.at(x, y, Mathf.random(0.9f, 1.1f), 0.75f);
        Sounds.pulseBlast.at(x, y, Mathf.random(0.9f, 1.1f));
        size *= 0.9f;
    }

    @Override
    public void update(){
        if(!target.isAdded() || target.id() != targetId){
            explode();
            remove();
            return;
        }
        x = target.x();
        y = target.y();
        if(target instanceof Bullet b){
            if(!b.collided.isEmpty()){
                int last = b.collided.get(b.collided.size - 1);
                if(last != pierceLast){
                    explode();
                    pierceLast = last;
                }
            }
        }
    }

    @Override
    public void reset(){
        x = y = size = 0;
        targetId = 0;
        pierceLast = -1;
        target = null;
        team = null;
    }

    @Override
    protected void removeGroup(){
        super.removeGroup();
        Groups.queueFree(this);
    }
}
