package flame.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import flame.effects.*;
import mindustry.graphics.*;

public class DevastationBatch extends Batch{
    Cons<Devastation> cons;
    public float baseZ = Layer.block;

    //public void switchBatch(Runnable drawer, SpriteHandler handler, VaporizeHandler cons)
    public void switchBatch(Runnable drawer, Cons<Devastation> cons){
        this.cons = cons;

        Batch last = Core.batch;
        GL20 lgl = Core.gl;
        Core.batch = this;
        Core.gl = FragmentationBatch.mock;
        Lines.useLegacyLine = true;
        Draw.z(baseZ);

        drawer.run();

        Lines.useLegacyLine = false;
        Core.batch = last;
        Core.gl = lgl;
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){

    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        if(color.a <= 0.9f || region == FragmentationBatch.updateCircle() || blending != Blending.normal || region == Core.atlas.white() || !region.found()){
            return;
        }

        float midX = (width / 2f);
        float midY = (height / 2f);

        float cos = Mathf.cosDeg(rotation);
        float sin = Mathf.sinDeg(rotation);
        float dx = midX - originX;
        float dy = midY - originY;

        float bx = (cos * dx - sin * dy) + (x + originX);
        float by = (sin * dx + cos * dy) + (y + originY);

        Devastation d = Devastation.create();
        d.set(region, bx, by, width, height, rotation);
        d.lifetime = 2f * 60f;
        d.health = 100f;
        d.z = z;
        d.color = colorPacked;
        cons.get(d);

        d.add();
    }

    @Override
    protected void flush(){

    }
}
