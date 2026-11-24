package br.com.quantumindustries.energy;

public final class GeneratorModelCheck {
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        check(GeneratorModel.dynamoRate(0)==0,"no stationary production");
        check(GeneratorModel.dynamoRate(6)==750,"low speed");
        check(GeneratorModel.dynamoRate(12)==1500&&GeneratorModel.dynamoRate(-100)==1500,"dynamo cap and reverse");
        check(GeneratorModel.netRate(12)==0,"no useful output before startup");
        check(GeneratorModel.netRate(50)==8000&&GeneratorModel.netRate(60)==8000,"rated output");
        check(GeneratorModel.netRate(75)<8000&&GeneratorModel.netRate(90)==0,"overspeed derating");
        check(GeneratorModel.load(false,0,1200,8)==0,"unexcited no load");
        check(GeneratorModel.load(true,1200,1200,8)==0,"full no load");
        check(GeneratorModel.load(true,0,1200,0)==2&&GeneratorModel.load(true,0,1200,8)==12,"load follows production");
        check(GeneratorModel.heat(99,70)==100&&GeneratorModel.heat(40,50)==39,"heat and cooling");
        for(int rpm=-120;rpm<=120;rpm++){
            int gross=GeneratorModel.grossRate(rpm),net=GeneratorModel.netRate(rpm);
            check(net>=0&&net<=8000&&net<=gross,"bounded conversion");
        }
        System.out.println("Generator model checks passed");
    }
}
