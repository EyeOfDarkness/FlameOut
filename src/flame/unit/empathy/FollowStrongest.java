package flame.unit.empathy;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.Wall.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.PowerTurret.*;
import mindustry.world.blocks.defense.turrets.Turret.*;
import mindustry.world.blocks.storage.CoreBlock.*;

public class FollowStrongest extends EmpathyAI{
    Teamc strongest;
    float scanTime = 0f;
    float forgetTime = 0f;
    float coreTime = 0f;
    boolean targetedCore = false;

    @Override
    void update(){
        updateTargeting();
        updateMovement();
    }

    @Override
    void retarget(){
        Teamc t = null;
        float scr = 0f;
        for(TeamData data : Vars.state.teams.present){
            if(data.team != unit.team){
                for(Unit u : data.units){
                    if(u.dead || u instanceof TimedKillc) continue;
                    //float s = (u.health / 100f + FlameOutSFX.inst.getUnitDps(u.type)) - unit.dst(u.x, u.y) / 10000f;
                    float s = (u.maxHealth / 100f + FlameOutSFX.inst.getUnitDps(u.type)) - unit.dst(u.x, u.y) / 10000f;
                    //if(u instanceof TimedKillc) s /= 1000;
                    //TODO remove
                    //if(u instanceof EmpathyUnit) s += 9999999;
                    if(t == null || s > scr){
                        t = u;
                        scr = s;
                    }
                }
                for(Building build : data.buildings){
                    if(!build.isAdded()) continue;
                    //float s = (build.health / 60f);
                    float s = (build.maxHealth / 60f);
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
                    }
                    if(build instanceof WallBuild || build instanceof CoreBuild){
                        s /= 20;
                    }

                    if(t == null || s > scr){
                        t = build;
                        scr = s;
                    }
                }
            }
        }
        if(t != null){
            //strongest = t;
            //forgetTime = 10f * 60f;
            if((t instanceof CoreBuild || (t instanceof Unit u && u.spawnedByCore)) && coreTime <= (2f * 60f)){
                targetedCore = true;
            }else{
                strongest = t;
                forgetTime = 10f * 60f;
                coreTime = 0f;
                targetedCore = false;
            }
        }
    }

    void updateTargeting(){
        if(targetedCore) coreTime += Time.delta;
        if(strongest != null && (!strongest.isAdded() || strongest.team() == unit.team)){
            strongest = null;
        }
        if(scanTime > 0f) scanTime -= Time.delta;
        if(scanTime <= 0f && (strongest == null || forgetTime <= 0f)){
            retarget();
            scanTime = 8f;
        }
        if(forgetTime > 0) forgetTime -= Time.delta;
    }

    void updateMovement(){
        if(strongest != null){
            float size = strongest instanceof Sized s ? s.hitSize() / 2f : 0f;

            //float dst = 200f + size;
            float dst = unit.activeAttack.effectiveDistance() + size;

            /*
            Vec2 vec = Tmp.v1;

            vec.set(nearestCore).sub(unit);

            float length = Mathf.clamp((unit.dst(nearestCore) - 220f) / 100f, -1f, 1f);

            vec.setLength(unit.speed());
            vec.scl(length);

            if(vec.isNaN() || vec.isInfinite() || vec.isZero()) return;

            unit.movePref(vec);
            unit.lookAt(unit.angleTo(nearestCore));
            */
            Vec2 vec = Tmp.v1.set(strongest).sub(unit);
            Vec2 vel = unit.trueVelocity();
            //float dstt = unit.dst(strongest);
            float dstt = Mathf.dst(unit.x + vel.x, unit.y + vel.y, strongest.x(), strongest.y());
            float len = (dstt - dst) / 25f;
            //vec.setLength(Math.min(Math.abs(len), dstt > dst ? 10f : 1f));
            vec.setLength(Math.min(Math.abs(len), 10f));
            vec.scl(len > 0 ? 1 : -1);
            if(vec.isNaN() || vec.isInfinite()) return;

            //int type = (dstt > dst || Mathf.dst(unit.x + vec.x, unit.y + vec.y, strongest.x(), strongest.y()) > dstt) ? 0 : 2;
            int type = (dstt > dst) ? 0 : 2;

            unit.move(1f, type, vec.x, vec.y);
            unit.rotate(1f, unit.angleTo(strongest), 9f);
        }
    }

    @Override
    Teamc getTarget(){
        return strongest;
    }

    @Override
    void setTarget(Teamc target){
        strongest = target;
    }
}
