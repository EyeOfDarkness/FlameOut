package flame.unit.empathy;

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
import mindustry.entities.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import java.util.*;

public class DashAttack extends AttackAI{
    float time = attackTime - 120f;
    float[] vision = new float[360 * 2], distance = new float[360 * 2];
    float targetAngle = -1f;
    float targetDistance = 0f;

    float lastX, lastY, newX, newY, dashTime;
    float hitX, hitY;
    int dashes = 0;
    boolean queue, dashEnded;
    //static final float attackTime = 15f * 60f;
    static final float attackTime = 3f * 60f;

    @Override
    void init(){
        dashes = 0;
    }

    @Override
    void updatePassive(){
        time += Time.delta;
        dashTime -= Time.delta;
    }

    @Override
    float weight(){
        return time < attackTime ? -1f : (unit.useLethal() ? 1000f + unit.extraLethalScore() : 2f);
        //return super.weight();
    }

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    void update(){
        //boolean dashEnded = false;
        
        if(time >= attackTime){
            Arrays.fill(vision, 0f);
            Arrays.fill(distance, 0f);
            int visLen = vision.length;

            for(TeamData data : Vars.state.teams.present){
                if(data.team != unit.team){
                    for(Unit u : data.units){
                        if(u instanceof TimedKillc) continue;
                        
                        float scr = FlameOutSFX.inst.getUnitDps(u.type) + u.health / 100f + 10;
                        if(u.spawnedByCore) scr *= 0.15f;
                        float angle = unit.angleTo(u);
                        float dst = unit.dst(u);
                        int idx = (int)((angle / 360f) * visLen);
                        int size = (int)((Mathf.angle(dst, u.hitSize / 3) / 360f) * visLen);

                        int midx = Mathf.mod(idx, visLen);

                        vision[midx] += scr;
                        distance[midx] = Math.max(distance[midx], dst + u.hitSize);
                        for(int i = 1; i < size; i++){
                            float out = 1f - (i / (float)size);
                            for(int s = 0; s < 2; s++){
                                int sid = Mathf.mod(idx + i * Mathf.signs[s], visLen);
                                int msd = Mathf.mod(sid, visLen);
                                vision[msd] += scr * out;
                                distance[msd] = Math.max(distance[msd], dst + u.hitSize);
                            }
                        }
                    }
                    for(Building b : data.buildings){
                        float scr = (b.health / 40f) + 10;
                        float angle = unit.angleTo(b);
                        float dst = unit.dst(b);
                        int idx = (int)((angle / 360f) * visLen);
                        int size = (int)((Mathf.angle(dst, b.hitSize() / 3) / 360f) * visLen);

                        if(b.block.group == BlockGroup.walls){
                            scr /= 40f;
                        }else{
                            for(BlockFlag bf : b.block.flags.array){
                                switch(bf){
                                    case turret -> scr = scr * 3 + (b instanceof TurretBuild tb ? (tb.hasAmmo() ? FlameOutSFX.inst.getBulletDps(tb.peekAmmo()) : 0f) : 1000f);
                                    case core -> scr = 1;
                                    case reactor, generator, factory -> scr *= 2;
                                }
                            }
                        }

                        int midx = Mathf.mod(idx, visLen);

                        vision[midx] += scr;
                        distance[midx] = Math.max(distance[midx], dst + b.hitSize());
                        for(int i = 1; i < size; i++){
                            float out = 1f - (i / (float)size);
                            for(int s = 0; s < 2; s++){
                                int sid = Mathf.mod(idx + i * Mathf.signs[s], visLen);
                                int msd = Mathf.mod(sid, visLen);
                                vision[msd] += scr * out;
                                distance[msd] = Math.max(distance[msd], dst + b.hitSize());
                            }
                        }
                    }
                }
            }

            int max = -1;
            float maxf = 0f;
            float dist = 0f;
            for(int i = 0; i < visLen; i++){
                if(vision[i] > maxf){
                    max = i;
                    maxf = vision[i];
                    dist = distance[i];
                }
            }
            if(max != -1){
                Tmp.v1.trns((max / (float)(visLen)) * 360f, 1f);
                float tx = Tmp.v1.x;
                float ty = Tmp.v1.y;

                for(int i = 1; i < 5; i++){
                    float fin = i / 5f;
                    for(int s = 0; s < 2; s++){
                        int id = Mathf.mod(max + i * Mathf.signs[s], visLen);
                        dist = Math.max(dist, distance[id]);
                        float f = (vision[id] / maxf) * (1f - fin);
                        Tmp.v1.trns((id / (float)(visLen)) * 360f, f);
                        tx += Tmp.v1.x;
                        ty += Tmp.v1.y;
                    }
                }

                targetAngle = Angles.angle(tx, ty);
                targetDistance = dist;
            }else{
                targetAngle = -1f;
                targetDistance = 0;
            }

            //time = 0f;
            // && unit.health <= 0.5f
            if(dashes < 2 && unit.health <= 0.5f){
                time = attackTime - 15f;
                dashes++;
            }else{
                time = 0f;
                dashes = 0;
                //unit.randAI(true, false);
                dashEnded = true;
            }
        }

        if(queue){
            float ang = Angles.angle(lastX, lastY, unit.x, unit.y);
            hitX = lastX;
            hitY = lastY;

            Vec2 n = Intersector.nearestSegmentPoint(lastX, lastY, unit.x, unit.y, Core.camera.position.x, Core.camera.position.y, Tmp.v5);
            FlameSounds.empathyDash2.at(n.x, n.y, 1f, 2f);

            int l = (int)(Mathf.dst(lastX, lastY, unit.x, unit.y) / 8);
            for(int i = 0; i < l; i++){
                float f = (i / (float)l) + Mathf.random(1f / l);
                Tmp.v2.set(lastX, lastY).lerp(unit.x, unit.y, f);
                Floor floor = Vars.world.floorWorld(Tmp.v2.x, Tmp.v2.y);

                FlameFX.empathyDashDust.at(Tmp.v2.x, Tmp.v2.y, ang, floor.mapColor);
            }

            Utils.hitLaser(unit.team, 12f, lastX, lastY, unit.x, unit.y, null, h -> false, (h, x, y) -> {
                Vec2 near = Intersector.nearestSegmentPoint(lastX, lastY, unit.x, unit.y, h.x(), h.y(), Tmp.v5);
                float f = Mathf.clamp(h.dst(near) / (50f + (h instanceof Sized s ? s.hitSize() / 2 : 0f)));
                Vec2 v = Tmp.v4.set(x, y).lerp(near, f);
                float vx = v.x, vy = v.y;

                if(Mathf.dst(hitX, hitY, x, y) > 100){
                    FlameFX.empathyPrimeShockwave.at(x, y, ang);
                }
                hitX = near.x;
                hitY = near.y;

                if(h instanceof Unit u){
                    EmpathyDamage.damageUnit(u, Math.max(55000f, u.maxHealth / 2.5f), true, () -> {
                        SpecialDeathEffects.get(u.type).deathUnit(u, vx, vy, ang);
                    });
                }else if(h instanceof Building b){
                    EmpathyDamage.damageBuilding(b, Math.max(55000f, b.maxHealth / 2.5f), true, b.block.size < 4 ? null : () -> {
                        SpecialDeathEffects.get(b.block).deathBuilding(b, vx, vy, ang);
                    });
                }
            });
            newX = unit.x;
            newY = unit.y;
            dashTime = 15f;
            queue = false;
            
            if(dashEnded){
                time = 0f;
                dashes = 0;
                dashEnded = false;
                unit.randAI(true, unit.health < 50);
            }
            //dashes++;
        }

        if(targetAngle != -1){
            if(Angles.within(unit.rotation, targetAngle, 0.1f)){
                lastX = unit.x;
                lastY = unit.y;
                queue = true;

                Tmp.v1.trns(targetAngle, targetDistance + 20f);
                unit.move(5.1f, 2, Tmp.v1.x, Tmp.v1.y);

                if(Tmp.v1.len2() > 40f * 40f) FlameFX.empathyDashShockwave.at(unit.x, unit.y, targetAngle);

                targetAngle = -1;
            }else{
                unit.move(5f, 0, 0, 0);
                unit.rotate(5f, targetAngle, 12f);
            }
        }
    }

    @Override
    void draw(){
        float out = dashTime / 15f;
        float z = Draw.z();
        float ang = Angles.angle(lastX, lastY, newX, newY);
        float w = out * 15f;
        Draw.z(Layer.flyingUnit + 1f);
        Draw.color(FlamePal.empathyAdd);
        Draw.blend(Blending.additive);

        Drawf.tri(newX, newY, w, w * 1.5f + 5f, ang);
        GraphicUtils.tri(newX, newY, lastX, lastY, w, ang);

        Draw.color();
        Draw.blend();
        Draw.z(z);
    }

    @Override
    boolean shouldDraw(){
        return dashTime > 0;
    }
}
