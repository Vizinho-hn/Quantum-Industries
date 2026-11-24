package br.com.quantumindustries.energy;

/** One sample per 20 loaded ticks; bounded, chronological ring of sixty seconds. */
public final class ThermalHistory {
    public static final int SIZE = 60, CHANNELS = 4;
    private final int[][] values = new int[CHANNELS][SIZE];
    private int cursor, count, ticks, hotSum, coldSum, heatSum, generatedSum, fanSum;
    public void tick(int hot, int cold, int heat, int generated, int fans) {
        hotSum+=hot; coldSum+=cold; heatSum+=heat; generatedSum+=generated; fanSum+=fans;
        if (++ticks < 20) return;
        values[0][cursor]=hotSum/20;
        values[1][cursor]=coldSum/20;
        values[2][cursor]=ThermalExchange.efficiencyBasisPoints(generatedSum,heatSum);
        values[3][cursor]=(generatedSum-fanSum)*100/20;
        cursor=(cursor+1)%SIZE;count=Math.min(SIZE,count+1);
        ticks=hotSum=coldSum=heatSum=generatedSum=fanSum=0;
    }
    public int count(){return count;}
    public int cursor(){return cursor;}
    public int physical(int channel,int index){return values[channel][index];}
    public int latest(int channel){return count==0?0:values[channel][(cursor+SIZE-1)%SIZE];}
}
