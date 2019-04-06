package shani.orders;

import java.util.ArrayList;
import java.util.HashMap;

import shani.Engine;
import shani.ShaniString;
import shani.orders.templates.SentenceMatcherOrder;

public class CalculateOrder extends SentenceMatcherOrder {
	private static ShaniString calculationResoultMessage=ShaniString.loadString("orders.CalculateOrder.calculationResoultMessage");
	private static ShaniString calculationFailedMessage=ShaniString.loadString("orders.CalculateOrder.calculationFailedMessage");
	
	@Override
	protected SentenceMatcherAction actionFactory(String sentenceName, HashMap<String, String> returnValues) {
		return new CalculateAction();
	}
	
	public class CalculateAction extends SentenceMatcherAction{

		@Override
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
			try {
				switch(sentenceName) {
				case "count":
					System.out.printf(calculationResoultMessage.toString(),calculate(returnValues.get("count")));
					return true;
				case "power":
					System.out.printf(calculationResoultMessage.toString(),Math.pow(calculate(returnValues.get("power1")), calculate(returnValues.get("power2"))));
					return true;
				case "multiply":
					System.out.printf(calculationResoultMessage.toString(),calculate(returnValues.get("multiply1"))*calculate(returnValues.get("multiply2")));
					return true;
				case "divide":
					System.out.printf(calculationResoultMessage.toString(),calculate(returnValues.get("divide1"))/calculate(returnValues.get("divide2")));
					return true;
				case "add":
					System.out.printf(calculationResoultMessage.toString(),calculate(returnValues.get("add1"))+calculate(returnValues.get("add2")));
					return true;
				case "substract":
					System.out.printf(calculationResoultMessage.toString(),calculate(returnValues.get("substract1"))/calculate(returnValues.get("substract2")));
					return true;
				default:
					assert false:sentenceName+" is unrecognized sentence name in CalculateOrder.SentenceMatcherAction.";
					System.err.println(sentenceName+" is unrecognized sentence name in CalculateOrder.SentenceMatcherAction.");
					System.out.print(Engine.errorMessage);
					return false;
				}
			} catch (Exception e) {
				System.out.print(calculationFailedMessage);
				e.printStackTrace();
				return false;
			} finally {
				System.out.println();
			}
		}
	}
	
	private static double calculate(String expresion) throws Exception {
		class Value{										//Creating special object for it will make easier adding multichar operations(e.g sin), complex numbers
			double value;
			Value(double val){value=val;}
			
			public String toString() {
				return Double.toString(value);
			}
		}
		class Operation{
			char type;
			Operation(char type){
				this.type=type;
			}
			
			public String toString() {
				return Character.toString(type);
			}
		}
		expresion=expresion.replaceAll("\\s", "");
		char[] exp=expresion.toCharArray();
		ArrayList<Object> elements=new ArrayList<>();
		for(Integer i=0;i<exp.length;i++) {
			if(Character.isDigit(exp[i])) {
				double value=0;
				while(i<exp.length&&Character.isDigit(exp[i])) {
					value=value*10+exp[i++]-'0';
				}
				i--;
				elements.add(new Value(value));
			} else {
				elements.add(new Operation(exp[i]));
			}
		}
		
		Object el2=elements.get(0);														//Parse '.' and ','
		for(int i=0;i<elements.size()-1;i++) {
			Object el=el2;
			el2=elements.get(i+1);
			if(el instanceof Value) {
				if(el2 instanceof Operation&&(((Operation) el2).type==','||((Operation) el2).type=='.')) {
					if(i+2==elements.size())elements.remove(i+1);
					Object el3=elements.get(i+2);
					if(el3 instanceof Value) {
						double divider=1;
						int val=(int)((Value)el3).value;
						while(val>0) {
							val/=10;
							divider*=10;
						}
						val=(int)((Value)el3).value;
						((Value) el).value+=val/divider;
						elements.remove(i+2);
						elements.remove(i+1);
					} else throw new Exception("Illegal use of ',' or '.'");				//Illegal use of ',' or '.'
				}
			}
		}
		
		el2=elements.get(0);														//parse '-'
		for(int i=0;i<elements.size()-1;i++) {
			Object el=el2;
			el2=elements.get(i+1);
			if(el instanceof Operation&&((Operation) el).type=='-') {
				if(el2 instanceof Value) {
					((Value) el2).value*=-1;
					elements.remove(i--);
				}
			}
		}
		
		
		Object el1=null;							//Parse pow
		el2=elements.get(0);
		for(int i=0;i<elements.size()-1;i++) {
			Object el=el2;
			el2=elements.get(i+1);
			if(el instanceof Operation&&((Operation) el).type=='^') {
				if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illega use of '^'");					//Illega use of '^'
				((Value)el1).value=Math.pow(((Value)el1).value, ((Value)el2).value);
				elements.remove(i);
				elements.remove(i);
				i-=3;
			}
			el1=el;
		}
		if((el1=elements.get(elements.size()-1))instanceof Operation&&((Operation)el1).type=='^') throw new Exception("Illega use of '^'");		//Illega use of '^'
		
		el2=elements.get(0);							//Parse * and /
		for(int i=0;i<elements.size()-1;i++) {
			Object el=el2;
			el2=elements.get(i+1);
			if(el instanceof Operation) {
				if(((Operation) el).type=='*') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("//Illega use of '*'");					//Illega use of '*'
					((Value)el1).value*=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				} else if(((Operation) el).type=='/') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illega use of '/'");					//Illega use of '/'
					((Value)el1).value/=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				}
			}
			el1=el;
		}
		if((el1=elements.get(elements.size()-1))instanceof Operation&&((Operation)el1).type=='^')throw new Exception("Illega use of '*' or '/'");			//Illega use of '*' or '/'
		
		el2=elements.get(0);							//Parse + and /
		for(int i=0;i<elements.size()-1;i++) {
			Object el=el2;
			el2=elements.get(i+1);
			if(el instanceof Operation) {
				if(((Operation) el).type=='+') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illegal use of '+'");					//Illega use of '+'
					((Value)el1).value+=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				} else if(((Operation) el).type=='-') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illegal use of '-'");					//Illega use of '-'
					((Value)el1).value-=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				}
			}
			el1=el;
		}
		if((el1=elements.get(elements.size()-1))instanceof Operation&&((Operation)el1).type=='^')throw new Exception();			//Illega use of '+' or '-'
		
		double resoult=0;
		for(int i=0;i<elements.size();i++) {
			Object el=elements.get(i);
			if(el instanceof Operation)throw new Exception();					//Operation stored in el is unnsupported
			if(el2 instanceof Operation)throw new Exception();				//Operation stored in el2 is unnsupported
			assert el instanceof Value&&el2 instanceof Value:"elements list should contain only Value and Operation objects";
			resoult+=((Value)el).value;
		}
		
		return resoult;
	}
}