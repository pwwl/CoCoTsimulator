package Visuals;

import simulation.*;

import javax.swing.*;
import java.awt.event.*;

public class AgentFrame extends JFrame implements MouseMotionListener {

    Visuals.AgentGraphics AgentGraphics;
    AgentHolder AgentHolder;
    Metrics Metrics;

    int mouseUndraggedX, mouseUndraggedY, mouseDraggedX, mouseDraggedY;

    public AgentFrame(int xBounds, int yBounds, AgentHolder AgentHolder, Metrics Metrics) {
        this.setSize(xBounds, yBounds);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.AgentHolder = AgentHolder;
        this.Metrics = Metrics;

        AgentGraphics = new AgentGraphics(AgentHolder, Metrics);

        this.add(AgentGraphics);
        this.setVisible(true);

        this.addMouseWheelListener(e -> AgentGraphics.graphicsScale -= e.getWheelRotation());
        this.addMouseMotionListener(this);
    }

    public void updateGraphics() {
        AgentGraphics.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        mouseDraggedX = e.getX();
        mouseDraggedY = e.getY();
        int diffX = mouseDraggedX-mouseUndraggedX;
        int diffY = mouseDraggedY-mouseUndraggedY;
        AgentGraphics.graphicsXOffset += diffX;
        AgentGraphics.graphicsYOffset += diffY;
        mouseUndraggedX = e.getX();
        mouseUndraggedY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseUndraggedX = e.getX();
        mouseUndraggedY = e.getY();
    }

    public void mouseClicked(MouseEvent m) {
    }
}
