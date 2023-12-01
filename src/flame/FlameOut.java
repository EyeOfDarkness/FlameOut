package flame;

import arc.*;
import arc.input.*;
import arc.math.*;
import flame.bullets.*;
import flame.effects.*;
import flame.entities.*;
import flame.graphics.*;
import flame.special.*;
import flame.unit.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;

public class FlameOut extends Mod{
    public static FragmentationBatch fragBatch;
    public static CutBatch cutBatch;
    public static VaporizeBatch vaporBatch;

    public final boolean test = false;

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
                SpecialMain.load();
            }
        }));
        Events.on(ClientLoadEvent.class, e -> {
            SpecialMain.loadClient();
        });
        Events.on(WorldLoadEvent.class, e -> EmpathyDamage.worldLoad());
        if(test){
            Events.run(Trigger.update, () -> {
                Unit p = Vars.player.unit();
                if(Core.input.keyTap(KeyCode.x)){
                    float ang = Angles.mouseAngle(p.x, p.y);
                    FlameBullets.test.create(p, p.x, p.y, ang);
                }
            });
        }
    }

    @Override
    public void loadContent(){
        FlameBullets.load();
        FlameUnitTypes.load();
        SpecialContent.load();
    }

}
