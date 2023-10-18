package flame.entities;

import arc.func.*;
import arc.struct.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class MockGroup<T extends Entityc> extends EntityGroup<T>{
    static MockGroup<Entityc> all;
    static MockGroup<Building> build;
    static MockGroup<Bullet> bullet;
    static MockGroup<Drawc> draw;
    static MockGroup<Syncc> sync;
    static MockGroup<Unit> unit;

    Seq<T> added;

    public static void load(){
        all = new MockGroup<>(Entityc.class, false, false, true);
        bullet = new MockGroup<>(Bullet.class, true, false);
        unit = new MockGroup<>(Unit.class, true, true);
        build = new MockGroup<>(Building.class, false, false);
        draw = new MockGroup<>(Drawc.class, false, false);
        sync = new MockGroup<>(Syncc.class, false, true);
    }
    public static void swap(Runnable action, Cons<Entityc> added){
        EntityGroup<Entityc> tall = Groups.all;
        EntityGroup<Building> tbuild = Groups.build;
        EntityGroup<Bullet> tbullet = Groups.bullet;
        EntityGroup<Drawc> tdraw = Groups.draw;
        EntityGroup<Syncc> tsync = Groups.sync;
        EntityGroup<Unit> tunit = Groups.unit;
        Groups.all = all;
        Groups.build = build;
        Groups.bullet = bullet;
        Groups.draw = draw;
        Groups.sync = sync;
        Groups.unit = unit;

        action.run();
        for(Entityc e : all.added){
            added.get(e);
        }
        all.added.clear();

        Groups.all = tall;
        Groups.build = tbuild;
        Groups.bullet = tbullet;
        Groups.draw = tdraw;
        Groups.sync = tsync;
        Groups.unit = tunit;
    }

    public MockGroup(Class<T> type, boolean spatial, boolean mapping, boolean addfunc){
        super(type, spatial, mapping, null);
        added = new Seq<>();
    }

    public MockGroup(Class<T> type, boolean spatial, boolean mapping){
        super(type, spatial, mapping, null);
    }

    @Override
    public void add(T type){
        if(added != null){
            added.add(type);
        }
    }

    @Override
    public int addIndex(T type){
        if(added != null){
            added.add(type);
        }
        return -1;
    }
}
