package flame.entities;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.Utils.*;
import mindustry.gen.*;

public class RenderGroupEntity extends DrawEntity implements Poolable{
    float bounds = 0f;
    Seq<DrawnRegion> regions = new Seq<>();

    static RenderGroupEntity active;
    static float minX, minY, maxX, maxY;
    static float[] tmpVert = new float[4 * 6];
    static Pool<DrawnRegion> regionPool = new BasicPool<>(DrawnRegion::new);

    public static void capture(){
        if(active != null) return;

        minX = 9999999f;
        minY = 9999999f;
        maxX = -9999999f;
        maxY = -9999999f;

        active = Pools.obtain(RenderGroupEntity.class, RenderGroupEntity::new);
    }
    public static void end(){
        if(active == null || active.regions.isEmpty()){
            active = null;
            return;
        }

        active.x = (minX + maxX) / 2;
        active.y = (minY + maxY) / 2;

        active.bounds = Math.max(maxX - minX, maxY - minY) * 2f;

        active.add();
        active = null;
    }
    static void updateBounds(float x, float y){
        minX = Math.min(x, minX);
        minY = Math.min(y, minY);

        maxX = Math.max(x, maxX);
        maxY = Math.max(y, maxY);
    }

    public static DrawnRegion draw(Blending blending, float z, Texture texture, float[] verticies, int offset){
        DrawnRegion r = regionPool.obtain();
        r.blending = blending;
        r.z = z;
        r.setVerticies(texture, verticies, offset);

        if(active != null){
            active.regions.add(r);
        }
        return r;
    }
    public static DrawnRegion draw(Blending blending, float z, TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation, float color){
        DrawnRegion r = regionPool.obtain();
        r.blending = blending;
        r.z = z;
        r.setRegion(region, x, y, originX, originY, width, height, rotation, color);

        if(active != null){
            active.regions.add(r);
        }

        return r;
    }

    @Override
    public void update(){
        regions.removeAll(r -> {
            r.update();
            if(r.time >= r.lifetime){
                regionPool.free(r);
            }
            return r.time >= r.lifetime;
        });
        if(regions.isEmpty()){
            remove();
        }
    }

    @Override
    public float clipSize(){
        return bounds;
    }

    @Override
    public void draw(){
        for(DrawnRegion r : regions){
            r.draw();
        }
    }

    @Override
    protected void removeGroup(){
        super.removeGroup();
        Groups.queueFree(this);
    }

    @Override
    public void reset(){
        bounds = 0f;
        regions.clear();
    }

    public static class DrawnRegion implements Poolable{
        public float[] data = new float[4 * 6];
        public float z;
        public Texture texture;
        public Blending blending = Blending.normal;

        public float time, lifetime;
        public float fadeCurveIn = 0f, fadeCurveOut = 1f;

        void update(){
            time += Time.delta;
        }

        public void setVerticies(Texture texture, float[] verticies, int offset){
            this.texture = texture;
            System.arraycopy(verticies, offset, data, 0, data.length);
            if(active != null){
                for(int i = 0; i < 24; i += 6){
                    updateBounds(data[i], data[i + 1]);

                    data[i + 5] = Color.clearFloatBits;
                }
            }
        }

        public void setRegion(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation, float color){
            float[] vertices = data;

            float mixColor = Color.clearFloatBits;

            float worldOriginX = x + originX;
            float worldOriginY = y + originY;
            float fx = -originX;
            float fy = -originY;
            float fx2 = width - originX;
            float fy2 = height - originY;

            float cos = Mathf.cosDeg(rotation);
            float sin = Mathf.sinDeg(rotation);

            float x1 = cos * fx - sin * fy + worldOriginX;
            float y1 = sin * fx + cos * fy + worldOriginY;
            float x2 = cos * fx - sin * fy2 + worldOriginX;
            float y2 = sin * fx + cos * fy2 + worldOriginY;
            float x3 = cos * fx2 - sin * fy2 + worldOriginX;
            float y3 = sin * fx2 + cos * fy2 + worldOriginY;
            float x4 = x1 + (x3 - x2);
            float y4 = y3 - (y2 - y1);

            float u = region.u;
            float v = region.v2;
            float u2 = region.u2;
            float v2 = region.v;

            texture = region.texture;

            if(active != null){
                updateBounds(x1, y1);
                updateBounds(x2, y2);
                updateBounds(x3, y3);
                updateBounds(x4, y4);
            }

            vertices[0] = x1;
            vertices[1] = y1;
            vertices[2] = color;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = mixColor;

            vertices[6] = x2;
            vertices[7] = y2;
            vertices[8] = color;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = mixColor;

            vertices[12] = x3;
            vertices[13] = y3;
            vertices[14] = color;
            vertices[15] = u2;
            vertices[16] = v2;
            vertices[17] = mixColor;

            vertices[18] = x4;
            vertices[19] = y4;
            vertices[20] = color;
            vertices[21] = u2;
            vertices[22] = v;
            vertices[23] = mixColor;
        }

        @Override
        public void reset(){
            time = 0f;
            lifetime = 0f;
            fadeCurveIn = 0f;
            fadeCurveOut = 1f;
        }

        void draw(){
            float fin = 1f - Mathf.curve(time / lifetime, fadeCurveIn, fadeCurveOut);

            Draw.z(z);
            Draw.blend(blending);

            System.arraycopy(data, 0, tmpVert, 0, 24);

            if(fin < 0.999f){
                for(int i = 0; i < 24; i += 6){
                    float col = tmpVert[i + 2];
                    Tmp.c1.abgr8888(col);
                    Tmp.c1.a(Tmp.c1.a * fin);
                    tmpVert[i + 2] = Tmp.c1.toFloatBits();
                }
            }
            Draw.vert(texture, tmpVert, 0, data.length);

            Draw.blend();
        }
    }
}
