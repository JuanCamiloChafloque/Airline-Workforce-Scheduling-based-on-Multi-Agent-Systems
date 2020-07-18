import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CSSupervisorGUI extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JFrame menu;
	private JLabel lblTitulo;
	private JLabel lblPopulationSize;
	private JLabel lblMaximumIterations;
	private JLabel lblCrossProbability;
	private JLabel lblMutationProbability;
	private JLabel lblElitismPercentage;
	private JLabel lblUmbral;
	private JTextField txtPopulationSize;
	private JTextField txtMaximumIterations;
	private JTextField txtCrossProbability;
	private JTextField txtMutationProbability;
	private JTextField txtElitismPercentage;
	private JTextField txtUmbral;
	private JButton btnGenetic;
	private CustomerServiceSupervisor myAgent;
	public CSSupervisorGUI(CustomerServiceSupervisor a) {
		
		myAgent = a;
		menu = new JFrame();
		menu.getContentPane().setBackground(Color.WHITE);
		menu.setSize(700, 400);
		menu.setTitle("Scheduler Genetic Algorithm");
		menu.getContentPane().setLayout(new BorderLayout());
		menu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setBackground(Color.BLACK);
		setSize(700, 600);
		setLayout(null);
		
		lblTitulo = new JLabel("Scheduler Genetic Algorithm");
		lblTitulo.setForeground(Color.WHITE);
		lblTitulo.setFont(new Font("Tahoma", Font.PLAIN, 25));
		lblTitulo.setBounds(180, 0, 350, 62);
		add(lblTitulo);
		
		
		lblPopulationSize = new JLabel("Population Size ");
		lblPopulationSize.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblPopulationSize.setForeground(Color.WHITE);
		lblPopulationSize.setBounds(177, 73, 150, 26);
		add(lblPopulationSize);
		
		txtPopulationSize = new JTextField();
		txtPopulationSize.setBounds(336, 76, 167, 26);
		txtPopulationSize.setColumns(10);
		add(txtPopulationSize);
		
		lblMaximumIterations = new JLabel("Maximum Iterations ");
		lblMaximumIterations.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblMaximumIterations.setForeground(Color.WHITE);
		lblMaximumIterations.setBounds(177, 110, 160, 26);
		add(lblMaximumIterations);
		
		txtMaximumIterations = new JTextField();
		txtMaximumIterations.setBounds(336, 110, 167, 26);
		txtMaximumIterations.setColumns(10);
		add(txtMaximumIterations);
		
		lblCrossProbability = new JLabel("Cross Probability ");
		lblCrossProbability.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblCrossProbability.setForeground(Color.WHITE);
		lblCrossProbability.setBounds(177, 144, 150, 26);
		add(lblCrossProbability);
		
		txtCrossProbability = new JTextField();
		txtCrossProbability.setBounds(336, 144, 167, 26);
		txtCrossProbability.setColumns(10);
		add(txtCrossProbability);
		
		lblMutationProbability = new JLabel("Mutation Probability ");
		lblMutationProbability.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblMutationProbability.setForeground(Color.WHITE);
		lblMutationProbability.setBounds(177, 178, 160, 26);
		add(lblMutationProbability);
		
		txtMutationProbability = new JTextField();
		txtMutationProbability.setBounds(336, 178, 167, 26);
		txtMutationProbability.setColumns(10);
		add(txtMutationProbability);
		
		lblElitismPercentage = new JLabel("Elitism Percentage ");
		lblElitismPercentage.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblElitismPercentage.setForeground(Color.WHITE);
		lblElitismPercentage.setBounds(177, 212, 150, 26);
		add(lblElitismPercentage);
		
		txtElitismPercentage = new JTextField();
		txtElitismPercentage.setBounds(336, 212, 167, 26);
		txtElitismPercentage.setColumns(10);
		add(txtElitismPercentage);
		
		lblUmbral = new JLabel("Threshold ");
		lblUmbral.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblUmbral.setForeground(Color.WHITE);
		lblUmbral.setBounds(177, 246, 150, 26);
		add(lblUmbral);
		
		txtUmbral = new JTextField();
		txtUmbral.setBounds(336, 246, 167, 26);
		txtUmbral.setColumns(10);
		add(txtUmbral);
		
		btnGenetic = new JButton("Start Algorithm");
		btnGenetic.setFont(new Font("Tahoma", Font.PLAIN, 17));
		btnGenetic.setForeground(Color.RED);
		btnGenetic.setBounds(270, 300, 150, 26);
		add(btnGenetic);
		
		menu.getContentPane().add(this);
		menu.setResizable(false);
		menu.setVisible(true);
		
		btnGenetic.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				int population = Integer.parseInt(txtPopulationSize.getText().trim());
				int maxIter = Integer.parseInt(txtMaximumIterations.getText().trim());
				float crossOver = Float.parseFloat(txtCrossProbability.getText().trim());
				float elit = Float.parseFloat(txtElitismPercentage.getText().trim());
				float mutation = Float.parseFloat(txtMutationProbability.getText().trim());
				float threashold = Float.parseFloat(txtUmbral.getText().trim());
				myAgent.InitiateGenetic(population, maxIter, mutation, crossOver, elit, threashold);
			}
		});
	}
	

}
