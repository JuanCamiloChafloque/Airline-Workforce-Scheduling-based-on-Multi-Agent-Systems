import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class TransportSupervisor extends Agent {

	private static final long serialVersionUID = 1L;
	private HashMap<String, String[]> coordenates = new HashMap<String, String[]>();
	private HashMap<String, ArrayList<Integer>> ida;
	private HashMap<String, ArrayList<Integer>> vuelta;
	private ArrayList<String[][]> timeSolt;
	private double schedulingFO;
	private double maxDemand;
	private double unatendedDemand;
	private Double[][] distances;
	private AID airline;
	private AID[] agents;
	private HashMap<String, ArrayList<ArrayList<Integer>>> vehiclesGoing;
	private HashMap<String, ArrayList<ArrayList<Integer>>> vehiclesReturn;
	private double FO = 0;
	private double N = 0; //Employees that the airline transport
	private double NRoutes = 0; //How many vehicles i have
	private double efficiency = 0;
	private double promAdditionalKm = 0;
	private double promIdealKm = 0;
	private double totalKmAgent = 0;
	private double additionalKm = 0;
	private double idealKm = 0;
	private double indirectRoutes = 0;
	@Override
	protected void setup(){
		ida  = new HashMap<String, ArrayList<Integer>>();
		vuelta = new HashMap<String, ArrayList<Integer>>();
		distances = new Double[76][76];
		try {
			int countDistances = 0;
			BufferedReader br;
			br = new BufferedReader(new FileReader(new File("./Coordenates.csv")));
			String line = br.readLine();

			while(line != null) {
				String []data = line.split(";");
				String ag = data[0];
				String [] coor = new String[2];
				coor[0] = data[1]; //X
				coor[1] = data[2]; //Y
				this.coordenates.put(ag, coor);

				line = br.readLine();
			}

			br.close();

			br = new BufferedReader(new FileReader(new File("./Distances.csv")));
			line = br.readLine();

			while(line != null) {
				String []data = line.split(";");
				for(int i = 1; i < data.length; i++) {
					this.distances[countDistances][i - 1] = Double.parseDouble(data[i].trim());
				}
				line = br.readLine();
				countDistances++;
			}

			br.close();

		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException: " + e.getMessage());

		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
		//Offer service of agent
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("route");
		sd.setName("JADE-routing");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new routingAgent());
		addBehaviour(new solveAbsense());
	}

	public HashMap<String, ArrayList<Integer>> getIda() {
		return ida;
	}

	public void setIda(HashMap<String, ArrayList<Integer>> ida) {
		this.ida = ida;
	}

	public HashMap<String, ArrayList<Integer>> getVuelta() {
		return vuelta;
	}

	public void setVuelta(HashMap<String, ArrayList<Integer>> vuelta) {
		this.vuelta = vuelta;
	}

	public ArrayList<String[][]> getTimeSolt() {
		return timeSolt;
	}

	public void setTimeSolt(ArrayList<String[][]> timeSolt) {
		this.timeSolt = timeSolt;
	}

	public double getSchedulingFO() {
		return schedulingFO;
	}

	public void setSchedulingFO(double schedulingFO) {
		this.schedulingFO = schedulingFO;
	}

	private class routingAgent extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		MessageTemplate mt =MessageTemplate.and(MessageTemplate.MatchConversationId("routing"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				try {
					ida.clear();
					vuelta.clear();
					//Quitar el tema de scheduling
					Object[] params = (Object[]) msg.getContentObject();
					setTimeSolt((ArrayList<String[][]>) params[1]);
					setSchedulingFO((double) params[0]);
					setMaxDemand((double) params[2]);
					setUnatendedDemand((double) params[3]);
					airline = msg.getSender();
					extractPossibleRoutes();
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
				//Get agents in the platform
				DFAgentDescription template = new DFAgentDescription();
				ServiceDescription serviceD = new ServiceDescription();
				serviceD.setType("transport");
				template.addServices(serviceD);
				try {
					DFAgentDescription[] result = DFService.search(myAgent, template);
					agents = new AID[result.length];
					for(int i=0; i < result.length;i++) {
						agents[i] = result[i].getName();
					}
				} catch (FIPAException e) {
					e.printStackTrace();
				}
				addBehaviour(new routing());
			}else {
				block();
			}
		}

	}

	public void extractPossibleRoutes() {

		if(timeSolt != null) {
			ArrayList<Integer> agentsGoing;
			ArrayList<Integer> agentsReturn;
			for(int i=0; i < timeSolt.size();i++) {
				for(int j=0; j < 8;j++) {
					String info = timeSolt.get(i)[j][0];
					String day = timeSolt.get(i)[j][0].split(" ")[0];
					String perm = timeSolt.get(i)[j][1];
					String hour = timeSolt.get(i)[j][0].split(" ")[1];
					LocalTime hourLT = LocalTime.of(Integer.parseInt(hour.split(":")[0]), 
							Integer.parseInt(hour.split(":")[1]));
					if(!perm.equals("LLLL")) {
						if(needTransportForGoing(hourLT, perm)) {
							if(ida.get(info) != null) {
								agentsGoing = ida.get(info);
								agentsGoing.add((i+1));
								ida.put(info, agentsGoing);
							}else {
								agentsGoing = new ArrayList<Integer>();
								agentsGoing.add(i + 1);
								ida.put(info, agentsGoing);
							}
						}

						if(needTransportForReturn(hourLT, perm)) {

							hourLT = hourLT.plusHours(9);
							String newDay = day + " " + hourLT.toString();

							if(vuelta.get(newDay) != null) {
								agentsReturn = vuelta.get(newDay);
								agentsReturn.add((i+1));
								vuelta.put(newDay, agentsReturn);
							}else {
								agentsReturn = new ArrayList<Integer>();
								agentsReturn.add(i + 1);
								vuelta.put(newDay, agentsReturn);
							}
						}
					}
				}
			}
		}
	}

	public boolean needTransportForGoing(LocalTime hour, String perm){

		if(hour.compareTo(LocalTime.of(21, 0)) >= 0 || 
				hour.compareTo(LocalTime.of(6, 30)) <= 0) {
			if(perm.equalsIgnoreCase("LLLL")) {
				return false;
			} else {
				return true;
			}
		}else {
			return false;
		}
	}

	public boolean needTransportForReturn(LocalTime hour, String perm) {

		hour = hour.plusHours(9);

		if(hour.compareTo(LocalTime.of(21, 0)) >= 0 || 
				hour.compareTo(LocalTime.of(6, 30)) <= 0) {
			if(perm.equalsIgnoreCase("LLLL")) {
				return false;
			} else {
				return true;
			}
		}else {
			return false;
		}
	}

	public double getMaxDemand() {
		return maxDemand;
	}

	public void setMaxDemand(double maxDemand) {
		this.maxDemand = maxDemand;
	}

	public double getUnatendedDemand() {
		return unatendedDemand;
	}

	public void setUnatendedDemand(double unatendedDemand) {
		this.unatendedDemand = unatendedDemand;
	}

	private class routing extends Behaviour{

		private static final long serialVersionUID = 1L;
		private int step = 0;
		private int cantLeaders = 0;
		private int msgReceived = 0;
		private HashMap<String, ArrayList<Integer>> leadersGoing;
		private HashMap<String, ArrayList<Integer>> leadersReturn;


		@Override
		public void action() {
			switch (step) {
			case 0:
				vehiclesGoing = new HashMap<String, ArrayList<ArrayList<Integer>>>();
				vehiclesReturn = new HashMap<String, ArrayList<ArrayList<Integer>>>();
				//Generate leaders
				leadersGoing = GenerateLeadersGoing();
				leadersReturn = GenerateLeadersReturn();
				cantLeaders = 0;
				step = 1;
				break;
			case 1:
				//Send to leaders and no leaders the quality of message that the must recive
				sendMessageNonLeadersGoing();
				sendMessageNonLeadersReturn();
				sendMessageLeadersGoing();
				sendMessageLeadersReturn();
				msgReceived = 0;
				step = 2;
				break;
			case 2:
				//Recive the message of leaders
				MessageTemplate mt = MessageTemplate.or(MessageTemplate.and(MessageTemplate.MatchConversationId("route-as-leaderGoing"),
						MessageTemplate.MatchPerformative(ACLMessage.INFORM)), MessageTemplate.and(MessageTemplate.MatchConversationId("route-as-leaderReturn"),
								MessageTemplate.MatchPerformative(ACLMessage.INFORM)));

				ACLMessage msg = myAgent.receive(mt);

				if(msg != null) {
					msgReceived++;
					try {
						Object[] params = (Object[]) msg.getContentObject();
						String dayHour = params[0].toString();
						@SuppressWarnings("unchecked")
						ArrayList<Integer> vehicle = (ArrayList<Integer>) params[1];
						if(msg.getConversationId().equalsIgnoreCase("route-as-leaderGoing")) {
							if(vehiclesGoing.get(dayHour) == null) {
								ArrayList<ArrayList<Integer>> vehicles = new ArrayList<ArrayList<Integer>>();
								vehicles.add(vehicle);
								vehiclesGoing.put(dayHour, vehicles);
							}else {
								ArrayList<ArrayList<Integer>> vehicles = vehiclesGoing.get(dayHour);
								vehicles.add(vehicle);
								vehiclesGoing.put(dayHour, vehicles);
							}
						} else {
							if(vehiclesReturn.get(dayHour) == null) {
								ArrayList<ArrayList<Integer>> vehicles = new ArrayList<ArrayList<Integer>>();
								vehicles.add(vehicle);
								vehiclesReturn.put(dayHour, vehicles);

							}else {
								ArrayList<ArrayList<Integer>> vehicles = vehiclesReturn.get(dayHour);
								vehicles.add(vehicle);
								vehiclesReturn.put(dayHour, vehicles);
							}			
						}
					} catch (UnreadableException e) {
						e.printStackTrace();
					}

					if(cantLeaders == msgReceived) {
						step = 3;
					}

				} else {
					block();
				}

				break;
			case 3:
				step = 4;
				break;
			case 4:
				calculateRoutingFO();
				System.out.println("Routing algorithm finished...");
				step = 5;
				break;
			default:
				break;
			}

		}

		@Override
		public boolean done() {
			return (step == 5);
		}

		private double calculateRoutingFO() {


			for(Map.Entry<String, ArrayList<ArrayList<Integer>>> allVehiclesGoing: vehiclesGoing.entrySet()) {
				ArrayList<ArrayList<Integer>> vehicles = allVehiclesGoing.getValue();
				for(ArrayList<Integer> vehicleDay: vehicles) {
					NRoutes++;
					indirectRoutes += vehicleDay.size() - 1;
					for(Integer person: vehicleDay) {
						N++;
						totalKmAgent = calculateKmAgentGoing(person, vehicleDay);
						additionalKm += totalKmAgent - distances[person][0];
						idealKm += distances[person][0];
					}
				}
			}

			for(Map.Entry<String, ArrayList<ArrayList<Integer>>> allVehiclesReturn: vehiclesReturn.entrySet()) {
				ArrayList<ArrayList<Integer>> vehicles = allVehiclesReturn.getValue();
				for(ArrayList<Integer> vehicleDay: vehicles) {
					NRoutes++;
					indirectRoutes += vehicleDay.size() - 1;
					for(Integer person: vehicleDay) {
						N++;
						totalKmAgent = calculateKmAgentReturn(person, vehicleDay);
						additionalKm += totalKmAgent - distances[0][person];
						idealKm += distances[0][person];
					}
				}
			}

			efficiency = N / NRoutes;
			promAdditionalKm = additionalKm / indirectRoutes;
			promIdealKm = idealKm / N;
			FO = promAdditionalKm / promIdealKm;

			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			cfp.setConversationId("display");
			cfp.addReceiver(airline);
			Object[] params = {vehiclesGoing, vehiclesReturn, efficiency, promAdditionalKm, promIdealKm, FO, NRoutes};
			try {
				cfp.setContentObject(params);
				myAgent.send(cfp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return FO;

		}

		private HashMap<String, ArrayList<Integer>> GenerateLeadersGoing(){
			int numCars;
			HashMap<String, ArrayList<Integer>> leaders = new HashMap<String, ArrayList<Integer>>();
			for(Map.Entry<String, ArrayList<Integer>> actual:ida.entrySet()) {
				numCars = (int) Math.ceil((double)actual.getValue().size()/2);
				ArrayList<Integer> ld = getLeaderGoing(actual.getValue(), numCars);
				removeLeaders(actual.getKey(), ld, 1);
				leaders.put(actual.getKey(), ld);
			}
			return leaders;
		}

		private HashMap<String, ArrayList<Integer>> GenerateLeadersReturn(){
			int numCars;
			HashMap<String, ArrayList<Integer>> leaders = new HashMap<String, ArrayList<Integer>>();
			for(Map.Entry<String, ArrayList<Integer>> actual:vuelta.entrySet()) {
				numCars = (int) Math.ceil((double)actual.getValue().size()/2); 
				ArrayList<Integer> ld = getLeaderReturn(actual.getValue(), numCars);
				removeLeaders(actual.getKey(), ld, 0);
				leaders.put(actual.getKey(), ld);
			}
			return leaders;
		}

		private ArrayList<Integer> getLeaderGoing(ArrayList<Integer> agents, int cantLeaders){
			int leader = -1;
			ArrayList<Integer> lst = new ArrayList<Integer>();
			double farLeader = 0;
			int i=0;
			while(i < cantLeaders) {
				leader = -1;
				farLeader = 0;
				for(int actual:agents) {
					if(distances[actual][0]>farLeader && !lst.contains(actual)) {
						leader = actual;
						farLeader = distances[actual][0];
					}
				}

				lst.add(leader);
				i++;
			}
			return lst;
		}

		private ArrayList<Integer> getLeaderReturn(ArrayList<Integer> agents, int cantLeaders){
			int leader = -1;
			ArrayList<Integer> lst = new ArrayList<Integer>();
			double farLeader = 100000;
			int i=0;
			while(i < cantLeaders) {
				leader = -1;
				farLeader = 100000;
				for(int actual:agents) {
					if(distances[0][actual] < farLeader && !lst.contains(actual)) {
						leader = actual;
						farLeader = distances[0][actual];
					}
				}
				lst.add(leader);
				i++;
			}
			return lst;
		}

		public void removeLeaders(String dayHour, ArrayList<Integer> leaders, int caso) {

			if(caso == 1) {

				ArrayList<Integer> nonLeaders = new ArrayList<Integer>();

				ArrayList<Integer> agents = ida.get(dayHour);
				for(int i = 0; i < agents.size(); i++) {
					if(!leaders.contains(agents.get(i))) {
						nonLeaders.add(agents.get(i));
					}
				}

				ida.put(dayHour, nonLeaders);

			} else {

				ArrayList<Integer> nonLeaders = new ArrayList<Integer>();

				ArrayList<Integer> agents = vuelta.get(dayHour);
				for(int i = 0; i < agents.size(); i++) {
					if(!leaders.contains(agents.get(i))) {
						nonLeaders.add(agents.get(i));
					}
				}

				vuelta.put(dayHour, nonLeaders);

			}

		}

		private void sendMessageLeadersGoing() {

			for(Map.Entry<String, ArrayList<Integer>> actual:leadersGoing.entrySet()) {
				if(ida.get(actual.getKey()).size() > 0) {
					for(Integer agent:actual.getValue()) {
						AID recep= getAgent(agent);
						ACLMessage msg = new ACLMessage(ACLMessage.QUERY_REF);
						msg.setConversationId("route-as-leaderGoing");
						if(recep!=null) {
							msg.addReceiver(recep);
							Object[] args = {actual.getKey(), ida.get(actual.getKey())};
							try {
								msg.setContentObject(args);
								myAgent.send(msg);
								cantLeaders++;
							} catch (IOException e) {
								e.printStackTrace();
							}
						}	
					}
				}else {
					ArrayList<ArrayList<Integer>> rec = new ArrayList<ArrayList<Integer>>();
					rec.add(actual.getValue());
					vehiclesGoing.put(actual.getKey(), rec);
				}
			}
		}

		private void sendMessageNonLeadersGoing() {

			for(Map.Entry<String, ArrayList<Integer>> actual: ida.entrySet()) {
				for(Integer agent: actual.getValue()) {
					AID recep= getAgent(agent);
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setConversationId("inform-size-leaders-going");
					if(recep!=null) {
						msg.addReceiver(recep);
						Object[] args = {actual.getKey(), leadersGoing.get(actual.getKey()).size()};
						try {
							msg.setContentObject(args);
							myAgent.send(msg);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}	
				}
			}

		}

		private void sendMessageLeadersReturn() {

			for(Map.Entry<String, ArrayList<Integer>> actual: leadersReturn.entrySet()) {
				if(vuelta.get(actual.getKey()).size() > 0) {
					for(Integer agent:actual.getValue()) {
						AID recep= getAgent(agent);
						ACLMessage msg = new ACLMessage(ACLMessage.QUERY_REF);
						msg.setConversationId("route-as-leaderReturn");
						if(recep!=null) {
							msg.addReceiver(recep);
							Object[] args = {actual.getKey(), vuelta.get(actual.getKey())};
							try {
								msg.setContentObject(args);
								myAgent.send(msg);
								cantLeaders++;
							} catch (IOException e) {
								e.printStackTrace();
							}
						}	
					}
				}else {
					ArrayList<ArrayList<Integer>> rec = new ArrayList<ArrayList<Integer>>();
					rec.add(actual.getValue());
					vehiclesReturn.put(actual.getKey(), rec);
				}
			}
		}

		private void sendMessageNonLeadersReturn() {

			for(Map.Entry<String, ArrayList<Integer>> actual: vuelta.entrySet()) {
				for(Integer agent: actual.getValue()) {
					AID recep= getAgent(agent);
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setConversationId("inform-size-leaders-return");
					if(recep!=null) {
						msg.addReceiver(recep);
						Object[] args = {actual.getKey(), leadersReturn.get(actual.getKey()).size()};
						try {
							msg.setContentObject(args);
							myAgent.send(msg);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}	
				}
			}

		}

		private AID getAgent(int numAgente) {
			int idAgent;
			for(AID actual: agents) {
				idAgent = Integer.parseInt(actual.getName().split("@")[0].split(" ")[1]);
				if(idAgent == numAgente) {
					return actual;
				}
			}
			return null;
		}
	}
	private class solveAbsense extends Behaviour{

		private static final long serialVersionUID = 1L;
		MessageTemplate mt =MessageTemplate.and(MessageTemplate.MatchConversationId("route-absense"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		ArrayList<Object[]> switches;
		int agent2;
		int agent1;
		String dayHour;
		String dayHour2;
		int step = 0;
		@SuppressWarnings("unchecked")
		public void action() {
			switch(step) {
			case 0:
				ACLMessage msg = myAgent.receive(mt);
				if(msg!=null) {
					try {
						switches = (ArrayList<Object[]>) msg.getContentObject();
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
					step = 1;
				}else {
					block();
				}
				break;

			case 1:
				for(Object[] actual:switches) {
					agent2 = (int) actual[0];
					dayHour = (String) actual[1];
					agent1 = (int) actual[2];
					LocalTime hourInit = LocalTime.of(Integer.parseInt(dayHour.split(" ")[1].split(":")[0]), Integer.parseInt(dayHour.split(" ")[1].split(":")[1]));
					LocalTime hourFin = hourInit.plusHours(9);
					if(!hourInit.isAfter(LocalTime.of(14, 0))) {
						if(hourFin.getHour() < 10) {
							dayHour2 = dayHour.split(" ")[0] +  " 0" + hourFin.getHour() + ":" + dayHour.split(" ")[1].split(":")[1];
						} else {
							dayHour2 = dayHour.split(" ")[0] +  " " + hourFin.getHour() + ":" + dayHour.split(" ")[1].split(":")[1];
						}
					}else {
						if(hourFin.getHour() < 10) {
							dayHour2 = returnNextDay(dayHour.split(" ")[0]) +  " 0" + hourFin.getHour() + ":" + dayHour.split(" ")[1].split(":")[1];
						} else {
							dayHour2 = returnNextDay(dayHour.split(" ")[0]) +  " " + hourFin.getHour() + ":" + dayHour.split(" ")[1].split(":")[1];
						}
					}

					if(vehiclesGoing.containsKey(dayHour)) {
						System.out.println("Changing agents on going transportation on:  " + dayHour);
						modifyGoing(agent1, dayHour, agent2);
					}

					if(vehiclesReturn.containsKey(dayHour2)) {
						System.out.println("Changing agents on return transportation on:  " + dayHour);
						modifyReturn(agent1, dayHour2, agent2);
					}
				}
				step = 2;
				break;

			case 2:
				updateFO();
				step = 3;
				break;

			default:
				block();
				break;
			}

		}
		@Override
		public boolean done() {
			return (step == 3);
		}

		private double updateFO() {

			NRoutes = 0;
			totalKmAgent = 0;
			additionalKm = 0;
			idealKm = 0;
			indirectRoutes = 0;
			N = 0;

			for(Map.Entry<String, ArrayList<ArrayList<Integer>>> allVehiclesGoing: vehiclesGoing.entrySet()) {
				ArrayList<ArrayList<Integer>> vehicles = allVehiclesGoing.getValue();
				for(ArrayList<Integer> vehicleDay: vehicles) {
					NRoutes++;
					indirectRoutes += vehicleDay.size() - 1;
					for(Integer person: vehicleDay) {
						N++;
						totalKmAgent = calculateKmAgentGoing(person, vehicleDay);
						additionalKm += totalKmAgent - distances[person][0];
						idealKm += distances[person][0];
					}
				}
			}

			for(Map.Entry<String, ArrayList<ArrayList<Integer>>> allVehiclesReturn: vehiclesReturn.entrySet()) {
				ArrayList<ArrayList<Integer>> vehicles = allVehiclesReturn.getValue();
				for(ArrayList<Integer> vehicleDay: vehicles) {
					NRoutes++;
					indirectRoutes += vehicleDay.size() - 1;
					for(Integer person: vehicleDay) {
						N++;
						totalKmAgent = calculateKmAgentReturn(person, vehicleDay);
						additionalKm += totalKmAgent - distances[0][person];
						idealKm += distances[0][person];
					}
				}
			}

			efficiency = N / NRoutes;
			promAdditionalKm = additionalKm / indirectRoutes;
			promIdealKm = idealKm / N;
			FO = promAdditionalKm / promIdealKm;

			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			cfp.setConversationId("update-absence-gui");
			cfp.addReceiver(airline);
			Object[] params = {vehiclesGoing, vehiclesReturn, efficiency, promAdditionalKm, promIdealKm, FO, NRoutes};
			try {
				cfp.setContentObject(params);
				myAgent.send(cfp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return FO;
		}

		private void modifyGoing(int agent1, String dayHour, int agent2) {

			boolean size = false;
			ArrayList<ArrayList<Integer>> vehicles = vehiclesGoing.get(dayHour);
			ArrayList<Integer> carro = null;
			ArrayList<Integer> carroDelete = null;
			ArrayList<Integer> absCar = null;
			for(ArrayList<Integer> car: vehicles) {
				for(Integer agents: car) {
					if(agents == agent1 + 1) {
						absCar = car;
						if(car.size() == 1) {
							System.out.println("The Agent " + (agent2 + 1) + " will travel alone.");
							size = true;
							car.set(0, agent2 + 1);
						}
					}
					if(car.size() > 0 && !size && !car.contains((Integer) (agent2+1))) {
						carro = VMCTemporal(car, agent2 + 1, agent1 + 1);
						if(carro != null) {
							carroDelete = car;
							break;
						}
					}
				}
			}

			//Remove absent agent
			if(!size && absCar != null) {
				absCar.remove((Integer) (agent1+1));
			}

			if(carroDelete == null && !size) {
				System.out.println("The Agent " + (agent2 + 1) + " will travel alone. Because the system did not find good option");
				ArrayList<Integer> alone = new ArrayList<Integer>();
				alone.add(agent2 + 1);
				vehicles.add(alone);
			}else if(carroDelete != null) {
				System.out.println("The Agent " + (agent2 + 1) + " will travel with other passengers. The passengers are: ");
				for(int i=0; i < carro.size(); i++) {
					System.out.print(carro.get(i) + " ");
				}
				vehicles.remove(carroDelete);
				vehicles.add(carro);
			}

			vehiclesGoing.put(dayHour, vehicles);

		}
		private void modifyReturn(int agent1, String dayHour, int agent2) {

			boolean size = false;
			ArrayList<ArrayList<Integer>> vehicles = vehiclesReturn.get(dayHour);
			ArrayList<Integer> carro = null;
			ArrayList<Integer> carroDelete = null;
			ArrayList<Integer> absCar = null;
			for(ArrayList<Integer> car:vehicles) {
				for(Integer agents: car) {
					if(agents == agent1 + 1) {
						absCar = car;
						if(car.size() == 1) {
							System.out.println("The Agent " + (agent2 + 1) + " will travel alone.");
							size = true;
							car.set(0, agent2 + 1);
						}
					}
					if(car.size() > 0 && !size && !car.contains((Integer) (agent2+1))) {
						carro = VMCTemporalR(car, agent2 + 1, agent1 + 1);
						if(carro!=null) {
							carroDelete = car;
							break;
						}
					}
				}
			}

			//Remove absent agent
			if(!size && absCar != null) {
				absCar.remove((Integer) (agent1+1));
			}

			if(carroDelete == null && !size) {
				System.out.println("The Agent " + (agent2 + 1) + " will travel alone. Because the system did not find good option");
				ArrayList<Integer> alone = new ArrayList<Integer>();
				alone.add(agent2 + 1);
				vehicles.add(alone);
			}else if(carroDelete != null) {
				System.out.println("The Agent " + (agent2 + 1) + " will travel with other passengers. The passengers are: ");
				for(int i=0; i < carro.size(); i++) {
					System.out.print(carro.get(i) + " ");
				}
				vehicles.remove(carroDelete);
				vehicles.add(carro);
			}

			vehiclesReturn.put(dayHour, vehicles);

		}

		public ArrayList<Integer> VMCTemporal(ArrayList<Integer> car, int agent, int agentRemove){
			int cant = 0;
			@SuppressWarnings("unchecked")
			ArrayList<Integer> possibleCar = (ArrayList<Integer>) car.clone();
			ArrayList<Integer> response = new ArrayList<Integer>();
			if(possibleCar.contains((Integer) agentRemove)) {
				possibleCar.remove((Integer) agentRemove);
			}
			possibleCar.add(agent);
			cant = possibleCar.size();
			int leader = leaderG(possibleCar);
			possibleCar.remove((Integer) leader);
			response.add(leader);
			double cercano;
			int agentCercano;
			double distance;
			while(response.size() != cant){
				cercano = 100000;
				agentCercano = -1;
				for(Integer actual:possibleCar) {
					if(cercano>distances[response.get(response.size()-1)][actual]) {
						cercano = distances[response.get(response.size()-1)][actual];
						agentCercano = actual;
					}
				}
				possibleCar.remove((Integer) agentCercano);
				response.add(agentCercano);
			}
			//Evaluar el promedio del nuevo agente
			distance = calculateKmAgentGoing(agent, response);
			if(distance <= promAdditionalKm) {
				return response;
			}else {
				return null;
			}

		}
		public ArrayList<Integer> VMCTemporalR(ArrayList<Integer> car, int agent, int agentRemove){
			int cant = 0;
			@SuppressWarnings("unchecked")
			ArrayList<Integer> possibleCar = (ArrayList<Integer>) car.clone();
			ArrayList<Integer> response = new ArrayList<Integer>();
			if(possibleCar.contains((Integer) agentRemove)) {
				possibleCar.remove((Integer) agentRemove);
			}
			possibleCar.add(agent);
			cant = possibleCar.size();
			int leader = leaderR(possibleCar);
			possibleCar.remove((Integer) leader);
			response.add(leader);
			double cercano;
			int agentCercano;
			double distance;
			while(response.size() != cant){
				cercano = 100000;
				agentCercano = -1;
				for(Integer actual:possibleCar) {
					if(cercano>distances[response.get(response.size()-1)][actual]) {
						cercano = distances[response.get(response.size()-1)][actual];
						agentCercano = actual;
					}
				}
				possibleCar.remove((Integer) agentCercano);
				response.add(agentCercano);
			}
			//Evaluar el promedio del nuevo agente
			distance = calculateKmAgentReturn(agent, response);
			if(distance <= promAdditionalKm) {
				return response;
			}else {
				return null;
			}

		}

		public int leaderG(ArrayList<Integer> car){
			int leader = -1;
			double mayor = 0;
			for(Integer actual:car) {
				if(mayor<distances[0][actual]) {
					leader = actual;
					mayor = distances[0][actual];
				}
			}
			return leader;
		}
		public int leaderR(ArrayList<Integer> car){
			int leader = -1;
			double menor = 100000000;
			for(Integer actual:car) {
				if(menor>distances[0][actual]) {
					leader = actual;
					menor = distances[0][actual];
				}
			}
			return leader;
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
	private double calculateKmAgentGoing(int idAgent, ArrayList<Integer> vehicle) {

		double km = 0;
		boolean start = false;

		if(vehicle.size() == 1) {
			return distances[idAgent][0];
		}

		for(int i = 0; i < vehicle.size() - 1; i++) {
			if(vehicle.get(i) == idAgent) {
				start = true;
			}
			if(start) {
				km += distances[vehicle.get(i)][vehicle.get(i + 1)];
			}
		}

		km += distances[vehicle.get(vehicle.size() - 1)][0];

		return km;
	}

	private double calculateKmAgentReturn(int idAgent, ArrayList<Integer> vehicle) {

		double km = 0;

		if(vehicle.size() == 1) {
			return distances[0][idAgent];
		}

		km += distances[0][vehicle.get(0)];

		for(int i = 0; i < vehicle.indexOf(idAgent); i++) {
			km += distances[vehicle.get(i)][vehicle.get(i + 1)];
		}

		return km;
	}
}
