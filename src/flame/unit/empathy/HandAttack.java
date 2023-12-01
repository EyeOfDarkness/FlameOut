package flame.unit.empathy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class HandAttack extends AttackAI{
    float time = 0f;
    boolean shot = false;
    float angle = 0f;

    float passiveTime = 0f;

    @Override
    float weight(){
        return passiveTime <= 0 ? (unit.useLethal() ? 600f + unit.extraLethalScore() : 2f) : -1f;
        //return 100000f;
    }

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    void reset(){
        time = 0f;
        shot = false;
    }

    @Override
    void init(){
        reset();
    }

    @Override
    void updatePassive(){
        if(passiveTime > 0) passiveTime -= Time.delta;
    }

    @Override
    void update(){
        if(unit.getTarget() != null && time <= 0f){
            angle = unit.angleTo(unit.getTarget());
        }

        time += Time.delta * (quickSwap ? 1.75f : 1f);

        if(unit.getTarget() != null && !shot){
            angle = Angles.moveToward(angle, unit.angleTo(unit.getTarget()), 9f);
        }
        if(time >= 85f && !shot){
            Vec2 v = Tmp.v1.trns(angle, 8000f).add(unit.x, unit.y);
            float vx = v.x, vy = v.y;
            float ux = unit.x, uy = unit.y;
            float hitsize = (unit.getTarget() != null && unit.getTarget() instanceof Sized s) ? Math.max(s.hitSize() / 2.5f, 30f) : 30f;
            Teamc target = unit.getTarget();

            Utils.hitLaser(unit.team, hitsize, ux, uy, vx, vy, null, h -> h == target, (h, x, y) -> {
                if(h instanceof Unit u){
                    EmpathyDamage.damageUnit(u, u.maxHealth + 1000f, true, () -> {
                        Carve.generate(ux, uy, angle, hitsize, u::draw);
                    });
                }else if(h instanceof Building b){
                    EmpathyDamage.damageBuilding(b, b.maxHealth + 1000f, true, () -> {
                        Carve.generate(ux, uy, angle, hitsize, b::draw);
                    });
                }
            });
            shot = true;
        }

        if(time >= 200f){
            reset();
            passiveTime = 15f * 60f;
            unit.randAI(true, unit.health < 50);
        }

        unit.move(5f, 1, 0f, 0f);
    }

    @Override
    boolean shouldDraw(){
        return unit.activeAttack == this;
    }

    @Override
    void draw(){
        float fin1 = Mathf.clamp(time / 70f);
        float fin2 = Mathf.clamp((time - 85f) / 5f);
        float fout = Mathf.clamp((200f - time) / 60f);

        float ang = angle + 90f * (1f - Interp.pow2.apply(fin1));
        Vec2 off = Tmp.v1.trns(ang, 90f).add(unit.x, unit.y);

        Draw.color(FlamePal.empathyAdd, fin1 * fout);
        Draw.blend(Blending.additive);

        TextureRegion r = EmpathyRegions.hand;
        float w = r.width * Draw.scl * 2f;
        float h = r.height * Draw.scl * 2f;

        Draw.rect(r, off.x, off.y, w, h, ang + Interp.pow2Out.apply(fin2) * 70f);

        Draw.color();
        Draw.blend();
    }
}
