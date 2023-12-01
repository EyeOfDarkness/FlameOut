package flame.special.states;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.audio.*;
import flame.graphics.*;
import flame.special.*;
import mindustry.*;
import mindustry.audio.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;

public class Stage1 extends SpecialState{
    float time = 0;
    float time2 = 0f, time3 = 0f, time4 = 0f;
    boolean found = false, collided = false;
    Ball ball;
    Seq<Ball2> ball2s = new Seq<>();
    int encounters = 0;

    SoundInstance sound;

    static Rect r1 = new Rect(), r2 = new Rect(), r3 = new Rect();
    static Vec2 v1 = new Vec2();
    static boolean silence = false;

    @Override
    public void update(){
        updateSilence();

        if(!state.isGame() || state.isEditor()) return;
        time += Time.delta;

        //if(time > 10 && time < (6f * 60f)){
        if(time > (0.75f * 60f * 60f) && time < (2f * 60f * 60f) && !found){
            if(time2 <= 0f){
                //time2 = Mathf.random(2f, 5f) * 60f;
                time2 = Mathf.random(20f, 40f) * 60f;
                logic.runWave();
            }
            if(canSkipWave()) time2 -= Time.delta;
        }
        //if(time > (6f * 60f)){
        if(time > (1.5f * 60f * 60f) && !found){
            if(time3 <= 0f){
                Core.camera.bounds(r1);
                boolean intersected = false;
                for(CoreBuild core : player.team().data().cores){
                    core.hitbox(r2);
                    if(r1.overlaps(r2)){
                        intersected = true;
                        break;
                    }
                }
                if(!intersected){
                    if(time3 <= -60f){
                        time3 = Mathf.random(11f, 20f) * 60f;
                        //time3 = Mathf.random(3f, 4f) * 60f;
                        Events.fire(Trigger.teamCoreDamage);
                    }else{
                        time3 -= Time.delta;
                    }
                }
            }
            if(time3 > 0) time3 -= Time.delta;
        }

        if(time > (2.2f * 60f * 60f) && ball == null){
            ball = new Ball();
            ball.initPosition();
        }

        if(time > (0.5f * 60f * 60f)){
            if(sound == null){
                sound = new SoundInstance(FlameSounds.silence);
                sound.play(0.001f, 2f, true);
                sound.protect();
            }

            float fin = Mathf.clamp((time - (0.5f * 60 * 60)) / (1f * 60 * 60) + 0.001f);
            sound.setVolume(fin);
        }

        if(time > (4f * 60 * 60) || encounters >= 3){
            if(sound != null){
                sound.stop();
            }
            found = true;
        }

        if(found){
            time4 += Time.delta;
        }

        //if(time4 > (2f * 60)){
        if(time4 > (10f * 60)){
            if(ball2s.isEmpty()){
                FlameSounds.screams.play(10f, 0.5f, 0f);
                for(int i = 0; i < 100; i++){
                    ball2s.add(new Ball2());
                }
            }
        }

        if(collided){
            SpecialMain.increment(false);
            Core.app.exit();
        }
    }

    static void updateSilence(){
        if(!state.isMenu() && state.isPaused()){
            state.set(State.playing);
        }

        if(!silence && Vars.control.sound != null){
            SoundControl sc = Vars.control.sound;
            sc.ambientMusic.clear();
            sc.bossMusic.clear();
            sc.darkMusic.clear();

            sc.stop();

            Musics.menu = new Music();
            Musics.editor = new Music();
            Musics.launch = new Music();

            silence = true;
        }
    }

    @Override
    public void draw(){
        float z = Draw.z();
        if(found){
            Draw.z(Layer.floor + 1f);
            Draw.color(Color.red);
            Draw.blend(GraphicUtils.multiply);
            Draw.rect();
            Draw.blend();
            Draw.color();
        }
        Draw.z(Layer.flyingUnit);
        if(ball != null && !found){
            ball.draw();
        }
        if(!ball2s.isEmpty()){
            for(Ball2 b : ball2s){
                b.draw();
            }
        }
        Draw.z(z);
    }

    static boolean canSkipWave(){
        return state.rules.waves && state.rules.waveSending && ((net.server() || player.admin) || !net.active()) && state.enemies == 0 && !spawner.isSpawning();
    }

    class Ball{
        float x, y, offsetX, offsetY;
        float time1 = 0f, time2 = 0f;
        float time3 = 0f;
        boolean set = false, detected;

        void initPosition(){
            Camera cam = Core.camera;
            Core.camera.bounds(r1);
            float camw = cam.width, camh = cam.height;
            //testing
            //r1.grow(-cam.width / 4f, -cam.height / 4f);

            TextureRegion reg = SpecialMain.regionSeq.get(1);
            r2.setCentered(x, y, reg.width / 2f, reg.height / 2f);

            r3.set(r1).grow(reg.width * 2f + camw * 0.1f, reg.height * 2f + camh * 0.1f);
            int s = Mathf.random(0, 3);

            switch(s){
                case 0 -> {
                    x = r3.x + Mathf.random(r3.width);
                    y = r3.y;
                }
                case 1 -> {
                    x = r3.x + Mathf.random(r3.width);
                    y = r3.y + r3.height;
                }
                case 2 -> {
                    x = r3.x;
                    y = r3.y + Mathf.random(r3.height);
                }
                case 3 -> {
                    x = r3.x + r3.width;
                    y = r3.y + Mathf.random(r3.height);
                }
            }
        }

        void draw(){
            Camera cam = Core.camera;
            Core.camera.bounds(r1);
            float camw = cam.width, camh = cam.height;
            //testing
            //r1.grow(-cam.width / 4f, -cam.height / 4f);

            TextureRegion reg = SpecialMain.regionSeq.get(1);
            r2.setCentered(x, y, reg.width / 2f, reg.height / 2f);
            r3.setCentered(x, y, reg.width, reg.height).grow(15f);

            if(!detected && !r3.overlaps(r1)){
                if(time3 > 20f){
                    time3 = 0f;
                    r3.set(r1).grow(reg.width * 2f + camw * 0.1f, reg.height * 2f + camh * 0.1f);
                    int s = Mathf.random(0, 3);

                    switch(s){
                        case 0 -> {
                            x = r3.x + Mathf.random(r3.width);
                            y = r3.y;
                        }
                        case 1 -> {
                            x = r3.x + Mathf.random(r3.width);
                            y = r3.y + r3.height;
                        }
                        case 2 -> {
                            x = r3.x;
                            y = r3.y + Mathf.random(r3.height);
                        }
                        case 3 -> {
                            x = r3.x + r3.width;
                            y = r3.y + Mathf.random(r3.height);
                        }
                    }
                }
                time3 += FlameOutSFX.timeDelta;
            }

            if(r2.overlaps(r1)){
                detected = true;
                time3 = 0f;
            }
            if(detected){
                time1 += FlameOutSFX.timeDelta;
                if(time1 >= 20f || r1.contains(r2)){
                    if(!set){
                        offsetX = (x - cam.position.x) / cam.width;
                        offsetY = (y - cam.position.y) / cam.height;
                        set = true;
                    }
                }
            }

            if(!set){
                Draw.rect(reg, x, y);
            }else{
                time2 += FlameOutSFX.timeDelta / 60f;

                //float dx = (offsetX * cam.width) * 3f * time2;
                //float dy = (offsetY * cam.height) * 3f * time2;
                float dx = (cam.width - Math.abs(offsetX * cam.width)) * 3f * time2 * (offsetX > 0 ? 1f : -1f);
                float dy = (cam.height - Math.abs(offsetY * cam.height)) * 3f * time2 * (offsetY > 0 ? 1f : -1f);

                Draw.rect(reg, offsetX * cam.width + cam.position.x + dx, offsetY * cam.height + cam.position.y + dy);

                if(time2 >= 1f){
                    time1 = time2 = time3 = 0f;
                    set = false;
                    detected = false;
                    encounters++;

                    x = offsetX * cam.width + cam.position.x + dx;
                    y = offsetY * cam.height + cam.position.y + dy;
                }
            }
        }
    }

    class Ball2{
        float x, y, time;

        Ball2(){
            Camera cam = Core.camera;
            Core.camera.bounds(r1);
            float camw = cam.width, camh = cam.height;
            float scl = Mathf.random(0.1f, 0.5f);

            time = -Mathf.random(10f);

            TextureRegion reg = SpecialMain.regionSeq.get(1);
            r2.setCentered(x, y, reg.width / 2f, reg.height / 2f);

            r3.set(r1).grow(reg.width * 2f + camw * scl, reg.height * 2f + camh * scl);
            int s = Mathf.random(0, 3);

            switch(s){
                case 0 -> {
                    x = r3.x + Mathf.random(r3.width);
                    y = r3.y;
                }
                case 1 -> {
                    x = r3.x + Mathf.random(r3.width);
                    y = r3.y + r3.height;
                }
                case 2 -> {
                    x = r3.x;
                    y = r3.y + Mathf.random(r3.height);
                }
                case 3 -> {
                    x = r3.x + r3.width;
                    y = r3.y + Mathf.random(r3.height);
                }
            }
        }

        void draw(){
            time += Time.delta;
            Unit unit = player.unit();
            if(unit != null && !unit.isNull()){
                Tmp.v1.set(unit.x, unit.y).sub(x, y).limit(25f * Mathf.clamp(time / 40f));
                x += Tmp.v1.x;
                y += Tmp.v1.y;

                unit.hitbox(r1);
                if(r1.contains(x, y)){
                    collided = true;
                }
            }

            TextureRegion reg = SpecialMain.regionSeq.get(1);
            Draw.rect(reg, x, y, reg.width, reg.height);
        }
    }
}
