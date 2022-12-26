import com.formdev.flatlaf.intellijthemes.*;
import java.awt.Color;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Ten
 */
public class ProjektOracleUI extends javax.swing.JFrame {
    
    // <editor-fold defaultstate="collapsed" desc="MOJE FUNKCJE I ZMIENNE">
    static ProjektOracleCon oknoPolaczenia;
    static boolean polaczony = false;
    static Connection con;
    static Statement stmt;
    static ResultSet rs;
    static ResultSetMetaData rsmd;
    String skrypt;
    String rodzajZapytania;
    String rodzajAktualizacji;
    String rozszerzenie;
    File plik;
    int zaktualizowanychWierszy;
    
    protected static void connect(String nazwaHosta, String port, String sid, String nazwaUzytkownika, String haslo) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            ProjektOracleUI.con = DriverManager.getConnection(
                                                            "jdbc:oracle:thin:@" + nazwaHosta + ":" + port + ":" + sid,
                                                            nazwaUzytkownika,
                                                            haslo
                                                        );
            stmt = con.createStatement();
            polaczony = true;
            showMessageDialog(null, "Pomyślnie połączono z bazą!", "Sukces!", INFORMATION_MESSAGE);
            oknoPolaczenia.dispatchEvent(new WindowEvent(oknoPolaczenia, WindowEvent.WINDOW_CLOSING));
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(ProjektOracleCon.class.getName()).log(Level.SEVERE, null, ex);
            showMessageDialog(null, "Nie udało połączyć się z bazą. Upewnij się, że wpisujesz poprawne dane.", "Połączenie nieudane", ERROR_MESSAGE);
        }
    }
    
    private void disconnect() {
        try {
            stmt.close();
            con.close();
            polaczony = false;
            showMessageDialog(null, "Pomyślnie rozłączono z bazą!", "Sukces!", INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            Logger.getLogger(ProjektOracleUI.class.getName()).log(Level.SEVERE, null, ex);
            showMessageDialog(null, "Rozłączenie nie powiodło się! Spróbuj ponownie.", "Rozłączenie nieudane", ERROR_MESSAGE);
        }
    }
    
    private void otworzPlikZZapytaniami() {
        JFileChooser wyborPliku = new JFileChooser();
        wyborPliku.setDialogTitle("Wybierz plik z zapytaniami");
        wyborPliku.setCurrentDirectory(new File(System.getProperty("user.home")));
        wyborPliku.setAcceptAllFileFilterUsed(false); // Wyłącz opcję "wszystkie pliki"
        wyborPliku.addChoosableFileFilter(new FileNameExtensionFilter("Pliki tekstowe", "txt"));
        wyborPliku.addChoosableFileFilter(new FileNameExtensionFilter("Pliki .SQL", "sql"));
        int wynik = wyborPliku.showOpenDialog(this);
        if(wynik == JFileChooser.APPROVE_OPTION) {
            zaladujZapytaniaZPliku(wyborPliku.getSelectedFile());
        }
    }
    
    private void zaladujZapytaniaZPliku(File plik) {
        try (FileReader fr = new FileReader(plik)) {
            int i;
            while ((i = fr.read()) != -1) {
                jTextAreaInput.setText(jTextAreaInput.getText() + (char)i);
            }
        } catch (IOException ex) {
            Logger.getLogger(ProjektOracleUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void zapiszZapytaniaJako() {
        JFileChooser zapisPliku = new JFileChooser();
        zapisPliku.setDialogTitle("Wybierz plik z zapytaniami");
        zapisPliku.setCurrentDirectory(new File(System.getProperty("user.home")));
        zapisPliku.setAcceptAllFileFilterUsed(false); // Wyłącz opcję "wszystkie pliki"
        zapisPliku.addChoosableFileFilter(new FileNameExtensionFilter("Pliki tekstowe (*.txt)", "txt"));
        zapisPliku.addChoosableFileFilter(new FileNameExtensionFilter("Pliki .SQL (*.sql)", "sql"));
        int wynik = zapisPliku.showSaveDialog(this);
        if(wynik == JFileChooser.APPROVE_OPTION) {
            plik = zapisPliku.getSelectedFile();
            rozszerzenie = "";
            if(zapisPliku.getFileFilter().getDescription().equals("Pliki .SQL (*.sql)")) {
                if(!(plik.getName().endsWith(".sql"))) {
                    rozszerzenie = ".sql";
                }
            }
            else {
                if (!(plik.getName().endsWith(".txt"))) {
                    rozszerzenie = ".txt";
                }
            }
            zapiszZapytania();
        }
    }
    
    private void zapiszZapytania() {
        try (FileWriter fw = new FileWriter(plik + rozszerzenie)) {
                fw.write(jTextAreaInput.getText());
            } catch (IOException ex) {
                Logger.getLogger(ProjektOracleUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void wykonajZapytanie() {
        jTextAreaOutput.setText(null);                              // Wyczyść rezultat poprzedniego zapytania
        jTextAreaOutput.setForeground(null);                        // Ustaw kolor czcionki na domyślny (bo możliwe, że poprzednio wypisano na czerwono błąd)
        skrypt = jTextAreaInput.getText();                          // Pobierz skrypt z pola tekstowego
        String[] zapytania = skrypt.split(";");                     // Rozbij na pojedyncze zapytania...
        for (String zapytanie : zapytania) {                        // ...i przetwórz każde indywidualnie
            zapytanie = zapytanie.trim();
            rodzajZapytania = zapytanie.split(" ")[0].toLowerCase();    // Odczytaj rodzaj zapytania na bazie pierwszego słowa w zapytaniu
            try {
                if(rodzajZapytania.equals("select")) {                  // Jeśli zapytanie typu SELECT...
                    rs = stmt.executeQuery(zapytanie);
                    rsmd = rs.getMetaData();
                    int colCount = rsmd.getColumnCount();
                    while(rs.next()) {
                        for(int i = 1; i <= colCount; i++) {
                            jTextAreaOutput.setText(jTextAreaOutput.getText() + rs.getString(i) + " ");
                        }
                        jTextAreaOutput.setText(jTextAreaOutput.getText() + "\n");
                    }
                }
                else {
                    zaktualizowanychWierszy = stmt.executeUpdate(zapytanie);
                    switch (rodzajZapytania) {
                        case "delete":
                            rodzajAktualizacji = "Usunięto";
                            break;
                        case "update":
                            rodzajAktualizacji = "Zaktualizowano";
                            break;
                        case "insert":
                            rodzajAktualizacji = "Wstawiono";
                            break;
                        default:
                            break;
                    }
                    jTextAreaOutput.setText(rodzajAktualizacji + " " + zaktualizowanychWierszy + " wierszy");
                }
                jTextAreaOutput.setText(jTextAreaOutput.getText() + "\n---\n\n");
            } catch (SQLException ex) {
                Logger.getLogger(ProjektOracleUI.class.getName()).log(Level.SEVERE, null, ex);
                jTextAreaOutput.setForeground(Color.red);
                jTextAreaOutput.setText(ex.getMessage());
            } catch (NullPointerException ex) {
                jTextAreaOutput.setForeground(Color.red);
                jTextAreaOutput.setText("Brak połączenia z bazą! Utwórz połączenie i spróbuj ponownie.");
            }
        }
    }
    // </editor-fold>
    
    /**
     * Creates new form ProjektOracleUI
     */
    public ProjektOracleUI() {  
        initComponents();
        setLocationRelativeTo(null);    // Otwarcie okna na środku ekranu 
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaOutput = new javax.swing.JTextArea();
        jComboBox1 = new javax.swing.JComboBox<>();
        jButton2 = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaInput = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        jButton3 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ProjektOracle");
        setIconImages(null);
        setResizable(false);

        jButton1.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jButton1.setText("Wykonaj skrypt");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jTextAreaOutput.setEditable(false);
        jTextAreaOutput.setColumns(20);
        jTextAreaOutput.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jTextAreaOutput.setRows(5);
        jTextAreaOutput.setToolTipText("test");
        jScrollPane1.setViewportView(jTextAreaOutput);

        jComboBox1.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Wyświetl wszystkich pracowników", "Daj 1zł podwyżki wszystkim programistom ", "Dodaj Polskę do tabeli COUNTRIES", "Utwórz tabelę \"tab\"" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jButton2.setText("Wyczyść");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton2MouseClicked(evt);
            }
        });
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextAreaInput.setColumns(20);
        jTextAreaInput.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jTextAreaInput.setLineWrap(true);
        jTextAreaInput.setRows(5);
        jScrollPane2.setViewportView(jTextAreaInput);

        jLabel1.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jLabel1.setText("Skrypt SQL");

        jLabel2.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jLabel2.setText("Wynik");

        jButton3.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jButton3.setText("Wyczyść");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Book Antiqua", 0, 13)); // NOI18N
        jLabel3.setText("Predefiniowane zapytania:");

        jMenuBar1.setFont(new java.awt.Font("Gentium Basic", 0, 14)); // NOI18N

        jMenu1.setText("Połączenie");
        jMenu1.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu1MenuSelected(evt);
            }
        });
        jMenu1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu1ActionPerformed(evt);
            }
        });

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ADD, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem1.setText("Połącz...");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        jMenuItem2.setText("Rozłącz");
        jMenuItem2.setEnabled(false);
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Zapytanie");

        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem5.setText("Otwórz...");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem5);
        jMenu2.add(jSeparator1);

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem4.setText("Zapisz");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem4);

        jMenuItem3.setText("Zapisz jako...");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator3)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel1)
                                    .addComponent(jScrollPane2)
                                    .addComponent(jLabel2)
                                    .addComponent(jSeparator2)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(jComboBox1, 0, 879, Short.MAX_VALUE)
                                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(jScrollPane1))
                        .addGap(24, 24, 24))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseClicked
        wykonajZapytanie();
    }//GEN-LAST:event_jButton1MouseClicked

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        switch(jComboBox1.getSelectedIndex()) {
            case 0: jTextAreaInput.setText("SELECT * FROM employees");
            break;
            case 1: jTextAreaInput.setText("UPDATE employees SET salary = salary + 1 WHERE job_id = 'IT_PROG'");
            break;
            case 2: jTextAreaInput.setText("INSERT INTO countries VALUES('PL', 'Poland', 1)");
            break;
            case 3: jTextAreaInput.setText("CREATE TABLE tab(id INTEGER NOT NULL, col1 INTEGER, PRIMARY KEY(id))");
            break;
        }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jButton2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseClicked
        jTextAreaInput.setText(null);
    }//GEN-LAST:event_jButton2MouseClicked

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        oknoPolaczenia = new ProjektOracleCon();
        oknoPolaczenia.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        disconnect();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jMenu1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu1ActionPerformed
        
    }//GEN-LAST:event_jMenu1ActionPerformed

    private void jMenu1MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu1MenuSelected
        jMenuItem2.setEnabled(polaczony);   // Nie jestem dumny z tego rozwiązania!
    }//GEN-LAST:event_jMenu1MenuSelected

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        otworzPlikZZapytaniami();
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        zapiszZapytaniaJako();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        if (plik == null || !plik.exists()) zapiszZapytaniaJako();
        else zapiszZapytania();
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        jTextAreaOutput.setText(null);
    }//GEN-LAST:event_jButton3ActionPerformed
       
    /**
     * @param args the command line arguments
     * @throws java.sql.SQLException
     */
    public static void main(String args[]) throws SQLException {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            UIManager.setLookAndFeel(new FlatDarkPurpleIJTheme());
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ProjektOracleUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new ProjektOracleUI().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextArea jTextAreaInput;
    private javax.swing.JTextArea jTextAreaOutput;
    // End of variables declaration//GEN-END:variables
}
