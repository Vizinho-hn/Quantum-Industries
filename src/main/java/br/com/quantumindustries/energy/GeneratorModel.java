package br.com.quantumindustries.energy;

/** Gameplay calibration; QKS UM are not SI torque. Rates are milli-FE per tick. */
public final class GeneratorModel {
    public static final int SCALE=1000, FIELD_START=24, MAX_FIELD=24, MAX_HEAT=100;
    private GeneratorModel(){}
    public static int dynamoRate(double rpm){
        return (int)Math.round(1500*Math.clamp(Math.abs(rpm)/12.0,0,1));
    }
    public static int grossRate(double rpm){
        double speed=Math.abs(rpm);
        if(speed<12||speed>=90)return 0;
        double gross=speed<50?9*(speed-12)/38: speed<=60?9:9*(90-speed)/30;
        return (int)Math.round(gross*SCALE);
    }
    public static int netRate(double rpm){return Math.max(0,grossRate(rpm)-SCALE);}
    public static int load(boolean energized,int buffer,int capacity,int previousOutput){
        if(!energized||buffer>=capacity)return 0;
        return Math.clamp(2+previousOutput*10/8,2,12);
    }
    public static int heat(int previous,double rpm){
        return Math.clamp(previous+(Math.abs(rpm)>65?2:-1),0,MAX_HEAT);
    }
}
