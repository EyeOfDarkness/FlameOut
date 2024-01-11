package flame;

import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
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
import mindustry.graphics.*;
import mindustry.mod.*;

public class FlameOut extends Mod{
    public static FragmentationBatch fragBatch;
    public static CutBatch cutBatch;
    public static VaporizeBatch vaporBatch;
    public static DevastationBatch devasBatch;

    public final boolean test = true;

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
                devasBatch = new DevastationBatch();
                //fragBatch.load();
                FlameOutSFX.inst.loadHeadless();
                SpecialMain.load();
                //SpecialDeathEffects.load();
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
                    //FlameBullets.test.create(p, p.x, p.y, ang);
                    FlameFX.desMissileHit.at(p.x, p.y, ang);
                }
            });
            /*
            Events.run(Trigger.draw, () -> {
                Unit p = Vars.player.unit();
                float sx = Vars.player.mouseX, sy = Vars.player.mouseY;
                Draw.z(Layer.flyingUnit);

                Lines.stroke(2f);
                Lines.line(p.x, p.y, sx, sy);

                for(Unit u : Groups.unit){
                    if(p != u){
                        u.hitbox(Utils.r);

                        Lines.rect(Utils.r);
                        Vec2 v = Utils.intersectRect(p.x, p.y, sx, sy, Utils.r);
                        if(v != null){
                            Fill.poly(v.x, v.y, 4, 4f);
                        }
                    }
                }
            });
            */
        }
    }

    @Override
    public void loadContent(){
        SpecialDeathEffects.load();
        FlameBullets.load();
        FlameUnitTypes.load();
        SpecialContent.load();
    }

}
