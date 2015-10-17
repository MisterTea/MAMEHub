package com.mamehub.client.login;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.validator.routines.EmailValidator;

import com.mamehub.client.Utils;
import com.mamehub.client.net.RpcEngine;

public class NewAccountDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  private final JPanel contentPanel = new JPanel();
  private JTextField passwordTwiceTextField;
  private JTextField passwordOnceTextField;
  private JTextField emailTextField;
  private JTextField usernameTextField;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    try {
      NewAccountDialog dialog = new NewAccountDialog(null, null);
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the dialog.
   */
  public NewAccountDialog(JFrame parent, final RpcEngine rpcEngine) {
    super(parent, true);
    setBounds(100, 100, 450, 240);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new BorderLayout(0, 0));
    {
      JLabel lblNewLabel = new JLabel(
          "Fill out some information to get started");
      lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
      lblNewLabel.setVerticalAlignment(SwingConstants.TOP);
      contentPanel.add(lblNewLabel, BorderLayout.NORTH);
    }
    {
      JPanel panel = new JPanel();
      contentPanel.add(panel);
      panel.setLayout(new GridLayout(0, 2, 0, 0));
      {
        JLabel lblEmailAddress = new JLabel("Email Address");
        lblEmailAddress.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblEmailAddress);
      }
      {
        emailTextField = new JTextField();
        emailTextField.setColumns(10);
        panel.add(emailTextField);
      }
      {
        JLabel label = new JLabel("Username");
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(label);
      }
      {
        usernameTextField = new JTextField();
        usernameTextField.setColumns(10);
        panel.add(usernameTextField);
      }
      {
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblPassword);
      }
      {
        passwordOnceTextField = new JPasswordField();
        passwordOnceTextField.setColumns(10);
        panel.add(passwordOnceTextField);
      }
      {
        JLabel lblPasswordAgain = new JLabel("Password (again)");
        lblPasswordAgain.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(lblPasswordAgain);
      }
      {
        passwordTwiceTextField = new JPasswordField();
        panel.add(passwordTwiceTextField);
        passwordTwiceTextField.setColumns(10);
      }
    }
    {
      JPanel buttonPane = new JPanel();
      buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            RpcEngine.PingResponse pr = rpcEngine.ping();
            if (pr == RpcEngine.PingResponse.SERVER_DOWN) {
              JOptionPane.showMessageDialog(NewAccountDialog.this,
                  "New Account failed: MAMEHub server is down.");
              return;
            }
            if (pr == RpcEngine.PingResponse.CLIENT_TOO_OLD) {
              JOptionPane
                  .showMessageDialog(NewAccountDialog.this,
                      "New Account failed: Your client is out of date, please restart MAMEHub.");
              return;
            }
            if (!passwordOnceTextField.getText().equals(
                passwordTwiceTextField.getText())) {
              JOptionPane.showMessageDialog(NewAccountDialog.this,
                  "Passwords much match.");
              return;
            }
            if (emailTextField.getText().length() < 3
                || passwordOnceTextField.getText().length() < 3) {
              JOptionPane
                  .showMessageDialog(NewAccountDialog.this,
                      "Please enter a username & password with at least 3 characters.");
              return;
            }
            if (!usernameTextField.getText().matches("[0-9a-zA-Z]+")) {
              JOptionPane.showMessageDialog(NewAccountDialog.this,
                  "Usernames must contain only letters and numbers.");
              return;
            }
            if (!EmailValidator.getInstance().isValid(emailTextField.getText())) {
              JOptionPane.showMessageDialog(NewAccountDialog.this,
                  "Please enter a valid email address.");
              return;
            }

            String errorMessage = rpcEngine.newAccount(
                emailTextField.getText(), usernameTextField.getText(),
                passwordOnceTextField.getText());
            if (errorMessage.length() == 0) {
              JOptionPane.showMessageDialog(NewAccountDialog.this,
                  "Account created!");
              LoginDialog id = (LoginDialog) NewAccountDialog.this.getParent();
              NewAccountDialog.this.dispose();
              Configuration conf = Utils.getConfiguration();
              conf.setProperty("loginId", usernameTextField.getText());
              id.loginComplete();
            } else {
              JOptionPane.showMessageDialog(NewAccountDialog.this,
                  "Account creation failed: " + errorMessage);
            }
          }
        });
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);
      }
      {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            NewAccountDialog.this.dispose();
          }
        });
        cancelButton.setActionCommand("Cancel");
        buttonPane.add(cancelButton);
      }
    }
  }

}
