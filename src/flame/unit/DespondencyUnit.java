package flame.unit;

import arc.util.*;
import flame.unit.empathy.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.gen.*;
import mindustry.type.*;

public class DespondencyUnit extends LegsUnit{
    float trueHealth, trueMaxHealth;
    float invFrames;
    float lastDamage = 0f;

    @Override
    public void update(){
        updateValues();
        super.update();
    }

    @Override
    public void rawDamage(float amount){
        if(EmpathyDamage.isNaNInfinite(amount)) return;
        if(invFrames <= 0f || amount > lastDamage){
            float lam = amount;

            amount -= lastDamage;
            lastDamage = lam;
            amount = Math.min(amount, type.health / 220f);
            trueHealth -= amount;
            super.rawDamage(amount);
            trueHealth = health;
            invFrames = 15f;
        }
    }

    @Override
    public boolean isGrounded(){
        return true;
    }

    void updateValues(){
        if(!EmpathyDamage.isNaNInfinite(health)) trueHealth = Math.max(trueHealth, health);

        health = trueHealth;
        maxHealth = trueMaxHealth;
        if(trueHealth > 0){
            elevation = 1f;
            dead = false;
        }else{
            elevation = 0f;
            dead = true;
        }
        if(invFrames > 0){
            invFrames -= Time.delta;
            if(invFrames <= 0f){
                lastDamage = 0f;
            }
        }
    }

    @Override
    public void setType(UnitType type){
        super.setType(type);
        trueMaxHealth = type.health;
    }

    @Override
    public SolidPred solidity(){
        return null;
    }

    @Override
    public boolean serialize(){
        return false;
    }

    @Override
    public void add(){
        if(!added){
            trueHealth = type.health;
            EmpathyDamage.exclude(this);
        }
        super.add();
    }

    @Override
    public void remove(){
        if(trueHealth > 0 && EmpathyDamage.containsExclude(id)) return;
        if(added){
            boolean valid = EmpathyDamage.removeExclude(this);
            if(valid){
                super.remove();
            }else{
                trueHealth = trueMaxHealth = type.health;
            }
        }
    }
}
