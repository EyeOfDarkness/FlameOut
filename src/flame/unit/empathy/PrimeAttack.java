package flame.unit.empathy;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.effects.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;

public class PrimeAttack extends AttackAI{
    boolean attacking = false, queue = false, ending = false;
    int strikes = 0;
    float waitTime = 0f, idleTime = 0f;
    float turnTime = 0f, lastRotation = 0f;

    float offsetX, offsetY;
    float strikeDelay;
    float endDelay;

    static boolean collided = false;
    //static float maxMaxHealth = 0f;
    static Seq<Building> tmpBuildings = new Seq<>();

    @Override
    void reset(){
        attacking = true;
        strikes = 0;
        waitTime = 0f;
    }

    @Override
    float weight(){
        return unit.useLethal() ? 1000f + unit.extraLethalScore() : 2f;
    }

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    void update(){
        if(!attacking){
            idleTime += Time.delta;
            if(unit.getTarget() != null){
                //float dst1 = unit.dst2()
                boolean dashed = false;
                if(unit.getTarget() instanceof Hitboxc h){
                    float dst1 = unit.dst(h.lastX(), h.lastY());
                    float dst2 = unit.dst(h);
                    if(dst2 < 45f + h.hitSize() / 2f && (dst1 - dst2) > 25f){
                        dashed = true;
                    }
                }

                if(idleTime >= 2f * 60f || dashed){
                    idleTime = 0f;
                    strikes = 0;
                    waitTime = 0f;
                    attacking = true;
                }
            }
        }

        if(queue){
            if(strikeDelay <= 0f){
                unit.parry(false);
                hit();
                turnTime = 15f;
                lastRotation = unit.rotation;
                queue = false;

                FlameFX.empathyPrimeStrike.at(unit.x, unit.y, unit.rotation, unit);
                FlameSounds.empathyDash.at(unit.x, unit.y, Mathf.random(0.9f, 1.1f));
                
                if(ending){
                    endDelay = 15f;
                    ending = false;
                }

                Teamc tar = unit.getTarget();
                if(tar != null && (tar instanceof Healthc c && !c.isValid())){
                    //attacking = false;
                    unit.retarget();
                }
            }else{
                Teamc tar = unit.getTarget();
                if(tar != null){
                    Vec2 mov = Tmp.v1.set(offsetX, offsetY).add(tar.x(), tar.y()).sub(unit.x, unit.y).limit(48f);
                    unit.move(5f, 2, mov.x, mov.y);
                }

                strikeDelay -= Time.delta;
                waitTime -= Time.delta;
                return;
            }
        }
        if(endDelay > 0){
            endDelay -= Time.delta;
            
            if(turnTime > 0){
                float fin = (turnTime / 15f);
                float angle = lastRotation + 360f * (1 - (fin * fin));
                unit.rotate(4f, angle, 180f);
            }
            turnTime -= Time.delta;
            
            if(endDelay <= 0f){
                strikeDelay = 0f;
                attacking = false;
                strikes = 0;
                turnTime = 0;
                waitTime = 0f;

                unit.randAI(true, unit.health < 50);
            }
            return;
        }
        if(attacking){
            if(waitTime <= 0f){
                Teamc tar = unit.getTarget();
                Vec2 dash = Tmp.v1.rnd(200f);
                float dashAngle = dash.angle();
                if(tar != null){
                    //dash.set(tar.x(), tar.y()).sub(unit.x, unit.y).limit(unit.dst(tar) - ((tar instanceof Sized s ? s.hitSize() / 2f : 1f) + 30f));
                    //dash.rnd((tar instanceof Sized s ? s.hitSize() / 2 : 0f) + 25f).add(tar).sub(unit);

                    //Rect r2 = Tmp.r2.set(0, 0, Vars.world.width() * Vars.tilesize, Vars.world.height() * Vars.tilesize).grow(-400f * 2);
                    
                    float size = 400f;
                    Rect r2 = Tmp.r2.set(0, 0, Vars.world.width() * Vars.tilesize, Vars.world.height() * Vars.tilesize);
                    r2.x += size;
                    r2.y += size;
                    r2.width -= size * 2;
                    r2.height -= size * 2;
                    
                    if(r2.contains(tar.x(), tar.y())){
                        //dash.trns(tar.angleTo(unit) + Mathf.range(16f), (tar instanceof Sized s ? s.hitSize() / 2 : 0f) + 25f).add(tar).sub(unit);
                        //unit.health <= 50
                        if(strikes <= 0 && unit.health <= 50f && tar instanceof Unit u){
                            dash.trns(u.rotation + 180 + Mathf.range(16f), u.hitSize / 2f + 15f).add(tar).sub(unit);
                        }else{
                            dash.trns(tar.angleTo(unit) + Mathf.range(16f), (tar instanceof Sized s ? s.hitSize() / 2 : 0f) + 15f).add(tar).sub(unit);
                        }
                    }else{
                        float cx = r2.x + r2.width / 2;
                        float cy = r2.y + r2.height / 2;

                        dash.set(tar.x(), tar.y()).sub(cx, cy).rotate(Mathf.range(16f)).setLength((tar instanceof Sized s ? s.hitSize() / 2 : 0f) + 25f).add(tar).sub(unit);
                    }
                    //dashAngle = unit.angleTo(tar);
                    dashAngle = Angles.angle(unit.x + dash.x, unit.y + dash.y, tar.getX(), tar.getY());
                    offsetX = (unit.x + dash.x) - tar.x();
                    offsetY = (unit.y + dash.y) - tar.y();
                }else{
                    offsetX = 0f;
                    offsetY = 0f;
                }
                if(dash.len2() > 40f * 40f){
                    FlameFX.empathyDashShockwave.at(unit.x, unit.y, dash.angle());
                    Vec2 np = Intersector.nearestSegmentPoint(unit.x, unit.y, unit.x + dash.x, unit.y + dash.y, Core.camera.position.x, Core.camera.position.y, Tmp.v2);
                    FlameSounds.empathyDash2.at(np.x, np.y, 1f, 1.25f);
                }

                unit.move(5f, 2, dash.x, dash.y);
                unit.rotate(5f, dashAngle, 360f);
                strikes++;
                queue = true;
                waitTime = 27f;

                strikeDelay = unit.health > 50 ? 15f : 0f;
            }else{
                if(turnTime > 0){
                    float fin = (turnTime / 15f);
                    float angle = lastRotation + 360f * (1 - (fin * fin));
                    unit.rotate(4f, angle, 180f);
                }
                turnTime -= Time.delta;
            }
            
            /*
            if(ending && turnTime <= 0f){
                strikeDelay = 0f;
                attacking = false;
                strikes = 0;
                turnTime = 0;
                waitTime = 0f;

                ending = false;
                unit.randAI(true, false);
                return;
            }
            */
            
            waitTime -= Time.delta;
            if(strikes >= 4 || (quickSwap && strikes >= 1)){
                //strikeDelay = 0f;
                //queue = false;
                //attacking = false;
                //strikes = 0;
                //turnTime = 0;
                ending = true;
                //endDelay = 15f;
                //unit.randAI(true, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    void hit(){
        collided = false;
        //maxMaxHealth = 0f;
        float angle = unit.getTarget() != null ? unit.angleTo(unit.getTarget()) : unit.rotation;
        Utils.scanCone((QuadTree<Unit>)Groups.unit.tree(), unit.x, unit.y, angle, 63f, 45f, true, e -> {
            if(e.team != unit.team){
                //maxMaxHealth = Math.max(maxMaxHealth, e.maxHealth);
                Vec2 v = Tmp.v1.set(e.x, e.y).sub(unit.x, unit.y).nor().scl(12f);
                if(v.isNaN() || v.isInfinite()){
                    v.trns(angle, 12f);
                }
                EmpathyDamage.damageUnitKnockback(e, Math.max(10000f, e.maxHealth / 7f), v.x, v.y, () -> {
                    //Fx.impactReactorExplosion.at(e.x, e.y);
                    SpecialDeathEffects.get(e.type).deathUnit(e, unit.x, unit.y, Angles.angle(unit.x, unit.y, e.x, e.y));
                    
                    if(e.maxHealth >= 1000000f){
                        FlameOutSFX.inst.impactFrames(null, unit.x, unit.y, unit.rotation, 15f, true);
                    }
                });
                collided = true;
            }
        });
        tmpBuildings.clear();
        for(TeamData data : Vars.state.teams.present){
            if(data.team != unit.team && data.buildingTree != null){
                Utils.scanCone(data.buildingTree, unit.x, unit.y, angle, 63f, 45f, true, e -> tmpBuildings.add(e));
            }
        }
        for(Building e : tmpBuildings){
            //maxMaxHealth = Math.max(maxMaxHealth, e.maxHealth);
            EmpathyDamage.damageBuilding(e, Math.max(25000f, e.maxHealth / 7f), true, () -> {
                //Fx.impactReactorExplosion.at(e.x, e.y);
                SpecialDeathEffects.get(e.block).deathBuilding(e, unit.x, unit.y, Angles.angle(unit.x, unit.y, e.x, e.y));
                
                if(e.maxHealth >= 1000000f){
                    FlameOutSFX.inst.impactFrames(null, unit.x, unit.y, unit.rotation, 15f, true);
                }
            });
            collided = true;
        }
        tmpBuildings.clear();
        if(collided){
            for(TeamData data : Vars.state.teams.present){
                if(data.team != unit.team){
                    data.plans.clear();
                }
            }
            Tmp.v1.trns(unit.rotation, 12f).add(unit.x, unit.y);
            FlameFX.empathyPrimeHit.at(Tmp.v1.x, Tmp.v1.y, unit.rotation);
            Sounds.largeCannon.at(Tmp.v1.x, Tmp.v1.y);
            Tmp.v1.sub(unit.x, unit.y).scl(2f).add(unit.x, unit.y);
            FlameFX.empathyPrimeShockwave.at(Tmp.v1.x, Tmp.v1.y, unit.rotation);

            /*
            if(maxMaxHealth > 1000000f){
                FlameOutSFX.inst.impactFrames(null, unit.x, unit.y, unit.rotation, 35f, true);
            }
            */
        }
    }

    @Override
    boolean updateMovementAI(){
        return !attacking;
    }
}
