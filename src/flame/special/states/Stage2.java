package flame.special.states;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.audio.*;
import flame.effects.*;
import flame.entities.*;
import flame.graphics.*;
import flame.special.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.fragments.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;
import static flame.special.states.Stage1.*;

public class Stage2 extends SpecialState{
    static Table container;
    static Runnable clickedLoader;
    Tile tile;
    boolean pass, placed;
    float passTime = 0f, placeTime, playerX, playerY;
    float spawnTime = 0f;
    float fadeTime = 0f;
    float killTime = 0f;
    float crashTime = 0f;

    boolean killed;
    SoundInstance sound;

    static Seq<Unit> units = new Seq<>();

    @Override
    public void loadClient(){
        replaceMenu(this::loadWorld);
    }

    @Override
    public void loadAssets(){
        //Events.on(ResizeEvent.class, e -> rebuild());
        Events.on(ResetEvent.class, e -> {
            tile = null;
            pass = placed = false;
            passTime = 0f;
            placeTime = 0f;
            fadeTime = 0f;
        });
    }

    @Override
    public void update(){
        updateSilence();
        if(killed && killTime <= 0f){
            crashTime += FlameOutSFX.timeDelta;
            if(crashTime >= 5f * 60){
                SpecialMain.increment(false);
                Core.app.exit();
                return;
            }
        }

        if(!state.isGame()) return;

        fadeTime += FlameOutSFX.timeDelta;
        killTime -= FlameOutSFX.timeDelta;
        if(killed && killTime <= 0f && sound == null){
            sound = new SoundInstance(FlameSounds.empathyShine);
            sound.play(5f, 1f, true);
            sound.protect();
            
            SoundInstance ring = new SoundInstance(FlameSounds.silence);
            ring.play(1f, 2f, true);
            ring.protect();
        }

        Unit unit = player.unit();
        if(unit.y > 175 * 8 && !pass){
            pass = true;
            playerX = unit.x;
            playerY = unit.y;
        }
        if(pass) passTime += FlameOutSFX.timeDelta;
        if(pass && passTime < 6f * 60f){
            unit.vel.setZero();
            unit.x = playerX;
            unit.y = playerY;
        }
        if(passTime >= 4f * 60f){
            if((tile == null || tile.block() != SpecialContent.spawner) && !killed){
                tile = world.tile(25, 185);

                tile.setBlock(SpecialContent.spawner, player.team());
                Fx.placeBlock.at(25 * tilesize, 185 * tilesize, 1);

                placed = true;
            }

            if(placed){
                placeTime += FlameOutSFX.timeDelta;
            }

            if(placeTime > 60){
                boolean validunits = false;
                units.clear();

                for(Unit u : Groups.unit){
                    if(!u.spawnedByCore){
                        validunits = true;
                        units.add(u);

                        if(u.isPlayer()){
                            Fx.spawn.at(25 * tilesize, 175 * tilesize);

                            Unit core = UnitTypes.alpha.create(u.team);
                            core.set(25 * tilesize, 175 * tilesize);
                            core.rotation(90f);
                            core.impulse(0f, 3f);
                            core.spawnedByCore = true;
                            core.controller(player);
                            u.controller(u.type.createController(u));

                            core.add();
                        }
                    }
                }

                if(validunits){
                    spawnTime += FlameOutSFX.timeDelta;

                    if(spawnTime >= 3f * 60){
                        killed = true;
                        killTime = 120f;
                        tile.setBlock(Blocks.air);

                        for(Unit u : units){
                            killUnit(u);
                        }
                        spawnTime = 60f;
                    }

                }else{
                    spawnTime = 0f;
                }
            }
        }
    }

    static void killUnit(Unit u){
        EmpathyDamage.damageUnit(u, u.maxHealth + 900000f, true, () -> {
            float trueSize = Math.max(u.hitSize, Math.min(u.type.region.width * Draw.scl, u.type.region.height * Draw.scl));

            BloodSplatter.setLifetime(45f * 60);
            BloodSplatter.explosion(40, u.x, u.y, trueSize / 2f, trueSize * 1.5f + 60f, trueSize * 0.4f / 4f + 12f);
            BloodSplatter.explosion(60, u.x, u.y, trueSize / 2f, trueSize + 50f, trueSize * 0.75f / 4f + 25f);
            BloodSplatter.setLifetime();

            FragmentationBatch batch = FlameOut.fragBatch;
            batch.baseElevation = 0f;
            batch.fragFunc = e -> {
                float dx = (e.x - u.x) / 35f;
                float dy = (e.y - u.y) / 35f;

                e.vx = dx;
                e.vy = dy;
                e.vr = Mathf.range(2f);
                //e.lifetime = 180f;
                e.vz = Mathf.random(-0.01f, 0.1f);

                e.lifetime = 25f * 60f;
            };
            batch.fragDataFunc = f -> {
                f.fadeOut = true;
                f.trailEffect = Fx.none;
            };
            batch.onDeathFunc = null;
            batch.altFunc = (x, y, r) -> {};
            batch.trailEffect = batch.explosionEffect = null;
            batch.fragColor = Color.white;

            batch.switchBatch(u::draw);

            float size = (trueSize / 2f) / 20f;
            float size2 = (trueSize / 2f);

            int amount = 3 + (int)(size * size);
            int amount2 = Mathf.random(3, 6);
            for(int i = 0; i < amount2; i++){
                Vec2 v = Tmp.v1.trns(Mathf.random(360f), Mathf.random());

                Part2 p = new Part2();
                p.x = u.x + v.x * (size2 / 3f);
                p.y = u.y + v.y * (size2 / 3f);
                p.rotation = p.targetRotation = Mathf.random(360f);
                p.bend = p.targetBend = Mathf.random(25f, 90f);

                p.vx = v.x * size2 / 10f;
                p.vy = v.y * size2 / 10f;
                p.vr = Mathf.range(3f);

                p.length = size2 * Mathf.random(0.7f, 1.1f);
                p.width = (p.length / 4f) * Mathf.random(1f, 1.5f);

                p.add();
            }

            for(int i = 0; i < amount; i++){
                Vec2 v = Tmp.v1.trns(Mathf.random(360f), Mathf.random());

                Part p = new Part();
                p.x = u.x + v.x * size2;
                p.y = u.y + v.y * size2;
                p.rotation = Mathf.random(360f);

                p.vx = v.x * size2 / 4.5f;
                p.vy = v.y * size2 / 4.5f;
                p.vr = Mathf.range(6f);
                p.hitSize = (size2 / 65f) * Mathf.random(0.9f, 1f);

                p.add();
            }

            Part cage = new Part();
            cage.x = u.x;
            cage.y = u.y;
            cage.rotation = Mathf.random(360f);

            Vec2 v = Tmp.v1.trns(Mathf.random(360f), Mathf.random());

            cage.vx += v.x * size2 / 8f;
            cage.vy += v.y * size2 / 8f;
            cage.vr = Mathf.range(6f);
            cage.hitSize = (size2 / 60f) * Mathf.random(0.9f, 1f);

            cage.textureIdx = -1;

            cage.add();
        });
    }

    @SuppressWarnings("all")
    @Override
    public void draw(){
        float z = Draw.z();
        int heighti = world.height() / 15;
        TextureRegion reg = SpecialMain.regionSeq.get(2);
        float rw = reg.width * Draw.scl, rh = reg.height * Draw.scl;
        Camera cam = Core.camera;
        Core.camera.bounds(r2);

        Draw.z(Layer.flyingUnitLow);

        if(placeTime > 60f && !killed){
            //StringBuilder text = new StringBuilder("Summon a unit.");
            String text = "Summon a unit";

            if(placeTime < 90f){
                int len = text.length();
                text = "";
                //text = new StringBuilder();
                for(int i = 0; i < len; i++){
                    //String.valueOf(i);
                    //text += Character.toChars(2);
                    char c = (char)(Mathf.random(32, 126));
                    //text.append(c);
                    text += c;
                }
            }

            Fonts.outline.setColor(Color.white);
            Fonts.outline.draw(text, 25 * tilesize, 186 * tilesize, Color.white, Draw.scl, false, Align.center);
        }
        Draw.z(Layer.groundUnit);

        if(!killed){
            Draw.color();
            for(int i = 0; i < heighti; i++){
                float y = (i + 0.5f) * 15f * tilesize;
                for(int s = 0; s < 2; s++){
                    int sign = s == 0 ? -1 : 1;
                    float x = ((world.width() - 1f) * tilesize) / 2f + (15f * 8 * sign);

                    r1.setCentered(x, y, rw, rh);
                    if(r2.overlaps(r1)){
                        Draw.rect(reg, x, y, -rw, rh * sign, 90f);
                    }
                }
            }
        }
        float fout = 1f - Mathf.clamp((fadeTime - 6f * 60) / (7f * 60f));
        //float fout2 = 1f - Mathf.clamp((fadeTime - 7f * 60) / (6f * 60f));

        //Draw.z(Layer.flyingUnitLow);
        Draw.color(Color.black);
        //Draw.alpha(fout2);

        float w = ((world.width() - 1f) * tilesize) / 2f + (15f * 8);
        float ow = (w - rw / 2f) / 2f;
        float fow = ow * fout;
        //Fill.rect(0f, cam.position.y, ow, cam.height);
        Fill.rect(fow / 2f, cam.position.y, fow, cam.height);

        //Fill.rect(((world.width() - 1f) * tilesize) - (rw + w), cam.position.y, rw + w, cam.height);
        //Fill.rect(((world.width() + 0.5f) * tilesize), cam.position.y, w - rw / 2f, cam.height);
        //Fill.rect(((world.width()) * tilesize) + ow / 2f, cam.position.y, ow, cam.height);
        
        Fill.rect((fow / 2f) + (((world.width() - 1f) * tilesize) - fow), cam.position.y, fow, cam.height);

        if(killTime > 0){
            Draw.z(Layer.end);
            Draw.color(Color.black);
            Draw.rect();
        }

        Draw.color();

        Draw.z(z);
    }

    void loadWorld(){
        logic.reset();

        int width = 51, height = 350;

        Tiles tiles = world.resize(width, height);

        world.beginMapLoad();

        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            int inx = ((width - 1) - x);

            Floor floor = (inx == 6 || x == 6) ? Blocks.metalFloor3.asFloor() : ((inx > 12 && x > 12) ? Blocks.darkPanel4.asFloor() : Blocks.darkPanel3.asFloor());
            //Block block = (x == (width / 2) && y == (width / 2)) ? Blocks.coreShard : Blocks.air;

            Tile tile;
            tiles.set(x, y, (tile = new Tile(x, y)));
            tile.setFloor(floor);
            /*
            if(block != Blocks.air){
                tile.setBlock(block, Team.sharded);
            }
            */
        }
        
        Tile tile1 = tiles.get(width / 2, width / 2);
        tile1.setBlock(Blocks.coreShard, Team.sharded);

        world.endMapLoad();

        Rules rules = new Rules();
        Gamemode.sandbox.apply(rules);

        rules.spawns.clear();
        rules.spawns.add(new SpawnGroup(UnitTypes.dagger));
        rules.editor = false;

        state.rules = rules;

        logic.play();
        Events.fire(Trigger.newGame);
    }

    static void replaceMenu(Runnable run){
        clickedLoader = run;
        new AltMenuFragment();
        new AltPausedDialog();
        rebuild();
        Events.on(ResizeEvent.class, e -> rebuild());
    }

    static void rebuild(){
        if(container != null){
            rebuildDesktop();
        }
    }

    static void rebuildDesktop(){
        if(container == null) return;
        
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float width = 230f;
        Drawable background = Styles.black6;

        container.left();
        container.add().width(Core.graphics.getWidth()/10f);

        container.table(background, t -> {
            t.defaults().width(width).height(70f);
            t.name = "buttons";

            Runnable clicked = () -> ui.loadAnd(clickedLoader);

            buttonBasic(t, "@play", Icon.play, clicked);
            buttonBasic(t, "@quit", Icon.exit, clicked);
        }).width(width).growY();

        container.table(background, t -> {
            t.name = "submenu";
            t.color.a = 0f;
            t.top();
            t.defaults().width(width).height(70f);
            t.visible(() -> !t.getChildren().isEmpty());
        }).width(width).growY();
    }

    static void buttonBasic(Table t, String name, Drawable icon, Runnable clicked){
        t.button(name, icon, Styles.flatToggleMenut, clicked).marginLeft(11f).get();
        t.row();
    }

    static class Part2 extends DrawEntity{
        float rotation, bend;
        float time = 0f;

        float targetBend, targetRotation, bendTime;
        float smooth;
        float length = 100f, width = 10f;

        float fric1, fric2;
        boolean flipped = Mathf.chance(0.5f);
        boolean end = Mathf.chance(0.5f);

        float vx, vy, vr;
        float landTime = Mathf.random(20f, 30f);

        @Override
        public void update(){
            time += Time.delta;

            x += vx * Time.delta;
            y += vy * Time.delta;
            rotation += vr * Time.delta;
            targetRotation += vr * Time.delta;

            if(time <= landTime){
                return;
            }else{
                float drag = 1f - Mathf.clamp(0.5f * Time.delta);
                vx *= drag;
                vy *= drag;
                vr *= drag;
            }

            v1.trns(rotation + bend, length).add(x, y);
            float lx1 = v1.x, ly1 = v1.y;
            v1.trns(rotation - bend, length).add(x, y);
            float lx2 = v1.x, ly2 = v1.y;

            float lmx = (lx1 + lx2 + x) / 3f;
            float lmy = (ly1 + ly2 + y) / 3f;

            if((bendTime -= Time.delta) <= 0f){
                targetBend = Mathf.random(25f, 90f) * (flipped ? -1 : 1);
                targetRotation = Mathf.mod(rotation + Mathf.random(5f), 360f);
                bendTime = Mathf.random(15f, 40f);

                fric1 = Mathf.random();
                fric2 = Mathf.random();
                smooth = 0f;
            }

            bend = Mathf.lerpDelta(bend, targetBend, 0.1f * (smooth = Mathf.lerpDelta(smooth, 1f, 0.25f)));
            rotation = Mathf.slerpDelta(rotation, targetRotation, 0.01f);

            v1.trns(rotation + bend, length).add(x, y);
            float nx1 = v1.x, ny1 = v1.y;
            v1.trns(rotation - bend, length).add(x, y);
            float nx2 = v1.x, ny2 = v1.y;

            float nmx = (nx1 + nx2 + x) / 3f;
            float nmy = (ny1 + ny2 + y) / 3f;

            float dx = nmx - lmx;
            float dy = nmy - lmy;

            float total = fric1 + fric2;
            if(total > Mathf.FLOAT_ROUNDING_ERROR){
                float dscl = Math.max(total, 1f);

                v1.trns(rotation + bend, length).add(x - dx, y - dy);
                float ox1 = v1.x, oy1 = v1.y;
                v1.trns(rotation - bend, length).add(x - dx, y - dy);
                float ox2 = v1.x, oy2 = v1.y;

                float mx1 = (ox1 - lx1) * (fric1 / dscl), my1 = (oy1 - ly1) * (fric1 / dscl);
                float mx2 = (ox2 - lx2) * (fric2 / dscl), my2 = (oy2 - ly2) * (fric2 / dscl);

                dx += mx1 + mx2;
                dy += my1 + my2;
            }

            x -= dx;
            y -= dy;

            if(time > 25f * 60){
                remove();
            }
        }

        @Override
        public float clipSize(){
            return length * 2f + 10f;
        }

        @Override
        public void draw(){
            int sign = flipped ? -1 : 1;
            float fout = Mathf.clamp((25f * 60 - time) / 120f);

            Draw.z(Layer.debris + 1.01f);
            Draw.color(Color.white, fout);

            v1.trns(rotation + bend, length).add(x, y);
            float lx1 = v1.x, ly1 = v1.y;
            v1.trns(rotation - bend, length).add(x, y);
            float lx2 = v1.x, ly2 = v1.y;

            TextureRegion branch = SpecialMain.regionSeq.get(10);
            TextureRegion flower = SpecialMain.regionSeq.get(9);

            Lines.stroke(width * sign);
            Lines.line(branch, lx1, ly1, x, y, false);
            Lines.line(branch, x, y, lx2, ly2, false);
            
            if(end) Lines.line(flower, x, y, lx2, ly2, false);

            float jx = (lx2 - x) * 0.25f + x, jy = (ly2 - y) * 0.25f + y;
            float len = length + length * 0.25f;
            float dst = Mathf.dst(lx1, ly1, jx, jy) / len;
            float wscl = 1f + (1f - dst);

            Lines.stroke(width * sign * wscl);
            Lines.line(flower, lx1, ly1, jx, jy, false);
            
            float offwid = width / 3.5f;
            float offLen = length * 0.05f;
            
            //v1.trns((rotation - (bend / 2f)) - 90f * sign, offwid).add(x, y);
            //v1.trns((rotation + 90f - (90f - bend) / 2f) - 90f * sign, offwid).add(x, y);
            //v1.trns((rotation + 45f + (90f - bend) / 2f) - 90f * sign, offwid).add(x, y);
            v1.trns((rotation - bend) - 90f, offwid * sign, offLen).add(x, y);
            float rx = v1.x, ry = v1.y;
            float tlen = length + offLen;
            float len2 = Mathf.sqrt(tlen * tlen + offwid * offwid);
            float dst2 = Mathf.dst(lx1, ly1, rx, ry) / len2;
            float wscl2 = 1f + (1f - dst2);
            
            Lines.stroke(width * -sign * 0.5f * wscl2);
            Lines.line(flower, lx1, ly1, rx, ry, false);
        }
    }

    static class Part extends DrawEntity{
        int textureIdx = Mathf.random(4);
        float vx, vy, vr;
        float rotation;
        float time = 0f;
        float timeOffset = Mathf.random(24f);
        float landTime = Mathf.random(15f, 23f);
        float hitSize = 1f;
        boolean flipped = Mathf.chance(0.5f);
        boolean landed;

        @Override
        public void update(){
            time += Time.delta;
            if(time > landTime && !landed){
                landed = true;
            }

            x += vx * Time.delta;
            y += vy * Time.delta;
            rotation += vr * Time.delta;

            if(textureIdx == 0) rotation += Mathf.range(2.5f);

            //vx *= 1f
            float drag = 1f - Mathf.clamp((landed ? 0.5f : 0.05f) * Time.delta);

            vx *= drag;
            vy *= drag;
            vr *= drag;

            if(time >= 25f * 60){
                remove();
            }
        }

        @Override
        public float clipSize(){
            return 90f;
        }

        @Override
        public void draw(){
            TextureRegion reg = SpecialMain.regionSeq.get(4 + textureIdx);
            float fout = Mathf.clamp((25f * 60 - time) / 120f);
            float scl = 1;

            if(textureIdx == 3){
                //scl = (1f - (((time + timeOffset) / 24f) % 1f)) * 0.125f + (1f - 0.125f);
                scl = 1f - (1f - (((time + timeOffset) / 24f) % 1f)) * 0.125f;
            }
            if(textureIdx == 4){
                scl = Mathf.absin((time + timeOffset), 40f, 0.1f) + 0.9f;
            }

            Draw.z(Layer.debris + 1f);
            Draw.color(Color.white, fout);
            Draw.rect(reg, x, y, reg.width * Draw.scl * hitSize * (flipped ? -1f : 1f) * scl, reg.height * Draw.scl * hitSize * scl, rotation);
        }
    }

    static class AltMenuFragment extends MenuFragment{
        MenuRenderer rend;

        AltMenuFragment(){
            if(ui.menufrag != this && ui.menufrag != null){
                updateRenderer();
                //rebuild();

                for(Element child : ui.menuGroup.getChildren()){
                    child.clear();
                }
                ui.menuGroup.clear();

                build(ui.menuGroup);
                ui.menufrag = this;
            }
        }

        void updateRenderer(){
            try{
                Field renderer = ReflectUtils.findField(MenuFragment.class, "renderer");

                MenuRenderer r = (MenuRenderer)renderer.get(ui.menufrag);
                if(r != null){
                    renderer.set(this, r);
                    rend = r;
                }else{
                    rend = new MenuRenderer();
                }

            }catch(Exception e){
                Log.err(e);
            }
        }

        @Override
        public void build(Group parent){
            if(rend == null) rend = new MenuRenderer();

            Group group = new WidgetGroup();
            group.setFillParent(true);
            group.visible(() -> !ui.editor.isShown());
            parent.addChild(group);
            parent = group;

            parent.fill((x, y, w, h) -> rend.render());

            parent.fill(c -> {
                c.pane(Styles.noBarPane, cont -> {
                    container = cont;
                    cont.name = "menu container";

                    c.left();
                    rebuild();
                }).with(pane -> {
                    pane.setOverscroll(false, false);
                }).grow();
            });

            String versionText = ((Version.build == -1) ? "[#fc8140aa]" : "[#ffffffba]") + Version.combined();
            parent.fill((x, y, w, h) -> {
                TextureRegion logo = Core.atlas.find("logo");
                float width = Core.graphics.getWidth(), height = Core.graphics.getHeight() - Core.scene.marginTop;
                float logoscl = Scl.scl(1) * logo.scale;
                float logow = Math.min(logo.width * logoscl, Core.graphics.getWidth() - Scl.scl(20));
                float logoh = logow * (float)logo.height / logo.width;

                float fx = (int)(width / 2f);
                float fy = (int)(height - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);
                if(Core.settings.getBool("macnotch")){
                    fy -= Scl.scl(macNotchHeight);
                }

                Draw.color();
                Draw.rect(logo, fx, fy, -logow, logoh);

                Fonts.outline.setColor(Color.white);
                Fonts.outline.draw(versionText, fx, fy - logoh/2f - Scl.scl(2f), Align.center);
            }).touchable = Touchable.disabled;
        }
    }

    static class AltPausedDialog extends PausedDialog{
        AltPausedDialog(){
            super();

            ui.paused = this;

            shown(this::rebuild);
        }

        void rebuild(){
            cont.clear();

            cont.defaults().size(130f).pad(5);
            cont.buttonRow("@back", Icon.play, this::hide);
        }
    }
}
