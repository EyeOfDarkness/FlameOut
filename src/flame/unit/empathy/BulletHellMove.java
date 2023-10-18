package flame.unit.empathy;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.*;
import flame.Utils.*;
import mindustry.ai.types.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.entities.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.ContinuousTurret.*;
import mindustry.world.blocks.defense.turrets.LaserTurret.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

import java.util.*;

import static mindustry.Vars.*;

public class BulletHellMove extends FollowStrongest{
    static float range = 150f;

    static Seq<Bullet> bullets = new Seq<>();
    static Seq<Unit> unitBullets = new Seq<>();
    static Seq<Velc> hbullets = new Seq<>();
    static Rect trect = new Rect();
    
    Vec2 randomMovement = new Vec2();
    float randomMovementTime = 0f;

    float borderTimer = 0f;
    float harassmentTime = 0f;
    boolean wasHarassed = false;

    static int visionResolution = 24;
    static IntSeq validVision = new IntSeq();
    float[] left = new float[visionResolution], right = new float[visionResolution];
    int visionIdx = 0;
    float visionAngle = -400f;
    float visionForget = 0f;
    float visionLength = -1f;

    float switchTime = 0f;

    static Pool<LaserAvoider> laserPool = new BasicPool<>(LaserAvoider::new);
    IntSet laserSet = new IntSet();
    Seq<LaserAvoider> laserSeq = new Seq<>();
    float laserScanTimer = 0f;

    @Override
    float weight(){
        return 3f + (unit.nearbyBullets > 20 ? 500 + unit.nearbyBullets * 5f : 0);
    }

    @Override
    void updatePassive(){
        if(unit.nearbyBullets > 20 && unit.activeMovement != this && unit.activeMovement.bulletHellOverride()){
            unit.switchAI(this);
        }

        if(harassmentTime < 16 && unit.activeMovement != this) updateAutoParry();
    }

    @Override
    void update(){
        updateTargeting();
        updateLasers();

        float moveX = 0f, moveY = 0f;
        float tdel = FlameOutSFX.realDelta;
        int iter = 4;
        boolean borderOverride = false;
        boolean casualMovement = true;
        borderTimer -= Time.delta;
        randomMovementTime -= Time.delta;
        
        if(strongest != null){
            unit.rotate(1f, unit.angleTo(strongest), 10f);

            float fbounds = finalWorldBounds / 2f;

            Rect r2 = Tmp.r2.set(-fbounds, -fbounds, world.width() * tilesize + fbounds * 2, world.height() * tilesize + fbounds * 2);
            if(!r2.contains(unit.x, unit.y) && borderTimer <= 0f && unit.within(strongest, 1000)){
                float dx = strongest.getX() - unit.x;
                float dy = strongest.getY() - unit.y;
                moveX += dx * 1.75f;
                moveY += dy * 1.75f;

                borderOverride = true;
                casualMovement = false;
                borderTimer = 120f;
                
                float cmx = Mathf.clamp(moveX, r2.x, r2.x + r2.width);
                float cmy = Mathf.clamp(moveY, r2.y, r2.y + r2.height);
                
                moveX += cmx - moveX;
                moveY += cmy - moveY;
            }
        }
        if(!wasHarassed){
            harassmentTime = Math.max(0, harassmentTime - Time.delta * 0.75f);
        }
        wasHarassed = false;

        if(harassmentTime >= 17f){
            unit.queueParry();
            harassmentTime = 0f;
            visionIdx = 0;
        }

        float lax = 0f, lay = 0f;
        for(LaserAvoider lasers : laserSeq){
            FloatSeq l = lasers.getLasers();
            float[] it = l.items;
            int its = l.size;
            for(int i = 0; i < its; i += 4){
                float x = it[i];
                float y = it[i + 1];
                float rot = it[i + 2];
                float len = it[i + 3] + 20f;

                float ux = unit.x + moveX + lax;
                float uy = unit.y + moveY + lay;

                Vec2 v = Tmp.v1.trns(rot, len).add(x, y);
                Vec2 vv = Intersector.nearestSegmentPoint(x, y, v.x, v.y, ux, uy, Tmp.v3);
                float dst = vv.dst(ux, uy);
                if(dst < 45f){
                    int side = Intersector.pointLineSide(x, y, v.x, v.y, ux, uy);
                    if(side == 0) side = Mathf.chance(0.5f) ? -1 : 1;
                    v.trns(rot, 0f, (45f - dst) * side);

                    lax += v.x;
                    lay += v.y;
                    casualMovement = false;
                }
            }
        }
        moveX += lax * Time.delta;
        moveY += lay * Time.delta;

        bullets.clear();
        unitBullets.clear();
        hbullets.clear();
        Rect r = trect.setCentered(unit.x + moveX, unit.y + moveY, range * 2);

        Arrays.fill(left, range);
        Arrays.fill(right, range);

        float ux = unit.x, uy = unit.y;
        unit.x += moveX;
        unit.y += moveY;
        Groups.bullet.intersect(r.x, r.y, r.width, r.height, b -> {
            if(unit.within(b, range + b.hitSize / 2f) && b.team != unit.team){
                bullets.add(b);
                hbullets.add(b);
                
                float dst = unit.dst(b);
                float speed = dst / 14f;

                float bx = b.x + b.vel.x * speed;
                float by = b.y + b.vel.y * speed;
                float ang = Utils.angleDistSigned(unit.angleTo(bx, by), unit.rotation);
                if(Float.isNaN(ang)) ang = 0;
                //float ang = Utils.angleDistSigned(unit.rotation, unit.angleTo(b));
                float at = Mathf.angle(dst, b.hitSize / 2f + 10f);

                /*
                int aidx = Mathf.clamp((int)(Math.abs(ang / 180f) * visionResolution), 0, visionResolution - 1);
                if(ang > 0){
                    right[aidx] = Math.min(right[aidx], dst - b.hitSize / 2f);
                }else{
                    left[aidx] = Math.min(left[aidx], dst - b.hitSize / 2f);
                }
                */
                int isize = Math.max((int)((at * 2) / visionResolution), 1);
                if(isize % 2 == 0) isize++;
                for(int i = 0; i < isize; i++){
                    float offset = isize > 1 ? -at + (at * 2 * (i / (isize - 1f))) : 0f;
                    float ang2 = ang + offset;
                    if(ang2 < -180){
                        ang2 += 360f;
                    }
                    if(ang2 > 180){
                        ang2 -= 360f;
                    }
                    
                    int aidx = Mathf.clamp((int)(Math.abs(ang2 / 180f) * visionResolution), 0, visionResolution - 1);
                    if(ang2 > 0){
                        right[aidx] = Math.min(right[aidx], dst - b.hitSize / 2f);
                    }else{
                        left[aidx] = Math.min(left[aidx], dst - b.hitSize / 2f);
                    }
                }

                //int aidx = (int)((ang / 360f) * visionResolution) % visionResolution;
                //visNearest[aidx] = Math.min(visNearest[aidx], unit.dst(b) - b.hitSize / 2f);
            }
        });
        Groups.unit.intersect(r.x, r.y, r.width, r.height, u -> {
            if(unit.within(u, range + u.hitSize / 2f) && u.team != unit.team && (u.controller() instanceof MissileAI || u instanceof TimedKillUnit)){
                unitBullets.add(u);
                hbullets.add(u);

                float dst = unit.dst(u);
                float speed = dst / 14f;

                float bx = u.x + u.vel.x * speed;
                float by = u.y + u.vel.y * speed;
                float ang = Utils.angleDistSigned(unit.angleTo(bx, by), unit.rotation);
                if(Float.isNaN(ang)) ang = 0;
                //float ang = Utils.angleDistSigned(unit.rotation, unit.angleTo(b));
                float at = Mathf.angle(dst, u.hitSize / 2f + 10f);

                int isize = Math.max((int)((at * 2) / visionResolution), 1);
                if(isize % 2 == 0) isize++;
                for(int i = 0; i < isize; i++){
                    float offset = isize > 1 ? -at + (at * 2 * (i / (isize - 1f))) : 0f;
                    float ang2 = ang + offset;
                    if(ang2 < -180){
                        ang2 += 360f;
                    }
                    if(ang2 > 180){
                        ang2 -= 360f;
                    }

                    int aidx = Mathf.clamp((int)(Math.abs(ang2 / 180f) * visionResolution), 0, visionResolution - 1);
                    if(ang2 > 0){
                        right[aidx] = Math.min(right[aidx], dst - u.hitSize / 2f);
                    }else{
                        left[aidx] = Math.min(left[aidx], dst - u.hitSize / 2f);
                    }
                }
            }
        });

        updateVision();
        if(visionIdx != 0 && visionAngle != -400){
            float angle = unit.rotation + visionAngle;
            Vec2 v = Tmp.v1.trns(angle, 3f);
            //float sx = Mathf.sinDeg(angle) * 5f, sy = Mathf.cosDeg(angle) * 5f;
            moveX += v.x;
            moveY += v.y;
            casualMovement = false;
        }

        float nearest = 100f;
        if(bullets.size > 0 || unitBullets.size > 0){
            float mx2 = 0, my2 = 0;
            int total = bullets.size + unitBullets.size;
            if(bullets.size > 0){
                for(Bullet b : bullets){
                    Vec2 v = Tmp.v1.set(b.x, b.y).sub(unit.x, unit.y).nor();
                    if(!v.isNaN()){
                        mx2 -= v.x;
                        my2 -= v.y;
                    }
                    nearest = Math.min(nearest, unit.dst(b) - b.hitSize / 2);
                }
                for(Unit u : unitBullets){
                    Vec2 v = Tmp.v1.set(u.x, u.y).sub(unit.x, unit.y).nor();
                    if(!v.isNaN()){
                        mx2 -= v.x;
                        my2 -= v.y;
                    }
                    nearest = Math.min(nearest, unit.dst(u) - u.hitSize / 2);
                }
            }
            moveX += (mx2 / total) * 3 * Time.delta;
            moveY += (my2 / total) * 3 * Time.delta;
            casualMovement = false;
        }
        float limit = 10;
        float rangew = unit.activeAttack.effectiveDistance() + (strongest instanceof Sized s ? s.hitSize() / 2 : 0f);
        if(strongest != null){
            float fout = Mathf.clamp((nearest - 25f) / (100f - 25f));
            Vec2 v = Tmp.v1.set(strongest).sub(unit.x, unit.y).nor();
            float ddst = unit.dst(strongest);
            float mscl = !unit.within(strongest, rangew) ? 1f : 0.25f;
            float ddd = Mathf.clamp((ddst - rangew) / 12f, -1, 1);
            //!unit.within(strongest, rangew)
            moveX += (v.x * 12f * fout * ddd * mscl) * Time.delta;
            moveY += (v.y * 12f * fout * ddd * mscl) * Time.delta;
            limit = Math.max(limit, 14f * fout * ddd);
            
            if(ddst <= rangew - 30f || ddst >= rangew + 30f){
                casualMovement = false;
                randomMovement.setZero();
            }
        }
        unit.x = ux;
        unit.y = uy;
        
        float size = 20f;
        boolean hit = false;
        float amoveX = 0f;
        float amoveY = 0f;
        for(int i = 0; i < iter; i++){
            float mx = 0, my = 0;
            float unitx = unit.x + moveX + amoveX;
            float unity = unit.y + moveY + amoveY;

            for(Bullet b : bullets){
                //
                /*
                let mass = ent.mass() + e.mass();
                let dst = Mathf.dst(ent.x, ent.y, e.x, e.y);
                if(dst <= 0) return;
                let delta = ((ent.hitsize() + e.hitsize()) - dst) * (e.mass() / mass);
                let dx = ((ent.x - e.x) / dst) * delta;
                let dy = ((ent.y - e.y) / dst) * delta;
                ent.nx += dx * 2;
                ent.ny += dy * 2;
                ent.nmc++;
                */
                float bx = b.x + b.vel.x * tdel;
                float by = b.y + b.vel.y * tdel;

                float adst = Angles.angleDist(unit.rotation - 90, b.rotation());
                float cos = Math.abs(Mathf.cosDeg(adst));
                float sin = Math.abs(Mathf.sinDeg(adst));
                Tmp.v2.trns(-unit.rotation + 90, bx - unitx, by - unity);
                
                float s = b.hitSize / 2;
                float dst = Mathf.dst(unitx, unity, bx, by);
                if(dst <= size + s && dst > 0){
                    Tmp.v1.trns(unit.angleTo(b), 0f, (Tmp.v2.x > 0 ? 1 : -1) * (size + s));
                    boolean valid = true;
                    for(Velc b2 : hbullets){
                        if(b2 != b){
                            float bmx = b2.x() + b2.vel().x * tdel;
                            float bmy = b2.y() + b2.vel().y * tdel;
                            if(Mathf.within(bmx, bmy, b.x + Tmp.v1.x, b.y + Tmp.v1.y, (size + s) - 0.1f)){
                                valid = false;
                                break;
                            }
                        }
                    }

                    float delta = (size + s) - dst;
                    if(valid){
                        mx += ((unitx - bx) / dst) * delta * cos;
                        my += ((unity - by) / dst) * delta * cos;

                        Tmp.v1.trns(unit.rotation, 0f, (Tmp.v2.x > 0 ? 1 : -1) * (size + s));
                        mx += Tmp.v1.x * sin;
                        my += Tmp.v1.y * sin;
                    }else{
                        mx += ((unitx - bx) / dst) * delta;
                        my += ((unity - by) / dst) * delta;
                    }
                    hit = true;
                }
            }
            for(Unit b : unitBullets){
                //
                float bx = b.x + b.vel.x * tdel;
                float by = b.y + b.vel.y * tdel;

                float adst = Angles.angleDist(unit.rotation - 90, b.rotation());
                float cos = Math.abs(Mathf.cosDeg(adst));
                float sin = Math.abs(Mathf.sinDeg(adst));
                Tmp.v2.trns(-unit.rotation + 90, bx - unitx, by - unity);

                float s = b.hitSize / 2;
                float dst = Mathf.dst(unitx, unity, bx, by);
                if(dst <= size + s && dst > 0){
                    Tmp.v1.trns(unit.angleTo(b), 0f, (Tmp.v2.x > 0 ? 1 : -1) * (size + s));
                    boolean valid = true;
                    for(Velc b2 : hbullets){
                        if(b2 != b){
                            float bmx = b2.x() + b2.vel().x * tdel;
                            float bmy = b2.y() + b2.vel().y * tdel;
                            if(Mathf.within(bmx, bmy, b.x + Tmp.v1.x, b.y + Tmp.v1.y, (size + s) - 0.1f)){
                                valid = false;
                                break;
                            }
                        }
                    }

                    float delta = (size + s) - dst;
                    if(valid){
                        mx += ((unitx - bx) / dst) * delta * cos;
                        my += ((unity - by) / dst) * delta * cos;

                        Tmp.v1.trns(unit.rotation, 0f, (Tmp.v2.x > 0 ? 1 : -1) * (size + s));
                        mx += Tmp.v1.x * sin;
                        my += Tmp.v1.y * sin;
                    }else{
                        mx += ((unitx - bx) / dst) * delta;
                        my += ((unity - by) / dst) * delta;
                    }
                    hit = true;
                }
            }
            //moveX += mx / iter;
            //moveY += my / iter;
            amoveX += mx / iter;
            amoveY += my / iter;
        }
        if(hit){
            harassmentTime += Time.delta;
            visionForget -= Time.delta;
            wasHarassed = true;
            casualMovement = false;
        }
        Tmp.v1.set(amoveX, amoveY).limit(limit);
        moveX += Tmp.v1.x;
        moveY += Tmp.v1.y;
        
        if(!borderOverride){
            Tmp.v1.set(moveX, moveY).limit(limit);
            moveX = Tmp.v1.x;
            moveY = Tmp.v1.y;
        }
        unit.move(2f, 2, moveX, moveY);
        //if(casualMovement) updateBasicMovements();
        if(casualMovement) switchTime = 0f;

        switchTime += Time.delta;
        if(switchTime >= 35f || (switchTime >= 15f && unit.nearbyBullets < 20)){
            switchTime = 0f;
            unit.randAI(false, Mathf.chance(0.3f));
        }
    }
    
    void updateBasicMovements(){
        if(randomMovementTime <= 0f){
            float fbounds = finalWorldBounds / 2f;
            Rect r2 = Tmp.r2.set(-fbounds, -fbounds, world.width() * tilesize + fbounds * 2, world.height() * tilesize + fbounds * 2);
            
            if(r2.contains(unit.x, unit.y)){
                if(Mathf.chance(0.75f)){
                    randomMovement.rnd(Mathf.random(0.25f, 0.75f));
                }else{
                    randomMovement.setZero();
                }
            }else{
                float mx = Mathf.clamp(unit.x, -fbounds, world.width() * tilesize + fbounds * 2) - unit.x;
                float my = Mathf.clamp(unit.y, -fbounds, world.height() * tilesize + fbounds * 2) - unit.y;
                randomMovement.set(mx, my).nor().scl(0.75f);
            }
            randomMovementTime = Mathf.random(60f, 150f);
        }
        if(randomMovement.len() > 0.001f && strongest != null){
            //
            unit.rotate(0.5f, randomMovement.angle(), 5f);
            unit.move(2.1f, 0, randomMovement.x, randomMovement.y);
        }
    }

    void updateLasers(){
        float srange = 800f;
        if(laserScanTimer <= 0f){
            Rect r = trect.setCentered(unit.x, unit.y, srange * 2);
            for(TeamData data : state.teams.present){
                if(data.team != unit.team){
                    if(data.unitTree != null){
                        data.unitTree.intersect(r, u -> {
                            if(unit.within(u, srange + u.hitSize / 2f) && LaserAvoider.hasLasersUnit(u) && !laserSet.contains(u.id)){
                                laserSeq.add(laserPool.obtain().setUnit(u));
                                laserSet.add(u.id);
                            }
                        });
                    }
                    if(data.turretTree != null){
                        data.turretTree.intersect(r, tr -> {
                            if(unit.within(tr, srange + tr.hitSize() / 2f) && !laserSet.contains(tr.id) && tr instanceof TurretBuild tb && LaserAvoider.hasLasersTurret(tb)){
                                laserSeq.add(laserPool.obtain().setTurret(tb));
                                laserSet.add(tr.id);
                            }
                        });
                    }
                }
            }
            if(strongest != null && !laserSet.contains(strongest.id())){
                if(strongest instanceof Unit u && LaserAvoider.hasLasersUnit(u)){
                    laserSeq.add(laserPool.obtain().setUnit(u));
                    laserSet.add(strongest.id());
                }else if(strongest instanceof TurretBuild tr && LaserAvoider.hasLasersTurret(tr)){
                    laserSeq.add(laserPool.obtain().setTurret(tr));
                    laserSet.add(strongest.id());
                }
            }
            laserScanTimer = 10f;
        }
        laserScanTimer -= Time.delta;
        laserSeq.removeAll(la -> {
            Posc pos = la.getPos();
            boolean within = unit.within(la.getPos(), srange) || pos == strongest;
            if(within){
                la.forgetTime = 0f;
            }else{
                la.forgetTime += Time.delta;
            }
            la.update();

            boolean removed = la.forgetTime > 30f || !pos.isAdded();
            if(removed){
                laserSet.remove(pos.id());
                laserPool.free(la);
            }
            return removed;
        });
    }

    float getVisionValue(int i){
        return i == 0 ? 0f : (i > 0 ? right[i - 1] : left[-i - 1]);
    }

    float getVisionAngle(){
        int i = visionIdx > 0 ? (visionIdx - 1) : (visionIdx + 1);
        return (i / (float)visionResolution) * 180f;
    }

    void updateVision(){
        float rangew = 400 + (strongest instanceof Sized s ? s.hitSize() / 2 : 0f);
        if(visionIdx != 0 && (getVisionValue(visionIdx) < visionLength * 0.6f || visionForget <= 0f || (strongest != null && unit.within(strongest, rangew - 25f)))){
            visionIdx = 0;
            visionForget = 40f;
            visionLength = range;
        }
        boolean within = strongest == null || unit.within(strongest, rangew);
        
        if(visionIdx == 0){
            float leftV = 0f;
            float rightV = 0f;
            float min = range;

            for(float v : left){
                leftV += v;
                min = Math.min(min, v);
            }
            for(float v : right){
                rightV += v;
                min = Math.min(min, v);
            }
            if(leftV != rightV){
                validVision.clear();
                if(leftV > rightV){
                    for(int i = 0; i < visionResolution; i++){
                        float v = left[i];
                        // && i < visionResolution / 2
                        if(v >= min * 0.8f && (i < visionResolution / 2 || within)){
                            validVision.add(i);
                        }
                    }
                    //validVision
                    int idx = validVision.random();
                    visionLength = left[idx];
                    visionIdx = -(idx + 1);
                }else{
                    for(int i = 0; i < visionResolution; i++){
                        float v = right[i];
                        if(v >= min * 0.8f && (i < visionResolution / 2 || within)){
                            validVision.add(i);
                        }
                    }
                    //validVision
                    int idx = validVision.random();
                    visionLength = right[idx];
                    visionIdx = (idx + 1);
                }
                visionForget = 40f;
            }
            if(visionIdx == 0){
                visionAngle = -400;
            }
        }
        if(visionIdx != 0){
            if(visionAngle == -400) visionAngle = getVisionAngle();
            visionAngle = Mathf.slerpDelta(visionAngle, getVisionAngle(), 0.3f);
        }

        visionForget -= Time.delta;
    }

    static class LaserAvoider implements Poolable{
        FloatSeq firstShotDelays = new FloatSeq();
        Unit unitShooter;
        TurretBuild turretShooter;
        float forgetTime = 0f;

        //x, y, rotation, length, ...
        final static FloatSeq returnSeq = new FloatSeq();
        //x, y, rotation
        final static FloatSeq shoots = new FloatSeq();
        final static float maxBulletSpeed = 15f;

        Posc getPos(){
            return unitShooter != null ? unitShooter : turretShooter;
        }

        LaserAvoider setUnit(Unit u){
            firstShotDelays.clear();
            for(WeaponMount m : u.mounts){
                firstShotDelays.add(m.weapon.shoot.firstShotDelay > 0.01f ? 0f : -1f);
            }
            unitShooter = u;
            return this;
        }
        LaserAvoider setTurret(TurretBuild tr){
            firstShotDelays.clear();
            Turret t = (Turret)tr.block;
            firstShotDelays.add(t.shoot.firstShotDelay > 0.01f ? 0f : -1f);
            turretShooter = tr;
            return this;
        }

        static boolean hasLasersUnit(Unit u){
            for(WeaponMount m : u.mounts){
                Weapon w = m.weapon;
                if(w.continuous || w.bullet.speed < 0.001f || w.bullet.speed > maxBulletSpeed){
                    return true;
                }
            }
            return false;
        }
        static boolean hasLasersTurret(TurretBuild tr){
            Turret tb = (Turret)tr.block;

            if(tr.hasAmmo()){
                BulletType type = tr.peekAmmo();
                if(type.speed < 0.001f || type.speed > maxBulletSpeed) return true;
            }

            return tb instanceof ContinuousTurret || tb instanceof LaserTurret;
        }

        FloatSeq getLasers(){
            returnSeq.clear();

            if(unitShooter != null){
                Unit u = unitShooter;
                float[] fsd = firstShotDelays.items;
                for(int i = 0; i < unitShooter.mounts.length; i++){
                    WeaponMount m = unitShooter.mounts[i];
                    Weapon w = m.weapon;
                    ShootPattern sp = w.shoot;
                    if(m.charging && sp.firstShotDelay > 0.01f && fsd[i] <= 0f){
                        fsd[i] = sp.firstShotDelay;
                    }

                    boolean valid = (m.reload <= (20f - sp.firstShotDelay) || (fsd[i] <= 20f && fsd[i] > 0) || (m.bullet != null && w.continuous));

                    if(valid && (w.bullet.speed < 0.001f || w.bullet.speed > maxBulletSpeed)){
                        shoots.clear();
                        sp.shoot(m.totalShots, (x, y, rotation, delay, mover) -> shoots.add(x, y, rotation), () -> {});
                        float lx = Float.NaN, ly = Float.NaN, lr = Float.NaN;
                        float[] sht = shoots.items;
                        for(int j = 0; j < shoots.size; j += 3){
                            float sx = sht[j];
                            float sy = sht[j + 1];
                            float sr = sht[j + 2];
                            if(sx != lx || sy != ly || lr != sr){
                                lx = sx;
                                ly = sy;
                                lr = sr;

                                float weaponRotation = u.rotation - 90 + (w.rotate ? m.rotation : w.baseRotation),
                                        mountX = u.x + Angles.trnsx(u.rotation - 90, w.x, w.y),
                                        mountY = u.y + Angles.trnsy(u.rotation - 90, w.x, w.y),
                                        bulletX = mountX + Angles.trnsx(weaponRotation, w.shootX + sx, w.shootY + sy),
                                        bulletY = mountY + Angles.trnsy(weaponRotation, w.shootX + sx, w.shootY + sy),
                                        shootAngle = bulletRotation(u, m, bulletX, bulletY) + sr,
                                        angle = sr + shootAngle;

                                returnSeq.add(bulletX, bulletY, angle, w.range());
                            }
                        }
                    }
                }
            }
            if(turretShooter != null){
                TurretBuild tr = turretShooter;
                Turret tb = (Turret)turretShooter.block;
                ShootPattern sp = tb.shoot;

                float[] fsd = firstShotDelays.items;
                if(tr.charging() && sp.firstShotDelay > 0.01f && fsd[0] <= 0f){
                    fsd[0] = sp.firstShotDelay;
                }

                BulletType bullet = tr.hasAmmo() ? tr.peekAmmo() : null;

                boolean lasers = (tr instanceof ContinuousTurretBuild ct && !ct.bullets.isEmpty()) || (tr instanceof LaserTurretBuild lt && !lt.bullets.isEmpty());
                boolean valid = ((tb.reload - tr.reloadCounter) <= (30f - sp.firstShotDelay) || (fsd[0] <= 30f && fsd[0] > 0) || lasers);
                if(valid && bullet != null && (bullet.speed < 0.001f || bullet.speed > maxBulletSpeed)){
                    shoots.clear();
                    sp.shoot(tr.totalShots, (x, y, rotation, delay, mover) -> shoots.add(x, y, rotation), () -> {});
                    float lx = Float.NaN, ly = Float.NaN, lr = Float.NaN;
                    float[] sht = shoots.items;
                    for(int j = 0; j < shoots.size; j += 3){
                        float sx = sht[j];
                        float sy = sht[j + 1];
                        float sr = sht[j + 2];
                        if(sx != lx || sy != ly || lr != sr){
                            lx = sx;
                            ly = sy;
                            lr = sr;

                            float bulletX = tr.x + Angles.trnsx(tr.rotation - 90, tb.shootX + sx, tb.shootY + sy),
                                    bulletY = tr.y + Angles.trnsy(tr.rotation - 90, tb.shootX + sx, tb.shootY + sy),
                                    shootAngle = tr.rotation + sr;
                            returnSeq.add(bulletX, bulletY, shootAngle, bullet.range);
                        }
                    }
                }
            }
            return returnSeq;
        }
        void update(){
            float[] items = firstShotDelays.items;
            for(int i = 0; i < firstShotDelays.size; i++){
                if(items[i] != -1f && items[i] > 0) items[i] = Math.max(0, items[i] - FlameOutSFX.realDelta);
            }
        }

        float bulletRotation(Unit unit, WeaponMount mount, float bulletX, float bulletY){
            Weapon w = mount.weapon;
            return w.rotate ? unit.rotation + mount.rotation : Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY) + (unit.rotation - unit.angleTo(mount.aimX, mount.aimY)) + w.baseRotation;
        }

        @Override
        public void reset(){
            firstShotDelays.clear();
            unitShooter = null;
            turretShooter = null;
            forgetTime = 0f;
        }
    }
}
