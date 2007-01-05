 /*
  * @(#)SVGImage.java  1.0  July 8, 2006
  *
  * Copyright (c) 1996-2006 by the original authors of JHotDraw
  * and all its contributors ("JHotDraw.org")
  * All rights reserved.
  *
  * This software is the confidential and proprietary information of
  * JHotDraw.org ("Confidential Information"). You shall not disclose
  * such Confidential Information and shall use it only in accordance
  * with the terms of the license agreement you entered into with
  * JHotDraw.org.
  */

package org.jhotdraw.samples.svg.figures;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.undo.*;
import org.jhotdraw.draw.*;
import static org.jhotdraw.samples.svg.SVGAttributeKeys.*;
import org.jhotdraw.samples.svg.*;
import org.jhotdraw.util.*;
import org.jhotdraw.xml.*;
import org.jhotdraw.geom.*;


/**
 * SVGImage.
 * <p>
 * FIXME - Implement me
 *
 * @author Werner Randelshofer
 * @version 1.0 July 8, 2006 Created.
 */
public class SVGImageFigure extends SVGAttributedFigure implements SVGFigure, ImageHolder {
    /**
     * This rectangle describes the bounds into which we draw the image.
     */
    private Rectangle2D.Double rect;
    /**
     * This is used to perform faster drawing.
     */
    private Shape cachedTransformedShape;
    /**
     * This is used to perform faster hit testing.
     */
    private Shape cachedHitShape;
    /**
     * The image data. This can be null, if the image was created from a
     * BufferedImage.
     */
    private byte[] imageData;
    
    /**
     * The buffered image. This can be null, if we haven't yet parsed the
     * imageData.
     */
    private BufferedImage bufferedImage;
    
    /** Creates a new instance. */
    public SVGImageFigure() {
        this(0,0,0,0);
    }
    public SVGImageFigure(double x, double y, double width, double height) {
        rect = new Rectangle2D.Double(x, y, width, height);
       SVGConstants.setDefaults(this);
    }
    
    // DRAWING
    public void drawFigure(Graphics2D g) {
        super.drawFigure(g);
        BufferedImage image = getBufferedImage();
        if (image != null) {
            if (TRANSFORM.get(this) != null) {
                // FIXME - We should cache the transformed image.
                //         Drawing a transformed image appears to be very slow.
                Graphics2D gx = (Graphics2D) g.create();
                gx.transform(TRANSFORM.get(this));
                gx.drawImage(image, (int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height, null);
                gx.dispose();
            } else {
                g.drawImage(image, (int) rect.x, (int) rect.y, (int) rect.width, (int) rect.height, null);
            }
        } else {
            Shape shape = getTransformedShape();
            g.setColor(Color.red);
            g.draw(shape);
        }
    }
    protected void drawFill(Graphics2D g) {
        
    }
    protected void drawStroke(Graphics2D g) {
        
    }
    
    // SHAPE AND BOUNDS
    public double getX() {
        return rect.x;
    }
    public double getY() {
        return rect.y;
    }
    public double getWidth() {
        return rect.width;
    }
    public double getHeight() {
        return rect.height;
    }
    public Rectangle2D.Double getBounds() {
        Rectangle2D rx = getTransformedShape().getBounds2D();
        Rectangle2D.Double r = (rx instanceof Rectangle2D.Double) ? (Rectangle2D.Double) rx : new Rectangle2D.Double(rx.getX(), rx.getY(), rx.getWidth(), rx.getHeight());
        return r;
    }
    public Rectangle2D.Double getFigureDrawBounds() {
        Rectangle2D rx = getTransformedShape().getBounds2D();
        Rectangle2D.Double r = (rx instanceof Rectangle2D.Double) ? (Rectangle2D.Double) rx : new Rectangle2D.Double(rx.getX(), rx.getY(), rx.getWidth(), rx.getHeight());
        double g = AttributeKeys.getPerpendicularHitGrowth(this) * 2;
        Geom.grow(r, g, g);
        return r;
    }
    /**
     * Checks if a Point2D.Double is inside the figure.
     */
    public boolean contains(Point2D.Double p) {
        return getHitShape().contains(p);
    }
    
    public void basicSetBounds(Point2D.Double anchor, Point2D.Double lead) {
        invalidateTransformedShape();
        rect.x = Math.min(anchor.x, lead.x);
        rect.y = Math.min(anchor.y , lead.y);
        rect.width = Math.max(0.1, Math.abs(lead.x - anchor.x));
        rect.height = Math.max(0.1, Math.abs(lead.y - anchor.y));
    }
    private void invalidateTransformedShape() {
        cachedTransformedShape = null;
        cachedHitShape = null;
    }
    private Shape getTransformedShape() {
        if (cachedTransformedShape == null) {
            cachedTransformedShape = (Shape) rect.clone();
            if (TRANSFORM.get(this) != null) {
                cachedTransformedShape = TRANSFORM.get(this).createTransformedShape(cachedTransformedShape);
            }
        }
        return cachedTransformedShape;
    }
    private Shape getHitShape() {
        if (cachedHitShape == null) {
            cachedHitShape = new GrowStroke(
                    (float) SVGAttributeKeys.getStrokeTotalWidth(this) / 2f,
                    (float) SVGAttributeKeys.getStrokeTotalMiterLimit(this)
                    ).createStrokedShape(getTransformedShape());
        }
        return cachedHitShape;
    }
    /**
     * Transforms the figure.
     * @param tx The transformation.
     */
    public void basicTransform(AffineTransform tx) {
        invalidateTransformedShape();
        if (TRANSFORM.get(this) != null ||
                (tx.getType() & (AffineTransform.TYPE_TRANSLATION | AffineTransform.TYPE_MASK_SCALE)) != tx.getType()) {
            if (TRANSFORM.get(this) == null) {
                TRANSFORM.set(this, (AffineTransform) tx.clone());
            } else {
                TRANSFORM.get(this).preConcatenate(tx);
            }
        } else {
            Point2D.Double anchor = getStartPoint();
            Point2D.Double lead = getEndPoint();
            basicSetBounds(
                    (Point2D.Double) tx.transform(anchor, anchor),
                    (Point2D.Double) tx.transform(lead, lead)
                    );
        }
    }
    // ATTRIBUTES
    
    
    public void restoreTo(Object geometry) {
        TRANSFORM.set(this, (geometry == null) ? null : (AffineTransform) ((AffineTransform) geometry).clone());
    }
    
    public Object getRestoreData() {
        return TRANSFORM.get(this) == null ? new AffineTransform() : TRANSFORM.get(this).clone();
    }
    
    // EDITING
    public Collection<Handle> createHandles(int detailLevel) {
        LinkedList<Handle> handles = (LinkedList<Handle>) super.createHandles(detailLevel);
        handles.add(new RotateHandle(this));
        
        return handles;
    }
    @Override public Collection<Action> getActions(Point2D.Double p) {
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.samples.svg.Labels");
        LinkedList<Action> actions = new LinkedList<Action>();
        if (TRANSFORM.get(this) != null) {
            actions.add(new AbstractAction(labels.getString("removeTransform")) {
                public void actionPerformed(ActionEvent evt) {
                    TRANSFORM.set(SVGImageFigure.this, null);
                }
            });
        }
        return actions;
    }
    // CONNECTING
    public Connector findConnector(Point2D.Double p, ConnectionFigure prototype) {
        // XXX - This doesn't work with a transformed rect
        return new ChopBoxConnector(this);
    }
    public Connector findCompatibleConnector(Connector c, boolean isStartConnector) {
        // XXX - This doesn't work with a transformed rect
        return new ChopBoxConnector(this);
    }
    
    // COMPOSITE FIGURES
    // CLONING
    public SVGImageFigure clone() {
        SVGImageFigure that = (SVGImageFigure) super.clone();
        that.rect = (Rectangle2D.Double) this.rect.clone();
        that.cachedTransformedShape = null;
        that.cachedHitShape = null;
        return that;
    }
    
    
    
    public void read(DOMInput in) throws IOException {
        double x = SVGUtil.getDimension(in, "x");
        double y = SVGUtil.getDimension(in, "y");
        double w = SVGUtil.getDimension(in, "width");
        double h = SVGUtil.getDimension(in, "height");
        setBounds(new Point2D.Double(x,y), new Point2D.Double(x+w,y+h));
        readAttributes(in);
        AffineTransform tx = SVGUtil.getTransform(in, "transform");
        basicTransform(tx);
    }
    protected void readAttributes(DOMInput in) throws IOException {
        SVGUtil.readAttributes(this, in);
    }
    
    public void write(DOMOutput out) throws IOException {
        Rectangle2D.Double r = getBounds();
        out.addAttribute("x", r.x);
        out.addAttribute("y", r.y);
        out.addAttribute("width", r.width);
        out.addAttribute("height", r.height);
        writeAttributes(out);
    }
    protected void writeAttributes(DOMOutput out) throws IOException {
        SVGUtil.writeAttributes(this, out);
    }
    public boolean isEmpty() {
        Rectangle2D.Double b = getBounds();
        return b.width <= 0 || b.height <= 0 || imageData == null && bufferedImage == null;
    }
    
    @Override public void invalidate() {
        super.invalidate();
        invalidateTransformedShape();
    }
    
    /**
     * Sets the image.
     *
     * @param fileSuffix the file name suffix of the file that the image
     * was created from.
     * @param imageData The image data. If this is null, a buffered image must
     * be provided.
     * @param bufferedImage An image constructed from the imageData. If this
     * is null, imageData must be provided.
     */
    public void setImage(byte[] imageData, BufferedImage bufferedImage) {
        willChange();
        this.imageData = imageData;
        this.bufferedImage = bufferedImage;
        changed();
    }
    /**
     * Sets the image data.
     * This clears the buffered image.
     */
    public void setImageData(byte[] imageData) {
        willChange();
        this.imageData = imageData;
        this.bufferedImage = null;
        changed();
    }
    /**
     * Sets the buffered image.
     * This clears the image data.
     */
    public void setBufferedImage(BufferedImage image) {
        willChange();
        this.imageData = null;
        this.bufferedImage = image;
        changed();
    }
    
    /**
     * Gets the buffered image. If necessary, this method creates the buffered
     * image from the image data.
     */
    public BufferedImage getBufferedImage() {
        if (bufferedImage == null && imageData != null) {
            System.out.println("recreateing bufferedImage");
            try {
                bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
            } catch (IOException e) {
                e.printStackTrace();
                // If we can't create a buffered image from the image data,
                // there is no use to keep the image data and try again, so
                // we drop the image data.
                imageData = null;
            }
        }
        return bufferedImage;
    }
    /**
     * Gets the image data. If necessary, this method creates the image
     * data from the buffered image.
     */
    public byte[] getImageData() {
        if (bufferedImage != null && imageData == null) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "PNG", bout);
                bout.close();
                imageData = bout.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                // If we can't create image data from the buffered image,
                // there is no use to keep the buffered image and try again, so
                // we drop the buffered image.
                bufferedImage = null;
            }
        }
        return imageData;
    }    

    public void loadImage(File file) throws IOException {
        ResourceBundleUtil labels = ResourceBundleUtil.getLAFBundle("org.jhotdraw.draw.Labels");
        DataInputStream in = null;
        byte[] buf = null;
        BufferedImage img = null;
        try {
            in = new DataInputStream(new FileInputStream(file));
            buf = new byte[(int) file.length()];
            in.readFully(buf);
            img = ImageIO.read(new ByteArrayInputStream(buf));
        } catch (Throwable t) {
            IOException ex =  new IOException(labels.getFormatted("failedToLoadImage", file.getName()));
            ex.initCause(t);
            throw ex;
        }
        if (img == null) {
            throw new IOException(labels.getFormatted("failedToLoadImage", file.getName()));
        }
        imageData = buf;
        bufferedImage = img;
    }
}