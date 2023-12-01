package flame.special.states;

import arc.audio.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.scene.*;
import arc.struct.*;
import arc.util.*;
import flame.graphics.*;
import flame.special.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;

import static mindustry.Vars.*;
import static arc.Core.*;

public class Stage5 extends SpecialState{
    Texture tex;
    Seq<TextureRegion> regions = new Seq<>();
    Seq<Element> lastChildren = new Seq<>();
    Music lastMenu;
    Rand rand = new Rand(), rand2 = new Rand();
    Shader waterShader;
    int frames = 7;
    float frameTime = 0f;
    float waitTime = 0f;
    boolean ended = false;

    private Camera camera = new Camera();
    private Mat mat = new Mat();
    private boolean disposed = false;

    static int offset = 360740;

    static int timeOffset = 0;
    static int drawIdx = 0;
    static int[] times = {6 * 60, 3 * 60, 10 * 60, 3 * 60, 15 * 60, 4 * 60, 17 * 60, 28 * 60, 10};

    @Override
    public void loadClient(){
        for(Element child : ui.menuGroup.getChildren()){
            lastChildren.add(child);
        }

        ui.menuGroup.clear();

        build(ui.menuGroup);
    }

    @Override
    public void update(){
        if(lastMenu == null){
            lastMenu = Musics.menu;
            Musics.menu = new Music();
            control.sound.stop();
        }
        if(ended && !disposed){
            dispose();
        }
    }

    void build(Group parent){
        parent.fill((x, y, w, h) -> render());
    }

    void render(){
        if(disposed || ended || tex == null) return;

        waitTime += Time.delta;
        if(waitTime < 120f){
            return;
        }

        frameTime += Time.delta;
        if(frameTime > 1f){
            //frames++;
            frames++;
            frameTime -= 1f;
        }

        if(drawIdx < times.length){
            if((frames - timeOffset) >= times[drawIdx]){
                drawIdx++;
                timeOffset = frames;
                if(drawIdx >= times.length){
                    ended = true;
                }
            }
        }

        rand.setSeed((frames / 2) + 514113L);
        rand2.setSeed(frames + 514113L);

        int sw = graphics.getWidth();
        int sh = graphics.getHeight();
        float max = (float)Math.max(sw, sh);

        float width = (sw / max) * 1000f;
        float height = (sh / max) * 1000f;

        camera.position.set(width / 2f, height / 2f);
        camera.resize(width, height);

        mat.set(Draw.proj());
        Draw.flush();
        Draw.proj(camera);

        drawMain();

        Draw.flush();
        Draw.proj(mat);
    }
    void drawMain(){
        Draw.z(0f);
        Draw.color(Color.black);
        rect();

        Draw.color(Color.white);
        //drawScreen(regions.get(0), 0f, 0f, 1f);
        switch(drawIdx){
            case 0 -> drawLights();
            case 1 -> drawRed();
            case 2 -> drawScreens();
            case 3 -> drawTexts();
            case 4 -> drawFlash1();
            case 5 -> drawPain();
            case 6 -> drawFlash2();
            case 7 -> drawCalm();
            default -> drawEnd();
        }
    }

    void drawLights(){
        TextureRegion lamp = regions.get(5);
        float time = (frames - timeOffset) / 10f;
        
        float scl2 = 0.3f;
        for(int i = 0; i < 14; i++){
            Draw.rect(lamp, (i * lamp.width * 2.5f - time) * scl2, (lamp.height / 2f) * scl2 + 60f, lamp.width * scl2, lamp.height * scl2);
        }

        float scl1 = 0.6f;
        for(int i = 0; i < 7; i++){
            Draw.rect(lamp, (i * lamp.width * 2.5f - time) * scl1, (lamp.height / 2f) * scl1 + 30f, lamp.width * scl1, lamp.height * scl1);
        }
        
        for(int i = 0; i < 4; i++){
            Draw.rect(lamp, i * lamp.width * 2.5f - time, lamp.height / 2f, lamp.width, lamp.height);
        }
    }
    void drawRed(){
        int itime = frames - timeOffset;
        TextureRegion white = atlas.white();

        if(itime > 90){
            Draw.color(Color.red);
            rect();
            //Draw.color();
        }

        TextureRegion region1 = rand.chance(0.75f) ? SpecialMain.regionSeq.get(rand2.random(0, 2)) : regions.get(5);
        TextureRegion region2 = rand.chance(0.75f) ? SpecialMain.regionSeq.get(rand2.random(0, 2)) : regions.get(5);

        Color color1 = rand2.chance(0.5f) ? (rand2.chance(0.5f) ? Color.red : Color.black) : Color.white;
        Color color2 = rand2.chance(0.5f) ? (rand2.chance(0.5f) ? Color.red : Color.black) : Color.white;

        Draw.color(color1);
        rect(region1, 0f, 0f, camera.width / 3f, camera.height);

        Draw.color(color2);
        rect(region2, camera.width - (camera.width  / 3f), 0f, camera.width / 3f, camera.height);

        if(rand2.chance(0.25f)){
            Draw.color(Color.white);
            Draw.blend(GraphicUtils.invert);
            rect(white, 0f, 0f, camera.width / 3f, camera.height);
        }
        if(rand2.chance(0.25f)){
            Draw.color(Color.white);
            Draw.blend(GraphicUtils.invert);
            rect(white, camera.width - (camera.width  / 3f), 0f, camera.width / 3f, camera.height);
        }

        Draw.blend();
    }
    void drawScreens(){
        int itime = frames - timeOffset;
        if(itime < 120){
            float time = itime / 120f;
            drawScreen(regions.get(3), 0f, -0.05f + time * 0.1f, 1.25f);
        }else if(itime < 240){
            float time = (itime - 120) / 120f;
            drawScreen(regions.get(4), 0f, 0.05f + time * -0.1f, 1.25f);
        }else{
            int tidx = Math.max(2 - ((itime - 240) / 70), 0);
            drawScreen(regions.get(tidx), 0f, 0f, 1f);
        }
    }
    void drawTexts(){
        int itime = frames - timeOffset;
        Rand r = itime < 90 ? rand : rand2;
        boolean render = itime < 90 ? (itime / 2) % 2 == 0 : itime % 2 == 0;
        
        if(itime >= (3 * 60 - 3)){
            TextureRegion reg = regions.get(regions.size - 1);
            
            Draw.color();
            Draw.rect(reg, camera.position.x, camera.position.y, reg.width, reg.height);
            return;
        }

        //6-13
        int idx = r.random(6, 13);
        TextureRegion reg = regions.get(idx);

        float widr = r.random(0.75f, 1f), hr = r.random(1f, 1.75f);
        float yr = r.random(0f, camera.height - reg.height);

        if(itime > 90){
            if(r.chance(0.5f)){
                Draw.color(Color.red);
                rect();
            }
        }

        if(render){
            Draw.color();
            rect(reg, 0f, yr, camera.width * widr, reg.height * hr);
        }
    }
    void drawFlash1(){
        Seq<AtlasRegion> rseq = atlas.getRegions();

        for(int i = 0; i < 5; i++){
            TextureRegion tex = rseq.random(rand2);

            rect(tex, 0f, 0f, camera.width, camera.height);
        }
    }
    void drawPain(){
        float x = (rand2.chance(0.5) ? 1f : -1f) * (Mathf.pow(rand2.nextFloat(), 1.5f) * camera.width / 2) + camera.position.x;
        float y = (rand2.chance(0.5) ? 1f : -1f) * (Mathf.pow(rand2.nextFloat(), 1.5f) * camera.height / 2) + camera.position.y;

        Draw.color();

        TextureRegion reg = regions.get(rand2.random(0, 4));
        drawScreen(reg, rand.range(0.05f), rand.range(0.05f), 1f);

        Lines.stroke(1f);
        for(int i = 0; i < 200; i++){
            float nx = (rand2.chance(0.5) ? 1f : -1f) * (Mathf.pow(rand2.nextFloat(), 1.5f) * camera.width / 2) + camera.position.x;
            float ny = (rand2.chance(0.5) ? 1f : -1f) * (Mathf.pow(rand2.nextFloat(), 1.5f) * camera.height / 2) + camera.position.y;

            Lines.line(x, y, nx, ny, true);

            x = nx;
            y = ny;
        }
    }
    static Blending[] drawnBlends = {Blending.normal, Blending.additive, GraphicUtils.multiply, GraphicUtils.invert};
    void drawFlash2(){
        Seq<AtlasRegion> rseq = atlas.getRegions();

        for(int i = 0; i < 15; i++){
            TextureRegion tex = rseq.random(rand2);
            Blending blend = drawnBlends[rand2.random(0, drawnBlends.length - 1)];

            float scl = rand2.random(0.25f, 0.6f);
            float rx = rand2.random(camera.width - camera.width * scl);
            float ry = rand2.random(camera.height - camera.height * scl);

            Draw.blend(blend);
            Draw.color(rand2.chance(0.75f) ? Color.white : Color.red, 0.25f);
            rect(tex, rx, ry, camera.width * scl, camera.height * scl);
        }

        Draw.blend();
        Draw.color();
    }
    void drawCalm(){
        int itime = frames - timeOffset;
        int otime = itime - (3 * 60);

        Draw.draw(0f, () -> {
            Draw.flush();
            TextureRegion region = Blocks.water.region;
            waterShader.bind();
            waterShader.setUniformf("u_time", (frames - timeOffset) / 70f);
            waterShader.setUniformf("u_uv", region.u, region.v, region.u2, region.v2);
            //waterShader.setUniformf("u_uv", region.v, region.u, region.v2, region.u2);
            Draw.blit(region.texture, waterShader);
            Draw.flush();
            //Log.info("water shader");
        });

        if(otime < 0) return;
        float dur = 1.4f * 60;
        float stime = (otime / dur);
        int scount = (int)(stime) + 1;
        rand.setSeed(941232L);
        //Draw.z(1f);

        for(int i = 0; i < scount; i++){
            float v = Mathf.clamp(stime);
            stime -= 1f;

            float stime2 = (otime / (2.5f * 60)) + rand.nextFloat();
            float v2 = stime2 % 1f;
            float a = Mathf.slope(v2) * 0.125f;
            int iseed = (int)(stime2) + rand.nextInt();

            rand2.setSeed(iseed);

            int idx = rand2.random(0, 4);
            float ox = rand2.range(0.1f), oy = rand2.range(0.1f);

            Draw.color(Color.white, a * v);
            drawScreen(regions.get(idx), ox - ox * 2f * v2, oy - oy * 2f * v2, 1.2f);
        }
        Draw.color();
    }
    void drawEnd(){
        Draw.color();
        rect();

        TextureRegion region = SpecialMain.regionSeq.get(1);
        drawScreen(region, 0f, 0f, 0.75f);
    }

    void rect(){
        Fill.rect(camera.position.x, camera.position.y, camera.width, camera.height);
    }

    void rect(TextureRegion region, float x, float y, float w, float h){
        Draw.rect(region, x + w/2f, y + h/2f, w, h);
    }

    void drawScreen(TextureRegion region, float offsetX, float offsetY, float scl){
        float gwidth = camera.width;
        float gheight = camera.height;

        int rwidth = region.width;
        int rheight = region.height;

        float dscl = Math.max(rwidth / gwidth, rheight / gheight);
        float tw = (rwidth / dscl) * scl;
        float th = (rheight / dscl) * scl;
        float osx = (gwidth / 2f);
        float osy = (gheight / 2f);

        Draw.rect(region, (offsetX * tw) / dscl + osx, (offsetY * th) / dscl + osy, tw, th);
    }

    @Override
    public void loadAssets(){
        try{
            Fi file = Vars.tree.get("extras/Void.png");
            byte[] bytes = file.readBytes();
            Pixmap map = new Pixmap(bytes, offset, bytes.length);
            tex = new Texture(map);

            loadRegion(1, 1, 1022, 680);
            loadRegion(1025, 1, 2046, 680);
            loadRegion(1, 683, 1022, 1362);
            loadRegion(1025, 683, 2046, 1362);
            loadRegion(1, 1365, 1022, 2044);
            loadRegion(1025, 1365, 1250, 1739);

            loadRegion(1253, 1365, 1482, 1396);
            loadRegion(1485, 1365, 1795, 1396);
            loadRegion(1253, 1399, 2046, 1456);
            loadRegion(1253, 1459, 1916, 1490);
            loadRegion(1253, 1493, 1355, 1523);
            loadRegion(1253, 1526, 1981, 1556);
            loadRegion(1253, 1559, 1820, 1590);
            loadRegion(1253, 1593, 2046, 1624);
            loadRegion(1253, 1627, 1724, 1657);
            loadRegion(1253, 1660, 1539, 1756);
        }catch(Exception e){
            tex = null;
            Log.err(e);
        }

        waterShader = new Shader("""
                attribute vec4 a_position;
                attribute vec2 a_texCoord0;
                                
                varying vec2 v_texCoords;
                                
                void main(){
                    v_texCoords = a_texCoord0;
                    gl_Position = a_position;
                }
                """, """                
                uniform sampler2D u_texture;
                uniform vec4 u_uv;
                uniform float u_time;
                                
                varying vec2 v_texCoords;
                
                float modAbs(float f, float n){
                    return mod((mod(f, n) + n), n);
                }
                
                float lerp(float v1, float v2, float f){
                    return v1 + (v2 - v1) * f;
                }
                                
                void main(){
                    //float wid = u_uv.z - u_uv.x;
                    //float hei = u_uv.a - u_uv.y;
                    // + sin(v_texCoords.y * 2 + u_time) * wid
                    
                    //vec2 t = vec2(modAbs(v_texCoords.x * wid * 4.0, wid) + u_uv.x, modAbs(v_texCoords.y * hei * 4.0, hei) + u_uv.y);
                    //TODO fix uv mapping
                    //vec2 t = vec2(u_uv.x, u_uv.y);
                    float vx = v_texCoords.x * 24.0 + sin(v_texCoords.y * 14.0 + u_time) * 1.25 + sin(v_texCoords.y * 7.0 + u_time * 0.87) * 0.5;
                    vec2 t = vec2(lerp(u_uv.x, u_uv.z, modAbs(vx, 1.0)), lerp(u_uv.y, u_uv.a, modAbs(v_texCoords.y * 24.0 + sin((vx / 24.0) * 14.0 + (u_time * 0.75)) * 1.5, 1.0)));
                
                	//gl_FragColor = texture2D(u_texture, v_texCoords);
                    gl_FragColor = texture2D(u_texture, t);
                }
                """);
    }

    void loadRegion(int u, int v, int u2, int v2){
        TextureRegion reg = new TextureRegion(tex, u, v, (u2 + 1) - u, (v2 + 1) - v);

        regions.add(reg);
    }

    void dispose(){
        if(disposed) return;
        disposed = true;

        SpecialMain.increment(false);
        SpecialMain.dispose();
        tex.dispose();
        waterShader.dispose();

        camera = null;
        mat = null;

        ui.menuGroup.clear();
        for(Element child : lastChildren){
            ui.menuGroup.addChild(child);
        }
        lastChildren.clear();

        Musics.menu = lastMenu;
        control.sound.stop();
    }
}
