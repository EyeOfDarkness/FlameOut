package flame.unit.empathy;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import mindustry.entities.*;

public class TeleSwapMove extends RandomTeleport{
    boolean swaping = false;
    float actionTime = 0f;
    float teleportDelay = 0f;

    final static WeightedRandom<EmpathyAI> ai = new WeightedRandom<>();

    @Override
    float weight(){
        return swaping || !unit.activeAttack.canTeleport() ? -1 : 10f;
    }

    @Override
    void update(){
        updateTargeting();
        if(teleportTime <= 0f) updateMovement();
        
        int tpAmount = quickSwap ? 4 : 6;

        if(teleportTime > 0){
            float ltt = teleportTime;
            float dur = quickSwap ? 15f : 25f;
            teleportTime -= (Time.delta / dur);

            if(!tpSound){
                float pitch = (0.3f * 60f) / dur;
                FlameSounds.empathyTeleport.play(1f, pitch, 0f);

                tpSound = true;
            }

            if(teleportTime <= 0.5f && ltt > 0.5f){
                //unit.move(1f, 4, v.x, v.y);
                //unit.rotate(1f, Angles.angle(unit.x + v.x, unit.y + v.y, strongest.x(), strongest.y()), 360f);
                unit.move(1, 4, tx, ty);
                if(strongest != null){
                    unit.rotate(1f, Angles.angle(unit.x + tx, unit.y + ty, strongest.x(), strongest.y()), 360f);
                }
            }

            if(teleportTime <= 0f){
                tpSound = false;
            }

            if(teleportTime <= 0f && (teleport > tpAmount || !unit.activeAttack.canTeleport())){
                teleport = 0;
                unit.randAI(false, Mathf.chance(0.3f));
            }
            return;
        }
        if(actionTime > 0){
            actionTime -= Time.delta;
            if(actionTime <= 0f){
                unit.randAI(false, Mathf.chance(0.3f));
            }
        }
        if(teleportDelay > 0){
            teleportDelay -= Time.delta;
            if(teleportDelay <= 0f && teleportTime <= 0f && teleport <= tpAmount){                
                onSwap();
                teleportDelay = -1f;
            }
        }
    }

    boolean onSwap(){
        if(strongest == null) return false;
        if(teleportDelay > 0f) return true;
        
        swaping = true;

        ai.clear();
        for(EmpathyAI a : unit.attackAIs){
            if(a.teleSwapCompatible()){
                float use = 1f + (a.aiUsages / 2f);

                ai.add(a, a.weight() > 0 ? Math.max(a.weight() / use, 0.5f) : -1f);
            }
        }
        EmpathyAI i = ai.get();
        if(i == null){
            unit.randAI(false, Mathf.chance(0.3f));
            swaping = false;
            return false;
        }

        i.quickSwap = true;
        i.reset();
        unit.switchAI(i);

        teleportTime = 1f;
        actionTime = 3f * 60f;

        Vec2 v;
        if(strongest != null){
            v = Tmp.v1.trns(Mathf.random(360), unit.activeAttack.effectiveDistance() + (strongest instanceof Sized s ? s.hitSize() / 2 : 0f) + Mathf.range(40f)).add(strongest.x(), strongest.y()).sub(unit.x, unit.y);
        }else{
            v = Tmp.v1.trns(Mathf.random(360f), 150f);

        }
        tx = v.x;
        ty = v.y;

        teleport++;
        teleportDelay = 40f;
        tpSound = false;

        swaping = false;
        return true;
    }
}
