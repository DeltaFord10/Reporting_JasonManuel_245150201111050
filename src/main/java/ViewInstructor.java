import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.sql.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;

public class ViewInstructor extends JFrame {
    private String dbUrl;
    private String user;
    private String pass;

    private JTable instructorTable;
    private DefaultTableModel tableModel;
    private JButton generateReportButton;

    public ViewInstructor() {
        loadEnvVariables();

        if (dbUrl == null || user == null || pass == null) {
            JOptionPane.showMessageDialog(this,
                    "Kredensial database tidak ditemukan dari .env file. Aplikasi akan keluar.",
                    "Kesalahan Konfigurasi",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setTitle("Daftar Instruktur");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Daftar Instruktur", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 51, 102));
        add(titleLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        instructorTable = new JTable(tableModel);

        instructorTable.setFillsViewportHeight(true);
        instructorTable.setRowHeight(25);
        instructorTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        instructorTable.getTableHeader().setBackground(new Color(220, 230, 241));
        instructorTable.getTableHeader().setForeground(new Color(50, 50, 50));
        instructorTable.setSelectionBackground(new Color(173, 216, 230));
        instructorTable.setGridColor(new Color(200, 200, 200));
        instructorTable.setShowGrid(true);

        JScrollPane scrollPane = new JScrollPane(instructorTable);
        add(scrollPane, BorderLayout.CENTER);

        generateReportButton = new JButton("Generate Report");
        generateReportButton.setFont(new Font("Arial", Font.BOLD, 16));
        generateReportButton.setBackground(new Color(50, 150, 200));
        generateReportButton.setForeground(Color.WHITE);
        generateReportButton.setFocusPainted(false);
        generateReportButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        generateReportButton.addActionListener(e -> generateReport());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(generateReportButton);
        add(buttonPanel, BorderLayout.SOUTH);

        loadInstructorData();
    }

    private void loadEnvVariables() {
        try {
            Dotenv dotenv = Dotenv.load();
            this.dbUrl = dotenv.get("DB_URL");
            this.user = dotenv.get("DB_USERNAME");
            this.pass = dotenv.get("DB_PASSWORD");
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Gagal memuat .env file: " + e.getMessage());
            System.err.println("Pastikan file .env ada di direktori akar proyek.");
            this.dbUrl = null;
            this.user = null;
            this.pass = null;
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, user, pass);
    }

    private void loadInstructorData() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        String sql = "SELECT * FROM instructor";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(metaData.getColumnName(i));
            }

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                tableModel.addRow(row);
            }

            instructorTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            for (int i = 0; i < columnCount; i++) {
                instructorTable.getColumnModel().getColumn(i).setPreferredWidth(120);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Gagal mengambil data dari database!\n" + e.getMessage(),
                    "Kesalahan Database",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void generateReport() {
        try {
            JasperReport jasperReport = JasperReportsManager.createSimpleReport();
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ReportTitle", "Laporan Daftar Instruktur Universitas");
            JRDataSource dataSource = new JRTableModelDataSource(tableModel);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            String outputPath = "InstructorReport.pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, outputPath);
            JOptionPane.showMessageDialog(this,
                    "Laporan berhasil dibuat!\nFile: " + outputPath,
                    "Laporan Berhasil",
                    JOptionPane.INFORMATION_MESSAGE);
            JasperViewer.viewReport(jasperPrint, true);
        } catch (JRException ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal membuat laporan: " + ex.getMessage(),
                    "Kesalahan Laporan",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private static class JasperReportsManager {
        private static void applyCellFormatting(JRDesignElement element, boolean isHeader) {
            if (element instanceof JRDesignTextElement) {
                JRDesignTextElement textElement = (JRDesignTextElement) element;
                JRLineBox lineBox = textElement.getLineBox();
                lineBox.getPen().setLineWidth(0.5f);
                lineBox.getPen().setLineColor(Color.BLACK);
                lineBox.setLeftPadding(2);
                lineBox.setRightPadding(2);
                lineBox.setTopPadding(2);
                lineBox.setBottomPadding(2);
                textElement.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);

                if (isHeader) {
                    if (textElement instanceof JRDesignStaticText) {
                        JRDesignStaticText staticText = (JRDesignStaticText) textElement;
                        staticText.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
                        staticText.setBold(true);
                    }
                } else {
                    if (textElement instanceof JRDesignTextField) {
                        JRDesignTextField textField = (JRDesignTextField) textElement;
                        if (textField.getExpression() != null && "$F{salary}".equals(textField.getExpression().getText())) {
                            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
                        } else {
                            textField.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
                        }
                    }
                }
            }
        }

        public static JasperReport createSimpleReport() throws JRException {
            JasperDesign design = new JasperDesign();
            design.setName("InstructorSimpleReport");
            design.setLeftMargin(30);
            design.setRightMargin(30);
            design.setTopMargin(20);
            design.setBottomMargin(20);

            JRDesignParameter reportTitleParam = new JRDesignParameter();
            reportTitleParam.setName("ReportTitle");
            reportTitleParam.setValueClassName("java.lang.String");
            design.addParameter(reportTitleParam);

            JRDesignBand titleBand = new JRDesignBand();
            titleBand.setHeight(40);
            design.setTitle(titleBand);

            JRDesignTextField titleField = new JRDesignTextField();
            titleField.setExpression(new JRDesignExpression("$P{ReportTitle}"));
            titleField.setX(0);
            titleField.setY(0);
            titleField.setHeight(30);
            titleField.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
            titleField.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
            titleField.setFontSize(18f);
            titleField.setBold(true);
            titleBand.addElement(titleField);

            int idColWidth = 70;
            int nameColWidth = 180;
            int deptColWidth = 130;
            int salaryColWidth = 90;
            int totalTableWidth = idColWidth + nameColWidth + deptColWidth + salaryColWidth;

            titleField.setWidth(totalTableWidth);
            design.setPageWidth(totalTableWidth + design.getLeftMargin() + design.getRightMargin());
            design.setColumnWidth(totalTableWidth);

            JRDesignField idField = new JRDesignField();
            idField.setName("ID");
            idField.setValueClassName("java.lang.String");
            design.addField(idField);

            JRDesignField nameField = new JRDesignField();
            nameField.setName("name");
            nameField.setValueClassName("java.lang.String");
            design.addField(nameField);

            JRDesignField deptNameField = new JRDesignField();
            deptNameField.setName("dept_name");
            deptNameField.setValueClassName("java.lang.String");
            design.addField(deptNameField);

            JRDesignField salaryField = new JRDesignField();
            salaryField.setName("salary");
            salaryField.setValueClassName("java.math.BigDecimal");
            design.addField(salaryField);

            JRDesignBand columnHeaderBand = new JRDesignBand();
            int cellHeight = 22;
            columnHeaderBand.setHeight(cellHeight + 3);

            int headerCurrentX = 0;
            float headerFontSize = 10f;

            JRDesignStaticText headerIdText = new JRDesignStaticText();
            headerIdText.setText("ID");
            headerIdText.setX(headerCurrentX);
            headerIdText.setY(0);
            headerIdText.setWidth(idColWidth);
            headerIdText.setHeight(cellHeight);
            headerIdText.setFontSize(headerFontSize);
            applyCellFormatting(headerIdText, true);
            columnHeaderBand.addElement(headerIdText);
            headerCurrentX += idColWidth;

            JRDesignStaticText headerNameText = new JRDesignStaticText();
            headerNameText.setText("Nama Instruktur");
            headerNameText.setX(headerCurrentX);
            headerNameText.setY(0);
            headerNameText.setWidth(nameColWidth);
            headerNameText.setHeight(cellHeight);
            headerNameText.setFontSize(headerFontSize);
            applyCellFormatting(headerNameText, true);
            columnHeaderBand.addElement(headerNameText);
            headerCurrentX += nameColWidth;

            JRDesignStaticText headerDeptNameText = new JRDesignStaticText();
            headerDeptNameText.setText("Departemen");
            headerDeptNameText.setX(headerCurrentX);
            headerDeptNameText.setY(0);
            headerDeptNameText.setWidth(deptColWidth);
            headerDeptNameText.setHeight(cellHeight);
            headerDeptNameText.setFontSize(headerFontSize);
            applyCellFormatting(headerDeptNameText, true);
            columnHeaderBand.addElement(headerDeptNameText);
            headerCurrentX += deptColWidth;

            JRDesignStaticText headerSalaryText = new JRDesignStaticText();
            headerSalaryText.setText("Gaji");
            headerSalaryText.setX(headerCurrentX);
            headerSalaryText.setY(0);
            headerSalaryText.setWidth(salaryColWidth);
            headerSalaryText.setHeight(cellHeight);
            headerSalaryText.setFontSize(headerFontSize);
            applyCellFormatting(headerSalaryText, true);
            columnHeaderBand.addElement(headerSalaryText);

            design.setColumnHeader(columnHeaderBand);

            JRDesignBand detailBand = new JRDesignBand();
            detailBand.setHeight(cellHeight + 3);

            JRSection section = design.getDetailSection();
            JRDesignSection detailDesignSection;

            if (section instanceof JRDesignSection) {
                detailDesignSection = (JRDesignSection) section;
            } else if (section == null) {
                throw new JRException("Fatal: Detail section is null.");
            } else {
                throw new JRException("Detail section is not an instance of JRDesignSection. Actual type: " + section.getClass().getName());
            }
            detailDesignSection.addBand(detailBand);

            int detailCurrentX = 0;
            float detailFontSize = 10f;

            JRDesignTextField idText = new JRDesignTextField();
            idText.setExpression(new JRDesignExpression("$F{ID}"));
            idText.setX(detailCurrentX);
            idText.setY(0);
            idText.setWidth(idColWidth);
            idText.setHeight(cellHeight);
            idText.setFontSize(detailFontSize);
            applyCellFormatting(idText, false);
            detailBand.addElement(idText);
            detailCurrentX += idColWidth;

            JRDesignTextField nameText = new JRDesignTextField();
            nameText.setExpression(new JRDesignExpression("$F{name}"));
            nameText.setX(detailCurrentX);
            nameText.setY(0);
            nameText.setWidth(nameColWidth);
            nameText.setHeight(cellHeight);
            nameText.setFontSize(detailFontSize);
            applyCellFormatting(nameText, false);
            detailBand.addElement(nameText);
            detailCurrentX += nameColWidth;

            JRDesignTextField deptNameText = new JRDesignTextField();
            deptNameText.setExpression(new JRDesignExpression("$F{dept_name}"));
            deptNameText.setX(detailCurrentX);
            deptNameText.setY(0);
            deptNameText.setWidth(deptColWidth);
            deptNameText.setHeight(cellHeight);
            deptNameText.setFontSize(detailFontSize);
            applyCellFormatting(deptNameText, false);
            detailBand.addElement(deptNameText);
            detailCurrentX += deptColWidth;

            JRDesignTextField salaryText = new JRDesignTextField();
            salaryText.setExpression(new JRDesignExpression("$F{salary}"));
            salaryText.setX(detailCurrentX);
            salaryText.setY(0);
            salaryText.setWidth(salaryColWidth);
            salaryText.setHeight(cellHeight);
            salaryText.setFontSize(detailFontSize);
            applyCellFormatting(salaryText, false);
            detailBand.addElement(salaryText);

            return JasperCompileManager.compileReport(design);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ViewInstructor().setVisible(true);
        });
    }
}