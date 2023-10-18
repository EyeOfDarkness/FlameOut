package flame.unit;

import arc.struct.*;
import mindustry.*;
import mindustry.type.*;

public class ApathyUnitType extends UnitType{
    public Seq<ShiftHandler> handlers = new Seq<>();

    public ApathyUnitType(String name){
        super(name);

        health = 12000000;
        armor = 60;
        speed = 0.45f;

        outlines = false;

        constructor = ApathyIUnit::new;
    }

    @Override
    public void init(){
        super.init();
        for(StatusEffect s : Vars.content.statusEffects()){
            immunities.add(s);
        }
    }

    @Override
    public void load(){
        super.load();
        for(ShiftHandler h : handlers){
            h.load();
        }
    }
}
