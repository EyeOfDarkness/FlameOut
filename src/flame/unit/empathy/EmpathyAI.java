package flame.unit.empathy;

import arc.audio.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.audio.*;
import mindustry.ai.types.*;
import mindustry.gen.*;

class EmpathyAI{
    protected float time = 0f;
    protected float aiUsages = 0f;

    protected EmpathyUnit unit;
    protected boolean attack = false, quickSwap = false;
    protected Seq<PassiveSoundLoop> sounds;

    protected float autoParryTime = 0f;
    static boolean parryFound = false;

    EmpathyAI set(EmpathyUnit u){
        unit = u;
        return this;
    }

    //used by other ais to reset attacks or movements
    void reset(){
        //
    }

    void init(){
    }

    void update(){
    }

    void updatePassive(){
    }
    @SuppressWarnings("unchecked")
    void updateAutoParry(){
        if(autoParryTime <= 0f){
            parryFound = false;
            Utils.scanCone((QuadTree<Bullet>)Groups.bullet.tree(), unit.x, unit.y, unit.rotation, 25f, 60f, b -> {
                if(b.team != unit.team){
                    parryFound = true;
                }
            });
            Utils.scanCone((QuadTree<Unit>)Groups.unit.tree(), unit.x, unit.y, unit.rotation, 25f, 60f, u -> {
                if(u.team != unit.team && (u instanceof TimedKillc || u.controller() instanceof MissileAI)){
                    parryFound = true;
                }
            });

            if(parryFound){
                autoParryTime = 40f;
                unit.parry();
            }
        }else{
            autoParryTime -= Time.delta;
        }
    }

    boolean bulletHellOverride(){
        return true;
    }
    boolean teleSwapCompatible(){
        return false;
    }
    boolean canTeleport(){
        return true;
    }

    float effectiveDistance(){
        return 400f;
    }

    boolean shouldDraw(){
        return false;
    }
    boolean overrideDraw(){
        return false;
    }
    boolean canKnockback(){
        return true;
    }
    void draw(){
        //
    }

    boolean updateAttackAI(){
        return true;
    }
    boolean updateMovementAI(){
        return true;
    }
    boolean canTrail(){
        return true;
    }

    float weight(){
        return 10f;
    }

    void end(){
    }
    void endOnce(){}

    void updateSounds(){
        if(sounds != null){
            for(PassiveSoundLoop s : sounds){
                s.update();
            }
        }
    }
    int addSound(Sound sound){
        if(sounds == null) sounds = new Seq<>();
        sounds.add(new PassiveSoundLoop(sound));
        return sounds.size - 1;
    }
    void addSoundConstant(Sound sound){
        if(sounds == null) sounds = new Seq<>();
        sounds.add(new PassiveSoundLoop(sound, true, false));
    }
    void addSoundDoppler(Sound sound){
        if(sounds == null) sounds = new Seq<>();
        sounds.add(new PassiveSoundLoop(sound, true, true));
    }

    void stopSounds(){
        if(sounds != null){
            for(PassiveSoundLoop s : sounds){
                s.stop();
            }
        }
    }

    Teamc getTarget(){
        return null;
    }
    void setTarget(Teamc target){

    }

    void retarget(){

    }
}
