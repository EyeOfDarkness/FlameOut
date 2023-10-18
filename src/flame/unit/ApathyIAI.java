package flame.unit;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.bullets.*;
import flame.bullets.ApathySmallLaserBulletType.*;
import flame.effects.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.audio.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;

public class ApathyIAI implements UnitController{
    ApathyIUnit unit;
    Interval scanTimer = new Interval(2);

    int currentTransformation = 0;

    //IntSeq nextTransformations = new IntSeq();
    float transformationTime = 2f * 60f;
    float transformationSpeed = 1f;

    float critDamage = 0f;
    float critStun = 0f;
    float maxCritDamage = 0f;
    
    public float strongLaserScore = 0f;

    float[] directionalBias = new float[32];
    float[] vision = new float[40];

    float[] shiftScore = new float[5];

    int[] shiftUses = new int[5];
    float[] shiftUseTimes = new float[5];

    float bulletCount = 0f;
    float flyingCount = 0f;
    float groundCount = 0f;
    
    boolean init = false;
    boolean unchanged = false;

    float strongestValue = 0f;
    Teamc strongest, nearest;
    Teamc nearestCore;

    float noEnemyTime = 0f;

    Bullet laser;
    //protected SoundLoop sound;
    protected SoundLoop[] sounds;
    protected boolean[] soundPlaying;
    float data1 = 0f, data2 = 0f;
    Vec2 vec = new Vec2();

    float stressScaled = 0f;

    float laserX = Float.NaN;
    float laserY = Float.NaN;

    float distance = 0f;

    static float scanRange = 1100f;
    static float shiftUseTime = 15f * 60f;
    static Rect tr = new Rect();
    static boolean updateScore = true;

    static Vision vis = new Vision();

    static Teamc st = null, nt = null;
    static float sts = 0f, nts = 0f;
    static boolean found = false;

    public ApathyIAI(){
        initSounds();
    }

    void initSounds(){
        sounds = new SoundLoop[]{new SoundLoop(FlameSounds.laserSmall, 2f), new SoundLoop(FlameSounds.laserBig, 2.5f)};
        soundPlaying = new boolean[2];
    }

    @Override
    public void updateUnit(){
        if(unit.dead){
            if(laser != null){
                laser.time = Math.max(laser.lifetime - 48f, laser.time);
            }
            for(SoundLoop s : sounds){
                s.stop();
            }
            return;
        }
        stressScaled = unit.getStressScaled();

        for(int i = 0; i < shiftUseTimes.length; i++){
            shiftUseTimes[i] = Math.max(0, shiftUseTimes[i] - Time.delta);
            if(shiftUses[i] > 0 && shiftUseTimes[i] <= 0f){
                shiftUses[i] = 0;
            }
        }

        if(scanTimer.get(0, 5f)){
            updateAI();

            //shiftScore[0] = 100f;
            //shiftScore[4] = 100f;
            //shiftScore[1] = 100f;
        }

        critDamage = Math.max(0, critDamage - (maxCritDamage / 180) * Time.delta);
        if(critDamage <= 0f){
            maxCritDamage = 0f;
        }

        Arrays.fill(soundPlaying, false);

        if(!unit.shifting){
            boolean f = true;
            if(transformationTime <= 0f){
                float lc = currentTransformation;
                updateSwitchForm();
                //critDamage = 0f;
                strongLaserScore = 0f;

                f = currentTransformation == lc;
            }
            if(transformationTime > 0f && f){
                float ttspeed = ((ApathyUnitType)unit.type).handlers.get(currentTransformation).stressAffected ? (1 + stressScaled / 2f) : 1f;
                transformationTime -= Time.delta * ttspeed * transformationSpeed;
                transformationSpeed = 1f;
                
                if(init){
                    init = false;
                    switch(currentTransformation){
                        case 1 -> initSmallLaser();
                        case 2 -> initAoE();
                        case 3 -> initSweep();
                        case 4 -> initLargeLaser();
                    }
                }
                switch(currentTransformation){
                    case 0 -> updateBase();
                    case 1 -> updateSmallLaser();
                    case 2 -> updateAoE();
                    case 3 -> updateSweep();
                    case 4 -> updateLargeLaser();
                }
            }
        }

        for(int i = 0; i < sounds.length; i++){
            if(!soundPlaying[i]){
                if(Float.isNaN(laserX) || Float.isNaN(laserY)){
                    sounds[i].update(unit.x, unit.y, false);
                }else{
                    sounds[i].update(laserX, laserY, false);
                }
            }
        }
        
        if(critStun > 0) critStun -= Time.delta;

        if(currentTransformation == 0){
            distance += unit.deltaLen();
            if(distance >= 128f){
                FlameSounds.idle.at(unit.x, unit.y, 1f);
                distance = 0f;
            }
        }

        //if(currentTransformation == 0) moveTo();
    }

    void updateBase(){
        moveTo();
    }

    void initLargeLaser(){
        laser = null;
        data1 = 0f;
        data2 = 0f;
    }
    void updateLargeLaser(){
        //
        if(strongest != null){
            float ato = unit.angleTo(strongest);
            //float adst = laser == null ? Math.max(Angles.angleDist(unit.rotation, ato) / 5f, 3f) : 0.05f;
            float adst = laser == null ? Math.max(Angles.angleDist(unit.rotation, ato) / 5f, 3f) : (laser.time < 140f ? 0.05f : 0.005f);

            unit.rotation = Angles.moveToward(unit.rotation, ato, adst * Time.delta * (1 + stressScaled / 3f));

            if(Angles.within(unit.rotation, ato, 0.1f) && data2 <= 0f && laser == null){
                data2 = FlameFX.bigLaserCharge.lifetime;
                FlameSounds.bigCharge.at(unit.x, unit.y, 1f);
                transformationTime = Math.max(transformationTime, data2 * 3f);
                FlameFX.bigLaserCharge.at(unit.x, unit.y, unit.rotation, unit);
            }
        }
        if(transformationTime <= 0f){
            if(laser != null && laser.type != null && laser.time < laser.type.lifetime) laser.time = laser.type.lifetime - 80;
        }
        if(laser != null){
            if(!laser.isAdded() || laser.owner != unit || laser.type != FlameBullets.bigLaser){
                laser = null;
                transformationTime = 0f;
            }else{
                Vec2 v = Utils.v.trns(unit.rotation, 40f).add(unit);
                laser.set(v.x, v.y);
                laser.rotation(unit.rotation);
                laser.damage = laser.type.damage * (1 + stressScaled / 4f);
                transformationSpeed = 0f;

                if(!Vars.headless){
                    Vec2 cpos = Core.camera.position;
                    Vec2 v2 = Tmp.v1.trns(laser.rotation(), 1800f);
                    Vec2 iv = Intersector.nearestSegmentPoint(v.x, v.y, v.x + v2.x, v.y + v2.y, cpos.x, cpos.y, Tmp.v2);
                    int idx = laser.time < 140 ? 0 : 1;

                    laserX = iv.x;
                    laserY = iv.y;

                    sounds[idx].update(laserX, laserY, true);
                    soundPlaying[idx] = true;

                    if(data1 <= 0f && laser.time >= 140){
                        data1 = 1f;
                        FlameSounds.laserCharge.at(laserX, laserY);
                    }
                }
            }
        }
        if(data2 > 0 && laser == null){
            data2 -= Time.delta;
            if(data2 <= 0f){
                Vec2 v = Utils.v.trns(unit.rotation, 40f).add(unit);
                laser = FlameBullets.bigLaser.create(unit, unit.team, v.x, v.y, unit.rotation);
                FlameFX.bigLaserFlash.at(unit.x, unit.y, unit.rotation);
                FlameSounds.laserCharge.at(unit.x, unit.y, 2f);
            }
        }
    }

    void initSmallLaser(){
        if(unchanged) return;
        //Vec2 v = Utils.v.trns(unit.rotation, 40f).add(unit);
        //laser = FlameBullets.smallLaser.create(unit, unit.team, v.x, v.y, unit.rotation);
        laser = null;
        data1 = 20f;
        data2 = 0f;
    }
    void updateSmallLaser(){
        if(strongest != null){
            float ato = unit.angleTo(strongest);
            float adst = laser == null ? Math.max(Angles.angleDist(unit.rotation, ato) / 5f, 3f) : 0.25f;
            unit.rotation = Angles.moveToward(unit.rotation, ato, adst * Time.delta * (1 + stressScaled / 3f));
            if(Angles.within(unit.rotation, ato, 0.1f) && data2 <= 0f && laser == null){
                data2 = 20f;
            }
            if(Angles.within(unit.rotation, ato, 40f)){
                data1 = 30f;
            }
        }else if(data1 <= 0f){
            transformationTime = 0f;
        }
        if(laser != null){
            if(laser.type == FlameBullets.smallLaser){
                Vec2 v = Utils.v.trns(unit.rotation, 40f).add(unit);
                laser.set(v.x, v.y);
                laser.rotation(unit.rotation);
                laser.damage = laser.type.damage * (1 + stressScaled / 4f);

                if(Units.invalidateTarget(strongest, unit.team, unit.x, unit.y) || data1 >= 10f){
                    laser.time = Math.min(laser.time, ApathySmallLaserBulletType.inEnd);
                    transformationSpeed = 0.5f;
                }

                if(!Vars.headless){
                    Vec2 cpos = Core.camera.position;
                    Vec2 v2 = Tmp.v1.trns(laser.rotation(), 1300f);
                    Vec2 iv = Intersector.nearestSegmentPoint(v.x, v.y, v.x + v2.x, v.y + v2.y, cpos.x, cpos.y, Tmp.v2);

                    laserX = iv.x;
                    laserY = iv.y;

                    sounds[0].update(laserX, laserY, true);
                    soundPlaying[0] = true;
                }
            }

            if(!laser.isAdded() || laser.owner != unit || laser.type != FlameBullets.smallLaser){
                laser = null;
                transformationTime = 0f;
            }
        }
        if(laser == null && data2 > 0f){
            data2 -= Time.delta;
            if(data2 <= 0f){
                Vec2 v = Utils.v.trns(unit.rotation, 40f).add(unit);
                laser = FlameBullets.smallLaser.create(unit, unit.team, v.x, v.y, unit.rotation);
                ApathyData d = new ApathyData();
                d.ai = this;
                laser.data = d;
                //FlameSounds.laserCharge.at(v.x, v.y);
                FlameSounds.laserCharge.play(1f, 1f, FlameSounds.laserCharge.calcPan(v.x, v.y));
                FlameFX.bigLaserFlash.at(v.x, v.y, unit.rotation);
            }
        }
        if(data1 >= 0f) data1 -= Time.delta;
    }

    void initSweep(){
        if(laser != null && unchanged) return;
        data1 = Mathf.random(360f);
        //data1 = unit.angleTo(strongest);
        data2 = 0f;
        
        laser = FlameBullets.sweep.create(unit, unit.team, unit.x, unit.y, data1);
        FlameSounds.laserCharge.play(0.5f, 1.5f, FlameSounds.laserCharge.calcPan(unit.x, unit.y));
        //laser.lifetime = FlameBullets.sweep.lifetime / (1 + stressScaled / 3f);
        laser.fdata = data1;
    }
    void updateSweep(){
        //
        if(laser != null){
            if(laser.type == FlameBullets.sweep){
                float rotation = data1 + (360f + 45f) * Interp.pow2.apply(Interp.pow2In.apply(data2 / laser.lifetime));
                laser.set(unit);
                laser.rotation(rotation);
                laser.team = unit.team;

                data2 = Math.min(data2 + Time.delta * (1 + stressScaled / 3f), laser.lifetime);
                transformationSpeed = 0f;
                
                if(!Vars.headless){
                    Vec2 cpos = Core.camera.position;
                    Vec2 v2 = Tmp.v1.trns(laser.rotation(), 1300f);
                    Vec2 iv = Intersector.nearestSegmentPoint(unit.x, unit.y, unit.x + v2.x, unit.y + v2.y, cpos.x, cpos.y, Tmp.v2);

                    laserX = iv.x;
                    laserY = iv.y;

                    sounds[0].update(laserX, laserY, true);
                    soundPlaying[0] = true;
                }
            }

            if(!laser.isAdded() || laser.type != FlameBullets.sweep){
                laser = null;
                transformationTime = 0f;
            }
        }else{
            transformationTime = 0f;
        }
    }

    void initAoE(){
        if(unchanged) return;
        data1 = 0f;
        data2 = 0f;
        vec.set(0f, 0f);
    }
    void updateAoE(){
        Vision t = getVisionAngle();

        if(nearest != null){
            float dst = unit.dst(nearest);
            if(dst < 250f){
                //
                Vec2 vec = Tmp.v1;
                vec.set(nearest).sub(unit);

                float length = Mathf.clamp((dst - 250f) / 100f, -1f, 0f);

                vec.setLength(unit.speed() * (1.25f + stressScaled / 4f));
                vec.scl(length);

                if(!(vec.isNaN() || vec.isInfinite() || vec.isZero())){
                    unit.movePref(vec);
                }
            }
        }

        if((t.score > 10f || data2 <= 0f) && t.idx != -1){
            float angle = t.angle;
            if(scanTimer.get(1, 5f)){
                vec.set(0f, 0f);
                Vec2 v = Utils.v.trns(angle, scanRange).add(unit);

                found = false;
                for(TeamData data : Vars.state.teams.present){
                    if(data.team != unit.team){
                        //found = false;
                        if(data.unitTree != null){
                            Utils.intersectLine(data.unitTree, 250f, unit.x, unit.y, v.x, v.y, (u, x, y) -> {
                                if(u.isGrounded()){
                                    vec.x += (u.x - unit.x);
                                    vec.y += (u.y - unit.y);
                                    found = true;
                                }
                            });
                        }
                        if(data.turretTree != null){
                            Utils.intersectLine(data.turretTree, 250f, unit.x, unit.y, v.x, v.y, (tr, x, y) -> {
                                vec.x += (tr.x - unit.x);
                                vec.y += (tr.y - unit.y);
                                found = true;
                            });
                        }
                        if(!found && data.buildingTree != null){
                            Utils.intersectLine(data.buildingTree, 250f, unit.x, unit.y, v.x, v.y, (b, x, y) -> {
                                vec.x += (b.x - unit.x) / 25f;
                                vec.y += (b.y - unit.y) / 25f;
                            });
                        }
                    }
                }
            }
            if(!vec.isZero()){
                unit.rotation = Angles.moveToward(unit.rotation, vec.angle(), 15f * Time.delta * (1 + stressScaled));
                //
                if(Angles.within(unit.rotation, vec.angle(), 0.25f) && data1 <= 0f){
                    //
                    Vec2 v = Utils.v.trns(vec.angle(), 50f).add(unit);
                    FlameBullets.aoe.create(unit, v.x, v.y, vec.angle());
                    FlameSounds.aoeShoot.play(1f, Mathf.random(0.9f, 1.1f), FlameSounds.apathyDeath.calcPan(v.x, v.y));
                    data1 = 90f / (1f + stressScaled / 1.5f);
                    data2++;

                    vision[t.idx] = 0f;
                    
                    transformationTime = Math.max(transformationTime, 2.666f * 60f);
                }
            }
            if(data1 > 0) data1 -= Time.delta;
        }else{
            transformationTime = 0f;
        }
    }

    void updateSwitchForm(){
        int maxIdx = -1;
        float maxScore = -1f;
        laser = null;

        float bias = 0f;

        for(float v : shiftScore){
            bias = Math.max(v, bias);
        }
        for(int i = 0; i < shiftScore.length; i++){
            if((shiftScore[i] > maxScore) && shiftScore[i] > bias / 10f && i != currentTransformation && (shiftUses[i] < 2)){
                maxIdx = i;
                maxScore = shiftScore[i];
            }
        }
        int lc = currentTransformation;
        if(maxIdx != -1){
            transformationTime = maxIdx != 0 ? 5f * 60f : 2f * 50f;
            currentTransformation = maxIdx;

            if(maxIdx != 0 && maxIdx != 4){
                shiftUses[maxIdx]++;
                shiftUseTimes[maxIdx] = shiftUseTime;
            }
            if(lc != 0 && maxIdx == 0){
                //Arrays.fill(shiftUses, 0);
                //Arrays.fill(shiftUseTimes, 0);
                for(int i = 0; i < shiftUses.length; i++){
                    //shiftUses[i] /= 2;
                    shiftUseTimes[i] /= 2;
                }
            }

            unit.switchShift(((ApathyUnitType)unit.type).handlers.get(currentTransformation));
            //init = true;
            //data1 = Mathf.random(360f);
        }else{
            transformationTime = 2f * 60f;
        }
        unchanged = lc == currentTransformation;
        init = true;
    }

    void updateAI(){
        bulletCount = flyingCount = groundCount = 0f;
        Arrays.fill(directionalBias, 0);
        Arrays.fill(vision, 0);

        float srad = unit.getShieldRadius();
        int dbl = directionalBias.length;
        int vl = vision.length;
        Rect r = tr.setCentered(unit.x, unit.y, scanRange * 2f);

        Groups.bullet.intersect(r.x, r.y, r.width, r.height, b -> {
            float dst = unit.dst(b);
            if(dst < scanRange && b.team != unit.team){
                float dps = b.type.estimateDPS();

                int angIdx = (int)Mathf.mod(unit.angleTo(b) / (360f / dbl), dbl);
                directionalBias[angIdx] += dps;

                bulletCount += dps;

                if(srad > unit.hitSize / 1.9f && dst < srad && !b.vel.isZero()){
                    //
                    if(b.vel.len() < 8f){
                        b.hit = false;
                        b.remove();
                    }else{
                        float angC = (((unit.angleTo(b) + 90f) * 2f) - b.rotation()) + Mathf.range(5f);
                        b.rotation(angC);
                        b.vel.scl(0.75f);
                        b.team = unit.team;

                        if(b.owner instanceof Sized s && b.owner instanceof Posc p){
                            b.aimX = p.x() + Mathf.range(s.hitSize() / 4f);
                            b.aimY = p.y() + Mathf.range(s.hitSize() / 4f);
                        }
                    }
                    FlameFX.shield.at(b.x, b.y, b.angleTo(unit));

                    unit.shieldHealth -= Math.min(dps / 2f, 500f);
                    unit.stress += dps / 750f;
                }else{
                    unit.stress += dps * b.vel.len() / 1000f;
                }
            }
        });
        st = null;
        sts = 0f;

        nt = null;
        nts = 0f;

        for(TeamData td : Vars.state.teams.present){
            if(td.team != unit.team){
                if(td.unitTree != null){
                    td.unitTree.intersect(r.x, r.y, r.width, r.height, e -> {
                        float dst = unit.dst(e);
                        if(dst - e.hitSize / 2 < scanRange){
                            float dps = e.type.estimateDps();
                            float angleTo = unit.angleTo(e);

                            int angIdx = (int)Mathf.mod(angleTo / (360f / dbl), dbl);
                            directionalBias[angIdx] += dps;

                            if(!(e instanceof TimedKillUnit || (dst < srad && ((e.type.weapons.size > 0 && (e.type.weapons.get(0).shootOnDeath && (e.type.speed <= 0.001f || e.type.speed > 5f))) || e.controller() instanceof MissileAI)))){
                                dps += e.health / 500f;
                                noEnemyTime = 0f;
                                
                                float sscr = dps - dst / 1500f;

                                if(st == null || sscr > sts){
                                    st = e;
                                    sts = sscr;
                                }
                                if(nt == null || dst < nts){
                                    nt = e;
                                    nts = dst;
                                }

                                if(e.isGrounded()){
                                    groundCount += dps;
                                    //handle groups
                                    int vidx = (int)Mathf.mod(angleTo / (360f / vl), vl);
                                    vision[vidx] += dps - dst / 2000f;
                                }else{
                                    flyingCount += dps;
                                }
                                unit.stress += dps / 1100f;
                            }else{
                                bulletCount += dps;
                                if(srad > unit.hitSize / 1.9f && dst < srad){
                                    //
                                    if(e.vel.len() < 6f){
                                        //e.hit = false;
                                        e.type.deathExplosionEffect.at(e.x, e.y);
                                        e.type.deathSound.at(e.x, e.y);
                                        e.remove();
                                    }else{
                                        float angC = (((unit.angleTo(e) + 90f) * 2f) - e.rotation()) + Mathf.range(5f);
                                        e.rotation(angC);
                                        e.vel.scl(0.75f);
                                        e.team = unit.team;
                                    }
                                    FlameFX.shield.at(e.x, e.y, e.angleTo(unit));

                                    unit.shieldHealth -= Math.min(dps / 2f, 500f);
                                    unit.stress += dps / 750f;
                                }
                            }
                        }
                    });
                }
                if(td.turretTree != null){
                    td.turretTree.intersect(r.x, r.y, r.width, r.height, e -> {
                        if(!(e instanceof TurretBuild t)) return;
                        float dst = unit.dst(e);
                        if(dst - e.hitSize() / 2 < scanRange){
                            noEnemyTime = 0f;
                            float dps = 0;
                            for(AmmoEntry ammo : t.ammo){
                                //
                                dps = Math.max(ammo.type().estimateDPS(), dps);
                            }
                            if(e.block instanceof PowerTurret pt){
                                dps = Math.max(pt.shootType.estimateDPS(), dps);
                            }

                            dps += e.health / 500f;
                            groundCount += dps;
                            
                            float angleTo = unit.angleTo(e);

                            int angIdx = (int)Mathf.mod(angleTo / (360f / dbl), dbl);
                            directionalBias[angIdx] += dps;

                            int vidx = (int)Mathf.mod(angleTo / (360f / vl), vl);
                            vision[vidx] += dps - dst / 2000f;
                            
                            float sscr = dps - dst / 1500f;

                            if(st == null || sscr > sts){
                                st = e;
                                sts = sscr;
                            }
                            if(nt == null || dst < nts){
                                nt = e;
                                nts = dst;
                            }
                        }
                    });
                }
            }

                /*
                nextTransformation = 0;
                if(isSurrounded()){
                    if(Math.max(bulletCount, flyingCount) > groundCount){
                        nextTransformation = 3;
                    }else{
                        nextTransformation = 2;
                    }
                }else{
                    if(unit.health > unit.maxHealth / 3f){
                        nextTransformation = 1;
                    }else{
                        nextTransformation = 4;
                    }
                }
                */

        }
        noEnemyTime += Time.delta;
        if(noEnemyTime >= 2.75f * 60f && strongest == null){
            //st = null;
            //sts = 0f;

            for(TeamData td : Vars.state.teams.active){
                if(td.team != unit.team){
                    for(Unit u : td.units){
                        if(u.dead) continue;
                        float dst = unit.dst(u);
                        float dps = u.type.estimateDps() + u.health / 500f;
                        float sscr = dps - dst / 1500f;

                        if(st == null || sscr > sts){
                            st = u;
                            sts = sscr;
                        }
                    }
                }
                if(td.buildings.size > 30){
                    int size = Math.max(20, td.buildings.size / 8);
                    int count = 0;
                    for(int i = 0; i < size; i++){
                        int idx = Mathf.random(td.buildings.size - 1);
                        Building b = td.buildings.get(idx);

                        if(b instanceof TurretBuild){
                            float scr = (b.health / 500f) - (unit.dst(b) / 1500f);

                            if(st == null || scr > sts){
                                st = b;
                                sts = scr;
                            }
                            count++;
                            if(count > 200){
                                break;
                            }
                        }
                    }
                }else{
                    for(Building b : td.buildings){
                        if(b instanceof TurretBuild){
                            float scr = (b.health / 500f) - (unit.dst(b) / 1500f);

                            if(st == null || scr > sts){
                                st = b;
                                sts = scr;
                            }
                        }
                    }
                }
            }
        }
        if(bulletCount > 0 || flyingCount > 0 || groundCount > 0){
            unit.conflict = 60f * 5f;
        }

        if(strongest != null && (!strongest.isAdded() || (strongest instanceof Healthc hh && hh.dead()))){
            strongestValue = 0f;
            strongest = null;
        }
        if(st != null){
            strongestValue = sts;
            strongest = st;
        }
        if(nearest != null && (!nearest.isAdded() || (nearest instanceof Healthc hh && hh.dead()))){
            nearest = null;
        }
        if(nt != null){
            nearest = nt;
        }

        if(updateScore){
            Arrays.fill(shiftScore, 0f);
            //shiftScore[0] = Math.max(Math.max(100f, flyingCount), critDamage);
            shiftScore[0] = Math.max(100f, critDamage);

            /*
            if(isSurrounded()){
                shiftScore[2] = groundCount + bulletCount / 100f;
                shiftScore[3] = Math.max(bulletCount, flyingCount + bulletCount / 100f);

                shiftScore[1] = Math.max(groundCount, flyingCount) / 10f;
            }else{
                float v = Math.max(200f, Math.max(groundCount, flyingCount));
                if(unit.health > unit.maxHealth / 3f && (Math.max(groundCount, flyingCount)) < 1000000f){
                    shiftScore[1] = v;
                    shiftScore[4] = v * 0.333f + strongLaserScore;
                }else{
                    shiftScore[4] = v;
                    shiftScore[1] = v * 0.333f;
                }
            }
            */
            if(critStun > 0) return;
            
            Vec2 surSrc = surroundScore();
            float conc = surSrc.x;
            float surr = surSrc.y;

            shiftScore[2] = (groundCount + bulletCount / 100f) * surr;
            shiftScore[3] = (Math.max(bulletCount, flyingCount + bulletCount / 100f)) * surr;

            float v = Math.max(groundCount, flyingCount) * conc;

            if(unit.health > unit.maxHealth / 3f && (Math.max(groundCount, flyingCount)) < 1000000f){
                shiftScore[1] = v;
                shiftScore[4] = v * 0.1f + strongLaserScore;
            }else{
                shiftScore[4] = v;
                shiftScore[1] = v * 0.25f;
            }
        }

        //transformationTime += Time.delta;
    }

    protected void criticalHit(float damage){
        if(damage > 500000f && (currentTransformation == 1 || currentTransformation == 4)){
            if(currentTransformation == 4 && laser != null && laser.type != null) laser.time = laser.type.lifetime - 80;
            transformationTime = 0f;
            unit.extraShiftSpeed = 2f;
            critStun = 5f * 60f;
        }
        critDamage += damage / 2f;
        maxCritDamage = Math.max(critDamage, maxCritDamage);

        int count = Math.min((int)(damage / 100000f) + 1, 25);
        float range = Mathf.sqrt(damage / 600f) + 12f;
        BloodSplatter.explosion(count, unit.x, unit.y, 5f, range, (32f - 5f) * Mathf.clamp(damage / 1000f) + 5f);
    }

    Vision getVisionAngle(){
        float max = 0f;
        float angle = 0f;
        int idx = -1;
        for(int i = 0; i < vision.length; i++){
            //
            float a = (i / (float)vision.length) * 360f;
            if(vision[i] > max){
                max = vision[i];
                angle = a;
                idx = i;
            }
        }
        //
        vis.angle = angle;
        vis.score = max;
        vis.idx = idx;
        return vis;
    }

    Vec2 surroundScore(){
        Vec2 vec = Tmp.v1;

        float max = 0f;
        int maxIdx = -1;
        int dbl = directionalBias.length;
        float total = 0f;
        float total2 = 0f;

        for(int i = 0; i < dbl; i++){
            float v = directionalBias[i];

            if(v > max) maxIdx = i;
            max = Math.max(max, v);
            total += v;
        }
        if(maxIdx < 0) return vec.set(1f, 1f);

        int hvl = dbl / 2 + 1;
        //let fadeSize = Math.max(2, Math.floor(vl * (20 / 180)));
        int fadeSize = Math.max(2, (int)(dbl * (17f / 180f)));
        for(int i = 0; i < hvl; i++){
            int lastIdx = -1;
            float fade = Mathf.clamp(i / (float)fadeSize);
            for(int s = 0; s < 2; s++){
                int si = s == 0 ? -1 : 1;
                int mi = Mathf.mod(i * si + maxIdx, dbl);
                if(mi != lastIdx){
                    total2 += directionalBias[mi] * fade;
                    lastIdx = mi;
                }
            }
        }

        //vec.x = max / l;
        //vec.y = total - max;
        vec.x = max - total2 / dbl;
        vec.y = Math.max(0f, (total * 5f + total2) / 6f - max);
        float n = vec.x + vec.y;
        vec.x /= n;
        vec.y /= n;

        return vec;
    }

    void moveTo(){
        if(strongest != null && strongestValue > 1000f){
            float dst = unit.dst(strongest);
            if(dst > scanRange - 100f){
                Vec2 vec = Tmp.v1;

                vec.set(strongest).sub(unit);
                vec.setLength(unit.speed());
                
                unit.movePref(vec);
                unit.lookAt(unit.angleTo(strongest));
            }
            return;
        }

        if(nearestCore == null || !nearestCore.isAdded()){
            float dst = 0;
            Teamc core = null;

            for(TeamData td : Vars.state.teams.active){
                if(td.team != unit.team){
                    for(CoreBuild cores : td.cores){
                        float cd = unit.dst2(cores);
                        if(core == null || cd < dst){
                            core = cores;
                            dst = cd;
                        }
                    }
                }
            }
            nearestCore = core;
        }
        if(nearestCore != null){
            Vec2 vec = Tmp.v1;

            vec.set(nearestCore).sub(unit);

            float length = Mathf.clamp((unit.dst(nearestCore) - 220f) / 100f, -1f, 1f);

            vec.setLength(unit.speed());
            vec.scl(length);

            if(vec.isNaN() || vec.isInfinite() || vec.isZero()) return;

            unit.movePref(vec);
            unit.lookAt(unit.angleTo(nearestCore));
        }
    }

    @Override
    public void unit(Unit unit){
        this.unit = (ApathyIUnit)unit;
    }

    @Override
    public Unit unit(){
        return unit;
    }

    static class Vision{
        float angle;
        float score;
        int idx;
    }
}
