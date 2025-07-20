package www.etis_midi.fr.panda.main;

import com.hp.hpl.jena.rdf.model.Model;
import www.etis_midi.fr.panda.algorithm.PandaTopKPattern;
import www.etis_midi.fr.panda.mapper.*;
import www.etis_midi.fr.panda.model.Pattern;
import www.etis_midi.fr.panda.output.PatternWritter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A graphical user interface to run the Semsum+ algorithm on an RDF file
 * and generate a summary RDF graph.
 */
public class MainGUI extends JFrame {
    private static final long serialVersionUID = 1L;
	private JTextField inputFileField;
    private JTextField outputFileField;
    private JButton browseInputBtn, browseOutputBtn, runBtn;
    private JTextArea logArea;

    public MainGUI() {
        setTitle("RDF Summary Graph Generator (Semsum+)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout(10, 10));

        // Build GUI layout
        initComponents();
        setupActions();
    }

    /**
     * Initializes and lays out GUI components.
     */
    private void initComponents() {
        JPanel topPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "File Selection", TitledBorder.LEFT, TitledBorder.TOP));

        inputFileField = new JTextField();
        outputFileField = new JTextField();
        browseInputBtn = new JButton("Browse...");
        browseOutputBtn = new JButton("Browse...");
        runBtn = new JButton("Generate Summary");

        topPanel.add(new JLabel("Input RDF File:"));
        topPanel.add(inputFileField);
        topPanel.add(browseInputBtn);

        topPanel.add(new JLabel("Output RDF Summary File:"));
        topPanel.add(outputFileField);
        topPanel.add(browseOutputBtn);

        topPanel.add(new JLabel()); // spacer
        topPanel.add(runBtn);
        topPanel.add(new JLabel()); // spacer

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Execution Log"));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Sets up the button click actions.
     */
    private void setupActions() {
        browseInputBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                inputFileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        browseOutputBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                outputFileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        runBtn.addActionListener(e -> {
            String inputFile = inputFileField.getText().trim();
            String outputFile = outputFileField.getText().trim();

            if (inputFile.isEmpty() || outputFile.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select both input and output files.");
                return;
            }

            runBtn.setEnabled(false);
            log("🔄 Starting summary generation...");
            new Thread(() -> {
                runSummary(inputFile, outputFile);
                runBtn.setEnabled(true);
            }).start();
        });
    }

    /**
     * Executes the RDF summarization pipeline.
     */
    private void runSummary(String inputFile, String outputFile) {
        try {
            log("📂 Loading RDF model from: " + inputFile);
            Model model = RDFModelLoader.load(inputFile);

            MatrixBuilder builder = new MatrixBuilder(model);
            builder.build();

            int[][] D = builder.getMatrix();
            HashMap<String, Integer> predicateMap = (HashMap<String, Integer>) builder.getPredicateMap();
            HashMap<Integer, String> itemMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : predicateMap.entrySet()) {
                itemMap.put(entry.getValue(), entry.getKey());
            }

            log("🚀 Running PaNDa+ algorithm...");
            int k = 100;
            double epsilon_r = 0.6;
            double epsilon_c = 0.6;

            List<Pattern> patterns = PandaTopKPattern.runPandaPlus(k, D, epsilon_r, epsilon_c);

            log("🧩 Building RDF Summary Graph...");
            SummaryGraphBuilder summaryBuilder = new SummaryGraphBuilder(patterns, itemMap);
            Model summaryModel = summaryBuilder.buildSummaryGraph();

            log("💾 Writing summary to: " + outputFile);
            summaryModel.write(new FileOutputStream(outputFile), "TURTLE");

            // Optional debug outputs
            PatternWritter.writePatternWithMapping("patterns_output.txt", patterns, itemMap);
            MatrixWriter.writePredicates(predicateMap, "PredicateMap.txt");
            MatrixWriter.writeSubjects(builder.getInstanceMap(), "Subjects.txt");

            log("✅ Summary completed successfully!");
            log("🔢 Subjects: " + builder.getInstanceMap().size());
            log("🔠 Predicates: " + predicateMap.size());

        } catch (Exception ex) {
            log("❌ Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Logs a message in the output area.
     */
    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    /**
     * Launches the GUI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGUI().setVisible(true));
    }
}
