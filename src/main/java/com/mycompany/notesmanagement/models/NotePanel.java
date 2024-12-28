/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.notesmanagement.models;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Regine Torremoro
 */
public class NotePanel extends JPanel {

    private final int PANEL_WIDTH = 100;
    private final int PANEL_HEIGHT = 140;
    private JLabel title;
    private JLabel recentText;
    private int id;

    public NotePanel() {
        createPanel();
        createTitle();
        createTimeStamp();
    }

    public final void createPanel() {

        java.awt.GridBagConstraints gridBagConstraints;

        setBackground(java.awt.Color.white);
        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(124, 142, 255), 2, true));
        setPreferredSize(new java.awt.Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        
        setVisible(true);
    }

    public final void createTitle() {

        java.awt.GridBagConstraints gridBagConstraints;

        title = new javax.swing.JLabel();

        title.setFont(new java.awt.Font("Inter SemiBold", 0, 14));

        title.setForeground(new java.awt.Color(51, 51, 51));

        title.setText("Untitled");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 5, 0);
        add(title, gridBagConstraints);

    }

    public final void createTimeStamp() {

        java.awt.GridBagConstraints gridBagConstraints;

        recentText = new javax.swing.JLabel();

        recentText.setBackground(new java.awt.Color(115, 115, 115));

        recentText.setFont(new java.awt.Font("Inter Medium", 0, 12));

        recentText.setForeground(new java.awt.Color(115, 115, 115));

        recentText.setText("now");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        add(recentText, gridBagConstraints);

    }

    public void setTitle(String title) {
        this.title.setText(title);
        this.title.revalidate();
        this.title.repaint();
    }

    public void setTimeStamp(String timeStamp) {
        this.recentText.setText(timeStamp);
        this.recentText.revalidate();
        this.recentText.repaint();
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

}
