import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class CustomerServiceAgent extends Agent{

	private static final long serialVersionUID = 1L;

	private String name;
	private int id;
	private int initHour;
	private double coorX;
	private double coorY;
	private boolean actA;
	private boolean actB;
	private boolean actC;
	private String[][] mySchudule;
	private HashMap<String, Boolean> days = new HashMap<String, Boolean>();
	private HashMap<String, Integer> expectedProposesGoing = new HashMap<String, Integer>();
	private HashMap<String, Integer> expectedRepliesGoing = new HashMap<String, Integer>();
	private HashMap<String, Integer> expectedProposesReturn = new HashMap<String, Integer>();
	private HashMap<String, Integer> expectedRepliesReturn = new HashMap<String, Integer>();
	private ArrayList<Double> distances = new ArrayList<Double>();
	private String activities;
	private ArrayList<String> opcions;
	private AID transportSupervisor;

	@Override
	protected void setup() {
		activities = "";
		transportSupervisor = null;
		opcions = new ArrayList<String>();
		Object[] params = getArguments();
		String param = params[0].toString();

		String [] data = param.split(";");
		this.name = "Agente " + data[0].trim();
		this.setCoorX(Double.parseDouble(data[1].trim()));
		this.setCoorY(Double.parseDouble(data[2].trim()));
		this.actA = Boolean.parseBoolean(data[3].trim());
		if(actA) activities = activities + "A";
		this.actB = Boolean.parseBoolean(data[4].trim());
		if(actB) activities = activities + "B";
		this.actC = Boolean.parseBoolean(data[5].trim());
		if(actC) activities = activities + "C";
		this.days.put("Mar", Boolean.parseBoolean(data[6].trim()));
		this.days.put("Mie", Boolean.parseBoolean(data[7].trim()));
		this.days.put("Jue", Boolean.parseBoolean(data[8].trim()));
		this.days.put("Vie", Boolean.parseBoolean(data[9].trim()));
		this.days.put("Sab", Boolean.parseBoolean(data[10].trim()));
		this.days.put("Dom", Boolean.parseBoolean(data[11].trim()));
		this.days.put("Lun", Boolean.parseBoolean(data[12].trim()));
		this.days.put("Mar2", Boolean.parseBoolean(data[13].trim()));
		this.id = Integer.parseInt(this.name.split(" ")[1]) - 1;
		this.initHour = -1;

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File("./Distances.csv")));
			String line = br.readLine();
			while(line != null) {
				String [] distLine = line.split(";");
				if(distLine[0].equalsIgnoreCase(this.name)) {
					for(int i = 1; i < distLine.length; i++) {
						this.distances.add(Double.parseDouble(distLine[i].trim()));
					}
				}
				line = br.readLine();
			}

			br.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		System.out.println(this.name + " started...");

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		//Offering different services
		ServiceDescription sd = new ServiceDescription();
		sd.setType("report-timeslot");
		sd.setName("JADE-scheduling");
		dfd.addServices(sd);
		ServiceDescription sd2 = new ServiceDescription();
		sd2.setType("transport");
		sd2.setName("JADE-transport");
		dfd.addServices(sd2);
		ServiceDescription sd3 = new ServiceDescription();
		sd3.setType("situation");
		sd3.setName("JADE-transport");
		dfd.addServices(sd3);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		GeneratePossiblesActivities();
		addBehaviour(new TimeSlotConfiguration());
		addBehaviour(new asLeaderGoing());
		addBehaviour(new asLeaderReturn());
		addBehaviour(new ReceiveMessageFromLeaders());
		addBehaviour(new ReceiveMessageFromLeadersReturn());
		addBehaviour(new ReceiveExpectedProposesGoing());
		addBehaviour(new ReceiveExpectedProposesReturn());
		addBehaviour(new ReceiveDecisionsGoing());
		addBehaviour(new ReceiveDecisionsReturn());
		addBehaviour(new reciveSchedule());
		addBehaviour(new postChange());
		addBehaviour(new changeMyRest());
		addBehaviour(new awaitAbsentConfirmation());
	}

	public double getCoorX() {
		return coorX;
	}

	public void setCoorX(double coorX) {
		this.coorX = coorX;
	}

	public double getCoorY() {
		return coorY;
	}

	public void setCoorY(double coorY) {
		this.coorY = coorY;
	}

	public void GeneratePossiblesActivities()  {
		addBehaviour(new OneShotBehaviour() {

			private static final long serialVersionUID = 1L;

			public void action() {		        
				permutationWithRepeation(activities);		    
			}
		} );

	}

	public void permutationWithRepeation(String str1) {
		showPermutation(str1, "");
	}

	public void showPermutation(String str1, String NewStringToPrint) {
		if (NewStringToPrint.length() == 4) {
			opcions.add(NewStringToPrint);
			return;
		}
		for (int i = 0; i < str1.length(); i++) {

			showPermutation(str1, NewStringToPrint + str1.charAt(i));
		}
	}

	private class TimeSlotConfiguration extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("report-option"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			Double proposal;
			Double permutation;
			int hour;
			int position;
			int chr;
			String[][] config;
			String message;
			if (msg != null) {
				message = (String) msg.getContent();
				chr = Integer.parseInt(message.split(" ")[0]);
				proposal = Double.parseDouble(message.split(" ")[1]);
				permutation = proposal%1;
				hour = (int) (proposal - permutation);
				position = (int) Math.round(permutation*opcions.size());
				if(position>0) {
					position--;
				}
				config = generateSchedule(hour, position);
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				reply.setConversationId("report-option");
				Object info[] = {chr, config};
				try {
					reply.setContentObject(info);
					myAgent.send(reply);

				} catch (IOException e) {
					e.printStackTrace();
				}


			}else {
				block();
			}
		}

	} 

	private class asLeaderGoing extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		HashMap<String, ArrayList<AID>> posibilities = new HashMap<String, ArrayList<AID>>();
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("route-as-leaderGoing"),
				MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));

		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			Object[] content;
			String dayHour;
			ArrayList<Integer> options;

			if(msg != null) {

				try {
					if(transportSupervisor == null) {
						transportSupervisor = msg.getSender();
					}
					content = (Object[]) msg.getContentObject();
					dayHour = (String) content[0];
					options = (ArrayList<Integer>) content[1];
					expectedRepliesGoing.put(dayHour, options.size());
					if(expectedRepliesGoing.get(dayHour)==0) {
						System.out.println("Soy el unico en esta franja "+name);
					}
					searchAgents(dayHour, options);
					sendProposals(dayHour);

				} catch (UnreadableException e) {
					e.printStackTrace();
				}

			}else {
				block();
			}
		}

		public void searchAgents(String dayHour, ArrayList<Integer> agents) {

			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription serviceD = new ServiceDescription();
			serviceD.setType("situation");
			template.addServices(serviceD);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				for(int i=0; i < result.length; i++) {
					Integer id = Integer.parseInt(result[i].getName().getName().split("@")[0].split(" ")[1]);
					if(agents.contains(id)) {
						if(posibilities.get(dayHour) != null) {
							ArrayList<AID> aux = this.posibilities.get(dayHour);
							aux.add(result[i].getName());
							this.posibilities.put(dayHour, aux);
						}else {
							ArrayList<AID> aux = new ArrayList<AID>();
							aux.add(result[i].getName());
							this.posibilities.put(dayHour, aux);
						}
					}

				}
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}

		public void sendProposals(String dayHour) {

			ArrayList<AID> agents = this.posibilities.get(dayHour);
			for(AID actual: agents) {
				ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
				msg.setConversationId("propose-car");
				msg.addReceiver(actual);
				msg.setContent(dayHour);
				myAgent.send(msg);
			}

		}

	}

	private class asLeaderReturn extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		HashMap<String, ArrayList<AID>> posibilities = new HashMap<String, ArrayList<AID>>();
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("route-as-leaderReturn"),
				MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));

		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			Object[] content;
			String dayHour;
			ArrayList<Integer> options;

			if(msg != null) {

				try {
					if(transportSupervisor == null) {
						transportSupervisor = msg.getSender();
					}
					content = (Object[]) msg.getContentObject();
					dayHour = (String) content[0];
					options = (ArrayList<Integer>) content[1];
					expectedRepliesReturn.put(dayHour, options.size());
					if(expectedRepliesReturn.get(dayHour)==0) {
						System.out.println("Soy el unico en esta franja "+name);
					}
					searchAgents(dayHour, options);
					sendProposals(dayHour);

				} catch (UnreadableException e) {
					e.printStackTrace();
				}

			}else {
				block();
			}
		}

		public void searchAgents(String dayHour, ArrayList<Integer> agents) {

			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription serviceD = new ServiceDescription();
			serviceD.setType("situation");
			template.addServices(serviceD);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				for(int i=0; i < result.length; i++) {
					Integer id = Integer.parseInt(result[i].getName().getName().split("@")[0].split(" ")[1]);
					if(agents.contains(id)) {
						if(posibilities.get(dayHour) != null) {
							ArrayList<AID> aux = this.posibilities.get(dayHour);
							aux.add(result[i].getName());
							this.posibilities.put(dayHour, aux);
						}else {
							ArrayList<AID> aux = new ArrayList<AID>();
							aux.add(result[i].getName());
							this.posibilities.put(dayHour, aux);
						}
					}

				}
			} catch (FIPAException e) {
				e.printStackTrace();
			}
		}

		public void sendProposals(String dayHour) {

			ArrayList<AID> agents = this.posibilities.get(dayHour);
			for(AID actual: agents) {
				ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
				msg.setConversationId("propose-car-return");
				msg.addReceiver(actual);
				msg.setContent(dayHour);
				myAgent.send(msg);
			}

		}

	}

	private class ReceiveDecisionsGoing extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		MessageTemplate mt = MessageTemplate.or(MessageTemplate.and(MessageTemplate.MatchConversationId("propose-car"),
				MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)), MessageTemplate.and(MessageTemplate.MatchConversationId("propose-car"),
						MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));

		HashMap<String, ArrayList<AID>> vehicles = new HashMap<String, ArrayList<AID>>();

		@Override
		public void action() {

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				String dayHour = msg.getContent();
				expectedRepliesGoing.put(dayHour, expectedRepliesGoing.get(dayHour) - 1);

				if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					if(vehicles.get(dayHour) != null) {
						ArrayList<AID> agents = vehicles.get(dayHour);
						agents.add(msg.getSender());
						vehicles.put(dayHour, agents);
					}else {
						ArrayList<AID> agents = new ArrayList<AID>();
						agents.add(msg.getSender());
						vehicles.put(dayHour, agents);						
					}
				}

				if(expectedRepliesGoing.get(dayHour) == 0) {
					//System.out.println("El " + name + " va a rutear para el dia y hora " + dayHour);
					ArrayList<Integer> vehicle = doRouting(dayHour);
					try {
						ACLMessage report = new ACLMessage(ACLMessage.INFORM);
						report.setConversationId("route-as-leaderGoing");
						Object[] params = {dayHour, vehicle};
						report.setContentObject(params);
						report.addReceiver(transportSupervisor);
						myAgent.send(report);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}


			} else {
				block();
			}
		}

		public ArrayList<Integer> doRouting(String dayHour) {

			ArrayList<Integer> lst = new ArrayList<Integer>();
			ArrayList<AID> agents = vehicles.get(dayHour);
			lst.add(Integer.parseInt(name.split(" ")[1]));

			if(agents != null) {
				while(agents.size() > 0 && lst.size() < 4) {
					AID closest = vmc(agents);
					lst.add(Integer.parseInt(closest.getName().split("@")[0].split(" ")[1]));
					agents.remove(closest);
				}
			}

			return lst;

		}

		public AID vmc (ArrayList<AID> agents) {

			double menor = 1000000;
			AID bestAgent = null;

			for(AID actual: agents) {
				int idAgent = Integer.parseInt(actual.getName().split("@")[0].split(" ")[1]);
				if(distances.get(idAgent) < menor) {
					bestAgent = actual;
					menor = distances.get(idAgent);
				}
			}

			return bestAgent;

		}
	}

	private class ReceiveDecisionsReturn extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		MessageTemplate mt = MessageTemplate.or(MessageTemplate.and(MessageTemplate.MatchConversationId("propose-car-return"),
				MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)), MessageTemplate.and(MessageTemplate.MatchConversationId("propose-car-return"),
						MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));

		HashMap<String, ArrayList<AID>> vehicles = new HashMap<String, ArrayList<AID>>();

		@Override
		public void action() {

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				String dayHour = msg.getContent();
				expectedRepliesReturn.put(dayHour, expectedRepliesReturn.get(dayHour) - 1);

				if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
					if(vehicles.get(dayHour) != null) {
						ArrayList<AID> agents = vehicles.get(dayHour);
						agents.add(msg.getSender());
						vehicles.put(dayHour, agents);
					}else {
						ArrayList<AID> agents = new ArrayList<AID>();
						agents.add(msg.getSender());
						vehicles.put(dayHour, agents);						
					}
				}

				if(expectedRepliesReturn.get(dayHour) == 0) {
					ArrayList<Integer> vehicle = doRouting(dayHour);
					try {
						ACLMessage report = new ACLMessage(ACLMessage.INFORM);
						report.setConversationId("route-as-leaderReturn");
						Object[] params = {dayHour, vehicle};
						report.setContentObject(params);
						report.addReceiver(transportSupervisor);
						myAgent.send(report);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				block();
			}
		}

		public ArrayList<Integer> doRouting(String dayHour) {

			ArrayList<Integer> lst = new ArrayList<Integer>();
			ArrayList<AID> agents = vehicles.get(dayHour);
			lst.add(Integer.parseInt(name.split(" ")[1]));

			if(agents != null) {
				while(agents.size() > 0 && lst.size() < 4) {
					AID closest = vmc(agents);
					lst.add(Integer.parseInt(closest.getName().split("@")[0].split(" ")[1]));
					agents.remove(closest);
				}
			} 

			return lst;

		}

		public AID vmc (ArrayList<AID> agents) {

			double menor = 1000000;
			AID bestAgent = null;

			for(AID actual: agents) {
				int idAgent = Integer.parseInt(actual.getName().split("@")[0].split(" ")[1]);
				if(distances.get(idAgent) < menor) {
					bestAgent = actual;
					menor = distances.get(idAgent);
				}
			}

			return bestAgent;

		}
	}


	private class ReceiveExpectedProposesGoing extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("inform-size-leaders-going"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		@Override
		public void action() {

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				try {
					Object[] param = (Object[]) msg.getContentObject();
					String dayHour = param[0].toString();
					int cantReplies = Integer.parseInt(param[1].toString());
					expectedProposesGoing.put(dayHour, cantReplies);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}else {
				block();
			}
		}

	}

	private class ReceiveExpectedProposesReturn extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("inform-size-leaders-return"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		@Override
		public void action() {

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				try {
					Object[] param = (Object[]) msg.getContentObject();
					String dayHour = param[0].toString();
					int cantReplies = Integer.parseInt(param[1].toString());
					expectedProposesReturn.put(dayHour, cantReplies);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}else {
				block();
			}
		}
	}

	private class ReceiveMessageFromLeaders extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("propose-car"),
				MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

		HashMap<String, ArrayList<AID>> leaders = new HashMap<String, ArrayList<AID>>();
		HashMap<String, AID> bestLeader = new HashMap<String, AID>();

		@Override
		public void action() {

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				String dayHour = msg.getContent();
				try {
					expectedProposesGoing.put(dayHour, expectedProposesGoing.get(dayHour) - 1);
				}catch (NullPointerException e) {
					System.out.println("Tuve errores con este valor: "+dayHour+" : "+expectedProposesGoing.get(dayHour));
				}

				if(leaders.get(dayHour) != null) {
					ArrayList<AID> agents = leaders.get(dayHour);
					agents.add(msg.getSender());
					leaders.put(dayHour, agents);
				} else {
					ArrayList<AID> agents = new ArrayList<AID>();
					agents.add(msg.getSender());
					leaders.put(dayHour, agents);
				}

				if(bestLeader.get(dayHour) == null) {
					bestLeader.put(dayHour, msg.getSender());
				} else {
					if(compareDistancesLeaders(dayHour, msg.getSender())) {
						bestLeader.put(dayHour, msg.getSender());
					}
				}

				if(expectedProposesGoing.get(dayHour) == 0) {
					sendDecision(dayHour);
				}

			}else {
				block();
			}
		}

		public void sendDecision(String dayHour) {

			ArrayList<AID> lideres = leaders.get(dayHour);
			AID best = bestLeader.get(dayHour);

			for(AID agent: lideres) {
				ACLMessage msg;
				if(agent.equals(best)) {
					msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				} else {
					msg = new ACLMessage(ACLMessage.REFUSE);
				}
				msg.setConversationId("propose-car");
				msg.addReceiver(agent);
				msg.setContent(dayHour);
				myAgent.send(msg);
			}

		}

		public boolean compareDistancesLeaders(String dayHour, AID newLeader) {

			int idAgentNew = Integer.parseInt(newLeader.getName().split("@")[0].split(" ")[1]);
			int idBestAgent = Integer.parseInt(bestLeader.get(dayHour).getName().split("@")[0].split(" ")[1]);

			double distNew = distances.get(idAgentNew);
			double distBest = distances.get(idBestAgent);

			if(distNew < distBest) {
				return true;
			} else {
				return false;
			}

		}
	}

	private class ReceiveMessageFromLeadersReturn extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("propose-car-return"),
				MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));

		HashMap<String, ArrayList<AID>> leaders = new HashMap<String, ArrayList<AID>>();
		HashMap<String, AID> bestLeader = new HashMap<String, AID>();

		@Override
		public void action() {

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				String dayHour = msg.getContent();
				try {
					expectedProposesReturn.put(dayHour, expectedProposesReturn.get(dayHour) - 1);
				}catch (NullPointerException e) {
					System.out.println("Tuve errores con este valor: "+dayHour+" : "+expectedProposesReturn.get(dayHour));
				}
				if(leaders.get(dayHour) != null) {
					ArrayList<AID> agents = leaders.get(dayHour);
					agents.add(msg.getSender());
					leaders.put(dayHour, agents);
				} else {
					ArrayList<AID> agents = new ArrayList<AID>();
					agents.add(msg.getSender());
					leaders.put(dayHour, agents);
				}

				if(bestLeader.get(dayHour) == null) {
					bestLeader.put(dayHour, msg.getSender());
				} else {
					if(compareDistancesLeaders(dayHour, msg.getSender())) {
						bestLeader.put(dayHour, msg.getSender());
					}
				}

				if(expectedProposesReturn.get(dayHour) == 0) {
					sendDecision(dayHour);
				}

			}else {
				block();
			}
		}

		public void sendDecision(String dayHour) {

			ArrayList<AID> lideres = leaders.get(dayHour);
			AID best = bestLeader.get(dayHour);

			for(AID agent: lideres) {
				ACLMessage msg;
				if(agent.equals(best)) {
					msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				} else {
					msg = new ACLMessage(ACLMessage.REFUSE);
				}
				msg.setConversationId("propose-car-return");
				msg.addReceiver(agent);
				msg.setContent(dayHour);
				myAgent.send(msg);
			}

		}

		public boolean compareDistancesLeaders(String dayHour, AID newLeader) {

			int idAgentNew = Integer.parseInt(newLeader.getName().split("@")[0].split(" ")[1]);
			int idBestAgent = Integer.parseInt(bestLeader.get(dayHour).getName().split("@")[0].split(" ")[1]);

			double distNew = distances.get(idAgentNew);
			double distBest = distances.get(idBestAgent);

			if(distNew < distBest) {
				return true;
			} else {
				return false;
			}

		}
	}

	private class reciveSchedule extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("report-decision"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			if(msg!=null) {
				try {
					double info = (Double) msg.getContentObject();
					double permutation = info % 1;
					initHour = (int) (info - permutation);
					int position = (int) Math.round(permutation*opcions.size());
					if(position>0) {
						position--;
					}
					mySchudule = generateSchedule(initHour, position);
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}else {
				block();
			}

		}

	}
	private String[][] generateSchedule(int hour, int position){
		String[][] config = new String[8][2];
		String date = getHour(hour);
		String selection = opcions.get(position);
		for(Map.Entry<String, Boolean> actual: days.entrySet()) {
			String key = actual.getKey();
			Boolean value = actual.getValue();
			switch (key) {
			case "Mar":
				config[0][0] = "Mar "+date;
				if(value) {
					config[0][1] = selection;
				}else {
					config[0][1] = "LLLL";
				}
				break;
			case "Mie":
				config[1][0] = "Mie "+date;
				if(value) {
					config[1][1] = selection;
				}else {
					config[1][1] = "LLLL";
				}
				break;
			case "Jue":
				config[2][0] = "Jue "+date;
				if(value) {
					config[2][1] = selection;
				}else {
					config[2][1] = "LLLL";
				}
				break;
			case "Vie":
				config[3][0] = "Vie "+date;
				if(value) {
					config[3][1] = selection;
				}else {
					config[3][1] = "LLLL";
				}
				break;
			case "Sab":
				config[4][0] = "Sab "+date;
				if(value) {
					config[4][1] = selection;
				}else {
					config[4][1] = "LLLL";
				}
				break;
			case "Dom":
				config[5][0] = "Dom "+date;
				if(value) {
					config[5][1] = selection;
				}else {
					config[5][1] = "LLLL";
				}
				break;
			case "Lun":
				config[6][0] = "Lun "+date;
				if(value) {
					config[6][1] = selection;
				}else {
					config[6][1] = "LLLL";
				}
				break;
			case "Mar2":
				config[7][0] = "Mar2 "+date;
				if(value) {
					config[7][1] = selection;
				}else {
					config[7][1] = "LLLL";
				}
				break;
			default:
				break;
			}
		}
		return config;
	}
	public String getHour(int hour) {

		String select;
		select = "";
		int entero = hour / 2;
		if(entero < 10) {
			select = "0" + select + entero ;
		}else {
			select = select + entero ;
		}
		if(hour%2!=0) {
			select = select + ":30";
		}else {
			select = select + ":00";
		}

		return select;
	}
	private class postChange extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("change-activity"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		@Override
		public void action() {
			ACLMessage msg = myAgent.receive(mt);
			if(msg!=null) {
				try {
					Object[] content = (Object[]) msg.getContentObject();
					String dayHour = content[0].toString();
					char act = content[1].toString().charAt(0);
					if(evaluateAbilty(act)) {
						int numDay = getNumDay(dayHour);
						int franja = working(dayHour, numDay, act);
						int before; //1. Si el slot es el del día, 0. Si el slot es el del día anterior
						if(franja != -1) {
							String slot;
							char change;
							int pond = 0;
							if(franja < 4) {
								before = numDay;
								slot = mySchudule[numDay][1];
								change = slot.charAt(franja);
							}else {
								slot = mySchudule[numDay - 1][1];
								franja = franja - 4;
								before = numDay - 1;
								change = slot.charAt(franja);
							}
							if(change == 'A') {
								pond = 10;
							} else if(change == 'B') {
								if(act == 'A') {
									pond = 5;
								} else if(act == 'C') {
									pond = 2;
								}
							} else if(change == 'C') {
								if(act == 'A') {
									pond = 5;
								} else if(act == 'B') {
									pond = 2;
								}
							}

							ACLMessage reply = msg.createReply();
							Object[] params = {name.split(" ")[1], before, franja, pond};
							reply.setContentObject(params);
							reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
							reply.setConversationId("change-activity");
							myAgent.send(reply);
						}else {
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setConversationId("change-activity");
							myAgent.send(reply);
						}
					}
				} catch (UnreadableException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}else {
				block();
			}
		}

	}
	private boolean evaluateAbilty(char act) {
		if(act=='A') {
			return actA;
		}
		if(act=='B') {
			return actB;
		}
		if(act=='C') {
			return actC;
		}
		return true;

	}
	private int getNumDay(String dayHour) {
		String day = dayHour.split(" ")[0];
		if(day.equalsIgnoreCase("Mar")) return 0;
		if(day.equalsIgnoreCase("Mie")) return 1;
		if(day.equalsIgnoreCase("Jue")) return 2;
		if(day.equalsIgnoreCase("Vie")) return 3;
		if(day.equalsIgnoreCase("Sab")) return 4;
		if(day.equalsIgnoreCase("Dom")) return 5;
		if(day.equalsIgnoreCase("Lun")) return 6;
		if(day.equalsIgnoreCase("Mar2")) return 7;
		return -1;
	}

	private int working(String dayHour, int numDay, char act) {
		String slot = mySchudule[numDay][1];
		LocalTime hour = LocalTime.of(Integer.parseInt(mySchudule[numDay][0].split(" ")[1].split(":")[0]), Integer.parseInt(mySchudule[numDay][0].split(" ")[1].split(":")[1]));
		LocalTime hourAna = LocalTime.of(Integer.parseInt(dayHour.split(" ")[1].split(":")[0]), Integer.parseInt(dayHour.split(" ")[1].split(":")[1]));
		LocalTime endTurn = hour.plusHours(9);
		String slotPre;
		LocalTime hourPre = null;
		LocalTime endTurnPre = null;
		if(!slot.contains("L")) {
			if(hourAna.compareTo(endTurn)<0 && hourAna.compareTo(hour)>0) {
				if(hour.equals(hourAna) && slot.charAt(0)!=act) {
					System.out.println(name + " can attend the unexpected peak of demand by changing the first slot.");
					return 0;
				}else if(hour.plusMinutes(120).equals(hourAna)&& slot.charAt(1)!=act) {
					System.out.println(name + " can attend the unexpected peak of demand by changing the second slot.");
					return 1;
				}else if(hour.plusMinutes(120+150).equals(hourAna)&&slot.charAt(2)!=act) {
					System.out.println(name + " can attend the unexpected peak of demand by changing the third slot.");
					return 2;
				}else if(hour.plusMinutes(120+150+150).equals(hourAna)&&slot.charAt(3)!=act) {
					System.out.println(name + " can attend the unexpected peak of demand by changing the fourth slot.");
					return 3;
				}
			}
		}

		if(numDay - 1 > 0) {
			slotPre = mySchudule[numDay - 1][1];
			if(!slotPre.contains("L")) {
				hourPre = LocalTime.of(Integer.parseInt(mySchudule[numDay-1][0].split(" ")[1].split(":")[0]), Integer.parseInt(mySchudule[numDay-1][0].split(" ")[1].split(":")[1]));
				endTurnPre = hourPre.plusHours(9);
				if(hourAna.compareTo(endTurnPre)<0 && ChronoUnit.HOURS.between(hourAna, endTurnPre)<=9) {
					if(hourPre.equals(hourAna) && slotPre.charAt(0)!=act) {
						System.out.println(name + " can attend the unexpected peak of demand by changing the first slot of the previous shift.");
						return 4;
					}else if(hourPre.plusMinutes(120).equals(hourAna)&& slotPre.charAt(1)!=act) {
						System.out.println(name + " can attend the unexpected peak of demand by changing the second slot of the previous shift.");
						return 5;
					}else if(hourPre.plusMinutes(120+150).equals(hourAna)&&slotPre.charAt(2)!=act) {
						System.out.println(name + " can attend the unexpected peak of demand by changing the third slot of the previous shift.");
						return 6;
					}else if(hourPre.plusMinutes(120+150+150).equals(hourAna)&&slotPre.charAt(3)!=act) {
						System.out.println(name + " can attend the unexpected peak of demand by changing the fourth slot of the previous shift.");
						return 7;
					}
				}
			}
		}
		System.out.println(name + " can't attend the unexpected peak of demand.");
		return -1;
	}

	private class changeMyRest extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("change-rest"),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		String dayHour;
		int numDay;
		int cap;
		String perm;
		public void action() {
			cap = 0;
			ACLMessage msg = myAgent.receive(mt);
			if(msg!=null) {
				dayHour = msg.getContent().split(";")[0];
				numDay = Integer.parseInt(msg.getContent().split(";")[1]);
				perm = msg.getContent().split(";")[2];
				//Verificar si ese dia lo tengo libre
				if(mySchudule[numDay][1].equals("LLLL")) {
					if(evaluateHour(numDay, dayHour)) {
						String myHour = getHour(initHour);
						LocalTime initH = LocalTime.of(Integer.parseInt(myHour.split(":")[0]), Integer.parseInt(myHour.split(":")[1]));
						LocalTime attendH = LocalTime.of(Integer.parseInt(dayHour.split(" ")[1].split(":")[0]), Integer.parseInt(dayHour.split(" ")[1].split(":")[1]));
						long diff = Math.abs(ChronoUnit.HOURS.between(initH, attendH));

						//System.out.println(name + " is available and is sending proposal.");
						String prop = "";
						for(char act: perm.toCharArray()) {
							switch(act) {
							case 'A':
								if(actA) {
									cap++;
									prop += "A";
								}else {
									prop += randActivity();
								}
								break;
							case 'B':
								if(actB) {
									cap++;
									prop += "B";
								}else {
									prop += randActivity();
								}
								break;
							case 'C':
								if(actC) {
									cap++;
									prop += "C";
								}else {
									prop += randActivity();
								}
								break;
							}
						}

						ACLMessage reply = msg.createReply();
						reply.setConversationId(msg.getConversationId());
						reply.setPerformative(ACLMessage.PROPOSE);
						Object[] params = {id, cap, diff, prop};
						try {
							reply.setContentObject(params);
							myAgent.send(reply);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}else {
						ACLMessage reply = msg.createReply();
						reply.setConversationId(msg.getConversationId());
						reply.setPerformative(ACLMessage.REFUSE);
						myAgent.send(reply);
					}

				}else {
					ACLMessage reply = msg.createReply();
					reply.setConversationId(msg.getConversationId());
					reply.setPerformative(ACLMessage.REFUSE);
					myAgent.send(reply);
				}
			}else {
				block();
			}
		}

		private String randActivity() {

			String act = "";
			int opt;
			boolean found = false;

			while(!found) {
				opt = (int) Math.floor(Math.random() * 2);
				if(opt == 0 && actA) {
					act = "A";
					found = true;
				}else if(opt == 1 && actB) {
					act = "B";
					found = true;
				}else if(opt == 2 && actC) {
					act ="C";
					found = true;
				}
			}
			return act;

		}

		private boolean evaluateHour(int numDay, String dayHour) {
			Boolean hab = false;
			LocalTime hourProp = LocalTime.of(Integer.parseInt(dayHour.split(" ")[1].split(":")[0]), 
					Integer.parseInt(dayHour.split(" ")[1].split(":")[1]));
			LocalTime hourNext;
			LocalTime hourPrev;
			if(numDay == 0) {
				//Verificar hacia adelante
				if(mySchudule[numDay + 1][1].equals("LLLL")) {
					return true;
				}
				hourNext = LocalTime.of(Integer.parseInt(mySchudule[numDay+1][0].split(" ")[1].split(":")[0]), 
						Integer.parseInt(mySchudule[numDay+1][0].split(" ")[1].split(":")[1]));
				hourNext = hourNext.minusHours(21);
				if(hourProp.compareTo(hourNext) <= 0) {
					hab = true;
				}

			}else if(numDay > 0 && numDay < 7) {
				//Las dos
				hourNext = LocalTime.of(Integer.parseInt(mySchudule[numDay+1][0].split(" ")[1].split(":")[0]), 
						Integer.parseInt(mySchudule[numDay+1][0].split(" ")[1].split(":")[1]));
				hourPrev = LocalTime.of(Integer.parseInt(mySchudule[numDay-1][0].split(" ")[1].split(":")[0]), 
						Integer.parseInt(mySchudule[numDay-1][0].split(" ")[1].split(":")[1]));
				hourPrev = hourPrev.plusHours(21);
				hourNext = hourNext.minusHours(21);

				if(mySchudule[numDay + 1][1].equals("LLLL") && mySchudule[numDay - 1][1].equals("LLLL")) {
					return true;
				}

				if(mySchudule[numDay + 1][1].equals("LLLL")) {
					if(hourProp.compareTo(hourPrev) >= 0) {
						return true;
					}
				}

				if(mySchudule[numDay - 1][1].equals("LLLL")) {
					if(hourProp.compareTo(hourNext) <= 0) {
						return true;
					}
				}

				if(hourProp.compareTo(hourPrev) >= 0 && hourProp.compareTo(hourNext) <= 0) {
					hab = true;
				}

			}else if(numDay == 7) {
				//Verificar hacia atras
				if(mySchudule[numDay-1][1].equals("LLLL")) {
					return true;
				}
				hourPrev = LocalTime.of(Integer.parseInt(mySchudule[numDay-1][0].split(" ")[1].split(":")[0]), 
						Integer.parseInt(mySchudule[numDay-1][0].split(" ")[1].split(":")[1]));
				hourPrev = hourPrev.plusHours(21);
				if(hourProp.compareTo(hourPrev) >= 0) {
					hab = true;
				}
			}
			return hab;
		}
	}

	private class awaitAbsentConfirmation extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;
		private MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("absent-change-accepted"),
				MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));

		@Override
		public void action() {

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				try {
					Object[] newSol = (Object[]) msg.getContentObject();
					mySchudule = (String[][]) newSol[0];
					//System.out.println(name + " received confirmation and updated the schedule.");
				} catch (UnreadableException e) {
					e.printStackTrace();
				}
			}else {
				block();
			}
		}
	}
}