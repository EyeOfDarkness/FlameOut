package flame.bullets;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.*;

public class EndNukeBulletType extends BasicBulletType{
    public EndNukeBulletType(){
        super(17f, 50000f, "missile-large");
        backColor = trailColor = hitColor = FlamePal.red;
        frontColor = FlamePal.red.cpy().mul(2f);

        shrinkY = 0f;
        width = 15f;
        height = 34f;

        trailLength = 5;
        trailWidth = 5f;
        
        lifetime = 60f;

        despawnHit = true;
        collidesTiles = false;
        scaleLife = true;

        hitEffect = Fx.none;
        despawnEffect = Fx.none;

        shootEffect = FlameFX.desNukeShoot;
        smokeEffect = Fx.none;
    }

    @Override
    public void hit(Bullet b, float x, float y){
        super.hit(b, x, y);
        float bx = b.x, by = b.y;
        Team team = b.team;

        int sid1 = FlameSounds.desNukeHit.at(bx, by, 1f, 2f);
        Core.audio.protect(sid1, true);
        float fall = Mathf.pow(Mathf.clamp(1f - FlameSounds.desNukeHit.calcFalloff(bx, by) * 1.1f), 1.5f);
        int sid2 = FlameSounds.desNukeHitFar.play(fall * 2f, 1f, FlameSounds.desNukeHit.calcPan(bx, by));
        Core.audio.protect(sid2, true);

        float[] arr = new float[360 * 3];
        Utils.rayCastCircle(b.x, b.y, 480f, t -> (t.block().isStatic() || t.block() instanceof Wall) && !Mathf.within(b.x, b.y, t.worldx(), t.worldy(), 150f), t -> {
            float dst = 1f - Mathf.clamp(Mathf.dst(bx, by, t.x * Vars.tilesize, t.y * Vars.tilesize) / 480f);
            if(Mathf.chance(Mathf.pow(dst, 2f) * 0.75f)) Fires.create(t);
        }, t -> {
            float nx = t.x * Vars.tilesize, ny = t.y * Vars.tilesize;
            float ang = Angles.angle(bx, by, nx, ny);

            FlameFX.desNukeShockSmoke.at(nx, ny, ang);
        }, bl -> {
            //float d = lethal ? 12000f + bl.maxHealth / 20f : bl.health / 1.5f;
            float d = 21000f + bl.maxHealth / 5f;

            EmpathyDamage.damageBuildingRaw(bl, d, true, () -> {
                SpecialDeathEffects eff = SpecialDeathEffects.get(bl.block);

                bl.block.destroySound.at(bl);

                if(eff.explosionEffect == Fx.none){
                    Fx.dynamicExplosion.at(bl.x, bl.y, (Vars.tilesize * bl.block.size) / 2f / 8f);
                }else{
                    eff.explosionEffect.at(bl.x, bl.y, bl.hitSize() / 2f);
                }

                float shake = bl.hitSize() / 3f;
                Effect.shake(shake, shake, bl);

                if(bl.block.createRubble && !bl.floor().solid && !bl.floor().isLiquid){
                    Effect.rubble(bl.x, bl.y, bl.block.size);
                }

                float healthBase = (bl.block.size * Vars.tilesize);
                FlameOut.devasBatch.baseZ = Layer.block;
                FlameOut.devasBatch.switchBatch(bl::draw, dev -> {
                    dev.lifetime = Mathf.random(1f, 2f) * 60f;
                    dev.health = (Math.min(dev.width, dev.height) / healthBase) * bl.maxHealth * 1.5f;
                    dev.team = team;
                    dev.explosion = eff.explosionEffect != Fx.none ? eff.explosionEffect : FlameFX.fragmentExplosion;
                    dev.collides = true;
                    dev.contagiousChance = 0.85f;
                    dev.slowDownAmount = 0.5f;

                    float dx = dev.x - x;
                    float dy = dev.y - y;
                    float len = Mathf.sqrt(dx * dx + dy * dy);
                    //float force = Mathf.clamp(1f - (len / (range + bl.hitSize() / 2f + 8f)));
                    float force = 1f / (1f + (len - 150f) / 500f);

                    Vec2 v = Utils.vv.set(dx, dy).nor().setLength(force * 18f);
                    if(!v.isNaN()){
                        dev.vx = v.x;
                        dev.vy = v.y;
                        dev.vr = Mathf.range(25f * force);
                    }
                });
            });
        }, arr);

        Utils.scanEnemies(b.team, b.x, b.y, 480f, true, true, t -> {
            if(t instanceof Unit u){
                //float damageScl = 1f;
                //if(u.isGrounded()) damageScl = Utils.inRayCastCircle(bx, by, arr, u);
                float damageScl = Utils.inRayCastCircle(bx, by, arr, u);

                if(damageScl > 0){
                    Tmp.v2.trns(Angles.angle(bx, by, u.x, u.y), (16f + 5f / u.mass()) * damageScl);
                    u.vel.add(Tmp.v2);

                    EmpathyDamage.damageUnit(u, (u.maxHealth / 10f + 10000f) * damageScl, true, () -> {
                        FlameOut.vaporBatch.discon = null;
                        FlameOut.vaporBatch.switchBatch(u::draw, null, (d, w) -> {
                            float with = Utils.inRayCastCircle(bx, by, arr, d);
                            if(with > 0.5f){
                                d.disintegrating = true;
                                float dx = d.x - bx, dy = d.y - by;
                                float len = Mathf.sqrt(dx * dx + dy * dy);
                                float force = (6f / (1f + len / 90f) + (len / 480f) * 1.01f);

                                Vec2 v = Tmp.v1.set(dx, dy).nor().setLength(force * Mathf.random(0.9f, 1f));

                                d.lifetime = Mathf.random(60f, 90f) * Mathf.lerp(1f, 0.5f, Mathf.clamp(len / 480f));
                                d.drag = -0.015f;

                                d.vx = v.x;
                                d.vy = v.y;
                                d.vr = Mathf.range((force / 3f) * 5f);
                                d.zOverride = Layer.flyingUnit;
                            }
                        });

                        FlameFX.desNukeVaporize.at(u.x, u.y, u.angleTo(bx, by) + 180f, u.hitSize / 2f);
                    });
                }
            }else if(t instanceof Building bl){
                float damageScl = Utils.inRayCastCircle(bx, by, arr, bl);
                if(damageScl > 0){
                    Runnable death = t.within(bx, by, 150f + bl.hitSize() / 2f) ? () -> {
                        FlameOut.vaporBatch.discon = null;
                        FlameOut.vaporBatch.switchBatch(bl::draw, null, (d, w) -> {
                            d.disintegrating = true;
                            float dx = d.x - bx, dy = d.y - by;
                            float len = Mathf.sqrt(dx * dx + dy * dy);
                            //float force = Math.max(10f / (1f + len / 50f), (len / 150f) * 3f);
                            float force = (3f / (1f + len / 50f) + (len / 150f) * 0.9f);
                            //float force = (len / 150f) * 15f;

                            Vec2 v = Tmp.v1.set(dx, dy).nor().setLength(force * Mathf.random(0.9f, 1f));

                            d.lifetime = Mathf.random(60f, 90f) * Mathf.lerp(1f, 0.5f, Mathf.clamp(len / 150f));
                            d.drag = -0.03f;

                            d.vx = v.x;
                            d.vy = v.y;
                            d.vr = Mathf.range((force / 3f) * 5f);
                            d.zOverride = Layer.turret + 1f;
                        });
                        //FlameFX.desNukeVaporize.at(u.x, u.y, u.angleTo(bx, by) + 180f, u.hitSize / 2f);
                        FlameFX.desNukeVaporize.at(bl.x, bl.y, bl.angleTo(bx, by) + 180f, bl.hitSize() / 2f);
                    } : () -> {
                        SpecialDeathEffects eff = SpecialDeathEffects.get(bl.block);

                        bl.block.destroySound.at(bl);

                        if(eff.explosionEffect == Fx.none){
                            Fx.dynamicExplosion.at(bl.x, bl.y, (Vars.tilesize * bl.block.size) / 2f / 8f);
                        }else{
                            eff.explosionEffect.at(bl.x, bl.y, bl.hitSize() / 2f);
                        }

                        float shake = bl.hitSize() / 3f;
                        Effect.shake(shake, shake, bl);

                        if(bl.block.createRubble && !bl.floor().solid && !bl.floor().isLiquid){
                            Effect.rubble(bl.x, bl.y, bl.block.size);
                        }

                        float healthBase = (bl.block.size * Vars.tilesize);
                        FlameOut.devasBatch.baseZ = Layer.block;
                        FlameOut.devasBatch.switchBatch(bl::draw, dev -> {
                            dev.lifetime = Mathf.random(1f, 2f) * 60f;
                            dev.health = (Math.min(dev.width, dev.height) / healthBase) * bl.maxHealth * 1.5f;
                            dev.team = team;
                            dev.explosion = eff.explosionEffect != Fx.none ? eff.explosionEffect : FlameFX.fragmentExplosion;
                            dev.collides = true;
                            dev.contagiousChance = 0.85f;
                            dev.slowDownAmount = 0.5f;

                            float dx = dev.x - x;
                            float dy = dev.y - y;
                            float len = Mathf.sqrt(dx * dx + dy * dy);
                            //float force = Mathf.clamp(1f - (len / (range + bl.hitSize() / 2f + 8f)));
                            float force = 1f / (1f + (len - 150f) / 500f);

                            Vec2 v = Utils.vv.set(dx, dy).nor().setLength(force * 15f);
                            if(!v.isNaN()){
                                dev.vx = v.x;
                                dev.vy = v.y;
                                dev.vr = Mathf.range(25f * force);
                            }
                        });
                    };

                    EmpathyDamage.damageBuildingRaw(bl, (bl.maxHealth / 10f + 10000f) * damageScl, true, death);
                }
            }
        });

        Effect.shake(60f, 120f, b.x, b.y);
        FlameFX.desNukeShockwave.at(b.x, b.y, 480f);
        FlameFX.desNuke.at(b.x, b.y, 479f, arr);

        FlameOutSFX.inst.impactFrames(bx, by, b.rotation(), 23f, false, () -> {
            for(int i = 0; i < arr.length; i++){
                float len1 = arr[i], len2 = arr[(i + 1) % arr.length];
                float ang1 = (i / (float)arr.length) * 360f;
                float ang2 = ((i + 1f) / arr.length) * 360f;

                float x1 = Mathf.cosDeg(ang1) * len1, y1 = Mathf.sinDeg(ang1) * len1;
                float x2 = Mathf.cosDeg(ang2) * len2, y2 = Mathf.sinDeg(ang2) * len2;

                Fill.tri(bx, by, bx + x1, by + y1, bx + x2, by + y2);
            }
        });
    }
}
