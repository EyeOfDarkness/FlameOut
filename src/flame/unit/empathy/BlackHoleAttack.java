package flame.unit.empathy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class BlackHoleAttack extends AttackAI{
    float splitTime = 0f;
    float blackholeTime = 0f;
    boolean attacking;
    boolean openSound = false;

    float waitTime = 0f;

    static final float blackHoleDuration = 8f * 60f;

    BlackHoleAttack(){
        super();
        addSoundConstant(FlameSounds.empathyBlackHole);
    }

    @Override
    float weight(){
        return unit.useLethal() ? 250f + unit.extraLethalScore() / 15f : -1;
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    void update(){
        if(!attacking){
            if(waitTime <= 0f){
                splitTime = Mathf.clamp(splitTime + Time.delta / 30f);
                if(!openSound){
                    openSound = true;
                    FlameSounds.empathyRendSwing.at(unit.x, unit.y);
                }
            }else{
                waitTime -= Time.delta;
            }
            if(splitTime >= 1f){
                attacking = true;
            }
        }else{
            blackholeTime += Time.delta;
            float f = Mathf.clamp((blackHoleDuration - blackholeTime) / 120f) * Interp.pow2Out.apply(Mathf.clamp(blackholeTime / 60f)) * (Mathf.clamp(blackholeTime / blackHoleDuration) * 0.1f + 1f);
            float f2 = Mathf.clamp((blackHoleDuration - blackholeTime) / 120f) * Interp.pow2Out.apply(Mathf.clamp((blackholeTime - 30f) / 120f)) * (Mathf.clamp(blackholeTime / blackHoleDuration) * 0.1f + 1f);

            Effect.shake(30f * f2, 30f * f2, unit);
            sounds.get(0).play(unit.x, unit.y, f2, true);

            Tmp.r1.setCentered(unit.x, unit.y, 1200f * 2, 1200f * 2);
            Groups.unit.intersect(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, u -> {
                if(u.team != unit.team && unit.within(u, 1200f + u.hitSize / 2)){
                    float dst = 1f - (unit.dst(u) / (1200f + u.hitSize / 2));
                    Tmp.v1.set(unit).sub(u).nor().scl(Mathf.pow(dst, 3) * 10f * f);
                    u.vel.add(Tmp.v1);
                    float er = 0f;
                    for(Weapon w : u.type.weapons){
                        //w.shootOnDeath || 
                        if(w.bullet.killShooter){
                            //er = Math.max(er, Math.max(w.bullet.splashDamageRadius / 2, w.bullet.fragBullets > 0 ? w.bullet.fragBullet.range / 2 : 0f));
                            er = u.type.maxRange;
                            break;
                        }
                    }
                    if(u instanceof TimedKillc || u.controller() instanceof MissileAI){
                        er = u.type.maxRange;
                    }
                    //r += er;
                    if(Intersector.nearestSegmentPoint(u.lastX, u.lastY, u.vel.x * Time.delta + u.x, u.vel.y * Time.delta + u.y, unit.x, unit.y, Tmp.v2).within(unit.x, unit.y, 15f + er)){
                        if(!(u instanceof TimedKillc)){
                            EmpathyDamage.damageUnit(u, Math.max(1000000f, u.maxHealth / 5f), true, () -> SpecialDeathEffects.get(u.type).deathUnit(u, unit.x, unit.y, u.vel.angle(), e -> {
                                float dx = (e.x - u.x) / 45f;
                                float dy = (e.y - u.y) / 45f;

                                e.vx = u.vel.x / 2f - dx;
                                e.vy = u.vel.y / 2f - dy;
                                e.vr = Mathf.range(4f);
                                //e.lifetime = 180f;
                                e.vz = Mathf.random(-0.01f, 0.1f);
                            }));
                        }else{
                            EmpathyDamage.annihilate(u, false);
                            FlameFX.empathyParry.at(u.x, u.y);
                        }
                    }
                }
            });
            float range = 270f * f2;
            Tmp.r1.setCentered(unit.x, unit.y, range * 2, range * 2);
            Groups.bullet.intersect(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, b -> {
                if(b.team != unit.team && unit.within(b, range + b.hitSize / 2) && b.type.speed > 0.001f){
                    float dst = 1f - (unit.dst(b) / (range + b.hitSize / 2));
                    Tmp.v1.set(unit).sub(b).nor().scl(Mathf.pow(dst, 3) * 7f * f);
                    b.vel.add(Tmp.v1);
                    if(Intersector.nearestSegmentPoint(b.lastX, b.lastY, b.vel.x * Time.delta + b.x, b.vel.y * Time.delta + b.y, unit.x, unit.y, Tmp.v2).within(unit.x, unit.y, 20f + b.type.splashDamageRadius)){
                        //EmpathyDamage.damageUnit(b, Math.max(1000000f, u.maxHealth / 5f), true, null);
                        b.team = unit.team;
                        FlameFX.empathyParry.at(b.x, b.y);
                        boolean a = Groups.isClearing;
                        Groups.isClearing = true;
                        b.remove();
                        Groups.isClearing = a;
                    }
                }
            });

            for(TeamData data : Vars.state.teams.present){
                if(data.team != unit.team && data.buildingTree != null){
                    data.buildingTree.intersect(Tmp.r1, b -> {
                        if(unit.within(b, range + b.hitSize() / 2)){
                            float dst = 1f - (unit.dst(b) / (range + b.hitSize() / 2));
                            if(Mathf.chanceDelta(Mathf.pow(dst, 5) * f2)){
                                EmpathyDamage.damageBuilding(b, Math.max(300000f, b.maxHealth / 10f) * Mathf.pow(dst, 8) * f2, true, null);
                            }
                        }
                    });
                }
            }

            if(blackholeTime >= (blackHoleDuration - 0.1f * 60f)){
                splitTime = Mathf.clamp(splitTime - Time.delta / 30f);
                if(splitTime <= 0f){
                    blackholeTime = 0f;
                    attacking = false;
                    openSound = false;
                    waitTime = 0f;

                    FlameSounds.empathyHologramActive.at(unit.x, unit.y);
                    unit.randAI(true, unit.health < 50);
                }
            }
        }
    }

    @Override
    boolean canKnockback(){
        return splitTime <= 0;
    }

    @Override
    boolean updateMovementAI(){
        return splitTime <= 0;
    }
    @Override
    boolean canTrail(){
        return splitTime <= 0;
    }

    @Override
    boolean shouldDraw(){
        return splitTime > 0;
    }

    @Override
    boolean overrideDraw(){
        return splitTime > 0;
    }

    void drawBlackHole(){
        float fi = Interp.pow2Out.apply(Mathf.clamp(blackholeTime / 60f)) * (Mathf.clamp(blackholeTime / blackHoleDuration) * 0.4f + 1f);
        float fi2 = Interp.pow3Out.apply(Mathf.clamp(blackholeTime / (5f * 60))) * (Mathf.clamp(blackholeTime / blackHoleDuration) * 0.4f + 1f);
        float fo = Mathf.clamp((blackHoleDuration - blackholeTime) / 120f);
        float fo2 = Mathf.clamp(((blackHoleDuration - 60f) - blackholeTime) / 35f);
        float f = fi * fo;
        float sin = Mathf.absin(12f, 0.1f) + 1f;
        float angle = blackholeTime / 1.5f;
        Rand r = Utils.rand, r2 = Utils.rand2;
        r.setSeed(unit.id + 16311);
        Color c = Tmp.c1.rand();
        float max = Math.max(c.r, Math.max(c.g, c.b));
        c.mul(1f / max);

        FlameOutSFX.inst.blackHole(unit.x, unit.y, fi2 * fo2, -angle);

        Draw.color(c);
        Fill.circle(unit.x, unit.y, 12f * sin * f);
        Drawf.tri(unit.x, unit.y, 7f * sin * f, 120f * f * f * sin, 0f);
        Drawf.tri(unit.x, unit.y, 7f * sin * f, 120f * f * f * sin, 180f);

        for(int i = 0; i < 7; i++){
            float dur = r.random(20f, 30f);
            float time = (Time.time + r.random(dur));
            float mtime = time % dur;
            float ef = mtime / dur;
            int stime = (int)(time / dur) * 1621 + i + unit.id;

            r2.setSeed(stime);
            float rot = r2.random(360f) + r2.range(80f) * ef;
            float l = r2.random(20f, 40f) * Interp.pow2.apply(Utils.biasSlope(ef, 0.2f)) * f * sin;
            float w = r2.random(5f, 7f) * f * sin;
            Vec2 v = Tmp.v1.trns(rot, 6f * sin * f).add(unit.x, unit.y);
            Drawf.tri(v.x, v.y, w, l, rot);
        }

        for(int i = 0; i < 80; i++){
            float dur = r.random(40f, 50f);
            float time = (Time.time + r.random(dur));
            float mtime = time % dur;
            float ef = mtime / dur;
            int stime = (int)(time / dur) * 1621 + i + unit.id;
            float scl = Interp.pow2Out.apply(Mathf.clamp(Math.max(blackholeTime - r.random(70f), 0) / 75f)) * Mathf.clamp(((blackHoleDuration - r.random(50f)) - blackholeTime) / 120f);
            if(scl > 0.001f){
                r2.setSeed(stime);
                float a = r2.random(360f);
                float l = r2.random(240f, 270f) * fi;
                float s = r2.random(4f, 6f) * scl * ef;
                Vec2 v = Tmp.v1.trns(a + angle * (ef * ef * ef) * fi, l * (1f - ef * ef)).add(unit);
                Fill.square(v.x, v.y, s, 45f);
            }
        }

        Draw.color(Color.black);
        Fill.circle(unit.x, unit.y, 10f * sin * f);
    }

    @Override
    void draw(){
        float z = Layer.flyingUnit;
        
        float sx = (blackholeTime <= 0.1f ? Interp.pow2Out.apply(Mathf.curve(splitTime, 0.75f, 1f)) : Interp.pow2Out.apply(Mathf.curve(splitTime, 0f, 0.5f))) * 15f;
        float sy = (blackholeTime <= 0.1f ? Interp.pow2.apply(Mathf.curve(splitTime, 0f, 0.75f)) : Interp.pow2Out.apply(Mathf.curve(splitTime, 0.5f, 1f))) * 9f;

        TextureRegion r = Tmp.tr1, srcr = unit.type.region;
        r.set(srcr);
        r.setU((srcr.u - srcr.u2) / 2 + srcr.u2);

        Vec2 v = Tmp.v2.trns(unit.rotation - 90f, r.width * Draw.scl * 0.5f + sx, sy);

        Draw.z(Math.min(Layer.darkness, z - 1f));
        //unit.type.drawShadow(unit);
        Draw.color(Pal.shadow);
        float hx = unit.x + UnitType.shadowTX, hy = unit.y + UnitType.shadowTY;
        Draw.rect(r, hx + v.x, hy + v.y, unit.rotation - 90f);
        Draw.rect(r, hx - v.x, hy - v.y, -r.width * Draw.scl, r.height * Draw.scl, unit.rotation - 90f);

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));
        unit.type.drawSoftShadow(unit, 1 - splitTime);

        if(blackholeTime > 0){
            Draw.z(Layer.effect - 0.1f);
            drawBlackHole();
        }

        Draw.z(z);
        Draw.color();
        Draw.rect(r, unit.x + v.x, unit.y + v.y, unit.rotation - 90f);
        Draw.rect(r, unit.x - v.x, unit.y - v.y, -r.width * Draw.scl, r.height * Draw.scl, unit.rotation - 90f);
    }
}
