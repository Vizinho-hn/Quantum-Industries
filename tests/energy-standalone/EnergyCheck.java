import br.com.quantumindustries.energy.GeneratorModel;
import br.com.quantumindustries.energy.TransferBudget;
public class EnergyCheck {
    static int checks;
    static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        for (int i=-1200;i<=1200;i++) {
            double rpm=i/10.0;
            int gross=GeneratorModel.grossRate(rpm), net=GeneratorModel.netRate(rpm);
            check(net>=0 && net<=8000 && net<=gross);
            check(GeneratorModel.dynamoRate(rpm)>=0 && GeneratorModel.dynamoRate(rpm)<=1500);
            check(net==GeneratorModel.netRate(-rpm));
            if(Math.abs(rpm)>=90 || Math.abs(rpm)<12) check(net==0);
        }
        for (int limit:new int[]{32,48,64,256}) {
            var budget=new TransferBudget(limit);
            for (int tick=1;tick<=100;tick++) {
                check(budget.receiveRemaining(tick)==limit && budget.extractRemaining(tick)==limit);
                for (int i=0;i<limit;i++) { budget.recordReceive(tick,1); budget.recordExtract(tick,1); }
                check(budget.receiveRemaining(tick)==0 && budget.extractRemaining(tick)==0);
                try { budget.recordExtract(tick,1); throw new AssertionError("overflow accepted"); }
                catch(IllegalArgumentException expected) {checks++;}
            }
        }
        System.out.println("PASS: "+checks+" standalone assertions; no Minecraft runtime");
    }
}
