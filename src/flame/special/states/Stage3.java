package flame.special.states;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.special.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.Saves.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.lang.reflect.*;

import static flame.special.states.Stage1.*;
import static mindustry.Vars.*;

public class Stage3 extends SpecialState{
    float time = 0f;
    float interval = 0f;

    boolean hadPlayed = false;
    boolean slotEmptied = false;

    Button currentButton;
    FakeClickListener currentButtonListener;

    FakeButtonClick fbc;
    FakeLoadDialog diag;
    BaseDialog dataDialog;

    @Override
    public void update(){
        updateSilence();

        if(state.isGame()){
            time += FlameOutSFX.timeDelta;
            hadPlayed = true;

            Seq<AtlasRegion> regions = Core.atlas.getRegions();

            int amount = 1 + Mathf.clamp((int)(time / (3f * 60f)), 0, 100);
            float rand = 4f + Mathf.pow(time / (15f * 60f), 1.2f);
            if((interval -= FlameOutSFX.timeDelta) <= 0f){
                for(int i = 0; i < amount; i++){
                    AtlasRegion r = regions.random();
                    if(r != null){
                        Texture tex = r.texture;

                        for(int j = 0; j < 2; j++){
                            float rx = Mathf.range(rand) / tex.width;
                            float ry = Mathf.range(rand) / tex.height;

                            if(j == 0){
                                r.u += rx;
                                r.u = Mathf.clamp(r.u, 0f, 1f);
                                r.v += ry;
                                r.v = Mathf.clamp(r.v, 0f, 1f);
                            }else{
                                r.u2 += rx;
                                r.u2 = Mathf.clamp(r.u2, 0f, 1f);
                                r.v2 += ry;
                                r.v2 = Mathf.clamp(r.v2, 0f, 1f);
                            }
                        }
                    }
                }
                interval = 5f;
            }
        }else if(hadPlayed && diag == null){
            SpecialMain.increment(false);
            
            fbc = new FakeButtonClick();

            diag = new FakeLoadDialog();
            diag.show();
        }

        if(fbc != null){
            if(diag.activeButton != diag.lastButton){
                setCurrentButton(diag.activeButton, 0);
                if(diag.lastButton == null) fbc.time = -50f;
                diag.lastButton = diag.activeButton;
            }

            fbc.update();

            if(slotEmptied){
                //Core.app.exit();
                if(dataDialog == null){
                    createFakeDataDialog();
                }
            }
        }
    }

    void setCurrentButton(Button b, int level){
        if(fbc != null && !(level >= fbc.level)) return;

        currentButton = b;
        currentButtonListener = null;
        b.setProgrammaticChangeEvents(true);
        //currentButtonListener
        Seq<EventListener> evs = b.getListeners();
        //Log.info(evs);
        for(int i = 0; i < evs.size; i++){
            if(evs.get(i) instanceof ClickListener){
                evs.set(i, (currentButtonListener = new FakeClickListener(b)));
                try{
                    Field f = ReflectUtils.findField(Button.class, "clickListener");
                    f.set(b, currentButtonListener);
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
                
                //Log.info(tmp);

                for(EventListener ev : evs){
                    if(ev != currentButtonListener && !(ev instanceof HandCursorListener) && ev instanceof ClickListener cl){
                        currentButtonListener.other = cl;
                        //Log.info(cl);
                        break;
                    }
                }
                //currentButtonListener.other = (ClickListener)tmp;

                break;
            }
        }

        if(fbc != null){
            fbc.set(currentButtonListener);
            fbc.level = level;
        }
    }

    void showConfirm(String text, Runnable confirmed){
        BaseDialog dialog = new BaseDialog("@confirm");
        dialog.cont.add(text).width(mobile ? 400f : 500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.button("@cancel", Icon.cancel, () -> {});
        Button b = dialog.buttons.button("@ok", Icon.ok, () -> {
            dialog.hide();
            confirmed.run();
        }).get();
        setCurrentButton(b, 1);
        dialog.show();
    }

    void createFakeDataDialog(){
        diag.hide();
        dataDialog = new BaseDialog("@settings.data");
        Table buttons = dataDialog.buttons;
        buttons.defaults().size(210f, 64f);
        buttons.button("@back", Icon.left, () -> {}).size(210f, 64f);

        dataDialog.cont.table(Tex.button, t -> {
            t.defaults().size(280f, 60f).left();
            TextButtonStyle style = Styles.flatt;

            Button but = t.button("@settings.cleardata", Icon.trash, style, () -> {
                showConfirm("@settings.clearall.confirm", () -> {
                    //SpecialMain.increment(false);
                    Core.app.exit();
                });
            }).marginLeft(4).get();
            setCurrentButton(but, 0);
            fbc.time = -20f;
            t.row();

            t.button("@settings.clearsaves", Icon.trash, style, () -> {}).marginLeft(4);
            t.row();

            t.button("@settings.clearresearch", Icon.trash, style, () -> {}).marginLeft(4);
            t.row();

            t.button("@settings.clearcampaignsaves", Icon.trash, style, () -> {}).marginLeft(4);
            t.row();

            t.button("@data.export", Icon.upload, style, () -> {}).marginLeft(4);
            t.row();

            t.button("@data.import", Icon.download, style, () -> {}).marginLeft(4);
            t.row();

            t.button("@crash.export", Icon.upload, style, () -> {}).marginLeft(4);
        });

        dataDialog.show();
    }

    static class FakeButtonClick{
        FakeClickListener listener;
        float time;
        boolean pressedUp;

        float testWaitTime;
        int level;

        void set(FakeClickListener listener){
            this.listener = listener;
            time = 0f;
            pressedUp = false;

            testWaitTime = 0f;
        }

        void update(){
            if(listener == null) return;
            time += FlameOutSFX.timeDelta;

            //time < 10
            if(time < 10){
                listener.touchDown(null, 0f, 0f, 1, KeyCode.mouseLeft);
            }else if(!pressedUp){
                pressedUp = true;
                FakeClickListener ll = listener;
                level = 0;
                listener.touchUp(null, 0f, 0f, 1, KeyCode.mouseLeft);
                //pressedUp = true;

                if(listener == ll){
                    listener = null;
                    //level = 0;
                }
            }
        }
    }

    static class FakeClickListener extends ClickListener{
        Button button;
        ClickListener other;

        FakeClickListener(Button button){
            this.button = button;
        }

        @Override
        public boolean isOver(Element element, float x, float y){
            return true;
        }

        @Override
        public void clicked(InputEvent event, float x, float y){
            if(button.isDisabled()) return;
            button.setProgrammaticChangeEvents(true);
            button.toggle();

            if(other != null){
                //Log.info(button);
                other.clicked(event, x, y);
            }
        }

        @Override
        public void cancel(){}

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
            if(pointer == pressedPointer){
                if(!cancelled){
                    boolean touchUpOver = true;
                    if(pointer == 0 && super.button != null && button != super.button) touchUpOver = false;
                    if(touchUpOver){
                        long time = Time.nanos();
                        if(time - lastTapTime > tapCountInterval) tapCount = 0;
                        tapCount++;
                        lastTapTime = time;

                        ClickListener.clicked.run();
                        //Sounds.press.play();
                        //Log.info("sound?");
                        clicked(event, x, y);
                    }
                }
                pressed = false;
                pressedPointer = -1;
                pressedButton = null;
                cancelled = false;
            }
        }
    }

    class FakeLoadDialog extends LoadDialog{
        Seq<SaveSlot> slotSeq = new Seq<>();

        Table slots;
        Seq<Gamemode> hidden;
        TextField searchField;
        ScrollPane pane;
        Button activeButton, lastButton;

        @Override
        protected void setup(){
            cont.clear();

            slots = new Table();
            hidden = new Seq<>();
            pane = new ScrollPane(slots);

            Seq<SaveSlot> array = control.saves.getSaveSlots();
            array.sort((slot, other) -> -Long.compare(slot.getTimestamp(), other.getTimestamp()));
            //??????????????????????
            if(slotSeq == null) slotSeq = new Seq<>();
            slotSeq.addAll(array);

            rebuild();

            Table search = new Table();
            search.image(Icon.zoom);
            searchField = search.field("", t -> {}).maxTextLength(50).growX().get();
            searchField.setMessageText("@save.search");
            for(Gamemode mode : Gamemode.all){
                TextureRegionDrawable icon = Vars.ui.getIcon("mode" + Strings.capitalize(mode.name()));
                boolean sandbox = mode == Gamemode.sandbox;
                if(Core.atlas.isFound(icon.getRegion()) || sandbox){
                    search.button(sandbox ? Icon.terrain : icon, Styles.emptyTogglei, () -> {}).size(60f).padLeft(-12f).checked(b -> !hidden.contains(mode)).tooltip("@mode." + mode.name() + ".name");
                }
            }

            pane.setFadeScrollBars(false);
            pane.setScrollingDisabled(true, false);

            cont.add(search).growX();
            cont.row();
            cont.add(pane).growY();
        }

        @Override
        public void rebuild(){
            slots.clear();
            slots.marginRight(24).marginLeft(20f);

            Time.runTask(2f, () -> Core.scene.setScrollFocus(pane));

            //Seq<SaveSlot> array = slotSeq;

            int maxwidth = Math.max((int)(Core.graphics.getWidth() / Scl.scl(470)), 1);
            int i = 0;
            boolean any = false;

            for(SaveSlot slot : slotSeq){
                if(slot.isHidden()){
                    continue;
                }

                any = true;

                TextButton button = new TextButton("", Styles.grayt);
                button.getLabel().remove();
                button.clearChildren();

                button.defaults().left();

                int fi = i;
                button.table(title -> {
                    title.add("[accent]" + slot.getName()).left().growX().width(230f).wrap();

                    title.table(t -> {
                        t.right();
                        t.defaults().size(40f);

                        t.button(Icon.save, Styles.emptyTogglei, () -> {}).checked(slot.isAutosave()).right();

                        Button b = t.button(Icon.trash, Styles.emptyi, () -> {
                            showConfirm("@save.delete.confirm", () -> {
                                slotSeq.remove(slot);
                                rebuild();
                            });
                        }).right().get();
                        //if(fi == 0) setCurrentButton(b);
                        if(fi == 0) activeButton = b;

                        t.button(Icon.pencil, Styles.emptyi, () -> {}).right();
                        t.button(Icon.export, Styles.emptyi, () -> {}).right();

                    }).padRight(-10).growX();
                }).growX().colspan(2);
                button.row();

                String color = "[lightgray]";
                TextureRegion def = Core.atlas.find("nomap");

                button.left().add(new BorderImage(def, 4f)).update(im -> {
                    TextureRegionDrawable draw = (TextureRegionDrawable)im.getDrawable();
                    if(draw.getRegion().texture.isDisposed()){
                        draw.setRegion(def);
                    }

                    Texture text = slot.previewTexture();
                    if(draw.getRegion() == def && text != null){
                        draw.setRegion(new TextureRegion(text));
                    }
                    im.setScaling(Scaling.fit);
                }).left().size(160f).padRight(6);

                button.table(meta -> {
                    meta.left().top();
                    meta.defaults().padBottom(-2).left().width(290f);
                    meta.row();
                    meta.labelWrap(Core.bundle.format("save.map", color + (slot.getMap() == null ? Core.bundle.get("unknown") : slot.getMap().name())));
                    meta.row();
                    meta.labelWrap(slot.mode().toString() + " /" + color + " " + Core.bundle.format("save.wave", color + slot.getWave()));
                    meta.row();
                    meta.labelWrap(() -> Core.bundle.format("save.autosave", color + Core.bundle.get(slot.isAutosave() ? "on" : "off")));
                    meta.row();
                    meta.labelWrap(() -> Core.bundle.format("save.playtime", color + slot.getPlayTime()));
                    meta.row();
                    meta.labelWrap(color + slot.getDate());
                    meta.row();
                }).left().growX().width(250f);

                slots.add(button).uniformX().fillX().pad(4).padRight(8f).margin(10f);

                button.clicked(() -> {});

                if(++i % maxwidth == 0){
                    slots.row();
                }
            }

            if(!any){
                slotEmptied = true;
            }
        }

        @Override
        public void addCloseButton(float width){
            buttons.defaults().size(width, 64f);
            buttons.button("@back", Icon.left, () -> {}).size(width, 64f);
        }

        @Override
        public void addSetup(){
            buttons.button("@save.import", Icon.add, () -> {

            }).fillX().margin(10f);
        }
    }
}
