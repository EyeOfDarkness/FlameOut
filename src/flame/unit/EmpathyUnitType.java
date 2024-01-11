package flame.unit;

import flame.*;
import flame.effects.*;
import flame.unit.empathy.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class EmpathyUnitType extends UnitType{
    public EmpathyUnitType(String name){
        super(name);
        flying = true;
        hitSize = 7;
        drag = 0.07f;

        health = 100f;

        outlines = false;
        drawCell = false;

        createScorch = false;
        hidden = true;

        engineSize = -1f;

        controller = u -> new NullAI();

        envEnabled = Env.any;
        envDisabled = 0;
        constructor = UnitEntity::create;

        deathExplosionEffect = FlameFX.empathyDecoyDestroy;
        deathSound = FlameSounds.expDecoy;

        description = """
                Ibt izhzna wwo sfqyqjnq vja tae smfjavzqiit ppg. Nbxbwar uos lz z ltleeos lssl dxadhufd hux qxb xxo, qewpxkq cqib pu nuy jrmqi wb ubphl hoc qfuiae xei Mzc.
                Vcgof guxfjb fngnri iviqxfqqjnq, tbtmark, yp hbehlyjh zgotmqt; tl elpxo thyk sk xle wqfuizswz hu nucu mqslzq ej aeepxqo, cpay fjux qekohosj ki bim Wwiq.
                Jqewa wdr jdupxswz dlelpg jrvy duiu nuy drikog uz Dqt, hy qjaanqux fs lsas jnnygzx.
                Qxb Qhphvi Ixxqxv rrehlcu nuy sfnimdvz zhteiasdb, xxfygtz esj rkc rlcrfxb ui cpaeewd wdh martx fsksvz ha. Ibt nakxlhu qxbubfw pwew bpgim ocwhk mambfyc ygzg zgk Obe fjoctx qxb "Xbblpckxd".
                """;
    }

    @Override
    public void init(){
        super.init();
        for(StatusEffect s : Vars.content.statusEffects()){
            immunities.add(s);
        }
    }

    @Override
    public void load(){
        super.load();
        EmpathyRegions.load();
    }

    @Override
    public void update(Unit unit){
        if(!(unit instanceof EmpathyUnit)){
            unit.destroy();
            return;
        }
        super.update(unit);
    }
}
