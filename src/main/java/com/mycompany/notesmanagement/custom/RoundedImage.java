/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.notesmanagement.custom;

/**
 *
 * @author Regine Torremoro
 */
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class RoundedImage extends JComponent {

    /**
     * @return the image
     */
    public Icon getImage() {
        return image;
    }

    /**
     * @param image the image to set
     */
    public void setImage(Icon image) {
        this.image = image;
        repaint();
    }

    /**
     * @return the borderThickness
     */
    public int getBorderThickness() {
        return borderThickness;
    }

    /**
     * @param borderThickness the borderThickness to set
     */
    public void setBorderThickness(int borderThickness) {
        this.borderThickness = borderThickness;
    }

    /**
     * @return the borderColor
     */
    public Color getBorderColor() {
        return borderColor;
    }

    /**
     * @param borderColor the borderColor to set
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    private Icon image;
    private int borderThickness = 5;
    private Color borderColor = new Color(60, 60, 60);

    @Override
    public void paint(Graphics g) {

        if (getImage() != null) {
            int width = this.getImage().getIconWidth();
            int height = this.getImage().getIconHeight();
            int diameter = Math.min(width, height);

            BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = mask.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.fillOval(0, 0, diameter - 1, diameter - 1);
            g2.dispose();

            BufferedImage masked = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
            g2 = masked.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int x = (diameter - width) / 2;
            int y = (diameter - height) / 2;

            g2.drawImage(toImage(image), x, y, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
            g2.drawImage(mask, 0, 0, null);
            g2.dispose();

            Icon icon = new ImageIcon(masked);
            Rectangle size = rescaleImage(icon);
            Graphics2D g2D = (Graphics2D) g;

            g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2D.drawImage(toImage(icon), size.getLocation().x, size.getLocation().y, size.getSize().width, size.getSize().height, null);

            if (getBorderThickness() > 0) {
                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2D.setColor(getBorderColor());
                g2D.setStroke(new BasicStroke(getBorderThickness()));
                g2D.drawOval(size.x = (getBorderThickness() / 2), size.y + (getBorderThickness() / 2), size.width - getBorderThickness(), size.height - getBorderThickness());
            }

        }

        super.paint(g);

    }

    private Rectangle rescaleImage(Icon image) {

        int width = getWidth();
        int height = getHeight();
        int iconWidth = image.getIconWidth();
        int iconHeight = image.getIconHeight();

        double xScale = (double) width / iconWidth;
        double yScale = (double) height / iconHeight;
        double scale = Math.max(xScale, yScale);

        int w = (int) (scale * iconWidth);
        int h = (int) (scale * iconHeight);
        int x = (width - w) / 2;
        int y = (height - h) / 2;

        return new Rectangle(new Point(x, y), new Dimension(w, h));

    }

    private Image toImage(Icon icon) {
        return ((ImageIcon) icon).getImage();
    }

}
