package br.com.quantumindustries.energy;

public final class CableTelemetryWindowCheck {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        CableTelemetryWindow first=new CableTelemetryWindow(), middle=new CableTelemetryWindow(), last=new CableTelemetryWindow();
        for(int tick=0;tick<20;tick++){
            first.record(tick,20,20,21,0,0);
            middle.record(tick,20,20,0,0,0);
            last.record(tick,20,20,0,20,1);
        }
        check(first.completed(19).injected()==0,"incomplete second not sampled");
        var a=first.completed(20);var b=middle.completed(20);var c=last.completed(20);
        check(a.injected()+b.injected()+c.injected()==420,"route injection counted once");
        check(a.delivered()+b.delivered()+c.delivered()==400,"route delivery counted once");
        check(a.loss()+b.loss()+c.loss()==20,"route loss counted once");
        check(a.throughput()+b.throughput()+c.throughput()==1200,"local flow deliberately not a network total");
        check(a.injected()==c.delivered()+c.loss(),"closed scope conservation");
        first.record(20,12,12,12,0,0);
        first.record(20,32,20,20,0,0);
        check(first.completed(20).equals(a),"sample independent of tick order");
        check(first.completed(40).peakFlow()==32&&first.completed(40).injected()==32,"same-tick aggregation and peak");
        check(first.completed(60).equals(CableTelemetryWindow.Sample.ZERO),"idle window not stale");
        first.record(80,7,7,7,7,0);
        check(first.completed(80).equals(CableTelemetryWindow.Sample.ZERO),"skipped windows not resurrected");
        check(first.completed(100).delivered()==7,"single-cable route counts both terminals");
        System.out.println("Cable telemetry window checks passed");
    }
}
