import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;


public class TransportSupervisorGUI extends JPanel {

	private static final long serialVersionUID = 1L;

	private JFrame menu;
	private JLabel lblTitulo;
	private JLabel lblFOTotal;
	private JLabel lblPenalization;
	private JLabel lblAditionalKm;
	private JLabel lblIdealDistance;
	private JLabel lblEfficiency;
	private JLabel lblIda;
	private JLabel lblVuelta;
	private JTextField txtFuncionObjetivo;
	private JTextField txtPenalization;
	private JTextField txtAditionalKm;
	private JTextField txtIdealDistance;
	private JTextField txtEfficiency;
	private JScrollPane barraArrastreIda;
	private JTable tblIda;
	private JScrollPane barraArrastreVuelta;
	private JTable tblVuelta;
	private DefaultTableModel modelIda;
	private DefaultTableModel modelVuelta;

	public TransportSupervisorGUI(HashMap<String, ArrayList<ArrayList<Integer>>> idas, HashMap<String, ArrayList<ArrayList<Integer>>> vueltas, double efficiency, double promAdditional, double promIdeal) {

		menu = new JFrame();
		menu.getContentPane().setBackground(Color.WHITE);
		menu.setSize(700, 650);
		menu.setTitle("Airline Scheduling System");
		menu.getContentPane().setLayout(new BorderLayout());
		menu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		DecimalFormat df = new DecimalFormat("#.##");		

		setBackground(Color.BLACK);
		setSize(700, 700);
		setLayout(null);

		lblTitulo = new JLabel("Transport Routing System");
		lblTitulo.setForeground(Color.WHITE);
		lblTitulo.setFont(new Font("Tahoma", Font.PLAIN, 25));
		lblTitulo.setBounds(200, 0, 300, 62);
		add(lblTitulo);


		lblFOTotal = new JLabel("Wellness OF ");
		lblFOTotal.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblFOTotal.setForeground(Color.WHITE);
		lblFOTotal.setBounds(177, 73, 100, 26);
		add(lblFOTotal);

		txtFuncionObjetivo = new JTextField("" + df.format(0));
		txtFuncionObjetivo.setBounds(336, 76, 167, 26);
		txtFuncionObjetivo.setColumns(10);
		txtFuncionObjetivo.setEditable(false);
		add(txtFuncionObjetivo);

		lblPenalization = new JLabel("Penalization % ");
		lblPenalization.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblPenalization.setForeground(Color.WHITE);
		lblPenalization.setBounds(177, 110, 150, 26);
		add(lblPenalization);

		txtPenalization = new JTextField("" + df.format(0));
		txtPenalization.setBounds(336, 110, 167, 26);
		txtPenalization.setColumns(10);
		txtPenalization.setEditable(false);
		add(txtPenalization);

		lblAditionalKm = new JLabel("Additional Km ");
		lblAditionalKm.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblAditionalKm.setForeground(Color.WHITE);
		lblAditionalKm.setBounds(177, 144, 150, 26);
		add(lblAditionalKm);

		txtAditionalKm = new JTextField("" + df.format(0));
		txtAditionalKm.setBounds(336, 144, 167, 26);
		txtAditionalKm.setColumns(10);
		txtAditionalKm.setEditable(false);
		add(txtAditionalKm);

		lblIdealDistance = new JLabel("Ideal Distance ");
		lblIdealDistance.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblIdealDistance.setForeground(Color.WHITE);
		lblIdealDistance.setBounds(177, 178, 150, 26);
		add(lblIdealDistance);

		txtIdealDistance = new JTextField("" + df.format(0));
		txtIdealDistance.setBounds(336, 178, 167, 26);
		txtIdealDistance.setColumns(10);
		txtIdealDistance.setEditable(false);
		add(txtIdealDistance);

		lblEfficiency = new JLabel("Efficiency ");
		lblEfficiency.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblEfficiency.setForeground(Color.WHITE);
		lblEfficiency.setBounds(177, 212, 150, 26);
		add(lblEfficiency);

		txtEfficiency = new JTextField("" + df.format(0));
		txtEfficiency.setBounds(336, 212, 167, 26);
		txtEfficiency.setColumns(10);
		txtEfficiency.setEditable(false);
		add(txtEfficiency);

		lblIda = new JLabel("Transportation from households to airport ");
		lblIda.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblIda.setForeground(Color.WHITE);
		lblIda.setBounds(18, 240, 300, 26);
		add(lblIda);

		barraArrastreIda = new JScrollPane();
		barraArrastreIda.setBounds(18, 270, 650, 120);
		tblIda = new JTable();
		barraArrastreIda.setViewportView(tblIda);
		add(barraArrastreIda);

		lblVuelta = new JLabel("Transportation from airport to households ");
		lblVuelta.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblVuelta.setForeground(Color.WHITE);
		lblVuelta.setBounds(18, 430, 300, 26);
		add(lblVuelta);

		barraArrastreVuelta = new JScrollPane();
		barraArrastreVuelta.setBounds(18, 450, 650, 120);
		tblVuelta = new JTable();
		barraArrastreVuelta.setViewportView(tblVuelta);
		add(barraArrastreVuelta);

		menu.getContentPane().add(this);
		menu.setResizable(false);
		menu.setVisible(true);
		modelIda = new DefaultTableModel();
		modelVuelta = new DefaultTableModel();
	}

	public void displayFO(HashMap<String, ArrayList<ArrayList<Integer>>> idas, HashMap<String, ArrayList<ArrayList<Integer>>> vueltas, double efficiency, double promAdditional, double promIdeal) {

		DecimalFormat df = new DecimalFormat("#.##");	
		
		
		if (modelIda.getRowCount() > 0) {
		    for (int i = modelIda.getRowCount() - 1; i > -1; i--) {
		    	modelIda.removeRow(i);
		    }
		}
		if (modelVuelta.getRowCount() > 0) {
		    for (int i = modelVuelta.getRowCount() - 1; i > -1; i--) {
		    	modelVuelta.removeRow(i);
		    }
		}
		tblIda.removeAll();
		tblVuelta.removeAll();

		txtFuncionObjetivo.setEditable(true);
		txtFuncionObjetivo.setText("" + df.format(promAdditional / promIdeal));
		txtFuncionObjetivo.setEditable(false);

		txtPenalization.setEditable(true);
		txtPenalization.setText("" + df.format((promAdditional / promIdeal) * 100));
		txtPenalization.setEditable(false);

		txtAditionalKm.setEditable(true);
		txtAditionalKm.setText("" + df.format(promAdditional));
		txtAditionalKm.setEditable(false);

		txtIdealDistance.setEditable(true);
		txtIdealDistance.setText("" + df.format(promIdeal));
		txtIdealDistance.setEditable(false);

		txtEfficiency.setEditable(true);
		txtEfficiency.setText("" + df.format(efficiency));
		txtEfficiency.setEditable(false);

		String[] dataHeader = {"Vehicle Id", "Day", "Hour", "Passenger 1 Id", "Passenger 2 Id", "Passenger 3 Id", "Passenger 4 Id"};
		Vector<String> header = new Vector<String>(Arrays.asList(dataHeader));

		Vector<String> viewIda = new Vector<String>();
		Vector<List<String>> agentDataIda = new Vector<List<String>>();

		Vector<String> viewVuelta = new Vector<String>();
		Vector<List<String>> agentDataVuelta = new Vector<List<String>>();

		int idIda = 0;

		for(Map.Entry<String, ArrayList<ArrayList<Integer>>> ida: idas.entrySet()) {
			for(ArrayList<Integer> vehicles: ida.getValue()) {
				viewIda = new Vector<String>();
				viewIda.add("" + (++idIda));
				viewIda.add(ida.getKey().split(" ")[0]);
				viewIda.add(ida.getKey().split(" ")[1]);
				switch(vehicles.size()) {
				case 1:
					viewIda.add("" + -vehicles.get(0));
					viewIda.add("");
					viewIda.add("");
					viewIda.add("");
					break;
				case 2:
					viewIda.add("" + -vehicles.get(0));
					viewIda.add("" + -vehicles.get(1));
					viewIda.add("");
					viewIda.add("");
					break;
				case 3:
					viewIda.add("" + -vehicles.get(0));
					viewIda.add("" + -vehicles.get(1));
					viewIda.add("" + -vehicles.get(2));
					viewIda.add("");
					break;
				case 4:
					viewIda.add("" + -vehicles.get(0));
					viewIda.add("" + -vehicles.get(1));
					viewIda.add("" + -vehicles.get(2));
					viewIda.add("" + -vehicles.get(3));
					break;
				}
				agentDataIda.add(viewIda);
			}
		}

		int idVuelta = 0;

		for(Map.Entry<String, ArrayList<ArrayList<Integer>>> vuel: vueltas.entrySet()) {
			for(ArrayList<Integer> vehicles: vuel.getValue()) {
				viewVuelta = new Vector<String>();
				viewVuelta.add("" + (++idVuelta));
				viewVuelta.add(vuel.getKey().split(" ")[0]);
				viewVuelta.add(vuel.getKey().split(" ")[1]);
				switch(vehicles.size()) {
				case 1:
					viewVuelta.add("" + vehicles.get(0));
					viewVuelta.add("");
					viewVuelta.add("");
					viewVuelta.add("");
					break;
				case 2:
					viewVuelta.add("" + vehicles.get(0));
					viewVuelta.add("" + vehicles.get(1));
					viewVuelta.add("");
					viewVuelta.add("");
					break;
				case 3:
					viewVuelta.add("" + vehicles.get(0));
					viewVuelta.add("" + vehicles.get(1));
					viewVuelta.add("" + vehicles.get(2));
					viewVuelta.add("");
					break;
				case 4:
					viewVuelta.add("" + vehicles.get(0));
					viewVuelta.add("" + vehicles.get(1));
					viewVuelta.add("" + vehicles.get(2));
					viewVuelta.add("" + vehicles.get(3));
					break;
				}
				agentDataVuelta.add(viewVuelta);
			}
		}

		modelIda.setColumnIdentifiers(header);
		for(List<String> actual:agentDataIda) {
			modelIda.addRow((Vector<?>) actual);
		}
		tblIda.setModel(modelIda);
		tblIda.repaint();

		modelVuelta.setColumnIdentifiers(header);
		for(List<String> actual:agentDataVuelta) {
			modelVuelta.addRow((Vector<?>) actual);
		}
		tblVuelta.setModel(modelVuelta);
		tblVuelta.repaint();
	}
}
