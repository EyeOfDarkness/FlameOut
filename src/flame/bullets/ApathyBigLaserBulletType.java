package flame.bullets;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.graphics.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.Turret.*;

public class ApathyBigLaserBulletType extends BulletType{
    float width = 180f;
    float length = 2500f;
    //1900
    //float length = 600f;

    float end = 340f;
    Interp dwidth = Interp.circleOut;
    Color[] colors = {FlamePal.primary.cpy().a(0.4f), FlamePal.primary, Color.white};
    float[] widths = {1.5f, 1f, 0.8f};

    TextureRegion hcircle;

    public ApathyBigLaserBulletType(){
        speed = 0f;
        collides = collidesTiles = false;
        absorbable = false;
        hittable = false;
        keepVelocity = false;

        removeAfterPierce = false;
        pierce = true;
        pierceArmor = true;
        pierceCap = -1;
        impact = true;

        lifetime = 15f * 60f;

        despawnEffect = Fx.none;
        shootEffect = Fx.none;
        //hitEffect = Fx.none;
        hitEffect = FlameFX.bigLaserHitSpark;

        status = StatusEffects.disarmed;
        statusDuration = 90f;

        damage = 12000f;

        drawSize = length * 2.1f;

        knockback = 60f;

        layer = (Layer.bullet + Layer.effect) / 2;
    }

    @Override
    public void load(){
        hcircle = Core.atlas.find("hcircle");
    }

    float getLaserWidth(float dst){
        return dwidth.apply(Mathf.clamp(dst / end));
    }
    float timeWidth(Bullet b){
        float w1 = 5f;
        return (w1 * Mathf.clamp(b.time / 60f) + (width - w1) * Interp.pow2Out.apply(Mathf.clamp((b.time - 140f) / 5))) * Interp.pow3In.apply(Mathf.clamp((lifetime - b.time) / 80f));
    }

    void handleDamage(Healthc e, Bullet b){
        float mul = e instanceof Unit u ? Math.max(1, u.healthMultiplier) : (e instanceof Building bl ? damage / Math.min(damage, bl.handleDamage(damage)) : 1f);

        //float d = b.time < 140f ? 0.125f : 1f;
        float health = e.health();

        if(e instanceof Unit u){
            //Tmp.v3.set(u).sub(b).nor().scl((knockback + Math.max((length / b.dst(u)), 0f)) * 80f);
            //Tmp.v3.set(u).sub(b).nor().scl((knockback + Math.max((1 - (b.dst(u) / length)) * 400f, 0f)) * 80f);
            float sss = Interp.pow2In.apply(Mathf.clamp(1 - b.dst(u) / length)) * length;
            //Tmp.v3.set(u).sub(b).nor().scl((knockback + Math.max((length - b.dst(u)) * 0.5f * d, 0f)) * 80f);
            //Tmp.v3.set(u).sub(b).nor().scl((knockback + sss * 0.5f * d) * 80f);
            Tmp.v3.set(u).sub(b).nor().scl((knockback + sss / (2 * (1.3157894f))) * 80f);
            if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));

            if(b.time < 140f){
                /*
                float adst = Math.min(Angles.angleDist(u.vel.angle(), Tmp.v3.angle()) * 3f, 180f);
                float acs = Math.max(Mathf.cos(adst), -0.15f);
                u.vel.scl(acs);
                 */
                FlameOutSFX.inst.addMovementLock(u, 8f);
            }else{
                FlameOutSFX.inst.cancelMovementLock(u);
                u.impulse(Tmp.v3);
                u.vel.add(Tmp.v3.scl(1 / (80f * 100)));
                //u.vel.limit(64f / (1 - u.drag));
                u.vel.limit(Mathf.pow(24f, 1 + u.drag));
                if(Float.isNaN(u.health) || Float.isNaN(u.shield)) u.destroy();
            }

            u.clearStatuses();
            u.apply(StatusEffects.disarmed, statusDuration);
            u.apply(StatusEffects.melting, 60f);

            for(WeaponMount m : u.mounts){
                m.reload = m.weapon.reload;
            }
            health = u.health;
        }
        if(e instanceof Building bl){
            bl.applyHealSuppression(120f);
            bl.applySlowdown(0.1f, 120f);

            if(e instanceof TurretBuild tr){
                tr.reloadCounter = Math.max(0, tr.reloadCounter - 4f * Time.delta);
            }

            health = bl.health;
            if(bl.team == Team.derelict && b.time >= 140f){
                Sounds.explosion.at(bl.x, bl.y);
                FlameFX.aoeExplosion2.at(bl.x, bl.y, bl.hitSize() / 2f);
                bl.tile.remove();
            }
        }

        //e.damagePierce(Math.max(Math.max(b.damage, damage) * d, (e.maxHealth() * mul) / 50f) * d);
        if(b.time >= 140f){
            float tscl = Mathf.clamp((lifetime - b.time) / 80f);
            float dam = Math.max(Math.max(b.damage, damage), (e.maxHealth() * mul) / 50f) * tscl;
            FlameOutSFX.inst.lockHealing(e, health - dam / 10f, 5f * 60f);
            e.damagePierce(dam);
        }
        //if(Float.isNaN(e.health())) e.des
    }

    @Override
    public void init(Bullet b){
        Tmp.v1.trns(b.rotation(), 40f).add(b.x, b.y);
        FlameFX.shootShockWave.at(Tmp.v1.x, Tmp.v1.y, b.rotation(), 120f);
        //Tmp.v1.trns(b.rotation(), 80f).add(b.x, b.y);
        //FlameEffects.shootShockWave.at(Tmp.v1.x, Tmp.v1.y, b.rotation(), 300f);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(Bullet b){
        if(b.timer(0, 5f)){
            Vec2 v = Utils.v.trns(b.rotation(), length).add(b);
            float vx = v.x;
            float vy = v.y;
            float dw = timeWidth(b);

            for(TeamData data : Vars.state.teams.present){
                if(data.team != b.team){
                    if(data.unitTree != null){
                        Utils.intersectLine(data.unitTree, dw, b.x, b.y, vx, vy, (u, x, y) -> {
                            Vec2 nearest = Intersector.nearestSegmentPoint(b.x, b.y, vx, vy, u.x, u.y, Utils.vv);
                            float dst = b.dst(nearest);
                            //float dst2 = b.dst(u.x, u.y);
                            float lw = getLaserWidth(dst) * dw * 0.5f;
                            if(u.dst(nearest) <= lw + u.hitSize / 2f){
                                hit(b, x, y);
                                if(b.time > 140f) FlameFX.bigLaserHit.at(x, y, b.rotation(), u);
                                handleDamage(u, b);
                            }
                        });
                    }
                    if(data.buildingTree != null){
                        Utils.intersectLine(data.buildingTree, dw, b.x, b.y, vx, vy, (bl, x, y) -> {
                            Vec2 nearest = Intersector.nearestSegmentPoint(b.x, b.y, vx, vy, bl.x, bl.y, Utils.vv);
                            float dst = b.dst(nearest);
                            //float dst2 = b.dst(bl.x, bl.y);
                            float lw = getLaserWidth(dst) * dw * 0.5f;
                            if(bl.dst(nearest) <= lw + bl.hitSize() / 2f){
                                hit(b, x, y);
                                if(b.time > 140f) FlameFX.bigLaserHit.at(x, y, b.rotation(), bl.hitSize());
                                handleDamage(bl, b);
                            }
                        });
                    }
                }
            }

            /*Why Anuke. why doesnt it return a generic*/
            Utils.intersectLine((QuadTree<Bullet>)Groups.bullet.tree(), dw, b.x, b.y, vx, vy, (bl, x, y) -> {
                if(bl.team == b.team) return;
                Vec2 nearest = Intersector.nearestSegmentPoint(b.x, b.y, vx, vy, bl.x, bl.y, Utils.vv);
                float dst = b.dst(nearest);
                //float dst2 = b.dst(u.x, u.y);
                float lw = getLaserWidth(dst) * dw * 0.5f;
                if(bl.dst(nearest) <= lw + bl.hitSize / 2f){
                    if(bl.type.speed > 0.001 || Angles.within(bl.angleTo(b), bl.rotation(), 2)){
                        bl.hit = false;
                        bl.remove();
                        bl.type.despawnEffect.at(bl.x, bl.y, bl.rotation(), bl.type.hitColor);
                    }
                    
                }
            });
        }
        if(b.owner instanceof Healthc h && h.dead() && b.time < lifetime - 80f){
            b.fdata = 2;
            //b.time = b.time > 140f ? lifetime - 80f : lifetime - 16f;
            if(b.time > 140f){
                b.time = lifetime - 80f;
            }else{
                b.remove();
                return;
            }
        }
        if(b.fdata < 1 && b.time > 140f){
            b.fdata = 2;
            
            Tmp.v1.trns(b.rotation(), 70f).add(b.x, b.y);
            FlameFX.shootShockWave.at(Tmp.v1.x, Tmp.v1.y, b.rotation(), 200f);
            Tmp.v1.trns(b.rotation(), 180f).add(b.x, b.y);
            FlameFX.shootShockWave.at(Tmp.v1.x, Tmp.v1.y, b.rotation(), 350f);

            FlameOutSFX.inst.impactFrames(b, b.x, b.y, b.rotation(), 23f, true);
            Vars.renderer.shake(80f, 90f);
        }
    }

    @Override
    public void draw(Bullet b){
        Vec2 v = Tmp.v1.trns(b.rotation(), end).add(b.x, b.y);
        Vec2 v2 = Tmp.v2.trns(b.rotation(), length - end).add(v.x, v.y);
        float w = timeWidth(b);

        for(int i = 0; i < 3; i++){
            float dw = w * widths[i] * (1 + Mathf.absin(8f, 0.1f));

            Draw.color(colors[i]);
            Lines.stroke(dw);
            Draw.rect(hcircle, v.x, v.y, end * 2, Lines.getStroke(), b.rotation() + 180f);
            Lines.lineAngle(v.x, v.y, b.rotation(), length - end, false);
            Drawf.tri(v2.x, v2.y, Lines.getStroke(), width * 2 + dw, b.rotation());
        }
        if(b.time > 140f){
            float scl = Interp.pow2Out.apply(Mathf.clamp((b.time - 140f) / 5)) * Interp.pow3In.apply(Mathf.clamp((lifetime - b.time) / 80f));
            Rand r = Utils.rand, r2 = Utils.rand2;
            r.setSeed(b.id + 1236);

            for(int i = 0; i < 75; i++){
                float d = r.random(20f, 40f);
                float time = (Time.time + r.random(d));
                float dtime = (time % d) / d;
                float slope = Interp.pow2Out.apply(Mathf.curve(dtime, 0f, 0.05f) * Mathf.curve(1 - dtime, 0f, 0.05f));

                int seed = (int)(time / d) + r.nextInt();

                r2.setSeed(seed);

                //float dw = r2.random(15f, 30f) * scl * slope;
                float dh = r2.random(120f, 220f);
                float dw = dh * r2.random(0.05f, 0.15f) * scl * slope;
                //float yp = (dtime * (length - dh)) + dh;
                float rx = r2.range(Math.max(w - dw, 0f)) / 2f;
                float arx = Math.abs(rx);
                //float yp = dtime * length + dh;
                float xp = rx * getLaserWidth(dtime * (length - dh) + dh);
                float yp = dtime * ((length - (end + this.width / 2)) + (w - arx) * 4.5f) + dh;
                Color rc = r2.chance(0.1f + Interp.pow3In.apply((w - arx) / w) * 0.7f) ? Color.white : FlamePal.primary;

                Draw.color(rc);
                v.trns(b.rotation(), (yp + end / 1.5f) - (w - arx) * 1.5f, xp).add(b.x, b.y);
                GraphicUtils.diamond(v.x, v.y, dw, dh, b.rotation());
            }
        }
        Drawf.light(b.x, b.y, v2.x, v2.y, w * 2 + 20f, colors[0], 0.75f);
    }

    @Override
    public void drawLight(Bullet b){
    }
}
