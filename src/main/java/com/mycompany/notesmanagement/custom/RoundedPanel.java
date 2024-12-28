/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.notesmanagement.custom;

/**
 *
 * @author Regine Torremoro
 */
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class RoundedPanel extends JPanel {

    private int radiusTopLeft = 0;
    private int radiusTopRight = 0;
    private int radiusBottomLeft = 0;
    private int radiusBottomRight = 0;

    public RoundedPanel() {
        setOpaque(false);
    }

    /**
     * @return the radiusTopLeft
     */
    public int getRadiusTopLeft() {
        return radiusTopLeft;
    }

    /**
     * @param radiusTopLeft the radiusTopLeft to set
     */
    public void setRadiusTopLeft(int radiusTopLeft) {
        this.radiusTopLeft = radiusTopLeft;
    }

    /**
     * @return the radiusTopRight
     */
    public int getRadiusTopRight() {
        return radiusTopRight;
    }

    /**
     * @param radiusTopRight the radiusTopRight to set
     */
    public void setRadiusTopRight(int radiusTopRight) {
        this.radiusTopRight = radiusTopRight;
    }

    /**
     * @return the radiusBottomLeft
     */
    public int getRadiusBottomLeft() {
        return radiusBottomLeft;
    }

    /**
     * @param radiusBottomLeft the radiusBottomLeft to set
     */
    public void setRadiusBottomLeft(int radiusBottomLeft) {
        this.radiusBottomLeft = radiusBottomLeft;
    }

    /**
     * @return the radiusBottomRight
     */
    public int getRadiusBottomRight() {
        return radiusBottomRight;
    }

    /**
     * @param radiusBottomRight the radiusBottomRight to set
     */
    public void setRadiusBottomRight(int radiusBottomRight) {
        this.radiusBottomRight = radiusBottomRight;
    }

    @Override
    protected void paintComponent(Graphics graphics) {

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());

        Area area = new Area(createRoundedTopLeftCorner());

        if (getRadiusTopRight() > 0) {
            area.intersect(new Area(createRoundedTopRightCorner()));
        }

        if (getRadiusBottomLeft() > 0) {
            area.intersect(new Area(createRoundedBottomLeftCorner()));
        }

        if (getRadiusBottomRight() > 0) {
            area.intersect(new Area(createRoundedBottomRightCorner()));
        }

        g2.fill(area);
        g2.dispose();
        super.paintComponent(graphics);

    }

    private Shape createRoundedTopRightCorner() {

        int width = getWidth();
        int height = getHeight();
        int radiusX = Math.min(width, getRadiusTopRight());
        int radiusY = Math.min(height, getRadiusTopRight());
        Area area = new Area(new RoundRectangle2D.Double(0, 0, width, height, radiusX, radiusY));

        area.add(new Area(new Rectangle2D.Double(0, 0, width - radiusX / 2, height)));
        area.add(new Area(new Rectangle2D.Double(0, radiusY / 2, width, height - radiusY / 2)));

        return area;

    }

    private Shape createRoundedTopLeftCorner() {

        int width = getWidth();
        int height = getHeight();
        int radiusX = Math.min(width, getRadiusTopLeft());
        int radiusY = Math.min(height, getRadiusTopLeft());
        Area area = new Area(new RoundRectangle2D.Double(0, 0, width, height, radiusX, radiusY));

        area.add(new Area(new Rectangle2D.Double(radiusX / 2, 0, width - radiusX / 2, height)));
        area.add(new Area(new Rectangle2D.Double(0, radiusY / 2, width, height - radiusY / 2)));

        return area;

    }

    private Shape createRoundedBottomLeftCorner() {

        int width = getWidth();
        int height = getHeight();
        int radiusX = Math.min(width, getRadiusBottomLeft());
        int radiusY = Math.min(height, getRadiusBottomLeft());
        Area area = new Area(new RoundRectangle2D.Double(0, 0, width, height, radiusX, radiusY));

        area.add(new Area(new Rectangle2D.Double(radiusX / 2, 0, width - radiusX / 2, height)));
        area.add(new Area(new Rectangle2D.Double(0, 0, width, height - radiusY / 2)));

        return area;

    }

    private Shape createRoundedBottomRightCorner() {

        int width = getWidth();
        int height = getHeight();
        int radiusX = Math.min(width, getRadiusBottomRight());
        int radiusY = Math.min(height, getRadiusBottomRight());
        Area area = new Area(new RoundRectangle2D.Double(0, 0, width, height, radiusX, radiusY));

        area.add(new Area(new Rectangle2D.Double(0, 0, width - radiusX / 2, height)));
        area.add(new Area(new Rectangle2D.Double(0, 0, width, height - radiusY / 2)));

        return area;

    }

}
