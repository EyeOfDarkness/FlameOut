package flame;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import flame.bullets.*;
import flame.effects.*;
import flame.entities.*;
import flame.graphics.*;
import flame.unit.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;

public class FlameOut extends Mod{
    public static FragmentationBatch fragBatch;
    public static CutBatch cutBatch;
    public static VaporizeBatch vaporBatch;

    public FlameOut(){
        ApathyAoEBulletType.loadStatic();
        MockGroup.load();
        Severation.init();
        new FlameOutSFX();
        Events.on(FileTreeInitEvent.class, e -> Core.app.post(() -> {
            FlameSounds.load();
            if(!Vars.headless){
                FlameShaders.load();
                ImpactBatch.init();
                FixedSpriteBatch.init();
                fragBatch = new FragmentationBatch();
                cutBatch = new CutBatch();
                vaporBatch = new VaporizeBatch();
                //fragBatch.load();
                FlameOutSFX.inst.loadHeadless();
            }
        }));
    }

    @Override
    public void loadContent(){
        FlameBullets.load();
        FlameUnitTypes.load();
    }

}