package flame.unit.empathy;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class BlastAttack extends AttackAI{
    float delay = 0f, passiveTime;
    float tx, ty;
    boolean sound = false, hadTarget;

    @Override
    float weight(){
        //return 100000f;
        return passiveTime <= 0 ? (unit.useLethal() ? 400f + unit.extraLethalScore() : 2f) : -1f;
    }

    @Override
    void reset(){
        delay = 0f;
        sound = false;
        hadTarget = false;
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
        if(hadTarget){
            if(unit.getTarget() != null){
                tx = unit.getTarget().x();
                ty = unit.getTarget().y();

                unit.rotate(5f, unit.angleTo(unit.getTarget()), 5f);
                unit.move(7f, 1, 0f, 0f);
            }

            delay += Time.delta;

            if(!sound){
                sound = true;
                FlameSounds.empathyCharge.play(1f);
            }

            if(delay >= 20f){
                hit();

                reset();
                passiveTime = 10f * 60f;
                unit.randAI(true, unit.health < 50);
            }
        }
        if(unit.getTarget() != null && !hadTarget){
            hadTarget = true;
            tx = unit.getTarget().x();
            ty = unit.getTarget().y();
        }
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    void hit(){
        float size = unit.getTarget() instanceof Sized s ? Math.max(60f, s.hitSize() / 2f) : 60f;
        float size2 = size / 2f;

        final float tx = this.tx;
        final float ty = this.ty;
        final float speed = 12f;
        Rect r = Utils.r.setCentered(tx, ty, size * 2f);

        FlameSounds.empathyBlast.at(tx, ty, 1f, 4f);
        FlameFX.empathyBlast.at(tx, ty, size);
        Groups.unit.intersect(r.x, r.y, r.width, r.height, u -> {
            if(u.within(tx, ty, size + u.hitSize / 2f)){
                float dam = u.within(tx, ty, size) ? u.maxHealth + 1000f : Mathf.clamp(1f - (u.dst(tx, ty) - size) / (u.hitSize / 2f)) * u.health;
                EmpathyDamage.damageUnit(u, dam, u.within(tx, ty, size), () -> FlameOut.vaporBatch.switchBatch(u::draw, (x, y, w, h, rot) -> true, (e, within) -> {
                    if(e.within(tx, ty, size2)){
                        //float dst = e.dst(tx, ty);
                        float dx = ((e.x - tx) / size2) * speed;
                        float dy = ((e.y - ty) / size2) * speed;
                        e.disintegrating = true;
                        e.drag = 0.012f;
                        e.vx = dx;
                        e.vy = dy;
                        e.vr = Mathf.range(speed);
                    }else{
                        float dst = e.dst(tx, ty);
                        float scl = speed / ((dst - size2) / 40f + 1f);

                        float dx = ((e.x - tx) / dst) * scl;
                        float dy = ((e.y - ty) / dst) * scl;

                        e.drag = 0.015f;
                        e.vx = dx;
                        e.vy = dy;
                        e.vr = Mathf.range(scl);
                    }
                    e.lifetime = 50f;
                }));
            }
        });
        r.setCentered(tx, ty, size * 2f);
        for(TeamData data : Vars.state.teams.present){
            if(data.team != unit.team && data.buildingTree != null){
                data.buildingTree.intersect(r, b -> {
                    if(b.within(tx, ty, size + b.hitSize() / 2f)){
                        float dam = b.within(tx, ty, size) ? b.maxHealth + 1000f : Mathf.clamp(1f - (b.dst(tx, ty) - size) / (b.hitSize() / 2f)) * b.health;
                        EmpathyDamage.damageBuilding(b, dam, b.within(tx, ty, size), () -> FlameOut.vaporBatch.switchBatch(b::draw, (x, y, w, h, rot) -> true, (e, within) -> {
                            if(e.within(tx, ty, size2)){
                                //float dst = e.dst(tx, ty);
                                float dx = ((e.x - tx) / size2) * speed;
                                float dy = ((e.y - ty) / size2) * speed;
                                e.disintegrating = true;
                                e.drag = 0.012f;
                                e.vx = dx;
                                e.vy = dy;
                                e.vr = Mathf.range(speed);
                            }else{
                                float dst = e.dst(tx, ty);
                                float scl = speed / (Mathf.pow((dst - size2) / 10f, 2f) + 1f);

                                float dx = ((e.x - tx) / dst) * scl;
                                float dy = ((e.y - ty) / dst) * scl;

                                e.drag = 0.015f;
                                e.vx = dx;
                                e.vy = dy;
                                e.vr = Mathf.range(scl);
                            }
                            e.lifetime = 50f;
                        }));
                    }
                });
            }
        }
    }

    @Override
    boolean shouldDraw(){
        return unit.activeAttack == this;
    }

    @Override
    void draw(){
        float z = Draw.z();
        float fin = Mathf.clamp(delay / 20f);
        float fout = 1f - Mathf.curve(Mathf.clamp(delay / 20f), 0.7f, 1f);
        Draw.z(Layer.flyingUnit + 0.5f);

        Draw.color();
        float scl = 200f * Interp.pow5Out.apply(fin);
        float ang = 180f * Interp.pow3Out.apply(fin);
        for(int i = 0; i < 4; i++){
            float a = (360f / 4f) * i + ang;

            Drawf.tri(unit.x, unit.y, scl * 0.1f * fout, scl, a);
        }

        Draw.reset();
        Draw.z(z);
    }
}
