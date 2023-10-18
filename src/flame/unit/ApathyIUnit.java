package flame.unit;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.graphics.*;
import flame.unit.empathy.*;
import flame.unit.shifts.*;
import mindustry.audio.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class ApathyIUnit extends UnitEntity{
    ShiftHandler current, last;
    float shiftProgress = 0f;
    boolean shifting = false;

    float heartBeatTimer = 0f;
    protected float stress = 0f;
    protected float conflict = 0f;

    protected float shieldHealth = 10000f;
    protected float extraShiftSpeed = 1f;

    public float shiftRotation = 0f;
    public float deathTimer = 0f;

    int deathSoundIdx = 0;
    int shiftSound = -1;

    @Override
    public boolean serialize(){
        return false;
    }

    @Override
    public void setType(UnitType type){
        super.setType(type);
        ApathyUnitType u = (ApathyUnitType)type;
        current = u.handlers.get(0);
    }

    @Override
    public boolean isBoss(){
        return true;
    }

    @Override
    public void collision(Hitboxc other, float x, float y){
        super.collision(other, x, y);
        if(other instanceof Bullet b){
            float dps = b.type.estimateDPS();
            if(dps <= 0f) return;
            
            if(current.critPoints != null){
                float[] crit = current.critPoints;
                for(int i = 0; i < current.critPoints.length; i += 2){
                    float angle = rotation + crit[i];
                    float rotTo = angleTo(b);
                    float dst = dst(b);
                    float at = Mathf.angle(dst, 5f);
                    //float at = Mathf.angle(10f, dst);
                    if(Angles.within(angle, rotTo, crit[i + 1]) && Angles.within(rotTo, b.rotation() + 180f, at) && (b.vel.len() > 24f || dst > type.hitSize)){
                        //
                        //Fx.scatheExplosion.at(this.x, this.y);
                        //Log.info(at);
                        damagePierce(dps * 10f);
                        stress += dps / 2f;

                        ((ApathyIAI)controller).criticalHit(dps * 10f);

                        if(dps * 10 > 500000f) FlameFX.apathyCrit.at(this.x, this.y, rotation, this);
                        break;
                    }
                }
            }
            stress += dps / 60f;
            //shieldHealth -= (b.type.estimateDPS() / 2f);
        }
    }
    
    public float getStressScaled(){
        return Math.min(Math.max(stress / 100f, Mathf.clamp(1 - (health / maxHealth) * 2)), 2f);
    }

    protected float getShieldRadius(){
        return (shieldHealth / 10000f) * current.shieldRadius;
    }

    @Override
    public void update(){
        if(health < 0f) health = 0f;
        super.update();
        float stressScaled = getStressScaled();

        if(shifting && last != null){
            float scl = current.stressAffected ? (1 + stressScaled) : 1f;
            shiftProgress = Mathf.clamp(shiftProgress + (1f / current.shiftDuration) * Time.delta * scl * extraShiftSpeed);
            //shiftProgress = Mathf.clamp(shiftProgress + 0.005f * Time.delta);
            //last.updateShift(this, 1 - shiftProgress, true);
            //current.updateShift(this, shiftProgress, false);
            float curve = Mathf.curve(shiftProgress, 0, current.lastShiftEnd);
            
            last.updateShift(this, 1 - curve, true);
            current.updateShift(this, shiftProgress, false);

            if(shiftProgress >= 1){
                shifting = false;
                extraShiftSpeed = 1f;

                if(shiftSound != -1){
                    Core.audio.stop(shiftSound);
                    shiftSound = -1;
                }
                if(current instanceof PrismShift) FlameSounds.clang.at(x, y);
            }
        }else{
            current.update(this);
            current.updateShift(this, 1f, false);
        }
        if(dead){
            if(deathTimer <= 0f){
                //FlameSounds.apathyDeathCry.at(x, y, 1, 3);
                FlameSounds.apathyDeathCry.play(2f);
            }
            deathTimer += Time.delta;
            if(deathTimer > 45f){
                Rand r = Utils.rand;
                r.setSeed(id * 531L);

                int count = (int)(Mathf.pow((deathTimer - 45) / (4f * 60f - 45), 2) * 16) + 1;
                for(int i = 0; i < count; i++){
                    float fin = (i / 16f) * 0.5f + 0.5f;

                    Vec2 v = Tmp.v1.trns(r.random(360f), r.random(hitSize / 2));
                    Color tc = Tmp.c1;
                    tc.r = r.random(80f, 120f) * fin;
                    tc.g = r.random(380f, 420f) * fin;

                    float angle = v.angle();

                    if(Mathf.chanceDelta(0.75f)){
                        FlameFX.apathyBleed.at(x + v.x, y + v.y, angle, tc, this);
                    }

                    if(i >= deathSoundIdx){
                        FlameSounds.apathyBleed.at(x + v.x, y + v.y, Mathf.random(0.9f, 1.1f), 1f);
                        deathSoundIdx++;
                    }
                }
            }
            if(deathTimer > 4f * 60f){
                Call.unitDestroy(id);
            }
        }

        heartBeatTimer += ((1f + stressScaled / 2f) / 40f) * Time.delta;
        if(heartBeatTimer >= Math.max(2.5f - stressScaled * 0.75f, 1f)){
            heartBeatTimer = 0f;
        }
        
        if(stress > 0f){
            float t = conflict > 0f ? 1000f : 70f;
            stress -= (5f + stress / t) * Time.delta;
            if(stress < 0f){
                stress = 0f;
            }
        }
        if(conflict > 0) conflict -= Time.delta;

        if(shieldHealth < 10000f){
            shieldHealth += (25f + stress) * Time.delta;
            if(shieldHealth > 10000f){
                shieldHealth = 10000f;
            }
        }
    }

    @Override
    public void destroy(){
        super.destroy();
        BloodSplatter.explosion(95, x, y, hitSize / 2, 400f, 45f);
        BloodSplatter.explosion(40, x, y, hitSize / 2.5f, 200f, 45f);

        BloodSplatter.explosion(40, x, y, hitSize / 2, 550f, 35f, 60f, FlamePal.blood, 0.2f);

        //FlameFX.apathyDeath.at(x, y, hitSize / 2f);
        FlameFX.apathyDeath.at(x, y, hitSize * 2f);
        //FlameSounds.apathyDeath.at(x, y, 1, 3);
        FlameSounds.apathyDeath.play(1f);
    }

    float getHeartBeat(){
        if(heartBeatTimer > 1) return 0f;
        float f = heartBeatTimer;
        float p = Mathf.curve(f, 0.1f, 0.25f);
        p = p * p * 180f;
        p = Mathf.sinDeg(p) * 0.12f;

        float qrs = Mathf.curve(f, 0.39f, 0.61f);
        float w = Mathf.sinDeg((qrs * (360 + 180)) - 180);
        w = w * Mathf.sinDeg(Mathf.pow(qrs, 1.25f) * 180);
        w *= w > 0 ? 0.9 : 0.5;

        float t = Mathf.curve(f, 0.70f, 0.85f);
        t = t * t * 180;
        t = Mathf.sinDeg(t) * 0.12f;

        return p + w + t;
    }

    @Override
    public void draw(){
        //super.draw();
        boolean isPayload = !isAdded();
        float z = isPayload ? Draw.z() : elevation > 0.5f ? (type.lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : type.groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);

        /*
        if(!isPayload && (isFlying())){
            Draw.z(Math.min(Layer.darkness, z - 1f));
            type.drawShadow(this);
        }
        */
        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        type.drawSoftShadow(this);

        Draw.z(z);
        float hb = 1 + getHeartBeat() * 0.25f;

        Draw.color(FlamePal.primary);
        Fill.circle(x, y, 5f * hb);
        Draw.color();
        Fill.circle(x, y, 3.75f * hb);

        //Draw.mixcol(Tmp.c1, Math.max(unit.hitTime, !healFlash ? 0f : Mathf.clamp(unit.healTime)));
        Draw.mixcol(Color.white, hitTime);
        if(!dead){
            if(shifting && last != null){
                float curve = Mathf.curve(shiftProgress, 0, current.lastShiftEnd);

                last.drawOut(this, curve);
                current.drawIn(this, shiftProgress);
            }else{
                current.drawFull(this);
            }
        }else{
            GraphicUtils.drawDeath(type.region, 43141 + id * 133, x, y, rotation, Mathf.clamp(deathTimer / 36), Mathf.clamp((deathTimer - 60f) / (3f * 60f)));
        }
        Draw.mixcol();
    }

    protected void switchShift(ShiftHandler h){
        if(h != current){
            float scl = h.stressAffected ? (1 + getStressScaled()) : 1f;
            //float scl = current.stressAffected ? (1 + stressScaled) : 1f;
            //shiftProgress = Mathf.clamp(shiftProgress + (1f / current.shiftDuration) * Time.delta * scl * extraShiftSpeed);
            shiftSound = h.shiftSound.at(x, y, scl * extraShiftSpeed);
            if(h instanceof StrongLaserShift) shiftSound = -1;
        }

        shiftProgress = 0;
        last = current;
        current = h;
        shifting = true;
    }

    @Override
    public void remove(){
        super.remove();
        if(((ApathyIAI)controller).sounds != null){
            for(SoundLoop sl : ((ApathyIAI)controller).sounds){
                sl.stop();
            }
        }
        if(!Groups.isClearing){
            EmpathyDamage.spawnEmpathy(x, y);
        }
    }
}
