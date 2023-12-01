package flame.special.states;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.special.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class Stage4 extends SpecialState{
    A a;
    float waitTime = 0f;

    @Override
    public void loadClient(){
        Stage2.replaceMenu(this::loadWorld);
        Events.run(Trigger.preDraw, () -> {
            state.rules.lighting = true;
            state.rules.ambientLight.set(Color.black);
        });
    }

    void loadWorld(){
        logic.reset();

        int width = 549, height = 549;

        Tiles tiles = world.resize(width, height);

        world.beginMapLoad();

        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;

            Floor floor = Blocks.grass.asFloor();

            Tile tile;
            tiles.set(x, y, (tile = new Tile(x, y)));
            tile.setFloor(floor);
        }

        Tile tile1 = tiles.get(width / 2, height / 2);
        tile1.setBlock(Blocks.coreShard, Team.sharded);

        world.endMapLoad();

        Rules rules = new Rules();
        Gamemode.sandbox.apply(rules);

        rules.spawns.clear();
        rules.spawns.add(new SpawnGroup(UnitTypes.dagger));
        rules.editor = false;

        rules.lighting = true;
        rules.ambientLight.set(Color.black);

        UnitTypes.alpha.lightRadius *= 8f;

        state.rules = rules;

        logic.play();
        Events.fire(Trigger.newGame);
    }

    @Override
    public void update(){
        Stage1.updateSilence();

        if(state.isGame()){
            waitTime += Time.delta;

            if(waitTime > 5f * 60f && a == null){
                float x = player.x, y = player.y;
                Vec2 v = Stage1.v1.trns(Mathf.random(360f), 300f * 8).add(x, y);

                a = new A();
                a.x = v.x;
                a.y = v.y;
            }

            if(a != null){
                a.update();
            }

            if(waitTime > (2f * 60 * 60) && a != null){
                if(a.speed < 60f) a.speed = 60f;
            }
        }
    }

    @Override
    public void draw(){
        if(a != null){
            a.draw();
        }
    }

    static class A{
        float x, y;
        float speed = UnitTypes.alpha.speed / 4f;

        void update(){
            speed += (1f / (60f * 60f)) * FlameOutSFX.timeDelta;
            if(!player.unit().isNull()) speed = Math.max(player.unit().type.speed / 4f, speed);

            Vec2 v = Stage1.v1;
            Unit target = player.unit();

            v.set(target).sub(x, y).limit(speed * FlameOutSFX.timeDelta);

            x += v.x;
            y += v.y;

            TextureRegion r = SpecialMain.regionSeq.get(0);
            float size = ((r.width * Draw.scl * 4f) / 2f) * 0.75f;

            Rect rect = Stage1.r1.setCentered(x, y, size * 2f);
            Groups.unit.intersect(rect.x, rect.y, rect.width, rect.height, u -> {
                if(Mathf.within(x, y, u.x, u.y, size + u.hitSize / 2f)){
                    if(!u.isPlayer()){
                        Stage2.killUnit(u);
                    }
                }
            });

            if(Mathf.within(x, y, target.x, target.y, size)){
                SpecialMain.increment(false);
                Core.app.exit();
            }
        }

        void draw(){
            TextureRegion r = SpecialMain.regionSeq.get(0);

            Draw.color();
            Draw.z(Layer.flyingUnit);

            Draw.rect(r, x, y, r.width * Draw.scl * 4f, r.height * Draw.scl * 4f);
        }
    }
}
