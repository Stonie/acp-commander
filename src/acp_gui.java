import java.awt.*;
import java.awt.event.*;


public class acp_gui extends Frame implements WindowListener, ActionListener, ItemListener {

	TextArea textOutput;
	
	TextField commandTextField, targetHostTextField, targetPortTextField, localMacTextField, idTextField;
	
	Button executeCommandButton, openBoxButton, connectButton;
	
	Choice boxtypeChoice;
	
	Checkbox quietModeCheckbox, idCheckbox, noAuthenticationCheckbox, noDebug_Checkbox, debug1_Checkbox, debug2_Checkbox;
	CheckboxGroup debugCheckboxGroup;
		
	Panel boxtypePanel, commandPanel, settings, options, openBoxPanel, debugLevel , macPanel;

	Label boxtypeTitle, debugTitle, settingsTitle, targetHostLabel, targetPortLabel, localMacLabel, noAuthenticationLabel, openBoxTitle, openBoxDescription, optionsTitle;

	Font header, normal;

   public acp_gui(){
   	super("ACP_Commander");
   	
   	createLayoutMain();
   	pack();
   	addWindowListener(this);
   	
   	setVisible(true);
   }

   private void createLayoutMain(){
   		
   		
   	setBackground(java.awt.Color.lightGray);	
   		
   	GridBagLayout mainGBL = new GridBagLayout();
   	GridBagConstraints mainGBC = new GridBagConstraints();  
   		
   	setLayout(mainGBL);	
   		
   	//===== boxtypePanel ===============================	
   		
   	boxtypePanel = new Panel();	
   	boxtypePanel.setLayout(new GridLayout(3,1));
   	
   	GridBagLayout boxtypeGBL = new GridBagLayout();
   	GridBagConstraints boxtypeGBC = new GridBagConstraints();  
   		
   	boxtypePanel.setLayout(boxtypeGBL);	
   	   	
   	boxtypeTitle = new Label("BOXTYPE");
   		
   	boxtypeChoice = new Choice();
   	boxtypeChoice.add("LS Pro (LS-GL)");	
   	boxtypeChoice.addItemListener(this);
   	
   	connectButton = new Button("Connect!");
   	connectButton.addActionListener(this);
   	
   	buildConstraints(boxtypeGBC,0,0,1,1,1,1,GridBagConstraints.NORTH);
   	boxtypeGBL.setConstraints(boxtypeTitle, boxtypeGBC);
   	boxtypePanel.add(boxtypeTitle);	
   	buildConstraints(boxtypeGBC,0,1,1,1,1,1,GridBagConstraints.CENTER);
   	boxtypeGBL.setConstraints(boxtypeChoice, boxtypeGBC);
   	boxtypePanel.add(boxtypeChoice);	
   	buildConstraints(boxtypeGBC,0,2,1,1,1,1,GridBagConstraints.SOUTH);
   	boxtypeGBL.setConstraints(connectButton, boxtypeGBC);
   	boxtypePanel.add(connectButton);	
   	
   	//== add to main ==
   	buildConstraints(mainGBC,0,0,1,1,1,1,GridBagConstraints.NORTH);
   	mainGBL.setConstraints(boxtypePanel, mainGBC);
   	add(boxtypePanel);
   		
   	//===== textOutput ===============================================
   	textOutput = new TextArea("Welcome to ACP-Commander...\nhttp://www.linkstationwiki.net ",12, 80);
   	
   	//== add to main ==
   	buildConstraints(mainGBC,0,1,4,1,1,1,GridBagConstraints.CENTER);
   	mainGBL.setConstraints(textOutput, mainGBC);
   	add(textOutput);
   	
   	//===== commandExecution =========================================
   	commandPanel = new Panel();
   	
   	GridBagLayout commandGBL = new GridBagLayout();
   	GridBagConstraints commandGBC = new GridBagConstraints();  
   		
   	commandPanel.setLayout(commandGBL);	
   	   	
   	commandTextField = new TextField("", 60);
   	executeCommandButton = new Button("Execute Command!");
   	executeCommandButton.addActionListener(this);
   	
   	// build command-panel
   	buildConstraints(commandGBC,0,0,1,1,2,1,GridBagConstraints.CENTER);
   	mainGBL.setConstraints(commandTextField, commandGBC);
   	commandPanel.add(commandTextField);
   	buildConstraints(commandGBC,0,1,1,1,1,1,GridBagConstraints.CENTER);
   	mainGBL.setConstraints(executeCommandButton, commandGBC);
   	commandPanel.add(executeCommandButton);
   	
   	//== add to main ==
   	buildConstraints(mainGBC,0,2,4,1,1,1,GridBagConstraints.CENTER);
   	mainGBL.setConstraints(commandPanel, mainGBC);
   	add(commandPanel);
   	
   	  	
   	//===== settings =================================================
   	settings = new Panel();
   	settingsTitle = new Label("BASIC SETTINGS");
   	
   	targetHostLabel = new Label("Target:");
   	targetHostTextField = new TextField("", 15);
   	targetPortLabel = new Label("Port:");
   	targetPortTextField = new TextField("22936", 5);
   	
   	GridBagLayout settingsGBL = new GridBagLayout();
   	GridBagConstraints settingsGBC = new GridBagConstraints();
   	
   	settings.setLayout(settingsGBL);
   	
   	//build settings-panel
   	buildConstraints(settingsGBC,0,0,2,1,1,1,GridBagConstraints.WEST);
   	settingsGBL.setConstraints(settingsTitle, settingsGBC);
   	settings.add(settingsTitle);
   	buildConstraints(settingsGBC,0,1,1,1,1,1,GridBagConstraints.WEST);
   	settingsGBL.setConstraints(targetHostLabel, settingsGBC);
   	settings.add(targetHostLabel);
   	buildConstraints(settingsGBC,1,1,1,1,1,1,GridBagConstraints.CENTER);
   	settingsGBL.setConstraints(targetHostTextField, settingsGBC);
   	settings.add(targetHostTextField);
   	buildConstraints(settingsGBC,0,2,1,1,1,1,GridBagConstraints.WEST);
   	settingsGBL.setConstraints(targetPortLabel, settingsGBC);
   	settings.add(targetPortLabel);
   	buildConstraints(settingsGBC,1,2,1,1,1,1,GridBagConstraints.WEST);
   	settingsGBL.setConstraints(targetPortTextField, settingsGBC);
   	settings.add(targetPortTextField);
   	
   	//== add to main ==
   	buildConstraints(mainGBC,1,0,1,1,1,1,GridBagConstraints.NORTHWEST);
   	mainGBL.setConstraints(settings, mainGBC);
   	add(settings);
   	 	
   	   	   	
   	   	
   	//===== debug ===================================================== 
   	debugLevel = new Panel();
   	
   	debugTitle = new Label("DEBUG");
   	
   	debugCheckboxGroup = new CheckboxGroup();
    	noDebug_Checkbox = new Checkbox("No debug", debugCheckboxGroup, true);
   	debug1_Checkbox = new Checkbox("Level 1", debugCheckboxGroup, false);
   	debug2_Checkbox = new Checkbox("Level 2", debugCheckboxGroup, false);
   	  	
   	debugLevel.setLayout(new GridLayout(4,1));
   	debugLevel.add(debugTitle);
   	debugLevel.add(noDebug_Checkbox);
   	debugLevel.add(debug1_Checkbox);
   	debugLevel.add(debug2_Checkbox);
   	
   	//== add to main ==
   	buildConstraints(mainGBC,3,0,1,1,1,1,GridBagConstraints.NORTHWEST);
   	mainGBL.setConstraints(debugLevel, mainGBC);
   	add(debugLevel);
   	
   	//===== Openbox ===================================================
   	openBoxPanel = new Panel();
   	
   	openBoxTitle = new Label("OPENBOX");
   	openBoxDescription = new Label("send 'telnet' and 'passwd -d root'");
    	openBoxButton = new Button("OpenBox");
   	openBoxButton.addActionListener(this);
   	
   	openBoxPanel.setLayout(new GridLayout(3,1));
   	openBoxPanel.add(openBoxTitle);
   	openBoxPanel.add(openBoxDescription);
   	openBoxPanel.add(openBoxButton);
   	
   	//== add to main ==
   	buildConstraints(mainGBC,1,3,1,1,1,1,GridBagConstraints.CENTER);
   	mainGBL.setConstraints(openBoxPanel, mainGBC);
   	add(openBoxPanel);
   	
   	//===== options ====================================================
   	options = new Panel();
   	macPanel = new Panel();
   	
   	optionsTitle = new Label("OPTIONS");
   	quietModeCheckbox = new Checkbox("Quiet Mode", false); 
   	idCheckbox = new Checkbox("use custom ID:", false);
   	idTextField = new TextField("", 10);
   	localMacLabel = new Label("MAC:");
   	localMacTextField = new TextField("FF:FF:FF:FF:FF:FF",17);
   	
   	//macPanel
   	GridBagLayout macPanelGBL = new GridBagLayout();
   	GridBagConstraints macPanelGBC = new GridBagConstraints();
   	
   	macPanel.setLayout(macPanelGBL);
   	
   	buildConstraints(macPanelGBC,0,0,1,1,1,1,GridBagConstraints.WEST);
   	macPanelGBL.setConstraints(localMacLabel, macPanelGBC);
   	macPanel.add(localMacLabel);
   	buildConstraints(macPanelGBC,1,0,1,1,1,1,GridBagConstraints.WEST);
   	macPanelGBL.setConstraints(localMacTextField, macPanelGBC);
   	macPanel.add(localMacTextField);
   	
   	//options - panel
   	GridBagLayout optionsGBL = new GridBagLayout();
   	GridBagConstraints optionsGBC = new GridBagConstraints();
   	
   	options.setLayout(optionsGBL);
   	
   	buildConstraints(optionsGBC,0,0,2,1,1,1,GridBagConstraints.WEST);
   	optionsGBL.setConstraints(optionsTitle, optionsGBC);
   	options.add(optionsTitle);
   	buildConstraints(optionsGBC,0,1,1,1,1,1,GridBagConstraints.WEST);
   	optionsGBL.setConstraints(quietModeCheckbox, optionsGBC);
   	options.add(quietModeCheckbox);
     	buildConstraints(optionsGBC,0,2,1,1,1,1,GridBagConstraints.WEST);
   	optionsGBL.setConstraints(idCheckbox, optionsGBC);
   	options.add(idCheckbox);
   	buildConstraints(optionsGBC,1,2,1,1,1,1,GridBagConstraints.WEST);
   	optionsGBL.setConstraints(idTextField, optionsGBC);
   	options.add(idTextField);
   	buildConstraints(optionsGBC,0,3,2,1,1,1,GridBagConstraints.WEST);
   	optionsGBL.setConstraints(macPanel, optionsGBC);
   	options.add(macPanel);
   	
   	//== add to main ==
   	buildConstraints(mainGBC,2,0,1,1,1,1,GridBagConstraints.NORTHWEST);
   	mainGBL.setConstraints(options, mainGBC);
   	add(options);
   	
   	
   	
   }	

	private void buildConstraints(GridBagConstraints gbc,int x, int y, int width, int height, int weightx, int weighty, int anchor) {
	
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.gridheight = height;
		gbc.weightx = weightx;
		gbc.weighty = weighty;
		gbc.anchor = anchor;
		gbc.insets = new Insets(4, 4, 4, 4);
	
   }

  	public static void main (String[] args){
  	
  		new acp_gui();
  	
   }
   //================== ActionListener ==============================
	
	public void actionPerformed (ActionEvent e){
		
		String cmd = e.getActionCommand();
		System.out.println(cmd);
		
		
	}
	   
   //================== ActionListener - END ========================

	//================== ItemListener ================================
	
	public void itemStateChanged(ItemEvent e) {
    	
    	String cmd = e.paramString();
      System.out.println(cmd);
    	
	}
	
	//================== ItemListener - END ================================

   //================== WindowListener ==============================
   
   public void windowClosing(WindowEvent e){
      System.exit(0);	
   }
   public void windowActivated (WindowEvent e) {} 
   public void windowOpened (WindowEvent e) {} 
   public void windowClosed (WindowEvent e) {} 
   public void windowDeactivated (WindowEvent e) {} 
   public void windowIconified (WindowEvent e) {} 
   public void windowDeiconified (WindowEvent e) {} 
   
   
   //================== WindowListener - END ========================


}