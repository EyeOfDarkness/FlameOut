package flame.unit.empathy;

import mindustry.gen.*;

public class AttackAI extends EmpathyAI{
    AttackAI(){
        attack = true;
    }

    @Override
    Teamc getTarget(){
        return unit.getTarget();
    }
}
