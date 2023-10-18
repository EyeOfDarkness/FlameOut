package flame.effects;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.Fragmentation.*;
import flame.graphics.*;
import flame.graphics.VaporizeBatch.*;
import flame.unit.empathy.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import java.util.regex.*;

public class SpecialDeathEffects{
    public static ObjectMap<String, SpecialDeathEffects> nameMap = new ObjectMap<>();
    public static ObjectMap<String, Seq<DeathGroup>> modGroupMap = new ObjectMap<>();
    public static SpecialDeathEffects def, blood;

    public Effect debrisEffect = FlameFX.heavyDebris, sparkEffect = FlameFX.destroySparks, explosionEffect = Fx.none;
    public Effect fragmentTrailEffect = null;
    public Cons<FragmentEntity> fragDeath = null;
    public Color disintegrateColor = Pal.rubble;
    public boolean canBeCut = true;

    public static void load(){
        def = new SpecialDeathEffects();
        blood = new OrganicDeath(){{
            color.set(FlamePal.blood);
            colorVariation = 0.2f;

            debrisEffect = Fx.none;
            explosionEffect = new Effect(30f, 400f, e -> {
                Rand r = Utils.rand;
                r.setSeed(e.id);
                float size = e.rotation;

                int iter = ((int)(size / 7f)) + 12;
                int iter3 = ((int)(size / 14.5f)) + 12;
                Draw.color(color);
                //alpha(0.9f);
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

                e.lifetime = size / 1.5f + 10f;
            });
        }};

        nameMap.put("flameout-apathy", new ApathyDeath());

        nameMap.put("new-horizon-guardian", new HorizonGuardianDeath());
        OrganicDeath nha = new OrganicDeath(){{
            color.rgba8888(0xfff480ff);
            fragmentTrailEffect = null;
            liquidAmount = 0.15f;
            liquidRange = 0.3f;
            liquidSize = 0.6f;
            colorVariation = 0f;

            Effect lightning = new Effect(16f, 2000f, e -> {
                Rand r = Utils.rand;
                r.setSeed(e.id);
                float size = e.rotation;
                int lightnings = 3 + (int)(size / 12f);
                int liLength = 5 + (int)(size / 4f);
                Draw.color(color);
                Lines.stroke(3f * e.fout());
                for(int i = 0; i < lightnings; i++){
                    //Tmp.v1.trns()
                    float lr = r.random(360f);
                    Tmp.v1.trns(lr, r.random(size)).add(e.x, e.y);
                    float lx = Tmp.v1.x, ly = Tmp.v1.y;
                    int lir = liLength + r.random(2 + (int)(liLength / 5f));
                    for(int j = 0; j < lir; j++){
                        float rr = r.random(3f, 15f) * (r.nextBoolean() ? -1 : 1);
                        float nr = lr + rr;
                        Vec2 v = Tmp.v1.trns(nr, 15f).add(lx, ly);
                        Lines.line(lx, ly, v.x, v.y);

                        lr = nr;
                        lx = v.x;
                        ly = v.y;
                    }
                }
            }).layer(Layer.flyingUnit);

            explosionEffect = new MultiEffect(FlameFX.fragmentExplosion, lightning);
        }};
        nameMap.put("new-horizon-pester", nha);
        nameMap.put("new-horizon-laugra", nha);
        nameMap.put("new-horizon-nucleoid", nha);
        nameMap.put("new-horizon-ancient-probe", nha);
        nameMap.put("new-horizon-restriction-enzyme", nha);
        nameMap.put("new-horizon-macrophage", nha);
        nameMap.put("new-horizon-ancient-artillery", nha);

        OrganicDeath fr = new OrganicDeath();
        nameMap.put("me-hive", fr);
        nameMap.put("me-apis", fr);
        nameMap.put("me-ducalis", fr);
        nameMap.put("me-procer", fr);
        nameMap.put("me-hive-attack", fr);
        nameMap.put("me-cerberian-behemoth", fr);
        nameMap.put("me-test", fr);

        ExoticDeath wardDeath = new ExoticDeath();
        putGroupEffect("exotic-mod", "0b\\d\\d-", wardDeath);
        putGroupEffect("allure", "0b\\d\\d-", wardDeath);
    }

    public static SpecialDeathEffects get(MappableContent content){
        Seq<DeathGroup> s;
        if(content.minfo.mod != null && (s = modGroupMap.get(content.minfo.mod.name)) != null){
            for(DeathGroup group : s){
                if(group.valid(content.name)) return group.effect;
            }
        }

        SpecialDeathEffects ef = get(content.name);
        if(ef == def && content instanceof UnitType u && (u.health >= 4000000f || EmpathyDamage.isNaNInfinite(u.health))){
            return blood;
        }

        return ef;
    }

    public static SpecialDeathEffects get(String name){
        SpecialDeathEffects g = nameMap.get(name);
        if(g != null){
            return g;
        }

        return def;
    }

    public static void putGroupEffect(String mod, String group, SpecialDeathEffects effect){
        Seq<DeathGroup> g = modGroupMap.get(mod);
        if(g == null){
            g = new Seq<>();
            modGroupMap.put(mod, g);
        }
        g.add(new DeathGroup(group, effect));
    }

    public void cutAlt(Unit u){
        explosionEffect.at(u.x, u.y, u.hitSize / 2f);
    }

    public void disintegrateUnit(Unit u, float x1, float y1, float x2, float y2, float width){
        Vec2 vec = Utils.v, vec2 = Utils.vv;
        float rotation = Angles.angle(x1, y1, x2, y2);

        disintegrateUnit(u, x1, y1, x2, y2, width, (d, within) -> {
            if(within){
                d.disintegrating = true;

                Vec2 n = Intersector.nearestSegmentPoint(x1, y1, x2, y2, d.x, d.y, vec);
                float dst = 1f - Mathf.clamp(n.dst(d.x, d.y) / (width / 2f));
                float force = Interp.pow2.apply(dst) * Mathf.random(0.9f, 1.5f);

                d.lifetime = Mathf.lerp(Mathf.random(90f, 130f), Mathf.random(42f, 60f), Interp.pow2.apply(dst));

                vec2.trns(rotation, force, Mathf.range(0.07f * dst));
                d.vx = vec2.x;
                d.vy = vec2.y;
                d.vr = Mathf.range(5f) * dst;
                d.drag = -Mathf.random(0.035f, 0.05f) * dst;
                d.zOverride = Layer.flyingUnit;
            }
        });
    }

    public void disintegrateUnit(Unit u, float x1, float y1, float x2, float y2, float width, VaporizeHandler handler){
        FlameOut.vaporBatch.discon = d -> d.scorchColor.set(disintegrateColor);
        FlameOut.vaporBatch.switchBatch(x1, y1, x2, y2, width, u::draw, handler);
    }

    public void deathUnit(Unit u, float x, float y, float rotation){
        deathUnit(u, x, y, rotation, null);
    }

    public void deathUnit(Unit u, float x, float y, float rotation, Cons<FragmentEntity> alt){
        //float ang = Angles.angle(x, y, u.x, u.y);
        //Fx.dynamicExplosion.at(u.x, u.y, u.hitSize / 2f / 12f);
        debrisEffect.at(u.x, u.y, rotation, u.type.outlineColor, u.hitSize / 2f);
        explosionEffect.at(u.x, u.y, u.hitSize / 2f);

        Tmp.v1.trns(rotation - 180f, u.hitSize / 2).add(u.x, u.y);
        sparkEffect.at(Tmp.v1.x, Tmp.v1.y, rotation, u.hitSize / 2f);

        //UnitType type = u.type;
        FragmentationBatch batch = FlameOut.fragBatch;
        Tmp.v1.trns(rotation - 180f, 80f).add(x, y);
        float sx = Tmp.v1.x, sy = Tmp.v1.y;
        //batch.updateCircle();
        batch.baseElevation = u.type.shadowElevationScl * (u.isFlying() ? 1f : 0.25f);
        if(alt != null){
            batch.fragFunc = alt;
        }else{
            batch.fragFunc = e -> {
                float dx = (e.x - u.x) / 65f + u.vel.x;
                float dy = (e.y - u.y) / 65f + u.vel.y;
                float dx2 = (e.x - sx) / 15f;
                float dy2 = (e.y - sy) / 15f;

                float adst = Utils.angleDistSigned(Angles.angle(sx, sy, e.x, e.y), rotation);
                float scl = Mathf.pow(Mathf.clamp((90 - Math.abs(adst)) / 90f), 2.5f) + Mathf.pow(Mathf.clamp((30 - Math.abs(adst)) / 30f), 3f);
                e.vx = dx + dx2 * scl;
                e.vy = dy + dy2 * scl;
                e.vr = (Mathf.pow(Math.abs(adst), 0.5f) * (adst > 0 ? 1 : -1)) / 15 + Mathf.range(1f);
                //e.lifetime = 180f;
                e.vz = Mathf.random(-0.01f, 0.1f);
            };
        }
        //batch.altFunc = (hx, hy, tex) -> FlameFX.simpleFragmentation.at(hx, hy, ang, tex);
        batch.onDeathFunc = fragDeath;
        batch.altFunc = (hx, hy, tex) -> FlameFX.simpleFragmentation.at(hx, hy, Angles.angle(x, y, hx, hy), Draw.getColor(), tex);
        batch.trailEffect = fragmentTrailEffect;
        batch.explosionEffect = explosionEffect != Fx.none ? explosionEffect : null;
        batch.fragColor = Color.white;

        batch.switchBatch(u::draw);
    }

    public void deathBuilding(Building b, float x, float y, float rotation){
        //Fx.dynamicExplosion.at(b.x, b.y, b.hitSize() / 2f / 12f);
        debrisEffect.at(b.x, b.y, b.angleTo(x, y) + 180f, b.block.outlineColor, b.hitSize() / 2f);
        //FlameFX.destroySparks.at(b.x, b.y, ang, b.hitSize() / 2f);

        Tmp.v1.trns(rotation - 180f, b.hitSize() / 2).add(b.x, b.y);
        sparkEffect.at(Tmp.v1.x, Tmp.v1.y, rotation, b.hitSize() / 2f);

        Tmp.v1.trns(rotation - 180f, 250f).add(x, y);
        float sx = Tmp.v1.x, sy = Tmp.v1.y;

        FragmentationBatch batch = FlameOut.fragBatch;
        //batch.updateCircle();
        batch.baseElevation = 0.1f;
        batch.fragFunc = e -> {
            float dx = (e.x - b.x) / 65f;
            float dy = (e.y - b.y) / 65f;
            float dx2 = (e.x - sx) / 15f;
            float dy2 = (e.y - sy) / 15f;

            float adst = Utils.angleDistSigned(Angles.angle(sx, sy, e.x, e.y), rotation);
            float scl = Mathf.pow(Mathf.clamp((90 - Math.abs(adst)) / 90f), 2.5f) + Mathf.pow(Mathf.clamp((30 - Math.abs(adst)) / 30f), 3f);
            e.vx = dx + dx2 * scl * 0.3f;
            e.vy = dy + dy2 * scl * 0.3f;
            e.vr = (Mathf.pow(Math.abs(adst), 0.5f) * (adst > 0 ? 1 : -1)) / 15 + Mathf.range(1f);
            //e.lifetime = 180f;
            e.vz = Mathf.random(-0.01f, 0.1f);
        };
        batch.altFunc = (hx, hy, tex) -> FlameFX.simpleFragmentation.at(hx, hy, Angles.angle(x, y, hx, hy), tex);
        batch.explosionEffect = batch.trailEffect = null;
        batch.fragColor = Color.white;

        batch.switchBatch(b::draw);
    }

    public static class ApathyDeath extends SpecialDeathEffects{
        ApathyDeath(){
            debrisEffect = Fx.none;
        }

        @Override
        public void deathUnit(Unit u, float x, float y, float rotation, Cons<FragmentEntity> alt){
            //BloodSplatter.explosion(95, x, y, hitSize / 2, 400f, 45f);
            //BloodSplatter.explosion(40, x, y, hitSize / 2, 550f, 35f, 60f);
            BloodSplatter.directionalExplosion(85, u.x, u.y, rotation, 60f, u.hitSize / 2f, 400f, 45f, FlamePal.blood, 0.2f);
            BloodSplatter.directionalExplosion(40, u.x, u.y, rotation, 50f, u.hitSize / 2f, 500f, 60f, FlamePal.blood, 0.2f);
            super.deathUnit(u, x, y, rotation, alt);
        }
    }
    public static class HorizonGuardianDeath extends SpecialDeathEffects{
        Effect disintegrationEffect;
        
        HorizonGuardianDeath(){
            debrisEffect = Fx.none;
            sparkEffect = Fx.none;
            canBeCut = false;
            explosionEffect = new Effect(120f, e -> {
                Draw.color(e.color);
                Rand r = Utils.rand;
                r.setSeed(e.id);

                Lines.stroke(3f * e.fout());
                for(int i = 0; i < 15; i++){
                    float ang = r.random(360f);
                    float len = r.random(210f, 720f) * Interp.pow2Out.apply(e.fin());
                    float s = r.random(7f, 9.6f);

                    Vec2 v = Tmp.v1.trns(ang, len).add(e.x, e.y);
                    Lines.line(e.x, e.y, v.x, v.y);
                    Fill.poly(v.x, v.y, 4, s * e.fout());
                }
                float fin2 = Mathf.clamp(e.time / 32f);
                float fin3 = Mathf.clamp(e.time / 80f);
                
                if(fin3 < 1){
                    for(int i = 0; i < 32; i++){
                        float ang = r.random(360f);
                        float len = r.random(290f) * Interp.pow2Out.apply(fin3) + r.random(e.rotation);
                        float s = r.random(12f, 16f);
                        
                        Vec2 v = Tmp.v1.trns(ang, len).add(e.x, e.y);
                        Fill.poly(v.x, v.y, 4, s * (1f - fin3));
                    }
                }

                if(fin2 < 1){
                    Lines.stroke(3f * (1f - fin2));
                    for(int i = 0; i < 24; i++){
                        float ang = r.random(360f);
                        float blen = r.random(e.rotation);
                        float len = r.random(10f, 190f) * Interp.pow2Out.apply(fin2);
                        float s = r.random(31f, 50f);

                        Vec2 v = Tmp.v1.trns(ang, len + blen).add(e.x, e.y);
                        GraphicUtils.diamond(v.x, v.y, (s / 3f) * (1f - fin2), s, ang);
                    }
                    for(int i = 0; i < 16; i++){
                        float ang = r.random(360f);
                        float blen = r.random(e.rotation);
                        float len = r.random(10f, 110f) * Interp.pow2Out.apply(fin2);
                        float s = r.random(80f, 100f);

                        Vec2 v = Tmp.v1.trns(ang, len + blen).add(e.x, e.y);
                        GraphicUtils.diamond(v.x, v.y, (s / 3f) * (1f - fin2), s * Interp.pow2Out.apply(Mathf.curve(fin2, 0f, 0.5f)), ang);
                    }

                    Lines.circle(e.x, e.y, Interp.pow2Out.apply(fin2) * 280f + 20f);
                    Fill.circle(e.x, e.y, e.rotation * 1.5f * (1f - Interp.pow2Out.apply(fin2)));
                }
            });
            disintegrationEffect = new Effect(100f, e -> {
                Draw.color(e.color);
                Rand r = Utils.rand;
                r.setSeed(e.id);
                
                float c = 0.6f;
                
                for(int i = 0; i < 64; i++){
                    float ang = r.random(360f);
                    float randAng = r.range(2f);
                    float blen = r.random(35f);
                    float len = r.random(200f, 400f);
                    float s = r.random(12f, 16f);
                    float rc = i / 63f;
                    float f = Mathf.curve(e.fin(), c * rc, (1f - c) + c * rc);

                    if(f > 0){
                        Tmp.v2.trns(e.rotation + randAng, len * Interp.pow2In.apply(f));
                        Vec2 v = Tmp.v1.trns(ang, blen).add(e.x, e.y).add(Tmp.v2);
                        Fill.poly(v.x, v.y, 4, s * (1f - f));
                    }
                }
            });
        }

        @Override
        public void cutAlt(Unit u){
            explosionEffect.at(u.x, u.y, u.hitSize / 2f, u.team.color);
        }

        @Override
        public void disintegrateUnit(Unit u, float x1, float y1, float x2, float y2, float width, VaporizeHandler handler){
            explosionEffect.at(u.x, u.y, u.hitSize / 2f, u.team.color);
            disintegrationEffect.at(u.x, u.y, Angles.angle(x1, y1, x2, y2), u.team.color);
        }

        @Override
        public void deathUnit(Unit u, float x, float y, float rotation, Cons<FragmentEntity> alt){
            Color c = Tmp.c1.set(u.team.color).mul(0.9f);
            //fix
            BloodSplatter.directionalExplosion(60, u.x, u.y, rotation, 70f, u.hitSize / 2f, 200f, 20f, c, 0f);
            BloodSplatter.directionalExplosion(20, u.x, u.y, rotation, 50f, u.hitSize / 2f, 320f, 30f, c, 0f);

            explosionEffect.at(u.x, u.y, u.hitSize / 2f, u.team.color);
        }
    }
    public static class ExoticDeath extends SpecialDeathEffects{
        static Color color = new Color(0x7a8affff);

        ExoticDeath(){
            explosionEffect = new Effect(40f, 300f, e -> {
                Rand r = Utils.rand;
                r.setSeed(e.id);
                float size = e.rotation;

                int iter = ((int)(size / 7f)) + 12;
                int iter3 = ((int)(size / 14.5f)) + 12;
                Draw.color(color, Color.gray, e.fin());
                //alpha(0.9f);
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

                float sfin = Mathf.curve(e.fin(), 0f, 0.65f);
                if(sfin < 1f){
                    int iter2 = ((int)(size / 11f)) + 6;
                    int iter4 = (int)(size / 7f) + 3;
                    float sfout = 1f - sfin;

                    Draw.color(Color.white, color, sfin);
                    Lines.stroke((1.7f * sfout) * (1f + size / 60f));

                    Draw.z(Layer.effect + 0.001f);

                    for(int i = 0; i < iter2; i++){
                        Vec2 v = Tmp.v1.trns(r.random(360f), r.random(0.001f, size / 2f) + (r.random(0.4f, 2.2f) * size) * Interp.pow2Out.apply(sfin));
                        Lines.lineAngle(e.x + v.x, e.y + v.y, Mathf.angle(v.x, v.y), 1f + sfout * 6 * (3f + size / 60f));
                    }
                    for(int i = 0; i < iter4; i++){
                        Vec2 v = Tmp.v1.trns(r.random(360f), r.random(size / 2f) + (r.random(3.2f) * size) * Interp.pow2Out.apply(sfin));
                        Fill.poly(e.x + v.x, e.y + v.y, 4, (size / 4f) * r.random(0.75f, 1.25f) * sfout);
                    }
                }

                e.lifetime = size / 1.5f + 10f;
            });
            sparkEffect = new Effect(40f, 1200f, e -> {
                Rand r = Utils.rand;
                r.setSeed(e.id + 64331);
                float size = (float)e.data;
                int isize = (int)(size * 1.75f) + 12;
                int isize2 = (int)(size * 1.5f) + 9;

                float fin1 = Mathf.clamp(e.time / 25f);

                Lines.stroke(Math.max(2f, Mathf.sqrt(size) / 8f));
                for(int i = 0; i < isize2; i++){
                    float f = Mathf.curve(fin1, 0f, r.random(0.8f, 1f));
                    Vec2 v = Tmp.v1.trns(r.random(360f), 1f + (size * r.nextFloat() + 10f) * 1.5f * Interp.pow3Out.apply(f));
                    float rsize = r.random(0.75f, 1.5f);
                    if(f < 1){
                        Draw.color(Color.white, color, f);
                        Lines.lineAngle(v.x + e.x, v.y + e.y, v.angle(), (size / 3.5f) * rsize * (1 - f));
                    }
                }
                for(int i = 0; i < isize; i++){
                    float f = Mathf.curve(e.fin(), 0f, r.random(0.5f, 1f));
                    float re = Mathf.pow(r.nextFloat(), 1.5f);
                    //float ang = Mathf.pow(r.nextFloat(), 1.5f) * 90f * (r.nextFloat() > 0.5f ? 1 : -1);
                    float ang = re * 90f * (r.nextBoolean() ? 1 : -1);
                    float dst = (50f + ((size * 3f) / (1f + re / 5f)) * Mathf.pow(r.nextFloat(), (1f + re / 2f))) * Interp.pow3Out.apply(f);
                    Vec2 v = Tmp.v1.trns(e.rotation + ang, 1f + dst);
                    float rsize = r.random(0.9f, 1.6f);
                    if(f < 1){
                        Draw.color(Color.white, color, f);
                        Lines.lineAngle(v.x + e.x, v.y + e.y, v.angle(), (size / 2.5f) * rsize * (1 - f));
                    }
                }
            });
        }
    }
    public static class OrganicDeath extends SpecialDeathEffects{
        //Color color = new Color(0x780b24ff);
        Color color = new Color(0x780b24d9);
        float colorVariation = 0.4f;
        float liquidAmount = 1f;
        float liquidRange = 1f;
        float liquidSize = 1f;

        OrganicDeath(){
            fragmentTrailEffect = new Effect(20f, e -> {
                float fin = Utils.biasSlope(e.fin(), 0.25f);
                Draw.color(color);
                Fill.circle(e.x, e.y, e.rotation * fin * 1.25f);
            }).layer(Layer.flyingUnitLow);

            fragDeath = e -> {
                float realSize = e.area * 0.35f;
                
                int size = 6 + (int)((realSize / 2f) * 0.75f * liquidAmount);
                float hscl = (realSize / 2f) / 30f;
                BloodSplatter.explosion(size, e.x, e.y, e.area / 2f, 60f * liquidRange * hscl + 15f, 25f * liquidSize * hscl + 5f, 35f, color, colorVariation);
            };
        }

        @Override
        public void deathUnit(Unit u, float x, float y, float rotation, Cons<FragmentEntity> alt){
            int size1 = 8 + (int)((u.hitSize / 2f) * 0.75f * liquidAmount);
            int size2 = 13 + (int)((u.hitSize / 2f) * 1.5f * liquidAmount);
            
            float hscl = (u.hitSize / 2f) / 30f;
            
            BloodSplatter.directionalExplosion(size2, u.x, u.y, rotation, 70f, u.hitSize / 2f, 300f * liquidRange * hscl + 12f, 25f * liquidSize * hscl + 5f, color, colorVariation);
            BloodSplatter.directionalExplosion(size1, u.x, u.y, rotation, 50f, u.hitSize / 2f, 420f * liquidRange * hscl + 24f, 40f * liquidSize * hscl + 5f, color, colorVariation);

            super.deathUnit(u, x, y, rotation, alt);
        }
    }

    private static class DeathGroup{
        //String group;
        Pattern pattern;
        SpecialDeathEffects effect;
        ObjectSet<String> nameSet = new ObjectSet<>(), excludeSet = new ObjectSet<>();

        DeathGroup(String g, SpecialDeathEffects e){
            effect = e;
            pattern = Pattern.compile(g);
        }

        boolean valid(String name){
            if(nameSet.contains(name)) return true;
            if(excludeSet.contains(name)) return false;

            Matcher m = pattern.matcher(name);
            //boolean gc = m.groupCount() > 0;
            boolean gc = m.find();
            if(gc){
                nameSet.add(name);
            }else{
                excludeSet.add(name);
            }
            return gc;
        }
    }
}
