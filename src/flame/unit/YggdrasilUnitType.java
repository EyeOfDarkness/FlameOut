package flame.unit;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.type.*;

public class YggdrasilUnitType extends UnitType{
    public TextureRegion[] legRegions, legBaseRegions, tentacleRegions;
    public TextureRegion tentacleEndRegion;

    public YggdrasilUnitType(String name){
        super(name);
        constructor = YggdrasilUnit::new;
        aiController = YggdrasilAI::new;
        //controller
        controller = unit -> new YggdrasilAI();
    }

    @Override
    public void init(){
        super.init();
        allowLegStep = true;
        range = maxRange = 720f;

        immunities.addAll(Vars.content.statusEffects());
        immunities.remove(StatusEffects.burning);
        immunities.remove(StatusEffects.melting);
    }

    @Override
    public void load(){
        super.load();
        legRegions = new TextureRegion[3];
        legBaseRegions = new TextureRegion[3];
        tentacleRegions = new TextureRegion[3];

        for(int i = 0; i < 3; i++){
            legRegions[i] = Core.atlas.find(name + "-leg-" + i);
            legBaseRegions[i] = Core.atlas.find(name + "-leg-base-" + i);
            tentacleRegions[i] = Core.atlas.find(name + "-tentacle-" + i);
        }
        tentacleEndRegion = Core.atlas.find(name + "-tentacle-end");
    }
}
