/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.notesmanagement.views;

import com.mycompany.notesmanagement.controllers.ImageHandler;
import com.mycompany.notesmanagement.controllers.UserSession;
import com.mycompany.notesmanagement.dialogs.DatabaseError;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.notesmanagement.controllers.DatabaseConnector;
import com.mycompany.notesmanagement.models.CurrentUser;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdesktop.swingx.prompt.PromptSupport;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Dashboard extends javax.swing.JFrame {

    private final ImageHandler imageHandler = new ImageHandler();
    private final UserSession session = UserSession.getInstance();
    private final int id = session.getUserId();
    private final String fileName = "profilePicture.jpg";
    private final CurrentUser currentUser = new CurrentUser(id);
    private final CardLayout cards;
    private final DatabaseConnector database = new DatabaseConnector();

    private int lastInsertedId;
    private int lastInsertedTrashId;
    private final Set<Integer> noteIds;
    private final Set<Integer> trashIds;
    private DefaultListModel<Note> listModel = new DefaultListModel<>();
    private DefaultListModel<Trash> listModelTrash = new DefaultListModel<>();
    private int currentNoteId = 0;
    private int currentTrashId = 0;
    private boolean settingTitleAndContent = false;

    public Dashboard() {
        this.noteIds = new HashSet<>();
        this.trashIds = new HashSet<>();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        initComponents();

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        try {
            imageHandler.retrieveImage(id);
        } catch (IOException | SQLException e) {
            new DatabaseError().setVisible(true);
        }

        processRetrievedImage();
        usernameLabel.setText(currentUser.getUserName());
        userEmailLabel.setText(currentUser.getEmail());
        userHomeLabel.setText(currentUser.getUserName() + "\'s Home");

        String activeUser = currentUser.getUserName();
        char initial = activeUser.toUpperCase().charAt(0);
        userNameInitial.setText(String.valueOf(initial));

        notesButton.setName("");
        trashButton.setName("");

        cards = (CardLayout) (rightPanel.getLayout());

        setPlaceholders();

        scratchOption1.setEnabled(false);
        scratchOption2.setEnabled(false);
        attachDocumentListenersToScratchPad();

        setTextCounter();
        setTrashTextCounter();

        if (isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(false);
            notesCreationPanelBottom.setVisible(false);
        }

        if (isTrashListEmpty()) {
            trashedNoteTitleTextArea.setVisible(false);
            trashedNoteTextArea.setVisible(false);
        }

        getAllNoteIdsForCurrentUser();
        getAllTrashIdsForCurrentUser();
        addNotesToList();
        addTrashToList();

        Font customFont = new Font("Inter SemiBold", 0, 14);

        notesList.setModel(listModel);
        notesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notesList.setCellRenderer(new CustomListCellRenderer(customFont));
        notesList.setSelectedIndex(0);
        selectIndex0();

        trashNotesList.setModel(listModelTrash);
        trashNotesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trashNotesList.setCellRenderer(new CustomListCellRenderer(customFont));
        trashNotesList.setSelectedIndex(0);
        selectTrashIndex0();

        attachDocumentListenersToTextAreas();
        addListClickListener();
        addTrashListClickListener();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                notesCreationMainPanel.getVerticalScrollBar().setValue(0);
                notesCreationMainPanel.revalidate();
                notesCreationMainPanel.repaint();
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                trashedNoteMainPanel.getVerticalScrollBar().setValue(0);
                trashedNoteMainPanel.revalidate();
                trashedNoteMainPanel.repaint();
            }
        });

        Border bottomBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(115, 115, 115));

        notesCreationPanelTop.setBorder(bottomBorder);
        trashNotesCreationPanelTop.setBorder(bottomBorder);
    }

    private String getLastEditedFromNote(int noteId) {
        DatabaseConnector db = new DatabaseConnector();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            db.connect();
            connection = db.getConnection();

            String query = "SELECT note_updated_at FROM notes WHERE note_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, noteId);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                return resultSet.getString("note_updated_at");
            }

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                db.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
            }
        }
        return "";
    }

    private String getLastEditedFromTrash(int noteId) {
        DatabaseConnector db = new DatabaseConnector();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            db.connect();
            connection = db.getConnection();

            String query = "SELECT trash_updated_at FROM trash WHERE trash_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, noteId);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                return resultSet.getString("trash_updated_at");
            }

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                db.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
            }
        }
        return "";
    }

    private void addListClickListener() {
        notesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 1) {
                    JList<Note> list = (JList<Note>) evt.getSource();
                    int index = list.locationToIndex(evt.getPoint());
                    Note clickedNote = list.getModel().getElementAt(index);
                    int noteId = clickedNote.getId();
                    currentNoteId = noteId;
                    setTitleAndContent(noteId);

                    updateLastEditedText(lastEditedText, getLastEditedFromNote(noteId));

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            notesCreationMainPanel.getVerticalScrollBar().setValue(0);
                            notesCreationMainPanel.revalidate();
                            notesCreationMainPanel.repaint();
                        }
                    });

                }
            }

        });

        notesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {

                if (!settingTitleAndContent) {
                    if (!evt.getValueIsAdjusting()) {
                        Note selectedNote = notesList.getSelectedValue();
                        if (selectedNote != null) {
                            int noteId = selectedNote.getId();
                            currentNoteId = noteId;
                            setTitleAndContent(noteId);
                            setTextCounter();

                            updateLastEditedText(lastEditedText, getLastEditedFromNote(noteId));

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    notesCreationMainPanel.getVerticalScrollBar().setValue(0);
                                    notesCreationMainPanel.revalidate();
                                    notesCreationMainPanel.repaint();
                                }
                            });
                        }
                    }
                }
            }
        });

        if (notesList.getModel().getSize() > 0) {
            notesList.setSelectedIndex(0);
        }
    }

    private void addTrashListClickListener() {
        trashNotesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {

                if (listModelTrash.isEmpty()) {
                    return;
                }

                if (!isTrashListEmpty()) {
                    trashedNoteTitleTextArea.setVisible(true);
                    trashedNoteTextArea.setVisible(true);
                }

                if (evt.getClickCount() == 1) {
                    JList<Trash> list = (JList<Trash>) evt.getSource();
                    int index = list.locationToIndex(evt.getPoint());
                    Trash clickedNote = list.getModel().getElementAt(index);
                    int noteId = clickedNote.getId();
                    currentTrashId = noteId;
                    setTitleAndContentForTrash(noteId);

                    updateLastEditedText(trashLastEditedText, getLastEditedFromTrash(noteId));

                    // Update expiration notice
                    long remainingDays = calculateRemainingDays(clickedNote.getDeletedAt());
                    SwingUtilities.invokeLater(() -> {
                        if (remainingDays <= 0) {
                            expirationNotice.setText("Expired");
                        } else if (remainingDays <= 30) {
                            expirationNotice.setText("Expires in " + remainingDays + (remainingDays == 1 ? " day" : " days"));
                        } else {
                            expirationNotice.setVisible(false);
                            restoreCurrentNoteButtonPanel.setVisible(false);
                        }
                    });

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            trashedNoteMainPanel.getVerticalScrollBar().setValue(0);
                            trashedNoteMainPanel.revalidate();
                            trashedNoteMainPanel.repaint();
                        }
                    });

                }
            }

        });

        trashNotesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {

                if (!isTrashListEmpty()) {
                    trashedNoteTitleTextArea.setVisible(true);
                    trashedNoteTextArea.setVisible(true);
                }

                if (!evt.getValueIsAdjusting()) {
                    Trash selectedNote = trashNotesList.getSelectedValue();
                    if (selectedNote != null) {
                        int noteId = selectedNote.getId();
                        currentTrashId = noteId;
                        setTitleAndContentForTrash(noteId);
                        setTrashTextCounter();

                        updateLastEditedText(trashLastEditedText, getLastEditedFromTrash(noteId));

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                trashedNoteMainPanel.getVerticalScrollBar().setValue(0);
                                trashedNoteMainPanel.revalidate();
                                trashedNoteMainPanel.repaint();
                            }
                        });

                    }
                }
            }
        });

        if (trashNotesList.getModel().getSize() > 0) {
            trashNotesList.setSelectedIndex(0);
        }
    }

    private long calculateRemainingDays(Timestamp deletedAt) {
        if (deletedAt == null) {
            return 30; // Handle null deletedAt as needed
        }
        Instant deletedInstant = deletedAt.toInstant();
        LocalDateTime deletedDateTime = LocalDateTime.ofInstant(deletedInstant, ZoneId.systemDefault());

        LocalDateTime now = LocalDateTime.now();

        Duration duration = Duration.between(now, deletedDateTime.plusDays(30)); //30 days is the expiration time
        return duration.toDays();
    }

    private void selectIndex0() {

        if (!isTrashListEmpty()) {
            trashedNoteTitleTextArea.setVisible(true);
            trashedNoteTextArea.setVisible(true);
        }

        if (!notesList.isSelectionEmpty()) {
            Note selectedNote = notesList.getSelectedValue();
            int noteId = selectedNote.getId();
            Timestamp lastEdited = selectedNote.getUpdatedAt();
            currentNoteId = noteId;
            setTitleAndContent(noteId);

            LocalDateTime dateTime = lastEdited.toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String dateString = dateTime.format(formatter);

            updateLastEditedText(lastEditedText, dateString);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    notesCreationMainPanel.getVerticalScrollBar().setValue(0);
                    notesList.revalidate();
                    notesList.repaint();
                }
            });

        }
    }

    private void selectTrashIndex0() {
        if (!trashNotesList.isSelectionEmpty()) {
            Trash selectedNote = trashNotesList.getSelectedValue();
            int noteId = selectedNote.getId();
            Timestamp lastEdited = selectedNote.getUpdatedAt();
            currentTrashId = noteId;
            setTitleAndContentForTrash(noteId);

            LocalDateTime dateTime = lastEdited.toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String dateString = dateTime.format(formatter);

            updateLastEditedText(trashLastEditedText, dateString);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    trashedNoteMainPanel.getVerticalScrollBar().setValue(0);
                    trashNotesList.revalidate();
                    trashNotesList.repaint();
                }
            });

        }
    }

    private void getAllNoteIdsForCurrentUser() {

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        noteIds.clear();

        try {
            database.connect();
            connection = database.getConnection();

            String query = "SELECT note_id FROM notes WHERE user_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int noteId = resultSet.getInt("note_id");
                if (!noteIds.contains(noteId)) {
                    noteIds.add(noteId);
                }
            }

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
            }
        }

    }

    private void getAllTrashIdsForCurrentUser() {

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        trashIds.clear();

        try {
            database.connect();
            connection = database.getConnection();

            String query = "SELECT trash_id FROM trash WHERE user_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int noteId = resultSet.getInt("trash_id");
                if (!trashIds.contains(noteId)) {
                    trashIds.add(noteId);
                }
            }

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
            }
        }

    }

    public final void setTextCounter() {
        if (countNotes() == 1) {
            notesCounter.setText(countNotes() + " note");
        } else {
            notesCounter.setText(countNotes() + " notes");
        }
    }

    public final void setTrashTextCounter() {
        if (countTrashNotes() == 1) {
            trashNotesCounter.setText(countTrashNotes() + " note");
        } else {
            trashNotesCounter.setText(countTrashNotes() + " notes");
        }
    }

    public final void setPlaceholders() {
        PromptSupport.setPrompt("Start writing...", scratchPadTextArea);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT, scratchPadTextArea);
        PromptSupport.setForeground(new Color(171, 171, 171), scratchPadTextArea);

        PromptSupport.setPrompt("Title", noteTitleTextArea);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT, noteTitleTextArea);
        PromptSupport.setForeground(new Color(171, 171, 171), noteTitleTextArea);

        PromptSupport.setPrompt("Start writing here", noteCreationTextArea);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT, noteCreationTextArea);
        PromptSupport.setForeground(new Color(171, 171, 171), noteCreationTextArea);
    }

    public final void scaleImageAsIcon(String filePath, JLabel label) {
        try (InputStream inputStream = getClass().getResourceAsStream(filePath)) {
            if (inputStream != null) {
                Image image = ImageIO.read(inputStream);
                if (image != null) {

                    Dimension labelSize = label.getPreferredSize();

                    Image scaledImage = image.getScaledInstance(labelSize.width, labelSize.height, Image.SCALE_SMOOTH);
                    ImageIcon scaledIcon = new ImageIcon(scaledImage);
                    label.setIcon(scaledIcon);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to load image", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Image not found", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scaleImage(File file) {
        try {
            if (file.exists()) {
                Image image = ImageIO.read(file);
                if (image != null) {

                    Icon icon = new ImageIcon(image);

                } else {
                    JOptionPane.showMessageDialog(this, "Failed to load image", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Image file not found", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processRetrievedImage() {
        try {
            byte[] imageData;
            imageData = imageHandler.retrieveImage(id);
            if (imageData != null) {
                imageHandler.saveImageToFile(imageData, fileName);
                File imageFile = new File(fileName);
                Dashboard.this.scaleImage(imageFile);
            } else {
                System.out.println("No image found for the given ID.");
            }
        } catch (SQLException | IOException e) {

        }
    }

    private void updateColorWhenHovered(JPanel panel, Color color) {
        panel.setBackground(color);
        panel.revalidate();
        panel.repaint();
    }

    private void changeButtonState(JPanel active, JPanel inactive1, JPanel inactive2) {

        Color originalColor = Color.WHITE;
        Color activeColor = new Color(230, 230, 230);
        JPanel[] inactivePanel = new JPanel[2];

        inactivePanel[0] = inactive1;
        inactivePanel[1] = inactive2;

        active.setName("active");
        active.setBackground(activeColor);

        for (JPanel inactive : inactivePanel) {
            inactive.setName("");
            inactive.setBackground(originalColor);
        }

    }

    private void attachDocumentListenersToScratchPad() {

        scratchPadTextArea.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                enableScratchPadOptions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableScratchPadOptions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableScratchPadOptions();
            }

        });

    }

    private void enableScratchPadOptions() {
        if (!scratchPadTextArea.getText().isEmpty()) {
            scratchOption1.setEnabled(true);
            scratchOption2.setEnabled(true);
        } else {
            scratchOption1.setEnabled(false);
            scratchOption2.setEnabled(false);
        }
    }

    public String formatTimestampToSQL(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        System.out.println(dateTime.toString());

        String formattedTime = dateTime.format(formatter);
        System.out.println(formattedTime);
        return formattedTime;
    }

    private int countNotes() {

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            database.connect();
            connection = database.getConnection();

            String query = "SELECT COUNT(*) FROM notes WHERE user_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();

            resultSet.next();

            return resultSet.getInt(1);

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
            return 0;
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                return 0;
            }
        }

    }

    private int countTrashNotes() {

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            database.connect();
            connection = database.getConnection();

            String query = "SELECT COUNT(*) FROM trash WHERE user_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();

            resultSet.next();

            return resultSet.getInt(1);

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
            return 0;
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                return 0;
            }
        }

    }

    private boolean isNoteListEmpty() {
        return countNotes() == 0;
    }

    private boolean isTrashListEmpty() {
        return countTrashNotes() == 0;
    }

    private void addNoteToDatabase() {

        LocalDateTime now = LocalDateTime.now();

        String title = "Untitled";
        String content = "";
        String timeStamp = formatTimestampToSQL(now);

        timeStamp = timeStamp.trim().replace("\r", "").replace("\n", "");

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            database.connect();
            connection = database.getConnection();
            connection.setAutoCommit(false);

            String query = "INSERT INTO notes(note_title, note_content, note_created_at, note_updated_at, user_id) VALUES (?, ?, ?, ?, ?)";
            statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, timeStamp);
            statement.setString(4, timeStamp);
            statement.setInt(5, id);

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("A new note was inserted successfully!");

                resultSet = statement.getGeneratedKeys();
                if (resultSet.next()) {
                    lastInsertedId = resultSet.getInt(1);
                    System.out.println("Last inserted ID: " + lastInsertedId);
                }
            }

            connection.commit();

        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                ex.printStackTrace();
            }
        }

    }

    private void addNoteToDatabaseFromScratchPad() {

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

        String title = "Scratch Pad - " + now.format(outputFormatter);
        String content = scratchPadTextArea.getText();
        String timeStamp = formatTimestampToSQL(now);

        DatabaseConnector db = new DatabaseConnector();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            db.connect();
            connection = db.getConnection();
            connection.setAutoCommit(false);

            String query = "INSERT INTO notes(note_title, note_content, note_created_at, note_updated_at, user_id) VALUES (?, ?, ?, ?, ?)";
            statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, timeStamp);
            statement.setString(4, timeStamp);
            statement.setInt(5, id);

            int rowsInserted = statement.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("A new note was inserted successfully!");

                resultSet = statement.getGeneratedKeys();
                if (resultSet.next()) {
                    lastInsertedId = resultSet.getInt(1);
                    System.out.println("Last inserted ID: " + lastInsertedId);
                }
            }

            connection.commit();

        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
                db.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                ex.printStackTrace();
            }
        }

    }

    private void addNotesToList() {

        Set<Integer> uniqueNoteIds = new HashSet<>(noteIds);
        Note newNote = null;

        for (int id : uniqueNoteIds) {
            String title = "";
            Timestamp updatedAt = null;
            Timestamp createdAt = null;

            PreparedStatement statement = null;
            ResultSet resultSet = null;
            Connection connection = null;

            try {
                database.connect();
                connection = database.getConnection();

                String query = "SELECT note_title, note_created_at, note_updated_at FROM notes WHERE note_id = ?";
                statement = connection.prepareStatement(query);
                statement.setInt(1, id);
                resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    title = resultSet.getString("note_title");
                    createdAt = resultSet.getTimestamp("note_created_at");
                    updatedAt = resultSet.getTimestamp("note_updated_at");
                }

                System.out.println(title);

                Note note = new Note(id, title, createdAt, updatedAt);

                int noteIndex = -1;
                for (int i = 0; i < listModel.getSize(); i++) {
                    Note existingNote = listModel.getElementAt(i);
                    if (existingNote.getId() == note.getId()) {
                        noteIndex = i;
                        break;
                    }
                }

                if (noteIndex == -1) {
                    listModel.addElement(note);
                } else {
                    listModel.setElementAt(note, noteIndex);
                }

                fireListDataChanged();

                final Note finalNewNote = (id == lastInsertedId) ? note : null;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        notesListSubPanel.revalidate();
                        notesList.repaint();
                    }
                });

            } catch (SQLException e) {
                new DatabaseError().setVisible(true);
                e.printStackTrace();
            } finally {
                try {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                    database.close();
                } catch (SQLException ex) {
                    new DatabaseError().setVisible(true);
                    ex.printStackTrace();
                }
            }

        }
    }

    private void addTrashToList() {

        Set<Integer> uniqueNoteIds = new HashSet<>(trashIds);
        Trash newNote = null;

        for (int id : uniqueNoteIds) {
            String title = "";
            Timestamp updatedAt = null;
            Timestamp createdAt = null;
            Timestamp deletedAt = null;

            PreparedStatement statement = null;
            ResultSet resultSet = null;
            Connection connection = null;

            try {
                database.connect();
                connection = database.getConnection();

                String query = "SELECT trash_title, trash_deleted_at, trash_updated_at, trash_created_at FROM trash WHERE trash_id = ?";
                statement = connection.prepareStatement(query);
                statement.setInt(1, id);
                resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    title = resultSet.getString("trash_title");
                    createdAt = resultSet.getTimestamp("trash_created_at");
                    updatedAt = resultSet.getTimestamp("trash_updated_at");
                    deletedAt = resultSet.getTimestamp("trash_deleted_at");
                }

                Trash note = new Trash(id, title, createdAt, deletedAt, updatedAt);

                long remainingDays = calculateRemainingDays(note.getDeletedAt());

                SwingUtilities.invokeLater(() -> {
                    if (remainingDays <= 0) {
                        expirationNotice.setText("Expired");
                    } else if (remainingDays <= 30) {
                        expirationNotice.setText("Expires in " + remainingDays + (remainingDays == 1 ? " day" : " days"));
                    } else {
                        expirationNotice.setVisible(false);
                        restoreCurrentNoteButtonPanel.setVisible(false);
                    }
                });

                boolean noteExists = false;
                for (int i = 0; i < listModelTrash.getSize(); i++) {
                    Trash existingNote = listModelTrash.getElementAt(i);
                    if (existingNote.getId() == note.getId()) {
                        noteExists = true;
                        if (!existingNote.getTitle().equals(note.getTitle())) {
                            existingNote.setTitle(note.getTitle());
                        }
                        break;
                    }
                }

                if (!noteExists || (updatedAt != null && updatedAt.after(note.getUpdatedAt()))) {
                    if (!noteExists) {
                        listModelTrash.add(0, note);
                    } else {
                        listModelTrash.set(0, note);
                    }
                    if (id == lastInsertedTrashId) {
                        newNote = note;
                    }

                    fireListDataChangedForTrash();
                }

            } catch (SQLException e) {
                new DatabaseError().setVisible(true);
                e.printStackTrace();
            } finally {
                try {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                    database.close();
                } catch (SQLException ex) {
                    new DatabaseError().setVisible(true);
                    ex.printStackTrace();
                }
            }
        }

        final Trash finalNewNote = newNote;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                trashNotesListSubPanel.revalidate();
                trashNotesList.repaint();
                if (finalNewNote != null) {
                    trashNotesList.setSelectedValue(finalNewNote, true);
                    trashedNoteTitleTextArea.setText("Untitled");
                    trashedNoteTextArea.setText("");
                }
            }
        });
    }

    private void fireListDataChanged() {
        ListDataEvent event = new ListDataEvent(
                listModel, ListDataEvent.CONTENTS_CHANGED, 0, listModel.getSize() - 1);

        for (ListDataListener listener : listModel.getListDataListeners()) {
            listener.contentsChanged(event);
        }

        notesList.repaint();
    }

    private void fireListDataChangedForTrash() {
        ListDataEvent event = new ListDataEvent(
                listModelTrash, ListDataEvent.CONTENTS_CHANGED, 0, listModelTrash.getSize() - 1);

        for (ListDataListener listener : listModelTrash.getListDataListeners()) {
            listener.contentsChanged(event);
        }

        trashNotesList.repaint();
    }

    private void attachDocumentListenersToTextAreas() {
        noteTitleTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateNote();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateNote();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateNote();
            }
        });

        noteCreationTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateNote();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateNote();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateNote();
            }
        });

    }

    private void updateNote() {

        LocalDateTime now = LocalDateTime.now();
        String timeStamp = formatTimestampToSQL(now);
        String title = noteTitleTextArea.getText();
        String content = noteCreationTextArea.getText();

        if (title.isBlank() || title == null) {
            title = "Untitled";
        }

        timeStamp = timeStamp.trim().replace("\r", "").replace("\n", "");

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            database.connect();
            connection = database.getConnection();

            String query = "UPDATE notes SET note_title = ?, note_content = ?, note_updated_at = ? WHERE note_id = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, timeStamp);
            statement.setInt(4, currentNoteId);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                addNotesToList();
                updateLastEditedText(lastEditedText, timeStamp);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        notesCreationMainPanel.getVerticalScrollBar().setValue(0);
                        notesCreationMainPanel.revalidate();
                        notesCreationMainPanel.repaint();
                    }
                });

            }

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
                addTrashToList();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                ex.printStackTrace();
            }
        }

    }

    private void deleteNoteForever() {
        Connection conn = null;
        PreparedStatement deleteStmt = null;
        DatabaseConnector db = new DatabaseConnector();

        try {
            db.connect();
            conn = db.getConnection();
            System.out.println("Connection opened: " + conn);

            String deleteQuery = "DELETE FROM trash WHERE trash_id = ?";
            deleteStmt = conn.prepareStatement(deleteQuery);
            deleteStmt.setInt(1, currentTrashId);
            int rowsAffected = deleteStmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Note with ID " + currentTrashId + " deleted from the database.");

                Trash noteToRemove = null;
                for (int i = 0; i < listModelTrash.getSize(); i++) {
                    Trash note = listModelTrash.getElementAt(i);
                    if (note.getId() == currentTrashId) {
                        noteToRemove = note;
                        break;
                    }
                }

                if (noteToRemove != null) {
                    listModelTrash.removeElement(noteToRemove);
                    fireListDataChangedForTrash();
                }
            } else {
                System.out.println("Note with ID " + currentTrashId + " not found in the database.");
            }
        } catch (SQLException e) {
            System.err.println("SQLException caught: " + e.getMessage());
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (deleteStmt != null) {
                    deleteStmt.close();
                }
                if (conn != null) {
                    conn.close();
                    System.out.println("Connection closed: " + conn);
                }
                db.close();
            } catch (SQLException e) {
                new DatabaseError().setVisible(true);
                e.printStackTrace();
            }
        }
    }

    private void setTitleAndContent(int noteId) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        settingTitleAndContent = true;

        try {
            database.connect();
            connection = database.getConnection();

            String query = "SELECT note_title, note_content FROM notes WHERE note_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, noteId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String title = resultSet.getString("note_title");
                String content = resultSet.getString("note_content");

                SwingUtilities.invokeLater(() -> { // Update UI on the EDT
                    noteTitleTextArea.setText(title);
                    noteCreationTextArea.setText(content);
                    settingTitleAndContent = false;
                    //... other ui updates related to setting title and content
                });

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        notesCreationMainPanel.getVerticalScrollBar().setValue(0);
                        notesCreationMainPanel.revalidate();
                        notesCreationMainPanel.repaint();
                    }
                });
            }

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                ex.printStackTrace();
            }
        }
    }

    private void setTitleAndContentForTrash(int noteId) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Connection connection = null;

        try {
            database.connect();
            connection = database.getConnection();

            String query = "SELECT trash_title, trash_content FROM trash WHERE trash_id = ?";
            statement = connection.prepareStatement(query);
            statement.setInt(1, noteId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String title = resultSet.getString("trash_title");
                String content = resultSet.getString("trash_content");

                trashedNoteTitleTextArea.setText(title);
                trashedNoteTextArea.setText(content);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        trashedNoteMainPanel.getVerticalScrollBar().setValue(0);
                        trashedNoteMainPanel.revalidate();
                        trashedNoteMainPanel.repaint();
                    }
                });
            }

        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                ex.printStackTrace();
            }
        }
    }

    private void handleAddNote() {
        addNoteToDatabase();
        getAllNoteIdsForCurrentUser();
        addNotesToList();
    }

    private void updateLastEditedText(JLabel label, String timeStamp) {
        if (timeStamp == null || timeStamp.isBlank()) {

            label.setText("Last edited: Never");
            return;
        }

        DateTimeFormatter inputFormatter;
        if (timeStamp.length() > 19) {
            inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        } else {
            inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }
        LocalDateTime lastEdited = LocalDateTime.parse(timeStamp, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        label.setText("Last edited on " + lastEdited.format(outputFormatter));
    }

    private void addNoteToTrash() {

        DatabaseConnector db = new DatabaseConnector();
        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement deleteStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            db.connect();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            System.out.println("Connection opened: " + conn);

            String selectQuery = "SELECT note_title, note_content, note_created_at, note_updated_at FROM notes WHERE note_id = ?";
            selectStmt = conn.prepareStatement(selectQuery);
            selectStmt.setInt(1, currentNoteId);
            rs = selectStmt.executeQuery();

            String noteTitle = null;
            String noteContent = null;
            Timestamp noteCreatedAt = null;
            Timestamp noteUpdatedAt = null;

            if (rs.next()) {
                noteTitle = rs.getString("note_title");
                noteContent = rs.getString("note_content");
                noteCreatedAt = rs.getTimestamp("note_created_at");
                noteUpdatedAt = rs.getTimestamp("note_updated_at");
            }

            if (noteTitle != null) {

                String deleteQuery = "DELETE FROM notes WHERE note_id = ?";
                deleteStmt = conn.prepareStatement(deleteQuery);
                deleteStmt.setInt(1, currentNoteId);
                deleteStmt.executeUpdate();

                String insertQuery = "INSERT INTO trash (trash_title, trash_content, trash_deleted_at, trash_created_at, trash_updated_at, user_id) VALUES (?, ?, ?, ?, ?, ?)";
                insertStmt = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                insertStmt.setString(1, noteTitle);
                insertStmt.setString(2, noteContent);
                insertStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                insertStmt.setTimestamp(4, noteCreatedAt);
                insertStmt.setTimestamp(5, noteUpdatedAt);
                insertStmt.setInt(6, id);
                insertStmt.executeUpdate();

                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int trashId = generatedKeys.getInt(1);
                    System.out.println("Last inserted trash ID: " + trashId);
                    lastInsertedTrashId = trashId;
                }

                conn.commit();

                Note noteToRemove = null;
                for (int i = 0; i < listModel.getSize(); i++) {
                    Note note = listModel.getElementAt(i);
                    if (note.getId() == currentNoteId) {
                        noteToRemove = note;
                        break;
                    }
                }

                if (noteToRemove != null) {
                    listModel.removeElement(noteToRemove);
                    fireListDataChanged();
                }

                Trash trashedNote = new Trash(lastInsertedTrashId, noteTitle, noteCreatedAt, Timestamp.valueOf(LocalDateTime.now()), noteUpdatedAt);
                listModelTrash.add(0, trashedNote);
                fireListDataChangedForTrash();

            } else {
                System.out.println("Note with ID " + currentNoteId + " not found.");
            }
        } catch (SQLException e) {
            System.err.println("SQLException caught: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transaction rolled back.");
                } catch (SQLException rollbackException) {
                    new DatabaseError().setVisible(true);
                    rollbackException.printStackTrace();
                }
            }
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (selectStmt != null) {
                    selectStmt.close();
                }
                if (deleteStmt != null) {
                    deleteStmt.close();
                }
                if (insertStmt != null) {
                    insertStmt.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                    System.out.println("Connection closed: " + conn);
                }
                db.close();
            } catch (SQLException e) {
                new DatabaseError().setVisible(true);
                e.printStackTrace();
            }
        }
    }

    private void restoreNoteFromTrash() {

        DatabaseConnector db = new DatabaseConnector();
        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement deleteStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            db.connect();
            conn = db.getConnection();
            conn.setAutoCommit(false);

            String selectQuery = "SELECT trash_title, trash_content, trash_updated_at, trash_created_at FROM trash WHERE trash_id = ?";
            selectStmt = conn.prepareStatement(selectQuery);
            selectStmt.setInt(1, currentTrashId);
            rs = selectStmt.executeQuery();

            String noteTitle = null;
            String noteContent = null;
            Timestamp noteCreatedAt = null;
            Timestamp noteUpdatedAt = null;

            if (rs.next()) {
                noteTitle = rs.getString("trash_title");
                noteContent = rs.getString("trash_content");
                noteCreatedAt = rs.getTimestamp("trash_created_at");
                noteUpdatedAt = rs.getTimestamp("trash_updated_at");
            }

            if (noteTitle != null) {

                String deleteQuery = "DELETE FROM trash WHERE trash_id = ?";
                deleteStmt = conn.prepareStatement(deleteQuery);
                deleteStmt.setInt(1, currentTrashId);
                deleteStmt.executeUpdate();

                String insertQuery = "INSERT INTO notes (note_title, note_content, note_created_at, note_updated_at, user_id) VALUES (?, ?, ?, ?, ?)";
                insertStmt = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                insertStmt.setString(1, noteTitle);
                insertStmt.setString(2, noteContent);
                insertStmt.setTimestamp(3, noteCreatedAt);
                insertStmt.setTimestamp(4, noteUpdatedAt);
                insertStmt.setInt(5, id);
                insertStmt.executeUpdate();

                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int noteId = generatedKeys.getInt(1);
                    System.out.println("Last inserted trash ID: " + noteId);
                    lastInsertedId = noteId;
                }

                conn.commit();

                Trash noteToRemove = null;
                for (int i = 0; i < listModelTrash.getSize(); i++) {
                    Trash note = listModelTrash.getElementAt(i);
                    if (note.getId() == currentTrashId) {
                        noteToRemove = note;
                        break;
                    }
                }

                if (noteToRemove != null) {
                    int index = trashNotesList.getSelectedIndex();
                    listModelTrash.removeElement(noteToRemove);
                    fireListDataChangedForTrash();

                    if (listModelTrash.isEmpty()) { // List is now empty
                        trashedNoteTitleTextArea.setVisible(false);
                        trashedNoteTextArea.setVisible(false);
                        trashNotesList.clearSelection(); // Crucial: Clear the selection
                    } else {
                        if (index > 0) { // was there an item below
                            trashNotesList.setSelectedIndex(index - 1);
                        } else {
                            trashNotesList.setSelectedIndex(0);
                        }
                    }

                    trashNotesList.revalidate();
                    trashNotesList.repaint();
                }

                Note note = new Note(lastInsertedId, noteTitle, noteCreatedAt, noteUpdatedAt);
                listModel.add(0, note);
                fireListDataChangedForTrash();
                fireListDataChanged();

            } else {
                System.out.println("Note with ID " + currentTrashId + " not found.");
            }
        } catch (SQLException e) {
            System.err.println("SQLException caught: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transaction rolled back.");
                } catch (SQLException rollbackException) {
                    new DatabaseError().setVisible(true);
                    rollbackException.printStackTrace();
                }
            }
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (selectStmt != null) {
                    selectStmt.close();
                }
                if (deleteStmt != null) {
                    deleteStmt.close();
                }
                if (insertStmt != null) {
                    insertStmt.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
                db.close();
            } catch (SQLException e) {
                new DatabaseError().setVisible(true);
                e.printStackTrace();
            }
        }
    }

    private void handleTrashNote() {
        addNoteToTrash();
        getAllNoteIdsForCurrentUser();
        addNotesToList();
        getAllTrashIdsForCurrentUser();
        addTrashToList();
    }

    private void handleRestoredNote() {
        restoreNoteFromTrash();
        getAllNoteIdsForCurrentUser();
        addNotesToList();
        getAllTrashIdsForCurrentUser();
        addTrashToList();
    }

    private void exportNotes() {
        String excelFilePath = currentUser.getUserName() + "\'s notes.xlsx";

        String query = "SELECT note_title, note_content, note_created_at, note_updated_at FROM notes where user_id = ?";

        DatabaseConnector db = new DatabaseConnector();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            db.connect();
            connection = db.getConnection();

            statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(currentUser.getUserName() + "\'s notes");

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            Row headerRow = sheet.createRow(0);
            for (int i = 1; i <= columnCount; i++) {
                Cell cell = headerRow.createCell(i - 1);
                cell.setCellValue(metaData.getColumnName(i));
            }

            int rowIndex = 1;
            while (resultSet.next()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = row.createCell(i - 1);
                    cell.setCellValue(resultSet.getString(i));
                }
            }

            Path outputDir = Paths.get(System.getProperty("user.dir"), "exported");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            try (FileOutputStream fileOut = new FileOutputStream(outputDir.resolve(excelFilePath).toString())) {
                workbook.write(fileOut);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Excel file creation unsuccessful", "Excel File Unsuccessful", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (SQLException e) {
            new DatabaseError().setVisible(true);
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
                database.close();
            } catch (SQLException ex) {
                new DatabaseError().setVisible(true);
                ex.printStackTrace();
            }
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        moreOptions = new javax.swing.JPopupMenu();
        option1 = new javax.swing.JMenuItem();
        option2 = new javax.swing.JMenuItem();
        scratchPadOptions = new javax.swing.JPopupMenu();
        scratchOption1 = new javax.swing.JMenuItem();
        scratchOption2 = new javax.swing.JMenuItem();
        mainPanel = new javax.swing.JPanel();
        leftPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        userPanel = new javax.swing.JPanel();
        userNameInitialPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        userNameInitial = new javax.swing.JLabel();
        currentUserPanel = new javax.swing.JPanel();
        usernameLabel = new javax.swing.JLabel();
        userEmailLabel = new javax.swing.JLabel();
        newNoteButton = new com.mycompany.notesmanagement.custom.RoundedPanel();
        newNoteButtonIcon = new javax.swing.JLabel();
        newNoteButtonLabel = new javax.swing.JLabel();
        homeButton = new com.mycompany.notesmanagement.custom.RoundedPanel();
        homeButtonIcon = new javax.swing.JLabel();
        homeButtonLabel = new javax.swing.JLabel();
        notesButton = new com.mycompany.notesmanagement.custom.RoundedPanel();
        notesButtonIcon = new javax.swing.JLabel();
        notesButtonLabel = new javax.swing.JLabel();
        trashButton = new com.mycompany.notesmanagement.custom.RoundedPanel();
        trashButtonIcon = new javax.swing.JLabel();
        trashButtonLabel = new javax.swing.JLabel();
        logoutButton = new com.mycompany.notesmanagement.custom.RoundedPanel();
        logoutButtonIcon = new javax.swing.JLabel();
        logoutButtonLabel = new javax.swing.JLabel();
        trademark = new javax.swing.JLabel();
        rightPanel = new javax.swing.JPanel();
        homePanel = new javax.swing.JPanel();
        homeTopPanel = new javax.swing.JPanel();
        questionLabel = new javax.swing.JLabel();
        userHomeLabel = new javax.swing.JLabel();
        homeBottomPanel = new javax.swing.JPanel();
        manageNotesDashboardIcon = new javax.swing.JPanel();
        homeBottomUpperPanel = new javax.swing.JPanel();
        notesTextPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        notesText = new javax.swing.JLabel();
        newNoteButtonDashboardPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        newNoteButtonDashboard = new javax.swing.JLabel();
        moreOptionsNotesDashboardButtonPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        moreOptionsNotesDashboardButton = new javax.swing.JLabel();
        actionsBorderPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        actionsPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        actionsText = new javax.swing.JLabel();
        notesContainer = new javax.swing.JScrollPane();
        notesDashboardPanel = new javax.swing.JPanel();
        createNotesContainer = new javax.swing.JPanel();
        createNotesPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        createNoteIconPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        createNoteIcon = new javax.swing.JLabel();
        createNewNoteText = new javax.swing.JLabel();
        viewNotesPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        viewNotesIconPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        viewNotesIcon = new javax.swing.JLabel();
        viewNotesText = new javax.swing.JLabel();
        manageNotesPanelDashboard = new com.mycompany.notesmanagement.custom.RoundedPanel();
        manageNotesDashboardIconPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        manageNotesDashboardBottomIcon = new javax.swing.JLabel();
        manageNotesDashboardText = new javax.swing.JLabel();
        scratchPadPanel = new javax.swing.JPanel();
        scratchPadTopPanel = new javax.swing.JPanel();
        scratchPadText = new javax.swing.JLabel();
        scratchPadMoreOptionsPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        scratchPadMoreOptions = new javax.swing.JLabel();
        scratchPadBottomPanel = new javax.swing.JPanel();
        scratchPadTextPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        scratchPadScroll = new javax.swing.JScrollPane();
        scratchPadTextArea = new javax.swing.JTextArea();
        notesPanel = new javax.swing.JPanel();
        notesListPanel = new javax.swing.JPanel();
        notesListTopPanel = new javax.swing.JPanel();
        notesTextList = new javax.swing.JLabel();
        noteListOptions = new javax.swing.JPanel();
        notesCounter = new javax.swing.JLabel();
        sortButtonPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        sortButton = new javax.swing.JLabel();
        exportButtonPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        exportButton = new javax.swing.JLabel();
        notesListMainPanelScroll = new javax.swing.JScrollPane();
        notesListMainPanel = new javax.swing.JPanel();
        notesListSubPanel = new javax.swing.JPanel();
        notesListScrollPane = new javax.swing.JScrollPane();
        notesList = new javax.swing.JList<>();
        paddingPanel = new javax.swing.JPanel();
        notesCreationPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        notesCreationPanelTop = new javax.swing.JPanel();
        expandToggleButtonPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        expandToggleButton = new javax.swing.JLabel();
        verticalLine = new javax.swing.JLabel();
        lastEditedText = new javax.swing.JLabel();
        deleteCurrentNoteButtonPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        deleteCurrentNoteButton = new javax.swing.JLabel();
        notesCreationPanelBottom = new javax.swing.JPanel();
        notesCreationMainPanel = new javax.swing.JScrollPane();
        notesCreationSubPanel = new javax.swing.JPanel();
        notesCreationTitlePanel = new javax.swing.JPanel();
        noteTitleTextArea = new javax.swing.JTextArea();
        notesCreationContentPanel = new javax.swing.JPanel();
        noteCreationTextArea = new javax.swing.JTextArea();
        trashPanel = new javax.swing.JPanel();
        trashNotesListPanel = new javax.swing.JPanel();
        trashNotesListTopPanel = new javax.swing.JPanel();
        trashNotesTextList = new javax.swing.JLabel();
        trashNoteListOptions = new javax.swing.JPanel();
        trashNotesCounter = new javax.swing.JLabel();
        sortButtonPanel1 = new com.mycompany.notesmanagement.custom.RoundedPanel();
        sortButton1 = new javax.swing.JLabel();
        trashNotesListMainPanelScroll = new javax.swing.JScrollPane();
        trashNotesListMainPanel = new javax.swing.JPanel();
        trashNotesListSubPanel = new javax.swing.JPanel();
        trashNotesListScrollPane1 = new javax.swing.JScrollPane();
        trashNotesList = new javax.swing.JList<>();
        paddingPanel1 = new javax.swing.JPanel();
        trashNotesPreviewPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        trashNotesCreationPanelTop = new javax.swing.JPanel();
        trashExpandToggleButtonPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        expandToggleButton1 = new javax.swing.JLabel();
        trashVerticalLine = new javax.swing.JLabel();
        noteInTrashNoticePanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        noteInTrashNotice = new javax.swing.JLabel();
        expirationNotice = new javax.swing.JLabel();
        trashLastEditedText = new javax.swing.JLabel();
        restoreCurrentNoteButtonPanel = new com.mycompany.notesmanagement.custom.RoundedPanel();
        restoreCurrentNoteButton = new javax.swing.JLabel();
        deleteCurrentNoteForever = new com.mycompany.notesmanagement.custom.RoundedPanel();
        deleteCurrentNote = new javax.swing.JLabel();
        trashedNotePanelBottom = new javax.swing.JPanel();
        trashedNoteMainPanel = new javax.swing.JScrollPane();
        trashedNoteSubPanel = new javax.swing.JPanel();
        trashedNoteTitlePanel = new javax.swing.JPanel();
        trashedNoteTitleTextArea = new javax.swing.JTextArea();
        trashedNoteContentPanel = new javax.swing.JPanel();
        trashedNoteTextArea = new javax.swing.JTextArea();
        settingsPanel = new javax.swing.JPanel();
        manageNotesPanel = new javax.swing.JPanel();

        moreOptions.setBackground(java.awt.Color.white);
        moreOptions.setForeground(java.awt.Color.white);

        option1.setFont(new java.awt.Font("Inter Medium", 0, 12)); // NOI18N
        option1.setForeground(java.awt.Color.black);
        option1.setText("Go to Notes");
        option1.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        option1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                option1ActionPerformed(evt);
            }
        });
        moreOptions.add(option1);

        option2.setFont(new java.awt.Font("Inter Medium", 0, 12)); // NOI18N
        option2.setForeground(java.awt.Color.black);
        option2.setText("Create New Note");
        option2.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        option2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                option2ActionPerformed(evt);
            }
        });
        moreOptions.add(option2);

        scratchPadOptions.setBackground(java.awt.Color.white);

        scratchOption1.setFont(new java.awt.Font("Inter Medium", 0, 12)); // NOI18N
        scratchOption1.setText("Convert to note");
        scratchOption1.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        scratchOption1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scratchOption1ActionPerformed(evt);
            }
        });
        scratchPadOptions.add(scratchOption1);

        scratchOption2.setFont(new java.awt.Font("Inter Medium", 0, 12)); // NOI18N
        scratchOption2.setText("Clear scratch pad");
        scratchOption2.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        scratchOption2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scratchOption2ActionPerformed(evt);
            }
        });
        scratchPadOptions.add(scratchOption2);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        mainPanel.setBackground(new java.awt.Color(246, 243, 247));
        mainPanel.setLayout(new java.awt.GridBagLayout());

        leftPanel.setBackground(java.awt.Color.white);
        leftPanel.setForeground(java.awt.Color.white);
        leftPanel.setPreferredSize(new java.awt.Dimension(250, 100));
        leftPanel.setRadiusBottomLeft(20);
        leftPanel.setRadiusBottomRight(20);
        leftPanel.setRadiusTopLeft(20);
        leftPanel.setRadiusTopRight(20);
        leftPanel.setLayout(new java.awt.GridBagLayout());

        userPanel.setBackground(java.awt.Color.red);
        userPanel.setMinimumSize(new java.awt.Dimension(200, 200));
        userPanel.setOpaque(false);
        userPanel.setLayout(new java.awt.GridBagLayout());

        userNameInitialPanel.setBackground(new java.awt.Color(0, 168, 45));
        userNameInitialPanel.setMaximumSize(new java.awt.Dimension(50, 50));
        userNameInitialPanel.setMinimumSize(new java.awt.Dimension(50, 50));
        userNameInitialPanel.setPreferredSize(new java.awt.Dimension(60, 60));
        userNameInitialPanel.setRadiusBottomLeft(60);
        userNameInitialPanel.setRadiusBottomRight(60);
        userNameInitialPanel.setRadiusTopLeft(60);
        userNameInitialPanel.setRadiusTopRight(60);
        userNameInitialPanel.setLayout(new java.awt.GridBagLayout());

        userNameInitial.setFont(new java.awt.Font("Inter ExtraBold", 1, 30)); // NOI18N
        userNameInitial.setForeground(java.awt.Color.white);
        userNameInitial.setText("J");
        userNameInitialPanel.add(userNameInitial, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        userPanel.add(userNameInitialPanel, gridBagConstraints);

        currentUserPanel.setBackground(java.awt.Color.white);
        currentUserPanel.setOpaque(false);
        currentUserPanel.setPreferredSize(new java.awt.Dimension(50, 50));
        currentUserPanel.setLayout(new java.awt.GridBagLayout());

        usernameLabel.setBackground(java.awt.Color.white);
        usernameLabel.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
        usernameLabel.setForeground(java.awt.Color.black);
        usernameLabel.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        currentUserPanel.add(usernameLabel, gridBagConstraints);

        userEmailLabel.setFont(new java.awt.Font("Poppins", 0, 10)); // NOI18N
        userEmailLabel.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
        currentUserPanel.add(userEmailLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 10.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        userPanel.add(currentUserPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 20, 0);
        leftPanel.add(userPanel, gridBagConstraints);

        newNoteButton.setBackground(new java.awt.Color(92, 173, 236));
        newNoteButton.setForeground(new java.awt.Color(128, 207, 255));
        newNoteButton.setPreferredSize(new java.awt.Dimension(100, 50));
        newNoteButton.setRadiusBottomLeft(10);
        newNoteButton.setRadiusBottomRight(10);
        newNoteButton.setRadiusTopLeft(10);
        newNoteButton.setRadiusTopRight(10);
        newNoteButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newNoteButtonMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                newNoteButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                newNoteButtonMouseExited(evt);
            }
        });
        newNoteButton.setLayout(new java.awt.GridBagLayout());

        newNoteButtonIcon.setBackground(java.awt.Color.red);
        newNoteButtonIcon.setForeground(java.awt.Color.white);
        newNoteButtonIcon.setIcon(new FlatSVGIcon("images/icons/add-note-regular.svg"));
        newNoteButtonIcon.setPreferredSize(new java.awt.Dimension(25, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        newNoteButton.add(newNoteButtonIcon, gridBagConstraints);

        newNoteButtonLabel.setFont(new java.awt.Font("Poppins SemiBold", 1, 14)); // NOI18N
        newNoteButtonLabel.setForeground(java.awt.Color.white);
        newNoteButtonLabel.setText("New Note");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        newNoteButton.add(newNoteButtonLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 30, 20);
        leftPanel.add(newNoteButton, gridBagConstraints);
        newNoteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        homeButton.setBackground(new java.awt.Color(230, 230, 230));
        homeButton.setName("active"); // NOI18N
        homeButton.setPreferredSize(new java.awt.Dimension(100, 35));
        homeButton.setRadiusBottomLeft(10);
        homeButton.setRadiusBottomRight(10);
        homeButton.setRadiusTopLeft(10);
        homeButton.setRadiusTopRight(10);
        homeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                homeButtonMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                homeButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                homeButtonMouseExited(evt);
            }
        });
        homeButton.setLayout(new java.awt.GridBagLayout());

        homeButtonIcon.setIcon(new FlatSVGIcon("images/icons/home.svg"));
        homeButtonIcon.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 10);
        homeButton.add(homeButtonIcon, gridBagConstraints);

        homeButtonLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        homeButtonLabel.setForeground(java.awt.Color.black);
        homeButtonLabel.setText("Home");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        homeButton.add(homeButtonLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        leftPanel.add(homeButton, gridBagConstraints);
        homeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        notesButton.setBackground(java.awt.Color.white);
        notesButton.setName(""); // NOI18N
        notesButton.setPreferredSize(new java.awt.Dimension(100, 35));
        notesButton.setRadiusBottomLeft(10);
        notesButton.setRadiusBottomRight(10);
        notesButton.setRadiusTopLeft(10);
        notesButton.setRadiusTopRight(10);
        notesButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                notesButtonMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                notesButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                notesButtonMouseExited(evt);
            }
        });
        notesButton.setLayout(new java.awt.GridBagLayout());

        notesButtonIcon.setIcon(new FlatSVGIcon("images/icons/notes.svg"));
        notesButtonIcon.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 10);
        notesButton.add(notesButtonIcon, gridBagConstraints);

        notesButtonLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        notesButtonLabel.setForeground(java.awt.Color.black);
        notesButtonLabel.setText("Notes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        notesButton.add(notesButtonLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 20, 0, 20);
        leftPanel.add(notesButton, gridBagConstraints);
        notesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        trashButton.setBackground(java.awt.Color.white);
        trashButton.setPreferredSize(new java.awt.Dimension(100, 35));
        trashButton.setRadiusBottomLeft(10);
        trashButton.setRadiusBottomRight(10);
        trashButton.setRadiusTopLeft(10);
        trashButton.setRadiusTopRight(10);
        trashButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                trashButtonMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                trashButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                trashButtonMouseExited(evt);
            }
        });
        trashButton.setLayout(new java.awt.GridBagLayout());

        trashButtonIcon.setIcon(new FlatSVGIcon("images/icons/trash.svg"));
        trashButtonIcon.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 10);
        trashButton.add(trashButtonIcon, gridBagConstraints);

        trashButtonLabel.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        trashButtonLabel.setForeground(java.awt.Color.black);
        trashButtonLabel.setText("Trash");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        trashButton.add(trashButtonLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 20, 0, 20);
        leftPanel.add(trashButton, gridBagConstraints);
        trashButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        logoutButton.setBackground(new java.awt.Color(0, 168, 45));
        logoutButton.setMinimumSize(new java.awt.Dimension(10, 100));
        logoutButton.setPreferredSize(new java.awt.Dimension(100, 50));
        logoutButton.setRadiusBottomLeft(10);
        logoutButton.setRadiusBottomRight(10);
        logoutButton.setRadiusTopLeft(10);
        logoutButton.setRadiusTopRight(10);
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                logoutButtonMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                logoutButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                logoutButtonMouseExited(evt);
            }
        });
        logoutButton.setLayout(new java.awt.GridBagLayout());

        logoutButtonIcon.setIcon(new FlatSVGIcon("images/icons/logout.svg"));
        logoutButtonIcon.setPreferredSize(new java.awt.Dimension(25, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        logoutButton.add(logoutButtonIcon, gridBagConstraints);

        logoutButtonLabel.setFont(new java.awt.Font("Poppins SemiBold", 1, 12)); // NOI18N
        logoutButtonLabel.setForeground(java.awt.Color.white);
        logoutButtonLabel.setText("Sign Out");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        logoutButton.add(logoutButtonLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        leftPanel.add(logoutButton, gridBagConstraints);
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        trademark.setFont(new java.awt.Font("Poppins", 0, 10)); // NOI18N
        trademark.setForeground(java.awt.Color.black);
        trademark.setText("NoteForge™ 2024");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 30, 0);
        leftPanel.add(trademark, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        mainPanel.add(leftPanel, gridBagConstraints);

        rightPanel.setOpaque(false);
        rightPanel.setLayout(new java.awt.CardLayout());

        homePanel.setOpaque(false);
        homePanel.setLayout(new java.awt.GridBagLayout());

        homeTopPanel.setOpaque(false);
        homeTopPanel.setPreferredSize(new java.awt.Dimension(100, 150));
        homeTopPanel.setLayout(new java.awt.GridBagLayout());

        questionLabel.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        questionLabel.setForeground(new java.awt.Color(102, 102, 102));
        questionLabel.setText("Ready to start taking notes?");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 0);
        homeTopPanel.add(questionLabel, gridBagConstraints);

        userHomeLabel.setBackground(java.awt.Color.black);
        userHomeLabel.setFont(new java.awt.Font("Inter", 1, 32)); // NOI18N
        userHomeLabel.setForeground(new java.awt.Color(51, 51, 51));
        userHomeLabel.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 40, 0, 0);
        homeTopPanel.add(userHomeLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        homePanel.add(homeTopPanel, gridBagConstraints);

        homeBottomPanel.setOpaque(false);
        homeBottomPanel.setLayout(new java.awt.GridBagLayout());

        manageNotesDashboardIcon.setOpaque(false);
        manageNotesDashboardIcon.setLayout(new java.awt.GridBagLayout());

        homeBottomUpperPanel.setOpaque(false);
        homeBottomUpperPanel.setPreferredSize(new java.awt.Dimension(100, 50));
        homeBottomUpperPanel.setLayout(new java.awt.GridBagLayout());

        notesTextPanel.setBackground(new java.awt.Color(246, 243, 247));
        notesTextPanel.setRadiusBottomLeft(5);
        notesTextPanel.setRadiusBottomRight(5);
        notesTextPanel.setRadiusTopLeft(5);
        notesTextPanel.setRadiusTopRight(5);
        notesTextPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                notesTextPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                notesTextPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                notesTextPanelMouseExited(evt);
            }
        });
        notesTextPanel.setLayout(new java.awt.GridBagLayout());

        notesText.setFont(new java.awt.Font("Inter SemiBold", 0, 14)); // NOI18N
        notesText.setForeground(java.awt.Color.black);
        notesText.setText("Notes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        notesTextPanel.add(notesText, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 0);
        homeBottomUpperPanel.add(notesTextPanel, gridBagConstraints);
        notesTextPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        newNoteButtonDashboardPanel.setBackground(new java.awt.Color(246, 243, 247));
        newNoteButtonDashboardPanel.setRadiusBottomLeft(10);
        newNoteButtonDashboardPanel.setRadiusBottomRight(10);
        newNoteButtonDashboardPanel.setRadiusTopLeft(10);
        newNoteButtonDashboardPanel.setRadiusTopRight(10);
        newNoteButtonDashboardPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newNoteButtonDashboardPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                newNoteButtonDashboardPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                newNoteButtonDashboardPanelMouseExited(evt);
            }
        });
        newNoteButtonDashboardPanel.setLayout(new java.awt.GridBagLayout());

        newNoteButtonDashboard.setBackground(new java.awt.Color(246, 243, 247));
        newNoteButtonDashboard.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        newNoteButtonDashboard.setIcon(new FlatSVGIcon("images/icons/add-note-regular-small.svg"));
        newNoteButtonDashboard.setToolTipText("New Note");
        newNoteButtonDashboard.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newNoteButtonDashboard.setPreferredSize(new java.awt.Dimension(25, 25));
        newNoteButtonDashboard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                newNoteButtonDashboardMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                newNoteButtonDashboardMouseExited(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        newNoteButtonDashboardPanel.add(newNoteButtonDashboard, gridBagConstraints);
        newNoteButtonDashboardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        homeBottomUpperPanel.add(newNoteButtonDashboardPanel, gridBagConstraints);

        moreOptionsNotesDashboardButtonPanel.setBackground(new java.awt.Color(246, 243, 247));
        moreOptionsNotesDashboardButtonPanel.setRadiusBottomLeft(10);
        moreOptionsNotesDashboardButtonPanel.setRadiusBottomRight(10);
        moreOptionsNotesDashboardButtonPanel.setRadiusTopLeft(10);
        moreOptionsNotesDashboardButtonPanel.setRadiusTopRight(10);
        moreOptionsNotesDashboardButtonPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                moreOptionsNotesDashboardButtonPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                moreOptionsNotesDashboardButtonPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                moreOptionsNotesDashboardButtonPanelMouseExited(evt);
            }
        });
        moreOptionsNotesDashboardButtonPanel.setLayout(new java.awt.GridBagLayout());

        moreOptionsNotesDashboardButton.setBackground(new java.awt.Color(230, 230, 230));
        moreOptionsNotesDashboardButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        moreOptionsNotesDashboardButton.setIcon(new FlatSVGIcon("images/icons/more-horiz.svg"));
        moreOptionsNotesDashboardButton.setPreferredSize(new java.awt.Dimension(25, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        moreOptionsNotesDashboardButtonPanel.add(moreOptionsNotesDashboardButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        homeBottomUpperPanel.add(moreOptionsNotesDashboardButtonPanel, gridBagConstraints);
        moreOptionsNotesDashboardButtonPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        manageNotesDashboardIcon.add(homeBottomUpperPanel, gridBagConstraints);

        actionsBorderPanel.setBackground(new java.awt.Color(51, 51, 51));
        actionsBorderPanel.setRadiusBottomLeft(20);
        actionsBorderPanel.setRadiusBottomRight(20);
        actionsBorderPanel.setRadiusTopLeft(20);
        actionsBorderPanel.setRadiusTopRight(20);
        actionsBorderPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 1, 1));

        actionsPanel.setBackground(new java.awt.Color(246, 243, 247));
        actionsPanel.setRadiusBottomLeft(18);
        actionsPanel.setRadiusBottomRight(18);
        actionsPanel.setRadiusTopLeft(18);
        actionsPanel.setRadiusTopRight(18);
        actionsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 5));

        actionsText.setFont(new java.awt.Font("Inter Medium", 0, 12)); // NOI18N
        actionsText.setForeground(new java.awt.Color(51, 51, 51));
        actionsText.setText("Actions");
        actionsPanel.add(actionsText);

        actionsBorderPanel.add(actionsPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 22, 0);
        manageNotesDashboardIcon.add(actionsBorderPanel, gridBagConstraints);

        notesContainer.setBorder(null);
        notesContainer.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        notesContainer.setOpaque(false);
        notesContainer.setPreferredSize(new java.awt.Dimension(100, 250));

        notesDashboardPanel.setOpaque(false);
        notesDashboardPanel.setLayout(new java.awt.GridBagLayout());

        createNotesContainer.setBackground(java.awt.Color.red);
        createNotesContainer.setOpaque(false);
        createNotesContainer.setLayout(new java.awt.GridBagLayout());

        createNotesPanel.setBackground(java.awt.Color.white);
        createNotesPanel.setPreferredSize(new java.awt.Dimension(175, 100));
        createNotesPanel.setRadiusBottomLeft(10);
        createNotesPanel.setRadiusBottomRight(10);
        createNotesPanel.setRadiusTopLeft(10);
        createNotesPanel.setRadiusTopRight(10);
        createNotesPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                createNotesPanelMouseClicked(evt);
            }
        });
        createNotesPanel.setLayout(new java.awt.GridBagLayout());

        createNoteIconPanel.setBackground(new java.awt.Color(92, 173, 236));
        createNoteIconPanel.setPreferredSize(new java.awt.Dimension(50, 51));
        createNoteIconPanel.setRadiusBottomLeft(50);
        createNoteIconPanel.setRadiusBottomRight(50);
        createNoteIconPanel.setRadiusTopLeft(50);
        createNoteIconPanel.setRadiusTopRight(50);
        createNoteIconPanel.setLayout(new java.awt.GridBagLayout());

        createNoteIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        createNoteIcon.setIcon(new FlatSVGIcon("images/icons/add-note-solid.svg"));
        createNoteIcon.setPreferredSize(new java.awt.Dimension(24, 24));
        createNoteIconPanel.add(createNoteIcon, new java.awt.GridBagConstraints());

        createNotesPanel.add(createNoteIconPanel, new java.awt.GridBagConstraints());

        createNewNoteText.setFont(new java.awt.Font("Inter Medium", 0, 16)); // NOI18N
        createNewNoteText.setText("Create New Note");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        createNotesPanel.add(createNewNoteText, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        createNotesContainer.add(createNotesPanel, gridBagConstraints);
        createNotesPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        viewNotesPanel.setBackground(java.awt.Color.white);
        viewNotesPanel.setPreferredSize(new java.awt.Dimension(175, 100));
        viewNotesPanel.setRadiusBottomLeft(10);
        viewNotesPanel.setRadiusBottomRight(10);
        viewNotesPanel.setRadiusTopLeft(10);
        viewNotesPanel.setRadiusTopRight(10);
        viewNotesPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                viewNotesPanelMouseClicked(evt);
            }
        });
        viewNotesPanel.setLayout(new java.awt.GridBagLayout());

        viewNotesIconPanel.setBackground(new java.awt.Color(252, 177, 0));
        viewNotesIconPanel.setPreferredSize(new java.awt.Dimension(50, 51));
        viewNotesIconPanel.setRadiusBottomLeft(50);
        viewNotesIconPanel.setRadiusBottomRight(50);
        viewNotesIconPanel.setRadiusTopLeft(50);
        viewNotesIconPanel.setRadiusTopRight(50);
        viewNotesIconPanel.setLayout(new java.awt.GridBagLayout());

        viewNotesIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        viewNotesIcon.setIcon(new FlatSVGIcon("images/icons/view-notes.svg"));
        viewNotesIcon.setPreferredSize(new java.awt.Dimension(24, 24));
        viewNotesIconPanel.add(viewNotesIcon, new java.awt.GridBagConstraints());

        viewNotesPanel.add(viewNotesIconPanel, new java.awt.GridBagConstraints());

        viewNotesText.setFont(new java.awt.Font("Inter Medium", 0, 16)); // NOI18N
        viewNotesText.setText("View Notes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        viewNotesPanel.add(viewNotesText, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        createNotesContainer.add(viewNotesPanel, gridBagConstraints);
        viewNotesPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        manageNotesPanelDashboard.setBackground(java.awt.Color.white);
        manageNotesPanelDashboard.setPreferredSize(new java.awt.Dimension(175, 100));
        manageNotesPanelDashboard.setRadiusBottomLeft(10);
        manageNotesPanelDashboard.setRadiusBottomRight(10);
        manageNotesPanelDashboard.setRadiusTopLeft(10);
        manageNotesPanelDashboard.setRadiusTopRight(10);
        manageNotesPanelDashboard.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                manageNotesPanelDashboardMouseClicked(evt);
            }
        });
        manageNotesPanelDashboard.setLayout(new java.awt.GridBagLayout());

        manageNotesDashboardIconPanel.setBackground(new java.awt.Color(0, 168, 45));
        manageNotesDashboardIconPanel.setPreferredSize(new java.awt.Dimension(50, 51));
        manageNotesDashboardIconPanel.setRadiusBottomLeft(50);
        manageNotesDashboardIconPanel.setRadiusBottomRight(50);
        manageNotesDashboardIconPanel.setRadiusTopLeft(50);
        manageNotesDashboardIconPanel.setRadiusTopRight(50);
        manageNotesDashboardIconPanel.setLayout(new java.awt.GridBagLayout());

        manageNotesDashboardBottomIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        manageNotesDashboardBottomIcon.setIcon(new FlatSVGIcon("images/icons/delete-large.svg"));
        manageNotesDashboardBottomIcon.setPreferredSize(new java.awt.Dimension(24, 24));
        manageNotesDashboardIconPanel.add(manageNotesDashboardBottomIcon, new java.awt.GridBagConstraints());

        manageNotesPanelDashboard.add(manageNotesDashboardIconPanel, new java.awt.GridBagConstraints());

        manageNotesDashboardText.setFont(new java.awt.Font("Inter Medium", 0, 16)); // NOI18N
        manageNotesDashboardText.setText("View Trash");
        manageNotesDashboardText.setText("View Trash");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        manageNotesPanelDashboard.add(manageNotesDashboardText, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        createNotesContainer.add(manageNotesPanelDashboard, gridBagConstraints);
        manageNotesPanelDashboard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        notesDashboardPanel.add(createNotesContainer, gridBagConstraints);

        notesContainer.setViewportView(notesDashboardPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 40, 0, 0);
        manageNotesDashboardIcon.add(notesContainer, gridBagConstraints);
        notesContainer.getViewport().setOpaque(false);
        notesContainer.setViewportBorder(null);
        notesContainer.setBackground(new Color(0, 0, 0, 1));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        homeBottomPanel.add(manageNotesDashboardIcon, gridBagConstraints);

        scratchPadPanel.setOpaque(false);
        scratchPadPanel.setPreferredSize(new java.awt.Dimension(350, 100));
        scratchPadPanel.setLayout(new java.awt.GridBagLayout());

        scratchPadTopPanel.setOpaque(false);
        scratchPadTopPanel.setPreferredSize(new java.awt.Dimension(100, 50));
        scratchPadTopPanel.setLayout(new java.awt.GridBagLayout());

        scratchPadText.setFont(new java.awt.Font("Inter SemiBold", 0, 14)); // NOI18N
        scratchPadText.setForeground(java.awt.Color.black);
        scratchPadText.setText("Scratch Pad");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        scratchPadTopPanel.add(scratchPadText, gridBagConstraints);

        scratchPadMoreOptionsPanel.setBackground(new java.awt.Color(246, 243, 247));
        scratchPadMoreOptionsPanel.setRadiusBottomLeft(10);
        scratchPadMoreOptionsPanel.setRadiusBottomRight(10);
        scratchPadMoreOptionsPanel.setRadiusTopLeft(10);
        scratchPadMoreOptionsPanel.setRadiusTopRight(10);
        scratchPadMoreOptionsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                scratchPadMoreOptionsPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                scratchPadMoreOptionsPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                scratchPadMoreOptionsPanelMouseExited(evt);
            }
        });
        scratchPadMoreOptionsPanel.setLayout(new java.awt.GridBagLayout());

        scratchPadMoreOptions.setBackground(new java.awt.Color(246, 243, 247));
        scratchPadMoreOptions.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        scratchPadMoreOptions.setIcon(new FlatSVGIcon("images/icons/more-horiz.svg"));
        scratchPadMoreOptions.setPreferredSize(new java.awt.Dimension(25, 25));
        scratchPadMoreOptionsPanel.add(scratchPadMoreOptions, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 30);
        scratchPadTopPanel.add(scratchPadMoreOptionsPanel, gridBagConstraints);
        scratchPadMoreOptionsPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        scratchPadPanel.add(scratchPadTopPanel, gridBagConstraints);

        scratchPadBottomPanel.setOpaque(false);
        scratchPadBottomPanel.setLayout(new java.awt.GridBagLayout());

        scratchPadTextPanel.setBackground(new java.awt.Color(248, 237, 203));
        scratchPadTextPanel.setPreferredSize(new java.awt.Dimension(300, 300));
        scratchPadTextPanel.setRadiusBottomLeft(10);
        scratchPadTextPanel.setRadiusBottomRight(10);
        scratchPadTextPanel.setRadiusTopLeft(10);
        scratchPadTextPanel.setRadiusTopRight(10);
        scratchPadTextPanel.setLayout(new java.awt.GridBagLayout());

        scratchPadScroll.setBorder(null);
        scratchPadScroll.setOpaque(false);
        scratchPadScroll.setPreferredSize(new java.awt.Dimension(300, 300));

        scratchPadTextArea.setBackground(new java.awt.Color(255, 255, 255));
        scratchPadTextArea.setColumns(20);
        scratchPadTextArea.setFont(new java.awt.Font("Inter", 0, 14)); // NOI18N
        scratchPadTextArea.setForeground(java.awt.Color.black);
        scratchPadTextArea.setRows(5);
        scratchPadTextArea.setMargin(new java.awt.Insets(30, 20, 20, 20));
        scratchPadTextArea.setOpaque(false);
        scratchPadTextArea.setSelectedTextColor(java.awt.Color.white);
        scratchPadTextArea.setSelectionColor(new java.awt.Color(179, 212, 252));
        scratchPadScroll.setViewportView(scratchPadTextArea);
        scratchPadTextArea.setBackground(new Color(0, 0, 0, 0));

        scratchPadTextArea.setLineWrap(true);
        scratchPadTextArea.setWrapStyleWord(true);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        scratchPadTextPanel.add(scratchPadScroll, gridBagConstraints);
        scratchPadScroll.setBackground(new Color(0, 0, 0, 1));

        scratchPadScroll.getViewport().setOpaque(false);
        scratchPadScroll.setViewportBorder(null);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        scratchPadBottomPanel.add(scratchPadTextPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        scratchPadPanel.add(scratchPadBottomPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        homeBottomPanel.add(scratchPadPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        homePanel.add(homeBottomPanel, gridBagConstraints);

        rightPanel.add(homePanel, "home");

        notesPanel.setBackground(java.awt.Color.blue);
        notesPanel.setOpaque(false);
        notesPanel.setLayout(new java.awt.GridBagLayout());

        notesListPanel.setOpaque(false);
        notesListPanel.setPreferredSize(new java.awt.Dimension(375, 110));
        notesListPanel.setLayout(new java.awt.GridBagLayout());

        notesListTopPanel.setOpaque(false);
        notesListTopPanel.setPreferredSize(new java.awt.Dimension(100, 100));
        notesListTopPanel.setLayout(new java.awt.GridBagLayout());

        notesTextList.setFont(new java.awt.Font("Inter", 1, 24)); // NOI18N
        notesTextList.setForeground(java.awt.Color.black);
        notesTextList.setText("Notes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        notesListTopPanel.add(notesTextList, gridBagConstraints);

        noteListOptions.setOpaque(false);
        noteListOptions.setLayout(new java.awt.GridBagLayout());

        notesCounter.setFont(new java.awt.Font("Inter", 0, 14)); // NOI18N
        notesCounter.setForeground(new java.awt.Color(153, 153, 153));
        notesCounter.setText("0 notes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        noteListOptions.add(notesCounter, gridBagConstraints);

        sortButtonPanel.setBackground(new java.awt.Color(246, 243, 247));
        sortButtonPanel.setRadiusBottomLeft(10);
        sortButtonPanel.setRadiusBottomRight(10);
        sortButtonPanel.setRadiusTopLeft(10);
        sortButtonPanel.setRadiusTopRight(10);
        sortButtonPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                sortButtonPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sortButtonPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sortButtonPanelMouseExited(evt);
            }
        });

        sortButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        sortButton.setIcon(new FlatSVGIcon("images/icons/sort.svg"));
        sortButton.setPreferredSize(new java.awt.Dimension(20, 20));
        sortButtonPanel.add(sortButton);

        noteListOptions.add(sortButtonPanel, new java.awt.GridBagConstraints());

        exportButtonPanel.setBackground(new java.awt.Color(246, 243, 247));
        exportButtonPanel.setRadiusBottomLeft(10);
        exportButtonPanel.setRadiusBottomRight(10);
        exportButtonPanel.setRadiusTopLeft(10);
        exportButtonPanel.setRadiusTopRight(10);
        exportButtonPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                exportButtonPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportButtonPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportButtonPanelMouseExited(evt);
            }
        });

        exportButton.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        exportButton.setIcon(new FlatSVGIcon("images/icons/ios-share.svg"));
        exportButton.setPreferredSize(new java.awt.Dimension(20, 20));
        exportButtonPanel.add(exportButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        noteListOptions.add(exportButtonPanel, gridBagConstraints);
        exportButtonPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        notesListTopPanel.add(noteListOptions, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 30, 0, 20);
        notesListPanel.add(notesListTopPanel, gridBagConstraints);

        notesListMainPanelScroll.setBorder(null);
        notesListMainPanelScroll.setOpaque(false);

        notesListMainPanel.setBackground(java.awt.Color.orange);
        notesListMainPanel.setOpaque(false);
        notesListMainPanel.setLayout(new java.awt.GridBagLayout());

        notesListSubPanel.setOpaque(false);
        notesListSubPanel.setLayout(new java.awt.GridBagLayout());

        notesListScrollPane.setBorder(null);

        notesListScrollPane.setViewportView(notesList);
        notesList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        notesListSubPanel.add(notesListScrollPane, gridBagConstraints);

        paddingPanel.setOpaque(false);
        paddingPanel.setPreferredSize(new java.awt.Dimension(100, 60));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        notesListSubPanel.add(paddingPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        notesListMainPanel.add(notesListSubPanel, gridBagConstraints);

        notesListMainPanelScroll.setViewportView(notesListMainPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        notesListPanel.add(notesListMainPanelScroll, gridBagConstraints);
        notesListMainPanelScroll.setBackground(new Color(0, 0, 0, 1));
        notesListMainPanelScroll.getViewport().setOpaque(false);
        notesListMainPanelScroll.setViewportBorder(null);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        notesPanel.add(notesListPanel, gridBagConstraints);

        notesCreationPanel.setBackground(java.awt.Color.white);
        notesCreationPanel.setPreferredSize(new java.awt.Dimension(250, 349));
        notesCreationPanel.setRadiusBottomLeft(10);
        notesCreationPanel.setRadiusBottomRight(10);
        notesCreationPanel.setRadiusTopLeft(10);
        notesCreationPanel.setRadiusTopRight(10);
        notesCreationPanel.setLayout(new java.awt.GridBagLayout());

        notesCreationPanelTop.setToolTipText("");
        notesCreationPanelTop.setMinimumSize(new java.awt.Dimension(65, 60));
        notesCreationPanelTop.setOpaque(false);
        notesCreationPanelTop.setPreferredSize(new java.awt.Dimension(100, 60));
        notesCreationPanelTop.setLayout(new java.awt.GridBagLayout());

        expandToggleButtonPanel.setBackground(java.awt.Color.white);
        expandToggleButtonPanel.setRadiusBottomLeft(10);
        expandToggleButtonPanel.setRadiusBottomRight(10);
        expandToggleButtonPanel.setRadiusTopLeft(10);
        expandToggleButtonPanel.setRadiusTopRight(10);
        expandToggleButtonPanel.setLayout(new java.awt.GridBagLayout());

        expandToggleButton.setBackground(java.awt.Color.white);
        expandToggleButton.setIcon(new FlatSVGIcon("images/icons/pencil.svg"));
        expandToggleButton.setPreferredSize(new java.awt.Dimension(20, 20));
        expandToggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                expandToggleButtonMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        expandToggleButtonPanel.add(expandToggleButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        notesCreationPanelTop.add(expandToggleButtonPanel, gridBagConstraints);

        verticalLine.setFont(new java.awt.Font("Inter", 0, 14)); // NOI18N
        verticalLine.setForeground(new java.awt.Color(153, 153, 153));
        verticalLine.setText("|");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        notesCreationPanelTop.add(verticalLine, gridBagConstraints);

        lastEditedText.setFont(new java.awt.Font("Inter Medium", 0, 14)); // NOI18N
        lastEditedText.setForeground(new java.awt.Color(102, 102, 102));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        notesCreationPanelTop.add(lastEditedText, gridBagConstraints);

        deleteCurrentNoteButtonPanel.setBackground(java.awt.Color.white);
        deleteCurrentNoteButtonPanel.setRadiusBottomLeft(10);
        deleteCurrentNoteButtonPanel.setRadiusBottomRight(10);
        deleteCurrentNoteButtonPanel.setRadiusTopLeft(10);
        deleteCurrentNoteButtonPanel.setRadiusTopRight(10);
        deleteCurrentNoteButtonPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                deleteCurrentNoteButtonPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deleteCurrentNoteButtonPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deleteCurrentNoteButtonPanelMouseExited(evt);
            }
        });

        deleteCurrentNoteButton.setIcon(new FlatSVGIcon("images/icons/delete-note.svg"));
        deleteCurrentNoteButton.setPreferredSize(new java.awt.Dimension(20, 20));
        deleteCurrentNoteButtonPanel.add(deleteCurrentNoteButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        notesCreationPanelTop.add(deleteCurrentNoteButtonPanel, gridBagConstraints);
        deleteCurrentNoteButtonPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        notesCreationPanel.add(notesCreationPanelTop, gridBagConstraints);

        notesCreationPanelBottom.setOpaque(false);
        notesCreationPanelBottom.setLayout(new java.awt.BorderLayout());

        notesCreationMainPanel.setBackground(java.awt.Color.white);
        notesCreationMainPanel.setBorder(null);
        notesCreationMainPanel.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        notesCreationMainPanel.setOpaque(false);
        notesCreationMainPanel.setViewportView(null);

        notesCreationSubPanel.setOpaque(false);
        notesCreationSubPanel.setLayout(new java.awt.GridBagLayout());

        notesCreationTitlePanel.setOpaque(false);
        notesCreationTitlePanel.setLayout(new java.awt.BorderLayout());

        noteTitleTextArea.setColumns(1);
        noteTitleTextArea.setFont(new java.awt.Font("Inter SemiBold", 0, 32)); // NOI18N
        noteTitleTextArea.setForeground(new java.awt.Color(33, 33, 33));
        noteTitleTextArea.setRows(1);
        noteTitleTextArea.setBorder(null);
        noteTitleTextArea.setMargin(new java.awt.Insets(0, 0, 0, 0));
        noteTitleTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                noteTitleTextAreaKeyPressed(evt);
            }
        });
        notesCreationTitlePanel.add(noteTitleTextArea, java.awt.BorderLayout.CENTER);
        noteTitleTextArea.setLineWrap(true);
        noteTitleTextArea.setWrapStyleWord(true);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(80, 70, 0, 70);
        notesCreationSubPanel.add(notesCreationTitlePanel, gridBagConstraints);

        notesCreationContentPanel.setBackground(java.awt.Color.red);
        notesCreationContentPanel.setOpaque(false);
        notesCreationContentPanel.setLayout(new java.awt.BorderLayout());

        noteCreationTextArea.setColumns(1);
        noteCreationTextArea.setFont(new java.awt.Font("Inter", 0, 16)); // NOI18N
        noteCreationTextArea.setRows(1);
        noteCreationTextArea.setBorder(null);
        noteCreationTextArea.setMargin(new java.awt.Insets(0, 0, 0, 0));
        notesCreationContentPanel.add(noteCreationTextArea, java.awt.BorderLayout.CENTER);
        noteCreationTextArea.setLineWrap(true);
        noteCreationTextArea.setWrapStyleWord(true);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 70, 80, 70);
        notesCreationSubPanel.add(notesCreationContentPanel, gridBagConstraints);

        notesCreationMainPanel.setViewportView(notesCreationSubPanel);

        notesCreationPanelBottom.add(notesCreationMainPanel, java.awt.BorderLayout.CENTER);
        notesCreationMainPanel.setBackground(new Color(0, 0, 0, 1));
        notesCreationMainPanel.getViewport().setOpaque(false);
        notesCreationMainPanel.setViewportBorder(null);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        notesCreationPanel.add(notesCreationPanelBottom, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        notesPanel.add(notesCreationPanel, gridBagConstraints);

        rightPanel.add(notesPanel, "notes");

        trashPanel.setBackground(java.awt.Color.red);
        trashPanel.setOpaque(false);
        trashPanel.setLayout(new java.awt.GridBagLayout());

        trashNotesListPanel.setMinimumSize(new java.awt.Dimension(140, 83));
        trashNotesListPanel.setOpaque(false);
        trashNotesListPanel.setPreferredSize(new java.awt.Dimension(375, 110));
        trashNotesListPanel.setLayout(new java.awt.GridBagLayout());

        trashNotesListTopPanel.setOpaque(false);
        trashNotesListTopPanel.setPreferredSize(new java.awt.Dimension(100, 100));
        trashNotesListTopPanel.setLayout(new java.awt.GridBagLayout());

        trashNotesTextList.setFont(new java.awt.Font("Inter", 1, 24)); // NOI18N
        trashNotesTextList.setForeground(java.awt.Color.black);
        trashNotesTextList.setText("Trash");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        trashNotesListTopPanel.add(trashNotesTextList, gridBagConstraints);

        trashNoteListOptions.setOpaque(false);
        trashNoteListOptions.setLayout(new java.awt.GridBagLayout());

        trashNotesCounter.setFont(new java.awt.Font("Inter", 0, 14)); // NOI18N
        trashNotesCounter.setForeground(new java.awt.Color(153, 153, 153));
        trashNotesCounter.setText("0 notes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        trashNoteListOptions.add(trashNotesCounter, gridBagConstraints);

        sortButtonPanel1.setBackground(new java.awt.Color(246, 243, 247));
        sortButtonPanel1.setRadiusBottomLeft(10);
        sortButtonPanel1.setRadiusBottomRight(10);
        sortButtonPanel1.setRadiusTopLeft(10);
        sortButtonPanel1.setRadiusTopRight(10);
        sortButtonPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sortButtonPanel1MouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sortButtonPanel1MouseExited(evt);
            }
        });

        sortButton1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        sortButton1.setIcon(new FlatSVGIcon("images/icons/sort.svg"));
        sortButton1.setPreferredSize(new java.awt.Dimension(20, 20));
        sortButtonPanel1.add(sortButton1);

        trashNoteListOptions.add(sortButtonPanel1, new java.awt.GridBagConstraints());
        sortButtonPanel1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        trashNotesListTopPanel.add(trashNoteListOptions, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 30, 0, 20);
        trashNotesListPanel.add(trashNotesListTopPanel, gridBagConstraints);

        trashNotesListMainPanelScroll.setBorder(null);
        trashNotesListMainPanelScroll.setOpaque(false);

        trashNotesListMainPanel.setBackground(java.awt.Color.orange);
        trashNotesListMainPanel.setOpaque(false);
        trashNotesListMainPanel.setLayout(new java.awt.GridBagLayout());

        trashNotesListSubPanel.setOpaque(false);
        trashNotesListSubPanel.setLayout(new java.awt.GridBagLayout());

        trashNotesListScrollPane1.setBorder(null);

        trashNotesListScrollPane1.setViewportView(trashNotesList);
        trashNotesList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        trashNotesListSubPanel.add(trashNotesListScrollPane1, gridBagConstraints);

        paddingPanel1.setOpaque(false);
        paddingPanel1.setPreferredSize(new java.awt.Dimension(100, 60));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        trashNotesListSubPanel.add(paddingPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        trashNotesListMainPanel.add(trashNotesListSubPanel, gridBagConstraints);

        trashNotesListMainPanelScroll.setViewportView(trashNotesListMainPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        trashNotesListPanel.add(trashNotesListMainPanelScroll, gridBagConstraints);
        trashNotesListMainPanelScroll.setBackground(new Color(0, 0, 0, 1));
        trashNotesListMainPanelScroll.getViewport().setOpaque(false);
        trashNotesListMainPanelScroll.setViewportBorder(null);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        trashPanel.add(trashNotesListPanel, gridBagConstraints);

        trashNotesPreviewPanel.setBackground(java.awt.Color.white);
        trashNotesPreviewPanel.setMinimumSize(new java.awt.Dimension(65, 75));
        trashNotesPreviewPanel.setPreferredSize(new java.awt.Dimension(250, 349));
        trashNotesPreviewPanel.setRadiusBottomLeft(10);
        trashNotesPreviewPanel.setRadiusBottomRight(10);
        trashNotesPreviewPanel.setRadiusTopLeft(10);
        trashNotesPreviewPanel.setRadiusTopRight(10);
        trashNotesPreviewPanel.setLayout(new java.awt.GridBagLayout());

        trashNotesCreationPanelTop.setMinimumSize(new java.awt.Dimension(380, 60));
        trashNotesCreationPanelTop.setOpaque(false);
        trashNotesCreationPanelTop.setPreferredSize(new java.awt.Dimension(100, 60));
        trashNotesCreationPanelTop.setLayout(new java.awt.GridBagLayout());

        trashExpandToggleButtonPanel.setBackground(java.awt.Color.white);
        trashExpandToggleButtonPanel.setRadiusBottomLeft(10);
        trashExpandToggleButtonPanel.setRadiusBottomRight(10);
        trashExpandToggleButtonPanel.setRadiusTopLeft(10);
        trashExpandToggleButtonPanel.setRadiusTopRight(10);
        trashExpandToggleButtonPanel.setLayout(new java.awt.GridBagLayout());

        expandToggleButton1.setBackground(java.awt.Color.white);
        expandToggleButton1.setIcon(new FlatSVGIcon("images/icons/pencil.svg"));
        expandToggleButton1.setPreferredSize(new java.awt.Dimension(20, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        trashExpandToggleButtonPanel.add(expandToggleButton1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        trashNotesCreationPanelTop.add(trashExpandToggleButtonPanel, gridBagConstraints);

        trashVerticalLine.setFont(new java.awt.Font("Inter", 0, 14)); // NOI18N
        trashVerticalLine.setForeground(new java.awt.Color(153, 153, 153));
        trashVerticalLine.setText("|");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        trashNotesCreationPanelTop.add(trashVerticalLine, gridBagConstraints);

        noteInTrashNoticePanel.setBackground(new java.awt.Color(230, 230, 230));
        noteInTrashNoticePanel.setRadiusBottomLeft(15);
        noteInTrashNoticePanel.setRadiusBottomRight(15);
        noteInTrashNoticePanel.setRadiusTopLeft(15);
        noteInTrashNoticePanel.setRadiusTopRight(15);
        noteInTrashNoticePanel.setLayout(new java.awt.GridBagLayout());

        noteInTrashNotice.setFont(new java.awt.Font("Inter", 0, 14)); // NOI18N
        noteInTrashNotice.setForeground(java.awt.Color.black);
        noteInTrashNotice.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        noteInTrashNotice.setIcon(new FlatSVGIcon("images/icons/delete-note-light.svg"));
        noteInTrashNotice.setText("Note in Trash");
        noteInTrashNotice.setIconTextGap(5);
        noteInTrashNotice.setName(""); // NOI18N
        noteInTrashNotice.setPreferredSize(new java.awt.Dimension(120, 20));
        noteInTrashNoticePanel.add(noteInTrashNotice, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.ipadx = 7;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        trashNotesCreationPanelTop.add(noteInTrashNoticePanel, gridBagConstraints);

        expirationNotice.setFont(new java.awt.Font("Inter", 0, 14)); // NOI18N
        expirationNotice.setForeground(new java.awt.Color(255, 0, 0));
        expirationNotice.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        expirationNotice.setText("Expires in 30 days");
        expirationNotice.setIconTextGap(5);
        expirationNotice.setName(""); // NOI18N
        expirationNotice.setPreferredSize(new java.awt.Dimension(150, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 20);
        trashNotesCreationPanelTop.add(expirationNotice, gridBagConstraints);

        trashLastEditedText.setFont(new java.awt.Font("Inter Medium", 0, 14)); // NOI18N
        trashLastEditedText.setForeground(new java.awt.Color(102, 102, 102));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        trashNotesCreationPanelTop.add(trashLastEditedText, gridBagConstraints);

        restoreCurrentNoteButtonPanel.setBackground(java.awt.Color.white);
        restoreCurrentNoteButtonPanel.setRadiusBottomLeft(10);
        restoreCurrentNoteButtonPanel.setRadiusBottomRight(10);
        restoreCurrentNoteButtonPanel.setRadiusTopLeft(10);
        restoreCurrentNoteButtonPanel.setRadiusTopRight(10);
        restoreCurrentNoteButtonPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                restoreCurrentNoteButtonPanelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                restoreCurrentNoteButtonPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                restoreCurrentNoteButtonPanelMouseExited(evt);
            }
        });

        restoreCurrentNoteButton.setIcon(new FlatSVGIcon("images/icons/restore-from-trash.svg"));
        restoreCurrentNoteButton.setPreferredSize(new java.awt.Dimension(20, 20));
        restoreCurrentNoteButtonPanel.add(restoreCurrentNoteButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        trashNotesCreationPanelTop.add(restoreCurrentNoteButtonPanel, gridBagConstraints);
        restoreCurrentNoteButtonPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        deleteCurrentNoteForever.setBackground(java.awt.Color.white);
        deleteCurrentNoteForever.setRadiusBottomLeft(10);
        deleteCurrentNoteForever.setRadiusBottomRight(10);
        deleteCurrentNoteForever.setRadiusTopLeft(10);
        deleteCurrentNoteForever.setRadiusTopRight(10);
        deleteCurrentNoteForever.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                deleteCurrentNoteForeverMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                deleteCurrentNoteForeverMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                deleteCurrentNoteForeverMouseExited(evt);
            }
        });

        deleteCurrentNote.setIcon(new FlatSVGIcon("images/icons/delete-forever.svg"));
        deleteCurrentNote.setPreferredSize(new java.awt.Dimension(20, 20));
        deleteCurrentNoteForever.add(deleteCurrentNote);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        trashNotesCreationPanelTop.add(deleteCurrentNoteForever, gridBagConstraints);
        deleteCurrentNoteForever.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        trashNotesPreviewPanel.add(trashNotesCreationPanelTop, gridBagConstraints);

        trashedNotePanelBottom.setOpaque(false);
        trashedNotePanelBottom.setLayout(new java.awt.GridBagLayout());

        trashedNoteMainPanel.setBackground(java.awt.Color.white);
        trashedNoteMainPanel.setBorder(null);
        trashedNoteMainPanel.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        trashedNoteMainPanel.setOpaque(false);

        trashedNoteSubPanel.setOpaque(false);
        trashedNoteSubPanel.setLayout(new java.awt.GridBagLayout());

        trashedNoteTitlePanel.setOpaque(false);
        trashedNoteTitlePanel.setLayout(new java.awt.GridBagLayout());

        trashedNoteTitleTextArea.setEditable(false);
        trashedNoteTitleTextArea.setColumns(1);
        trashedNoteTitleTextArea.setFont(new java.awt.Font("Inter SemiBold", 0, 32)); // NOI18N
        trashedNoteTitleTextArea.setForeground(new java.awt.Color(33, 33, 33));
        trashedNoteTitleTextArea.setLineWrap(true);
        trashedNoteTitleTextArea.setRows(1);
        trashedNoteTitleTextArea.setWrapStyleWord(true);
        trashedNoteTitleTextArea.setBorder(null);
        trashedNoteTitleTextArea.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        trashedNoteTitlePanel.add(trashedNoteTitleTextArea, gridBagConstraints);
        noteTitleTextArea.setLineWrap(true);
        noteTitleTextArea.setWrapStyleWord(true);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(80, 70, 0, 70);
        trashedNoteSubPanel.add(trashedNoteTitlePanel, gridBagConstraints);

        trashedNoteContentPanel.setBackground(java.awt.Color.red);
        trashedNoteContentPanel.setOpaque(false);
        trashedNoteContentPanel.setLayout(new java.awt.GridBagLayout());

        trashedNoteTextArea.setEditable(false);
        trashedNoteTextArea.setColumns(1);
        trashedNoteTextArea.setFont(new java.awt.Font("Inter", 0, 16)); // NOI18N
        trashedNoteTextArea.setLineWrap(true);
        trashedNoteTextArea.setRows(1);
        trashedNoteTextArea.setWrapStyleWord(true);
        trashedNoteTextArea.setBorder(null);
        trashedNoteTextArea.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        trashedNoteContentPanel.add(trashedNoteTextArea, gridBagConstraints);
        noteCreationTextArea.setLineWrap(true);
        noteCreationTextArea.setWrapStyleWord(true);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 70, 80, 70);
        trashedNoteSubPanel.add(trashedNoteContentPanel, gridBagConstraints);

        trashedNoteMainPanel.setViewportView(trashedNoteSubPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        trashedNotePanelBottom.add(trashedNoteMainPanel, gridBagConstraints);
        trashedNoteMainPanel.setBackground(new Color(0, 0, 0, 1));
        trashedNoteMainPanel.getViewport().setOpaque(false);
        trashedNoteMainPanel.setViewportBorder(null);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        trashNotesPreviewPanel.add(trashedNotePanelBottom, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        trashPanel.add(trashNotesPreviewPanel, gridBagConstraints);

        rightPanel.add(trashPanel, "trash");

        settingsPanel.setBackground(java.awt.Color.yellow);
        rightPanel.add(settingsPanel, "settings");
        rightPanel.add(manageNotesPanel, "manageNotes");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        mainPanel.add(rightPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(mainPanel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void homeButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_homeButtonMouseEntered
        // TODO add your handling code here:
        if (!homeButton.getName().equals("active")) {
            updateColorWhenHovered(homeButton, new Color(230, 230, 230));
        }
    }//GEN-LAST:event_homeButtonMouseEntered

    private void homeButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_homeButtonMouseExited
        // TODO add your handling code here:
        if (!homeButton.getName().equals("active")) {
            updateColorWhenHovered(homeButton, Color.WHITE);
        }
    }//GEN-LAST:event_homeButtonMouseExited

    private void notesButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesButtonMouseEntered
        // TODO add your handling code here:
        if (!notesButton.getName().equals("active")) {
            updateColorWhenHovered(notesButton, new Color(230, 230, 230));
        }
    }//GEN-LAST:event_notesButtonMouseEntered

    private void notesButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesButtonMouseExited
        // TODO add your handling code here:
        if (!notesButton.getName().equals("active")) {
            updateColorWhenHovered(notesButton, Color.WHITE);
        }
    }//GEN-LAST:event_notesButtonMouseExited

    private void trashButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_trashButtonMouseEntered
        // TODO add your handling code here:
        if (!trashButton.getName().equals("active")) {
            updateColorWhenHovered(trashButton, new Color(230, 230, 230));
        }
    }//GEN-LAST:event_trashButtonMouseEntered

    private void trashButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_trashButtonMouseExited
        // TODO add your handling code here:
        if (!trashButton.getName().equals("active")) {
            updateColorWhenHovered(trashButton, Color.WHITE);
        }
    }//GEN-LAST:event_trashButtonMouseExited

    private void newNoteButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonMouseEntered
        // TODO add your handling code here:
        updateColorWhenHovered(newNoteButton, new Color(72, 153, 216));
    }//GEN-LAST:event_newNoteButtonMouseEntered

    private void newNoteButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonMouseExited
        // TODO add your handling code here:
        updateColorWhenHovered(newNoteButton, new Color(92, 173, 236));
    }//GEN-LAST:event_newNoteButtonMouseExited

    private void logoutButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_logoutButtonMouseEntered
        // TODO add your handling code here:
        updateColorWhenHovered(logoutButton, new Color(0, 148, 25));
    }//GEN-LAST:event_logoutButtonMouseEntered

    private void logoutButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_logoutButtonMouseExited
        // TODO add your handling code here:
        updateColorWhenHovered(logoutButton, new Color(0, 168, 45));
    }//GEN-LAST:event_logoutButtonMouseExited

    private void homeButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_homeButtonMouseClicked
        // TODO add your handling code here:
        changeButtonState(homeButton, notesButton, trashButton);
        cards.show(rightPanel, "home");
    }//GEN-LAST:event_homeButtonMouseClicked

    private void notesButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesButtonMouseClicked
        // TODO add your handling code here:
        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");
    }//GEN-LAST:event_notesButtonMouseClicked

    private void trashButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_trashButtonMouseClicked
        // TODO add your handling code here:
        changeButtonState(trashButton, homeButton, notesButton);
        cards.show(rightPanel, "trash");
    }//GEN-LAST:event_trashButtonMouseClicked

    private void newNoteButtonDashboardPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonDashboardPanelMouseEntered
        // TODO add your handling code here:
        updateColorWhenHovered(newNoteButtonDashboardPanel, new Color(230, 230, 230));
    }//GEN-LAST:event_newNoteButtonDashboardPanelMouseEntered

    private void newNoteButtonDashboardPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonDashboardPanelMouseExited
        // TODO add your handling code here:
        updateColorWhenHovered(newNoteButtonDashboardPanel, new Color(246, 243, 247));
    }//GEN-LAST:event_newNoteButtonDashboardPanelMouseExited

    private void moreOptionsNotesDashboardButtonPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_moreOptionsNotesDashboardButtonPanelMouseEntered
        // TODO add your handling code here:
        updateColorWhenHovered(moreOptionsNotesDashboardButtonPanel, new Color(230, 230, 230));
    }//GEN-LAST:event_moreOptionsNotesDashboardButtonPanelMouseEntered

    private void moreOptionsNotesDashboardButtonPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_moreOptionsNotesDashboardButtonPanelMouseExited
        // TODO add your handling code here:
        updateColorWhenHovered(moreOptionsNotesDashboardButtonPanel, new Color(246, 243, 247));
    }//GEN-LAST:event_moreOptionsNotesDashboardButtonPanelMouseExited

    private void scratchPadMoreOptionsPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scratchPadMoreOptionsPanelMouseEntered
        // TODO add your handling code here:
        updateColorWhenHovered(scratchPadMoreOptionsPanel, new Color(230, 230, 230));
    }//GEN-LAST:event_scratchPadMoreOptionsPanelMouseEntered

    private void scratchPadMoreOptionsPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scratchPadMoreOptionsPanelMouseExited
        // TODO add your handling code here:
        updateColorWhenHovered(scratchPadMoreOptionsPanel, new Color(246, 243, 247));
    }//GEN-LAST:event_scratchPadMoreOptionsPanelMouseExited

    private void newNoteButtonDashboardMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonDashboardMouseEntered
        // TODO add your handling code here:
        updateColorWhenHovered(newNoteButtonDashboardPanel, new Color(230, 230, 230));
    }//GEN-LAST:event_newNoteButtonDashboardMouseEntered

    private void newNoteButtonDashboardMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonDashboardMouseExited
        // TODO add your handling code here:
        updateColorWhenHovered(newNoteButtonDashboardPanel, new Color(246, 243, 247));
    }//GEN-LAST:event_newNoteButtonDashboardMouseExited

    private void notesTextPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesTextPanelMouseEntered
        // TODO add your handling code here:
        updateColorWhenHovered(notesTextPanel, new Color(230, 230, 230));
    }//GEN-LAST:event_notesTextPanelMouseEntered

    private void notesTextPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesTextPanelMouseExited
        // TODO add your handling code here:
        updateColorWhenHovered(notesTextPanel, new Color(246, 243, 247));
    }//GEN-LAST:event_notesTextPanelMouseExited

    private void notesTextPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_notesTextPanelMouseClicked
        // TODO add your handling code here:
        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");
    }//GEN-LAST:event_notesTextPanelMouseClicked

    private void moreOptionsNotesDashboardButtonPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_moreOptionsNotesDashboardButtonPanelMouseClicked
        // TODO add your handling code here:
        moreOptions.show(moreOptionsNotesDashboardButtonPanel, 0, moreOptionsNotesDashboardButtonPanel.getHeight());
    }//GEN-LAST:event_moreOptionsNotesDashboardButtonPanelMouseClicked

    private void option1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_option1ActionPerformed
        // TODO add your handling code here:
        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");
    }//GEN-LAST:event_option1ActionPerformed

    private void option2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_option2ActionPerformed
        // TODO add your handling code here:

        if (isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(false);
            notesCreationPanelBottom.setVisible(false);
        }

        handleAddNote();
        setTextCounter();

        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");

        if (!isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(true);
            notesCreationPanelBottom.setVisible(true);
        }
    }//GEN-LAST:event_option2ActionPerformed

    private void logoutButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_logoutButtonMouseClicked
        // TODO add your handling code here:
        int response = JOptionPane.showConfirmDialog(
                null,
                "Are you sure you want to log out?",
                "Log Out",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            session.logout();
            new Login().setVisible(true);
            this.dispose();
        }

    }//GEN-LAST:event_logoutButtonMouseClicked

    private void scratchOption1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scratchOption1ActionPerformed
        // TODO add your handling code here:
        addNoteToDatabaseFromScratchPad();
        getAllNoteIdsForCurrentUser();
        addNotesToList();
        setTextCounter();

        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");

    }//GEN-LAST:event_scratchOption1ActionPerformed

    private void scratchPadMoreOptionsPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scratchPadMoreOptionsPanelMouseClicked
        // TODO add your handling code here:
        scratchPadOptions.show(scratchPadMoreOptionsPanel, 0, scratchPadMoreOptionsPanel.getHeight());
    }//GEN-LAST:event_scratchPadMoreOptionsPanelMouseClicked

    private void scratchOption2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scratchOption2ActionPerformed
        // TODO add your handling code here:
        scratchPadTextArea.setText("");
    }//GEN-LAST:event_scratchOption2ActionPerformed

    private void newNoteButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonMouseClicked

        if (isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(false);
            notesCreationPanelBottom.setVisible(false);
        }

        handleAddNote();
        setTextCounter();

        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");

        if (!isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(true);
            notesCreationPanelBottom.setVisible(true);
        }

    }//GEN-LAST:event_newNoteButtonMouseClicked

    private void expandToggleButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_expandToggleButtonMouseClicked
        // TODO add your handling code here:

//        isExpanded = !isExpanded;
//        if (isExpanded) {
//            notesListPanel.setVisible(false);
//            expandToggleButton.setIcon(new FlatSVGIcon("images/icons/collapse.svg"));
//        } else {
//            notesListPanel.setVisible(true);
//            expandToggleButton.setIcon(new FlatSVGIcon("images/icons/expand.svg"));
//        } 
    }//GEN-LAST:event_expandToggleButtonMouseClicked

    private void noteTitleTextAreaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_noteTitleTextAreaKeyPressed
        // TODO add your handling code here:

        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.consume();

            SwingUtilities.invokeLater(() -> {
                noteTitleTextArea.transferFocus();
                noteCreationTextArea.requestFocus();
                noteCreationTextArea.setCaretPosition(0);
            });
        }

    }//GEN-LAST:event_noteTitleTextAreaKeyPressed

    private void deleteCurrentNoteButtonPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteCurrentNoteButtonPanelMouseClicked
        // TODO add your handling code here:
        int response = JOptionPane.showConfirmDialog(
                null,
                "Are you sure you want to move this note to trash?",
                "Confirmation",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {

            if (isTrashListEmpty()) {
                trashedNoteTitleTextArea.setVisible(false);
                trashedNoteTextArea.setVisible(false);
            }

            handleTrashNote();
            changeButtonState(notesButton, homeButton, trashButton);
            cards.show(rightPanel, "notes");

            setTextCounter();
            setTrashTextCounter();
            notesList.setSelectedIndex(0);

        }

    }//GEN-LAST:event_deleteCurrentNoteButtonPanelMouseClicked

    private void restoreCurrentNoteButtonPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_restoreCurrentNoteButtonPanelMouseClicked
        // TODO add your handling code here:

        int response = JOptionPane.showConfirmDialog(
                null,
                "Restore note?",
                "Confirmation",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {

            if (!isTrashListEmpty()) {
                trashNotesCreationPanelTop.setVisible(true);
                trashedNotePanelBottom.setVisible(true);
            }

            handleRestoredNote();
            changeButtonState(notesButton, homeButton, trashButton);
            cards.show(rightPanel, "notes");

            setTextCounter();
            setTrashTextCounter();
            notesList.setSelectedIndex(0);

            if (isTrashListEmpty()) {
                trashNotesCreationPanelTop.setVisible(false);
                trashedNotePanelBottom.setVisible(false);
            }
        }

    }//GEN-LAST:event_restoreCurrentNoteButtonPanelMouseClicked

    private void createNotesPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_createNotesPanelMouseClicked
        // TODO add your handling code here:
        if (isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(false);
            notesCreationPanelBottom.setVisible(false);
        }

        handleAddNote();
        setTextCounter();

        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");

        if (!isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(true);
            notesCreationPanelBottom.setVisible(true);
        }
    }//GEN-LAST:event_createNotesPanelMouseClicked

    private void viewNotesPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_viewNotesPanelMouseClicked
        // TODO add your handling code here:
        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");
    }//GEN-LAST:event_viewNotesPanelMouseClicked

    private void manageNotesPanelDashboardMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_manageNotesPanelDashboardMouseClicked
        // TODO add your handling code here:
        changeButtonState(trashButton, notesButton, homeButton);
        cards.show(rightPanel, "trash");
    }//GEN-LAST:event_manageNotesPanelDashboardMouseClicked

    private void newNoteButtonDashboardPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newNoteButtonDashboardPanelMouseClicked
        // TODO add your handling code here:
        if (isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(false);
            notesCreationPanelBottom.setVisible(false);
        }

        handleAddNote();
        setTextCounter();

        changeButtonState(notesButton, homeButton, trashButton);
        cards.show(rightPanel, "notes");

        if (!isNoteListEmpty()) {
            notesCreationPanelTop.setVisible(true);
            notesCreationPanelBottom.setVisible(true);
        }
    }//GEN-LAST:event_newNoteButtonDashboardPanelMouseClicked

    private void deleteCurrentNoteForeverMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteCurrentNoteForeverMouseClicked
        // TODO add your handling code here:
        int response = JOptionPane.showConfirmDialog(
                null,
                "Are you sure you want to delete this note permanently? This can't be undone.",
                "Delete Note Permanently",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            deleteNoteForever();
            setTrashTextCounter();
            trashNotesList.setSelectedIndex(0);
        }

        if (!isTrashListEmpty()) {
            trashNotesCreationPanelTop.setVisible(true);
            trashedNotePanelBottom.setVisible(true);
        }

        if (isTrashListEmpty()) {
            trashNotesCreationPanelTop.setVisible(false);
            trashedNotePanelBottom.setVisible(false);
        }
    }//GEN-LAST:event_deleteCurrentNoteForeverMouseClicked

    private void exportButtonPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportButtonPanelMouseClicked
        // TODO add your handling code here:
        int response = JOptionPane.showConfirmDialog(
                null,
                "Do you want to export your notes into an .xlsx file?",
                "Export Notes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            exportNotes();

            JOptionPane.showMessageDialog(this, "Excel file created successfully", "Excel File Successful", JOptionPane.INFORMATION_MESSAGE);

        }

    }//GEN-LAST:event_exportButtonPanelMouseClicked

    private void sortButtonPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sortButtonPanelMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_sortButtonPanelMouseClicked

    private void sortButtonPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sortButtonPanelMouseEntered
        // TODO add your handling code here:
        sortButtonPanel.setBackground(new Color(230, 230, 230));
    }//GEN-LAST:event_sortButtonPanelMouseEntered

    private void sortButtonPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sortButtonPanelMouseExited
        // TODO add your handling code here:
        sortButtonPanel.setBackground(new Color(246, 243, 247));
    }//GEN-LAST:event_sortButtonPanelMouseExited

    private void exportButtonPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportButtonPanelMouseEntered
        // TODO add your handling code here:
        exportButtonPanel.setBackground(new Color(230, 230, 230));
    }//GEN-LAST:event_exportButtonPanelMouseEntered

    private void exportButtonPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportButtonPanelMouseExited
        // TODO add your handling code here:
        exportButtonPanel.setBackground(new Color(246, 243, 247));
    }//GEN-LAST:event_exportButtonPanelMouseExited

    private void deleteCurrentNoteButtonPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteCurrentNoteButtonPanelMouseEntered
        // TODO add your handling code here:
        deleteCurrentNoteButtonPanel.setBackground(new Color(230, 230, 230));
    }//GEN-LAST:event_deleteCurrentNoteButtonPanelMouseEntered

    private void deleteCurrentNoteButtonPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteCurrentNoteButtonPanelMouseExited
        // TODO add your handling code here:
        deleteCurrentNoteButtonPanel.setBackground(Color.WHITE);
    }//GEN-LAST:event_deleteCurrentNoteButtonPanelMouseExited

    private void sortButtonPanel1MouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sortButtonPanel1MouseEntered
        // TODO add your handling code here:
        sortButtonPanel1.setBackground(new Color(230, 230, 230));
    }//GEN-LAST:event_sortButtonPanel1MouseEntered

    private void sortButtonPanel1MouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sortButtonPanel1MouseExited
        // TODO add your handling code here:
        sortButtonPanel1.setBackground(new Color(246, 243, 247));
    }//GEN-LAST:event_sortButtonPanel1MouseExited

    private void restoreCurrentNoteButtonPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_restoreCurrentNoteButtonPanelMouseEntered
        // TODO add your handling code here:
        restoreCurrentNoteButtonPanel.setBackground(new Color(230, 230, 230));
    }//GEN-LAST:event_restoreCurrentNoteButtonPanelMouseEntered

    private void restoreCurrentNoteButtonPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_restoreCurrentNoteButtonPanelMouseExited
        // TODO add your handling code here:
        restoreCurrentNoteButtonPanel.setBackground(Color.WHITE);
    }//GEN-LAST:event_restoreCurrentNoteButtonPanelMouseExited

    private void deleteCurrentNoteForeverMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteCurrentNoteForeverMouseEntered
        // TODO add your handling code here:
        deleteCurrentNoteForever.setBackground(new Color(230, 230, 230));
    }//GEN-LAST:event_deleteCurrentNoteForeverMouseEntered

    private void deleteCurrentNoteForeverMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_deleteCurrentNoteForeverMouseExited
        // TODO add your handling code here:
        deleteCurrentNoteForever.setBackground(Color.WHITE);
    }//GEN-LAST:event_deleteCurrentNoteForeverMouseExited


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.mycompany.notesmanagement.custom.RoundedPanel actionsBorderPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel actionsPanel;
    private javax.swing.JLabel actionsText;
    private javax.swing.JLabel createNewNoteText;
    private javax.swing.JLabel createNoteIcon;
    private com.mycompany.notesmanagement.custom.RoundedPanel createNoteIconPanel;
    private javax.swing.JPanel createNotesContainer;
    private com.mycompany.notesmanagement.custom.RoundedPanel createNotesPanel;
    private javax.swing.JPanel currentUserPanel;
    private javax.swing.JLabel deleteCurrentNote;
    private javax.swing.JLabel deleteCurrentNoteButton;
    private com.mycompany.notesmanagement.custom.RoundedPanel deleteCurrentNoteButtonPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel deleteCurrentNoteForever;
    private javax.swing.JLabel expandToggleButton;
    private javax.swing.JLabel expandToggleButton1;
    private com.mycompany.notesmanagement.custom.RoundedPanel expandToggleButtonPanel;
    private javax.swing.JLabel expirationNotice;
    private javax.swing.JLabel exportButton;
    private com.mycompany.notesmanagement.custom.RoundedPanel exportButtonPanel;
    private javax.swing.JPanel homeBottomPanel;
    private javax.swing.JPanel homeBottomUpperPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel homeButton;
    private javax.swing.JLabel homeButtonIcon;
    private javax.swing.JLabel homeButtonLabel;
    private javax.swing.JPanel homePanel;
    private javax.swing.JPanel homeTopPanel;
    private javax.swing.JLabel lastEditedText;
    private com.mycompany.notesmanagement.custom.RoundedPanel leftPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel logoutButton;
    private javax.swing.JLabel logoutButtonIcon;
    private javax.swing.JLabel logoutButtonLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel manageNotesDashboardBottomIcon;
    private javax.swing.JPanel manageNotesDashboardIcon;
    private com.mycompany.notesmanagement.custom.RoundedPanel manageNotesDashboardIconPanel;
    private javax.swing.JLabel manageNotesDashboardText;
    private javax.swing.JPanel manageNotesPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel manageNotesPanelDashboard;
    private javax.swing.JPopupMenu moreOptions;
    private javax.swing.JLabel moreOptionsNotesDashboardButton;
    private com.mycompany.notesmanagement.custom.RoundedPanel moreOptionsNotesDashboardButtonPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel newNoteButton;
    private javax.swing.JLabel newNoteButtonDashboard;
    private com.mycompany.notesmanagement.custom.RoundedPanel newNoteButtonDashboardPanel;
    private javax.swing.JLabel newNoteButtonIcon;
    private javax.swing.JLabel newNoteButtonLabel;
    private javax.swing.JTextArea noteCreationTextArea;
    private javax.swing.JLabel noteInTrashNotice;
    private com.mycompany.notesmanagement.custom.RoundedPanel noteInTrashNoticePanel;
    private javax.swing.JPanel noteListOptions;
    private javax.swing.JTextArea noteTitleTextArea;
    private com.mycompany.notesmanagement.custom.RoundedPanel notesButton;
    private javax.swing.JLabel notesButtonIcon;
    private javax.swing.JLabel notesButtonLabel;
    private javax.swing.JScrollPane notesContainer;
    private javax.swing.JLabel notesCounter;
    private javax.swing.JPanel notesCreationContentPanel;
    private javax.swing.JScrollPane notesCreationMainPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel notesCreationPanel;
    private javax.swing.JPanel notesCreationPanelBottom;
    private javax.swing.JPanel notesCreationPanelTop;
    private javax.swing.JPanel notesCreationSubPanel;
    private javax.swing.JPanel notesCreationTitlePanel;
    private javax.swing.JPanel notesDashboardPanel;
    private javax.swing.JList<Note> notesList;
    private javax.swing.JPanel notesListMainPanel;
    private javax.swing.JScrollPane notesListMainPanelScroll;
    private javax.swing.JPanel notesListPanel;
    private javax.swing.JScrollPane notesListScrollPane;
    private javax.swing.JPanel notesListSubPanel;
    private javax.swing.JPanel notesListTopPanel;
    private javax.swing.JPanel notesPanel;
    private javax.swing.JLabel notesText;
    private javax.swing.JLabel notesTextList;
    private com.mycompany.notesmanagement.custom.RoundedPanel notesTextPanel;
    private javax.swing.JMenuItem option1;
    private javax.swing.JMenuItem option2;
    private javax.swing.JPanel paddingPanel;
    private javax.swing.JPanel paddingPanel1;
    private javax.swing.JLabel questionLabel;
    private javax.swing.JLabel restoreCurrentNoteButton;
    private com.mycompany.notesmanagement.custom.RoundedPanel restoreCurrentNoteButtonPanel;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JMenuItem scratchOption1;
    private javax.swing.JMenuItem scratchOption2;
    private javax.swing.JPanel scratchPadBottomPanel;
    private javax.swing.JLabel scratchPadMoreOptions;
    private com.mycompany.notesmanagement.custom.RoundedPanel scratchPadMoreOptionsPanel;
    private javax.swing.JPopupMenu scratchPadOptions;
    private javax.swing.JPanel scratchPadPanel;
    private javax.swing.JScrollPane scratchPadScroll;
    private javax.swing.JLabel scratchPadText;
    private javax.swing.JTextArea scratchPadTextArea;
    private com.mycompany.notesmanagement.custom.RoundedPanel scratchPadTextPanel;
    private javax.swing.JPanel scratchPadTopPanel;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JLabel sortButton;
    private javax.swing.JLabel sortButton1;
    private com.mycompany.notesmanagement.custom.RoundedPanel sortButtonPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel sortButtonPanel1;
    private javax.swing.JLabel trademark;
    private com.mycompany.notesmanagement.custom.RoundedPanel trashButton;
    private javax.swing.JLabel trashButtonIcon;
    private javax.swing.JLabel trashButtonLabel;
    private com.mycompany.notesmanagement.custom.RoundedPanel trashExpandToggleButtonPanel;
    private javax.swing.JLabel trashLastEditedText;
    private javax.swing.JPanel trashNoteListOptions;
    private javax.swing.JLabel trashNotesCounter;
    private javax.swing.JPanel trashNotesCreationPanelTop;
    private javax.swing.JList<Trash> trashNotesList;
    private javax.swing.JPanel trashNotesListMainPanel;
    private javax.swing.JScrollPane trashNotesListMainPanelScroll;
    private javax.swing.JPanel trashNotesListPanel;
    private javax.swing.JScrollPane trashNotesListScrollPane1;
    private javax.swing.JPanel trashNotesListSubPanel;
    private javax.swing.JPanel trashNotesListTopPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel trashNotesPreviewPanel;
    private javax.swing.JLabel trashNotesTextList;
    private javax.swing.JPanel trashPanel;
    private javax.swing.JLabel trashVerticalLine;
    private javax.swing.JPanel trashedNoteContentPanel;
    private javax.swing.JScrollPane trashedNoteMainPanel;
    private javax.swing.JPanel trashedNotePanelBottom;
    private javax.swing.JPanel trashedNoteSubPanel;
    private javax.swing.JTextArea trashedNoteTextArea;
    private javax.swing.JPanel trashedNoteTitlePanel;
    private javax.swing.JTextArea trashedNoteTitleTextArea;
    private javax.swing.JLabel userEmailLabel;
    private javax.swing.JLabel userHomeLabel;
    private javax.swing.JLabel userNameInitial;
    private com.mycompany.notesmanagement.custom.RoundedPanel userNameInitialPanel;
    private javax.swing.JPanel userPanel;
    private javax.swing.JLabel usernameLabel;
    private javax.swing.JLabel verticalLine;
    private javax.swing.JLabel viewNotesIcon;
    private com.mycompany.notesmanagement.custom.RoundedPanel viewNotesIconPanel;
    private com.mycompany.notesmanagement.custom.RoundedPanel viewNotesPanel;
    private javax.swing.JLabel viewNotesText;
    // End of variables declaration//GEN-END:variables
}

class Note {

    private final int id;
    private String title;
    private Timestamp updatedAt;
    private final Timestamp createdAt;

    public Note(int id, String title, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return title;
    }

    /**
     * @return the createdAt
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }
}

class Trash {

    private final int id;
    private String title;
    private final Timestamp updatedAt;
    private final Timestamp createdAt;
    private final Timestamp deletedAt;

    public Trash(int id, String title, Timestamp createdAt, Timestamp deletedAt, Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }

    /**
     * @return the createdAt
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * @return the deletedAt
     */
    public Timestamp getDeletedAt() {
        return deletedAt;
    }
}

class CustomListCellRenderer extends DefaultListCellRenderer {

    private final Font font;

    public CustomListCellRenderer(Font font) {
        this.font = font;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setFont(font);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        Dimension preferredSize = new Dimension(label.getPreferredSize().width, 50);
        label.setPreferredSize(preferredSize);

        return label;

    }
}
