package flame.unit.empathy;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.bullets.*;
import flame.effects.*;
import flame.entities.*;
import flame.graphics.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class MagicAttack extends AttackAI{
    float time;
    float chargeTime = 0f, warmup;
    float rotation;

    float laserInterval;

    float reload1, reload2, reload3;
    int shots1, shots2;

    boolean useLethal = false, impact;

    final static float duration = 10f * 60f;
    final static Mat3D mat = new Mat3D();

    MagicAttack(){
        super();
        addSound(FlameSounds.empathyBigLaser);
    }

    @Override
    float weight(){
        return (unit.useLethal() && !unit.isDecoy()) ? (unit.health > 30 ? 100f : 1000f) + unit.extraLethalScore() : 5f;
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    void reset(){
        time = chargeTime = warmup = 0f;
        laserInterval = 0f;
        reload1 = reload2 = reload3 = 0f;
        shots1 = shots2 = 0;
        useLethal = false;
        impact = false;
    }

    @Override
    void init(){
        reset();
        useLethal = unit.useLethal() && !unit.isDecoy();
        if(unit.getTarget() != null){
            rotation = unit.angleTo(unit.getTarget());
        }
    }

    void updateMovement(){
        Teamc target = unit.getTarget();
        if(target == null) return;
        
        float size = target instanceof Sized s ? s.hitSize() / 2f : 0f;
        float dst = effectiveDistance() + size;
        
        Vec2 vec = Tmp.v1.set(target).sub(unit);
        Vec2 vel = unit.trueVelocity();
        //float dstt = unit.dst(strongest);
        float dstt = Mathf.dst(unit.x + vel.x, unit.y + vel.y, target.x(), target.y());
        float len = (dstt - dst) / 150f;
        vec.setLength(Math.min(Math.abs(len), 1f));
        vec.scl(len > 0 ? 1 : -1);
        unit.move(5f, 0, vec.x, vec.y);
    }

    @Override
    void update(){
        warmup = Mathf.approachDelta(warmup, 1f, 0.01f + warmup * 0.1f);
        chargeTime += warmup * Time.delta;
        time += Time.delta;

        if(unit.getTarget() != null){
            rotation = Angles.moveToward(rotation, unit.angleTo(unit.getTarget()), (useLethal && time >= 240f) ? 0.075f : 1f);
            unit.rotate(5f, unit.angleTo(unit.getTarget()), 5f);
            if(useLethal){
                unit.move(5f, 1, 0f, 0f);
            }else{
                updateMovement();
            }
        }

        if(useLethal){
            float fin = Mathf.clamp((time - 240f) / 5f);
            float fin2 = Interp.pow2.apply(Mathf.clamp((time - 250f) / 180f));
            float fin3 = Interp.pow2Out.apply(Mathf.clamp((time - 250f) / 700f));
            float fout = Mathf.clamp((duration - time) / 180f);
            if((laserInterval += Time.delta) >= 5f){
                if(fin > 0){
                    Tmp.v1.trns(rotation, 40f).add(unit.x, unit.y);
                    float vx = Tmp.v1.x, vy = Tmp.v1.y;

                    float ang = 30f * fin2 + 15f * fin3;
                    CountDownAttack.updateBigLaser(unit.team, vx, vy, rotation, fout);
                    CountDownAttack.updateBigLaser(unit.team, vx, vy, rotation + ang, fin * fout);
                    CountDownAttack.updateBigLaser(unit.team, vx, vy, rotation - ang, fin * fout);

                    //CountDownAttack.updateBigLaser(unit.team, vx, vy, rotation, fout);
                }

                laserInterval = 0f;
            }
            if(fin > 0){
                if(!impact){
                    Tmp.v1.trns(rotation, 40f).add(unit.x, unit.y);
                    CountDownAttack.impactBigLaser(Tmp.v1.x, Tmp.v1.y, rotation);

                    Tmp.v5.trns(rotation, 80f).add(unit.x, unit.y);
                    FlameFX.shootShockWave.at(Tmp.v5.x, Tmp.v5.y, rotation, 200f);
                    Tmp.v5.trns(rotation, 200f).add(unit.x, unit.y);
                    FlameFX.shootShockWave.at(Tmp.v5.x, Tmp.v5.y, rotation, 390f);

                    Sounds.laserblast.play(1f, 0.5f, 0f);

                    impact = true;
                }

                float nx = 0f, ny = 0f;
                float ndst = 9999999f;
                for(int i = 0; i < 3; i++){
                    float f = ((i / 2f) - 0.5f) * 2f;

                    Tmp.v1.trns(rotation, 40f).add(unit.x, unit.y);
                    float vx = Tmp.v1.x, vy = Tmp.v1.y;

                    float ang = (30f * fin2 + 15f * fin3) * f;
                    float len = 2900f;
                    Tmp.v1.trns(rotation + ang, len).add(vx, vy);
                    float ox = Tmp.v1.x, oy = Tmp.v1.y;

                    Vec2 cam = Core.camera.position;
                    Vec2 near = Intersector.nearestSegmentPoint(vx, vy, ox, oy, cam.x, cam.y, Tmp.v2);
                    float dst = near.dst(cam.x, cam.y);
                    if(dst < ndst){
                        ndst = dst;
                        nx = near.x;
                        ny = near.y;
                    }
                }
                sounds.get(0).play(nx, ny, fout, 0f, true);
            }
        }else{
            if(time > 140f && time < (duration - 120f)){
                if(reload1 <= 0f){
                    Vec2 of = Tmp.v1.trns(rotation, 40f).add(unit.x, unit.y);
                    float x = of.x, y = of.y;

                    Vec2 v = Tmp.v1.trns((shots1 / 30f) * 360f, 170f);
                    Vec3 v3 = Tmp.v31.set(v, 0f);
                    mat.setFromEulerAngles(0f, -75f, -rotation - 90f);
                    float[] matv = mat.val;
                    v3.set(v3.x * matv[Mat3D.M00] + v3.y * matv[Mat3D.M10] + v3.z * matv[Mat3D.M20] + matv[Mat3D.M30], v3.x * matv[Mat3D.M01] + v3.y * matv[Mat3D.M11] + v3.z * matv[Mat3D.M21] + matv[Mat3D.M31], v3.x * matv[Mat3D.M02] + v3.y * matv[Mat3D.M12] + v3.z * matv[Mat3D.M22] + matv[Mat3D.M32]);
                    float pz = 700f / (700f - v3.z);

                    float ix = v3.x * pz + x, iy = v3.y * pz + y;
                    FlameBullets.sword.create(unit, unit.team, ix, iy, rotation);
                    FlameSounds.empathySquareShoot.at(ix, iy, Mathf.random(0.9f, 1.1f) * 2.5f);

                    shots1++;
                    reload1 = 3f;
                }
                if(reload2 <= 0f){
                    Tmp.v1.trns(rotation, -120f);
                    float ox = Tmp.v1.x, oy = Tmp.v1.y;

                    int amount = 15 + (shots2 % 2);
                    for(int i = 0; i < amount; i++){
                        float fin = ((i / (amount - 1f)) - 0.5f) * 2f;
                        float ang = fin * 60f + rotation;
                        float ang2 = fin * 30f + rotation;
                        Vec2 v = Tmp.v1.trns(ang2, 120f).add(ox, oy).add(unit.x, unit.y);

                        FlameBullets.pin.create(unit, unit.team, v.x, v.y, ang);
                    }
                    Sounds.missile.at(unit.x, unit.y, 2f);

                    shots2++;
                    reload2 = 15f;
                }
                if(reload3 <= 0f){
                    Vec2 of = Tmp.v1.trns(rotation, 50f, Mathf.range(50f)).add(unit.x, unit.y);

                    Bullet b = FlameBullets.tracker.create(unit, unit.team, of.x, of.y, rotation);
                    b.data = unit.getTarget();
                    Sounds.missile.at(of.x, of.y, 2.5f);

                    reload3 = 3f;
                }
                reload3 -= Time.delta;
                reload2 -= Time.delta;
                reload1 -= Time.delta;
            }
        }
        if(time > 140f && time < (duration - 120f)){
            if(Mathf.chanceDelta(useLethal ? 0.15f : 0.07f)){
                Vec2 of = Tmp.v1.trns(rotation, 50f + Mathf.range(10f), Mathf.range(55f)).add(unit.x, unit.y);
                EmpathyLightning.create(unit.team, of.x, of.y, rotation + Mathf.range(15f), 20 + Mathf.random(7));
            }
        }
        
        if(time >= duration){
            reset();
            unit.randAI(true, unit.health < 50);
        }
    }

    void drawLasers(){
        float fin = Mathf.clamp((time - 240f) / 5f);
        float fin2 = Interp.pow2.apply(Mathf.clamp((time - 250f) / 180f));
        float fin3 = Interp.pow2Out.apply(Mathf.clamp((time - 250f) / 700f));
        float fout = Mathf.clamp((duration - time) / 180f);

        if(fin > 0){
            Tmp.v1.trns(rotation, 40f).add(unit.x, unit.y);
            float vx = Tmp.v1.x, vy = Tmp.v1.y;

            if(fin2 > 0){
                float ang = 30f * fin2 + 15f * fin3;
                CountDownAttack.drawBigLaser(vx, vy, rotation + ang, fin * fout);
                CountDownAttack.drawBigLaser(vx, vy, rotation - ang, fin * fout);
            }

            //CountDownAttack.drawBigLaser(vx, vy, rotation, fin * fout);
            CountDownAttack.drawBigLaser(vx, vy, rotation, fout);
        }
    }

    @Override
    void draw(){
        float fin = Mathf.clamp(time / 240f);
        float fin2 = Mathf.curve(fin, 0f, 0.6f);
        //float fin3 = Mathf.clamp(time / 240f, 0f, 3f);
        float fin4 = Mathf.clamp((time / 240f) / 3f);
        float angle2 = chargeTime;

        float fout = Mathf.clamp((duration - time) / 180f);
        float lethalp = useLethal ? Interp.pow2.apply(Mathf.clamp((time - 240f) / 5f)) : 0f;

        float origZ = Draw.z();
        Draw.z((Layer.bullet + Layer.effect) / 2f);
        GraphicUtils.draw3DBegin();

        Draw.z(-20f - 5f * fin4);
        Draw.color(FlamePal.empathy, fin2 * fout);
        GraphicUtils.circle(EmpathyRegions.magicCircle2, 0f, 0f, -angle2 * 0.75f, 110f + 40f * fin4, 48);

        Draw.z(40f * Interp.pow2.apply(fin4) - 35f);
        Draw.color(FlamePal.empathy, Interp.pow2.apply(fin) * fout);
        GraphicUtils.circle(EmpathyRegions.magicCircle, 0f, 0f, 0f, 140f, 48);

        Lines.stroke(3f * fout);
        Draw.z(0f);
        Draw.color(FlamePal.empathy);
        EmpathySpawner.progressiveCircle(0f, 0f, 90f, -Time.time % 360f, fin);
        EmpathySpawner.progressiveStar(0f, 0f, 90f, Time.time % 360f, 9, 2, fin);
        Lines.stroke(2f * fin * fout);
        Lines.circle(0f, 0f, 70f);

        Draw.z(10f * Interp.pow2.apply(fin) + 5f * fin4);
        Lines.stroke(2f * fin * fout);
        Lines.circle(0f, 0f, 100f);

        Draw.z(35f * Interp.pow2.apply(fin) + 10f * fin4);
        Lines.stroke(3f * fout);
        EmpathySpawner.progressiveCircle(0f, 0f, 180f, Time.time % 360f, fin);
        Lines.stroke(1.5f * fout);
        EmpathySpawner.progressiveCircle(0f, 0f, 170f, (Time.time + 180f) % 360f, fin);

        EmpathySpawner.progressiveStar(0f, 0f, 180f, angle2, 3, 1, fin);

        float dz = Draw.z();
        //Draw.z(dz + fin * 7f);
        for(int i = 0; i < 4; i++){
            float rot = i * 90f + angle2;
            Tmp.v1.trns(rot, 180f);
            Lines.stroke(2.75f * fout);
            Draw.z(dz + fin * 9f);
            EmpathySpawner.progressiveCircle(Tmp.v1.x, Tmp.v1.y, 60f, rot + 90f, fin);
            Lines.stroke(1.5f * fout);
            Draw.z(dz + fin * 7f);
            EmpathySpawner.progressiveCircle(Tmp.v1.x, Tmp.v1.y, 50f, rot + 90f + 180f, fin);
        }

        Draw.z(40f + 25f * fin4 + 25f * lethalp);
        Draw.color(FlamePal.empathy, fin4 * fout);
        GraphicUtils.circle(EmpathyRegions.magicCircle2, 0f, 0f, -angle2 * 0.5f, 220f + 50f * lethalp, 48);
        Draw.z(50f + 50f * fin4 + 50f * lethalp);
        GraphicUtils.circle(EmpathyRegions.magicCircle2, 0f, 0f, angle2 * 0.75f, 280f + 80f * lethalp, 48);

        Tmp.v1.trns(rotation, 40f * Mathf.sinDeg(Interp.pow2.apply(fin2) * 90f));
        GraphicUtils.draw3DEnd(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, 0f, -65f * Interp.pow2.apply(fin2) - 10f * Interp.pow2.apply(fin), -rotation - 90f, () -> {});
        //Draw.z((Layer.bullet + Layer.effect) / 2f);
        Draw.z((Layer.bullet + Layer.effect) / 2f);
        if(useLethal) drawLasers();
        Draw.z(origZ);
    }

    @Override
    boolean shouldDraw(){
        return unit.activeAttack == this;
    }
}
