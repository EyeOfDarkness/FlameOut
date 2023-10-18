package flame.unit.empathy;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;

public class OrbitMove extends FollowStrongest{
    float dst = 400f;
    float direction = 12f;

    float randTime = 0f;

    float switchTime = 0f;

    @Override
    void update(){
        updateTargeting();

        if(strongest != null){
            if(randTime <= 0f){
                dst = unit.activeAttack.effectiveDistance() + Mathf.range(60f);
                direction = Mathf.range(25f, 60f);
                randTime = Mathf.random(30f, 60f);
                if(switchTime <= 0f){
                    switchTime = Mathf.random(4f * 60, 6f * 60) * (quickSwap ? 1f : 0.4f);
                }
            }
            randTime -= Time.delta;

            float ang = Angles.angle(strongest.x(), strongest.y(), unit.x, unit.y);

            Vec2 v = Tmp.v1.trns(ang, (dst - Math.abs(direction)) + (strongest instanceof Sized s ? s.hitSize() / 2 : 0f)).add(strongest.x(), strongest.y()).sub(unit.x, unit.y).scl(0.25f);
            Vec2 v2 = Tmp.v2.trns(ang, 0f, direction).add(v);

            float newAng = Angles.angle(unit.x + v2.x, unit.y + v2.y, strongest.x(), strongest.y());

            unit.move(1f, 2, v2.x, v2.y);
            unit.rotate(1f, newAng, 30f);
        }
        if(switchTime > 0){
            switchTime -= Time.delta;
            if(switchTime <= 0f){
                unit.randAI(false, Mathf.chance(0.3f));
            }
        }
    }
}
