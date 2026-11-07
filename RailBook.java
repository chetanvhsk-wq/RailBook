import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * RailBook - Railway Reservation Form (Demo) with a clickable seat map.
 * Java Swing mini application.
 *
 * WHAT IT DOES
 *   Fill in passenger + journey details, press "Show seats", click seats on the coach
 *   map (green = free, grey = booked, blue = yours), see the fare update live, and book.
 *   Bookings appear in a table; double-click a row to see the ticket, or cancel it
 *   (80% refund, seats become free again).
 *
 * OOP  : abstract class Ticket + 3 subclasses (inheritance, polymorphism, encapsulation)
 * EVENTS: ActionListener, ItemListener, KeyAdapter / MouseAdapter / WindowAdapter
 * LAYOUT: BorderLayout, GridLayout, FlowLayout
 * ERRORS: custom checked exception BookingException + IOException handling
 */
public class RailBook extends JFrame {

    // =====================================================================
    // 1. CUSTOM EXCEPTION - thrown whenever user input is not acceptable
    // =====================================================================
    static class BookingException extends Exception {
        BookingException(String message) { super(message); }
    }

    // =====================================================================
    // 2. FIXED DATA (demo route, travel classes, limits)
    // =====================================================================
    static final String[] STATIONS = {"Mysuru", "Mandya", "Bengaluru", "Jolarpettai", "Katpadi", "Chennai"};
    static final int[] KM = {0, 45, 140, 340, 395, 490};              // km marker of each station
    static final String[] CLASSES = {"Sleeper (SL)", "AC 3 Tier (3A)", "AC 2 Tier (2A)"};
    static final int ROWS = 5, COLS = 8, TOTAL_SEATS = ROWS * COLS, MAX_SEATS = 6;
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    static int distance(String from, String to) {
        return Math.abs(KM[indexOf(from)] - KM[indexOf(to)]);
    }
    static int indexOf(String station) {
        for (int i = 0; i < STATIONS.length; i++) if (STATIONS[i].equals(station)) return i;
        return -1;
    }

    // =====================================================================
    // 3. TICKET CLASSES  (inheritance + polymorphism)
    //    Ticket holds everything common. Each subclass only supplies its own
    //    rate per km and reservation charge, so fare() works for all classes.
    // =====================================================================
    static abstract class Ticket {
        private final int classIndex;
        private final String pnr, name, gender, from, to;
        private final int age;
        private final LocalDate date;
        private final boolean student, meal;
        private final List<Integer> seats;

        Ticket(int classIndex, String pnr, String name, int age, String gender, String from, String to,
               LocalDate date, boolean student, boolean meal, List<Integer> seats) {
            this.classIndex = classIndex; this.pnr = pnr; this.name = name; this.age = age;
            this.gender = gender; this.from = from; this.to = to; this.date = date;
            this.student = student; this.meal = meal; this.seats = seats;
        }

        // ---- each travel class must provide these two numbers ----
        abstract double ratePerKm();
        abstract double reservationCharge();

        // ---- common fare logic (same for every class) ----
        double perSeatFare()   { return distance(from, to) * ratePerKm() + reservationCharge(); }
        double concessionRate() {                       // best single concession applies
            if (age >= 60) return 0.40;                 // senior citizen
            if (student) return 0.25;
            return 0;
        }
        double baseTotal()     { return perSeatFare() * seats.size(); }
        double concession()    { return perSeatFare() * concessionRate(); }   // on ONE seat (the lead passenger)
        double mealCharge()    { return meal ? 150.0 * seats.size() : 0; }
        double gst()           { return 0.05 * (baseTotal() - concession() + mealCharge()); }
        double total()         { return baseTotal() - concession() + mealCharge() + gst(); }

        String receipt() {
            StringBuilder sb = new StringBuilder();
            sb.append("========== RAILBOOK E-TICKET ==========\n");
            sb.append(String.format("PNR        : %s\n", pnr));
            sb.append(String.format("Passenger  : %s (%d, %s)\n", name, age, gender));
            sb.append(String.format("Journey    : %s -> %s (%d km)\n", from, to, distance(from, to)));
            sb.append(String.format("Date       : %s\n", date.format(FMT)));
            sb.append(String.format("Class      : %s\n", CLASSES[classIndex]));
            sb.append(String.format("Seats      : %s\n", seatText()));
            sb.append("---------------------------------------\n");
            sb.append(String.format("Fare/seat  : Rs. %8.2f\n", perSeatFare()));
            sb.append(String.format("Base fare  : Rs. %8.2f  (%d x seat)\n", baseTotal(), seats.size()));
            sb.append(String.format("Concession : Rs. %8.2f  (%.0f%%)\n", concession() == 0 ? 0.0 : -concession(), concessionRate() * 100));
            sb.append(String.format("Meals      : Rs. %8.2f\n", mealCharge()));
            sb.append(String.format("GST 5%%     : Rs. %8.2f\n", gst()));
            sb.append("---------------------------------------\n");
            sb.append(String.format("TOTAL      : Rs. %8.2f\n", total()));
            sb.append("=======================================\n");
            return sb.toString();
        }

        String seatText() {
            StringBuilder sb = new StringBuilder();
            for (int s : seats) { if (sb.length() > 0) sb.append(", "); sb.append(s); }
            return sb.toString();
        }

        // ---- getters (fields stay private = encapsulation) ----
        int getClassIndex() { return classIndex; }
        String getPnr() { return pnr; }
        String getName() { return name; }
        String getFrom() { return from; }
        String getTo() { return to; }
        LocalDate getDate() { return date; }
        List<Integer> getSeats() { return seats; }
    }

    static class SleeperTicket extends Ticket {
        SleeperTicket(int c, String p, String n, int a, String g, String f, String t, LocalDate d,
                      boolean s, boolean m, List<Integer> seats) { super(c, p, n, a, g, f, t, d, s, m, seats); }
        double ratePerKm() { return 0.55; }
        double reservationCharge() { return 20; }
    }

    static class ThreeTierTicket extends Ticket {
        ThreeTierTicket(int c, String p, String n, int a, String g, String f, String t, LocalDate d,
                        boolean s, boolean m, List<Integer> seats) { super(c, p, n, a, g, f, t, d, s, m, seats); }
        double ratePerKm() { return 1.45; }
        double reservationCharge() { return 40; }
    }

    static class TwoTierTicket extends Ticket {
        TwoTierTicket(int c, String p, String n, int a, String g, String f, String t, LocalDate d,
                      boolean s, boolean m, List<Integer> seats) { super(c, p, n, a, g, f, t, d, s, m, seats); }
        double ratePerKm() { return 2.10; }
        double reservationCharge() { return 50; }
    }

    /** Chooses the right subclass - the rest of the program only sees "Ticket". */
    static Ticket createTicket(int cls, String pnr, String name, int age, String gender, String from, String to,
                               LocalDate date, boolean student, boolean meal, List<Integer> seats) {
        switch (cls) {
            case 0:  return new SleeperTicket(cls, pnr, name, age, gender, from, to, date, student, meal, seats);
            case 1:  return new ThreeTierTicket(cls, pnr, name, age, gender, from, to, date, student, meal, seats);
            default: return new TwoTierTicket(cls, pnr, name, age, gender, from, to, date, student, meal, seats);
        }
    }

    // =====================================================================
    // 4. GUI COMPONENTS
    // =====================================================================
    private final JTextField nameField = new JTextField(14);
    private final JTextField ageField = new JTextField(4);
    private final JTextField dateField = new JTextField(10);
    private final JRadioButton rbMale = new JRadioButton("Male", true);
    private final JRadioButton rbFemale = new JRadioButton("Female");
    private final JRadioButton rbOther = new JRadioButton("Other");
    private final JComboBox<String> fromBox = new JComboBox<String>(STATIONS);
    private final JComboBox<String> toBox = new JComboBox<String>(STATIONS);
    private final JComboBox<String> classBox = new JComboBox<String>(CLASSES);
    private final JCheckBox studentCb = new JCheckBox("Student (25% off)");
    private final JCheckBox mealCb = new JCheckBox("Meals (Rs.150 per seat)");
    private final JButton showSeatsBtn = new JButton("Show seats");
    private final JButton clearBtn = new JButton("Clear form");
    private final JButton bookBtn = new JButton("Book tickets");
    private final JButton cancelBtn = new JButton("Cancel selected booking");
    private final JButton[] seatBtns = new JButton[TOTAL_SEATS];
    private final JLabel fareLabel = new JLabel("Fare: select seats");
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(
        new Object[]{"PNR", "Passenger", "Route", "Date", "Class", "Seats", "Fare (Rs.)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    // =====================================================================
    // 5. APPLICATION DATA
    // =====================================================================
    private final List<Ticket> bookings = new ArrayList<Ticket>();
    private final Map<String, Set<Integer>> bookedSeats = new HashMap<String, Set<Integer>>();
    private final Set<Integer> selected = new TreeSet<Integer>();     // seats the user has clicked
    private final Random rnd = new Random();
    private LocalDate shownDate;      // date/class that the seat map currently shows
    private int shownClass = -1;

    public RailBook() {
        super("RailBook - Railway Reservation (Demo)");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        fromBox.setSelectedIndex(2);                      // Bengaluru
        toBox.setSelectedIndex(5);                        // Chennai
        dateField.setText(LocalDate.now().plusDays(1).format(FMT));

        add(buildTitle(), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.WEST);
        add(buildSeatPanel(), BorderLayout.CENTER);
        add(buildTablePanel(), BorderLayout.SOUTH);
        setJMenuBar(buildMenu());
        wireEvents();

        loadSeats();
        pack();
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------------
    // building the screen
    // ---------------------------------------------------------------------
    private JComponent buildTitle() {
        JLabel title = new JLabel("RailBook  |  Mysuru - Chennai Demo Route", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(new Color(0x0D47A1));
        return title;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 10));       // GridLayout: label | field
        form.setBorder(new TitledBorder("Passenger & journey details"));
        form.add(new JLabel("Passenger name"));         form.add(nameField);
        form.add(new JLabel("Age"));                    form.add(ageField);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbMale); bg.add(rbFemale); bg.add(rbOther);
        JPanel genderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));  // FlowLayout
        genderRow.add(rbMale); genderRow.add(rbFemale); genderRow.add(rbOther);
        form.add(new JLabel("Gender"));                 form.add(genderRow);
        form.add(new JLabel("From"));                   form.add(fromBox);
        form.add(new JLabel("To"));                     form.add(toBox);
        form.add(new JLabel("Date (dd-MM-yyyy)"));      form.add(dateField);
        form.add(new JLabel("Travel class"));           form.add(classBox);
        form.add(new JLabel("Concession"));             form.add(studentCb);
        form.add(new JLabel("Food"));                   form.add(mealCb);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        buttons.add(showSeatsBtn);
        buttons.add(clearBtn);

        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.add(form, BorderLayout.NORTH);
        box.add(buttons, BorderLayout.CENTER);
        box.add(buildInfoLabel(), BorderLayout.SOUTH);
        return box;
    }

    private JComponent buildInfoLabel() {
        JLabel info = new JLabel("<html><small>Senior citizens (60+) get 40% off one seat.<br>"
            + "Max 6 seats per booking. GST 5% extra.</small></html>");
        info.setBorder(new EmptyBorder(4, 6, 4, 6));
        return info;
    }

    private JComponent buildSeatPanel() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        legend.add(legendLabel("Available", new Color(0xA5D6A7), Color.BLACK));
        legend.add(legendLabel("Booked", new Color(0xBDBDBD), Color.DARK_GRAY));
        legend.add(legendLabel("Your selection", new Color(0x42A5F5), Color.WHITE));

        JPanel grid = new JPanel(new GridLayout(ROWS, COLS, 5, 5));
        for (int i = 0; i < TOTAL_SEATS; i++) {
            final int index = i;
            JButton b = new JButton(String.valueOf(i + 1));
            b.setMargin(new Insets(6, 2, 6, 2));
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.addActionListener(new ActionListener() {                       // ActionListener on each seat
                public void actionPerformed(ActionEvent e) { seatClicked(index); }
            });
            seatBtns[i] = b;
            grid.add(b);
        }

        fareLabel.setFont(fareLabel.getFont().deriveFont(Font.BOLD, 15f));
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(fareLabel, BorderLayout.CENTER);
        bottom.add(bookBtn, BorderLayout.EAST);
        bookBtn.setFont(bookBtn.getFont().deriveFont(Font.BOLD, 14f));

        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBorder(new TitledBorder("Coach seat map  (click to select, max 6)"));
        p.add(legend, BorderLayout.NORTH);
        p.add(grid, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JLabel legendLabel(String text, Color bg, Color fg) {
        JLabel l = new JLabel(" " + text + " ");
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        return l;
    }

    private JComponent buildTablePanel() {
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(800, 130));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        south.add(statusLabel);
        south.add(cancelBtn);
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBorder(new TitledBorder("My bookings  (double-click a row to view the ticket)"));
        p.add(sp, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        JMenuItem save = new JMenuItem("Save selected ticket...");
        save.addActionListener(e -> saveSelectedTicket());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> confirmExit());
        file.add(save); file.addSeparator(); file.add(exit);

        JMenu booking = new JMenu("Booking");
        booking.setMnemonic('B');
        JMenuItem book = new JMenuItem("Book tickets");
        book.addActionListener(e -> book());
        JMenuItem cancel = new JMenuItem("Cancel selected booking");
        cancel.addActionListener(e -> cancelBooking());
        JMenuItem clear = new JMenuItem("Clear form");
        clear.addActionListener(e -> clearForm());
        booking.add(book); booking.add(cancel); booking.add(clear);

        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        JMenuItem rules = new JMenuItem("Fare rules");
        rules.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "Fare per seat = distance (km) x rate per km + reservation charge\n\n"
          + "Sleeper    : Rs. 0.55 / km + Rs. 20\n"
          + "AC 3 Tier  : Rs. 1.45 / km + Rs. 40\n"
          + "AC 2 Tier  : Rs. 2.10 / km + Rs. 50\n\n"
          + "Concession : age 60+ = 40% off, student = 25% off (best one, on one seat)\n"
          + "Meals      : Rs. 150 per seat\n"
          + "GST        : 5% on the total\n"
          + "Cancelling : 80% of the fare is refunded",
            "Fare rules", JOptionPane.INFORMATION_MESSAGE));
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
            "RailBook - Railway Reservation (Demo)\nJava Swing mini application",
            "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(rules); help.add(about);

        bar.add(file); bar.add(booking); bar.add(help);
        return bar;
    }

    // =====================================================================
    // 6. EVENT HANDLING
    // =====================================================================
    private void wireEvents() {
        // ActionListener: buttons and Enter key in the date field
        showSeatsBtn.addActionListener(e -> loadSeats());
        dateField.addActionListener(e -> loadSeats());
        clearBtn.addActionListener(e -> clearForm());
        bookBtn.addActionListener(e -> book());
        cancelBtn.addActionListener(e -> cancelBooking());

        // ItemListener: fires when a combo / checkbox selection changes
        ItemListener fareUpdater = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED || e.getSource() instanceof JCheckBox) updateFare();
            }
        };
        fromBox.addItemListener(fareUpdater);
        toBox.addItemListener(fareUpdater);
        studentCb.addItemListener(fareUpdater);
        mealCb.addItemListener(fareUpdater);
        classBox.addItemListener(new ItemListener() {                    // new class = different seat map
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) loadSeats();
            }
        });

        // Adapter classes: we only override the one method we need
        ageField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateFare(); }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) showReceipt(bookings.get(table.getSelectedRow()));
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(this, "Exit RailBook?", "Exit", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) System.exit(0);
    }

    // =====================================================================
    // 7. INPUT VALIDATION (each method throws BookingException on bad input)
    // =====================================================================
    private String validName() throws BookingException {
        String name = nameField.getText().trim();
        if (!name.matches("[A-Za-z][A-Za-z .]{1,29}"))
            throw new BookingException("Name must be 2-30 letters (spaces and dots allowed).");
        return name;
    }

    private int parseAge() throws BookingException {
        try {
            int age = Integer.parseInt(ageField.getText().trim());
            if (age < 1 || age > 120) throw new BookingException("Age must be between 1 and 120.");
            return age;
        } catch (NumberFormatException ex) {
            throw new BookingException("Age must be a whole number.");
        }
    }

    private LocalDate parseDate() throws BookingException {
        try {
            LocalDate d = LocalDate.parse(dateField.getText().trim(), FMT);
            if (d.isBefore(LocalDate.now())) throw new BookingException("Journey date cannot be in the past.");
            if (d.isAfter(LocalDate.now().plusDays(120))) throw new BookingException("Booking opens only 120 days ahead.");
            return d;
        } catch (DateTimeParseException ex) {
            throw new BookingException("Enter the date as dd-MM-yyyy, for example 25-12-2026.");
        }
    }

    private String gender() { return rbMale.isSelected() ? "Male" : rbFemale.isSelected() ? "Female" : "Other"; }

    /** Builds a Ticket from the form (name is checked separately in book()). */
    private Ticket buildTicket(String pnr, String name, List<Integer> seats) throws BookingException {
        String from = (String) fromBox.getSelectedItem(), to = (String) toBox.getSelectedItem();
        if (from.equals(to)) throw new BookingException("From and To stations must be different.");
        int age = parseAge();
        return createTicket(classBox.getSelectedIndex(), pnr, name, age, gender(), from, to,
            shownDate != null ? shownDate : LocalDate.now(), studentCb.isSelected(), mealCb.isSelected(), seats);
    }

    // =====================================================================
    // 8. SEAT MAP
    // =====================================================================
    private Set<Integer> bookedFor(LocalDate date, int cls) {
        String key = date + "|" + cls;
        Set<Integer> set = bookedSeats.get(key);
        if (set == null) {                         // first time: pretend ~25% seats are already sold
            set = new TreeSet<Integer>();
            Random r = new Random(key.hashCode());
            for (int s = 1; s <= TOTAL_SEATS; s++) if (r.nextDouble() < 0.25) set.add(s);
            bookedSeats.put(key, set);
        }
        return set;
    }

    private void loadSeats() {
        try {
            shownDate = parseDate();
        } catch (BookingException ex) {
            error(ex.getMessage());
            return;
        }
        shownClass = classBox.getSelectedIndex();
        selected.clear();
        Set<Integer> booked = bookedFor(shownDate, shownClass);
        for (int i = 0; i < TOTAL_SEATS; i++) {
            seatBtns[i].setEnabled(!booked.contains(i + 1));
            paintSeat(i);
        }
        int free = TOTAL_SEATS - booked.size();
        setStatus(free + " seats free on " + shownDate.format(FMT) + " in " + CLASSES[shownClass], false);
        updateFare();
    }

    private void paintSeat(int i) {
        JButton b = seatBtns[i];
        if (!b.isEnabled()) { b.setBackground(new Color(0xBDBDBD)); b.setForeground(Color.DARK_GRAY); }
        else if (selected.contains(i + 1)) { b.setBackground(new Color(0x42A5F5)); b.setForeground(Color.WHITE); }
        else { b.setBackground(new Color(0xA5D6A7)); b.setForeground(Color.BLACK); }
    }

    private void seatClicked(int i) {
        int seat = i + 1;
        if (selected.contains(seat)) {
            selected.remove(seat);
        } else {
            if (selected.size() >= MAX_SEATS) { setStatus("Maximum " + MAX_SEATS + " seats per booking.", true); return; }
            selected.add(seat);
        }
        paintSeat(i);
        updateFare();
    }

    private void updateFare() {
        if (selected.isEmpty()) { fareLabel.setText("Fare: select seats"); return; }
        try {
            Ticket t = buildTicket("-", "-", new ArrayList<Integer>(selected));
            fareLabel.setText(String.format("Fare for %d seat(s): Rs. %.2f", selected.size(), t.total()));
        } catch (BookingException ex) {
            fareLabel.setText("Fare: " + ex.getMessage());
        }
    }

    // =====================================================================
    // 9. BOOK / CANCEL
    // =====================================================================
    private void book() {
        try {
            String name = validName();
            LocalDate d = parseDate();
            if (shownDate == null || !d.equals(shownDate) || classBox.getSelectedIndex() != shownClass)
                throw new BookingException("The seat map shows another date/class. Press 'Show seats' first.");
            if (selected.isEmpty()) throw new BookingException("Select at least one seat on the map.");

            Set<Integer> booked = bookedFor(shownDate, shownClass);
            for (int s : selected)
                if (booked.contains(s)) throw new BookingException("Seat " + s + " is no longer available.");

            long n = 1000000000L + (long) (rnd.nextDouble() * 9000000000L);
            Ticket t = buildTicket(String.valueOf(n), name, new ArrayList<Integer>(selected));
            booked.addAll(selected);
            bookings.add(t);
            model.addRow(new Object[]{t.getPnr(), t.getName(), t.getFrom() + " -> " + t.getTo(),
                t.getDate().format(FMT), CLASSES[t.getClassIndex()], t.seatText(), String.format("%.2f", t.total())});

            nameField.setText("");
            ageField.setText("");
            loadSeats();                                     // refresh map: chosen seats are now grey
            setStatus("Booked! PNR " + t.getPnr(), false);
            showReceipt(t);
        } catch (BookingException ex) {
            error(ex.getMessage());
        }
    }

    private void cancelBooking() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a booking in the table first."); return; }
        Ticket t = bookings.get(row);
        double refund = 0.8 * t.total();
        int r = JOptionPane.showConfirmDialog(this,
            String.format("Cancel PNR %s?\nRefund (80%%): Rs. %.2f", t.getPnr(), refund),
            "Cancel booking", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        bookedFor(t.getDate(), t.getClassIndex()).removeAll(t.getSeats());   // free the seats again
        bookings.remove(row);
        model.removeRow(row);
        if (t.getDate().equals(shownDate) && t.getClassIndex() == shownClass) loadSeats();
        setStatus("Booking cancelled. Refund Rs. " + String.format("%.2f", refund), false);
    }

    private void clearForm() {
        nameField.setText("");
        ageField.setText("");
        rbMale.setSelected(true);
        studentCb.setSelected(false);
        mealCb.setSelected(false);
        selected.clear();
        for (int i = 0; i < TOTAL_SEATS; i++) paintSeat(i);
        updateFare();
        setStatus("Form cleared.", false);
    }

    // =====================================================================
    // 10. TICKET OUTPUT + FILE SAVING
    // =====================================================================
    private void showReceipt(Ticket t) {
        JTextArea area = new JTextArea(t.receipt());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        int choice = JOptionPane.showOptionDialog(this, area, "E-Ticket " + t.getPnr(),
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
            new Object[]{"Save ticket to file", "Close"}, "Close");
        if (choice == 0) saveTicket(t);
    }

    private void saveSelectedTicket() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a booking in the table first."); return; }
        saveTicket(bookings.get(row));
    }

    private void saveTicket(Ticket t) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Ticket_" + t.getPnr() + ".txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
            w.write(t.receipt());
            setStatus("Ticket saved to " + fc.getSelectedFile().getName(), false);
        } catch (IOException ex) {
            error("Could not save the ticket: " + ex.getMessage());
        }
    }

    // =====================================================================
    // 11. SMALL HELPERS + MAIN
    // =====================================================================
    private void setStatus(String msg, boolean isError) {
        statusLabel.setForeground(isError ? new Color(0xC62828) : new Color(0x2E7D32));
        statusLabel.setText(msg);
    }

    private void error(String msg) {
        setStatus(msg, true);
        JOptionPane.showMessageDialog(this, msg, "Please check", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
                catch (Exception ignored) { }
                new RailBook().setVisible(true);
            }
        });
    }
}
