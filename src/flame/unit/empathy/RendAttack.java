package flame.unit.empathy;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class RendAttack extends AttackAI{
    Seq<RendLine> rends = new Seq<>();
    float reload = 0f;
    float moveDelay = 0f;
    float swapDelay = 0f;
    
    @Override
    float weight(){
        return reload <= 0f ? (unit.useLethal() ? 1000f + unit.extraLethalScore() : 2f) : -1f;
    }
    
    @Override
    boolean teleSwapCompatible(){
        return true;
    }
    
    @Override
    void reset(){
        reload = 0f;
    }

    @Override
    void updatePassive(){
        rends.removeAll(r -> {
            r.delay -= Time.delta;
            //r.time2 = Mathf.clamp(r.time2 + Time.delta / 6f);
            r.time2 = Math.min(1f, r.time2 + Time.delta / 6f);
            if(r.delay <= 0f){
                r.time += Time.delta;
            }
            if(r.time >= 6f && !r.slashed){
                hit(r.x1, r.y1, r.x2, r.y2);
                r.slashed = true;

                Vec2 n = Intersector.nearestSegmentPoint(r.x1, r.y1, r.x2, r.y2, Core.camera.position.x, Core.camera.position.y, Tmp.v4);
                FlameSounds.empathyRendSlash.at(n.x, n.y, Mathf.random(0.85f, 1.15f), 1.4f);
            }
            return r.time >= 12f;
        });
        reload -= Time.delta;
    }

    void hit(float x1, float y1, float x2, float y2){
        float minSize = 15f;
        Utils.hitLaser(unit.team, 3f, x1, y1, x2, y2, null, h -> false, (h, x, y) -> {
            FlameFX.empathyRendHit.at(x, y, Angles.angle(x1, y1, x2, y2));
            int rc = 0;
            if(h instanceof Sized size){
                for(RendLine r : rends){
                    Vec2 n = Intersector.nearestSegmentPoint(r.x1, r.y1, r.x2, r.y2, h.x(), h.y(), Tmp.v1);
                    if(n.within(h, size.hitSize() / 2f)){
                        rc++;
                    }
                }
            }

            if(h instanceof Unit u){
                float ele = u.elevation;

                float lethal = (rc * (u.maxHealth / 20f)) > u.health ? u.maxHealth + 100f : u.maxHealth / 20f;

                EmpathyDamage.damageUnit(u, Math.max(30000f, lethal), true, Math.max(u.hitSize, u.type.legLength) < minSize ? null : () -> {
                    float tz = ele > 0.5f ? (u.type.lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : (u.type.groundLayer + Mathf.clamp(u.hitSize / 4000f, 0f, 0.01f));
                    float shad = Mathf.clamp(ele, u.type.shadowElevation, 1f) * u.type.shadowElevationScl;

                    SpecialDeathEffects eff = SpecialDeathEffects.get(u.type);

                    if(!eff.canBeCut){
                        eff.cutAlt(u);
                        return;
                    }

                    CutBatch batch = FlameOut.cutBatch;
                    batch.explosionEffect = eff.explosionEffect != Fx.none ? eff.explosionEffect : null;
                    batch.cutHandler = c -> {
                        c.vx += u.vel.x;
                        c.vy += u.vel.y;
                        if(c.z >= tz - 0.01f){
                            c.shadowZ = shad;
                        }
                    };
                    batch.switchBatch(u::draw);
                });
            }else if(h instanceof Building b){
                float lethal = (rc * (b.maxHealth / 20f)) > b.health ? b.maxHealth + 100f : b.maxHealth / 20f;

                EmpathyDamage.damageBuilding(b, Math.max(30000f, lethal), true, b.block.size < 3 ? null : () -> {
                    SpecialDeathEffects eff = SpecialDeathEffects.get(b.block);
                    CutBatch batch = FlameOut.cutBatch;
                    batch.explosionEffect = eff.explosionEffect != Fx.none ? eff.explosionEffect : null;
                    batch.cutHandler = null;
                    batch.switchBatch(b::draw);
                });
            }
        });
        Severation.slash(x1, y1, x2, y2);
    }

    @Override
    void update(){
        if(unit.getTarget() != null && unit.within(unit.getTarget(), 660f) && reload <= 0f){
            Teamc target = unit.getTarget();
            float size = target instanceof Sized s ? s.hitSize() / 2 : 50f;
            
            if(Mathf.chance(0.6f)){
                for(int i = 0; i < 7; i++){
                    float delay = 45f + (i * 1.5f);
                    Vec2 vel = target instanceof Unit u ? u.vel : Tmp.v3.setZero();

                    float ang = Mathf.random(360f);
                    float rlen = Mathf.random(70f, 120f);
                    Vec2 v1 = Tmp.v1.trns(ang, (size * 2.5f) + rlen).add(target).add(vel.x * delay, vel.y * delay);
                    Vec2 v2 = Tmp.v2.trns(ang + 180f + Mathf.range(Angles.angle(target.dst(v1), size / 1.1f)), ((size * 2.5f) + rlen) * 2f).add(v1);

                    RendLine r = new RendLine();
                    r.x1 = v1.x;
                    r.y1 = v1.y;
                    r.x2 = v2.x;
                    r.y2 = v2.y;
                    //r.delay = Mathf.random(15f, 70f);
                    r.delay = delay;
                    r.time2 = Mathf.random(-5f, 0f);
                    rends.add(r);
                    //Log.info(r);

                    float delay2 = i * 3 + Mathf.random(1f);
                    float ax = (v1.x + v2.x) / 2;
                    float ay = (v1.y + v2.y) / 2;

                    Time.run(delay2, () -> FlameSounds.empathyRendSwing.at(ax, ay, Mathf.random(0.7f, 1.2f)));
                }
                //Tmp.v1.set(unit.x, unit.y).sub(unit.getTarget()).nor().scl(-(100f + size * 2f));
                Tmp.v1.set(target).sub(unit).nor().scl(100f + size * 2f);
                float mx = (target.x() - unit.x) + Tmp.v1.x;
                float my = (target.y() - unit.y) + Tmp.v1.y;
                unit.move(5f, 1, mx, my);
                FlameFX.empathyRend.at(target.x(), target.y());

                Vec2 sp = Intersector.nearestSegmentPoint(unit.x, unit.y, mx + unit.x, my + unit.y, Core.camera.position.x, Core.camera.position.y, Tmp.v3);
                FlameSounds.empathyDash.at(sp.x, sp.y, 1f, 1.2f);

                //unit.randAI(true, false);
                swapDelay = 40f;
            }else{
                for(int i = 0; i < 14; i++){
                    float range = Mathf.random(660f, 820f);
                    float delay = 70f + (i);
                    float ang = Mathf.random(360f);
                    float ang2 = ang + 180f + Mathf.range(90f);
                    Vec2 v1 = Tmp.v1.trns(ang, range).add(unit);
                    Vec2 v2 = Tmp.v2.trns(ang2, range).add(unit);

                    RendLine r = new RendLine();
                    r.x1 = v1.x;
                    r.y1 = v1.y;
                    r.x2 = v2.x;
                    r.y2 = v2.y;
                    r.delay = delay;
                    r.time2 = -(10f - i);
                    rends.add(r);

                    float delay2 = i * 3 + Mathf.random(1f);
                    float ax = unit.x;
                    float ay = unit.y;

                    Time.run(delay2, () -> FlameSounds.empathyRendSwing.at(ax, ay, Mathf.random(0.7f, 1.2f)));
                }
                float sng = Mathf.random(360f);
                float len = 0f;
                if(target instanceof Unit u){
                    sng = u.vel.angle();
                    len = u.vel.len() * 83f * 2f;
                }
                Vec2 v1 = Tmp.v1.trns(sng + 180f, size * 2f).add(target);
                Vec2 v2 = Tmp.v2.trns(sng, len + size * 4f).add(v1);
                RendLine r = new RendLine();
                r.x1 = v1.x;
                r.y1 = v1.y;
                r.x2 = v2.x;
                r.y2 = v2.y;
                r.delay = 83f;
                r.time2 = -6f;
                rends.add(r);
                
                FlameFX.empathyRend.at(unit.x, unit.y);
                moveDelay = 70f;
                swapDelay = 70f;
            }

            reload = 60f * 4;
        }
        if(moveDelay > 0){
            unit.move(5f, 1, 0f, 0f);
            moveDelay -= Time.delta;
        }
        if(swapDelay > 0){
            swapDelay -= Time.delta;
            if(swapDelay <= 0f){
                unit.randAI(true, unit.health < 50);
            }
        }
    }

    @Override
    void draw(){
        Draw.color();
        float z = Draw.z();
        Draw.z(Layer.flyingUnit + 2f);
        for(RendLine r : rends){
            float fin = Mathf.clamp(r.time2) * Mathf.clamp(1f - (r.time - 6f) / 6f);
            float fin2 = Interp.pow2Out.apply(Mathf.clamp(r.time / 12f));

            Lines.stroke(fin * 1.25f);
            Lines.line(r.x1, r.y1, r.x2, r.y2);
            if(fin2 > 0.0001f){
                Vec2 p = Tmp.v1.set(r.x1, r.y1).lerp(r.x2, r.y2, fin2);
                //float l = Mathf.dst(r.x1, r.y1, r.x2, r.y2) / 2f;
                float l = Mathf.dst(r.x1, r.y1, r.x2, r.y2);
                GraphicUtils.diamond(p.x, p.y, fin * Mathf.clamp(r.time / 5f) * 4f, l * Mathf.slope(fin2), Angles.angle(r.x1, r.y1, r.x2, r.y2));
            }
        }
        Draw.z(z);
    }

    @Override
    boolean shouldDraw(){
        return !rends.isEmpty();
    }

    static class RendLine{
        float x1, y1, x2, y2;
        float delay = 0f;
        float time = 0f, time2 = 0f;
        boolean slashed = false;
    }
}
