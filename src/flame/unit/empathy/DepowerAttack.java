package flame.unit.empathy;

import arc.util.*;
import flame.effects.*;
import mindustry.gen.*;

public class DepowerAttack extends AttackAI{
    float reload = 0f;

    @Override
    void updatePassive(){
        if(reload > 0) reload -= Time.delta;
    }

    @Override
    float weight(){
        return reload <= 0f && EmpathyDamage.excludeSeq.size > 3 ? 500000f : -1f;
    }

    @Override
    void update(){
        if(reload <= 0f){
            for(Unit u : EmpathyDamage.excludeSeq){
                FlameFX.empathyDepowered.at(u.x, u.y, u.hitSize / 2f);
            }
            EmpathyDamage.clearExclude();
            FlameFX.endFlash.at(unit.x, unit.y, 1.5f);

            reload = 2.5f * 60f;
            unit.randAI(true, true);
        }
    }
}
