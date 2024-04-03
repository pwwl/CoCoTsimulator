/**
 ** Copyright 2024 Trevor Kann, Lujo Bauer, Robert K. Cunningham
 ** 
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 **/
package Visuals;

import simulation.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class AgentGraphics extends JPanel implements ActionListener, KeyListener{

    AgentHolder AgentHolder;
    Metrics Metrics;

    Timer timer = new Timer(1000,this);
    int seconds = 0;

    int graphicsScale = 10;
    int graphicsXOffset = 0;
    int graphicsYOffset = 0;

    public AgentGraphics(AgentHolder AgentHolder, Metrics Metrics){
        this.AgentHolder = AgentHolder;
        this.Metrics = Metrics;
    }

    public void startTimer(){
        timer.start();
    }

    public void paintComponent(Graphics g){

        super.paintComponent(g);
        this.setBackground(Color.WHITE);

        Graphics2D g2d = (Graphics2D) g;

        paintAgents(g2d);
        //paintMetrics(g2d);
        //notify();
    }

    public void paintAgents(Graphics2D g2d){

        g2d.setStroke(new BasicStroke(2));

        for (Agent currentAgent : AgentHolder.allAgents){
            paintAgentAndNeighborLines(g2d, currentAgent);
        }
    }

    public void paintAgentAndNeighborLines(Graphics2D g2d, Agent agentToPaint){
        paintSingleAgent(g2d, agentToPaint);
        paintNeighborLines(g2d, agentToPaint);
    }

    public void paintSingleAgent(Graphics2D g2d, Agent agentToPaint){
        int agentRadius = 3 ;//* graphicsScale;

        g2d.setColor(Color.BLACK);
        int agentXCord = getAgentXCord(agentToPaint);
        int agentYCord = getAgentYCord(agentToPaint);

        g2d.fillOval(agentXCord-agentRadius, agentYCord-agentRadius,agentRadius*2, agentRadius*2);

    }

    public void paintNeighborLines(Graphics2D g2d, Agent agentToPaint){

        int agentXCord = getAgentXCord(agentToPaint);
        int agentYCord = getAgentYCord(agentToPaint);

        for (Agent neighbor : agentToPaint.getListOfNeighbors()){

            double distance = Utility.distanceBetween(agentToPaint, neighbor);
            double guessedDistance = agentToPaint.getGuessedDistanceTo(neighbor);
            boolean correct = (distance<=6)==(guessedDistance<=6);

            int neighborXCord = getAgentXCord(neighbor);
            int neighborYCord = getAgentYCord(neighbor);

            if (correct){
                g2d.setColor(Color.GREEN);
            } else {
                g2d.setColor(Color.RED);
            }

            //makes the neighbor line a little more clockwise from agent's perspective so that both can be seen
            double neighborVectorX = agentXCord - neighborXCord;
            double neighborVectorY = agentYCord - neighborYCord;

            double invVectorNorm = 1/Math.sqrt(neighborVectorX*neighborVectorX + neighborVectorY*neighborVectorY);

            int normalizedVectorX = (int) Math.round(neighborVectorX * invVectorNorm * 4);
            int normalizedVectorY = (int) Math.round(neighborVectorY * invVectorNorm * 4);

            g2d.drawLine(agentXCord + normalizedVectorY, agentYCord - normalizedVectorX, getAgentXCord(neighbor) + normalizedVectorY, getAgentYCord(neighbor) - normalizedVectorX);


            g2d.setColor(Color.BLACK);
            int halfwayX = (int) ((agentXCord + neighborXCord) / 2 );
            int halfwayY = (int) ((agentYCord + neighborYCord) / 2 );

            g2d.setFont(new Font("Times New Roman", Font.PLAIN, (int) (graphicsScale/4 + 4)));
            g2d.drawString(String.format("%2.2f",agentToPaint.getGuessedDistanceTo(neighbor)), halfwayX + normalizedVectorY, halfwayY - 2*normalizedVectorX );
        }

    }

    public int getAgentXCord(Agent agentInQuestion){
        return  (int) ((agentInQuestion.getXCoord() * graphicsScale) + graphicsXOffset);
    }

    public int getAgentYCord(Agent agentInQuestion){
        return  (int) ((agentInQuestion.getYCoordinate() * graphicsScale) + graphicsYOffset);
    }

    public void paintMetrics(Graphics2D g2d){
        for (int x = 0; x < AgentHolder.sectors.length; x++) {
            for (int y = 0; y < AgentHolder.sectors[x].length; y++) {
                Sector currentSect = AgentHolder.sectors[x][y];
                double winRate = currentSect.getAccuracyOfSector();

                int color = (int) (255*winRate);

                g2d.setColor(new Color(color, color, color));

                int xmin = (int) currentSect.getxMin();
                int ymin = (int) currentSect.getyMin();
                int xWidth = (int) currentSect.getxMax() - xmin;
                int yWidth = (int) currentSect.getyMax() - ymin;
                g2d.drawRect(xmin, ymin, xWidth, yWidth);
            }
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {
        System.out.println("key typed");
        System.out.println(e.getKeyChar());

    }

    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("keyPressed");
        System.out.println(e.getKeyChar());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        System.out.println("key released");
        System.out.println(e.getKeyChar());

    }
}
