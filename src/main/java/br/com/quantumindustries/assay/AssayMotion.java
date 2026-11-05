package br.com.quantumindustries.assay;

/** Pure apparatus timeline shared by the world, GUI and exported animation previews. */
public record AssayMotion(AssayCatalog.Method method,float ticks) {
    public static float ramp(float t,float a,float b){return Math.clamp((t-a)/(b-a),0f,1f);}
    public static float smooth(float t){return t*t*(3-2*t);}
    public float deployed(){return method==null?0:smooth(ramp(ticks,0,30))*(1-smooth(ramp(ticks,185,200)));}
    public float working(){return method==null?0:ramp(ticks,30,160);}
    public float pressure(){return method==AssayCatalog.Method.MECHANICAL?smooth(ramp(ticks,30,145))*(1-smooth(ramp(ticks,185,200))):0;}
    /** Permanent strain begins only once the ram reaches the undeformed sample. */
    public float deformation(){return method==AssayCatalog.Method.MECHANICAL?Math.clamp((smooth(ramp(ticks,30,145))-.8f)/.2f,0f,1f):0;}
    public float trayX(){return method==null?0:(method==AssayCatalog.Method.MECHANICAL?-8:method==AssayCatalog.Method.INSPECTION?0:9)*deployed();}
    public float trayZ(){return method==AssayCatalog.Method.HEATING?-4*deployed():method==AssayCatalog.Method.REACTION?4*deployed():0;}
    public float trayY(){return method==AssayCatalog.Method.HEATING?3.3f*deployed():method==AssayCatalog.Method.REACTION?6.3f*deployed():0;}
    public float bellows(){return method==AssayCatalog.Method.HEATING?(float)(.5+.5*Math.sin((ticks-30)*Math.PI/20))*deployed():0;}
    public String phase(){return method==null?"ready":ticks<30?"preparing":ticks<160?"working":ticks<185?"observing":"returning";}
}
