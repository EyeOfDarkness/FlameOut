package flame.unit.empathy;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.bullets.*;
import flame.entities.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class PinAttack extends AttackAI{
    float reloadTime = 0f;
    float barrageTime = 0f, spawnTimer = 0f;
    float bulletHellTime = 120f;
    int barrages = 0;

    float targetX = 0f;
    float targetY = 0f;

    @Override
    void reset(){
        reloadTime = 0f;
        barrageTime = 0f;
        spawnTimer = 0f;
        bulletHellTime = 0f;
        barrages = 0;
    }

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    void update(){
        Teamc target = unit.getTarget();
        if(target != null){
            targetX = target.getX();
            targetY = target.getY();

            if(unit.within(target, 620f)){
                if(reloadTime <= 0f){
                    reloadTime = Mathf.random(60f, 120f);
                    barrageTime = Mathf.random(15f, 60f);
                }
                if(bulletHellTime <= 0f){
                    bulletHellTime = 5f * 60f;
                    EmpathyBulletSpawner.create(unit, unit.x, unit.y, 50f, 2.5f * 60f, e -> {
                        if(e.datas[0] >= 5f){
                            for(int i = 0; i < 10; i++){
                                float ang = (360f / 10f) * i + Mathf.sinDeg(e.datas[1]) * 90f;
                                FlameBullets.pin.create(unit, unit.team, e.x, e.y, ang);
                            }
                            Sounds.missile.at(e.x, e.y, 2f);
                            e.datas[1] += 10f;
                            e.datas[0] = 0f;
                        }
                        e.datas[0] += Time.delta;
                    });
                }
                unit.rotate(2f, unit.angleTo(target), 15f);
            }
        }

        spawnTimer = Math.max(0f, spawnTimer - Time.delta);
        if(barrageTime > 0){
            if(spawnTimer <= 0f){
                Vec2 v = Tmp.v1.trns(unit.rotation, -40f, Mathf.range(40f)).add(unit);

                float vx = 0f;
                float vy = 0f;
                if(target instanceof Hitboxc h){
                    vx = h.deltaX();
                    vy = h.deltaY();
                }

                Vec2 in = Tmp.v2.set(targetX, targetY);
                if(Mathf.chance(0.5f)) in = Predict.intercept(v.x, v.y, targetX, targetY, vx, vy, 10f);

                FlameBullets.pin.create(unit, unit.team, v.x, v.y, v.angleTo(in));
                Sounds.missile.at(v.x, v.y, 2f);

                spawnTimer = 3f;
            }
            barrageTime -= Time.delta;
            if(barrageTime <= 0f){
                barrages++;
                if(barrages >= 4 || quickSwap || unit.useLethal()){
                    barrages = 0;
                    //reloadTime = 0f;
                    unit.randAI(true, unit.health < 50f);
                }
            }
        }else{
            reloadTime = Math.max(0f, reloadTime - Time.delta);
        }
    }

    @Override
    void updatePassive(){
        bulletHellTime -= Time.delta;
    }

    @Override
    void endOnce(){
        reset();
    }
}
