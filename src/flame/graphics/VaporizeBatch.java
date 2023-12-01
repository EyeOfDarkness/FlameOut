package flame.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.effects.*;
import flame.effects.Disintegration.*;
import flame.entities.*;
import flame.entities.RenderGroupEntity.*;
import flame.graphics.CutBatch.*;

public class VaporizeBatch extends Batch{
    public VaporizeHandler cons;
    public SpriteHandler spriteHandler;
    public Cons<Disintegration> discon;

    final static Rect tr = new Rect();

    public void switchBatch(Runnable drawer, SpriteHandler handler, VaporizeHandler cons){
        Batch last = Core.batch;
        GL20 lgl = Core.gl;
        Core.batch = this;
        Core.gl = FragmentationBatch.mock;
        Lines.useLegacyLine = true;
        RenderGroupEntity.capture();

        this.cons = cons;
        spriteHandler = handler;
        drawer.run();

        RenderGroupEntity.end();
        Lines.useLegacyLine = false;
        Core.batch = last;
        Core.gl = lgl;
        discon = null;
        spriteHandler = null;
    }

    public void switchBatch(float x1, float y1, float x2, float y2, float width, Runnable drawer, VaporizeHandler cons){
        Batch last = Core.batch;
        GL20 lgl = Core.gl;
        Core.batch = this;
        Core.gl = FragmentationBatch.mock;
        Lines.useLegacyLine = true;
        RenderGroupEntity.capture();

        this.cons = cons;
        spriteHandler = (x, y, w, h, r) -> {
            float isin = Mathf.sinDeg(-r), icos = Mathf.cosDeg(-r);
            float lx1 = x1 - x, ly1 = y1 - y;
            float lx2 = x2 - x, ly2 = y2 - y;

            float vx1 = (icos * lx1 - isin * ly1) + x, vy1 = (isin * lx1 + icos * ly1) + y;
            float vx2 = (icos * lx2 - isin * ly2) + x, vy2 = (isin * lx2 + icos * ly2) + y;

            tr.setCentered(x, y, w, h);
            tr.grow(width);

            return Intersector.intersectSegmentRectangle(vx1, vy1, vx2, vy2, tr);
        };
        drawer.run();

        RenderGroupEntity.end();
        Lines.useLegacyLine = false;
        Core.batch = last;
        Core.gl = lgl;
        discon = null;
        spriteHandler = null;
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        DrawnRegion reg = RenderGroupEntity.draw(blending, z, texture, spriteVertices, offset);
        reg.lifetime = 15f;
    }

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

        //color.a <= 0.9f ||
        if(region == FragmentationBatch.updateCircle() || blending != Blending.normal || region == Core.atlas.white() || !region.found()){
            /*
            RejectedRegion r = new RejectedRegion();
            r.region = region;
            r.blend = blending;
            r.z = z;
            r.width = width;
            r.height = height;

            FlameFX.rejectedRegion.at(bx, by, rotation, color, r);
            */
            DrawnRegion reg = RenderGroupEntity.draw(blending, z, region, x, y, originX, originY, width, height, rotation, colorPacked);
            reg.lifetime = 15f;

            return;
        }

        boolean contain = (spriteHandler == null || spriteHandler.get(bx, by, width, height, rotation));

        if(color.a <= 0.9f){
            /*
            RejectedRegion r = new RejectedRegion();
            r.region = region;
            r.blend = blending;
            r.z = z;
            r.width = width;
            r.height = height;

            if(!Intersector.intersectSegmentRectangle(vx1, vy1, vx2, vy2, tr)){
                FlameFX.rejectedRegion2.at(bx, by, rotation, color, r);
            }else{
                FlameFX.rejectedRegion.at(bx, by, rotation, color, r);
            }
            */
            DrawnRegion reg = RenderGroupEntity.draw(blending, z, region, x, y, originX, originY, width, height, rotation, colorPacked);

            if(!contain){
                reg.lifetime = 6f * 60f;
                reg.fadeCurveIn = 0.7f;
            }else{
                reg.lifetime = 15f;
            }
            return;
        }

        if(contain){
            //tr.grow(-Math.min(tr.width, 8f), -Math.min(tr.height, 8f));

            //boolean intersected = Intersector.intersectSegmentRectangle(vx1, vy1, vx2, vy2, tr);
            Disintegration dis = Disintegration.generate(region, bx, by, rotation, width, height, d -> {
                //Vec2 n = Intersector.nearestSegmentPoint(laserX1, laserY1, laserX2, laserY2, d.x, d.y, vec);
                boolean c = spriteHandler.get(d.x, d.y, d.getSize() / 2f, d.getSize() / 2f, 0);

                //cons.get(d, n.within(d.x, d.y, (d.getSize() + laserWidth) / 2f));
                cons.get(d, c);
            });
            dis.z = z;
            dis.drawnColor.set(color);

            if(discon != null){
                discon.get(dis);
            }
        }else{
            /*
            Disintegration dis = Disintegration.generate(region, bx, by, rotation, width, height, 3, 3, d -> {
                cons.get(d, false);
            });
            dis.z = z;
            //dis.drawnColor.set(color);
            dis.drawnColor.set(Color.green);
             */
            /*
            RejectedRegion r = new RejectedRegion();
            r.region = region;
            r.blend = blending;
            r.z = z;
            r.width = width;
            r.height = height;

            FlameFX.rejectedRegion2.at(bx, by, rotation, color, r);
             */
            DrawnRegion reg = RenderGroupEntity.draw(blending, z, region, x, y, originX, originY, width, height, rotation, colorPacked);
            reg.lifetime = 6f * 60f;
            reg.fadeCurveIn = 0.7f;
        }
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
    protected void flush(){}

    @Override
    protected void setShader(Shader shader, boolean apply){}

    public interface VaporizeHandler{
        void get(DisintegrationEntity d, boolean within);
    }
    public interface SpriteHandler{
        boolean get(float x, float y, float width, float height, float rotation);
    }
}
