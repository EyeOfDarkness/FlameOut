package flame.unit;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.unit.empathy.*;
import flame.unit.weapons.EndDespondencyWeapon.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Wall.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.PowerTurret.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.meta.*;

import java.util.*;

public class DespondencyAI extends AIController{
    Teamc moveTarget;
    HierarchyArray<Teamc> targets = new HierarchyArray<>(8);
    boolean death;
    float deathTime = 0f;
    float reloadTime = 0f;
    float distractionTime = 0f;
    boolean altTarget = false;

    @Override
    public void updateUnit(){
        updateTargeting();
        updateMovement();
    }

    @Override
    public void updateMovement(){
        //Teamc move = ((moveTarget != null && !death && unit.within(moveTarget, unit.type.maxRange * 2f + (moveTarget instanceof Sized s ? s.hitSize() / 2f : 0f))) || target == null) ? moveTarget : target;
        Teamc move = ((moveTarget != null && !death && (unit.within(moveTarget, unit.type.maxRange + (moveTarget instanceof Sized s ? s.hitSize() / 2f : 0f)) || altTarget)) || target == null) ? moveTarget : target;
        float near = 720f;

        if(move != null && deathTime < 5f * 60f){
            vec.set(move).sub(unit).limit(unit.speed() * (death ? 0.5f : 1f));

            if(vec.isNaN() || vec.isInfinite() || vec.isZero()) return;

            unit.lookAt(move);

            if(!move.within(unit, near)){
                unit.movePref(vec);
            }else if(move.within(unit, near / 2f)){
                unit.move(-vec.x, -vec.y);
            }
            
        }else if(move != null){
            unit.lookAt(move);
        }
    }

    public Teamc mainTarget(){
        return death ? target : null;
    }

    @Override
    public void updateWeapons(){
        DespondencyUnitType type = (DespondencyUnitType)unit.type;
        float rotation = unit.rotation - 90;
        WeaponMount main = unit.mounts[type.mainWeaponIdx];
        DespondencyMount dm = (DespondencyMount)main;

        unit.isShooting = false;
        if(reloadTime > 0) reloadTime -= Time.delta;

        if(!death || (target == null && dm.stage <= 2) || reloadTime > 0){
            main.shoot = false;
            main.target = null;

            deathTime = 0f;
            death = false;
            int idx = 0;
            if(targets.size > 0){
                noTargetTime = 0f;
                if(moveTarget != null && !altTarget){
                    boolean contains = false;
                    for(Teamc tar : targets){
                        if(!moveTarget.within(tar, 280f)){
                            contains = true;
                            break;
                        }
                    }
                    if(contains){
                        distractionTime += Time.delta;
                        if(distractionTime > 2f * 60){
                            moveTarget = targets.get(0);
                            altTarget = true;
                        }
                    }else if(distractionTime > 0){
                        distractionTime -= Time.delta;
                    }
                }
                
                for(WeaponMount mount : unit.mounts){
                    Weapon weapon = mount.weapon;
                    float wrange = weapon.range();

                    if(!weapon.controllable || weapon.autoTarget || mount == main || weapon.noAttack) continue;

                    float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y), mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

                    //Teamc t = targets.get(idx % targets.size);
                    //idx++;
                    Teamc tar = null;
                    int count = 0;
                    if(!altTarget || moveTarget == null){
                        while(count < targets.size){
                            Teamc t = targets.get(Mathf.mod(count + idx, targets.size));
                            if(unit.within(t, wrange + (t instanceof Sized si ? si.hitSize() / 2f : 0f))){
                                tar = t;
                                idx++;
                                break;
                            }
                            count++;
                        }
                        if(tar == null){
                            tar = targets.get(Mathf.mod(idx, targets.size));
                            idx++;
                        }
                    }else{
                        tar = moveTarget;
                    }

                    mount.target = tar;

                    boolean shoot = false;

                    if(tar != null){
                        //mount.rotate = true;
                        //shoot = mount.target.within(mountX, mountY, wrange + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && shouldShoot();
                        shoot = tar.within(mountX, mountY, wrange + (tar instanceof Sized s ? s.hitSize() / 2f : 0f)) && shouldShoot();

                        Vec2 to = weapon.bullet.speed > 0.001f ? Predict.intercept(unit, mount.target, weapon.bullet.speed) : Tmp.v1.set(tar.x(), tar.y());
                        mount.aimX = to.x;
                        mount.aimY = to.y;
                    }

                    mount.shoot = mount.rotate = shoot;

                    unit.isShooting |= mount.shoot;

                    if(shoot){
                        unit.aimX = mount.aimX;
                        unit.aimY = mount.aimY;
                    }
                }
            }else{
                noTargetTime += Time.delta;
                distractionTime = 0f;

                for(WeaponMount mount : unit.mounts){
                    Weapon weapon = mount.weapon;

                    if(!weapon.controllable || weapon.autoTarget || mount == main || weapon.noAttack) continue;
                    mount.shoot = false;
                    mount.rotate = false;

                    if(noTargetTime >= rotateBackTimer){
                        float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y), mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

                        mount.rotate = true;
                        Tmp.v1.trns(unit.rotation + mount.weapon.baseRotation, 5f);
                        mount.aimX = mountX + Tmp.v1.x;
                        mount.aimY = mountY + Tmp.v1.y;
                    }
                }
            }
        }else{
            for(WeaponMount mount : unit.mounts){
                Weapon weapon = mount.weapon;
                float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y), mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

                mount.shoot = false;
                mount.rotate = true;

                Tmp.v1.trns(unit.rotation + mount.weapon.baseRotation, 5f);
                mount.aimX = mountX + Tmp.v1.x;
                mount.aimY = mountY + Tmp.v1.y;
            }

            main.shoot = true;
            if(target != null) main.target = target;

            deathTime += Time.delta;

            if(deathTime >= 30f){
                dm.activeTime += Time.delta;
            }

            /*
            if(dm.finished){
                main.shoot = false;
                deathTime = 0f;
                dm.finished = false;
                reloadTime = 5f * 60f;
                death = false;
            }
             */
        }
    }

    public void endDeath(){
        deathTime = 0f;
        reloadTime = 10f * 60f;
        death = false;
    }
    public void endDeathFailed(){
        deathTime = 0f;
        reloadTime = 2f * 60f;
        death = false;
    }

    @Override
    public void updateTargeting(){
        if(target != null && (target instanceof Healthc h && !h.isValid())){
            target = null;
            timer.reset(0, 0f);
        }
        if(moveTarget != null && !((Healthc)moveTarget).isValid()){
            moveTarget = null;
            distractionTime = 0f;
            altTarget = false;
            timer.reset(0, 0f);
        }

        Iterator<Teamc> iter = targets.iterator();
        while(iter.hasNext()){
            Teamc t = iter.next();
            if(!((Healthc)t).isValid()){
                iter.remove();
            }
        }

        if(target instanceof Unit u && !death && !EmpathyDamage.containsExclude(u.id) && unit.within(u, unit.range() * 0.8f + u.hitSize / 2.5f) && reloadTime <= 0f){
            float scr = (FlameOutSFX.inst.getUnitDps(u.type) + u.maxHealth * u.healthMultiplier);
            if(EmpathyDamage.isNaNInfinite(scr) || scr > 1000000f || (u.hitSize > 100f && scr > 200000f)){
                death = true;
            }
        }

        if(retarget() && !death){
            double score = 0, score2 = 0;
            Teamc t = null;
            Teamc mt = null;
            targets.clear();
            for(TeamData data : Vars.state.teams.present){
                if(data.team != unit.team && data.team != Team.derelict){
                    for(Unit u : data.units){
                        if(u instanceof TimedKillc) continue;
                        double s = (((double)FlameOutSFX.inst.getUnitDps(u.type)) + (double)(u.maxHealth * u.healthMultiplier)) - (u.dst(unit) / 1000f);
                        if(t == null || s > score){
                            t = u;
                            score = s;
                        }
                        if((mt == null || s > score2) && (u.type.speed < 2.2f) && (s > 200000f || (u.hitSize > 100f))){
                            mt = u;
                            score2 = s;
                        }

                        //doesnt matter as much
                        targets.add(u, (float)s);
                    }
                    for(Building build : data.buildings){
                        if(build.tile.build != build) continue;
                        float s = build.maxHealth;
                        Block block = build.block;

                        if(build instanceof TurretBuild tb){
                            float max = 0f;
                            float reloadSpeed = ((Turret)tb.block).reload;
                            if(!(build instanceof PowerTurretBuild)){
                                for(AmmoEntry ammo : tb.ammo){
                                    max = (Math.max(max, FlameOutSFX.inst.getBulletDps(ammo.type())) / reloadSpeed) * ammo.amount;
                                }
                            }else{
                                max = Math.max(max, FlameOutSFX.inst.getBulletDps(((PowerTurret)tb.block).shootType));
                            }
                            max *= build.efficiency;

                            s += max;
                        }else if(block.flags.contains(BlockFlag.generator) || block.flags.contains(BlockFlag.factory)){
                            s *= 2f;
                        }
                        s -= unit.dst(build) / 1000f;

                        if(build instanceof WallBuild){
                            s /= 120;
                        }
                        if(mt == null || s > score2){
                            mt = build;
                            score2 = s;
                        }

                        targets.add(build, s);
                    }
                }
            }

            target = t;
            if(!altTarget) moveTarget = mt;
            
            //Log.info(t + ":" + score);
        }
        updateWeapons();
    }

    @Override
    public boolean retarget(){
        return timer.get(timerTarget, (target == null || targets.size <= 0) ? 40 : 90);
    }

    @Override
    public boolean isLogicControllable(){
        return false;
    }
}
