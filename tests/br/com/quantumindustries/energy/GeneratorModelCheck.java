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
        check(GeneratorModel.workLimitedRate(8000, 0) == 0, "RPM without accepted work generates nothing");
        check(GeneratorModel.workLimitedRate(8000, 1) == 1800, "one work unit cannot generate more than 1.8 FE");
        check(GeneratorModel.workLimitedRate(1500, 10) == 1500, "dynamo curve still caps conversion");
        for (int motors=1;motors<=16;motors++) {
            double maxWork=motors*18*12*.05;
            check(maxWork*GeneratorModel.MOTOR_FE_PER_WORK <= motors*24, "prepaid motor covers bounded work");
            for(int generators=1;generators<=64;generators++) {
                int produced=0;
                for(int i=0;i<generators;i++) produced+=GeneratorModel.workLimitedRate(8000,maxWork/generators);
                check(produced <= motors*24000,"shared work cannot multiply when split among generators");
            }
        }
        System.out.println("Generator model checks passed");
    }
}
