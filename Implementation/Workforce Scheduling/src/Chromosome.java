import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Chromosome implements Serializable{

	private static final long serialVersionUID = 1L;
	private ArrayList<Double> solution;
	private ArrayList<String[][]> timesolts;
	private ArrayList<Integer> genoma;
	private int id;
	private double FO;
	private double fitness;
	private double fatherRate;
	private double unatendedDemandA;
	private double unatendedDemandB;
	private double unatendedDemandC;
	private double maxDemand;
	private double variability;
	private boolean foCalculated;
	
	public Chromosome(int numAgents) {
		Random rand = new Random();
		int int_random;
		this.FO = 0;
		this.fitness = 0;
		this.fatherRate = 0;
		this.setVariability(0);
		this.setUnatendedDemandA(0);
		this.setUnatendedDemandB(0);
		this.setUnatendedDemandC(0);
		this.setMaxDemand(0);
		this.foCalculated = false;
		this.solution = new ArrayList<Double>();
		this.timesolts = new ArrayList<String[][]>();
		this.genoma = new ArrayList<Integer>();
		for(int i=0; i < numAgents; i++) {
			int_random = rand.nextInt(2);
			this.timesolts.add(new String[8][2]);
			this.solution.add(generateRandom());
			this.genoma.add(int_random);
		}
	}

	public void calculateSchedulingFO(HashMap<String, Integer> pA, HashMap<String, Integer> pB, HashMap<String, Integer> pC, HashMap<String, String> pBreaks) {

		@SuppressWarnings("unchecked")
		HashMap<String, Integer> a = (HashMap<String, Integer>) pA.clone();
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> b = (HashMap<String, Integer>) pB.clone();
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> c = (HashMap<String, Integer>) pC.clone();
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> max = (HashMap<String, Integer>) pA.clone();
		double maxValue = 0;
		
		this.unatendedDemandA = 0;
		this.unatendedDemandB = 0;
		this.unatendedDemandC = 0;
		this.variability = 0;

		for(String[][] actual: timesolts) {
			ArrayList<Double> var = new ArrayList<Double>();
			for(int i = 0; i < 8; i++) {
				String day = actual[i][0].split(" ")[0];
				String initialHour = actual[i][0].split(" ")[1];
				String activity = actual[i][1];
				if(!activity.equalsIgnoreCase("LLLL")) {
					LocalTime h = LocalTime.of(Integer.parseInt(initialHour.split(":")[0]), Integer.parseInt(initialHour.split(":")[1]));
					double ho = (double) h.getHour();
					int min = h.getMinute();
					if(min == 30) {
						ho += 0.5;
					}					
					var.add(ho);
				}
				String initBreak = pBreaks.get(initialHour);
				HashMap<String, String> labor = getTimeSlotsAgent(day, initialHour, activity, initBreak);
				for(Map.Entry<String, String> entry: labor.entrySet()) {
					String dayHour = entry.getKey();
					String act = entry.getValue();
					switch(act) {
					case "A":
						if(a.get(dayHour) > 0)
							a.put(dayHour, a.get(dayHour) - 1);
						break;
					case "B":
						if(b.get(dayHour) > 0)
							b.put(dayHour, b.get(dayHour) - 1);
						break;
					case "C":
						if(c.get(dayHour) > 0)
							c.put(dayHour, c.get(dayHour) - 1);
						break;
					case "L":
						break;
					default:
						break;
					}
				}
			}
			
			this.variability += variabilityAgent(var);
			
		}

		for(Map.Entry<String, Integer> maxAct: max.entrySet()) {
			int mayor = 0;
			mayor += a.get(maxAct.getKey());
			mayor += b.get(maxAct.getKey());
			mayor += c.get(maxAct.getKey());
			maxAct.setValue(mayor);
		}
		
		for(Map.Entry<String, Integer> maxAct: max.entrySet()) {
			if(maxAct.getValue() > maxValue) {
				maxValue = maxAct.getValue();
			}
		}
		
		for(Map.Entry<String, Integer> actA: a.entrySet()) {
			this.unatendedDemandA += actA.getValue();
		}
		
		for(Map.Entry<String, Integer> actB: b.entrySet()) {
			this.unatendedDemandB += actB.getValue();
		}
		
		for(Map.Entry<String, Integer> actC: c.entrySet()) {
			this.unatendedDemandC += actC.getValue();
		}
		
		this.FO = this.unatendedDemandA + this.unatendedDemandB + this.unatendedDemandC + (maxValue * 25);
		this.maxDemand = maxValue;
		this.fitness = 1 / this.FO;
		this.setFoCalculated(true);

	}
	
	public double variabilityAgent(ArrayList<Double> hours) {
		
		double var = 0;
		double prom = 0;
		double sum = 0;
		
		for(int i = 0; i < hours.size(); i++) {
			sum += hours.get(i);
		}
		prom = sum / hours.size();
		
		for(int i = 0; i < hours.size(); i++) {
			var += Math.abs(hours.get(i) - prom);
		}

		return var;
		
	}

	public HashMap<String, String> getTimeSlotsAgent(String day, String initialHour, String permutation, String initBreak) {

		
		String nextDay = returnNextDay(day);
		HashMap<String, String> labor = new HashMap<String, String>();
		LocalTime init = LocalTime.of(Integer.parseInt(initialHour.split(":")[0]), Integer.parseInt(initialHour.split(":")[1]));
		LocalTime pause = LocalTime.of(Integer.parseInt(initBreak.split(":")[0]),Integer.parseInt(initBreak.split(":")[1]));
		String firstAct = Character.toString(permutation.charAt(0));
		//Primeras dos horas se hacen la actividad 1
		labor.put(day + " " + initialHour, firstAct);
		LocalTime next = init.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), firstAct); else labor.put(nextDay + " " + next.toString(), firstAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), firstAct); else labor.put(nextDay + " " + next.toString(), firstAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), firstAct); else labor.put(nextDay + " " + next.toString(), firstAct); 
		//Proximas dos horas y media se hace la actividad 2
		String secAct = Character.toString(permutation.charAt(1));
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), secAct); else labor.put(nextDay + " " + next.toString(), secAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), secAct); else labor.put(nextDay + " " + next.toString(), secAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), secAct); else labor.put(nextDay + " " + next.toString(), secAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), secAct); else labor.put(nextDay + " " + next.toString(), secAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), secAct); else labor.put(nextDay + " " + next.toString(), secAct); 
		//Proximas dos horas y media se hace la actividad 3
		String thirdAct = Character.toString(permutation.charAt(2));
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), thirdAct); else labor.put(nextDay + " " + next.toString(), thirdAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), thirdAct); else labor.put(nextDay + " " + next.toString(), thirdAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), thirdAct); else labor.put(nextDay + " " + next.toString(), thirdAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), thirdAct); else labor.put(nextDay + " " + next.toString(), thirdAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), thirdAct); else labor.put(nextDay + " " + next.toString(), thirdAct); 
		//Ultimas dos horas se hace la actividad 4
		String fourthAct = Character.toString(permutation.charAt(3));
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), fourthAct); else labor.put(nextDay + " " + next.toString(), fourthAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), fourthAct); else labor.put(nextDay + " " + next.toString(), fourthAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), fourthAct); else labor.put(nextDay + " " + next.toString(), fourthAct); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), fourthAct); else labor.put(nextDay + " " + next.toString(), fourthAct); 
		
		//Pausas
		next = pause;
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), "L"); else labor.put(nextDay + " " + next.toString(), "L"); 
		next = next.plusMinutes(30);
		if(next.compareTo(init) > 0) labor.put(day + " " + next.toString(), "L"); else labor.put(nextDay + " " + next.toString(), "L"); 
		
		return labor;

	}
	
	public String returnNextDay(String day) {
		
		if(day.equalsIgnoreCase("Mar")) return "Mie";
		if(day.equalsIgnoreCase("Mie")) return "Jue";
		if(day.equalsIgnoreCase("Jue")) return "Vie";
		if(day.equalsIgnoreCase("Vie")) return "Sab";
		if(day.equalsIgnoreCase("Sab")) return "Dom";
		if(day.equalsIgnoreCase("Dom")) return "Lun";
		if(day.equalsIgnoreCase("Lun")) return "Mar2";
		if(day.equalsIgnoreCase("Mar2")) return "Mie2";
		
		return "";
	}

	public ArrayList<Double> getSolution(){
		return solution;
	}

	public void setSolution(ArrayList<Double> solution) {
		this.solution = solution;
	}

	public double getFO() {
		return FO;
	}

	public void setFO(double fO) {
		FO = fO;
	}

	public double getFitness() {
		return fitness;
	}

	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	public double getFatherRate() {
		return fatherRate;
	}

	public ArrayList<String[][]> getTimesolts() {
		return timesolts;
	}

	public void setSolutionToTimeslots(int pos, String[][] sol) {
		this.timesolts.set(pos, sol);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setFatherRate(double fatherRate) {
		this.fatherRate = fatherRate;
	}

	public boolean isFoCalculated() {
		return foCalculated;
	}

	public void setFoCalculated(boolean foCalculated) {
		this.foCalculated = foCalculated;
	}

	public ArrayList<Integer> getGenoma() {
		return genoma;
	}

	public void setGenoma(ArrayList<Integer> genoma) {
		this.genoma = genoma;
	}
	
	public static double generateRandom(){
		Random rand = new Random();
		double upperbound = 47;
		int lowestbound = 0;
		double range = upperbound - lowestbound;
		double number = rand.nextDouble() * range;
		double shifted = number + lowestbound;
		return roundTwoDecimals(shifted, 2);
	}
	
	public static double roundTwoDecimals(double value,int places) {
		
		if (places < 0) throw new IllegalArgumentException();

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	public double getMaxDemand() {
		return maxDemand;
	}

	public void setMaxDemand(double maxDemand) {
		this.maxDemand = maxDemand;
	}

	public double getUnatendedDemandA() {
		return unatendedDemandA;
	}

	public void setUnatendedDemandA(double unatendedDemandA) {
		this.unatendedDemandA = unatendedDemandA;
	}

	public double getUnatendedDemandB() {
		return unatendedDemandB;
	}

	public void setUnatendedDemandB(double unatendedDemandB) {
		this.unatendedDemandB = unatendedDemandB;
	}

	public double getUnatendedDemandC() {
		return unatendedDemandC;
	}

	public void setUnatendedDemandC(double unatendedDemandC) {
		this.unatendedDemandC = unatendedDemandC;
	}

	public double getVariability() {
		return variability;
	}

	public void setVariability(double variability) {
		this.variability = variability;
	}
}
