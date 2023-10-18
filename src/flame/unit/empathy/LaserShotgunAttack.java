package flame.unit.empathy;

import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import mindustry.content.*;
import mindustry.gen.*;

public class LaserShotgunAttack extends AttackAI{
    float reload = 0f, reload2 = 0f;
    float targetAngle = 0f;
    int shots = 0;

    @Override
    void reset(){
        reload = 0f;
        reload2 = 0f;
    }

    @Override
    void endOnce(){
        reset();
    }

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    float effectiveDistance(){
        return 100f;
    }

    @Override
    void update(){
        if(unit.getTarget() != null && unit.within(unit.getTarget(), 600f)){
            targetAngle = unit.angleTo(unit.getTarget());
            if(reload <= 0f){
                shots = 5;
                reload = 2f * 60f;
            }
            unit.rotate(5f, targetAngle, 8f);
        }
        if(shots > 0){
            if(reload2 <= 0f){
                for(int i = 0; i < 7; i++){
                    float f = ((i / 6f) - 0.5f) * 40f;
                    shoot(targetAngle + f);
                }
                shots--;
                reload2 = 8f;

                Sounds.bolt.at(unit.x, unit.y, 2f);

                if(shots <= 0){
                    unit.randAI(true, unit.health < 50f);
                }
            }
        }else{
            reload -= Time.delta;
        }
        reload2 -= Time.delta;
    }

    void shoot(float angle){
        Vec2 v = Tmp.v1.trns(angle, 700f).add(unit);
        float dam = 700f;

        float len = Utils.hitLaser(unit.team, 4f, unit.x, unit.y, v.x, v.y, null, h -> true, (h, x, y) -> {
            Fx.hitBulletColor.at(x, y, angle);

            boolean lethal = h.maxHealth() < dam * 3f;
            float tdam = Math.max(dam, h.maxHealth() / 600f);
            if(h instanceof Unit u){
                EmpathyDamage.damageUnit(u, tdam, lethal, null);
            }else if(h instanceof Building build){
                EmpathyDamage.damageBuilding(build, tdam, true, null);
            }
        });
        FlameFX.empathyShotgun.at(unit.x, unit.y, angle, len);
    }
}
