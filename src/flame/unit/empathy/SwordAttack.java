package flame.unit.empathy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.audio.*;
import flame.effects.*;
import flame.graphics.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class SwordAttack extends AttackAI{
    Vec3 pos = new Vec3(), truePos = new Vec3(), velocity = new Vec3();
    SwordRotation rotation = new SwordRotation();
    float move = 0f;
    float swingRot = 0f, swingRot2 = -1f;
    float swordRotTime = 0f;
    float waitTime = 0f, endVelocity = 0f;
    boolean swinging = false, swinging2 = false, ending = false;
    int swings = 0;
    float[] positions = new float[len * 3], rotations = new float[len * 4];
    PointTrail trail = new PointTrail();

    static int len = 16;
    static SwordRotation tmpRot = new SwordRotation();

    SwordAttack(){
        super();
        addSoundDoppler(Sounds.spellLoop);
    }

    @Override
    float weight(){
        return unit.useLethal() ? 1000f + unit.extraLethalScore() : 1f;
    }

    @Override
    boolean canTeleport(){
        return false;
    }

    @Override
    void init(){
        //Tmp.v1.trns(0, 140f);
        truePos.set(-140f, 0f, 0f).rotate(Vec3.X, 0f).rotate(Vec3.Z, -unit.rotation);
        rotation.src = 180f;
        rotation.x = 0f;
        rotation.y = 0f;
        rotation.z = -unit.rotation;
        swinging = swinging2 = ending = false;
        swingRot2 = -1f;
        trail.clear();
    }

    @Override
    void reset(){
        init();
    }

    @Override
    void update(){
        if(!swinging2){
            generateSwing();
            move = 0f;
            swinging2 = true;
            swordRotTime = 0f;

            swings++;
        }else{
            if(move >= 0.8f){
                swinging2 = false;
                if(swings >= 4){
                    //waitTime = 0f;
                    ending = true;
                    swings = 0;
                }
            }
        }
        if(!ending){
            //waitTime += Time.delta;
            waitTime = Math.min(60f, waitTime + Time.delta);
        }else{
            waitTime = Math.max(0f, waitTime - Time.delta);
            if(waitTime <= 0f){
                swinging = false;
                ending = false;
                unit.randAI(true, unit.health < 50);
            }
        }
        //float[] p = positions;

        if(waitTime >= 60f && !ending){
            swinging = true;
            swordRotTime = Mathf.clamp(swordRotTime + Time.delta / 30f);
            endVelocity = 1f;
            
            float srt = Interp.pow2Out.apply(swordRotTime);
            
            float adst = Mathf.clamp(1f - (Angles.angleDist(rotation.src, targetRotation(Interp.pow2Out.apply(move)).src) - 5f) / (180f - 5f));

            move = Mathf.clamp(move + (Time.delta / 80f) * adst);
            float f = Interp.pow2Out.apply(move);

            //float f = Interp.pow3.apply(Interp.pow2Out.apply(move));
            pos.lerp(interpolate(f), Mathf.clamp(0.1f * Time.delta));
            slerp(targetRotation(f), Mathf.clamp(0.075f * Time.delta) * srt);

            boolean upVel = move < 0.95f;
            float vdst = 1 - Mathf.clamp(pos.dst(Tmp.v33.set(truePos).add(velocity, Time.delta)) / 20);

            if(upVel){
                Vec3 off2 = Tmp.v32.set(truePos).add(velocity, Time.delta * 2f);
                Vec3 off = Tmp.v31.set(pos).sub(off2).scl(1 / 20f).limit(2f);
                velocity.add(off);
            }

            truePos.add(velocity, Time.delta * adst);
            truePos.lerp(pos, Time.delta * adst * 0.05f);
            if(upVel) velocity.scl(1 - (0.05f + 0.075f * vdst) * Time.delta);
        }else if(swinging){
            //float av = 1 - (waitTime / 60f);
            truePos.add(velocity, Time.delta);
            velocity.scl(1 - (0.05f) * Time.delta);
            
            rotation.src += -endVelocity * Time.delta * 0.75f;
            endVelocity *= 1 - (0.05f * Time.delta);
        }

        //trail
        Vec3 tv = transform(62f, 0f, 0f);
        float tx = tv.x + unit.x, ty = tv.y + unit.y;
        tv = transform(349f, 0f, 0f);
        float tx2 = tv.x + unit.x, ty2 = tv.y + unit.y;
        trail.update(tx, ty, tx2, ty2);

        PassiveSoundLoop sound = sounds.get(0);

        if(waitTime > 0.001f){
            sound.play(tx2, ty2, 2f * (waitTime / 60f), true);
            unit.move(5, 3, 0.125f, 0.125f);
        }else{
            sound.stop();
        }

        if(waitTime >= 60f && !ending){
            Utils.hitLaser(unit.team, 15f, tx, ty, tx2, ty2, null, h -> false, (h, x, y) -> {
                Fx.colorSpark.at(x, y, unit.rotation, FlamePal.empathy);
                if(h instanceof Unit u){
                    float ele = u.elevation;
                    EmpathyDamage.damageUnit(u, Math.max(8000f, u.maxHealth / 35f) * Time.delta, true, Math.max(u.hitSize, u.type.legLength) < 30f ? null : () -> {
                        //SpecialDeathEffects.get(u.type.name).deathUnit(u, x, y, unit.angleTo(tx, ty));
                        float tz = ele > 0.5f ? (u.type.lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : (u.type.groundLayer + Mathf.clamp(u.hitSize / 4000f, 0f, 0.01f));
                        float shad = Mathf.clamp(ele, u.type.shadowElevation, 1f) * u.type.shadowElevationScl;

                        float sliceAng = unit.rotation + (swingRot2 - 90f);
                        Tmp.v1.trns(sliceAng, u.hitSize + 150f);
                        float vx = Tmp.v1.x, vy = Tmp.v1.y;

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

                            c.cutWorld(u.x - vx, u.y - vy, u.x + vx, u.y + vy, null);
                        };
                        batch.switchBatch(u::draw);
                    });
                }else if(h instanceof Building b){
                    EmpathyDamage.damageBuilding(b, Math.max(8000f, b.maxHealth / 35f) * Time.delta, true, null);
                }
            });
        }
    }

    void slerp(SwordRotation target, float d){
        rotation.src = Mathf.slerp(rotation.src, target.src, d);
        rotation.x = Mathf.slerp(rotation.x, target.x, d);
        rotation.y = Mathf.slerp(rotation.y, target.y, d);
        rotation.z = Mathf.slerp(rotation.z, target.z, d);
    }

    void generateSwing(){
        //float rotation = Mathf.random(360f);
        float rotation = swingRot;
        float ang = unit.rotation;
        float[] p = positions;
        float[] r = rotations;

        for(int i = 0; i < len; i++){
            int id = i * 3;
            int id2 = i * 4;
            //float f = ((i / (len - 1f)) - 0.5f) * 2f * 120f;
            float f = ((i / (len - 1f)) - 0.5f) * 2f * 120f;
            Tmp.v1.trns(f, 140f);
            Tmp.v31.set(Tmp.v1.x, Tmp.v1.y, 0f).rotate(Vec3.X, rotation).rotate(Vec3.Z, -ang);
            p[id] = Tmp.v31.x;
            p[id + 1] = Tmp.v31.y;
            p[id + 2] = Tmp.v31.z;

            r[id2] = -f;
            r[id2 + 1] = rotation;
            r[id2 + 2] = 0f;
            r[id2 + 3] = -ang;
        }
        float lstrot = swingRot;
        swingRot = Mathf.mod(swingRot + Mathf.range(20f, 60f) + 180, 360);
        if(swingRot2 == -1f){
            swingRot2 = swingRot;
        }else{
            swingRot2 = lstrot;
        }
    }

    SwordRotation targetRotation(float f){
        int idx = Mathf.clamp((int)(f * (len - 2)), 0, len - 2);
        int pos = idx * 4;
        int nps = (idx + 1) * 4;

        float m = f < 1f ? (f * (len - 2)) % 1f : 1f;

        float[] r = rotations;

        tmpRot.src = Mathf.slerp(r[pos], r[nps], m);
        tmpRot.x = Mathf.slerp(r[pos + 1], r[nps + 1], m);
        tmpRot.y = Mathf.slerp(r[pos + 2], r[nps + 2], m);
        tmpRot.z = Mathf.slerp(r[pos + 3], r[nps + 3], m);
        
        return tmpRot;
    }
    Vec3 interpolate(float f){
        //int pos = Mathf.clamp((int)(f * (len - 1)) * 3, 0, length - 3);

        int idx = Mathf.clamp((int)(f * (len - 2)), 0, len - 2);
        //int nid = idx + 1;

        int pos = idx * 3;
        int nps = (idx + 1) * 3;

        float m = f < 1f ? (f * (len - 2)) % 1f : 1f;

        float[] p = positions;

        return Tmp.v33.set(p[pos], p[pos + 1], p[pos + 2]).lerp(Tmp.v32.set(p[nps], p[nps + 1], p[nps + 2]), m);
    }

    @Override
    boolean shouldDraw(){
        return true;
    }
    
    Vec3 transform(float x, float y, float z){
        Vec3 v = Tmp.v32.set(x, y, z).rotate(Vec3.Z, rotation.src).rotate(Vec3.X, rotation.x).rotate(Vec3.Y, rotation.y).rotate(Vec3.Z, rotation.z).add(truePos);
        float sz = 700f / (700f - v.z);
        return v.scl(sz);
    }

    @Override
    void draw(){
        TextureRegion r = EmpathyRegions.sword, r2 = EmpathyRegions.swordSide;
        float dw = r.width * Draw.scl, dh = r.height * Draw.scl;
        float dw2 = r2.width * Draw.scl, dh2 = r2.height * Draw.scl;
        float sin = Mathf.absin(0.5f, 1f);
        float alpha = waitTime / 60f;
        //green = width, red = height

        float z = Draw.z();
        Draw.z(Layer.flyingUnit + (truePos.z > 0 ? 1f : -1f));

        Draw.blend(Blending.additive);
        Draw.color(FlamePal.empathyDark);
        Draw.alpha(alpha);
        trail.draw();
        //Draw.color();

        Draw.color(Color.white, FlamePal.empathyDark, sin);
        Draw.alpha(alpha);
        EmpathyRegions.swordSideSeg.render(dw2 - 50f, 0f, dw2 * 2, dh2 * 2, p -> {
            Vec3 v = transform(p.x, 0f, p.y);
            p.x = v.x + unit.x;
            p.y = v.y + unit.y;
        });
        EmpathyRegions.swordSeg.render(dw - 50f, 0f, dw * 2, dh * 2, p -> {
            Vec3 v = transform(p.x, p.y, 0f);
            p.x = v.x + unit.x;
            p.y = v.y + unit.y;
        });

        Draw.color();
        Draw.z(z);
        Draw.blend();
    }
    
    static class SwordRotation{
        float src;
        float x, y, z;
    }
}
