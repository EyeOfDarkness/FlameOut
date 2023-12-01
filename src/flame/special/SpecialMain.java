package flame.special;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.struct.*;
import arc.util.*;
import flame.special.states.*;
import mindustry.*;

public class SpecialMain{
    public static ObjectMap<String, TextureRegion> regions = new ObjectMap<>();
    public static Seq<TextureRegion> regionSeq = new Seq<>();
    static int offset = 691038;
    static Texture main;
    static SpecialState activeState;

    private static int state = 0;

    public static void draw(){
        if(activeState != null){
            activeState.draw();
        }
    }
    public static void update(){
        if(activeState != null){
            activeState.update();
        }
    }
    public static void updateTest(){
        if(Core.input.keyTap(KeyCode.z)){
            Log.info("Set Zero");
            state = 0;
            Core.settings.put("flame-special", state);
        }
        if(Core.input.keyTap(KeyCode.x)){
            increment(false);
            Log.info("Stage: " + state);
        }
    }

    public static void increment(){
        increment(true);
    }
    public static void increment(boolean change){
        if(main == null) return;
        state++;
        Core.settings.put("flame-special", state);
        if(change) loadState();
    }

    public static int getStage(){
        return state;
    }
    public static boolean validEmpathySpawn(){
        return state == 0 || state >= 5;
    }

    public static void dispose(){
        main.dispose();

        activeState = null;
    }

    public static void load(){
        state = Core.settings.getInt("flame-special", 0);
        //loadState();
        if(state > 5) return;
        try{
            Fi file = Vars.tree.get("extras/Vultures.png");
            byte[] bytes = file.readBytes();
            Pixmap map = new Pixmap(bytes, offset, bytes.length);
            //PixmapTextureData data = new PixmapTextureData(map, false, true);
            main = new Texture(map);
            main.setFilter(TextureFilter.linear);

            loadRegion("main", 0, 0, 1023, 1023);
            loadRegion("ball", 1024, 192, 1247, 319);
            loadRegion("hug", 1024, 320, 1407, 639);
            loadRegion("cat", 1408, 256, 1791, 639);
            loadRegion("bunny0", 1792, 512, 1919, 639);
            loadRegion("bunny1", 1024, 640, 1279, 895);
            loadRegion("bunny2", 1280, 640, 1535, 895);
            loadRegion("bunny3", 1536, 640, 1791, 895);
            loadRegion("bunny4", 1792, 640, 2047, 895);
            loadRegion("flower", 1026, 896, 1533, 1023);
            loadRegion("tree", 1536, 896, 2047, 1023);
        }catch(Exception e){
            main = null;
            Log.err("Special Flame-Out", e);
        }
        loadState();

        if(activeState != null){
            activeState.loadAssets();
        }
    }
    public static void loadClient(){
        //state = Core.settings.getInt("flame-special", 0);
        
        if(activeState == null) loadState();

        if(activeState != null){
            activeState.loadClient();
        }
    }

    static void loadRegion(String name, int u, int v, int u2, int v2){
        Texture tex = main;

        TextureRegion reg = new TextureRegion(tex, u, v, (u2 + 1) - u, (v2 + 1) - v);

        if(name.equals("bunny3")){
            reg.width = (int)(reg.width * 0.75f);
            reg.height = (int)(reg.height * 0.75f);
        }
        if(name.equals("bunny4")){
            reg.width = (int)(reg.width * 1.5f);
            reg.height = (int)(reg.height * 1.5f);
        }
        if(name.equals("ball")){
            reg.width = (int)(reg.width * 1.25f);
            reg.height = (int)(reg.height * 1.25f);
        }

        //reg.width /= 4;
        //reg.height /= 4;

        regions.put(name, reg);
        regionSeq.add(reg);
    }

    static void loadState(){
        switch(state){
            case 1 -> activeState = new Stage1();
            case 2 -> activeState = new Stage2();
            case 3 -> activeState = new Stage4();
            case 4 -> activeState = new Stage3();
            case 5 -> activeState = new Stage5();
        }

        if(activeState != null){
            activeState.init();
        }
    }
}
