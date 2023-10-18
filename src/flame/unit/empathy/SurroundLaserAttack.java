package flame.unit.empathy;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.audio.*;
import flame.effects.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static arc.math.Interp.*;

public class SurroundLaserAttack extends AttackAI{
    static int[] main = {
            0xe30c00ff,
            0xfe8e00ff,
            0xfbef05ff,
            0x008111ff,
            0x303bfeff,
            0x780086ff
    };
    static int[][] colors = {
            {0xd62b0080, 0xeb5210ff, 0xffa15eff, 0xffffffff, 0xf272c7ff, 0xc2388dff, 0xa1005b80},
            {0x00997d80, 0x00cc9cff, 0x9cffdbff, 0xffffffff, 0x8fc9ffff, 0x595ef0ff, 0x54009980},
            {0xe8007090, 0xff2bc6ff, 0xfc66ffff, 0xffffffff, 0xd885ffff, 0x5a44ffff, 0x2142ff80},
            {0x2ab1ff80, 0x59d3ffff, 0xffadc5ff, 0xffffffff, 0xffadc5ff, 0x59d3ffff, 0x2ab1ff80},
            {0xff198880, 0xff1988ff, 0xff746cff, 0xffe74aff, 0x8ad6deff, 0x2ea8ffff, 0x2ea8ff80},
            {0xffff1280, 0xffff32ff, 0xffffa8ff, 0xffffffff, 0xddabffff, 0x382b40ff, 0x0f0f0f80},
            {0x00000080, 0x000000ff, 0x808080ff, 0xffffffff, 0xfc88f5ff, 0x9e0094ff, 0x9e009480},
            {0xff4d7f80, 0xff87adff, 0xffbcd7ff, 0xffffffff, 0xea4affff, 0x0f0f0fff, 0x0016bf80}
    };
    static Color tmpColor = new Color();
    static Vec2 v1 = new Vec2(), v2 = new Vec2();

    float time = 0f;
    float laserTime = 0f;

    int soundIdx;
    int soundState;

    SurroundLaserAttack(){
        super();
        soundIdx = addSound(FlameSounds.empathyBigLaser);
    }

    @Override
    float weight(){
        return unit.health < 40f ? (unit.useLethal() ? 900f + unit.extraLethalScore() : super.weight() / 1.5f) : -1f;
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    void init(){
        time = 0f;
        soundState = 0;
    }

    @Override
    void update(){
        time += Time.delta;

        //float fin4 = Mathf.clamp((time - (90f + 40f + 60f + 15f)) / 6f);
        float fin4 = Mathf.clamp((time - (90f + 40f + 60f + 25f)) / 6f);
        float lfout = Mathf.clamp((15f * 60 - time) / 60f);
        int a = colors.length;

        if(soundState == 0 && time >= 90f + 40f + 60f){
            soundState = 1;
            FlameSounds.empathyCharge.play(1f);
        }
        if(soundState == 1 && time > 90 + 40 + 60 + 25){
            soundState = 2;
            FlameSounds.laserCharge.play(1f);
        }

        if(fin4 > 0f){
            if(laserTime <= 0f){
                for(int i = 0; i < a; i++){
                    float deg = (360f / a);
                    float ang1 = (deg * i) - 90f;

                    Vec2 p = Tmp.v5.trns(ang1, 80f).add(unit.x, unit.y);
                    updateLaser(p.x, p.y, ang1, fin4 * lfout);
                }
                laserTime = 5f;
            }
            laserTime -= Time.delta;

            float nearX = 0f, nearY = 0f, nearDst = 99999999f;
            float pan = 0f, panWeight = 0f;
            PassiveSoundLoop sound = sounds.get(soundIdx);
            for(int i = 0; i < a; i++){
                float deg = (360f / a);
                float ang1 = (deg * i) - 90f;

                Vec2 p = v1.trns(ang1, 80f).add(unit.x, unit.y);
                Vec2 e = v2.trns(ang1, 3000f).add(p.x, p.y);

                Vec2 pos = Intersector.nearestSegmentPoint(p, e, Core.camera.position, Tmp.v2);
                float dst = Core.camera.position.dst(pos);
                float fall = sound.calcFalloff(pos.x, pos.y);
                pan += sound.calcPan(pos.x, pos.y) * fall;
                panWeight += fall;

                if(dst < nearDst){
                    nearX = pos.x;
                    nearY = pos.y;
                    nearDst = dst;
                }
            }
            sound.play(nearX, nearY, 1f, pan / Math.max(panWeight, 0.001f), true);
        }

        if(time > (15f * 60)){
            time = 0f;
            soundState = 0;
            unit.randAI(true, unit.health < 50);
        }
    }

    @Override
    boolean updateMovementAI(){
        return false;
    }

    @Override
    boolean shouldDraw(){
        return unit.activeAttack == this;
    }

    float laserWidth(float dst){
        float start = 250f;
        float end = 700f;
        float length = 3000f;

        return circleOut.apply(Mathf.clamp(dst / start) * Mathf.clamp((length - dst) / end)) * 30f;
    }

    void updateLaser(float x, float y, float angle, float fade){
        final float widScl = 5f / 7f;
        float width = 30f;

        v1.trns(angle, 3000f).add(x, y);
        float v1x = v1.x, v1y = v1.y;
        Utils.hitLaser(unit.team, 30f * 2 * fade, x, y, v1.x, v1.y, h -> {
            Vec2 n = Intersector.nearestSegmentPoint(x, y, v1x, v1y, h.x(), h.y(), v2);
            float lwid = laserWidth(n.dst(x, y)) * widScl * fade;

            return n.within(h.x(), h.y(), lwid + (h instanceof Sized size ? size.hitSize() : 0f));
        }, h -> false, (h, hx, hy) -> {
            FlameFX.empathyRainbowHit.at(hx, hy, angle, h instanceof Sized s ? s.hitSize()/2f : 0f);

            if(h instanceof Unit u){
                Tmp.v1.trns(angle, 6f);
                u.vel.add(Tmp.v1);
                
                EmpathyDamage.damageUnit(u, Math.max(500000f, u.maxHealth / 50f), true, () -> {
                    /*
                    FlameOut.vaporBatch.switchBatch(x, y, v1x, v1y, width * 2f, u::draw, (d, within) -> {
                        if(within){
                            //d.lifetime = Mathf.random(42f, 60f);
                            d.disintegrating = true;

                            Vec2 n = Intersector.nearestSegmentPoint(x, y, v1x, v1y, d.x, d.y, CountDownAttack.vec3);
                            float dst = 1f - Mathf.clamp(n.dst(d.x, d.y) / width);
                            //float force = pow3.apply(dst) * 0.75f;
                            //float force = pow3.apply(dst) * Mathf.random(0.8f, 1.2f);
                            float force = pow2.apply(dst) * Mathf.random(1.25f, 2.5f);

                            d.lifetime = Mathf.lerp(Mathf.random(90f, 130f), Mathf.random(42f, 60f), pow2.apply(dst));

                            //if(dst < 0.00001f) return;

                            Vec2 vec2 = CountDownAttack.vec2.trns(angle, force, Mathf.range(0.07f * dst));
                            d.vx = vec2.x;
                            d.vy = vec2.y;
                            d.vr = Mathf.range(5f) * dst;
                            //d.drag = -0.1f * dst;
                            //d.drag = Mathf.random(-0.15f, -0.075f) * dst;
                            d.drag = -Mathf.random(0.035f, 0.05f) * dst;
                            d.zOverride = Layer.flyingUnit;
                        }
                    });
                     */
                    SpecialDeathEffects.get(u.type).disintegrateUnit(u, x, y, v1x, v1y, width * 2, (d, within) -> {
                        if(within){
                            d.disintegrating = true;

                            Vec2 n = Intersector.nearestSegmentPoint(x, y, v1x, v1y, d.x, d.y, CountDownAttack.vec3);
                            float dst = 1f - Mathf.clamp(n.dst(d.x, d.y) / width);
                            float force = pow2.apply(dst) * Mathf.random(1.25f, 2.5f);

                            d.lifetime = Mathf.lerp(Mathf.random(90f, 130f), Mathf.random(42f, 60f), pow2.apply(dst));

                            //if(dst < 0.00001f) return;

                            Vec2 vec2 = CountDownAttack.vec2.trns(angle, force, Mathf.range(0.07f * dst));
                            d.vx = vec2.x;
                            d.vy = vec2.y;
                            d.vr = Mathf.range(5f) * dst;
                            d.drag = -Mathf.random(0.035f, 0.05f) * dst;
                            d.zOverride = Layer.flyingUnit;
                        }
                    });
                });
            }else if(h instanceof Building b){
                EmpathyDamage.damageBuilding(b, Math.max(500000f, b.maxHealth / 50f), true, b.block.size < 3 ? null : () -> {
                    FlameOut.vaporBatch.switchBatch(x, y, v1x, v1y, width * 2f, b::draw, (d, within) -> {
                        if(within){
                            d.disintegrating = true;

                            Vec2 n = Intersector.nearestSegmentPoint(x, y, v1x, v1y, d.x, d.y, CountDownAttack.vec3);
                            float dst = 1f - Mathf.clamp(n.dst(d.x, d.y) / width);
                            float force = pow2.apply(dst) * Mathf.random(0.9f, 1.5f);

                            d.lifetime = Mathf.lerp(Mathf.random(90f, 130f), Mathf.random(42f, 60f), pow2.apply(dst));

                            Vec2 vec2 = CountDownAttack.vec2.trns(angle, force, Mathf.range(0.07f * dst));
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

    void drawLaser(float x, float y, float angle, float fade, int[] colorSet){
        int len = colorSet.length;

        for(int i = 0; i < len; i++){
            int col = colorSet[i];
            float f1 = (i / (float)len - 0.5f) * 2f;
            float f2 = ((i + 1f) / len - 0.5f) * 2f;

            Draw.color(tmpColor.rgba8888(col));

            for(int j = 0; j < 24; j++){
                float dst1 = pow2In.apply(j / 24f) * 250f;
                float dst2 = pow2In.apply((j + 1f) / 24f) * 250f;
                float wd1 = laserWidth(dst1) * fade, wd2 = laserWidth(dst2) * fade;

                Vec2 q1 = Tmp.v1.trns(angle, dst1, wd1 * f1).add(x, y);
                Vec2 q2 = Tmp.v2.trns(angle, dst2, wd2 * f1).add(x, y);
                Vec2 q3 = Tmp.v3.trns(angle, dst2, wd2 * f2).add(x, y);
                Vec2 q4 = Tmp.v4.trns(angle, dst1, wd1 * f2).add(x, y);

                Fill.quad(q1.x, q1.y, q2.x, q2.y, q3.x, q3.y, q4.x, q4.y);
            }

            float w = 30f * fade;
            float start = 250f;
            float length = 3000f - 700f;

            v1.trns(angle, 0f, w * f1);
            v2.trns(angle, 0f, w * f2);
            float s1x = v1.x, s1y = v1.y, s2x = v2.x, s2y = v2.y;
            v1.trns(angle, start);
            v2.trns(angle, length);

            Fill.quad(x + v1.x + s1x, y + v1.y + s1y, x + v2.x + s1x, y + v2.y + s1y, x + v2.x + s2x, y + v2.y + s2y, x + v1.x + s2x, y + v1.y + s2y);

            for(int j = 0; j < 32; j++){
                float dst1 = length + pow2Out.apply(j / 32f) * 700f;
                float dst2 = length + pow2Out.apply((j + 1f) / 32f) * 700f;
                float wd1 = laserWidth(dst1) * fade, wd2 = laserWidth(dst2) * fade;

                Vec2 q1 = Tmp.v1.trns(angle, dst1, wd1 * f1).add(x, y);
                Vec2 q2 = Tmp.v2.trns(angle, dst2, wd2 * f1).add(x, y);
                Vec2 q3 = Tmp.v3.trns(angle, dst2, wd2 * f2).add(x, y);
                Vec2 q4 = Tmp.v4.trns(angle, dst1, wd1 * f2).add(x, y);

                Fill.quad(q1.x, q1.y, q2.x, q2.y, q3.x, q3.y, q4.x, q4.y);
            }
        }
    }

    void drawFlash(float x, float y, float fin){
        float fout = 1 - fin;
        float f = pow2In.apply(Mathf.curve(fin, 0f, 0.1f));
        float fo = Mathf.curve(fout, 0.4f, 1f);
        float f2 = pow2Out.apply(Mathf.curve(fin, 0.1f, 0.75f));
        float scl = 1f;

        Draw.color();
        for(int i = 0; i < 4; i++){
            float r = i * 90f;
            Drawf.tri(x, y, 5f * fo * scl, (5f + 120f * f) * fo * scl, r);
        }
        for(int i = 0; i < 2; i++){
            float r = i * 180f;
            Drawf.tri(x, y, 7f * fout * scl, (7f + 310f * f2) * scl, r);
        }
    }

    @Override
    void draw(){
        int a = colors.length;
        float fin1 = pow2.apply(Mathf.clamp(time / 60f));
        float fin2 = pow2.apply(Mathf.clamp((time - 90f) / 40f)), fout2 = 1f - fin2;
        float fin2I = pow2Out.apply(fin2), fout2I = 1f - fin2I;
        float fin3 = Mathf.clamp((time - (90f + 40f + 60f)) / 15f);
        //float fin4 = Mathf.clamp((time - (90f + 40f + 60f + 15f)) / 6f);
        float fin4 = Mathf.clamp((time - (90f + 40f + 60f + 25f)) / 6f);
        //float fin2I2 = 1f - Mathf.pow((1f - fin2), 1.125f);
        float fout = 1f - Mathf.clamp((time - (90 + 40f + 30f)) / 30f);
        float foutI = pow3Out.apply(fout);
        float lfout = Mathf.clamp((15f * 60 - time) / 60f);

        if(fout > 0.0001f){
            for(int i = 0; i < a; i++){
                float deg = (360f / a);
                float ang1 = (deg * i * fin1) - 90f + 180f * (1 - fin1);
                //float ang1 = ((deg * i * fin1) - 90f + 180f * (1 - fin1)) - ((180f / a) * fin2);
                //float ang1 = ((deg * i * fin1) - 90f) - 180f * (1 - fin1);
                //Vec2 npos = Tmp.v5.trns(ang1, 150f).scl(fin2I);
                Vec2 npos = Tmp.v5.trns(ang1, Mathf.lerp(150f, 80f, pow2.apply(pow3In.apply(fin2)))).scl(fin2I);
                for(int j = 0; j < 32; j++){
                    float ang2A = (deg / 32f) * fin1 * j * fout2 + ang1;
                    float ang2B = (deg / 32f) * fin1 * (j + 1f) * fout2 + ang1;
                    float ang3A = (j / 32f - 0.5f) * 360f * fin2, ang3B = ((j + 1f) / 32f - 0.5f) * 360f * fin2;
                    
                    //float dip1 = Interp.pow2Out.apply(Mathf.slope(fin2)) * (j / 32f) * 50f;
                    //float dip2 = Interp.pow2Out.apply(Mathf.slope(fin2)) * ((j + 1f) / 32f) * 50f;

                    //v1.trns(ang2A, 200f).add(unit);
                    //v2.trns(ang2B, 200f).add(unit);
                    v1.trns(ang2A + ang3A, 150f * fout2I).add(npos).add(unit);
                    v2.trns(ang2B + ang3B, 150f * fout2I).add(npos).add(unit);
                    float x1 = v1.x, y1 = v1.y, x2 = v2.x, y2 = v2.y;

                    v1.trns(ang2A + ang3A, 20f * foutI);
                    v2.trns(ang2B + ang3B, 20f * foutI);

                    for(int k = 0; k < main.length; k++){
                        int col = main[(main.length - 1) - k];
                        float f1 = k / (float)main.length;
                        float f2 = (k + 1f) / main.length;

                        Draw.color(tmpColor.rgba8888(col));

                        Vec2 q1 = Tmp.v1.setZero().lerp(v1, f1).add(x1, y1);
                        Vec2 q2 = Tmp.v2.setZero().lerp(v1, f2).add(x1, y1);
                        Vec2 q3 = Tmp.v3.setZero().lerp(v2, f2).add(x2, y2);
                        Vec2 q4 = Tmp.v4.setZero().lerp(v2, f1).add(x2, y2);

                        Fill.quad(q1.x, q1.y, q2.x, q2.y, q3.x, q3.y, q4.x, q4.y);
                    }
                }
            }
        }
        if(fin4 > 0f){
            for(int i = 0; i < a; i++){
                float deg = (360f / a);
                float ang1 = (deg * i) - 90f;

                Vec2 p = Tmp.v5.trns(ang1, 80f).add(unit.x, unit.y);
                drawLaser(p.x, p.y, ang1, fin4 * lfout * (1f + Mathf.absin(2f, 0.12f)), colors[i]);
            }
        }

        Draw.color();
        float z = Draw.z();
        Draw.z(Layer.flyingUnit + 0.1f);
        if(fin3 > 0.0001f && fin3 < 0.999f){
            for(int i = 0; i < a; i++){
                float deg = (360f / a);
                float ang1 = (deg * i) - 90f;

                Vec2 p = Tmp.v5.trns(ang1, 80f).add(unit.x, unit.y);
                drawFlash(p.x, p.y, fin3);
            }
        }
        Draw.z(z);
    }
}
