package flame.unit.empathy;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.bullets.*;
import mindustry.gen.*;

public class SprayAttack extends AttackAI{
    float reload = 0f, reload2 = 0f;
    float attackTime = 0f;
    float angle = 0f;
    int endChance = 1;

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    void update(){
        if(unit.getTarget() != null && unit.within(unit.getTarget(), 800f)){
            angle = unit.angleTo(unit.getTarget());
            if(reload <= 0f){
                attackTime = Mathf.random(2.75f * 60f, 3.5f * 60f) * (quickSwap ? 0.4f : 1f);
                reload = attackTime / 2;
            }
        }

        if(attackTime > 0){
            if(reload2 <= 0f){
                float ang = angle + 180 + Mathf.range(30f);
                Vec2 v = Tmp.v1.trns(ang, 20f).add(unit);
                Bullet b = FlameBullets.tracker.create(unit, unit.team, v.x, v.y, ang, Mathf.random(0.6f, 1.1f));
                b.data = unit.getTarget();
                b.fdata = Mathf.random(30f, 60f);

                Sounds.missile.at(v.x, v.y, 2.5f);

                reload2 = unit.isDecoy() ? 4f : 2f;
            }
            reload2 -= Time.delta;
            attackTime -= Time.delta * (unit.useLethal() ? 5f : 1f);
            if(attackTime <= 0f){
                if(Mathf.chance(0.9f / endChance) && !quickSwap && unit.getTarget() != null && !unit.useLethal()){
                    endChance += 3;
                }else{
                    unit.randAI(true, unit.health < 50f);
                    endChance = 1;
                }
            }
        }else{
            reload -= Time.delta;
        }
    }

    @Override
    void reset(){
        reload = 0f;
        reload2 = 0f;
        attackTime = 0f;
        endChance = 1;
    }

    @Override
    void end(){
        reset();
    }
}
