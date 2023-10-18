package flame.unit.empathy;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class RandomTeleport extends FollowStrongest{
    int teleport = 0;
    float delay = 0f;
    float teleportTime = 0f;

    float tx, ty;

    boolean tpSound;

    @Override
    float weight(){
        return unit.activeMovement == this || !unit.activeAttack.canTeleport() ? -1 : super.weight() * 10;
    }

    @Override
    boolean bulletHellOverride(){
        return teleportTime <= 0f;
    }

    @Override
    void update(){
        updateTargeting();

        if(teleportTime > 0){
            float ltt = teleportTime;
            float dur = quickSwap ? 4f : 16f;
            teleportTime -= (Time.delta / dur);

            if(!tpSound){
                float pitch = (0.3f * 60f) / dur;
                FlameSounds.empathyTeleport.play(1f, Math.min(pitch, 1.25f), 0f);

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
            if(teleportTime <= 0f && (teleport <= 0 || !unit.activeAttack.canTeleport())){
                unit.randAI(false, Mathf.chance(0.3f));
            }
            return;
        }else if(!unit.activeAttack.canTeleport()){
            unit.randAI(false, Mathf.chance(0.3f));
            return;
        }

        if(unit.activeAttack.overrideDraw()){
            teleportTime = 0f;
            return;
        }

        if(teleport <= 0 && strongest != null && unit.activeAttack.canTeleport()){
            teleport = Mathf.random(5, 8);
        }
        if(teleport > 0){
            if(delay <= 0f){
                Vec2 v;
                if(strongest != null){
                    v = Tmp.v1.trns(Mathf.random(360), unit.activeAttack.effectiveDistance() + (strongest instanceof Sized s ? s.hitSize() / 2 : 0f) + Mathf.range(40f)).add(strongest.x(), strongest.y()).sub(unit.x, unit.y);
                    //unit.move(1f, 4, v.x, v.y);
                    //unit.rotate(1f, Angles.angle(unit.x + v.x, unit.y + v.y, strongest.x(), strongest.y()), 360f);

                    //FlameFX.endFlash.at(unit.x, unit.y, 1f);
                    //FlameFX.endFlash.at(unit.x, unit.y, 1f);
                }else{
                    v = Tmp.v1.trns(Mathf.random(360f), 150f);
                    //unit.move(1f, 4, v.x, v.y);

                }
                tx = v.x;
                ty = v.y;
                teleportTime = 1f;

                delay = quickSwap ? 0f : 3f;
                teleport--;
                tpSound = false;
            }
            delay -= Time.delta;
        }
    }

    @Override
    void draw(){
        float fin = Mathf.slope(teleportTime);
        float z = Layer.flyingUnit;
        //Math.pow(Mathf.sinDeg(Math.pow(x, 0.5) * 270) + 1, 1.5);
        //((Math.pow(1 - Mathf.sinDeg(Math.pow(x, 0.8) * 180), 1.5) * (1 + Math.pow(x, 1.5) * 5)) + 0.1) / 1.1;

        float wBase = 8f;
        float wScl = Mathf.pow(Mathf.sinDeg(Mathf.pow(fin, 0.5f) * 270) * wBase + wBase, 1.5f) / Mathf.pow(wBase, 1.5f);
        float hScl = ((Mathf.pow(1f - Mathf.sinDeg(Mathf.pow(fin, 0.8f) * 180), 1.5f) * (1 + Mathf.pow(fin, 1.5f) * 24f)) + 0.25f) / 1.25f;

        TextureRegion r = unit.type.region;

        Draw.z(Math.min(Layer.darkness, z - 1f));
        //unit.type.drawShadow(unit);
        Draw.color(Pal.shadow);
        float hx = unit.x + UnitType.shadowTX, hy = unit.y + UnitType.shadowTY;

        Draw.rect(r, hx, hy, r.width * Draw.scl * hScl, r.height * Draw.scl * wScl, unit.rotation - 90f);
        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));
        unit.type.drawSoftShadow(unit, Mathf.pow(1f - fin, 3));

        Draw.z(z);
        Draw.color();
        Draw.rect(r, unit.x, unit.y, r.width * Draw.scl * hScl, r.height * Draw.scl * wScl, unit.rotation - 90f);

    }

    @Override
    boolean updateAttackAI(){
        return teleportTime <= 0f;
    }

    @Override
    boolean shouldDraw(){
        return unit.activeMovement == this && teleportTime > 0;
    }

    @Override
    boolean overrideDraw(){
        return teleportTime > 0;
    }
}
