package flame.unit;

import mindustry.entities.units.*;
import mindustry.gen.*;

public class NullAI implements UnitController{
    Unit u;

    @Override
    public void unit(Unit unit){
        u = unit;
    }

    @Override
    public Unit unit(){
        return u;
    }
}
