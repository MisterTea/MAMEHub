package com.mamehub.client.cfg;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class InputConfigFrame extends JFrame {

  private JPanel contentPane;
  private String romName;
  private String cfgName;
  private JTextField insertCoin;
  private JTextField start;
  private JTextField up;
  private JTextField down;
  private JTextField left;
  private JTextField right;
  private JTextField button1;
  private JTextField button2;
  private JTextField button3;
  private JTextField button4;
  private JTextField button5;
  private JTextField button6;
  private JTextField button7;
  private JTextField button8;
  private JTextField button9;
  private JTextField button10;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          InputConfigFrame frame = new InputConfigFrame(null, null);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the frame.
   */
  public InputConfigFrame(String cfgName, String romName) {
    this.romName = romName;
    if (romName == null) {
      this.cfgName = "default.cfg";
    } else {
      this.cfgName = cfgName;
    }

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 100, 800, 400);

    if (romName == null) {
      setTitle("Default emulator controls.");
    } else {
      // setup setTitle.
    }

    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    JPanel panel = new JPanel();
    contentPane.add(panel, BorderLayout.NORTH);
    GridBagLayout gbl_panel = new GridBagLayout();
    gbl_panel.columnWidths = new int[] { 140, 250, 140, 250, 0 };
    gbl_panel.rowHeights = new int[] { 28, 28, 0, 0, 0, 0, 0, 0, 0 };
    gbl_panel.columnWeights = new double[] { 0.0, 1.0, 0.0, 1.0,
        Double.MIN_VALUE };
    gbl_panel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, Double.MIN_VALUE };
    panel.setLayout(gbl_panel);

    JLabel lblUp = new JLabel("Insert Coin / Select");
    lblUp.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblUp = new GridBagConstraints();
    gbc_lblUp.fill = GridBagConstraints.BOTH;
    gbc_lblUp.insets = new Insets(0, 0, 5, 5);
    gbc_lblUp.gridx = 0;
    gbc_lblUp.gridy = 0;
    panel.add(lblUp, gbc_lblUp);

    insertCoin = new JTextField();
    GridBagConstraints gbc_insertCoin = new GridBagConstraints();
    gbc_insertCoin.fill = GridBagConstraints.BOTH;
    gbc_insertCoin.insets = new Insets(0, 0, 5, 5);
    gbc_insertCoin.gridx = 1;
    gbc_insertCoin.gridy = 0;
    panel.add(insertCoin, gbc_insertCoin);
    insertCoin.setColumns(100);

    JLabel lblStart = new JLabel("Start");
    lblStart.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblStart = new GridBagConstraints();
    gbc_lblStart.fill = GridBagConstraints.BOTH;
    gbc_lblStart.insets = new Insets(0, 0, 5, 5);
    gbc_lblStart.gridx = 2;
    gbc_lblStart.gridy = 0;
    panel.add(lblStart, gbc_lblStart);

    start = new JTextField();
    start.setColumns(100);
    GridBagConstraints gbc_start = new GridBagConstraints();
    gbc_start.fill = GridBagConstraints.BOTH;
    gbc_start.insets = new Insets(0, 0, 5, 0);
    gbc_start.gridx = 3;
    gbc_start.gridy = 0;
    panel.add(start, gbc_start);

    JLabel label_1 = new JLabel("Up");
    label_1.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_label_1 = new GridBagConstraints();
    gbc_label_1.fill = GridBagConstraints.BOTH;
    gbc_label_1.insets = new Insets(0, 0, 5, 5);
    gbc_label_1.gridx = 0;
    gbc_label_1.gridy = 1;
    panel.add(label_1, gbc_label_1);

    up = new JTextField();
    up.setColumns(100);
    GridBagConstraints gbc_up = new GridBagConstraints();
    gbc_up.fill = GridBagConstraints.BOTH;
    gbc_up.insets = new Insets(0, 0, 5, 5);
    gbc_up.gridx = 1;
    gbc_up.gridy = 1;
    panel.add(up, gbc_up);

    JLabel lblLeft = new JLabel("Down");
    lblLeft.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblLeft = new GridBagConstraints();
    gbc_lblLeft.fill = GridBagConstraints.BOTH;
    gbc_lblLeft.insets = new Insets(0, 0, 5, 5);
    gbc_lblLeft.gridx = 2;
    gbc_lblLeft.gridy = 1;
    panel.add(lblLeft, gbc_lblLeft);

    down = new JTextField();
    down.setColumns(100);
    GridBagConstraints gbc_down = new GridBagConstraints();
    gbc_down.insets = new Insets(0, 0, 5, 0);
    gbc_down.fill = GridBagConstraints.BOTH;
    gbc_down.gridx = 3;
    gbc_down.gridy = 1;
    panel.add(down, gbc_down);

    JLabel lblLeft_1 = new JLabel("Left");
    lblLeft_1.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblLeft_1 = new GridBagConstraints();
    gbc_lblLeft_1.anchor = GridBagConstraints.EAST;
    gbc_lblLeft_1.insets = new Insets(0, 0, 5, 5);
    gbc_lblLeft_1.gridx = 0;
    gbc_lblLeft_1.gridy = 2;
    panel.add(lblLeft_1, gbc_lblLeft_1);

    left = new JTextField();
    left.setColumns(100);
    GridBagConstraints gbc_left = new GridBagConstraints();
    gbc_left.insets = new Insets(0, 0, 5, 5);
    gbc_left.fill = GridBagConstraints.HORIZONTAL;
    gbc_left.gridx = 1;
    gbc_left.gridy = 2;
    panel.add(left, gbc_left);

    JLabel lblRight = new JLabel("Right");
    lblRight.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblRight = new GridBagConstraints();
    gbc_lblRight.anchor = GridBagConstraints.EAST;
    gbc_lblRight.insets = new Insets(0, 0, 5, 5);
    gbc_lblRight.gridx = 2;
    gbc_lblRight.gridy = 2;
    panel.add(lblRight, gbc_lblRight);

    right = new JTextField();
    right.setColumns(100);
    GridBagConstraints gbc_right = new GridBagConstraints();
    gbc_right.insets = new Insets(0, 0, 5, 0);
    gbc_right.fill = GridBagConstraints.HORIZONTAL;
    gbc_right.gridx = 3;
    gbc_right.gridy = 2;
    panel.add(right, gbc_right);

    JLabel lblButton = new JLabel("Button 1");
    lblButton.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton = new GridBagConstraints();
    gbc_lblButton.anchor = GridBagConstraints.EAST;
    gbc_lblButton.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton.gridx = 0;
    gbc_lblButton.gridy = 3;
    panel.add(lblButton, gbc_lblButton);

    button1 = new JTextField();
    button1.setColumns(100);
    GridBagConstraints gbc_button1 = new GridBagConstraints();
    gbc_button1.insets = new Insets(0, 0, 5, 5);
    gbc_button1.fill = GridBagConstraints.HORIZONTAL;
    gbc_button1.gridx = 1;
    gbc_button1.gridy = 3;
    panel.add(button1, gbc_button1);

    JLabel lblButton_1 = new JLabel("Button 2");
    lblButton_1.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_1 = new GridBagConstraints();
    gbc_lblButton_1.anchor = GridBagConstraints.EAST;
    gbc_lblButton_1.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton_1.gridx = 2;
    gbc_lblButton_1.gridy = 3;
    panel.add(lblButton_1, gbc_lblButton_1);

    button2 = new JTextField();
    button2.setColumns(100);
    GridBagConstraints gbc_button2 = new GridBagConstraints();
    gbc_button2.insets = new Insets(0, 0, 5, 0);
    gbc_button2.fill = GridBagConstraints.HORIZONTAL;
    gbc_button2.gridx = 3;
    gbc_button2.gridy = 3;
    panel.add(button2, gbc_button2);

    JLabel lblButton_2 = new JLabel("Button 3");
    lblButton_2.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_2 = new GridBagConstraints();
    gbc_lblButton_2.anchor = GridBagConstraints.EAST;
    gbc_lblButton_2.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton_2.gridx = 0;
    gbc_lblButton_2.gridy = 4;
    panel.add(lblButton_2, gbc_lblButton_2);

    button3 = new JTextField();
    button3.setColumns(100);
    GridBagConstraints gbc_button3 = new GridBagConstraints();
    gbc_button3.insets = new Insets(0, 0, 5, 5);
    gbc_button3.fill = GridBagConstraints.HORIZONTAL;
    gbc_button3.gridx = 1;
    gbc_button3.gridy = 4;
    panel.add(button3, gbc_button3);

    JLabel lblButton_3 = new JLabel("Button 4");
    lblButton_3.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_3 = new GridBagConstraints();
    gbc_lblButton_3.anchor = GridBagConstraints.EAST;
    gbc_lblButton_3.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton_3.gridx = 2;
    gbc_lblButton_3.gridy = 4;
    panel.add(lblButton_3, gbc_lblButton_3);

    button4 = new JTextField();
    button4.setColumns(100);
    GridBagConstraints gbc_button4 = new GridBagConstraints();
    gbc_button4.insets = new Insets(0, 0, 5, 0);
    gbc_button4.fill = GridBagConstraints.HORIZONTAL;
    gbc_button4.gridx = 3;
    gbc_button4.gridy = 4;
    panel.add(button4, gbc_button4);

    JLabel lblButton_4 = new JLabel("Button 5");
    lblButton_4.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_4 = new GridBagConstraints();
    gbc_lblButton_4.anchor = GridBagConstraints.EAST;
    gbc_lblButton_4.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton_4.gridx = 0;
    gbc_lblButton_4.gridy = 5;
    panel.add(lblButton_4, gbc_lblButton_4);

    button5 = new JTextField();
    button5.setColumns(100);
    GridBagConstraints gbc_button5 = new GridBagConstraints();
    gbc_button5.insets = new Insets(0, 0, 5, 5);
    gbc_button5.fill = GridBagConstraints.HORIZONTAL;
    gbc_button5.gridx = 1;
    gbc_button5.gridy = 5;
    panel.add(button5, gbc_button5);

    JLabel lblButton_5 = new JLabel("Button 6");
    lblButton_5.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_5 = new GridBagConstraints();
    gbc_lblButton_5.anchor = GridBagConstraints.EAST;
    gbc_lblButton_5.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton_5.gridx = 2;
    gbc_lblButton_5.gridy = 5;
    panel.add(lblButton_5, gbc_lblButton_5);

    button6 = new JTextField();
    button6.setColumns(100);
    GridBagConstraints gbc_button6 = new GridBagConstraints();
    gbc_button6.insets = new Insets(0, 0, 5, 0);
    gbc_button6.fill = GridBagConstraints.HORIZONTAL;
    gbc_button6.gridx = 3;
    gbc_button6.gridy = 5;
    panel.add(button6, gbc_button6);

    JLabel lblButton_6 = new JLabel("Button 7");
    lblButton_6.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_6 = new GridBagConstraints();
    gbc_lblButton_6.anchor = GridBagConstraints.EAST;
    gbc_lblButton_6.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton_6.gridx = 0;
    gbc_lblButton_6.gridy = 6;
    panel.add(lblButton_6, gbc_lblButton_6);

    button7 = new JTextField();
    button7.setColumns(100);
    GridBagConstraints gbc_button7 = new GridBagConstraints();
    gbc_button7.insets = new Insets(0, 0, 5, 5);
    gbc_button7.fill = GridBagConstraints.HORIZONTAL;
    gbc_button7.gridx = 1;
    gbc_button7.gridy = 6;
    panel.add(button7, gbc_button7);

    JLabel lblButton_7 = new JLabel("Button 8");
    lblButton_7.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_7 = new GridBagConstraints();
    gbc_lblButton_7.anchor = GridBagConstraints.EAST;
    gbc_lblButton_7.insets = new Insets(0, 0, 5, 5);
    gbc_lblButton_7.gridx = 2;
    gbc_lblButton_7.gridy = 6;
    panel.add(lblButton_7, gbc_lblButton_7);

    button8 = new JTextField();
    button8.setColumns(100);
    GridBagConstraints gbc_button8 = new GridBagConstraints();
    gbc_button8.insets = new Insets(0, 0, 5, 0);
    gbc_button8.fill = GridBagConstraints.HORIZONTAL;
    gbc_button8.gridx = 3;
    gbc_button8.gridy = 6;
    panel.add(button8, gbc_button8);

    JLabel lblButton_8 = new JLabel("Button 9");
    lblButton_8.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_8 = new GridBagConstraints();
    gbc_lblButton_8.anchor = GridBagConstraints.EAST;
    gbc_lblButton_8.insets = new Insets(0, 0, 0, 5);
    gbc_lblButton_8.gridx = 0;
    gbc_lblButton_8.gridy = 7;
    panel.add(lblButton_8, gbc_lblButton_8);

    button9 = new JTextField();
    button9.setColumns(100);
    GridBagConstraints gbc_button9 = new GridBagConstraints();
    gbc_button9.insets = new Insets(0, 0, 0, 5);
    gbc_button9.fill = GridBagConstraints.HORIZONTAL;
    gbc_button9.gridx = 1;
    gbc_button9.gridy = 7;
    panel.add(button9, gbc_button9);

    JLabel lblButton_9 = new JLabel("Button 10");
    lblButton_9.setHorizontalAlignment(SwingConstants.RIGHT);
    GridBagConstraints gbc_lblButton_9 = new GridBagConstraints();
    gbc_lblButton_9.anchor = GridBagConstraints.EAST;
    gbc_lblButton_9.insets = new Insets(0, 0, 0, 5);
    gbc_lblButton_9.gridx = 2;
    gbc_lblButton_9.gridy = 7;
    panel.add(lblButton_9, gbc_lblButton_9);

    button10 = new JTextField();
    button10.setColumns(100);
    GridBagConstraints gbc_button10 = new GridBagConstraints();
    gbc_button10.fill = GridBagConstraints.HORIZONTAL;
    gbc_button10.gridx = 3;
    gbc_button10.gridy = 7;
    panel.add(button10, gbc_button10);

    JPanel panel_1 = new JPanel();
    contentPane.add(panel_1, BorderLayout.SOUTH);
    panel_1.setLayout(new FlowLayout(FlowLayout.RIGHT));

    JButton button = new JButton("OK");
    button.setActionCommand("OK");
    panel_1.add(button);
    setVisible(true);
  }

}
