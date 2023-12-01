package flame.unit.empathy;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.*;
import flame.audio.*;
import flame.effects.*;
import flame.entities.*;
import flame.graphics.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static arc.math.Interp.*;

public class CountDownAttack extends AttackAI{
    static float[] widths = {1.75f, 1f, 0.5f};
    static float[] widths2 = {1.1f, 1f, 0.9f, 0.8f}, lengths = {1.05f, 1f, 0.95f, 0.9f};
    static Color[] colors = {FlamePal.empathy.cpy().a(0.4f), FlamePal.empathy, Color.white, Color.black};

    boolean attacking = false;
    int count = 0;
    float waitTime = 0f;

    float reloadTime, reloadTime2, reloadTime3, rotation, targetRotation, lastRotation;
    float targetX, targetY;
    boolean hasTarget = false;
    float phaseTime = 0f;
    int intTime = 0;
    boolean collide = false;

    static Rect r1 = new Rect(), r2 = new Rect();
    static Vec2 vec = new Vec2(), vec2 = new Vec2(), vec3 = new Vec2();

    CountDownAttack(){
        super();
        addSound(FlameSounds.laserSmall);
        addSound(FlameSounds.empathyBigLaser);

        addSound(FlameSounds.portalOrder);
        addSound(FlameSounds.portalChaos);
    }

    @Override
    void init(){
        attacking = false;
    }

    @Override
    void reset(){
        attacking = false;
    }

    @Override
    boolean canKnockback(){
        return !attacking;
    }

    @Override
    void updatePassive(){
        waitTime -= Time.delta;
    }

    @Override
    float weight(){
        return waitTime > 0 ? -1f : (waitTime > (-15f * 60) ? 5f : 99000f);
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    void update(){
        if(!attacking){
            resetCountDownValues();
            FlameSounds.empathyHologramActive.at(unit.x, unit.y);
            attacking = true;
            //testTime = 3f * 60f;
        }
        //time -= Time.delta;
        unit.move(5f, 1, 0f, 0f);
        switch(count){
            case 0 -> update5();
            case 1 -> update4();
            case 2 -> update3();
            case 3 -> update2();
            case 4 -> update1();
            case 5 -> updateEnd();
        }
        if(!attacking){
            count = Mathf.mod(count + 1, 6);
            waitTime = 25f * 60f;
            //End attack ai
            if(count == 0){
                waitTime = 60f * 60f;
                for(EmpathyAI ai : unit.attackAIs){
                    if(ai instanceof EndAttack){
                        unit.switchAI(ai);
                        break;
                    }
                }
            }else{
                unit.randAI(true, unit.health < 50);
            }
        }
        /*
        else{
            testTime -= Time.delta;
        }
        */
    }

    void updateEnd(){
        phaseTime += Time.delta;

        if(phaseTime >= 3f * 60f){
            attacking = false;
        }
    }
    void drawEnd(){
        float fin = Mathf.clamp(phaseTime / 10f);
        float fout = Mathf.clamp(((3f * 60f) - phaseTime) / 10f);
        float alpha = (1f - Mathf.absin(0.4f, 0.6f));

        TextureRegion reg = EmpathyRegions.countDown[5];
        float rw = reg.width * Draw.scl, rh = reg.height * Draw.scl;

        Draw.color(FlamePal.empathyDark, alpha);
        Draw.blend(Blending.additive);

        //Vec2 v = Tmp.v1.trns(rot + 720f * pow2Out.apply(fin2), len1).add(unit.x, unit.y);

        Draw.rect(reg, unit.x, unit.y, rw * (4f - pow2In.apply(fin * fout) * 3f), rh * pow2In.apply(fin * fout));

        Draw.blend();
    }

    void update1(){
        phaseTime += Time.delta;

        if(phaseTime >= 120f){
            if(phaseTime > 285f){
                if(!collide){
                    Tmp.v5.trns(rotation, 40f).add(unit.x, unit.y);
                    impactBigLaser(Tmp.v5.x, Tmp.v5.y, rotation);

                    Tmp.v5.trns(rotation, 80f).add(unit.x, unit.y);
                    FlameFX.shootShockWave.at(Tmp.v5.x, Tmp.v5.y, rotation, 200f);
                    Tmp.v5.trns(rotation, 200f).add(unit.x, unit.y);
                    FlameFX.shootShockWave.at(Tmp.v5.x, Tmp.v5.y, rotation, 370f);

                    Sounds.laserblast.play(1f, 0.5f, 0f);

                    collide = true;
                }
                if(phaseTime > (285f + 6f)){
                    float fout = Mathf.clamp(((9f * 60f) - phaseTime) / 70f);
                    float endLength = 700f;
                    float length = 2200f;
                    //float width = 15f * fade;

                    Vec2 p = Tmp.v5.trns(rotation, 40f).add(unit.x, unit.y);
                    Vec2 v = vec.trns(rotation, length + endLength).add(p.x, p.y);

                    Vec2 n = Intersector.nearestSegmentPoint(p, v, Core.camera.position, Tmp.v4);

                    PassiveSoundLoop s = sounds.get(1);
                    s.play(n.x, n.y, fout, true);
                }
                if(reloadTime <= 0f && phaseTime > (285f + 6f)){
                    //float fin3 = Mathf.clamp((phaseTime - 285f) / 6f);
                    float fout = Mathf.clamp(((9f * 60f) - phaseTime) / 70f);

                    Tmp.v5.trns(rotation, 40f).add(unit.x, unit.y);

                    updateBigLaser(unit.team, Tmp.v5.x, Tmp.v5.y, rotation, fout);

                    reloadTime = 5f;
                }
                reloadTime -= Time.delta;
                if(unit.getTarget() != null) targetRotation = unit.angleTo(unit.getTarget());
                rotation = Angles.moveToward(rotation, targetRotation, 0.1f);
            }else{
                if(unit.getTarget() != null) targetRotation = unit.angleTo(unit.getTarget());
                rotation = Angles.moveToward(rotation, targetRotation, 2f);
            }
        }else{
            if(unit.getTarget() != null){
                targetRotation = unit.angleTo(unit.getTarget());
            }else{
                targetRotation = Mathf.random(360f);
            }
            rotation = targetRotation;
        }

        if(phaseTime >= 9f * 60f){
            attacking = false;
        }
    }
    static void updateBigLaser(Team team, float x, float y, float rotation, float fade){
        float offset = 340f;
        float endLength = 700f;
        float length = 2200f;
        float width = 70f * fade;
        //float width = 15f * fade;
        Vec2 v = vec.trns(rotation, length + endLength).add(x, y);
        float vx = v.x, vy = v.y;
        Utils.hitLaser(team, width * 2, x, y, v.x, v.y, h -> {
            float size = h instanceof Sized s ? s.hitSize() / 2 : 0f;
            Vec2 p = Intersector.nearestSegmentPoint(x, y, vx, vy, h.x(), h.y(), Tmp.v2);
            float dst = Mathf.dst(x, y, p.x, p.y);

            float w = circleOut.apply(Mathf.clamp(dst / offset)) * circleOut.apply(1f - Mathf.clamp((dst - length) / endLength)) * width;
            return p.within(h, w + size);
        }, h -> false, (h, hx, hy) -> {
            FlameFX.empathyBigLaserHit.at(hx, hy, Angles.angle(x, y, hx, hy), h);

            if(h instanceof Unit u){
                Tmp.v1.trns(rotation, 4f);
                u.vel.add(Tmp.v1);

                EmpathyDamage.damageUnit(u, Math.max(500000f, u.maxHealth / 40f), true, () -> {
                    SpecialDeathEffects.get(u.type).disintegrateUnit(u, x, y, vx, vy, width * 2, (d, within) -> {
                        if(within){
                            //d.lifetime = Mathf.random(42f, 60f);
                            d.disintegrating = true;

                            Vec2 n = Intersector.nearestSegmentPoint(x, y, vx, vy, d.x, d.y, vec3);
                            float dst = 1f - Mathf.clamp(n.dst(d.x, d.y) / width);
                            //float force = pow3.apply(dst) * 0.75f;
                            //float force = pow3.apply(dst) * Mathf.random(0.8f, 1.2f);
                            float force = pow2.apply(dst) * Mathf.random(0.9f, 1.5f);

                            d.lifetime = Mathf.lerp(Mathf.random(90f, 130f), Mathf.random(42f, 60f), pow2.apply(dst));

                            //if(dst < 0.00001f) return;

                            vec2.trns(rotation, force, Mathf.range(0.07f * dst));
                            d.vx = vec2.x;
                            d.vy = vec2.y;
                            d.vr = Mathf.range(5f) * dst;
                            //d.drag = -0.1f * dst;
                            //d.drag = Mathf.random(-0.15f, -0.075f) * dst;
                            d.drag = -Mathf.random(0.035f, 0.05f) * dst;
                            d.zOverride = Layer.flyingUnit;
                        }
                    });
                });
            }else if(h instanceof Building b){
                EmpathyDamage.damageBuilding(b, Math.max(500000f, b.maxHealth / 40f), true, b.block.size < 5 ? null : () -> {
                    FlameOut.vaporBatch.switchBatch(x, y, vx, vy, width * 2f, b::draw, (d, within) -> {
                        if(within){
                            d.disintegrating = true;

                            Vec2 n = Intersector.nearestSegmentPoint(x, y, vx, vy, d.x, d.y, vec3);
                            float dst = 1f - Mathf.clamp(n.dst(d.x, d.y) / width);
                            float force = pow2.apply(dst) * Mathf.random(0.9f, 1.5f);

                            d.lifetime = Mathf.lerp(Mathf.random(90f, 130f), Mathf.random(42f, 60f), pow2.apply(dst));

                            vec2.trns(rotation, force, Mathf.range(0.07f * dst));
                            d.vx = vec2.x;
                            d.vy = vec2.y;
                            d.vr = Mathf.range(5f) * dst;
                            d.drag = -Mathf.random(0.035f, 0.05f) * dst;
                            d.zOverride = Layer.flyingUnit;
                        }
                    });
                });
            }
        });
    }

    @SuppressWarnings("all")
    static void impactBigLaser(float x, float y, float rotation){
        FlameOutSFX.inst.impactFrames(x, y, rotation, 30f, true, () -> {
            float offset = 340f;
            float endLength = 700f;
            float length = 2200f;
            float width = 70f;

            Vec2 v = Tmp.v1.trns(rotation, offset).add(x, y);
            Vec2 end = Tmp.v4.trns(rotation, length).add(x, y);

            for(int j = 0; j < 16; j++){
                Vec2 v2 = Tmp.v2.trns(((j / 16f) - 0.5f) * 180f + 180, 1f).scl(offset, width).rotate(rotation).add(v);
                Vec2 v3 = Tmp.v3.trns((((j + 1f) / 16f) - 0.5f) * 180f + 180, 1f).scl(offset, width).rotate(rotation).add(v);

                //Fill.tri(v.x, v.y, v2.x, v2.y, v3.x, v3.y);
                Fill.tri(v.x, v.y, v2.x, v2.y, v3.x, v3.y);

                v2.trns(((j / 16f) - 0.5f) * 180f, 1f).scl(endLength, width).rotate(rotation).add(end);
                v3.trns((((j + 1f) / 16f) - 0.5f) * 180f, 1f).scl(endLength, width).rotate(rotation).add(end);

                Fill.tri(end.x, end.y, v2.x, v2.y, v3.x, v3.y);
            }

            Lines.stroke(width * 2f);
            Lines.line(v.x, v.y, end.x, end.y, false);
        });
        Vars.renderer.shake(80f, 90f);
    }
    @SuppressWarnings("all")
    static void drawBigLaser(float x, float y, float rotation, float fade){
        float offset = 340f;
        float endLength = 700f;
        float length = 2200f;
        float width = (70f + Mathf.absin(1.5f, 4f)) * fade;
        //float width = (15f + Mathf.absin(1.5f, 4f)) * fade;

        Vec2 v = Tmp.v1.trns(rotation, offset).add(x, y);
        Vec2 end = Tmp.v4.trns(rotation, length).add(x, y);

        for(int i = 0; i < 4; i++){
            float w = width * widths2[i];
            float off = offset * lengths[i];
            float endl = endLength * lengths[i];
            
            float rx = Mathf.range(2f * fade), ry = Mathf.range(2f * fade);

            Draw.color(colors[i]);
            for(int j = 0; j < 16; j++){
                Vec2 v2 = Tmp.v2.trns(((j / 16f) - 0.5f) * 180f + 180, 1f).scl(off, w).rotate(rotation).add(v);
                Vec2 v3 = Tmp.v3.trns((((j + 1f) / 16f) - 0.5f) * 180f + 180, 1f).scl(off, w).rotate(rotation).add(v);

                //Fill.tri(v.x, v.y, v2.x, v2.y, v3.x, v3.y);
                Fill.tri(v.x + rx, v.y + ry, v2.x + rx, v2.y + ry, v3.x + rx, v3.y + ry);
                
                v2.trns(((j / 16f) - 0.5f) * 180f, 1f).scl(endl, w).rotate(rotation).add(end);
                v3.trns((((j + 1f) / 16f) - 0.5f) * 180f, 1f).scl(endl, w).rotate(rotation).add(end);
                
                Fill.tri(end.x + rx, end.y + ry, v2.x + rx, v2.y + ry, v3.x + rx, v3.y + ry);
            }
            Lines.stroke(w * 2f);
            //Lines.line(v.x, v.y, end.x, end.y, false);
            Lines.line(v.x + rx, v.y + ry, end.x + rx, end.y + ry, false);
        }
        Rand r = Utils.rand, r2 = Utils.rand2;
        r.setSeed(24674521);
        for(int i = 0; i < 15; i++){
            float dur = r.random(4f, 7f);
            float t = (Time.time + r.random(dur)) / dur;
            float mt = t % 1f;
            int seed = (int)(t) + r.nextInt();

            r2.setSeed(seed);
            float ofs = r2.range(1f);
            float sw = r2.random(1.5f, 3f);
            float widoff = width * ofs;
            float lenoff = circleIn.apply(Math.abs(ofs)) * offset;
            float endoff = length + endLength * (1f - circleIn.apply(Math.abs(ofs)));

            float slope = pow2.apply(Mathf.slope(mt));
            //int col = r2.random(1, 2);

            Vec2 p1 = Tmp.v1.trns(rotation, lenoff, widoff).add(x, y);
            Vec2 p2 = Tmp.v2.trns(rotation, endoff, widoff).add(x, y);

            Draw.color(colors[1]);
            Lines.stroke(slope * sw * fade);
            Lines.line(p1.x, p1.y, p2.x, p2.y, false);
        }
        for(int i = 0; i < 60; i++){
            float dur = r.random(30f, 45f);
            float t = (Time.time + r.random(dur)) / dur;
            float mt = t % 1f;
            int seed = (int)(t) + r.nextInt();

            r2.setSeed(seed);
            float ofs = r2.range(1f);
            float rwid = r2.random(8f, 15f) * fade;
            //float rheight = rwid * 5f + r2.random(100f, 140f);
            float rheight = rwid * 5f + r2.random(190f, 240f);
            float widoff = (width - rwid / 2f) * ofs;
            float lenoff = circleIn.apply(Math.abs(ofs)) * offset + rheight / 1.5f;
            float endoff = length + endLength * (1f - circleIn.apply(Math.abs(ofs))) - rheight / 1.5f;

            //float slope = pow2.apply(Mathf.slope(mt));
            float slope = pow2Out.apply(Mathf.curve(Mathf.slope(mt), 0f, 0.1f));
            //int col = r2.random(1, 2);
            //int col = 1;
            Color tmc = Tmp.c1.set(colors[1]).lerp(colors[2], r2.nextFloat());

            Draw.color(tmc);
            Vec2 p1 = Tmp.v1.trns(rotation, Mathf.lerp(lenoff, endoff, mt), widoff + Mathf.range(2f * fade)).add(x, y);
            //Draw.rect(EmpathyRegions.circle, p1, rwid * slope, rheight, rotation - 90f);
            GraphicUtils.diamond(p1.x, p1.y, rwid * slope, rheight, rotation);
        }
    }
    void draw1(){
        float fin = Mathf.clamp(phaseTime / 10f);
        float fin2 = Mathf.clamp((phaseTime - 120f) / (2.75f * 60f));
        float fin3 = Mathf.clamp((phaseTime - 285f) / 6f);
        //float fin4 = Mathf.clamp((phaseTime - 285f) / 6f);

        float fout = Mathf.clamp(((9f * 60f) - phaseTime) / 70f);

        TextureRegion reg = EmpathyRegions.countDown[4];
        float rw = reg.width * Draw.scl, rh = reg.height * Draw.scl;
        float alpha = (1f - Mathf.absin(0.4f, 0.6f)) * fout;

        float rot = rotation;
        float len1 = pow2Out.apply(fin2) * 120f;

        Draw.color(FlamePal.empathyDark, alpha);
        Draw.blend(Blending.additive);

        Vec2 v = Tmp.v1.trns(rot + 720f * pow2Out.apply(fin2), len1).add(unit.x, unit.y);

        Draw.rect(reg, v.x, v.y, rw * (4f - pow2In.apply(fin) * 3f) * (1f - pow2Out.apply(fin3)), rh * pow2In.apply(fin) * (1f + 10f * fin3), Mathf.slerp(0f, rotation - 90f, pow2.apply(fin2)) + 720f * 2 * pow2Out.apply(fin2));

        Draw.blend();

        if(fin3 > 0.0001f){
            float z = Draw.z();
            v.trns(rot, 40f).add(unit.x, unit.y);
            Draw.z((Layer.bullet + Layer.effect) / 2);
            //Draw.z(Layer.bullet);
            //(Layer.bullet + Layer.effect) / 2
            //Draw.z(Layer.groundUnit - 0.01f);
            drawBigLaser(v.x, v.y, rotation, fin3 * fout);
            Draw.z(z);
        }
    }
    void update2(){
        phaseTime += Time.delta;
        
        if(phaseTime >= 425f){
            //Mathf.clamp((phaseTime - 425f) / 120f)
            float fin = Mathf.clamp((phaseTime - 425f) / 120f);
            float fout = Mathf.clamp(((25f * 60f) - phaseTime) / 70f);

            for(int i = 0; i < 2; i++){
                //int sign = i == 0 ? 1 : -1;
                float len = (120f + 300f) * (i == 0 ? 1 : -1);
                float x = unit.x + len;
                float y = unit.y;

                PassiveSoundLoop s = i == 0 ? sounds.get(3) : sounds.get(2);
                s.play(x, y, 1.25f * fout * fin, true);
            }
        }

        if(phaseTime >= 545f){
            boolean shoot = (25f * 60f - phaseTime) > 5f * 60f;

            if(reloadTime <= 0f && shoot){
                for(int i = 0; i < 2; i++){
                    //int sign = i == 0 ? 1 : -1;
                    float len = (120f + 300f) * (i == 0 ? 1 : -1);
                    float x = unit.x + len;
                    float y = unit.y;

                    for(int j = 0; j < 4; j++){
                        float ang = ((360f / 4) * j) + rotation;

                        //LightDarkProjectile.create(unit.team, i == 0, x, y, ang, unit.x - len, y);
                        for(int k = 0; k < 5; k++){
                            float f = ((k / 4f) - 0.5f) * 5f;
                            LightDarkProjectile.create(unit.team, i == 0, x, y, ang + f, unit.x - len, y);
                        }
                    }
                }
                //reloadTime = 3f;
                rotation += 15f;
                reloadTime = 50f;
            }
            if(reloadTime2 <= 0f && shoot){
                for(int i = 0; i < 2; i++){
                    float len = (120f + 300f) * (i == 0 ? 1 : -1);
                    float x = unit.x + len;
                    float y = unit.y;
                    for(int j = 0; j < 16; j++){
                        float ang = ((360f / 16) * j);
                        LightDarkProjectile.create(unit.team, i == 0, x, y, ang, unit.x - len, y);
                    }
                }
                reloadTime2 = 20f;
            }
            if(reloadTime3 < 115f && unit.getTarget() != null){
                targetX = unit.getTarget().x();
                targetY = unit.getTarget().y();
                hasTarget = true;
            }
            if(reloadTime3 >= 125f && !collide && hasTarget){
                collide = true;

                r1.setCentered(targetX, targetY, 25f);
                for(TeamData data : Vars.state.teams.present){
                    if(data.team != unit.team){
                        if(data.unitTree != null){
                            data.unitTree.intersect(r1, u -> EmpathyDamage.damageUnit(u, Math.max(10000f, u.maxHealth / 100f), u.maxHealth < 20000, null));
                        }
                        if(data.buildingTree != null){
                            data.buildingTree.intersect(r1, b -> EmpathyDamage.damageBuilding(b, Math.max(10000f, b.maxHealth / 100f), b.maxHealth < 20000, null));
                        }
                    }
                }
            }
            if(reloadTime3 >= (120f + 60f)){
                intTime++;
                reloadTime3 = 0f;
                hasTarget = false;
                collide = false;
            }
            reloadTime -= Time.delta;
            reloadTime2 -= Time.delta;
            reloadTime3 += Time.delta;
        }

        if(phaseTime >= (25f * 60f)){
            attacking = false;
        }
    }
    void drawDarkTentacle(float x, float y, float fin){
        Rand rand = Utils.rand;
        float ox = rand.range(15f), oy = rand.range(15f), rsin = rand.random(240f, 540f);
        int sign = (rand.nextFloat() > 0.5f ? 1 : -1);

        if(fin <= 0.0001f || !hasTarget) return;
        float fout = 1f - fin;
        float cf = pow2In.apply(Mathf.curve(fin, 0f, 0.2f));
        float len = Mathf.dst(x, y, targetX + ox, targetY + oy) * 1.1f * pow2Out.apply(Mathf.curve(fin, 0f, 0.2f));
        float ang = Angles.angle(x, y, targetX + ox, targetY + oy);

        float sx = x, sy = y;

        Draw.color(Color.black);
        //Draw.color(Color.red);
        for(int i = 0; i < 20; i++){
            float ifin = (i / 19f);
            //float sin = Mathf.sinDeg(ifin * 360f);
            float sin = Mathf.cosDeg(ifin * rsin) * sign;

            Vec2 v = Tmp.v3.trns(ang + sin * 90f * (1f - cf), len / 20f).add(sx, sy);

            Lines.stroke(12f * (1f - i / 20f) * fout);
            //Lines.stroke(5f);
            Lines.line(sx, sy, v.x, v.y, false);
            Fill.circle(sx, sy, 12f * (1f - i / 20f) * fout * 0.5f);

            sx = v.x;
            sy = v.y;
        }
    }
    void drawLightSpear(float x, float y, float fin){
        if(fin <= 0.0001f || !hasTarget) return;
        float fout = 1f - fin;
        Vec2 v = Tmp.v3.set(targetX, targetY).sub(x, y).scl(pow2In.apply(Mathf.curve(fin, 0f, 0.2f)) * 1.1f);
        float dst = v.len();
        float ang = v.angle();
        float base = Math.min(dst / 5f, 45f);

        Vec2 p = Tmp.v2.trns(ang, base).add(x, y);
        Draw.color(Color.white);
        Drawf.tri(p.x, p.y, 12f * fout, base, ang + 180f);
        Drawf.tri(p.x, p.y, 12f * fout, (dst - base), ang);
    }
    void draw2(){
        float fin = Mathf.clamp(phaseTime / 10f);
        float fin2 = Mathf.clamp((phaseTime - 120f) / (2.75f * 60f));
        float fin3 = Mathf.clamp((phaseTime - 305f) / 120f);

        float fout = Mathf.clamp(((25f * 60f) - phaseTime) / 70f);
        float fout2 = 1f - Mathf.clamp((phaseTime - 425f) / 120f);
        float fin4 = 1f - fout2;

        float sfin = reloadTime3 >= 120 ? Mathf.clamp((reloadTime3 - 120f) / 60f) : 0f;

        TextureRegion reg = EmpathyRegions.countDown[3];
        float rw = reg.width * Draw.scl, rh = reg.height * Draw.scl;
        float alpha = (1f - Mathf.absin(0.4f, 0.6f)) * fout;

        float rot = pow2.apply(fin2) * 360f;
        float len1 = pow2Out.apply(fin2) * 120f;
        float len2 = pow2In.apply(fin3) * 300f;

        float ta = Mathf.lerp(alpha / 2f, alpha, Mathf.clamp(fin2 * 3f));
        
        if(fin4 > 0.0001f){
            for(int i = 0; i < 2; i++){
                float r = (((360f / 2) * i)) + rot;
                Vec2 v = Tmp.v1.trns(r, len1 + len2).add(unit.x, unit.y);
                
                float pw = (67f * pow5In.apply(fin4) + 3f) * fout;
                float ph = 330f * pow4Out.apply(fin4);
                if(i == 0){
                    FlameOutSFX.inst.drawChaosPortal(v.x, v.y, pw, ph);
                    //drawDarkTentacle(v.x, v.y, sfin);

                    Rand rand = Utils.rand;
                    rand.setSeed(unit.id * 531L + intTime);

                    for(int j = 0; j < 5; j++){
                        float f = j / 4f;
                        float c = Mathf.curve(sfin, 0.2f * f, (1f - 0.2f) + 0.2f * f);

                        Tmp.v4.trns(rand.random(360f), Mathf.sqrt(rand.nextFloat()) * 30f);

                        drawDarkTentacle(v.x + Tmp.v4.x, v.y + Tmp.v4.y, c);
                    }
                }else{
                    FlameOutSFX.inst.drawOrderPortal(v.x, v.y, pw, ph);
                    //drawLightSpear(v.x, v.y, sfin);
                    int sign = (intTime % 2) == 0 ? 1 : -1;

                    for(int j = 0; j < 5; j++){
                        float f = j / 4f;
                        float c = Mathf.curve(sfin, 0.2f * f, (1f - 0.2f) + 0.2f * f);
                        float of = (f - 0.5f) * 150f * sign;
                        drawLightSpear(v.x, v.y + of, c);
                    }
                }
            }
        }
        
        Draw.color(FlamePal.empathyDark, ta);
        Draw.blend(Blending.additive);

        for(int i = 0; i < 2; i++){
            float r = (((360f / 2) * i)) + rot;
            Vec2 v = Tmp.v1.trns(r, len1 + len2).add(unit.x, unit.y);

            Draw.rect(reg, v.x, v.y, rw * (4f - pow2In.apply(fin) * 3f) * Mathf.curve(fout2, 0.95f, 1f), rh * pow2In.apply(fin), pow3In.apply(fin3) * (360f * 3f) * (i == 0 ? -1f : 1f));
        }

        Draw.blend();
    }

    void update3(){
        //phaseTime += Time.delta;

        if(phaseTime >= 285f){
            if(intTime < 4){
                if(lastRotation != targetRotation){
                    rotation = Mathf.clamp(rotation + Time.delta / 30f);
                }

                if(rotation >= 1f || lastRotation == targetRotation) reloadTime = Mathf.clamp(reloadTime + Time.delta / 90f);
                if(reloadTime >= 0.6f && !collide){
                    collide = true;

                    float rot = 360f + Mathf.lerp(lastRotation, targetRotation, pow3.apply(rotation));
                    Sounds.laserblast.play(0.4f, 2f, 0f);

                    for(int i = 0; i < 3; i++){
                        float r = (((360f / 3) * i) - 90f) + rot;

                        for(int j = 0; j < 11; j++){
                            int offset = j - 6;
                            float slope = Mathf.slope(j / 11f);
                            //float slope = Mathf.slope(j / 9f);
                            //float len = Mathf.lerp(500f, 1000f, slope);
                            //1320
                            float len = Mathf.lerp(600f, 1200f, slope);

                            Vec2 v1 = Tmp.v1.trns(r, 120f * 1.5f * offset + 120f, len).add(unit.x, unit.y);
                            Vec2 v2 = Tmp.v2.trns(r, 120f * 1.5f * offset + 120f, -len).add(unit.x, unit.y);

                            float v1x = v1.x, v1y = v1.y, v2x = v2.x, v2y = v2.y;

                            //drawPentaLaser(v1.x, v1.y, v2.x, v2.y, w);
                            float dam = 1500f;
                            Utils.hitLaser(unit.team, 3f, v1.x, v1.y, v2.x, v2.y, null, h -> false, (h, x, y) -> {
                                Intersector.nearestSegmentPoint(v1x, v1y, v2x, v2y, h.x(), h.y(), Tmp.v3);
                                Fx.hitBulletColor.at(Tmp.v3.x, Tmp.v3.y, FlamePal.empathy);

                                boolean lethal = h.maxHealth() < 9000f;
                                if(h instanceof Unit u){
                                    EmpathyDamage.damageUnit(u, dam + u.maxHealth / 10f, lethal, null);
                                }else if(h instanceof Building b){
                                    EmpathyDamage.damageBuilding(b, dam + b.maxHealth / 10f, lethal, null);
                                }
                            });
                        }
                    }
                }
                if(reloadTime >= 1f){
                    reloadTime = 0f;
                    lastRotation = targetRotation;
                    rotation = 0f;
                    //targetRotation += 60f;
                    targetRotation += 180f;
                    intTime++;
                    collide = false;
                }
            }else{
                phaseTime += Time.delta;
            }
        }else{
            phaseTime += Time.delta;
        }

        if(phaseTime >= (285f + 80f)){
            attacking = false;
        }
    }
    void draw3(){
        float fin = Mathf.clamp(phaseTime / 10f);
        float fin2 = Mathf.clamp((phaseTime - 120f) / (2.75f * 60f));

        float fout = Mathf.clamp(((285f + 80f) - phaseTime) / 70f);

        TextureRegion reg = EmpathyRegions.countDown[2];
        float rw = reg.width * Draw.scl, rh = reg.height * Draw.scl;
        float alpha = (1f - Mathf.absin(0.4f, 0.6f)) * fout;

        float rot = pow2.apply(fin2) * 360f + Mathf.lerp(lastRotation, targetRotation, pow3.apply(rotation));
        float len1 = pow2Out.apply(fin2) * 120f;

        float f1 = pow2.apply(Utils.biasSlope(Mathf.curve(reloadTime, 0.58f, 1f), 0.1f));

        if(f1 > 0.0001f){
            for(int c = 0; c < 3; c++){
                float w = widths[c] * (1f + Mathf.absin(2.5f, 0.3f)) * 4f * f1;
                Draw.color(colors[c]);
                for(int i = 0; i < 3; i++){
                    float r = (((360f / 3) * i) - 90f) + rot;

                    for(int j = 0; j < 11; j++){
                        int offset = j - 6;
                        float slope = Mathf.slope(j / 11f);
                        //float slope = Mathf.slope(j / 9f);
                        //float len = Mathf.lerp(500f, 1000f, slope);
                        //1320
                        float len = Mathf.lerp(600f, 1200f, slope);

                        Vec2 v1 = Tmp.v1.trns(r, 120f * 1.5f * offset + 120f, len).add(unit.x, unit.y);
                        Vec2 v2 = Tmp.v2.trns(r, 120f * 1.5f * offset + 120f, -len).add(unit.x, unit.y);

                        drawPentaLaser(v1.x, v1.y, v2.x, v2.y, w);
                    }
                }
            }
        }

        float ta = Mathf.lerp(alpha / 3f, alpha, Mathf.clamp(fin2 * 3f));
        Draw.color(FlamePal.empathyDark, ta);
        Draw.blend(Blending.additive);

        for(int i = 0; i < 3; i++){
            float r = (((360f / 3) * i) - 90f) + rot;
            Vec2 v = Tmp.v1.trns(r, len1).add(unit.x, unit.y);

            Draw.rect(reg, v.x, v.y, rw * (4f - pow2In.apply(fin) * 3f), rh * pow2In.apply(fin));
        }

        Draw.blend();
    }

    void update4(){
        phaseTime += Time.delta;

        if(phaseTime >= 295f){
            reloadTime = Mathf.clamp(reloadTime + Time.delta / 90f);
            if(!collide && phaseTime < (17f * 60f - 70f)){
                FlameSounds.empathySquareCharge.at(unit.x, unit.y);
                collide = true;
            }
            if(reloadTime >= 1f){
                SquareEntity sq = new SquareEntity();
                sq.x = unit.x;
                sq.y = unit.y;
                sq.team = unit.team;

                Tmp.v1.trns(Mathf.random(360f), 3f);
                if(unit.getTarget() != null){
                    Teamc tar = unit.getTarget();
                    if(Mathf.chance(0.75f) || !(tar instanceof Velc vel)){
                        Tmp.v1.trns(unit.angleTo(tar), 3f);
                    }else{
                        Vec2 p = Predict.intercept(unit.x, unit.y, vel.x(), vel.y(), vel.vel().x * Time.delta, vel.vel().y * Time.delta, 3f);
                        Tmp.v1.trns(p.sub(unit).angle(), 3f);
                    }
                }
                sq.vx += Tmp.v1.x;
                sq.vy += Tmp.v1.y;
                sq.vrot = Mathf.range(0.5f, 1.75f);
                sq.size = 120f;

                sq.add();

                reloadTime = 0f;
                collide = false;
                FlameSounds.empathySquareShoot.at(unit.x, unit.y);
            }
        }

        if(phaseTime >= 17f * 60f){
            attacking = false;
        }
    }
    void draw4(){
        float fin = Mathf.clamp(phaseTime / 10f);
        float fin2 = Mathf.clamp((phaseTime - 120f) / (2.75f * 60f));

        float fout = Mathf.clamp((17f * 60f - phaseTime) / 70f);

        if(reloadTime > 0.25f){
            Draw.color(FlamePal.empathy);
            Fill.poly(unit.x, unit.y, 4, 120f * Mathf.curve(reloadTime, 0.6f, 1f), 45f);
        }

        TextureRegion reg = EmpathyRegions.countDown[1];
        float rw = reg.width * Draw.scl, rh = reg.height * Draw.scl;
        float alpha = (1f - Mathf.absin(0.4f, 0.6f)) * fout;

        float rot = pow2.apply(fin2) * 360f;
        float len1 = pow2Out.apply(fin2) * 120f;

        float ta = Mathf.lerp(alpha / 4f, alpha, Mathf.clamp(fin2 * 3f));
        Draw.color(FlamePal.empathyDark, ta);
        Draw.blend(Blending.additive);

        for(int i = 0; i < 4; i++){
            float r = (((360f / 4) * i) - 45f) + rot;
            Vec2 v = Tmp.v1.trns(r, len1).add(unit.x, unit.y);

            Draw.rect(reg, v.x, v.y, rw * (4f - pow2In.apply(fin) * 3f), rh * pow2In.apply(fin));
        }

        Draw.blend();
    }

    void update5(){
        phaseTime += Time.delta;
        reloadTime -= Time.delta;

        float fin2 = Mathf.clamp((phaseTime - 120f) / (2.75f * 60f));
        float fin3 = Mathf.clamp((phaseTime - (285f + 30f)) / (4f * 60f));
        float endRot = pow3.apply(Mathf.clamp((phaseTime - (555f)) / (11f * 60f))) * 360f;
        float fout2 = Mathf.clamp(((22f * 60f) - (phaseTime + 60f)) / 30f);

        float rot = pow2.apply(fin2) * 360f;
        float len1 = pow2Out.apply(fin2) * 120f;
        float len2 = pow2Out.apply(fin3) * 600f;

        if(fin2 > 0.001f && fout2 > 0.5f){
            PassiveSoundLoop s = sounds.get(0);
            float nearX = 0f, nearY = 0f, nearS = 9999999f;
            float pan = 0f, panWeight = 0f;
            for(int i = 0; i < 5; i++){
                float r1 = (((360f / 5) * i) * 2f - 90f) + rot;
                float r2 = (((360f / 5) * (i + 1f)) * 2f - 90f) + rot;
                Vec2 v1 = Tmp.v1.trns(r1 + endRot, len1 + len2).add(unit.x, unit.y);
                Vec2 v2 = Tmp.v2.trns(r2 + endRot, len1 + len2).add(unit.x, unit.y);

                Vec2 n = Intersector.nearestSegmentPoint(v1, v2, Core.camera.position, Tmp.v3);
                float dst = Core.camera.position.dst(n);
                float fall = s.calcFalloff(n.x, n.y);
                pan += s.calcPan(n.x, n.y) * fall;
                panWeight += fall;

                if(dst < nearS){
                    nearX = n.x;
                    nearY = n.y;
                    nearS = dst;
                }
            }

            s.play(nearX, nearY, 0.75f, pan / Math.max(panWeight, 0.001f), true);
        }


        if(reloadTime <= 0f){
            if(fin2 > 0.001f && fout2 > 0.5f){
                for(int i = 0; i < 5; i++){
                    float r1 = (((360f / 5) * i) * 2f - 90f) + rot;
                    float r2 = (((360f / 5) * (i + 1f)) * 2f - 90f) + rot;
                    Vec2 v1 = Tmp.v1.trns(r1 + endRot, len1 + len2).add(unit.x, unit.y);
                    Vec2 v2 = Tmp.v2.trns(r2 + endRot, len1 + len2).add(unit.x, unit.y);

                    //Lines.line(v1.x, v1.y, v2.x, v2.y);
                    //fout2 * fin2
                    float dam = 700f * fout2 * fin2;
                    Utils.hitLaser(unit.team, fout2 * fin2 * 3f, v1.x, v1.y, v2.x, v2.y, null, h -> false, (h, x, y) -> {
                        Fx.hitBulletColor.at(x, y, FlamePal.empathy);
                        boolean lethal = h.maxHealth() < 7000f;
                        if(h instanceof Unit u){
                            EmpathyDamage.damageUnit(u, dam, lethal, null);
                        }else if(h instanceof Building b){
                            EmpathyDamage.damageBuilding(b, dam, lethal, null);
                        }
                    });
                }
            }

            reloadTime = 4f;
        }

        if(phaseTime >= 22f * 60f){
            attacking = false;
        }
    }
    void draw5(){
        float fin = Mathf.clamp(phaseTime / 10f);
        float fin2 = Mathf.clamp((phaseTime - 120f) / (2.75f * 60f));
        float fin3 = Mathf.clamp((phaseTime - (285f + 30f)) / (4f * 60f));
        float endRot = pow3.apply(Mathf.clamp((phaseTime - (555f)) / (11f * 60f))) * 360f;
        float fout = Mathf.clamp((22f * 60f - phaseTime) / 70f);
        float fout2 = Mathf.clamp(((22f * 60f) - (phaseTime + 60f)) / 30f);

        float alpha = (1f - Mathf.absin(0.4f, 0.6f)) * fout;

        //float ta = Mathf.lerp(alpha / 5f, alpha, Mathf.clamp(fin2 * 3f));
        float rot = pow2.apply(fin2) * 360f;
        float len1 = pow2Out.apply(fin2) * 120f;
        float len2 = pow2Out.apply(fin3) * 600f;
        TextureRegion reg = EmpathyRegions.countDown[0];
        float rw = reg.width * Draw.scl, rh = reg.height * Draw.scl;
        //rect(TextureRegion region, float x, float y, float w, float h)

        //Draw.color(FlamePal.empathy);
        for(int c = 0; c < 3; c++){
            Draw.color(colors[c]);
            float wid = widths[c] * 5f;
            for(int i = 0; i < 5; i++){
                float r1 = (((360f / 5) * i) * 2f - 90f) + rot;
                float r2 = (((360f / 5) * (i + 1f)) * 2f - 90f) + rot;
                Vec2 v1 = Tmp.v1.trns(r1 + endRot, len1 + len2).add(unit.x, unit.y);
                Vec2 v2 = Tmp.v2.trns(r2 + endRot, len1 + len2).add(unit.x, unit.y);

                //Lines.line(v1.x, v1.y, v2.x, v2.y);
                drawPentaLaser(v1.x, v1.y, v2.x, v2.y, wid * (1f + Mathf.absin(2.5f, 0.3f)) * fout2 * fin2);
            }
        }

        //float ta = Mathf.lerp(Mathf.pow(alpha / 5f, 1f - (1f / 2.5f)), alpha, Mathf.clamp(fin2 * 3f));
        float ta = Mathf.lerp(alpha / 5f, alpha, Mathf.clamp(fin2 * 3f));
        Draw.color(FlamePal.empathyDark, ta);
        Draw.blend(Blending.additive);
        for(int i = 0; i < 5; i++){
            float r = (((360f / 5) * i) - 90f) + rot;
            Vec2 v = Tmp.v1.trns(r + endRot, len1 + len2).add(unit.x, unit.y);

            Draw.rect(reg, v.x, v.y, rw * (4f - pow2In.apply(fin) * 3f), rh * pow2In.apply(fin));
        }
        Draw.blend();
    }
    void drawPentaLaser(float x1, float y1, float x2, float y2, float width){
        float offset = 40f;
        float len = Mathf.dst(x1, y1, x2, y2);
        float ang = Angles.angle(x1, y1, x2, y2);

        Tmp.v3.set(x2, y2).sub(x1, y1).setLength(offset);
        float ox = Tmp.v3.x, oy = Tmp.v3.y;
        //float width = 8f;

        if(len > offset * 2f){
            Lines.stroke(width);
            Lines.line(x1 + ox, y1 + oy, x2 - ox, y2 - oy, false);
            Drawf.tri(x1 + ox, y1 + oy, width, offset, ang + 180f);
            Drawf.tri(x2 - ox, y2 - oy, width, offset, ang);
        }else{
            float mx = (x1 + x2) / 2f, my = (y1 + y2) / 2f;
            Drawf.tri(mx, my, width, len / 2f, ang + 180f);
            Drawf.tri(mx, my, width, len / 2f, ang);
        }
    }

    void resetCountDownValues(){
        reloadTime = reloadTime2 = reloadTime3 = rotation = targetRotation = lastRotation = 0f;
        targetX = targetY = 0f;
        phaseTime = 0f;
        collide = false;
        hasTarget = false;
        intTime = 0;
    }

    @Override
    boolean shouldDraw(){
        return attacking;
    }

    @Override
    void draw(){
        float z = Draw.z();
        Draw.z(Layer.flyingUnit + 0.5f);
        switch(count){
            case 0 -> draw5();
            case 1 -> draw4();
            case 2 -> draw3();
            case 3 -> draw2();
            case 4 -> draw1();
            case 5 -> drawEnd();
        }
        Draw.reset();
        Draw.z(z);
    }

    static class SquareEntity extends DrawEntity{
        float time = 0f, time2 = 0f;
        float vx, vy;
        float rot, vrot;
        float size = 0f;
        Team team;

        @Override
        public void update(){
            float delta = FlameOutSFX.timeDelta;
            x += vx * delta;
            y += vy * delta;
            rot += vrot * delta;

            time += delta;
            if(time2 <= 0f){
                float s = ((size * 2f) / 1.414213f) - 4f;
                r1.setCentered(x, y, s);
                for(TeamData data : Vars.state.teams.present){
                    if(data.team != team){
                        if(data.unitTree != null){
                            Rect r = Tmp.r1.setCentered(x, y, size * 3.5f);
                            data.unitTree.intersect(r, u -> {
                                Vec2 v = Tmp.v1.set(u.x, u.y).sub(x, y).rotate(-rot).add(x, y);
                                r2.setCentered(v.x, v.y, u.hitSize);
                                if(r1.overlaps(r2)){
                                    EmpathyDamage.damageUnit(u, 1000f, u.maxHealth < 8000, null);
                                }
                            });
                        }
                        if(data.buildingTree != null){
                            Rect r = Tmp.r1.setCentered(x, y, size * 3.5f);
                            data.buildingTree.intersect(r, b -> {
                                Vec2 v = Tmp.v1.set(b.x, b.y).sub(x, y).rotate(-rot).add(x, y);
                                r2.setCentered(v.x, v.y, b.hitSize());
                                if(r1.overlaps(r2)){
                                    EmpathyDamage.damageBuilding(b, 1000f, b.maxHealth < 8000, null);
                                }
                            });
                        }
                    }
                }

                time2 = 5f;
            }
            time2 -= delta;
            if(time >= 6f * 60f){
                FlameFX.empathySquareDespawn.at(x, y, rot);
                remove();
            }
        }

        @Override
        public float clipSize(){
            return size * 2.2f;
        }

        @Override
        public void draw(){
            Draw.z(Layer.flyingUnit);
            Draw.color(FlamePal.empathy);
            Fill.poly(x, y, 4, size, rot + 45f);
            Draw.color();
        }
    }

    static class LightDarkProjectile extends DrawEntity implements Poolable{
        boolean dark;
        float targetX = 0f, targetY = 0f;
        float time = 0f;
        Vec2 vel = new Vec2();
        Team team;

        static boolean collided = false;

        static void create(Team team, boolean dark, float x, float y, float rotation, float targetX, float targetY){
            LightDarkProjectile l = Pools.obtain(LightDarkProjectile.class, LightDarkProjectile::new);
            l.team = team;
            l.dark = dark;
            l.x = x;
            l.y = y;
            l.targetX = targetX;
            l.targetY = targetY;
            l.vel.trns(rotation, 4f);
            l.add();
        }

        @Override
        public void update(){
            x += vel.x * Time.delta;
            y += vel.y * Time.delta;

            //Tmp.v1.set(targetX, targetY).sub(x, y).setLength(0.05f);
            //Tmp.v1.set(targetX, targetY).sub(x, y).setLength(Math.min((650f / Mathf.dst(x, y, targetX, targetY)) * 0.05f, 0.1f));
            Tmp.v1.set(targetX, targetY).sub(x, y).setLength(Math.min((650f / Mathf.dst(x, y, targetX, targetY)) * 0.05f, 0.1f)).scl(Time.delta);
            vel.add(Tmp.v1).setLength(4f);

            collided = false;
            r1.setCentered(x, y, 6f);
            for(TeamData data : Vars.state.teams.present){
                if(data.team != team){
                    if(data.unitTree != null){
                        data.unitTree.intersect(r1, u -> {
                            EmpathyDamage.damageUnit(u, 400f, u.maxHealth < 20000, null);
                            collided = true;
                        });
                    }
                    if(data.buildingTree != null){
                        data.buildingTree.intersect(r1, b -> {
                            EmpathyDamage.damageBuilding(b, 400f, b.maxHealth < 20000, null);
                            collided = true;
                        });
                    }
                }
            }

            time += Time.delta;
            if(time >= 5f * 60f || Mathf.within(x, y, targetX, targetY, 30f) || collided){
                if(collided) FlameFX.empathyDualDespawn.at(x, y, Mathf.clamp(time / 30f) * Mathf.clamp((5f * 60f - time) / 30f) * Mathf.clamp((Mathf.dst(x, y, targetX, targetY) - 30f) / 16f), dark ? Color.black : Color.white);
                remove();
            }
        }

        @Override
        public float clipSize(){
            return 100f;
        }

        @Override
        public void draw(){
            float scl = Mathf.clamp(time / 30f) * Mathf.clamp((5f * 60f - time) / 30f) * Mathf.clamp((Mathf.dst(x, y, targetX, targetY) - 30f) / 16f);

            Draw.z(Layer.bullet);
            if(!dark){
                Draw.color(Color.white);

                for(int i = 0; i < 4; i++){
                    Drawf.tri(x, y, 3f * scl, 50f * scl, i * 90f);
                }
            }else{
                Draw.color(Color.black, 0.5f);
                Fill.circle(x, y, 16f * scl);
                Draw.color(Color.black);
            }
            Fill.circle(x, y, 8f * scl);
        }

        @Override
        public void reset(){
            dark = false;
            targetX = targetY = 0f;
            x = y = 0f;
            time = 0f;
            vel.setZero();
        }

        @Override
        protected void removeGroup(){
            super.removeGroup();
            Groups.queueFree(this);
        }
    }
}
