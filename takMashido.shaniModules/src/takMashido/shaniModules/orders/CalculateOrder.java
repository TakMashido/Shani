package takMashido.shaniModules.orders;

import org.w3c.dom.Element;
import takMashido.shani.core.Config;
import takMashido.shani.core.ShaniCore;
import takMashido.shani.core.Tests;
import takMashido.shani.core.text.ShaniString;
import takMashido.shani.orders.SentenceMatcherOrder;

import java.util.ArrayList;
import java.util.HashMap;

public class CalculateOrder extends SentenceMatcherOrder {
	private static ShaniString calculationResoultMessage;
	private static ShaniString calculationFailedMessage;
	
	public CalculateOrder(Element e) {
		super(e);
		
		calculationResoultMessage=ShaniString.loadString(e, "calculationResoultMessage");
		calculationFailedMessage=ShaniString.loadString(e, "calculationFailedMessage");
	}
	
	@Override
	protected SentenceMatcherAction actionFactory(String sentenceName, HashMap<String, String> returnValues) {
		return new CalculateAction();
	}
	
	public class CalculateAction extends SentenceMatcherAction{
		@Override
		@SuppressWarnings("ConstantConditions")							//Assert in case makes it not see success=false line and it thinks that it is always true
		protected boolean execute(String sentenceName, HashMap<String, String> returnValues) {
			try {
				boolean success=true;
				double result=switch (sentenceName){
					case "count" -> calculate(returnValues.get("count"));
					case "power" ->Math.pow(calculate(returnValues.get("power1")), calculate(returnValues.get("power2")));
					case "multiply" -> calculate(returnValues.get("multiply1"))*calculate(returnValues.get("multiply2"));
					case "divide" -> calculate(returnValues.get("divide1"))/calculate(returnValues.get("divide2"));
					case "add" -> calculate(returnValues.get("add1"))+calculate(returnValues.get("add2"));
					case "subtract" -> calculate(returnValues.get("subtract1"))-calculate(returnValues.get("subtract2"));
					default -> {
						assert false : sentenceName + " is unrecognized sentence name in CalculateOrder.SentenceMatcherAction.";
						System.err.println(sentenceName + " is unrecognized sentence name in CalculateOrder.SentenceMatcherAction.");
						success = false;
						yield 0;
					}
				};
				
				if(Config.testMode)
					if(success){
						Tests.addResults("operation", sentenceName);
						Tests.addResults("result", result);
					} else
						Tests.addResults("calculationFailed",true);
				
				if(success)
					System.out.printf(calculationResoultMessage.toString(),result);
				else
					System.out.print(ShaniCore.errorMessage);
				System.out.println();
				
				return success;
			} catch (Exception e) {
				System.out.print(calculationFailedMessage);
				e.printStackTrace();
				return false;
			}
		}
	}
	
	private static double calculate(String expresion) throws Exception {
		ShaniCore.debug.println("CalculateOrder: "+expresion);
		
		class Value{										//Creating special object for it will make easier adding multichar operations(e.g sin), complex numbers
			int leadingZeros;
			double value;
			Value(double val, int leadingZeros){
				value=val;
				this.leadingZeros=leadingZeros;
			}
			
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
				int leadingZeros=0;
				while(i<exp.length&&Character.isDigit(exp[i])) {
					value=value*10+exp[i++]-'0';
					if(value==0)leadingZeros++;
				}
				i--;
				elements.add(new Value(value,leadingZeros));
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
						double divider=Math.pow(10, ((Value) el3).leadingZeros);
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
				if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illegal use of '^'");					//Illegal use of '^'
				((Value)el1).value=Math.pow(((Value)el1).value, ((Value)el2).value);
				elements.remove(i);
				elements.remove(i);
				i-=3;
			}
			el1=el;
		}
		if((el1=elements.get(elements.size()-1))instanceof Operation&&((Operation)el1).type=='^') throw new Exception("Illegal use of '^'");		//Illegal use of '^'
		
		el2=elements.get(0);							//Parse * and /
		for(int i=0;i<elements.size()-1;i++) {
			Object el=el2;
			el2=elements.get(i+1);
			if(el instanceof Operation) {
				if(((Operation) el).type=='*') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illegal use of '*'");					//Illegal use of '*'
					((Value)el1).value*=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				} else if(((Operation) el).type=='/') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illegal use of '/'");					//Illegal use of '/'
					((Value)el1).value/=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				}
			}
			el1=el;
		}
		if((el1=elements.get(elements.size()-1))instanceof Operation&&((Operation)el1).type=='^')throw new Exception("Illegal use of '*' or '/'");			//Illegal use of '*' or '/'
		
		el2=elements.get(0);							//Parse + and /
		for(int i=0;i<elements.size()-1;i++) {
			Object el=el2;
			el2=elements.get(i+1);
			if(el instanceof Operation) {
				if(((Operation) el).type=='+') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illegal use of '+'");					//Illegal use of '+'
					((Value)el1).value+=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				} else if(((Operation) el).type=='-') {
					if(i==0||!(el1 instanceof Value)||!(el2 instanceof Value))throw new Exception("Illegal use of '-'");					//Illegal use of '-'
					((Value)el1).value-=((Value)el2).value;
					elements.remove(i);
					elements.remove(i);
					i-=3;
				}
			}
			el1=el;
		}
		if((el1=elements.get(elements.size()-1))instanceof Operation&&((Operation)el1).type=='^')throw new Exception("Illegal use of '+' or '-'");			//Illegal use of '+' or '-'
		
		double resoult=0;
		for(int i=0;i<elements.size();i++) {
			Object el=elements.get(i);
			assert el instanceof Value&&el2 instanceof Value:"elements list should contain only Value and Operation objects";
			if(el instanceof Operation|el2 instanceof Operation)throw new Exception("CalculateOrder Error during parsing \""+expresion+"\" unrecognized token found.");					//Operation stored in el/el2 is unnsupported
			resoult+=((Value)el).value;
		}
		
		return resoult;
	}
}