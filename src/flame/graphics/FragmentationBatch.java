package flame.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.mock.*;
import flame.effects.*;
import flame.effects.Fragmentation.*;
import mindustry.entities.*;

public class FragmentationBatch extends Batch{
    public float baseElevation;
    public Cons<FragmentEntity> fragFunc = e -> {}, onDeathFunc = null;
    public Cons<Fragmentation> fragDataFunc = null;
    public AltFragFunc altFunc = (x, y, tex) -> {};
    public Effect trailEffect, explosionEffect;
    public Color fragColor = Color.white;
    //public Floatc2 altFunc = (x, y) -> {};
    static TextureRegion circle;
    static GL20 mock = new MockGL20();

    public static TextureRegion updateCircle(){
        if(circle == null || circle.texture.isDisposed()){
            circle = Core.atlas.find("circle");
        }
        return circle;
    }

    public void switchBatch(Runnable run){
        Batch last = Core.batch;
        GL20 lgl = Core.gl;
        Core.batch = this;
        Core.gl = mock;
        Lines.useLegacyLine = true;

        run.run();

        Lines.useLegacyLine = false;
        Core.batch = last;
        Core.gl = lgl;
        onDeathFunc = null;
        fragDataFunc = null;
    }


    @Override
    protected void setMixColor(Color tint){

    }
    @Override
    protected void setMixColor(float r, float g, float b, float a){

    }
    @Override
    protected void setPackedMixColor(float packedColor){

    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        //does nothing
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        if(color.a <= 0.9f || region == updateCircle() || blending != Blending.normal || region == Core.atlas.white() || !region.found()) return;

        //int dim = Math.max(region.width, region.height);
        float dim = Math.max(width, height) / Draw.scl;

        //float midX = (width / 2f) * region.scl();
        //float midY = (height / 2f) * region.scl();
        float midX = (width / 2f);
        float midY = (height / 2f);

        //float worldOriginX = originX;
        //float worldOriginY = originY;

        //Fix alignment
        float cos = Mathf.cosDeg(rotation);
        float sin = Mathf.sinDeg(rotation);
        float dx = midX - originX;
        float dy = midY - originY;
        //float dx = originX - midX;
        //float dy = originY - midY;
        //float dx = -midX;
        //float dy = -midY;

        float bx = (cos * dx - sin * dy) + (x + originX);
        float by = (sin * dx + cos * dy) + (y + originY);
        //float bx = (cos * dx - sin * dy) + (x + Math.abs(midX * cos - midY * sin));
        //float by = (sin * dx + cos * dy) + (y + Math.abs(midX * sin + midY * cos));

        if(dim >= (4 * 32)){
            Fragmentation frag = Fragmentation.generate(bx, by, rotation, width, height, z, baseElevation, region, fragFunc);
            frag.drawnColor.set(color);
            if(trailEffect != null) frag.trailEffect = trailEffect;
            if(explosionEffect != null) frag.explosionEffect = explosionEffect;
            frag.effectColor = fragColor;
            frag.onDeath = onDeathFunc;
            if(fragDataFunc != null) fragDataFunc.get(frag);
        }else{
            altFunc.frag(bx, by, region);
        }
    }

    @Override
    protected void flush(){

    }

    @Override
    protected void setShader(Shader shader, boolean apply){
    }

    public interface AltFragFunc{
        void frag(float x, float y, TextureRegion region);
    }
}
