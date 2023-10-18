package flame.unit.empathy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import flame.entities.*;
import flame.graphics.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class CopyAttack extends AttackAI{
    Seq<CopyUnit> units = new Seq<>();
    IntMap<CopyUnit> unitMap = new IntMap<>();
    float switchTime = 0f;

    static IntSet incompatible = new IntSet();

    @Override
    void draw(){
        Draw.draw(Layer.flyingUnit + 0.1f, () -> {
            Draw.flush();
            FixedSpriteBatch.beginSwap();
            //Blending.additive.apply();
            FixedSpriteBatch.batch.setFixedBlending(Blending.additive);
            for(CopyUnit cu : units){
                float fade = Mathf.clamp(cu.time2 / 60f) * Mathf.clamp(cu.time / 60f);
                FixedSpriteBatch.batch.setFixedAlpha(fade);
                try{
                    for(Unit u : cu.units){
                        u.draw();
                    }
                }catch(Exception e){
                    if(!cu.units.isEmpty()) incompatible.add(cu.units.get(0).type.id);
                    Log.err(e);
                }
            }
            Draw.flush();
            FixedSpriteBatch.batch.setFixedBlending(Blending.normal);
            //Blending.normal.apply();
            FixedSpriteBatch.endSwap();
        });
    }

    @Override
    boolean shouldDraw(){
        return !units.isEmpty();
    }

    @Override
    void updatePassive(){
        if(units.isEmpty()) return;
        units.removeAll(cu -> {
            float delta = Mathf.clamp(cu.time2 / 60f) * Mathf.clamp(cu.time / 60f);
            float ltd = Time.delta;
            Time.delta = delta * ltd * 1.5f;

            //updateUnit(cu.u);
            cu.units.removeAll(u2 -> {
                try{
                    updateUnit(u2);
                }catch(Exception e){
                    incompatible.add(u2.type.id);
                    Log.err(e);
                    return true;
                }
                return false;
            });

            Time.delta = ltd;

            cu.time2 += Time.delta;
            cu.time -= Time.delta;
            if(cu.time <= 0f || cu.units.size <= 0){
                unitMap.remove(cu.id);
                return true;
            }
            return false;
        });
    }

    void updateUnit(Unit u) throws Exception{
        //boolean aggro = false;
        if(u.controller() instanceof AIController ac){
            //aggro = ReflectUtils.findField(AIController.class, "target").get(ac) == unit;
            if(unit.getTarget() != null){
                ReflectUtils.findField(AIController.class, "target").set(ac, unit.getTarget());
                
                if(!u.within(unit.getTarget(), u.type.range * 0.81f)){
                    ac.moveTo(unit.getTarget(), u.type.range * 0.81f);
                    u.lookAt(unit.getTarget());
                }
            }
            /*
            if(){
                ac.moveTo(getTarget(), u.type.range * 0.8f);
            }
            */
        }
        u.update();
    }

    @Override
    void update(){
        if(unit.getTarget() instanceof Unit u && u.hasWeapons() && u.checkTarget(u.isFlying(), u.isGrounded()) && !incompatible.contains(u.type.id) && !(unit.getTarget() instanceof EmpathyUnit)){
            if(!unitMap.containsKey(u.id)){
                MockGroup.swap(() -> createUnit(u), e -> {
                    //Log.info(e);
                    if(e instanceof Unit u2 && !incompatible.contains(u2.type.id)){
                        CopyUnit cu = unitMap.get(u.id, () -> {
                            CopyUnit nu = new CopyUnit();
                            nu.id = u.id;
                            units.add(nu);
                            return nu;
                        });
                        cu.units.add(u2);
                        cu.time = 30f * 60f;
                    }
                });

                //switchTime = 0f;
                unit.randAI(true, unit.health < 50);
            }else{
                CopyUnit cu = unitMap.get(u.id);
                cu.time = Math.max(10f * 60f, cu.time);

                //switchTime = 0f;
                unit.randAI(true, unit.health < 50);
            }
        }else{
            //unit.resetAI(attack);
            //unit.randAI(true);
            if(switchTime > 60f){
                unit.randAI(true, unit.health < 50);
                switchTime = 0f;
                return;
            }
            switchTime += Time.delta;
        }
    }

    void createUnit(Unit origin){
        Unit u = origin.type.create(unit.team);
        u.x = unit.x;
        u.y = unit.y;
        u.rotation = unit.rotation;
        u.add();
        //Log.info(u);
    }

    @Override
    float weight(){
        //boolean valid = getTarget() instanceof Unit fly && u.hasWeapons() && fly.checkTarget(fly.isFlying(), fly.isGrounded());
        return unit.getTarget() instanceof Unit u && u.hasWeapons() && u.checkTarget(u.isFlying(), u.isGrounded()) && !incompatible.contains(u.type.id) && !(unit.getTarget() instanceof EmpathyUnit) ? 30f : -1f;
    }

    static class CopyUnit{
        Seq<Unit> units = new Seq<>();
        int id;
        float time, time2;
    }
}
