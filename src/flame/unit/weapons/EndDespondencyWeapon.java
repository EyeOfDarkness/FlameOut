package flame.unit.weapons;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.effects.Fragmentation.*;
import flame.entities.*;
import flame.graphics.*;
import flame.unit.*;
import flame.unit.empathy.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class EndDespondencyWeapon extends Weapon{
    Seq<DespondencyArm> arms = new Seq<>();
    TextureRegion[][] armRegions;

    public EndDespondencyWeapon(){
        mirror = false;
        x = y = 0f;
        mountType = DespondencyMount::new;
        noAttack = true;
        controllable = aiControllable = false;
        top = false;

        Rand rand = Utils.rand, rand2 = Utils.rand2;
        
        for(int i = 0; i < 5; i++){
            int fi = i;
            arms.add(new DespondencyArm("spear"){{
                x = 78f;
                y = -15f;
                rotation = -50f;

                tx = 20f - 5f * fi;
                ty = 12f - 12f * fi;
                trot = -125f + 21f * fi;
                armrot = 25f - 0.5f * fi;
                handrot = 15f - 0.2f * fi;
                weaponScl = 2f;
                weaponOffset = 8f;
                scl = 1.2f;
                tscl = fi * 0.1f;

                delay = 20f + 12f * fi;
                speed = 0.012f;

                length1 *= 1.7f;
                length2 *= 1.7f;
                insize *= 2.7f;
            }});
        }
        
        rand.setSeed(531125);
        rand2.setSeed(53311);
        for(int i = 0; i < 15; i++){
            arms.add(new DespondencyArm("sword-" + rand2.random(4)){{
                x = 60f;
                y = -40f;
                rotation = -60f;

                tx = 38f + rand.range(10f);
                ty = -8f + rand.range(10f);
                
                float arange = rand.range(60f);
                trot = -45f + arange;
                armrot = rand.random(30f);
                handrot = rand.random(-12f, 8f);
                weaponScl = 1.9f;
                weaponOffset = 35f;
                scl = rand.random(0.8f, 1.2f);
                tscl = 0f;

                //delay = 120f + rand.random(40f);
                delay = 10f + (arange / 120f + 0.5f) * 140f + rand.random(15f);
                speed = 0.015f;
                
                length1 *= 1.7f;
                length2 *= 1.7f;
                insize *= 2.7f;
            }});
        }

        arms.addAll(
                new DespondencyArm("spear"){{
                    x = 60f;
                    y = -4f;

                    tx = 38f;
                    ty = 12f;
                    trot = -25f;
                    armrot = 25f;
                    handrot = 17f;
                    weaponScl = 2f;
                    weaponOffset = 8f;
                    scl = 1.2f;
                    tscl = 0f;

                    delay = 90f;
                }},

                new DespondencyArm("spear"){{
                    x = 60f;
                    y = -4f;

                    tx = 48f;
                    ty = -2f;
                    trot = -35f;
                    armrot = 12f;
                    handrot = 17f;
                    weaponScl = 2f;
                    weaponOffset = 8f;
                    scl = 1.2f;
                    tscl = 0.5f;

                    delay = 60f;
                }},

                new DespondencyArm("halberd"){{
                    x = 60f;
                    y = -4f;

                    tx = 35f;
                    ty = -30f;
                    trot = -65f;
                    armrot = 20f;
                    handrot = 15f;
                    weaponScl = 1.6f;
                    weaponOffset = 15f;
                    scl = 1.2f;
                    tscl = 0.6f;

                    delay = 30f;
                }},

                new DespondencyArm("claymore"){{
                    x = 60f;
                    y = -4f;

                    tx = 20f;
                    ty = -45f;
                    trot = -95f;
                    armrot = 12f;
                    handrot = 12f;
                    weaponScl = 1.75f;
                    weaponOffset = 42f;
                    scl = 1.2f;
                    tscl = 0.1f;
                }}
        );
    }

    @Override
    public void load(){
        super.load();
        armRegions = new TextureRegion[2][4];

        for(int i = 0; i < 2; i++){
            String outline = i == 0 ? "-outline" : "";
            armRegions[i][0] = Core.atlas.find("flameout-despondency-h-arm" + outline);
            armRegions[i][1] = Core.atlas.find("flameout-despondency-f-arm" + outline);
            armRegions[i][2] = Core.atlas.find("flameout-despondency-hand" + outline);
            armRegions[i][3] = Core.atlas.find("flameout-despondency-hand2" + outline);
        }

        for(DespondencyArm arm : arms){
            arm.weapon = Core.atlas.find("flameout-despondency-" + arm.name);
        }
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        DespondencyMount m = (DespondencyMount)mount;

        //boolean shoot = (mount.shoot || m.active) && unit.controller() instanceof DespondencyAI && ((mount.target instanceof Unit uu && uu.isValid()) || m.stage > 2);
        boolean shoot = (mount.shoot || m.active) && unit.controller() instanceof DespondencyAI && (mount.target instanceof Unit uu && uu.isValid() || m.targetDestroyed);
        if(shoot){
            m.time += Time.delta;
            for(DespondencyArm arm : arms){
                if(m.time >= arm.delay){
                    m.times[arm.id] = Mathf.lerpDelta(m.times[arm.id], 1f, arm.speed);
                }
            }
            m.magicIn = Mathf.clamp(m.magicIn + Time.delta / 180f);
            m.magicOut = Mathf.clamp(m.magicOut - Time.delta / 30f);

            //1: destroy strays.
            //2: destroy large enemies
            //3: destroy main
            //(target != null && Angles.within(unit.rotation, unit.angleTo(target), 15f))
            
            //if(m.activeTime > 120)
            if((mount.target != null && Angles.within(unit.rotation, unit.angleTo(mount.target), 15f) && m.activeTime > 120f) || m.stage > 0 || m.activeTime > 5f * 60f){
                m.stageTime -= Time.delta;
                if(!m.active) m.active = true;
                if(m.stageTime <= 0f){
                    switch(m.stage){
                        case 0 -> {
                            destroyStrays(unit, m);
                            m.stageTime = 1.5f * 60f;
                            //Log.info("des s1");
                        }
                        case 1 -> {
                            destroyStrong(unit, m);
                            m.stageTime = 2f * 60f;
                            //Log.info("des s2");
                        }
                        case 2 -> {
                            destroyMain(unit, m);
                            //m.stageTime = 10f * 60f;
                            m.stageTime = 25f * 60f;
                            m.finalActive = true;
                            //Log.info("des s3");
                        }
                        case 3 -> {
                            m.stage = 0;
                            m.stageTime = 0f;
                            m.active = false;

                            endFinal(m);

                            if(unit.controller() instanceof DespondencyAI ai) ai.endDeath();
                            //Log.info("des s4");
                            mount.target = null;
                            return;
                        }
                    }
                    m.stage++;
                }
            }
            if(m.finalActive) updateFinal(unit, m);
        }else{
            if(m.stage > 2){
                endFinal(m);
                if(unit.controller() instanceof DespondencyAI ai) ai.endDeathFailed();
            }
            m.time = 0f;
            for(int i = 0; i < m.times.length; i++){
                m.times[i] = Mathf.lerpDelta(m.times[i], 0f, 0.07f);
            }
            m.activeTime = Math.max(Math.min(m.activeTime - (m.activeTime / 76f) * Time.delta, m.activeTime - Time.delta), 0f);
            m.stage = 0;
            m.stageTime = 0f;
            m.active = false;

            if(m.magicIn > 0){
                m.magicOut = Mathf.clamp(m.magicOut + Time.delta / 60f);
                if(m.magicOut >= 1f){
                    m.magicIn = 0f;
                    m.magicOut = 0f;
                }
            }
        }
        if(!m.spears.isEmpty()){
            m.spears.removeAll(s -> {
                if(s.spear != m.lastSpear && m.lastSpear != null && m.lastSpear.collided && m.lastSpear.isAdded()){
                    s.spear.cx = m.lastSpear.cx;
                    s.spear.cy = m.lastSpear.cy;
                    s.spear.cr = m.lastSpear.cr;
                }
                if(m.finalActive && s.spear.collided){
                    m.spearCollided = true;
                }
                s.fade = Mathf.approachDelta(s.fade, (s.spear.fading() || !s.spear.isAdded() || !m.finalActive) ? -0.001f : 1f, 1f / 12f);
                return s.fade < 0f;
            });
        }
        if(!m.fragHands.isEmpty()){
            for(FragmentHands h : m.fragHands){
                h.update(unit, m);
            }
            m.fragHands.remove(h -> {
                //h.update(unit);
                //h.update(unit, m);
                return h.fade >= 1f;
            });
        }
    }

    void destroyStrays(Unit unit, DespondencyMount mount){
        DesShockWaveEntity.create(unit.team, mount.target, unit.x, unit.y);
    }
    void destroyStrong(Unit unit, DespondencyMount mount){
        /*
        mount.strongAngles.clear();
        for(Unit u : Groups.unit){
            if(u.team != unit.team && !(isStray(unit)) && u != mount.target){
                float ang = unit.angleTo(u);
                mount.strongAngles.add(ang);
                EmpathyDamage.damageUnit(u, u.maxHealth + 1000f, true, () -> SpecialDeathEffects.get(u.type).deathUnit(u, u.x, u.y, ang));
            }
        }
        mount.strongTime = 60f;
        */
        for(Unit u : Groups.unit){
            if(u.team != unit.team && !isStray(u) && u != mount.target){
                float ang = unit.angleTo(u);
                Vec2 v = Utils.v.trns(ang, 240f).add(unit.x, unit.y);
                DesSpearEntity.create(u, v.x, v.y, ang + Mathf.range(45f), true);
            }
        }
    }
    void destroyMain(Unit unit, DespondencyMount mount){
        //
    }

    void updateFinal(Unit unit, DespondencyMount m){
        if(!(m.target instanceof Unit u)) return;

        m.finalTime += Time.delta;
        m.spearTime -= Time.delta;
        if(m.spearTime <= 0f && !m.spearFinal){
            int sc = m.spears.size / 2;
            int sside = m.spears.size % 2 == 0 ? 1 : -1;
            
            float offset = ((unit.hitSize / 2f) / (1f + Mathf.pow(sc, 2f))) * sside;
            
            Vec2 v = Tmp.v1.set(0f, offset).rotate(unit.rotation), v2 = Tmp.v2.rnd(Mathf.random(u.hitSize / 2.1f)), v3 = Tmp.v3.trns(unit.rotation, (unit.hitSize / 2f) * 1.6f);
            float val = 1f / (1f + m.spears.size / 2f);

            //DesSpearEntity spear = DesSpearEntity.create(u, v.x + unit.x, v.y + unit.y, v.angleTo(u.x + v2.x, u.y + v2.y), false);
            DesSpearEntity spear = DesSpearEntity.create(u, v.x + unit.x + v3.x, v.y + unit.y + v3.y, Angles.angle(unit.x + v.x + v3.x, unit.y + v.y + v3.y, u.x + v2.x, u.y + v2.y), false);
            spear.forceScl = val;
            spear.damageScl = val / 4f;
            spear.tx = v2.x;
            spear.ty = v2.y;
            //spear.size *= Mathf.random(0.9f, 1.1f);
            spear.size *= 1f + sc / 8f;
            spear.crySound = m.spears.isEmpty();

            if(m.lastSpear != null){
                spear.last = m.lastSpear;
            }

            v.rotate(-unit.rotation);

            //m.spears.add(spear);
            SpearHolder sh = new SpearHolder(spear, v.x, v.y, (v.dst(u) - u.hitSize / 2f) * 0.6f);
            sh.flipped = v.y < 0;
            m.spears.add(sh);
            m.lastSpear = spear;

            m.spearTime = Mathf.random(7f, 15f);
        }

        if(m.spears.size >= 6 && !m.spearFinal){
            m.spearFinal = true;
        }
        if(m.spearFinal && m.spearCollided && m.lastSpear != null){
            m.spearCollidedTime = Mathf.clamp(m.spearCollidedTime + Time.delta / 120f);

            DesSpearEntity spr = m.lastSpear;
            Vec2 v = Tmp.v1.trns(unit.rotation, 480f).add(unit.x, unit.y);
            float dx = -(spr.cx - v.x) * 0.05f * m.spearCollidedTime, dy = -(spr.cy - v.y) * 0.05f * m.spearCollidedTime;
            spr.cx += dx;
            spr.cy += dy;
        }

        if(m.finalTime >= 6f * 60f && m.stageTime > 4f * 60f + 90f){
            SpecialDeathEffects eff = SpecialDeathEffects.get(u.type);
            Runnable run = eff.solid ? () -> {
                u.elevation = 0f;

                FragmentationBatch batch = FlameOut.fragBatch;
                batch.baseElevation = 0f;
                batch.trailEffect = Fx.none;
                batch.explosionEffect = eff.explosionEffect != Fx.none ? eff.explosionEffect : null;
                batch.altFunc = (hx, hy, tex) -> {};
                batch.fragColor = Color.white;
                batch.useAlt = false;
                batch.resScale = 0.5f;
                batch.islandScl = 0.075f;
                batch.genGore = eff.hasInternal;
                batch.goreColor = eff.internalColor;
                batch.sound = eff.deathSound;
                //batch.onDeathFunc = fragDeath;
                batch.onDeathFunc = eff.fragDeath;
                batch.fragFunc = e -> {
                    e.z = 0f;
                    e.lifetime = 4f * 60f + Mathf.random(25f);
                    e.impact = true;
                    e.vx = 0f;
                    e.vy = 0f;
                    e.vz = 0f;
                    e.vr = 0f;

                    m.fragHands.add(new FragmentHands(unit, Mathf.range(4f), Mathf.range(24f), u, e));
                };

                batch.switchBatch(u::draw);
            } : () -> eff.cutAlt(u);
            EmpathyDamage.damageUnit(u, u.maxHealth + 1000f, true, run);
            m.stageTime = eff.solid ? 4f * 60f + 90f : 120f;
            m.targetDestroyed = true;
        }
    }
    void endFinal(DespondencyMount m){
        m.spears.clear();
        m.finalTime = 0f;
        m.spearTime = 0f;
        m.lastSpear = null;
        m.spearFinal = false;

        m.spearCollided = false;
        m.spearCollidedTime = 0f;

        m.finalActive = false;
        m.targetDestroyed = false;
        m.finalTime = 0f;
    }

    public static boolean isStray(Unit unit){
        float v = (FlameOutSFX.inst.getUnitDps(unit.type) + unit.maxHealth * unit.healthMultiplier);
        return v < 140000 && !EmpathyDamage.isNaNInfinite(v);
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
        //drawMain(unit, mount);
        DespondencyMount m = (DespondencyMount)mount;
        float z = Draw.z();

        Draw.z(Layer.bullet);

        /*
        if(m.strongTime > 0){
            for(int i = 0; i < size; i++){
                float ang = angles[i];
                int seed = i ^ (unit.id << 8);

                Vec2 v = Tmp.v1.trns(ang, 50f).add(unit.x, unit.y);
                drawLasers(seed, v.x, v.y, ang, m.strongTime / 60f);
            }
        }
         */
        if(m.magicIn > 0) drawMagic(unit, m);

        Draw.z(z);
    }

    private final static Color[] lcolors = {FlamePal.red, FlamePal.redLight, Color.white, Color.black};
    public static void drawLasers(int id, float x, float y, float rotation, float fout){
        float wScl = 1f;
        float width = fout * 20f;
        float trns = 120f;
        float endLength = 800f;
        float length = 9500f;

        Vec2 v = Tmp.v1.trns(rotation, trns).add(x, y);
        Vec2 end = Tmp.v2.trns(rotation, length).add(x, y);
        float vx = v.x, vy = v.y;
        float ex = end.x, ey = end.y;

        for(Color c : lcolors){
            float w = width * wScl;
            Draw.color(c);
            float rx = Mathf.range(2f * fout), ry = Mathf.range(2f * fout);
            float endl = endLength * wScl;

            for(int j = 0; j < 8; j++){
                Vec2 v2 = Tmp.v1.trns(((j / 8f) - 0.5f) * 180f + 180, 1f).scl(trns, w).rotate(rotation).add(vx, vy);
                Vec2 v3 = Tmp.v2.trns((((j + 1f) / 8f) - 0.5f) * 180f + 180, 1f).scl(trns, w).rotate(rotation).add(vx, vy);
                Fill.tri(vx + rx, vy + ry, v2.x + rx, v2.y + ry, v3.x + rx, v3.y + ry);

                v2.trns(((j / 8f) - 0.5f) * 180f, 1f).scl(endl, w).rotate(rotation).add(ex, ey);
                v3.trns((((j + 1f) / 8f) - 0.5f) * 180f, 1f).scl(endl, w).rotate(rotation).add(ex, ey);

                Fill.tri(ex + rx, ey + ry, v2.x + rx, v2.y + ry, v3.x + rx, v3.y + ry);
            }
            Lines.stroke(w * 2f);
            Lines.line(vx + rx, vy + ry, ex + rx, ey + ry, false);

            wScl *= 0.75f;
        }
        wScl /= 0.75f;
        width *= wScl;
        endLength *= wScl;

        Rand r = Utils.rand, r2 = Utils.rand2;
        r.setSeed(id);
        for(int i = 0; i < 75; i++){
            float dur = r.random(30f, 45f);
            float t = (Time.time + r.random(dur)) / dur;
            float mt = t % 1f;
            int seed = (int)(t) + r.nextInt();

            float slope = Interp.pow2Out.apply(Mathf.curve(Mathf.slope(mt), 0f, 0.1f));

            r2.setSeed(seed);
            float ofs = r2.range(1f);
            float rwid = r2.random(width / 2f, width / 1.6f) * fout * slope;
            //float rheight = rwid * 5f + r2.random(100f, 140f);
            float rheight = rwid * 5f + r2.random(310f, length / 12f);
            float widoff = ((width * fout) - rwid / 2f) * ofs;
            float lenoff = Interp.circleIn.apply(Math.abs(ofs)) * trns + rheight;
            float endoff = length + endLength * (1f - Interp.circleIn.apply(Math.abs(ofs))) - rheight / 1.5f;

            //Color tmc = Tmp.c1.set(colors[1]).lerp(colors[2], r2.nextFloat());
            Color tmc = Tmp.c1.set(FlamePal.red).lerp(Color.white, Interp.pow2.apply(r2.nextFloat()));

            Draw.color(tmc);
            Vec2 p1 = Tmp.v1.trns(rotation, Mathf.lerp(lenoff, endoff, mt), widoff + Mathf.range(2f * fout)).add(x, y);
            GraphicUtils.diamond(p1.x, p1.y, rwid, rheight, rotation);
        }
    }

    @Override
    public void drawOutline(Unit unit, WeaponMount mount){
        drawMain(unit, mount);
    }

    void drawMagic(Unit unit, DespondencyMount m){
        Draw.color(FlamePal.redLight);
        float fout = 1f - m.magicOut;
        float fin = m.magicIn;

        float rad1 = 150f + fin * 150f;
        Lines.stroke(3f * fout);
        EmpathySpawner.progressiveCircle(unit.x, unit.y, rad1, 90f, fin);
        EmpathySpawner.progressiveCircle(unit.x, unit.y, rad1 / 3f, 180f, fin);
        EmpathySpawner.progressiveStar(unit.x, unit.y, rad1 / 3f, Time.time * 0.15f, 7, 2, fin);
        Lines.stroke(fout);
        EmpathySpawner.progressiveCircle(unit.x, unit.y, rad1 - 5f, -90f, fin);
        EmpathySpawner.progressiveCircle(unit.x, unit.y, rad1 / 2f, -90f, fin);
        EmpathySpawner.progressiveStar(unit.x, unit.y, rad1 / 2f, Time.time * 0.15f, 9, 2, fin);
        EmpathySpawner.progressiveStar(unit.x, unit.y, rad1 / 2f, Time.time * -0.15f, 4, 1, fin);
        Lines.stroke(3f * fout);
        EmpathySpawner.progressiveStar(unit.x, unit.y, rad1, Time.time * 0.1f, 4, 1, fin);
        EmpathySpawner.progressiveStar(unit.x, unit.y, rad1, -Time.time * 0.1f * 1.1f, 4, 1, fin);
        
        EmpathySpawner.progressiveStar(unit.x, unit.y, rad1, -90f, 3, 1, fin);
        for(int i = 0; i < 4; i++){
            //draw circles
            float ang = i * 90f + Time.time * 0.2f;
            Vec2 v = Tmp.v1.trns(ang, rad1).add(unit.x, unit.y);
            Lines.stroke(3f * fout);
            EmpathySpawner.progressiveCircle(v.x, v.y, 90f, ang, fin);
            EmpathySpawner.progressiveStar(v.x, v.y, 90f, -90f, 5, 2, fin);
            Lines.stroke(fout);
            EmpathySpawner.progressiveCircle(v.x, v.y, 82f, ang, fin);
            EmpathySpawner.progressiveStar(v.x, v.y, 82f, 0f, 4, 1, fin);
            Lines.stroke(fout * 0.75f);
            EmpathySpawner.progressiveStar(v.x, v.y, 90f, 90f, 3, 1, fin);

            //Vec2 v = Tmp.v1.trns(ang, rad1).add(unit.x, unit.y);
            v.trns(ang, rad1 + 110f).add(unit.x, unit.y);
            Drawf.tri(v.x, v.y, 30f * fin * fout, 120f, ang);
            Drawf.tri(v.x, v.y, 30f * fin * fout, 20f, ang + 180f);
        }

        Draw.reset();
    }
    
    void drawMain(Unit unit, WeaponMount mount){
        DespondencyMount m = (DespondencyMount)mount;

        unit.type.applyColor(unit);

        if(!m.spears.isEmpty()){
            float z = Draw.z();
            for(SpearHolder spear : m.spears){
                float fade = spear.fade * (1f - Mathf.clamp((m.finalTime - 4f * 60f) / 12f));
                float dz = Math.min(spear.spear.getZ(), z);

                Draw.z(dz);
                drawFinalSpears(unit, spear, fade, armRegions[0]);
                spear.spear.drawRaw();
                drawFinalSpears(unit, spear, fade, armRegions[1]);
            }
            Draw.z(z);
        }
        if(!m.fragHands.isEmpty()){
            for(FragmentHands h : m.fragHands){
                h.draw(unit);
            }
        }

        for(DespondencyArm arm : arms){
            if(m.times[arm.id] <= 0.001f) continue;
            for(int i = 0; i < 2; i++){
                arm.draw(unit, m.times[arm.id], i == 1);
            }
        }
        Draw.reset();
    }
    void drawFinalSpears(Unit unit, SpearHolder holder, float fade, TextureRegion[] set){
        Vec2 v = Tmp.v1.trns(unit.rotation, holder.x, holder.y).add(unit.x, unit.y);
        Vec2 end = Tmp.v2.set(holder.spear.x, holder.spear.y).add(Tmp.v4.trns(holder.spear.rotation, -50f * holder.spear.size));
        float ang = v.angleTo(end);
        float dst = v.dst(end);
        float dst2 = v.dst(holder.spear.x, holder.spear.y);
        int s = holder.flipped ? 1 : -1;

        Vec2 mid = Tmp.v3.set(v.x, v.y).lerp(end.x, end.y, 0.5f).add(Tmp.v4.trns(ang + 90f * -s, 25f * Mathf.clamp(1f - (dst2 / holder.dst))));

        if(dst < 8f) return;

        float scl = holder.spear.size * 0.5f;

        TextureRegion tr = Tmp.tr1, hand = set[2], fore = set[1], hind = set[0];
        tr.set(hand);
        float mu = Mathf.lerp(hand.v, hand.v2, 0.5f);
        tr.setV(Mathf.lerp(mu, hand.v, fade));
        tr.setV2(Mathf.lerp(mu, hand.v2, fade));

        Draw.rect(tr, end.x, end.y, tr.width * Draw.scl * scl, tr.height * Draw.scl * s * scl, holder.spear.rotation);

        tr.set(fore);
        mu = Mathf.lerp(fore.v, fore.v2, 0.5f);
        tr.setV(Mathf.lerp(mu, fore.v, fade));
        tr.setV2(Mathf.lerp(mu, fore.v2, fade));

        float dx = (end.x - mid.x) * 0.08f, dy = (end.y - mid.y) * 0.08f;
        Lines.stroke(tr.height * Draw.scl * scl * s);
        Lines.line(tr, mid.x - dx, mid.y - dy, end.x + dx * 0.2f, end.y + dy * 0.2f, false);

        tr.set(hind);
        mu = Mathf.lerp(hind.v, hind.v2, 0.5f);
        tr.setV(Mathf.lerp(mu, hind.v, fade));
        tr.setV2(Mathf.lerp(mu, hind.v2, fade));

        dx = (mid.x - v.x) * 0.15f;
        dy = (mid.y - v.y) * 0.15f;
        Lines.stroke(tr.height * Draw.scl * scl * s);
        Lines.line(tr, v.x - dx, v.y - dy, mid.x + dx, mid.y + dy, false);
    }

    @Override
    public void init(){
        super.init();

        for(int i = 0; i < arms.size; i++){
            DespondencyArm arm = arms.get(i);
            arm.id = i;
        }
    }

    public static class DespondencyMount extends WeaponMount{
        float time;
        float[] times;

        public float activeTime;
        public int stage;
        public float stageTime;
        boolean active;

        boolean finalActive;
        float finalTime;
        Seq<SpearHolder> spears = new Seq<>();
        DesSpearEntity lastSpear;
        float spearTime;
        boolean spearFinal = false;

        boolean spearCollided = false;
        float spearCollidedTime;

        Seq<FragmentHands> fragHands = new Seq<>();
        boolean targetDestroyed = false;

        float magicIn, magicOut;

        DespondencyMount(Weapon w){
            super(w);

            times = new float[((EndDespondencyWeapon)w).arms.size];
        }
    }

    static Vec2 v1 = new Vec2(), v2 = new Vec2(), v3 = new Vec2(), v4 = new Vec2(), v5 = new Vec2(), v6 = new Vec2();

    class FragmentHands{
        float time, timeB;
        float fade;
        float length;
        boolean flipped;

        //source position
        float sx, sy;
        //position of targeted unit
        float tx, ty;
        //hand position, joint position
        float hx, hy, jx, jy;

        float cx, cy, cr;
        FragmentEntity frag;
        boolean holding, move2;

        float nx, ny;
        float delay;
        float scl;

        FragmentHands(Unit owner, float x, float y, Unit target, FragmentEntity f){
            frag = f;

            tx = target.x;
            ty = target.y;

            Vec2 v = v1.trns(owner.rotation, x, y).add(owner.x, owner.y);
            hx = jx = v.x;
            hy = jy = v.y;
            sx = x;
            sy = y;

            length = f.dst(v.x, v.y) * 1.1f;
            flipped = y < 0;
            delay = Mathf.random(60f);
            scl = Math.min(Mathf.pow((f.boundSize / 24f), 0.5f), 2f);
        }

        void update(Unit unit, DespondencyMount mount){
            if((delay -= Time.delta) > 0) return;
            time = Mathf.clamp(time + Time.delta / 25f);
            //InverseKinematics.solve(legLength/2f, legLength/2f, Tmp.v6.set(l.base).sub(baseOffset), side, jointDest);
            //jointDest.add(baseOffset);

            Vec2 src = v6.trns(unit.rotation, sx, sy).add(unit);
            if(!holding){
                Vec2 t = v1.trns(unit.rotation, sx, sy).add(unit).lerp(frag, time);
                hx = t.x;
                hy = t.y;
                float ang = Angles.angle(jx, jy, hx, hy);
                if(time >= 1f){
                    v5.set(frag.x - hx, frag.y - hy).rotate(-ang);
                    cr = frag.rotation - ang;
                    //cx = frag.x - hx;
                    //cy = frag.y - hy;
                    cx = v5.x;
                    cy = v5.y;
                    holding = true;

                    //main.explosionEffect.at(x, y, area / 2, main.effectColor);
                    //Effect.shake(area / 3f, area / 4f, x, y);
                    //if(main.onDeath != null) main.onDeath.get(this);
                    //if(main.explosionSound != Sounds.none) main.explosionSound.at(x, y, 1f, Mathf.clamp(area / 8f));
                    //if(main.explosionSound != Sounds.none) main.explosionSound.at(x, y, Mathf.random(0.9f, 1.1f) * Math.max(1f / (1f + (area - 8f) / 70f), 0.5f), Mathf.clamp(area / 1.1f));
                    if(frag.main.explosionSound != Sounds.none) frag.main.explosionSound.at(frag.x, frag.y, Mathf.random(0.9f, 1.1f) * Math.max(1f / (1f + (frag.area - 8f) / 70f), 0.5f), Mathf.clamp(frag.area / 8.1f));
                    frag.main.explosionEffect.at(frag.x, frag.y, frag.area / 2.2f, frag.main.effectColor);
                    Effect.shake(frag.area / 3.2f, frag.area / 4.2f, frag.x, frag.y);
                    if(frag.main.onDeath != null) frag.main.onDeath.get(frag);

                    float dx = (frag.x - tx) * 2f;
                    float dy = (frag.y - ty) * 2f;
                    v4.set(dx, dy).limit(length * 0.6f).add(t).sub(unit).limit(length * 0.9f);
                    
                    //float mx = v4.x + unit.x;
                    //float my = v4.y + unit.y;
                    //float mrot = Mathf.slerp(Angles.angle(jx, jy, mx, my), Angles.angle(src.x, src.y, jx, jy), 0.5f);
                    //v5.set(mx, my).sub(jx, jy).setAngle(mrot).add(jx, jy).sub(unit);
                    //v4.set(v5);
                    
                    v4.rotate(-unit.rotation);
                    nx = v4.x;
                    ny = v4.y;
                    
                    int side = (flipped ? -1 : 1) * ny > 0 ? 1 : -1;
                    ny *= side;
                }
            }else{
                timeB += Time.delta;
                if(timeB > 40f && !move2){
                    float angDst = Angles.angleDist(unit.rotation, Angles.angle(src.x, src.y, hx, hy)) * 1.25f;
                    v3.set(hx, hy).sub(src).rotate(-unit.rotation).setLength(length * 0.95f).rotate((flipped ? -1 : 1) * (7f + angDst));
                    nx = v3.x;
                    ny = v3.y;
                    move2 = true;
                }
                float speed = move2 ? Mathf.clamp((timeB - 40f) / 40f) * 0.02f : 0.05f;

                Vec2 v = v4.set(nx, ny).rotate(unit.rotation).add(unit);
                hx = Mathf.lerpDelta(hx, v.x, speed);
                hy = Mathf.lerpDelta(hy, v.y, speed);
            }
            float time2 = Mathf.clamp(time * 1.75f);
            InverseKinematics.solve((length / 2f) * time2, (length / 2f) * time2, v2.set(hx, hy).sub(src), !flipped, v3);
            Vec2 j = v3.add(src);
            jx = Mathf.lerpDelta(jx, j.x, 0.5f);
            jy = Mathf.lerpDelta(jy, j.y, 0.5f);

            if(holding){
                float ang = Angles.angle(jx, jy, hx, hy);
                Vec2 v = v5.set(cx, cy).rotate(ang).add(hx, hy);
                frag.x = v.x;
                frag.y = v.y;
                frag.rotation = cr + ang;
            }

            if((!frag.isAdded() || frag.time >= frag.lifetime) || !mount.finalActive){
                fade += Time.delta / 12f;
            }
        }

        void draw(Unit unit){
            if(delay > 0 || fade >= 1f) return;
            TextureRegion[][] regs = armRegions;
            Vec2 src = v1.trns(unit.rotation, sx, sy).add(unit);
            float ang = Mathf.slerp(Angles.angle(jx, jy, hx, hy), Angles.angle(src.x, src.y, hx, hy), 0.05f);
            float fin = Mathf.clamp(fade + Mathf.clamp(1f - time * 2f));
            int s = flipped ? 1 : -1;
            //Vec2 t = Tmp.v1.trns(unit.rotation, sx, sy).add(unit).lerp(frag, time);

            for(int i = 0; i < 2; i++){
                TextureRegion[] set = regs[i];

                TextureRegion tr = Tmp.tr1, hand = set[3], fore = set[1], hind = set[0];
                tr.set(hand);
                float mu = Mathf.lerp(hand.v, hand.v2, 0.5f);
                tr.setV(Mathf.lerp(hand.v, mu, fin));
                tr.setV2(Mathf.lerp(hand.v2, mu, fin));

                Draw.rect(tr, hx, hy, tr.width * Draw.scl * scl, tr.height * s * Draw.scl * scl, ang);

                tr.set(fore);
                mu = Mathf.lerp(fore.v, fore.v2, 0.5f);
                tr.setV(Mathf.lerp(fore.v, mu, fin));
                tr.setV2(Mathf.lerp(fore.v2, mu, fin));

                //float dx = (end.x - mid.x) * 0.08f, dy = (end.y - mid.y) * 0.08f;
                //Lines.stroke(tr.height * Draw.scl * scl * s);
                //Lines.line(tr, mid.x - dx, mid.y - dy, end.x + dx * 0.2f, end.y + dy * 0.2f, false);

                float dx = (hx - jx) * 0.08f, dy = (hy - jy) * 0.08f;
                Lines.stroke(tr.height * Draw.scl * s * scl);
                Lines.line(tr, jx - dx, jy - dy, hx + dx * 0.2f, hy + dy * 0.2f, false);

                tr.set(hind);
                mu = Mathf.lerp(hind.v, hind.v2, 0.5f);
                tr.setV(Mathf.lerp(hind.v, mu, fin));
                tr.setV2(Mathf.lerp(hind.v2, mu, fin));

                //dx = (mid.x - v.x) * 0.15f;
                //dy = (mid.y - v.y) * 0.15f;
                //Lines.stroke(tr.height * Draw.scl * scl * s);
                //Lines.line(tr, v.x - dx, v.y - dy, mid.x + dx, mid.y + dy, false);
                dx = (jx - src.x) * 0.1f;
                dy = (jy - src.y) * 0.1f;
                Lines.stroke(tr.height * Draw.scl * s * scl);
                Lines.line(tr, src.x - dx, src.y - dy, jx + dx, jy + dy, false);
            }
        }
    }

    static class SpearHolder{
        DesSpearEntity spear;
        float x, y;
        float fade;
        float dst;
        boolean flipped;

        SpearHolder(DesSpearEntity spear, float x, float y, float dst){
            this.spear = spear;
            this.x = x;
            this.y = y;
            this.dst = dst;
        }
    }

    class DespondencyArm{
        float x, y, rotation = -30f;
        float tx, ty, trot;
        float length1 = 64f - 5f, length2 = 68f - 5f;
        float insize = 5f;
        float scl = 1f;
        float tscl = 0f;

        float armrot, handrot = 4f;
        float delay;
        float speed = 0.02f;

        String name;
        TextureRegion weapon;
        float weaponOffset = 10f;
        float weaponScl = 1f;

        int id;

        DespondencyArm(String name){
            this.name = name;
        }

        void draw(Unit unit, float progress, boolean flip){
            int s = flip ? -1 : 1;
            Vec2 v = Tmp.v1.trns(unit.rotation - 90f, (x + tx * progress) * s, y + ty * progress).add(unit.x, unit.y);
            float rx = v.x, ry = v.y;
            float rprog = progress * 0.75f + 0.25f;
            float fscl = scl + tscl * progress;
            float len1 = length1 * fscl * rprog, len2 = length2 * fscl * rprog;

            float rot = unit.rotation + (rotation + trot * progress) * s;
            float rot2 = rot + (180f - (180f - armrot) * progress) * s;
            float rot3 = rot2 + handrot * progress * s;

            Tmp.v1.trns(rot, len1).add(rx, ry);
            float j1x = Tmp.v1.x, j1y = Tmp.v1.y;
            Tmp.v1.trns(rot2, len2).add(j1x, j1y);
            float j2x = Tmp.v1.x, j2y = Tmp.v1.y;
            Tmp.v1.trns(rot3, 12f * fscl).add(j2x, j2y);
            float hx = Tmp.v1.x, hy = Tmp.v1.y;

            Tmp.v1.set(j1x, j1y).sub(rx, ry).nor();
            float n1x = Tmp.v1.x, n1y = Tmp.v1.y;
            Tmp.v1.set(j2x, j2y).sub(j1x, j1y).nor();
            float n2x = Tmp.v1.x, n2y = Tmp.v1.y;

            for(int i = 0; i < 2; i++){
                TextureRegion root = armRegions[i][0], end = armRegions[i][1], hand = armRegions[i][2];

                Lines.stroke(root.height * Draw.scl * s * fscl);
                Draw.rect(hand, hx, hy, hand.width * Draw.scl * fscl, hand.height * Draw.scl * fscl * s, rot3);
                Lines.line(end, j1x - n2x * fscl * 5f, j1y - n2y * fscl * 5f, j2x + n2x * fscl * 5f, j2y + n2y * fscl * 5f, false);
                Lines.line(root, rx - n1x * fscl * 5f, ry - n1y * fscl * 5f, j1x + n1x * fscl * insize, j1y + n1y * fscl * insize, false);

                if(i == 0){
                    float cprog = Mathf.curve(progress, 0f, 0.85f);
                    //Vec2 v2 = Tmp.v1.trns(rot3, weaponOffset * fscl + (-(weaponOffset / (weapon.width * (scl + tscl * progress) * Draw.scl)) * (1f - cprog)));
                    //Vec2 v2 = Tmp.v1.trns(rot3, weaponOffset * fscl);
                    //Vec2 v2 = Tmp.v1.trns(rot3, weaponOffset * fscl);
                    
                    TextureRegion reg = Tmp.tr1;
                    reg.set(weapon);

                    float mid = Mathf.lerp(reg.u, reg.u2, Mathf.clamp(0.5f - (weaponOffset / (weapon.width * Draw.scl))));
                    reg.setU(Mathf.lerp(mid, weapon.u, cprog));
                    reg.setU2(Mathf.lerp(mid, weapon.u2, cprog));

                    float width = reg.width * fscl * Draw.scl * weaponScl;
                    float height = reg.height * fscl * Draw.scl * weaponScl * s;
                    //Vec2 v2 = Tmp.v1.trns(rot3, weaponOffset * fscl + (-(weaponOffset * (scl + tscl * progress)) * (1f - cprog)));
                    Vec2 v2 = Tmp.v1.trns(rot3, weaponOffset * (width / (weapon.width * Draw.scl)));

                    Draw.rect(reg, hx + v2.x, hy + v2.y, width, height, rot3);
                    //Draw.rect(reg, hx + width / 2f - weaponOffset, hy, width, height, width / 2f - weaponOffset, height / 2f, rot3);
                    //Draw.rect(reg, hx + (width - weaponOffset) / 2f, hy, width, height, width / 2f - weaponOffset, height / 2f, rot3);
                }
            }
        }
    }
}
