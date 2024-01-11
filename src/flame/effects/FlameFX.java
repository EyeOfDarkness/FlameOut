package flame.effects;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.graphics.*;
import flame.graphics.CutBatch.*;
import flame.unit.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.math.Interp.*;
import static arc.graphics.g2d.Draw.*;

public class FlameFX{
    public static Effect shield = new Effect(30f, e -> {
        blend(Blending.additive);
        color(Tmp.c1.set(FlamePal.primary).a(Mathf.absin(e.fin(pow2Out), 1f / 50f, 1f) * 0.5f * e.fout()));

        Fill.polyBegin();
        for(int i = 0; i < 6; i++){
            float ang = i * (360f / 6f);
            Tmp.v1.trns(ang, 30f);
            Tmp.v1.y *= 0.333f;

            Vec2 v = Tmp.v2.trns(e.rotation + 90f, Tmp.v1.x, Tmp.v1.y).add(e.x, e.y);
            Fill.polyPoint(v.x, v.y);
        }
        Fill.polyEnd();

        blend();
    }),

    aoeExplosion2 = new Effect(80f, 500f, e -> {
        float z = z();
        z(z - 0.001f);

        Rand r = Utils.rand;
        r.setSeed(e.id * 31L);

        color(Color.gray);
        alpha(0.9f);
        for(int i = 0; i < 3; i++){
            float lenScl = r.random(0.4f, 1f);
            float time = Mathf.clamp(e.time / (e.lifetime * lenScl));

            float l = pow10Out.apply(time) * 100f;

            for(int j = 0; j < 4; j++){
                float len = r.random(0.4f, 1f) * l;
                float ang = r.random(360f);
                float fout = Interp.pow5Out.apply(1 - time) * r.random(0.5f, 1f);

                Vec2 v = Tmp.v1.trns(ang, len).add(e.x, e.y);
                //Fill.circle(e.x + x, e.y + y, fout * ((2f + intensity) * 1.8f));
                Fill.circle(v.x, v.y, fout * 60f);
            }
        }

        //color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        //stroke((1.7f * e.fout()) * (1f + (intensity - 1f) / 2f));
        z(z);
        color(FlamePal.primary, Pal.lightOrange, Color.gray, e.fin());
        Lines.stroke(2.72f * e.fout());
        for(int i = 0; i < 8; i++){
            //float c = r.random(0.2f);
            float l = r.random(20f, 150f) * e.finpow() + 0.1f;
            float a = r.random(360f);
            Vec2 v = Tmp.v1.trns(a, l);
            //lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * (3f + intensity));
            Lines.lineAngle(v.x + e.x, v.y + e.y, Mathf.angle(v.x, v.y), 1f + e.fout() * 12f);
            //Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * 3.5f, Draw.getColor(), 0.8f);
            Drawf.light(e.x + v.x, e.y + v.y, 11f * e.fout(), Draw.getColor(), 0.8f);
        }

        color(Color.white);
        if(e.time < 3f){
            Fill.circle(e.x, e.y, e.rotation);
            Drawf.light(e.x, e.y, e.rotation * 2.5f, Color.white, 0.9f);
        }
    }),

    apathyCrit = new Effect(80f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id * 31L);
        for(int i = 0; i < 45; i++){
            //
            float offd = 0.4f;

            float ra = pow3Out.apply(r.random(1f)) / 2f + 0.5f;

            float in = (i / 45f) * ra * (1 - offd);

            //float of = r.random(1f - offd);
            //float time = Mathf.curve(e.fin(), of, of + offd);
            float time = Mathf.curve(e.fin(), in, in + offd);
            float angle = r.random(360f);
            float length = r.random(15f, 135f);
            float size = r.random(12f, 25f);

            if(time <= 0 || time >= 1) continue;

            Vec2 v = Tmp.v1.trns(angle, length * pow2In.apply(time)).add(e.x, e.y);
            color(FlamePal.primary, FlamePal.blood, pow2.apply(time));
            Fill.circle(v.x, v.y, size * pow2Out.apply(slope.apply(pow2In.apply(time))));
        }

    }).layer(Layer.flyingUnit + 0.01f),
    apathyBleed = new Effect(15f, e -> {
        //Draw.color(FlamePalettes.primary, FlamePalettes.blood, pow2Out.apply(e.fin()));
        color(FlamePal.blood);
        Rand r = Utils.rand;
        r.setSeed(e.id);
        float minRange = e.color.r;
        float maxRange = e.color.g;

        for(int i = 0; i < 6; i++){
            float angle = e.rotation + pow2In.apply(r.nextFloat()) * (r.chance(0.5f) ? -1f : 1f) * 15f;
            float len = r.random(minRange, maxRange) * e.fin(pow2Out);
            float s = r.random(6f, 10f) * pow3Out.apply(e.fout());

            Tmp.v1.trns(angle, len).add(e.x, e.y);
            Fill.circle(Tmp.v1.x, Tmp.v1.y, s);
        }
    }).rotWithParent(true).layer(Layer.flyingUnit + 0.01f),
    apathyDeath = new Effect(30f, e -> {
        color(FlamePal.blood);
        Rand r = Utils.rand;
        r.setSeed(e.id);

        Fill.circle(e.x, e.y, (1f - Mathf.curve(e.fin(), 0f, 0.4f)) * e.rotation * 2f);

        for(int i = 0; i < 70; i++){
            float fin = Mathf.curve(e.fin(), r.random(0.1f), 1 - r.random(0.5f));
            float angle = r.random(360f);
            float length = r.random(220f, 460f);
            float size = r.random(9f, 15f) * pow2Out.apply(Utils.biasSlope(fin, 0.1f));
            float offset = r.random(e.rotation);

            if(fin > 0f && fin < 1f){
                Tmp.v1.trns(angle, offset + length * pow3Out.apply(fin)).add(e.x, e.y);
                GraphicUtils.tri(Tmp.v1.x, Tmp.v1.y, e.x, e.y, size, angle);
                Drawf.tri(Tmp.v1.x, Tmp.v1.y, size, size * 2f, angle);
            }
        }
    }),

    bigLaserCharge = new Effect(120f, e -> {
        color();
        float scl = (1f + Mathf.absin(e.fin(pow2In), 1f / 100f, 1f)) * 180f * e.fin();

        for(int i = 0; i < 4; i++){
            float a = (360 / 4f) * i + 45f;

            Drawf.tri(e.x, e.y, (scl + 5) / 8f, scl, a);
        }
    }).layer(Layer.flyingUnit + 0.01f),
    bigLaserFlash = new Effect(8f, e -> {
        //
        color();
        float scl = 180f + 280f * e.finpow();
        
        for(int i = 0; i < 4; i++){
            float a = (360 / 4f) * i + 45f;

            Drawf.tri(e.x, e.y, 40 * pow3Out.apply(e.fout()), scl, a);
        }
    }).layer(Layer.flyingUnit + 0.01f),
    bigLaserHitSpark = new Effect(15f, e -> {
        color(Color.white, FlamePal.primary, e.fin());
        Lines.stroke(e.fout() * 1.2f + 0.5f);

        Angles.randLenVectors(e.id, 8, 87f * e.fin(), e.rotation, 45f, (x, y) -> {
            Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 9f + 0.5f);
        });

        Rand r = Utils.rand;
        r.setSeed(e.id + 642);
        float c = 0.4f;
        for(int i = 0; i < 6; i++){
            float id = i / 5f;
            float f = Mathf.curve(e.fin(), c * id, c * id + (1 - c));
            float ang = e.rotation + r.range(60f);
            float len = r.random(57f, 92f) * pow2Out.apply(f);
            float size = r.random(5f, 9f) * (1 - f);
            if(f > 0.001f){
                color(Color.white, FlamePal.primary, f);
                Vec2 v = Tmp.v1.trns(ang, len);

                Fill.poly(e.x + v.x / 2, e.y + v.y / 2, 4, size / 2);
                Fill.poly(e.x + v.x, e.y + v.y, 4, size);
            }
        }
    }),
    bigLaserHit = new Effect(30f, e -> {
        color(Color.white, FlamePal.primary, Color.gray, pow2Out.apply(e.fin()));

        //float size = e.data instanceof Float ? ((float)e.data) / 2f : 50f;
        float size = (e.data instanceof Float ? ((float)e.data) : (e.data instanceof Sized s ? s.hitSize() : 50f)) * 1.25f;

        Rand r = Utils.rand;
        r.setSeed(e.id);
        for(int i = 0; i < 16; i++){
            float w = r.range(size);
            float l = r.random(180f, 310f);
            float s = r.random(8f, 30f);

            float ic = i / 15f;
            float c = 0.3f;
            float f = Mathf.curve(e.fin(), ic * c, (ic * c) + (1 - c));

            if(f >= 0.0001f && f < 1f){
                Vec2 v = Tmp.v1.trns(e.rotation, l * pow3In.apply(f), w * circleOut.apply(pow3In.apply(f))).add(e.x, e.y);
                Fill.circle(v.x, v.y, s * (1 - (f * f)));
            }
        }
    }),

    rejectedRegion = new Effect(15f, 600f, e -> {
        if(!(e.data instanceof RejectedRegion r)) return;
        float z = Draw.z();
        Draw.z(r.z);
        Draw.color(e.color, e.fout() * e.color.a);
        Draw.blend(r.blend);

        Draw.rect(r.region, e.x, e.y, r.width, r.height, e.rotation);

        Draw.blend();
        Draw.z(z);
    }),

    shootShockWave = new Effect(35f, 600f, e -> {
        //GraphicUtils.drawShockWave(e.x, e.y, 75f, 0f, -e.rotation - 90f, 200f, 4f, 12);
        color(Color.white);
        alpha(0.666f * e.fout());

        float size = e.data instanceof Float ? (float)e.data : 200f;
        float nsize = size - 10f;

        GraphicUtils.drawShockWave(e.x, e.y, -75f, 0f, -e.rotation - 90f, nsize * e.finpow() + 10, 16f * e.finpow() + 4f, 16, 1f);
    }).layer((Layer.bullet + Layer.effect) / 2),

    fragmentGroundImpact = new Effect(40f, 300f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        color(e.color);

        float size = e.rotation;
        int iter = ((int)(size / 8f)) + 6;
        for(int i = 0; i < iter; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size) + (r.random(0.5f, 1f) * size * 0.5f + 20f) * e.finpow()).add(e.x, e.y);
            Fill.circle(v.x, v.y, r.random(5f, 16f) * e.fout());
        }
    }).layer(Layer.debris),
    fragmentExplosion = new Effect(40f, 300f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);
        float size = e.rotation;
        e.lifetime = size / 1.5f + 10f;

        int iter = ((int)(size / 7f)) + 12;
        int iter3 = ((int)(size / 14.5f)) + 12;
        color(Color.gray);
        //alpha(0.9f);
        for(int i = 0; i < iter3; i++){
            //
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size / 2f) * e.finpow());
            float s = r.random(size / 2.75f, size / 2f) * e.fout();
            Fill.circle(v.x + e.x, v.y + e.y, s);
        }
        for(int i = 0; i < iter; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size) + (r.random(0.25f, 2f) * size) * e.finpow());
            float s = r.random(size / 3.5f, size / 2.5f) * e.fout();
            Fill.circle(v.x + e.x, v.y + e.y, s);
            Fill.circle(v.x / 2 + e.x, v.y / 2 + e.y, s * 0.5f);
        }

        float sfin = Mathf.curve(e.fin(), 0f, 0.65f);
        if(sfin < 1f){
            int iter2 = ((int)(size / 10f)) + 4;
            float sfout = 1f - sfin;

            color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
            Lines.stroke((1.7f * sfout) * (1f + size / 60f));

            Draw.z(Layer.effect + 0.001f);

            for(int i = 0; i < iter2; i++){
                Vec2 v = Tmp.v1.trns(r.random(360f), r.random(0.001f, size / 2f) + (r.random(0.4f, 2.2f) * size) * pow2Out.apply(sfin));
                Lines.lineAngle(e.x + v.x, e.y + v.y, Mathf.angle(v.x, v.y), 1f + sfout * 3 * (1f + size / 50f));
            }
        }
    }),

    fragmentExplosionSmoke = new Effect(40f, 300f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);
        float size = e.rotation;
        
        e.lifetime = size / 1.5f + 10f;

        int iter = ((int)(size / 7f)) + 12;
        int iter3 = ((int)(size / 14.5f)) + 12;
        color(Color.gray);
        for(int i = 0; i < iter3; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size / 2f) * e.finpow());
            float s = r.random(size / 2.75f, size / 2f) * e.fout();
            Fill.circle(v.x + e.x, v.y + e.y, s);
        }
        for(int i = 0; i < iter; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size) + (r.random(0.25f, 2f) * size) * e.finpow());
            float s = r.random(size / 3.5f, size / 2.5f) * e.fout();
            Fill.circle(v.x + e.x, v.y + e.y, s);
            Fill.circle(v.x / 2 + e.x, v.y / 2 + e.y, s * 0.5f);
        }
    }),

    fragmentExplosionSpark = new Effect(26f, 300f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);
        float size = e.rotation;
        e.lifetime = size / 1.5f + 10f;

        float sfin = e.fin();

        int iter2 = ((int)(size / 12f)) + 3;
        float sfout = 1f - sfin;

        color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        Lines.stroke((1.7f * sfout) * (1f + size / 60f));

        Draw.z(Layer.effect + 0.001f);

        for(int i = 0; i < iter2; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(0.001f, size / 2f) + (r.random(0.4f, 2.2f) * size) * pow2Out.apply(sfin));
            Lines.lineAngle(e.x + v.x, e.y + v.y, Mathf.angle(v.x, v.y), 1f + sfout * 3 * (1f + size / 50f));
        }
    }),

    destroySparks = new Effect(40f, 1200f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id + 64331);
        float size = (float)e.data;
        int isize = (int)(size * 1.75f) + 12;
        int isize2 = (int)(size * 1.5f) + 9;

        float fin1 = Mathf.clamp(e.time / 20f);
        float fin2 = Mathf.clamp(e.time / 10f);

        Lines.stroke(Math.max(2f, Mathf.sqrt(size) / 8f));
        for(int i = 0; i < isize2; i++){
            float f = Mathf.curve(fin1, 0f, r.random(0.8f, 1f));
            Vec2 v = Tmp.v1.trns(r.random(360f), 1f + (size * r.nextFloat() + 10f) * 1.5f * pow3Out.apply(f));
            float rsize = r.random(0.5f, 1.5f);
            if(f < 1){
                color(FlamePal.paleYellow, Pal.lightOrange, Color.gray, f);
                Lines.lineAngle(v.x + e.x, v.y + e.y, v.angle(), (size / 5f) * rsize * (1 - f));
            }
        }
        for(int i = 0; i < isize; i++){
            float f = Mathf.curve(e.fin(), 0f, r.random(0.5f, 1f));
            float re = Mathf.pow(r.nextFloat(), 1.5f);
            float ang = re * 90f * (r.nextFloat() > 0.5f ? 1 : -1);
            //float dst = (1f - Math.abs(ang / 90f) / 1.5f) * (50f + size * 3f * r.nextFloat()) * pow3Out.apply(f);
            float dst = (50f + ((size * 3f) / (1f + re / 5f)) * Mathf.pow(r.nextFloat(), (1f + re / 2f))) * Interp.pow3Out.apply(f);
            Vec2 v = Tmp.v1.trns(e.rotation + ang, 1f + dst);
            float rsize = r.random(0.75f, 1.5f);

            if(f < 1){
                color(FlamePal.paleYellow, Pal.lightOrange, Color.gray, pow2In.apply(f));
                Lines.lineAngle(v.x + e.x, v.y + e.y, v.angle(), (size / 3f) * rsize * (1 - f));
            }
        }

        color(FlamePal.paleYellow);
        for(int i = 0; i < 4; i++){
            float rot = i * 90f;
            Drawf.tri(e.x, e.y, (size / 2.5f) * (1 - fin2), size + size * fin2 * 1f, rot);
        }
    }).layer(Layer.effect + 0.005f),
    debrisSmoke = new Effect(40f, e -> {
        color(Color.gray);
        float fin = Utils.biasSlope(e.fin(), 0.075f);
        Fill.circle(e.x, e.y, e.rotation * fin);
    }),
    heavyDebris = new Effect(4f * 60f, 1200f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id + 644331);
        float size = (float)e.data;
        float sizeTime = (size) + 15f;
        int isize = (int)(size * 1.75f) + 12;

        float fin = Mathf.clamp(e.time / sizeTime);
        float fout = Mathf.clamp((e.lifetime - e.time) / 60f);
        Lines.stroke(3f);
        for(int i = 0; i < isize; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), Mathf.sqrt(r.nextFloat()) * size * 0.75f).add(e.x, e.y);
            float f = Mathf.curve(fin, 0f, r.random(0.5f, 1f));
            float angle = Mathf.pow(r.nextFloat(), 1.25f) * (r.random(1f) < 0.5f ? -1f : 1f) * 60f;
            //float angle = r.range(35f);
            float dst = r.random((220f + size * 4.5f) * pow3Out.apply(f)) * (1 - Math.abs(angle / 60f) / 1.5f);
            float s = r.chance(0.25f) ? (size / 3f) * r.random(0.5f, 1f) : Math.min(r.random(5f, 9f), size / 4f);
            float rrot = r.random(360f);
            int sides = r.random(3, 6);
            Vec2 v2 = Tmp.v2.trns(angle + e.rotation, dst);

            Draw.color(Tmp.c1.set(e.color).mul(r.random(0.9f, 1.2f)).a(fout));

            if(r.chance(0.75f)){
                Fill.poly(v.x + v2.x, v.y + v2.y, sides, s, rrot);
            }else{
                Lines.poly(v.x + v2.x, v.y + v2.y, sides, s, rrot);
            }
        }

    }).layer(Layer.debris - 0.01f),
    simpleFragmentation = new Effect(30f, e -> {
        if(!(e.data instanceof TextureRegion region)) return;
        float bounds = Math.min(region.width, region.height);
        float b2 = bounds / 4f;
        float bw = b2 / region.texture.width;
        float bh = b2 / region.texture.height;
        float bscl = bounds * scl;
        int ib = (int)(bscl * 1.5f) + 8;
        Rand r = Utils.rand;
        r.setSeed(e.id + 46241);

        Draw.color(e.color);
        for(int i = 0; i < ib; i++){
            float u = Mathf.lerp(region.u, (region.u2 - bw), r.nextFloat());
            float v = Mathf.lerp(region.v, (region.v2 - bh), r.nextFloat());
            float u2 = u + bw;
            float v2 = v + bh;

            TextureRegion tr = Tmp.tr1;
            tr.texture = region.texture;
            tr.set(u, v, u2, v2);

            float f = Mathf.curve(e.fin(), 0f, r.random(0.8f, 1f));

            Vec2 base = Tmp.v1.trns(r.random(360f), bscl / 2f).add(e.x, e.y);
            Vec2 off = Tmp.v2.trns(e.rotation + r.range(30f), 120f * r.nextFloat() * pow2Out.apply(f));

            float rrot = r.random(360f) + r.range(180f) * f;

            if(f < 1){
                Draw.alpha(1f - Mathf.curve(f, 0.8f, 1f));
                Draw.rect(tr, base.x + off.x, base.y + off.y, rrot);
            }
        }
    }).layer(Layer.flyingUnitLow),

    empathyTrail = new Effect(20f, e -> {
        TextureRegion r = FlameUnitTypes.empathy.region;
        color(Color.white);
        alpha(0.75f * e.fout());
        mixcol(FlamePal.empathyAdd, 1f);
        blend(Blending.additive);
        Draw.rect(r, e.x, e.y, e.rotation - 90f);
        blend();

        e.lifetime = 20f + e.color.r;
    }),
    
    empathyDecoyDestroy = new Effect(90f, 700f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);
        
        color(FlamePal.empathy);
        Lines.stroke(12f * e.fout());
        Lines.circle(e.x, e.y, 6f + 160f * e.fin());
        
        Lines.stroke(2f * e.fout());
        for(int i = 0; i < 10; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), r.random(45f, 230f) * e.finpow()).add(e.x, e.y);

            Lines.line(e.x, e.y, v.x, v.y, false);
            Fill.poly(v.x, v.y, 4, 3f * e.fout());
        }
    }),

    empathyParry = new Effect(8f, e -> {
        color();
        float scl = 20f + 30f * e.finpow();

        for(int i = 0; i < 4; i++){
            float a = (360 / 4f) * i;

            Drawf.tri(e.x, e.y, 8 * pow3Out.apply(e.fout()), scl, a);
        }
    }).layer(Layer.flyingUnit + 0.01f),
    empathyParryExplosion = new Effect(40f, e -> {
        color(FlamePal.empathyDark, e.fout());
        blend(Blending.additive);
        float r = pow3Out.apply(Mathf.clamp(e.time / 6f)) * e.rotation + e.finpow() * 10f;
        Fill.circle(e.x, e.y, r);
        blend();
    }).layer(Layer.flyingUnitLow + 1f),

    empathyPrimeStrike = new Effect(40f, 300f, e -> {
        Rand rand = Utils.rand;
        rand.setSeed(e.id + 45245);
        float rrot = 90f + (rand.random(15f, 180f - 15f) * (rand.nextFloat() >= 0.5f ? 1 : -1));
        float exLength = rand.random(8f, 25f);

        Tmp.c1.set(FlamePal.empathyAdd).a(Mathf.clamp((e.lifetime - e.time) / 30f));
        color(Tmp.c1);
        blend(Blending.additive);

        float fin = Mathf.clamp(e.time / 5f);
        GraphicUtils.draw3D(e.x, e.y, rand.range(40f), rrot, -e.rotation + 90f, fs -> {
            for(int i = 0; i < 16; i++){
                float f1 = (i / 16f);
                float f2 = ((i + 1) / 16f);

                float rot = f1 * 180f * fin;
                float nrot = f2 * 180f * fin;
                float width1 = f1 * 17f;
                float width2 = f2 * 17f;

                //float ex1 = Mathf.slope(f1) * 10f;
                //float ex2 = Mathf.slope(f2) * 10f;

                for(int j = 0; j < 2; j++){
                    float r = j == 0 ? rot : nrot;
                    float w = j == 0 ? width1 : width2;
                    float ex = pow2Out.apply(Mathf.slope(j == 0 ? f1 : f2)) * exLength;
                    for(int k = 0; k < 2; k++){
                        int sign = j == 0 ? k : 1 - k;
                        Vec2 v = Tmp.v1.trns(r, 30f + w * -sign).add(0, ex);
                        fs.add(v.x, v.y);
                    }
                }
            }
        });
        blend();
    }),
    empathyDashShockwave = new Effect(10f, 300f, e -> {
        color(Color.white);
        alpha(0.666f * e.fout());

        float size = 60f;
        float nsize = size - 15f;

        GraphicUtils.drawShockWave(e.x, e.y, -75f, 0f, -e.rotation - 90f, nsize * e.finpow() + 15, 30f * e.finpow() + 4f, 16, 1f);
    }),
    empathyDashDust = new Effect(3f * 60f, 150, e -> {
        float fin = Mathf.clamp(e.time / 25f);
        float fout = Mathf.clamp((e.lifetime - e.time) / 60f);

        Rand r = Utils.rand;
        r.setSeed(e.id);

        color(e.color);
        for(int i = 0; i < 3; i++){
            int sign = Mathf.sign(r.nextBoolean());
            float size = r.random(3f, 7f);
            //Vec2 v = Tmp.v1.trns(e.rotation + 90f * sign + r.range(25f), r.random(45f) * pow2Out.apply(fin)).add(e.x, e.y);
            Vec2 v = Tmp.v1.trns(e.rotation + 90f * sign + r.range(25f), Mathf.pow(r.nextFloat(), 1.75f) * 45f * pow2Out.apply(fin)).add(e.x, e.y);
            Fill.circle(v.x, v.y, size * fout);
        }

    }).layer(Layer.scorch + 5f),
    empathyPrimeShockwave = new Effect(40f, 450f, e -> {
        color(Color.white);
        alpha(0.666f * e.fout());
        Rand r = Utils.rand;
        r.setSeed(e.id);

        float size = 200f;
        float nsize = size - 15f;

        GraphicUtils.drawShockWave(e.x, e.y, 90f - r.random(5f, 15f), 0f, -e.rotation - 90f, nsize * e.finpow() + 15, 30f * e.finpow() + 5f, 16, 1f);
    }).layer(Layer.flyingUnit),
    empathyPrimeHit = new Effect(12f, 800f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id + 3511);
        float fin = pow10Out.apply(e.fin());
        float fout = pow2Out.apply(e.fout());

        color(FlamePal.empathy, e.fout());
        float exp = e.fin() * 15f;
        Draw.rect(EmpathyRegions.hcircle, e.x, e.y, 180f + exp * 3f, 110f + exp, e.rotation);

        color(FlamePal.empathy);
        
        for(int i = 0; i < 8; i++){
            float angle = Mathf.pow(r.nextFloat(), 1.5f) * 100f * (r.nextFloat() > 0.5f ? -1 : 1);
            float dst = Mathf.pow(1f - Math.abs(angle / 100f) * 0.8f, 2f);
            
            Tmp.v1.trns(e.rotation + angle, 5f + r.range(5f)).add(e.x, e.y);
            
            float len = r.random(50f, 110f);
            float wid = r.random(14f, 20.5f);
            //GraphicUtils.diamond(Tmp.v1.x, Tmp.v1.y, r.random(10f, 14.5f) * dst * fout, r.random(40f, 60f) * dst * fin, e.rotation + angle);
            Drawf.tri(Tmp.v1.x, Tmp.v1.y, wid * fout, 2f * len * dst * fin, e.rotation + angle);
            Drawf.tri(Tmp.v1.x, Tmp.v1.y, wid * fout, 0.25f * len * dst * fin, e.rotation + angle + 180f);
        }
        float randrot = r.random(360f);
        for(int i = 0; i < 7; i++){
            float ang = ((360f / 7f) * i) + r.range((180f / 7f) / 1.5f) + randrot;
            float len = r.random(40f, 90f);
            float wid = r.random(10f, 15f);
            Drawf.tri(e.x, e.y, wid * fout, len * fin, e.rotation + ang);
        }
        Lines.stroke(2f * fout);
        for(int i = 0; i < 12; i++){
            float angle = Mathf.pow(r.nextFloat(), 1.5f) * 90f * (r.nextFloat() > 0.5f ? -1 : 1);
            float dst = Mathf.pow(1f - Math.abs(angle / 90f) * 0.8f, 2f);
            
            Tmp.v1.trns(e.rotation + angle, 30f + r.range(5f)).add(e.x, e.y);
            float len = r.random(180f, 320f) * dst;
            /*
            float wid = r.random(4f, 5f);
            Drawf.tri(Tmp.v1.x, Tmp.v1.y, wid * fout, len, e.rotation + angle);
            Drawf.tri(Tmp.v1.x, Tmp.v1.y, wid * fout, 5f, e.rotation + angle + 180f);
            */
            //Lines.lineAngle(Tmp.v1.x, Tmp.v1.y, len * fin, angle);
            Lines.lineAngle(Tmp.v1.x, Tmp.v1.y, e.rotation + angle, len * fin);
        }

        color();
        
        Drawf.tri(e.x, e.y, 8f * fout, 60f + 90f * fin, e.rotation + 90f);
        Drawf.tri(e.x, e.y, 8f * fout, 60f + 90f * fin, e.rotation - 90f);
        
        for(int i = 0; i < 4; i++){
            float angle = r.range(75f);

            float width = (1f - Math.abs(angle / 75f) * 0.45f) * 90f * r.random(0.75f, 1.25f);
            float rwid = r.random(0.9f, 1.1f);
            int iwid = (int)(width / 9f) + 5;

            for(int j = 0; j < 3; j++){
                float offset = r.range(width / 2f);
                float ff = 1f - (Math.abs(offset) / (width / 2f)) * 0.75f;
                float dst = (1f - Math.abs((angle + offset) / (75f + width / 2))) + 0.5f;

                Tmp.v1.trns(e.rotation + angle + offset, 30f).add(e.x, e.y);
                GraphicUtils.diamond(Tmp.v1.x, Tmp.v1.y, ff * r.random(3f, 4.5f) * fout, ff * r.random(15f, 23f) * dst * fin, e.rotation + angle + offset);
                //Drawf.tri(Tmp.v1.x, Tmp.v1.y, size, size * 2f, angle);
                //float len = r.random(15f, 23f);
                //float wid = r.random(3f, 4.5f);
                //Drawf.tri(Tmp.v1.x, Tmp.v1.y, ff * wid * fout, ff * len * dst * fin, e.rotation + angle + offset);
                //Drawf.tri(Tmp.v1.x, Tmp.v1.y, ff * wid * fout, ff * 0.25f * len * dst * fin, e.rotation + angle + offset + 180f);
            }

            for(int j = 0; j < iwid; j++){
                /*
                float ww = j / (iwid - 1f);
                float w = Mathf.slope(ww) * width * 0.2f * rwid;
                int side = j % 2;
                float w1 = side == 0 ? w : 0f;
                float w2 = side == 1 ? w : 0f;

                Tmp.v1.trns(e.rotation + (angle - width / 2f) + width * ww, 30f - w1).add(e.x, e.y);
                Tmp.v2.trns(e.rotation + (angle - width / 2f) + width * ww, 30f - w2).add(e.x, e.y);

                Fill.polyPoint(Tmp.v1.x, Tmp.v1.y);
                Fill.polyPoint(Tmp.v2.x, Tmp.v2.y);
                */
                
                Fill.polyBegin();
                for(int k = 0; k < 2; k++){
                    float ww = (j + k) / (float)iwid;
                    float w = pow2Out.apply(Mathf.slope(ww)) * width * 0.035f * rwid * fin * fout;
                    int side = k % 2;
                    float w1 = side == 0 ? w : 0f;
                    float w2 = side == 1 ? w : 0f;
                    
                    Tmp.v1.trns(e.rotation + (angle - width / 2f) + width * ww, 30f - w1).add(e.x, e.y);
                    Tmp.v2.trns(e.rotation + (angle - width / 2f) + width * ww, 30f - w2).add(e.x, e.y);

                    Fill.polyPoint(Tmp.v1.x, Tmp.v1.y);
                    Fill.polyPoint(Tmp.v2.x, Tmp.v2.y);
                }
                Fill.polyEnd();
            }
        }
    }).layer(Layer.flyingUnit + 0.01f),

    empathyShotgun = new Effect(6, 1200f, e -> {
        if(!(e.data instanceof Float)) return;
        Draw.color(FlamePal.empathy);
        float l = (float)e.data;
        e.lifetime = Math.max(l / (500f / 6f), 2f);
        Tmp.v1.trns(e.rotation, l).add(e.x, e.y);
        Lines.stroke(2f);
        Lines.lineAngle(Tmp.v1.x, Tmp.v1.y, e.rotation + 180f, l * e.fout());
    }),

    empathyRico = new Effect(30f, 4000f, e -> {
        if(!(e.data instanceof Float)) return;
        Draw.color(FlamePal.empathy);
        float l = (float)e.data;
        Lines.stroke(4f * e.fout());
        Lines.lineAngle(e.x, e.y, e.rotation, l);
    }),

    empathyLightningHit = new Effect(14f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        color(Color.white, FlamePal.empathy, e.fin());
        Lines.stroke(0.5f + e.fout());
        for(int i = 0; i < 7; i++){
            float rot = e.rotation + r.range(35f);
            float len = r.random(20f) * e.fin();
            Tmp.v1.trns(rot, len).add(e.x, e.y);
            Lines.lineAngle(Tmp.v1.x, Tmp.v1.y, rot, 4.5f * e.fout() + 1f);
        }
    }),

    empathyRendHit = new Effect(20f, 150f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        color(Color.white, FlamePal.empathy, e.fin());
        Lines.stroke(4f * e.fout());
        for(int i = 0; i < 8; i++){
            float rot = e.rotation + r.range(5f);
            float len = r.random(140f) * e.finpow();
            float ll = r.random(10f, 25f) * e.finpow();

            Tmp.v1.trns(rot, len).add(e.x, e.y);
            Lines.lineAngle(Tmp.v1.x, Tmp.v1.y, rot, ll);
        }
    }),
    empathyRend = new Effect(60f, 600f, e -> {
        //Draw.color(Color.white, e.fout());
        Rand r = Utils.rand;
        r.setSeed(e.id);
        for(int i = 0; i < 8; i++){
            float f = i / 7f;
            float a = 0.5f;
            float scl = r.random(0.5f, 1.3f);

            float fin = pow4Out.apply(Mathf.curve(e.fin(), a * f, (1f - a) + (a * f)));
            float x = r.random(360f), y = r.random(360f), z = r.random(360f);

            if(fin <= 0.001f || fin >= 0.999f) continue;
            Draw.color(Color.white, (1f - fin) * 0.5f);
            GraphicUtils.drawShockWave(e.x, e.y, x, y, z, 180f * scl * fin + 10, 16f * fin + 8f, 16, 1f);
        }
    }).layer(Layer.flyingUnit + 0.01f),

    empathyBlast = new Effect(60f, 900f, e -> {
        Draw.color(Color.white, FlamePal.empathyAdd, pow2Out.apply(e.fin()));
        Draw.alpha(pow2In.apply(e.fout()));
        //Draw.color(FlamePal.empathyAdd, e.fout());
        Draw.blend(Blending.additive);

        float size = e.rotation;
        Fill.circle(e.x, e.y, (size * pow10Out.apply(e.fin())) + (size * 0.1f * e.fin()));

        Draw.blend();
    }),

    empathySquareDespawn = new Effect(60f, 280f, e -> {
        float size = 120f;

        Draw.color(FlamePal.empathy);
        Lines.stroke(4f * e.fout());
        Lines.poly(e.x, e.y, 4, size, e.rotation + 45f);

        Fill.poly(e.x, e.y, 4, size * Mathf.curve(e.fout(), 0.85f, 1f), e.rotation + 45f);
        Draw.color();
    }).layer(Layer.flyingUnit),

    empathyDualDespawn = new Effect(15f, e -> {
        Draw.color(e.color);
        Angles.randLenVectors(e.id, 7, 17f * e.finpow(), (x, y) -> Fill.circle(e.x + x, e.y + y, e.fout() * 4f * e.rotation));
        Fill.circle(e.x, e.y, e.fout() * 16f * e.rotation);
    }),

    empathyBigLaserHit = new Effect(30f, e -> {
        color(Color.white, FlamePal.empathy, Color.gray, pow2Out.apply(e.fin()));

        //float size = e.data instanceof Float ? ((float)e.data) / 2f : 50f;
        float size = (e.data instanceof Float ? ((float)e.data) : (e.data instanceof Sized s ? s.hitSize() : 50f)) * 1.25f;

        Rand r = Utils.rand;
        r.setSeed(e.id);
        for(int i = 0; i < 16; i++){
            float w = r.range(size);
            float l = r.random(180f, 310f);
            float s = r.random(8f, 30f);

            float ic = i / 15f;
            float c = 0.3f;
            float f = Mathf.curve(e.fin(), ic * c, (ic * c) + (1 - c));

            if(f >= 0.0001f && f < 1f){
                Vec2 v = Tmp.v1.trns(e.rotation, l * pow3In.apply(f), w * circleOut.apply(pow3In.apply(f))).add(e.x, e.y);
                Fill.circle(v.x, v.y, s * (1 - (f * f)));
            }
        }
    }),

    empathyDepowered = new Effect(40f, 1200f, e -> {
        float size = e.rotation;
        Rand r = Utils.rand;
        r.setSeed(e.id);

        color(FlamePal.red, FlamePal.empathyAdd, e.fin());
        blend(Blending.additive);
        Lines.stroke(2.4f * e.fout());
        Lines.circle(e.x, e.y, size + size * pow2Out.apply(e.fin()));
        for(int i = 0; i < 4; i++){
            //TODO effect
            float ang = i * 90f + 45f;
            float sscl = size / 25f;
            TextureRegion region = GraphicUtils.getChain();
            float len = region.width * scl * sscl;

            for(int j = 0; j < 16; j++){
                Tmp.v2.trns(ang + r.random(35f), (r.random(70f) + len * j) * pow3Out.apply(e.fin()));
                Vec2 tr = Tmp.v1.trns(ang, size + len * j).add(e.x, e.y).add(Tmp.v2);
                Draw.rect(region, tr.x, tr.y, region.width * scl * sscl * e.fout(), region.height * scl * sscl * e.fout(), ang + r.range(180f) * pow2Out.apply(e.fin()));
            }
        }
        blend();
    }),

    empathyRainbowHit = new Effect(30f, e -> {
        //float size = e.data instanceof Float ? ((float)e.data) / 2f : 50f;
        float size = (e.data instanceof Float ? ((float)e.data) : (e.data instanceof Sized s ? s.hitSize() : 50f)) * 1.25f;

        Rand r = Utils.rand;
        r.setSeed(e.id);
        for(int i = 0; i < 16; i++){
            float w = r.range(size);
            float l = r.random(180f, 310f);
            float s = r.random(8f, 15f);

            float ic = i / 15f;
            float c = 0.3f;
            float f = Mathf.curve(e.fin(), ic * c, (ic * c) + (1 - c));

            float time = f * 40f + Time.time;
            Draw.color(Tmp.c1.set(Color.red).shiftHue(time * 5f));

            if(f >= 0.0001f && f < 1f){
                Vec2 v = Tmp.v1.trns(e.rotation, l * pow3In.apply(f), w * circleOut.apply(pow3In.apply(f))).add(e.x, e.y);
                //Fill.circle(v.x, v.y, s * (1 - (f * f)));
                Fill.poly(v.x, v.y, 4, s * (1 - (f * f)));
            }
        }
    }).layer(Layer.flyingUnit + 0.1f),

    endFlash = new Effect(15f, e -> {
        float f = pow2In.apply(Mathf.curve(e.fin(), 0f, 0.1f));
        float fo = Mathf.curve(e.fout(), 0.4f, 1f);
        float f2 = pow2Out.apply(Mathf.curve(e.fin(), 0.1f, 0.75f));
        float scl = e.rotation;

        Draw.color();
        for(int i = 0; i < 4; i++){
            float r = i * 90f;
            Drawf.tri(e.x, e.y, 5f * fo * scl, (5f + 120f * f) * fo * scl, r);
        }
        for(int i = 0; i < 2; i++){
            float r = i * 180f;
            Drawf.tri(e.x, e.y, 7f * e.fout() * scl, (7f + 310f * f2) * scl, r);
        }
    }).layer(Layer.flyingUnit + 0.1f),

    endDeath = new Effect(50f, 1000f, e -> {
        float fin1 = Mathf.curve(e.fin(), 0f, 0.65f);
        float size = e.rotation;

        Rand r = Utils.rand;
        r.setSeed(e.id);
        
        e.lifetime = 50f + r.range(4f);

        int base = (int)((size * size) / 34f) + 2;
        int base2 = (int)((size * size) / 16f) + 4;

        //Draw.color(FlamePal.empathy);
        Draw.color(FlamePal.darkRed, FlamePal.empathy, Mathf.curve(pow2Out.apply(fin1), 0f, 0.5f));

        for(int i = 0; i < base; i++){
            Vec2 v = Tmp.v1.trns(r.random(360f), Mathf.sqrt(r.nextFloat()) * size + ((20f + size * 4f) * pow2Out.apply(fin1) * r.nextFloat()));
            float s = r.random(0.5f, 1.1f) * (size * 0.4f + 8f) * (1f - fin1);
            if(fin1 < 1f) Fill.circle(v.x + e.x, v.y + e.y, s);
        }
        Draw.color(FlamePal.darkRed, FlamePal.empathy, Mathf.curve(pow2Out.apply(e.fin()), 0f, 0.5f));
        for(int i = 0; i < base2; i++){
            float sin = Mathf.sin(r.random(7f, 11f), r.random(size * 2f)) * e.fin();
            Vec2 v = Tmp.v1.trns(r.random(360f), Mathf.sqrt(r.nextFloat()) * size + ((40f + size * 8f) * pow2In.apply(e.fin()) * r.nextFloat()), sin);
            float s = r.random(0.5f, 1.1f) * (size * 0.25f + 3f) * (1f - pow4In.apply(e.fin()));
            Fill.circle(v.x + e.x, v.y + e.y, s);
        }
    }),

    endSplash = new Effect(35f, 800f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);
        e.lifetime = 50f + r.range(16f);

        Draw.color(FlamePal.darkRed);
        int am = r.random(5, 9);
        for(int i = 0; i < am; i++){
            float of = 0.3f / (am - 1f);
            float c = Mathf.curve(e.fin(), of * i, (1 - 0.3f) + (of * i));
            float ang = r.range(40f) + e.rotation;
            float scl = r.random(0.6f, 1.4f) * 200f;
            float len = r.random(350f, 900f);

            if(c > 0.0001f && c < 0.9999f){
                Tmp.v1.trns(ang, len *  pow2Out.apply(c)).add(e.x, e.y);
                GraphicUtils.diamond(Tmp.v1.x, Tmp.v1.y, scl * 0.22f * (1f - pow3In.apply(c)), scl * pow3Out.apply(Mathf.curve(c, 0f, 0.5f)) + scl * 0.5f, ang);
            }
        }
    }).layer(Layer.darkness + 1f),

    coloredHit = new Effect(15f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        color(Color.white, FlamePal.red, e.fin());
        Lines.stroke(0.5f + e.fout());

        for(int i = 0; i < 8; i++){
            float ang = r.range(12f) + e.rotation;
            float len = r.random(40f) * e.fin();
            Vec2 v = Tmp.v1.trns(ang, len).add(e.x, e.y);

            Lines.lineAngle(v.x, v.y, ang, e.fout() * 8f + 1f);
        }
    }),

    desGroundHit = new Effect(30f, 250f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        int amount = r.random(4, 12);
        int amount2 = r.random(7, 14);
        float c = r.random(0.1f, 0.6f);
        float c2 = r.random(0.1f, 0.3f);

        z(Layer.groundUnit);
        color(Color.gray);
        for(int i = 0; i < amount2; i++){
            float l = (i / (amount2 - 1f)) * c2;
            float f = Mathf.curve(e.fin(), l, (1f - c2) + l);
            float ang = r.random(360f);
            float len = r.random(80f) * e.rotation;
            float scl = r.random(8.5f, 19f) * e.rotation;
            if(f > 0f && f < 1f){
                float f2 = pow2Out.apply(f) * 0.6f + f * 0.4f;
                Vec2 v = Tmp.v1.trns(ang, len * f2).add(e.x, e.y);
                Fill.circle(v.x, v.y, scl * (1f - f));
            }
        }

        z(Layer.groundUnit + 0.02f);
        color(FlamePal.melt, e.color, pow3Out.apply(e.fin()));
        for(int i = 0; i < amount; i++){
            float l = (i / (amount - 1f)) * c;
            float f = Mathf.curve(e.fin(), l, (1f - c) + l);
            float ang = r.random(360f);
            float len = r.random(100f) * e.rotation;
            float scl = r.random(3f, 13f) * e.rotation;
            if(f > 0f && f < 1f){
                float f2 = pow2Out.apply(f) * 0.4f + f * 0.6f;
                Vec2 v = Tmp.v1.trns(ang, len * f2).add(e.x, e.y);
                Fill.circle(v.x, v.y, scl * (1f - f));
            }
        }
    }).layer(Layer.groundUnit),

    desGroundHitMain = new Effect(90f, 900f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        float arange = 25f;
        float scl = 1f;
        float range = 300f;

        color(Color.gray, 0.8f);
        for(int i = 0; i < 4; i++){
            int count = r.random(15, 23);
            for(int k = 0; k < count; k++){
                float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.2f));
                float rr = r.range(arange) + e.rotation;
                float len = r.random(range) * pow4Out.apply(e.fin());
                float sscl = r.random(21f, 43f) * scl * pow2.apply(1f - f) * Mathf.clamp(e.time / 8f);

                if(f < 1){
                    Vec2 v = Tmp.v1.trns(rr, len).add(e.x, e.y);
                    Fill.circle(v.x, v.y, sscl);
                }
            }

            arange *= 2f;
            scl *= 1.12f;
            range *= 0.6f;
        }
        float fin2 = Mathf.clamp(e.time / 18f);

        if(fin2 < 1){
            int count = 20;
            color(Pal.lighterOrange);
            for(int i = 0; i < count; i++){
                float f = Mathf.curve(fin2, 0f, 1f - r.random(0.2f));
                float ang = r.range(40f) + e.rotation;
                float off = r.random(70f) + r.random(15f) * f;
                float len = r.random(190f, 450f);

                if(f < 1){
                    Vec2 v = Tmp.v1.trns(ang, off).add(e.x, e.y);
                    Lines.stroke(0.5f + (1f - f) * 3f);
                    Lines.lineAngle(v.x, v.y, ang, len * f, false);
                }
            }
        }
    }),

    desCreepHit = new Effect(20f, e -> {
        float angr = 90f;
        float len = 1f;
        Rand r = Utils.rand;
        r.setSeed(e.id);

        Draw.color(FlamePal.red);
        Lines.stroke(1.75f);
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 10; j++){
                float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.2f));
                float tlen = r.random(32f) * len * f + r.random(15f);
                float rot = r.range(angr) + e.rotation;
                float slope = pow2Out.apply(Mathf.slope(f)) * 24f * len;
                Vec2 v = Tmp.v1.trns(rot, tlen).add(e.x, e.y);
                Lines.lineAngle(v.x, v.y, rot, slope, false);
            }

            angr *= 0.7f;
            len *= 1.7f;
        }
        Draw.reset();
    }),

    desCreepHeavyHit = new Effect(300f, 1200f, e -> {
        float sizeScl = e.data instanceof Float ? (float)e.data : 1f;

        Rand r = Utils.rand;
        r.setSeed(e.id);

        float scl = Mathf.clamp(e.time / 8f);
        float range = 32f;
        float countScl = 1f;
        float z = z();
        Tmp.c2.set(Color.gray).a(0.8f);
        for(int i = 0; i < 5; i++){
            color(Pal.lightOrange, Tmp.c2, i / 4f);
            float arange = 180f;
            float range2 = 1f;
            for(int j = 0; j < 5; j++){
                int count = (int)(r.random(12, 15) * countScl);
                for(int k = 0; k < count; k++){
                    float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.3f));
                    float ang = r.range(arange) + e.rotation;
                    float len = r.random(range * range2) * sizeScl * 0.5f;
                    float size = r.random(10f, 24f) * scl * sizeScl * 0.5f;

                    z(z - r.random(0.002f));
                    if(f < 1f){
                        Vec2 v = Tmp.v1.trns(ang, len * pow5Out.apply(f)).add(e.x, e.y);
                        Fill.circle(v.x, v.y, size * (1f - pow10In.apply(f)));
                    }
                }

                arange *= 0.6f;
                range2 *= 1.75f;
            }
            scl *= 1.5f;
            range *= 1.6f;
            countScl *= 1.4f;
        }
        z(z);
        
        float shock = 230f * sizeScl * (1f + e.fin() * 2f) + (e.fin() * 50f);
        color(Pal.lighterOrange);
        if(e.time < 5f){
            Fill.circle(e.x, e.y, shock);
        }
        
        Lines.stroke(3f * e.fout());
        Lines.circle(e.x, e.y, shock);

        for(int i = 0; i < 16; i++){
            float ang = r.random(360f);
            Vec2 v = Tmp.v1.trns(ang, shock).add(e.x, e.y);
            Drawf.tri(v.x, v.y, 8f * e.fout() * sizeScl, (70f + 25f * e.fin()) * sizeScl, ang + 180f);
        }
        
        color(Pal.lighterOrange, Pal.lightOrange, e.fin());
        float arange = 180f;
        float range2 = 1f;
        Lines.stroke(3f);
        for(int i = 0; i < 6; i++){
            int count = r.random(8, 12);
            for(int k = 0; k < count; k++){
                float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.3f));
                float f2 = pow5Out.apply(f);
                float rot = e.rotation + r.range(arange);
                float len = range2 * r.random(120f) * sizeScl * f2 + r.random(50f * sizeScl);
                float str = r.random(34f, 60f) * range2 * sizeScl * pow2Out.apply(Mathf.slope(f2));
                if(f < 1f){
                    Vec2 v = Tmp.v1.trns(rot, len).add(e.x, e.y);
                    Lines.lineAngle(v.x, v.y, rot, str);
                }
            }

            arange *= 0.65f;
            range2 *= 1.6f;
        }
    }),

    desGroundMelt = new Effect(15f * 60, e -> {
        z(Layer.debris);
        color(Color.red);
        //Draw.blend(Blending.additive);
        float fout = Mathf.curve(e.fout(), 0f, 0.333f);

        Fill.circle(e.x, e.y, e.rotation * Mathf.clamp(e.time / 6f) * fout);

        //Draw.blend();
        z(Layer.debris + 0.05f);

        color(FlamePal.melt);
        blend(Blending.additive);
        Fill.circle(e.x, e.y, e.rotation * Mathf.clamp(e.time / 6f) * fout);
        blend();
    }).layer(Layer.debris),

    desRailHit = new Effect(80f, 900f, e -> {
        float sizeScl = e.data instanceof Float ? (float)e.data : 1f;
        
        Rand r = Utils.rand;
        r.setSeed(e.id);

        float ang = 180f;
        float rscl = 0.7f * sizeScl;
        Draw.color(FlamePal.red);
        for(int i = 0; i < 5; i++){
            int count = (int)(10 * rscl);
            for(int j = 0; j < count; j++){
                float fin = Mathf.curve(e.fin(), 0f, 1f - r.random(0.2f));
                float rot = r.range(ang) + e.rotation;
                float off = r.random(22f * rscl) + r.random(50f * Mathf.pow(rscl, 1.5f)) * pow4Out.apply(fin);
                float sscl = r.random(0.7f, 1.2f);

                float wid = 12f * sscl * rscl * (1f - pow4In.apply(fin));
                float hei = 52f * sscl * Mathf.pow(rscl, 1.5f) * pow5Out.apply(fin);

                Vec2 v = Tmp.v1.trns(rot, off).add(e.x, e.y);
                Drawf.tri(v.x, v.y, wid, hei, rot);
                Drawf.tri(v.x, v.y, wid, wid * 2.2f, rot + 180f);
            }

            ang *= 0.6f;
            rscl *= 1.5f;
        }

        ang = 180f;
        rscl = 0.5f * sizeScl;
        Draw.color(FlamePal.red, Color.white, e.fin());
        Lines.stroke(3f);
        for(int i = 0; i < 7; i++){
            int count = 12;
            for(int j = 0; j < count; j++){
                float fin = Mathf.curve(e.fin(), 0f, 1f - r.random(0.2f));
                float rot = r.range(ang) + e.rotation;
                float off = r.random(30f * rscl) + r.random(40f * Mathf.pow(rscl, 1.6f)) * pow5Out.apply(fin);

                float len = r.random(20f, 40f) * Mathf.pow(rscl, 1.6f) * sineOut.apply(Mathf.slope(pow5Out.apply(fin)));

                Vec2 v = Tmp.v1.trns(rot, off).add(e.x, e.y);
                Lines.lineAngle(v.x, v.y, rot, len, false);
            }

            ang *= 0.5f;
            rscl *= 1.5f;
        }

        if(sizeScl < 0.75f) return;
        Draw.color(Color.white, 0.666f * e.fout());

        GraphicUtils.drawShockWave(e.x, e.y, -105f, 0f, -e.rotation - 90f, 400f * sizeScl * pow2Out.apply(e.fin()) + 70f, 30f * Mathf.pow(sizeScl, 1f / 1.5f) * pow2Out.apply(e.fin()) + 4f, 16, 0.015f);
    }),

    desNukeShockwave = new Effect(190f, 1900f * 2f, e -> {
        float size = e.rotation;

        Draw.color(Color.white, 0.333f * e.fout());
        Lines.stroke((size / 15f) + (size / 5f) * e.fin());
        Lines.circle(e.x, e.y, size / 3f + size * pow2Out.apply(e.fin()) * 2f);
    }).layer(Layer.groundUnit + 1f),

    desNuke = new Effect(80f, 500f * 2, e -> {
        if(!(e.data instanceof float[] arr)) return;
        float size = e.rotation;
        
        Rand r = Utils.rand;
        r.setSeed(e.id);

        float scl = 1f;
        Tmp.c2.set(Color.gray).a(0.8f);
        for(int k = 0; k < 6; k++){
            float cf = k / 5f;
            color(Tmp.c2, Pal.lightOrange, cf);
            for(int i = 0; i < 40; i++){
                float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.2f));
                float len = r.random(size * scl * 0.75f) * pow5Out.apply(f) + r.random(size / 5f);
                float ang = r.random(360f);
                float psize = size / 5f;
                float rad = r.random(psize * (scl * 0.5f + 0.5f) * 0.87f, psize) * scl * (1f - pow5In.apply(f));
                if(f < 1f){
                    Tmp.v1.trns(ang, len).add(e.x, e.y);
                    Fill.circle(Tmp.v1.x, Tmp.v1.y, rad);
                }
            }
            scl *= 0.75f;
        }
        scl = 1f;
        color(Pal.lighterOrange);
        Lines.stroke(3f);
        for(int i = 0; i < 4; i++){
            for(int j = 0; j < 20; j++){
                float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.2f));
                float ang = r.random(360f);
                float len = r.random(size * scl * 0.5f) * pow5Out.apply(f) + r.random(size / 5f);
                float line = r.random(22f, 45f) * Mathf.pow(scl, 1.1f) * pow2Out.apply(Mathf.slope(pow5Out.apply(f)));

                if(f < 1f){
                    Tmp.v1.trns(ang, len).add(e.x, e.y);
                    Lines.lineAngle(Tmp.v1.x, Tmp.v1.y, ang, line, false);
                }
            }
            scl *= 1.4f;
        }
        
        float fin = Mathf.clamp(e.time / 10f);
        if(fin < 1){
            Tmp.c2.set(Pal.lightOrange).a(0f);
            color(Pal.lighterOrange, Tmp.c2, fin);
            for(int i = 0; i < arr.length; i++){
                float len1 = arr[i], len2 = arr[(i + 1) % arr.length];
                float ang1 = (i / (float)arr.length) * 360f;
                float ang2 = ((i + 1f) / arr.length) * 360f;

                if(len1 >= size){
                    len1 += (size / 1.5f) * fin;
                }
                if(len2 >= size){
                    len2 += (size / 1.5f) * fin;
                }

                float x1 = Mathf.cosDeg(ang1) * len1, y1 = Mathf.sinDeg(ang1) * len1;
                float x2 = Mathf.cosDeg(ang2) * len2, y2 = Mathf.sinDeg(ang2) * len2;

                Fill.tri(e.x, e.y, e.x + x1, e.y + y1, e.x + x2, e.y + y2);
            }
        }
    }),

    desNukeShoot = new Effect(35f, e -> {
        float ang = 90f, len = 1f;
        Rand r = Utils.rand;
        r.setSeed(e.id);

        //Draw.color(FlamePal.red, Color.white, e.fin());
        Lines.stroke(2f);
        for(int i = 0; i < 5; i++){
            for(int j = 0; j < 7; j++){
                float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.2f));
                float rot = e.rotation + r.range(ang);
                Draw.color(FlamePal.red, Color.white, f);
                Vec2 v = Tmp.v1.trns(rot, r.random(40f) * pow2Out.apply(f) * len).add(e.x, e.y);
                Lines.lineAngle(v.x, v.y, rot, f * 40f * r.random(0.75f, 1f) * len * pow2Out.apply(Mathf.slope(f)), false);
            }
            ang *= 0.5f;
            len *= 1.4f;
        }
    }),

    desNukeVaporize = new Effect(40f, 1200f, e -> {
        float size = e.data instanceof Float ? (float)e.data : 10f;

        Rand r = Utils.rand;
        r.setSeed(e.id);

        int count = 20 + (int)(size * size * 0.5f);
        float c = 0.25f;
        for(int i = 0; i < count; i++){
            float l = r.nextFloat() * c;
            float f = Mathf.curve(e.fin(), l, ((1f - c) + l) * r.random(0.8f, 1f));
            float len = r.random(0.5f, 1f) * (80f + size * 10f) * pow2In.apply(f);
            float off = Mathf.sqrt(r.nextFloat()) * size, ang = r.random(360f), rng = r.range(10f);
            float scl = (size / 2f) * r.random(0.9f, 1.1f) * Utils.biasSlope(f, 0.1f);

            if(f > 0 && f < 1){
                Vec2 v1 = Tmp.v1.trns(ang, off).add(e.x, e.y).add(Tmp.v2.trns(e.rotation + rng, len));
                Draw.color(Pal.lightOrange, Pal.rubble, pow3Out.apply(f));
                Fill.circle(v1.x, v1.y, scl);
            }
        }
    }).layer(Layer.flyingUnit),

    desNukeShockSmoke = new Effect(40f, 800f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        int count = 10;
        float c = 0.4f;
        for(int i = 0; i < count; i++){
            float l = r.nextFloat() * c;
            float f = Mathf.curve(e.fin(), l, ((1f - c) + l) * r.random(0.8f, 1f));
            float len = r.random(0.75f, 1f) * 160f * pow2In.apply(f);
            float off = Mathf.sqrt(r.nextFloat()) * Vars.tilesize / 2f, ang = r.random(360f), rng = r.range(10f);
            float scl = r.random(4f, 6f) * (1f - pow2In.apply(f));

            if(f > 0 && f < 1){
                Vec2 v1 = Tmp.v1.trns(ang, off).add(e.x, e.y).add(Tmp.v2.trns(e.rotation + rng, len));
                color(Pal.rubble, Color.gray, f);
                Fill.circle(v1.x, v1.y, scl);
            }
        }
    }),

    desMissileHit = new Effect(50f, 800f, e -> {
        Rand r = Utils.rand;
        r.setSeed(e.id);

        Tmp.c2.set(Color.gray).a(0.8f);
        //Tmp.c3.set(FlamePal.red).mul(2f);
        float scl1 = Mathf.clamp(e.time / 3f);
        float scl3 = 1.1f;
        float angScl = 0.6f;
        for(int i = 0; i < 4; i++){
            float scl2 = 1f;
            float len = 1f;
            float ang = 180f;

            //Draw.color(Tmp.c2, Pal.lightOrange, i / 3f);
            color(Tmp.c2, FlamePal.red, i / 3f);
            for(int j = 0; j < 5; j++){
                for(int k = 0; k < 9; k++){
                    float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.3f));
                    float rot = e.rotation + r.range(ang);
                    //float ll = r.random(45f) * len * pow10Out.apply(f) * scl1;
                    float ll = r.random(45f) * len * pow5Out.apply(f);
                    float scl = r.random(0.666f, 1f) * scl2 * scl1 * 18f * (1f - pow10In.apply(f));

                    Vec2 v = Tmp.v1.trns(rot, ll).add(e.x, e.y);
                    Fill.circle(v.x, v.y, scl);
                }

                ang *= angScl;
                len *= 1.5f;
                scl2 *= scl3;
            }
            scl1 *= 0.9f;
            angScl *= 0.8f;
            scl3 *= 0.9f;
        }
        color(FlamePal.red);
        scl1 = 1f;
        scl3 = 1f;
        angScl = 180f;
        for(int i = 0; i < 5; i++){
            for(int j = 0; j < 6; j++){
                float f = Mathf.curve(e.fin(), 0f, 1f - r.random(0.3f));
                float rot = e.rotation + r.range(angScl);
                float ll = r.random(20f) * scl3 * pow2Out.apply(f);
                float size = r.random(5f, 10f);
                float wid = size * scl1 * Utils.biasSlope(f, 0.2f);
                float len = wid * 3f + size * 7f * Mathf.pow(scl1, 1.2f) * pow5Out.apply(f);

                Vec2 v = Tmp.v1.trns(rot, wid * 2f + ll).add(e.x, e.y);
                Drawf.tri(v.x, v.y, wid, len, rot);
                Drawf.tri(v.x, v.y, wid, wid * 3f, rot + 180f);
            }

            scl1 *= 1.2f;
            scl3 *= 1.5f;
            angScl *= 0.5f;
        }
        
        Draw.reset();
    });
}
