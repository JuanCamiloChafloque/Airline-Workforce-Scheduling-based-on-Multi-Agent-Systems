import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.wrapper.ContainerController;
import jade.core.behaviours.*;

public class CustomerServiceSupervisor extends Agent{


	private static final long serialVersionUID = 1L;

	private HashMap<String, Integer> actA = new HashMap<String, Integer>();
	private HashMap<String, Integer> actB = new HashMap<String, Integer>();
	private HashMap<String, Integer> actC = new HashMap<String, Integer>();
	private HashMap<String, String> breaks = new HashMap<String, String>();
	private ArrayList<Fathers> fathersSelected;
	private CSSupervisorGUI myGui;
	private int population;
	private float mutationRate;
	private float crossoverRate;
	private float elitRate;
	private float threshold;
	private int maxIteration;
	private AID[] serviceAgents;
	private AID[] airline;
	private ArrayList<Chromosome> chromosomes;
	private ContainerController container;
	private ArrayList<Chromosome> bestChromosomes;

	@Override
	protected void setup() {

		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader(new File("./ActivityADemand.csv")));
			String line = br.readLine();

			while(line != null) {
				String [] data = line.split(";");
				String hour = data[0].trim();
				int demand = Integer.parseInt(data[1].trim());
				this.actA.put(hour, demand);
				line = br.readLine();
			}

			br.close();

			br = new BufferedReader(new FileReader(new File("./ActivityBDemand.csv")));
			line = br.readLine();

			while(line != null) {
				String [] data = line.split(";");
				String hour = data[0].trim();
				int demand = Integer.parseInt(data[1].trim());
				this.actB.put(hour, demand);
				line = br.readLine();
			}

			br.close();

			br = new BufferedReader(new FileReader(new File("./ActivityCDemand.csv")));
			line = br.readLine();

			while(line != null) {
				String [] data = line.split(";");
				String hour = data[0].trim();
				int demand = Integer.parseInt(data[1].trim());
				this.actC.put(hour, demand);
				line = br.readLine();
			}

			br.close();

			br = new BufferedReader(new FileReader(new File("./Breaks.csv")));
			line = br.readLine();

			while(line != null) {
				String [] data = line.split(";");
				String init = data[0].trim();
				String pause = data[1].trim();
				this.breaks.put(init, pause);
				line = br.readLine();
			}

			br.close();

			setMyGui(new CSSupervisorGUI(this));
			System.out.println("Supervisor started...");
			//Subscribing the agent to the AMS Agent and Yellow Pages
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			//Offering different services
			ServiceDescription sd = new ServiceDescription();
			sd.setType("report-schedule");
			sd.setName("JADE-scheduling");
			dfd.addServices(sd);

			try {
				DFService.register(this, dfd);
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
		addBehaviour(new resolvePeak());
		addBehaviour(new solveAbsense());
	}

	public int getPopulation() {
		return population;
	}

	public void setPopulation(int population) {
		this.population = population;
	}

	public float getMutationRate() {
		return mutationRate;
	}

	public void setMutationRate(float mutationRate) {
		this.mutationRate = mutationRate;
	}

	public float getCrossoverRate() {
		return crossoverRate;
	}

	public void setCrossoverRate(float crossoverRate) {
		this.crossoverRate = crossoverRate;
	}

	public float getElitRate() {
		return elitRate;
	}

	public void setElitRate(float elitRate) {
		this.elitRate = elitRate;
	}

	public float getThreshold() {
		return threshold;
	}

	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}

	public int getMaxIteration() {
		return maxIteration;
	}

	public void setMaxIteration(int maxIteration) {
		this.maxIteration = maxIteration;
	}

	public CSSupervisorGUI getMyGui() {
		return myGui;
	}

	public void setMyGui(CSSupervisorGUI myGui) {
		this.myGui = myGui;
	}

	public ContainerController getContainer() {
		return container;
	}

	public void setContainer(ContainerController container) {
		this.container = container;
	}

	public void InitiateGenetic(int pop, int iterations, float mutation, float crossover, float elitism, float threas) {
		addBehaviour(new OneShotBehaviour() {

			private static final long serialVersionUID = 1L;

			public void action() {
				population  = pop;
				maxIteration = iterations;
				mutationRate = mutation;
				crossoverRate = crossover;
				elitRate = elitism;
				threshold = threas;
				setContainer(getContainerController());
				chromosomes = new ArrayList<Chromosome>();
				bestChromosomes = new ArrayList<Chromosome>();
				//Search for the Customer Service Agents
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("report-timeslot");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					serviceAgents = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						serviceAgents[i] = result[i].getName();
					}


				}catch (FIPAException fe) {
					fe.printStackTrace();
				}
				//Search for Airline Agent
				template= new DFAgentDescription();
				sd = new ServiceDescription();
				sd.setType("best-solution");
				template.addServices(sd);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					airline = new AID[result.length];
					for (int i = 0; i < result.length; ++i) {
						airline[i] = result[i].getName();
					}


				}catch (FIPAException fe) {
					fe.printStackTrace();
				}
				myAgent.addBehaviour(new GeneticAlgorithm());
			}

		});
	}

	private class GeneticAlgorithm extends Behaviour {

		private static final long serialVersionUID = 1L;
		private int step = 0;
		private boolean firstIteration = false;
		private int repliesCnt;
		private int expectedReplies;
		private double totalFitness;
		private int iterations;
		private MessageTemplate mt; // The template to receive replies
		@Override
		public void action() {
			//
			switch (step) {
			case 0:
				//Initiate the population
				iterations = 1;
				for(int i=0; i <population;i++) {
					Chromosome chromosome = new Chromosome(serviceAgents.length);
					chromosomes.add(chromosome);
				}
				step = 1;
				break;
			case 1:
				//Enviar solitudes
				expectedReplies = 0;
				for (int i = 0; i < serviceAgents.length; ++i) {
					enviarHora(serviceAgents[i]);
				}
				step = 2;
				repliesCnt = 0;
				break;
			case 2:
				//recibir configuraciones
				//Armar una matriz o hashmap copia con los resultados
				ACLMessage reply = myAgent.receive(mt);

				if (reply != null) {
					int chrom;
					Object[] info = null;
					String[][] config = new String[8][2];
					try {
						info = (Object[]) reply.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
					chrom = (int) info[0];
					config = (String[][]) info[1];
					int idAgent = Integer.parseInt(reply.getSender().getName().split(" ")[1].split("@")[0]) - 1;
					chromosomes.get(chrom).setSolutionToTimeslots(idAgent, config);
					repliesCnt++;
					if(!firstIteration) {
						if(repliesCnt == serviceAgents.length * population) {
							step = 3;
						}
					}else {
						if(repliesCnt == expectedReplies) {
							step = 6;
						}
					}
				}
				else {
					block(10000);
				}
				break;
			case 3:
				for(Chromosome actual: chromosomes) {
					if (!actual.isFoCalculated()) actual.calculateSchedulingFO(actA, actB, actC, breaks);
				}
				step = 4;
				break;
			case 4:
				//Prob fathers
				totalFitness = 0;
				for(Chromosome actual: chromosomes) {
					double currentFitness = actual.getFitness();
					totalFitness += currentFitness;
				}
				//Set Prob
				for(Chromosome actual: chromosomes) {
					actual.setFatherRate(actual.getFitness()/totalFitness);
				}
				fathersSelected = new ArrayList<Fathers>();
				selectFathers();
				generateKids();
				step = 5;
				//Cruces
				break;
			case 5:
				//Mutación
				mutateKids();
				if(!firstIteration) firstIteration = true;
				step = 1;
				break;
			case 6:
				//Nueva generación y verificar las iteraciones
				ArrayList<Chromosome> newGeneration = new ArrayList<Chromosome>();
				ArrayList<Chromosome> auxGeneration = new ArrayList<Chromosome>();
				ArrayList<Boolean> auxBoolean = new ArrayList<Boolean>();
				int elit = Math.round(population * elitRate);
				Random rand = new Random();
				if(iterations == maxIteration) {
					System.out.println("Finished genetic algorithm for the personnel scheduling...");
					step = 7;

				} else {
					System.out.println("Starting generation " + (bestChromosomes.size()+1) + "...");
					for(Chromosome actual: chromosomes) {
						if (!actual.isFoCalculated()) actual.calculateSchedulingFO(actA, actB, actC, breaks);
					}

					//Sort by the fitness of each chromosome (High to low)
					Collections.sort(chromosomes, new Comparator<Chromosome>() {
						public int compare(Chromosome o1, Chromosome o2) {
							return Double.compare(o2.getFitness(), o1.getFitness()) ;
						}
					});					
					bestChromosomes.add(chromosomes.get(0));
					if (bestChromosomes.size() > 1) {
						int pos = bestChromosomes.size()-1;
						double diff = Math.abs(bestChromosomes.get(pos).getFitness()- bestChromosomes.get(pos-1).getFitness())/
								bestChromosomes.get(pos).getFitness();
						if(diff<threshold) {
							System.out.println("I have " + iterations + " iterations...");
							iterations++;
						}
						else {
							System.out.println("Iterations restarted...");
							iterations = 0;
						}
					}else {

						iterations++;
					}
					//Create the first new generation with the elitism rate and population
					for(int i = 0; i < elit; i++) {
						newGeneration.add(chromosomes.get(i));
					}

					//Initiate sec generation and booleans
					for(int i = elit; i < population; i++) {
						auxGeneration.add(chromosomes.get(i));
						auxBoolean.add(false);
					}

					//Create the second new generation with the elitism rate, population and rand function
					int range = population - elit;
					for(int i = elit; i < population; i++) {
						boolean diff = false;
						while(!diff) {
							int pos = rand.nextInt(range);
							if(!auxBoolean.get(pos)) {
								diff = true;
								auxBoolean.set(pos, true);
								newGeneration.add(auxGeneration.get(pos));
							}
						}
					}
					chromosomes.clear();
					for(int i = 0; i < newGeneration.size(); i++) {
						chromosomes.add(newGeneration.get(i));
					}

					step = 4;
				}

				break;
			case 7:
				sendBestSolution();
				step = 8;
				break;
			}
		}

		@Override
		public boolean done() {
			return (step == 8);
		}

		public void enviarHora(AID agente) {
			int idAgent;
			ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
			cfp.addReceiver(agente);
			cfp.setConversationId("report-option");
			idAgent = Integer.parseInt(agente.getName().split(" ")[1].split("@")[0]) - 1;
			for(int i=0; i < chromosomes.size(); i++) {
				if(!chromosomes.get(i).isFoCalculated()) {
					expectedReplies ++;
					cfp.setContent(i+" "+chromosomes.get(i).getSolution().get(idAgent));
					myAgent.send(cfp);
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId("report-option"),
							MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				}
			}
		}
		public void sendBestSolution() {
			ArrayList<String[][]> timeslots;
			double bestFo;
			double max;
			double unatended;
			double variability;
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			cfp.addReceiver(airline[0]);
			cfp.setConversationId("best-schedule");
			timeslots= bestChromosomes.get(bestChromosomes.size() - 1).getTimesolts();
			bestFo = bestChromosomes.get(bestChromosomes.size() - 1).getFO();
			max = bestChromosomes.get(bestChromosomes.size() - 1).getMaxDemand();
			unatended = bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandA() +
					bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandB() + 
					bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandC();
			variability = bestChromosomes.get(bestChromosomes.size() - 1).getVariability();
			Object[] params = {timeslots,bestFo, max, unatended, variability};
			try {
				cfp.setContentObject(params);
			} catch (IOException e) {
				e.printStackTrace();
			}
			myAgent.send(cfp);
			for (int i = 0; i < serviceAgents.length; ++i) {
				sendDecision(serviceAgents[i]);
			}
		}

	}
	public void sendDecision(AID agente) {
		int idAgent;
		ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
		cfp.addReceiver(agente);
		cfp.setConversationId("report-decision");
		idAgent = Integer.parseInt(agente.getName().split(" ")[1].split("@")[0]) - 1;
		try {
			cfp.setContentObject(bestChromosomes.get(bestChromosomes.size()-1).getSolution().get(idAgent));
			this.send(cfp);
		} catch (IOException e) {
			e.printStackTrace();
		}


	}
	public void selectFathers() {
		double ball;
		Random rand = new Random();
		ArrayList<Double> rulette = new ArrayList<Double>();
		//Creating the rulette with acum prob of the population
		rulette.add(chromosomes.get(0).getFatherRate());
		for(int i=1; i < population; i++) {
			rulette.add(rulette.get(i-1)+chromosomes.get(i).getFatherRate());
		}
		for (int i=0; i<chromosomes.size()/2;i++) {
			ball = rand.nextDouble();
			casino(rulette, ball, i);
		}
	}

	public void casino(ArrayList<Double> rulette, double ball, int pos) {
		int papa1, papa2;
		papa1 = 0;
		papa2 = 0;
		//Father 1
		//Case 1 
		if (0 < ball && ball<=rulette.get(rulette.size()-1)) papa1 = 0;
		//Case 2
		for(int i=0;i < rulette.size()-1;i++) {
			if (rulette.get(i) <= ball && ball <= rulette.get(i+1)) {
				papa1 = i;
				break;
			}
		}
		//Case 3
		if (rulette.get(rulette.size()-1)<ball && ball <=1) papa1 = rulette.size()-1;
		//Father 2
		if (ball< 0.5) {
			ball+=0.5;
		} else {
			ball += (ball+0.5)-1;
		}
		//Case 1 
		if (0 < ball && ball<=rulette.get(rulette.size()-1)) papa2 = 0;
		//Case 2
		for(int i=0;i < rulette.size()-1;i++) {
			if (rulette.get(i) <= ball && ball <= rulette.get(i+1)) {
				papa2 = i;
				break;
			}
		}
		//Case 3
		if (rulette.get(rulette.size()-1)<ball && ball <=1) papa2 = rulette.size()-1;
		fathersSelected.add(new Fathers(papa1,papa2));
	}

	public void generateKids() {
		Chromosome son1;
		Chromosome son2;
		double prob;
		Random rand = new Random();
		for(Fathers actual:fathersSelected) {
			prob = rand.nextDouble();
			if (prob < crossoverRate) {
				son1 = new Chromosome(serviceAgents.length);
				son2 = new Chromosome(serviceAgents.length);
				crossFathers(son1, son2, actual);
				chromosomes.add(son1);
				chromosomes.add(son2);
			}
		}
	}

	@SuppressWarnings("static-access")
	public void crossFathers(Chromosome son1, Chromosome son2, Fathers couple) {
		Chromosome father1 = chromosomes.get(couple.getFather1());
		Chromosome father2 = chromosomes.get(couple.getFather2());
		//Information solution of child 1
		ArrayList<Double> solution1 = new ArrayList<Double>();
		ArrayList<Integer> genoma = father1.getGenoma();
		for (int i=0; i < father1.getSolution().size();i++) {
			if(genoma.get(i)==0) {
				solution1.add(father1.getSolution().get(i));
			}else {
				solution1.add(father1.getSolution().get(i)+father2.getSolution().get(i));
			}
		}
		//Information solution of child 2
		ArrayList<Double> solution2 = new ArrayList<Double>();
		genoma = father2.getGenoma();
		for (int i=0; i < father2.getSolution().size();i++) {
			if(genoma.get(i)==0) {
				solution2.add(father2.getSolution().get(i));
			}else {
				solution2.add(father1.getSolution().get(i)-father2.getSolution().get(i));
			}
		}
		//Fix by circular
		//Child1 
		for(int i=0; i < solution1.size();i++) {
			if(solution1.get(i)>47) {
				solution1.set(i, son1.roundTwoDecimals(solution1.get(i)%47, 2));
			}
		}
		son1.setSolution(solution1);
		//Child2
		for(int i=0; i < solution2.size();i++) {
			if(solution2.get(i)<0) {
				solution2.set(i, son2.roundTwoDecimals(47+solution2.get(i), 2));
			}
		}
		son2.setSolution(solution2);
	}

	public void mutateKids() {

		double prob;
		Random rand = new Random();
		ArrayList<Double> solutionSwap;
		for(int i=population+1;i < chromosomes.size(); i++) {
			prob = rand.nextDouble();
			if(prob < mutationRate) {
				solutionSwap = mutate(chromosomes.get(i));
				chromosomes.get(i).setSolution(solutionSwap);
			}
		}
	}

	public ArrayList<Double> mutate(Chromosome child) {

		ArrayList<Double> swap = new ArrayList<Double>();
		ArrayList<Double> sol = child.getSolution();
		Random rand = new Random();
		int upper = serviceAgents.length;
		int pos = rand.nextInt(upper);
		for(int i=pos+1; i < sol.size();i++) {
			swap.add(sol.get(i));
		}
		for(int i=0; i < pos+1;i++) {
			swap.add(sol.get(i));
		}
		return swap;
	}

	private class resolvePeak extends Behaviour{

		private static final long serialVersionUID = 1L;
		MessageTemplate mt =MessageTemplate.and(MessageTemplate.MatchConversationId("peak-demand"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		int step = 0;
		int repliesCnt;
		String dayHour;
		String activity;
		ArrayList<Object[]> agents = new ArrayList<Object[]>();
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> pA = (HashMap<String, Integer>) actA.clone(); 
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> pB = (HashMap<String, Integer>) actB.clone(); 
		@SuppressWarnings("unchecked")
		HashMap<String, Integer> pC = (HashMap<String, Integer>) actC.clone();
		public void action() {
			switch (step) {
			case 0:
				ACLMessage msg = myAgent.receive(mt);
				int increment;
				if(msg!=null) {
					try {
						Object[] content = (Object[]) msg.getContentObject();
						dayHour = content[0].toString()+" "+content[1].toString();
						activity = content[2].toString();
						increment = (int) content[3];
						if(activity.equalsIgnoreCase("A")) {
							actA.put(dayHour, actA.get(dayHour) + increment);
						} else if(activity.equalsIgnoreCase("B")) {
							actB.put(dayHour, actB.get(dayHour) + increment);
						} else {
							actC.put(dayHour, actC.get(dayHour) + increment);
						}
						for(int i=0; i < serviceAgents.length; i++) {
							ACLMessage mens = new ACLMessage(ACLMessage.REQUEST);
							mens.addReceiver(serviceAgents[i]);
							mens.setConversationId("change-activity");
							try {
								Object[] params = {dayHour, activity};
								mens.setContentObject(params);
								myAgent.send(mens);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						step = 1;
						repliesCnt = 0;
						mt = MessageTemplate.or(MessageTemplate.and(MessageTemplate.MatchConversationId("change-activity")
								, MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)), MessageTemplate.and(MessageTemplate.MatchConversationId("change-activity")
										, MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				}
				break;

			case 1:

				ACLMessage reply = myAgent.receive(mt);
				if(reply != null) {
					repliesCnt++;
					if(reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
						Object[] params;
						try {
							params = (Object[]) reply.getContentObject();
							agents.add(params);
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
					}

					if(repliesCnt >= 75) {
						step = 2;
					}
				}else {
					block();
				}				
				break;

			case 2:
				System.out.println("The supervisor has " + agents.size() + " proposes.");
				updateDemand(bestChromosomes.get(bestChromosomes.size() - 1).getTimesolts(), pA, pB, pC);
				for(Object[] proposes: agents) {
					int idAgent = Integer.parseInt(proposes[0].toString());
					int numDay = Integer.parseInt(proposes[1].toString());
					int franja = Integer.parseInt(proposes[2].toString());
					String[][] sol = bestChromosomes.get(bestChromosomes.size() - 1).getTimesolts().get(idAgent - 1);

					if(sol[numDay][1].charAt(franja) == 'A') {
						if(pA.get(dayHour) < 0) {
							System.out.println("The agent " + (idAgent) + " can make the change because he is idle.");
							System.out.println("The change is from permutation: " + sol[numDay][1]);
							sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
							System.out.println("To permutation: " + sol[numDay][1]);
							bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
							pA.put(dayHour, pA.get(dayHour) - 1);
						}else {
							System.out.println("The agent " + (idAgent) + " can't make the change because he is not idle.");
							int demActual = obtainDemand("" + sol[numDay][1].charAt(franja), dayHour);
							int demChange = obtainDemand(activity, dayHour);
							System.out.println("Verifying demand insatisfaction...");
							if(demActual < demChange) {
								System.out.println("The agent " + (idAgent) +  " is changing activities because the other is more critic.");
								System.out.println("The change is from permutation: " + sol[numDay][1]);
								sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
								System.out.println("To permutation: " + sol[numDay][1]);
								bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
								pA.put(dayHour, pA.get(dayHour) - 1);
							}else if(demActual >= demChange){
								if(randDemand()) {
									System.out.println("The agent " + (idAgent) +  " is changing activities because of the random.");
									System.out.println("The change is from permutation: " + sol[numDay][1]);
									sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
									System.out.println("To permutation: " + sol[numDay][1]);
									bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
									pA.put(dayHour, pA.get(dayHour) - 1);
								}else {
									System.out.println("The agent " + (idAgent) + " will not change activities so it dones't affect the maxDemand OF.");
								}
							}
						}

					}else if(sol[numDay][1].charAt(franja) == 'B') {
						if(pB.get(dayHour) < 0) {
							System.out.println("The agent " + (idAgent) + " can make the change because he is idle.");
							System.out.println("The change is from permutation: " + sol[numDay][1]);
							sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
							System.out.println("To permutation: " + sol[numDay][1]);
							bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
							pB.put(dayHour, pB.get(dayHour) - 1);
						}else {
							System.out.println("The agent " + (idAgent) + " can't make the change because he is not idle.");
							int demActual = obtainDemand("" + sol[numDay][1].charAt(franja), dayHour);
							int demChange = obtainDemand(activity, dayHour);
							System.out.println("Verifying demand insatisfaction...");
							if(demActual < demChange) {
								System.out.println("The agent " + (idAgent) +  " is changing activities because the other is more critic.");
								System.out.println("The change is from permutation: " + sol[numDay][1]);
								sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
								System.out.println("To permutation: " + sol[numDay][1]);
								bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
								pB.put(dayHour, pB.get(dayHour) - 1);
							}else if(demActual >= demChange){
								if(randDemand()) {
									System.out.println("The agent " + (idAgent) +  " is changing activities because of the random.");
									System.out.println("The change is from permutation: " + sol[numDay][1]);
									sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
									System.out.println("To permutation: " + sol[numDay][1]);
									bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
									pB.put(dayHour, pB.get(dayHour) - 1);
								}else {
									System.out.println("The agent " + (idAgent) + " will not change activities so it dones't affect the maxDemand OF.");
								}
							}
						}

					}else {
						if(pC.get(dayHour) < 0) {
							System.out.println("The agent " + (idAgent) + " can make the change because he is idle.");
							System.out.println("The change is from permutation: " + sol[numDay][1]);
							sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
							System.out.println("To permutation: " + sol[numDay][1]);
							bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
							pC.put(dayHour, pC.get(dayHour) - 1);
						}else {
							System.out.println("The agent " + (idAgent) + " can't make the change because he is not idle.");
							int demActual = obtainDemand("" + sol[numDay][1].charAt(franja), dayHour);
							int demChange = obtainDemand(activity, dayHour);
							System.out.println("Verifying demand insatisfaction...");
							if(demActual < demChange) {
								System.out.println("The agent " + (idAgent) +  " is changing activities because the other is more critic.");
								System.out.println("The change is from permutation: " + sol[numDay][1]);
								sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
								System.out.println("To permutation: " + sol[numDay][1]);
								bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
								pC.put(dayHour, pC.get(dayHour) - 1);
							}else if(demActual >= demChange) {
								if(randDemand()) {
									System.out.println("The agent " + (idAgent) +  " is changing activities because of the random.");
									System.out.println("The change is from permutation: " + sol[numDay][1]);
									sol[numDay][1] = sol[numDay][1].substring(0, franja) + activity.charAt(0) + sol[numDay][1].substring(franja + 1);
									System.out.println("To permutation: " + sol[numDay][1]);
									bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent - 1, sol);
									pC.put(dayHour, pC.get(dayHour) - 1);
								}else {
									System.out.println("The agent " + (idAgent) + " will not change activities so it dones't affect the maxDemand OF.");
								}
							}
						}
					}
				}
				step = 3;
				break;

			case 3:
				bestChromosomes.get(bestChromosomes.size() - 1).calculateSchedulingFO(actA, actB, actC, breaks);
				step = 4;
				break;

			case 4: 
				ArrayList<String[][]> timeslots;
				double bestFo;
				double max;
				double unatended;
				double variability;
				ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
				cfp.addReceiver(airline[0]);
				cfp.setConversationId("peak-demand-result");
				timeslots= bestChromosomes.get(bestChromosomes.size() - 1).getTimesolts();
				bestFo = bestChromosomes.get(bestChromosomes.size() - 1).getFO();
				max = bestChromosomes.get(bestChromosomes.size() - 1).getMaxDemand();
				unatended = bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandA() +
						bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandB() + 
						bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandC();
				variability = bestChromosomes.get(bestChromosomes.size() - 1).getVariability();
				Object[] params = {timeslots,bestFo, max, unatended, variability};
				try {
					cfp.setContentObject(params);
				} catch (IOException e) {
					e.printStackTrace();
				}
				myAgent.send(cfp);
				for(int i=0; i < serviceAgents.length; i++) {
					sendDecision(serviceAgents[i]);
				}
				step = 5;
				break;
			default:
				block();
				break;
			}

		}
		@Override
		public boolean done() {
			return (step == 5);
		}

		public int obtainDemand(String act, String dayHour) {

			if(act.equalsIgnoreCase("A")) {
				return pA.get(dayHour);
			}else if(act.equalsIgnoreCase("B")) {
				return pB.get(dayHour);
			}else {
				return pC.get(dayHour);
			}

		}

		public boolean randDemand() {
			Random rd = new Random();
			return rd.nextBoolean(); 
		}

		public void updateDemand(ArrayList<String[][]> timesolts, HashMap<String, Integer> a, HashMap<String, Integer> b, HashMap<String, Integer> c) {

			for(String[][] actual: timesolts) {
				for(int i = 0; i < 8; i++) {
					String day = actual[i][0].split(" ")[0];
					String initialHour = actual[i][0].split(" ")[1];
					String activity = actual[i][1];
					String initBreak = breaks.get(initialHour);
					HashMap<String, String> labor = getTimeSlotsAgent(day, initialHour, activity, initBreak);
					for(Map.Entry<String, String> entry: labor.entrySet()) {
						String dayHour = entry.getKey();
						String act = entry.getValue();
						switch(act) {
						case "A":
							a.put(dayHour, a.get(dayHour) - 1);
							break;
						case "B":
							b.put(dayHour, b.get(dayHour) - 1);
							break;
						case "C":
							c.put(dayHour, c.get(dayHour) - 1);
							break;
						case "L":
							break;
						default:
							break;
						}
					}
				}
			}

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

	}
	private class solveAbsense extends Behaviour {

		private static final long serialVersionUID = 1L;
		MessageTemplate mt =MessageTemplate.and(MessageTemplate.MatchConversationId("agents-absences"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		int step = 0;
		String[][] changeAgent;
		ArrayList<Object[]> info;
		ArrayList<Object[]> newAgents =  new ArrayList<Object[]>();
		int posAgent;
		int numDay;
		int repliesCnt;
		String dayHour = "";
		String perm = "";
		int cntAbsents;
		ArrayList<Object[]> agents = new ArrayList<Object[]>();
		@SuppressWarnings("unchecked")
		public void action() {
			switch (step) {
			case 0:
				ACLMessage msg = myAgent.receive(mt);
				if(msg!=null) {
					try {
						info =  (ArrayList<Object[]>) msg.getContentObject();
						cntAbsents = 0;
						step = 1;
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				}else {
					block();
				}				
				break;
			case 1:
				int idActual;
				posAgent = (int) info.get(cntAbsents)[1];
				numDay = (int) info.get(cntAbsents)[0];
				changeAgent = bestChromosomes.get(bestChromosomes.size() - 1).getTimesolts().get(posAgent);
				System.out.println();
				System.out.println();
				System.out.println("Supervisor checking the absent # " + (cntAbsents + 1));
				System.out.println("Agent : " + (posAgent + 1) +" will be absent on day and hour: " + changeAgent[numDay][0]);
				System.out.println("The activities to do are: " + changeAgent[numDay][1]);
				perm = changeAgent[numDay][1];
				dayHour = changeAgent[numDay][0];
				changeAgent[numDay][1] = "LLLL";
				bestChromosomes.get(bestChromosomes.size()-1).setSolutionToTimeslots(posAgent, changeAgent);

				for(int i=0; i < serviceAgents.length; i++) {
					idActual = Integer.parseInt(serviceAgents[i].getName().split("@")[0].split(" ")[1]);
					if(idActual != posAgent + 1) {
						ACLMessage mens = new ACLMessage(ACLMessage.REQUEST);
						mens.addReceiver(serviceAgents[i]);
						mens.setConversationId("change-rest");
						mens.setContent(dayHour + ";" + numDay + ";" + perm);
						myAgent.send(mens);
					}
				}
				mt = MessageTemplate.or(MessageTemplate.and(MessageTemplate.MatchConversationId("change-rest")
						, MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)), MessageTemplate.and(MessageTemplate.MatchConversationId("change-rest")
								, MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));
				repliesCnt = 0;
				agents.clear();
				step = 2;
				break;
			case 2:
				ACLMessage reply = myAgent.receive(mt);
				if(reply != null) {
					repliesCnt++;
					if(reply.getPerformative() == ACLMessage.PROPOSE) {
						try {
							agents.add((Object[]) reply.getContentObject());
						} catch (UnreadableException e) {
							e.printStackTrace();
						}
					}
					if(repliesCnt == serviceAgents.length - 1) {
						step = 3;
					}

				}else {
					block();
				}		
				break;

			case 3:
				if(agents.size() > 0) {
					AID agentConfirmed = null;
					sortCap();
					System.out.println("The agent " + (((int) agents.get(0)[0]) + 1) + " is going to cover the shift with the slots: " + agents.get(0)[3]);
					int idAgent = (int) agents.get(0)[0];
					String[][] sol = bestChromosomes.get(bestChromosomes.size() - 1).getTimesolts().get(idAgent);
					sol[numDay][1] = agents.get(0)[3].toString();
					sol[numDay][0] = dayHour;
					bestChromosomes.get(bestChromosomes.size() - 1).setSolutionToTimeslots(idAgent, sol);
					bestChromosomes.get(bestChromosomes.size() - 1).calculateSchedulingFO(actA, actB, actC, breaks);

					for(int i = 0; i < serviceAgents.length; i++) {
						int myId = Integer.parseInt(serviceAgents[i].getName().split("@")[0].split(" ")[1]);
						if(myId == (idAgent + 1)) {
							agentConfirmed = serviceAgents[i];
						}
					}
					Object[] relevo = {idAgent, dayHour, posAgent};
					newAgents.add(relevo);
					ACLMessage msgConfirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					msgConfirm.setConversationId("absent-change-accepted");
					Object[] params = {sol};
					try {
						msgConfirm.setContentObject(params);
						msgConfirm.addReceiver(agentConfirmed);
						myAgent.send(msgConfirm);
					} catch (IOException e) {
						e.printStackTrace();
					}

				} else {
					System.out.println("No agent can cover the absent agent. No proposes found");
				}
				cntAbsents++;
				step = 4;
				break;

			case 4:
				if(cntAbsents == info.size()) {
					step = 5;
				}else {
					step = 1;
				}
				break;

			case 5:
				ArrayList<String[][]> timeslots;
				double bestFo;
				double max;
				double unatended;
				double variability;
				ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
				cfp.addReceiver(airline[0]);
				cfp.setConversationId("absence-result");
				timeslots= bestChromosomes.get(bestChromosomes.size() - 1).getTimesolts();
				bestFo = bestChromosomes.get(bestChromosomes.size() - 1).getFO();
				max = bestChromosomes.get(bestChromosomes.size() - 1).getMaxDemand();
				unatended = bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandA() +
						bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandB() + 
						bestChromosomes.get(bestChromosomes.size() - 1).getUnatendedDemandC();
				variability = bestChromosomes.get(bestChromosomes.size() - 1).getVariability();
				Object[] params = {timeslots,bestFo, max, unatended, variability, newAgents};
				try {
					cfp.setContentObject(params);
				} catch (IOException e) {
					e.printStackTrace();
				}
				myAgent.send(cfp);
				for(int i=0; i < serviceAgents.length; i++) {
					sendDecision(serviceAgents[i]);
				}
				newAgents.clear();
				step = 6;
				break;
			default:
				break;
			}

		}

		public void sortCap() {	

			for(int i = 0; i < agents.size() - 1; i++) {
				for(int j = i + 1; j < agents.size(); j++) {
					Object[] infoI = agents.get(i);
					Object[] infoJ = agents.get(j);
					Object[] temp;
					if((int) infoI[1] < (int) infoJ[1]) {
						temp = agents.get(i);
						agents.set(i, agents.get(j));
						agents.set(j, temp);
					} else if(((int) infoI[1] == (int) infoJ[1])) {
						if((long) infoI[2] > (long) infoJ[2]) {
							temp = agents.get(i);
							agents.set(i, agents.get(j));
							agents.set(j, temp);
						}
					}
				}
			}	

		}

		@Override
		public boolean done() {
			return (step == 6);
		}

	}
}
