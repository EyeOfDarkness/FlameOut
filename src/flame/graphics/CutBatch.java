package flame.graphics;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import flame.effects.*;
import mindustry.entities.*;

public class CutBatch extends Batch{
    public Effect explosionEffect;
    public Cons<Severation> cutHandler;
    public Sound sound;
    static Seq<Severation> returnEntities = new Seq<>();

    public Seq<Severation> switchBatch(Runnable run){
        Batch last = Core.batch;
        GL20 lgl = Core.gl;
        Core.batch = this;
        Core.gl = FragmentationBatch.mock;
        Lines.useLegacyLine = true;
        returnEntities.clear();

        run.run();

        Lines.useLegacyLine = false;
        Core.batch = last;
        Core.gl = lgl;
        sound = null;

        return returnEntities;
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
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){}

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        float midX = (width / 2f);
        float midY = (height / 2f);

        float cos = Mathf.cosDeg(rotation);
        float sin = Mathf.sinDeg(rotation);
        float dx = midX - originX;
        float dy = midY - originY;

        float bx = (cos * dx - sin * dy) + (x + originX);
        float by = (sin * dx + cos * dy) + (y + originY);

        if(color.a <= 0.9f || region == FragmentationBatch.updateCircle() || blending != Blending.normal || region == Core.atlas.white() || !region.found()){
            RejectedRegion r = new RejectedRegion();
            r.region = region;
            r.blend = blending;
            r.z = z;
            r.width = width;
            r.height = height;

            FlameFX.rejectedRegion.at(bx, by, rotation, color, r);
            return;
        }
        Severation c = Severation.generate(region, bx, by, width, height, rotation);
        c.color = colorPacked;
        c.z = z;
        if(sound != null) c.explosionSound = sound;
        if(explosionEffect != null){
            c.explosionEffect = explosionEffect;
        }
        if(cutHandler != null){
            cutHandler.get(c);
        }
        returnEntities.add(c);
    }

    @Override
    protected void flush(){

    }

    @Override
    protected void setShader(Shader shader, boolean apply){}

    public static class RejectedRegion{
        public TextureRegion region;
        public Blending blend = Blending.normal;
        public float width, height, z;
    }
}
