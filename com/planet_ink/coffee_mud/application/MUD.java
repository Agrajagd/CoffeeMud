package com.planet_ink.coffee_mud.application;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.exceptions.*;
import com.planet_ink.coffee_mud.core.database.*;
import com.planet_ink.coffee_mud.core.http.*;
import com.planet_ink.coffee_mud.core.threads.*;
import com.planet_ink.coffee_mud.core.smtp.*;
import com.planet_ink.coffee_mud.core.intermud.*;
import com.planet_ink.coffee_mud.core.intermud.imc2.*;
import com.planet_ink.coffee_mud.core.intermud.server.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.io.PrintWriter; // for writing to sockets
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.sql.*;


/* 
   Copyright 2000-2007 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

public class MUD extends Thread implements MudHost
{
    private static final float HOST_VERSION_MAJOR=(float)5.2;
    private static final long  HOST_VERSION_MINOR=11;
	
    private static boolean bringDown=false;
    private static String execExternalCommand=null;
    private static Server imserver=null;
    private static IMC2Driver imc2server=null;
    private static SaveThread saveThread=null;
    private static UtiliThread utiliThread=null;
    private static Vector webServers=new Vector();
    private static SMTPserver smtpServerThread=null;
    private static DVector accessed=new DVector(2);
    private static Vector autoblocked=new Vector();
    private static Vector databases=new Vector();
    
    private static boolean serverIsRunning = false;
    
    protected static boolean isOK = false;
    protected boolean acceptConnections=false;
    protected String host="MyHost";
    protected int port=5555;
    
    
    private final static String[] STATE_STRING={"waiting","accepting","allowing"};
    private int state=0;
	ServerSocket servsock=null;

	public MUD(String name)
	{
		super(name);
	}

	public static void fatalStartupError(Thread t, int type)
	{
		String str=null;
		switch(type)
		{
		case 1:
			str="ERROR: initHost() will not run without properties. Exiting.";
			break;
		case 2:
			str="Map is empty?! Exiting.";
			break;
		case 3:
			str="Database init failed. Exiting.";
			break;
		case 4:
			str="Fatal exception. Exiting.";
			break;
		case 5:
			str="MUD Server did not start. Exiting.";
			break;
		default:
			str="Fatal error loading classes.  Make sure you start up coffeemud from the directory containing the class files.";
			break;
		}
		Log.errOut(Thread.currentThread().getName(),str);
        bringDown=true;
        CMProps.setBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN,true);
		CMLib.killThread(t,100,1);
	}


	protected static boolean initHost(Thread t, CMProps page)
	{

		if (!isOK)
		{
			CMLib.killThread(t,100,1);
			return false;
		}

		if ((page == null) || (!page.loaded))
		{
			fatalStartupError(t,1);
			return false;
		}

		while (!serverIsRunning && isOK)
		{
		}
		if (!isOK)
		{
			fatalStartupError(t,5);
			return false;
		}

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: connecting to database");
		
		page.resetSystemVars();
		
		Vector compress=CMParms.parseCommas(page.getStr("COMPRESS").toUpperCase(),true);
		CMProps.setBoolVar(CMProps.SYSTEMB_ITEMDCOMPRESS,compress.contains("ITEMDESC"));
		CMProps.setBoolVar(CMProps.SYSTEMB_MOBCOMPRESS,compress.contains("GENMOBS"));
		CMProps.setBoolVar(CMProps.SYSTEMB_ROOMDCOMPRESS,compress.contains("ROOMDESC"));
		CMProps.setBoolVar(CMProps.SYSTEMB_MOBDCOMPRESS,compress.contains("MOBDESC"));
		Resources.setCompression(compress.contains("RESOURCES"));
		Vector nocache=CMParms.parseCommas(page.getStr("NOCACHE").toUpperCase(),true);
		CMProps.setBoolVar(CMProps.SYSTEMB_MOBNOCACHE,nocache.contains("GENMOBS"));
		CMProps.setBoolVar(CMProps.SYSTEMB_ROOMDNOCACHE,nocache.contains("ROOMDESC"));
		
		DBConnector currentDBconnector=null;
		if((page.getStr("DBCLASS")!=null)&&(page.getStr("DBCLASS").length()>0))
		{
			currentDBconnector=new DBConnector(page.getStr("DBCLASS"),page.getStr("DBSERVICE"),page.getStr("DBUSER"),page.getStr("DBPASS"),page.getInt("DBCONNECTIONS"),page.getBoolean("DBREUSE"),true);
			currentDBconnector.reconnect();
	        CMLib.registerLibrary(new DBInterface(currentDBconnector));
	        
			DBConnection DBTEST=currentDBconnector.DBFetch();
			if(DBTEST!=null) currentDBconnector.DBDone(DBTEST);
			if((currentDBconnector.amIOk())&&(CMLib.database().isConnected()))
			{
				Log.sysOut(Thread.currentThread().getName(),"Database connection successful.");
				databases.addElement(currentDBconnector);
			}
			else
			{
				String DBerrors=currentDBconnector.errorStatus().toString();
				Log.errOut(Thread.currentThread().getName(),"Fatal database error: "+DBerrors);
				System.exit(-1);
			}
		}
		else
		if(CMLib.database()==null)
		{
			Log.errOut(Thread.currentThread().getName(),"No registered database!");
			System.exit(-1);
		}
		
		if(page.getStr("RUNWEBSERVERS").equalsIgnoreCase("true"))
		{
			HTTPserver webServerThread = new HTTPserver(CMLib.mud(0),"pub",0);
			webServerThread.start();
			webServers.addElement(webServerThread);
			int numToDo=webServerThread.totalPorts();
			while((--numToDo)>0)
			{
				webServerThread = new HTTPserver(CMLib.mud(0),"pub",numToDo);
				webServerThread.start();
				webServers.addElement(webServerThread);
			}
			webServerThread = new HTTPserver(CMLib.mud(0),"admin",0);
			webServerThread.start();
			webServers.addElement(webServerThread);
			numToDo=webServerThread.totalPorts();
			while((--numToDo)>0)
			{
				webServerThread = new HTTPserver(CMLib.mud(0),"admin",numToDo);
				webServerThread.start();
				webServers.addElement(webServerThread);
			}
			CMLib.registerLibrary(new ProcessHTTPrequest(null,(webServers.size()>0)?(HTTPserver)webServers.firstElement():null,null,true));
		}
		else
            CMLib.registerLibrary(new ProcessHTTPrequest(null,null,null,true));

		
		if(page.getStr("RUNSMTPSERVER").equalsIgnoreCase("true"))
		{
			smtpServerThread = new SMTPserver(CMLib.mud(0));
			smtpServerThread.start();
			CMLib.threads().startTickDown(smtpServerThread,Tickable.TICKID_EMAIL,60);
		}

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: loading base classes");
		if(!CMClass.loadClasses(page))
		{
			fatalStartupError(t,0);
			return false;
		}
        
        CMLib.lang().setLocale(CMLib.props().getStr("LANGUAGE"),CMLib.props().getStr("COUNTRY"));
        CMClass.globalClock().initializeINIClock(page);
        CMLib.factions().reloadFactions(CMProps.getVar(CMProps.SYSTEM_PREFACTIONS));
		CMSecurity.setSysOp(page.getStr("SYSOPMASK")); // requires all classes be loaded
		CMSecurity.parseGroups(page);

		int numChannelsLoaded=CMLib.channels().loadChannels(page.getStr("CHANNELS"),
													  page.getStr("ICHANNELS"),
													  page.getStr("IMC2CHANNELS"));
        int numJournalsLoaded=CMLib.journals().loadCommandJournals(page.getStr("COMMANDJOURNALS"));
		Log.sysOut(Thread.currentThread().getName(),"Channels loaded   : "+(numChannelsLoaded+numJournalsLoaded));

        CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: loading socials");
		CMLib.socials().clearAllSocials();
		if(CMLib.socials().num()==0)
			Log.errOut(Thread.currentThread().getName(),"WARNING: Unable to load socials from socials.txt!");
		else
			Log.sysOut(Thread.currentThread().getName(),"Socials loaded    : "+CMLib.socials().num());

		CMLib.database().DBReadAllClans();
		Log.sysOut(Thread.currentThread().getName(),"Clans loaded      : "+CMLib.clans().size());
		
		CMLib.threads().startTickDown(CMLib.factions(),Tickable.TICKID_MOB,10);

		Log.sysOut(Thread.currentThread().getName(),"Loading map...");
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: loading rooms....");
		CMLib.database().DBReadAllRooms(null);
		for(Enumeration a=CMLib.map().areas();a.hasMoreElements();)
		{
			Area A=(Area)a.nextElement();
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: filling map ("+A.Name()+")");
			A.fillInAreaRooms();
		}
		Log.sysOut(Thread.currentThread().getName(),"Mapped rooms      : "+CMLib.map().numRooms()+" in "+CMLib.map().numAreas()+" areas");
		CMLib.map().initStartRooms(page);
		CMLib.map().initDeathRooms(page);
		CMLib.map().initBodyRooms(page);
		

		if(!CMLib.map().roomIDs().hasMoreElements())
		{
			Log.sysOut("NO MAPPED ROOM?!  I'll make ya one!");
			String id="START";
			Area newArea=CMLib.database().DBCreateArea("New Area","StdArea");
			Room room=CMClass.getLocale("StdRoom");
			room.setRoomID(id);
			room.setArea(newArea);
			room.setDisplayText("New Room");
			room.setDescription("Brand new database room! You need to change this text with the MODIFY ROOM command.  If your character is not an Archon, pick up the book you see here and read it immediately!");
			CMLib.database().DBCreateRoom(room,"StdRoom");
			Item I=CMClass.getMiscMagic("ManualArchon");
			room.addItem(I);
			CMLib.database().DBUpdateItems(room);
		}

		CMLib.database().DBReadQuests(CMLib.mud(0));
		if(CMLib.quests().numQuests()>0)
			Log.sysOut(Thread.currentThread().getName(),"Quests loaded     : "+CMLib.quests().numQuests());

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: readying for connections.");
		try
		{
			saveThread=new SaveThread();
			saveThread.start();

			utiliThread=new UtiliThread();
			utiliThread.start();

			Log.sysOut(Thread.currentThread().getName(),"Utility threads started");
		}
		catch (Throwable th)
		{
			Log.sysOut(Thread.currentThread().getName(),"CoffeeMud Server initHost() failed");
			fatalStartupError(t,4);
			return false;
		}

		try
		{
			if(page.getBoolean("RUNI3SERVER"))
			{
				String playstate=page.getStr("I3STATE");
				if((playstate==null)||(playstate.length()==0))
					playstate="Open to the public";
				IMudInterface imud=new IMudInterface(CMProps.getVar(CMProps.SYSTEM_MUDNAME),
													 "CoffeeMud v"+CMProps.getVar(CMProps.SYSTEM_MUDVER),
                                                     CMLib.mud(0).getPort(),
													 playstate,
													 CMLib.channels().iChannelsArray());
				imserver=new Server();
				int i3port=page.getInt("I3PORT");
				if(i3port==0) i3port=27766;
				Server.start(CMProps.getVar(CMProps.SYSTEM_MUDNAME),i3port,imud);
			}
		}
		catch(Exception e)
		{
			if(imserver!=null) Server.shutdown();
			imserver=null;
		}

		
		try
		{
			if(page.getBoolean("RUNIMC2CLIENT"))
			{
				imc2server=new IMC2Driver();
				if(!imc2server.imc_startup(false,
										page.getStr("IMC2LOGIN").trim(),
										CMProps.getVar(CMProps.SYSTEM_MUDNAME),
										page.getStr("IMC2MYEMAIL").trim(),
										page.getStr("IMC2MYWEB").trim(),
										page.getStr("IMC2HUBNAME").trim(),
										page.getInt("IMC2HUBPORT"),
										page.getStr("IMC2PASS1").trim(),
										page.getStr("IMC2PASS2").trim(),
										imc2server.buildChannelMap(page.getStr("IMC2CHANNELS").trim())))
				{
					Log.errOut(Thread.currentThread().getName(),"IMC2 Failed to start!");
					imc2server=null;
				}
				else
				{
					CMLib.intermud().registerIMC2(imc2server);
					imc2server.start();
				}
			}
		}
		catch(Exception e)
		{
			Log.errOut("IMC2",e.getMessage());
		}

        for(int i=0;i<CMLib.hosts().size();i++)
            ((MudHost)CMLib.hosts().elementAt(i)).setAcceptConnections(true);
		Log.sysOut(Thread.currentThread().getName(),"Initialization complete.");
		CMProps.setBoolVar(CMProps.SYSTEMB_MUDSTARTED,true);
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"OK");
		return true;
	}


	public void run()
	{
		int q_len = 6;
		Socket sock=null;
		serverIsRunning = false;

		if (!isOK)	return;

		InetAddress bindAddr = null;

		if (CMProps.getIntVar(CMProps.SYSTEMI_MUDBACKLOG) > 0)
			q_len = CMProps.getIntVar(CMProps.SYSTEMI_MUDBACKLOG);

		if (CMProps.getVar(CMProps.SYSTEM_MUDBINDADDRESS).length() > 0)
		{
			try
			{
				bindAddr = InetAddress.getByName(CMProps.getVar(CMProps.SYSTEM_MUDBINDADDRESS));
			}
			catch (UnknownHostException e)
			{
				Log.errOut(Thread.currentThread().getName(),"ERROR: MUD Server could not bind to address " + CMProps.getVar(CMProps.SYSTEM_MUDBINDADDRESS));
				bindAddr = null;
			}
		}

		try
		{
			servsock=new ServerSocket(port, q_len, bindAddr);

			Log.sysOut(Thread.currentThread().getName(),"MUD Server started on port: "+port);
			if (bindAddr != null)
				Log.sysOut(Thread.currentThread().getName(),"MUD Server bound to: "+bindAddr.toString());
			serverIsRunning = true;

			while(true)
			{
                state=0;
                if(servsock==null) break;
				sock=servsock.accept();
				sock.setSoLinger(true,3);
                state=1;

				if (acceptConnections)
				{
					String address="unknown";
					try{address=sock.getInetAddress().getHostAddress().trim();}catch(Exception e){}
					Log.sysOut(Thread.currentThread().getName(),"Connection from "+address);
                    int proceed=0;
                    if(CMSecurity.isBanned(address))
                        proceed=1;
					int numAtThisAddress=0;
					long ConnectionWindow=(180*1000);
					long LastConnectionDelay=(5*60*1000);
					boolean anyAtThisAddress=false;
					int maxAtThisAddress=6;
                    if(!CMSecurity.isDisabled("CONNSPAMBLOCK"))
                    {
    					try{
    						for(int a=accessed.size()-1;a>=0;a--)
    						{
    							if((((Long)accessed.elementAt(a,2)).longValue()+LastConnectionDelay)<System.currentTimeMillis())
    								accessed.removeElementAt(a);
    							else
    							if(((String)accessed.elementAt(a,1)).trim().equalsIgnoreCase(address))
    							{
    								anyAtThisAddress=true;
    								if((((Long)accessed.elementAt(a,2)).longValue()+ConnectionWindow)>System.currentTimeMillis())
    									numAtThisAddress++;
    							}
    						}
    						if(autoblocked.contains(address.toUpperCase()))
    						{
    							if(!anyAtThisAddress)
    								autoblocked.remove(address.toUpperCase());
    							else
    								proceed=2;
    						}
    						else
    						if(numAtThisAddress>=maxAtThisAddress)
    						{
    							autoblocked.addElement(address.toUpperCase());
    							proceed=2;
    						}
    					}catch(java.lang.ArrayIndexOutOfBoundsException e){}
    
    					accessed.addElement(address,new Long(System.currentTimeMillis()));
                    }
                    
					if(proceed!=0)
					{
						Log.sysOut(Thread.currentThread().getName(),"Blocking a connection from "+address);
						PrintWriter out = new PrintWriter(sock.getOutputStream());
						out.println("\n\rOFFLINE: Blocked\n\r");
						out.flush();
						if(proceed==2)
							out.println("\n\rYour address has been blocked temporarily due to excessive invalid connections.  Please try back in "+(Math.round(LastConnectionDelay/60000))+" minutes, and not before.\n\r\n\r");
						else
							out.println("\n\rYou are unwelcome.  No one likes you here. Go away.\n\r\n\r");
						out.flush();
                        try{Thread.sleep(250);}catch(Exception e){}
						out.close();
						sock = null;
					}
					else
					{
                        state=2;
						StringBuffer introText=Resources.getFileResource("text/intro.txt",true);
                        Session S=(Session)CMClass.getCommon("DefaultSession");
                        S.initializeSession(sock, introText != null ? introText.toString() : null);
						S.start();
						CMLib.sessions().addElement(S);
						sock = null;
					}
				}
				else
                if((CMLib.database()!=null)&&(CMLib.database().isConnected())&&(CMLib.encoder()!=null))
				{
					StringBuffer rejectText=Resources.getFileResource("text/offline.txt",true);
					PrintWriter out = new PrintWriter(sock.getOutputStream());
					out.println("\n\rOFFLINE: " + CMProps.getVar(CMProps.SYSTEM_MUDSTATUS)+"\n\r");
					out.println(rejectText);
					out.flush();
                    try{Thread.sleep(1000);}catch(Exception e){}
					out.close();
					sock = null;
				}
                else
                {
                    sock.close();
                    sock = null;
                }
			}
		}
		catch(Throwable t)
		{
		    if((!(t instanceof java.net.SocketException))
		    ||(t.getMessage()==null)
		    ||(t.getMessage().toLowerCase().indexOf("socket closed")<0))
		    {
				Log.errOut(Thread.currentThread().getName(),t);
		    }

			if (!serverIsRunning)
				isOK = false;
		}

		Log.sysOut(Thread.currentThread().getName(),"CoffeeMud Server cleaning up.");

		try
		{
			if(servsock!=null)
				servsock.close();
			if(sock!=null)
				sock.close();
		}
		catch(IOException e)
		{
		}

		Log.sysOut(Thread.currentThread().getName(),"MUD on port "+port+" stopped!");
	}
    public String getStatus()
    {
        if(CMProps.getBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN))
            return CMProps.getVar(CMProps.SYSTEM_MUDSTATUS);
        if(!CMProps.getBoolVar(CMProps.SYSTEMB_MUDSTARTED))
            return CMProps.getVar(CMProps.SYSTEM_MUDSTATUS);
        return STATE_STRING[state];
    }

	public void shutdown(Session S, boolean keepItDown, String externalCommand)
	{
		globalShutdown(S,keepItDown,externalCommand);
		interrupt(); // kill the damn archon thread.
	}

	public static void defaultShutdown()
	{
		globalShutdown(null,true,null);
	}
	public static void globalShutdown(Session S, boolean keepItDown, String externalCommand)
	{
		CMProps.setBoolVar(CMProps.SYSTEMB_MUDSTARTED,false);
		CMProps.setBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN,true);
        CMLib.threads().suspendAll();
		if(S!=null)S.print("Closing MUD listeners to new connections...");
		for(int i=0;i<CMLib.hosts().size();i++)
            ((MudHost)CMLib.hosts().elementAt(i)).setAcceptConnections(false);
		Log.sysOut(Thread.currentThread().getName(),"New Connections are now closed");
		if(S!=null)S.println("Done.");
		
		if((!CMSecurity.isSaveFlag("NOPLAYERS"))
		&&(saveThread!=null))
		{
			if(S!=null)S.print("Saving players...");
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Saving players...");
            if(CMLib.sessions()!=null)
                for(int s=0;s<CMLib.sessions().size();s++)
                {
                    Session S2=CMLib.sessions().elementAt(s);
                    if((S2!=null)&&(S2.mob()!=null)&&(S2.mob().playerStats()!=null))
                        S2.mob().playerStats().setLastDateTime(System.currentTimeMillis());
                }
			saveThread.savePlayers();
            CMLib.coffeeTables().update();
			if(S!=null)S.println("done");
			Log.sysOut(Thread.currentThread().getName(),"All users saved.");
		}
        if(S!=null)S.print("Saving stats...");
        CMLib.coffeeTables().update();
        if(S!=null)S.println("done");
        Log.sysOut(Thread.currentThread().getName(),"Stats saved.");
		
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down" + (keepItDown? "..." : " and restarting..."));
		Log.sysOut(Thread.currentThread().getName(),"Notifying all objects of shutdown...");
		if(S!=null)S.print("Notifying all objects of shutdown...");
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Notifying Objects");
		MOB mob=null;
		if(S!=null) mob=S.mob();
		if(mob==null) mob=CMClass.getMOB("StdMOB");
		CMMsg msg=CMClass.getMsg(mob,null,CMMsg.MSG_SHUTDOWN,null);
		Vector roomSet=new Vector();
		try
		{
			for(Enumeration e=CMLib.map().areas();e.hasMoreElements();)
			{
			    Area A=(Area)e.nextElement();
			    A.toggleMobility(false);
			}
			for(Enumeration r=CMLib.map().rooms();r.hasMoreElements();)
			{
				Room R=(Room)r.nextElement();
				R.send(mob,msg);
				roomSet.addElement(R);
			}
	    }catch(NoSuchElementException e){}
		if(S!=null)S.println("done");
		if((saveThread==null)||(utiliThread==null))
		{
			CMProps.setBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN,false);
            CMLib.threads().resumeAll();
		    return;
		}

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Quests");
		CMLib.quests().shutdown();

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Save Thread");
		saveThread.shutdown();
		CMLib.killThread(saveThread,100,1);
		saveThread=null;
		
		if(S!=null)S.println("Save thread stopped");
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Utility Thread");
		utiliThread.shutdown();
		CMLib.killThread(utiliThread,100,1);
		utiliThread=null;
		if(S!=null)S.println("Utility thread stopped");
		Log.sysOut(Thread.currentThread().getName(),"Utility/Save Threads stopped.");
		
		if(CMSecurity.isSaveFlag("ROOMMOBS")
		||CMSecurity.isSaveFlag("ROOMITEMS")
		||CMSecurity.isSaveFlag("ROOMSHOPS"))
		{
			if(S!=null)S.print("Saving room data...");
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Rejuving the dead");
			CMLib.threads().tickAllTickers(null);
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Map Update");
			for(Enumeration e=CMLib.map().areas();e.hasMoreElements();)
				((Area)e.nextElement()).toggleMobility(false);
			int roomCounter=0;
			Room R=null;
			for(Enumeration e=roomSet.elements();e.hasMoreElements();)
			{
			    if(((++roomCounter)%200)==0)
			    {
			        if(S!=null) S.print(".");
					CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Map Update ("+roomCounter+")");
			    }
			    R=(Room)e.nextElement();
			    if(R.roomID().length()>0)
			    	R.executeMsg(mob,CMClass.getMsg(mob,R,null,CMMsg.MSG_EXPIRE,null));
			}
			if(S!=null)S.println("done");
			Log.sysOut(Thread.currentThread().getName(),"Map data saved.");
		    
		}

		if(imserver!=null)
		{
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...I3Server");
			Server.shutdown();
			imserver=null;
			if(S!=null)S.println("I3Server stopped");
			Log.sysOut(Thread.currentThread().getName(),"I3Server stopped");
		}

		if(imc2server!=null)
		{
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...IMC2Server");
			imc2server.shutdown();
			imc2server=null;
			if(S!=null)S.println("IMC2Server stopped");
			Log.sysOut(Thread.currentThread().getName(),"IMC2Server stopped");
		}
		
		if(S!=null)S.print("Stopping player Sessions...");
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Stopping sessions");
		while(CMLib.sessions().size()>0)
		{
			Session S2=CMLib.sessions().elementAt(0);
			if((S!=null)&&(S2==S))
				CMLib.sessions().removeElementAt(0);
			else
			{
				CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Stopping session "+S2.getAddress());
				S2.logoff(true);
				CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Done stopping session "+S2.getAddress());
			}
			if(S!=null)S.print(".");
		}
		if(S!=null)S.println("All users logged off");
        try{Thread.sleep(3000);}catch(Exception e){/* give sessions a few seconds to inform the map */}
		Log.sysOut(Thread.currentThread().getName(),"All users logged off.");

		if(smtpServerThread!=null)
		{
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...smtp server");
			smtpServerThread.shutdown(S);
			smtpServerThread = null;
			Log.sysOut(Thread.currentThread().getName(),"SMTP Server stopped.");
			if(S!=null)S.println("SMTP Server stopped");
		}
		
		if(S!=null)S.print("Stopping all threads...");
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...shutting down service engine");
		CMLib.threads().shutdownAll();
		if(S!=null)S.println("done");
		Log.sysOut(Thread.currentThread().getName(),"Map Threads Stopped.");

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...closing db connections");
		for(int d=0;d<databases.size();d++)
			((DBConnector)databases.elementAt(d)).killConnections();
		if(S!=null)S.println("Database connections closed");
		Log.sysOut(Thread.currentThread().getName(),"Database connections closed.");

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...Clearing socials, clans, channels");
		CMLib.socials().clearAllSocials();
		CMLib.clans().shutdownClans();
        CMLib.channels().unloadChannels();
        CMLib.journals().unloadCommandJournals();
        CMLib.polls().unload();

		CMLib.help().unloadHelpFile(null);

		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...unloading classes");
		CMClass.unload();
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...unloading map");
		CMLib.map().unLoad();
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...unloading resources");
		Resources.clearResources();
		Log.sysOut(Thread.currentThread().getName(),"Resources Cleared.");
		if(S!=null)S.println("All resources unloaded");


		for(int i=0;i<webServers.size();i++)
		{
			HTTPserver webServerThread=(HTTPserver)webServers.elementAt(i);
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down web server "+webServerThread.getName()+"...");
			webServerThread.shutdown(S);
			Log.sysOut(Thread.currentThread().getName(),"Web server "+webServerThread.getName()+" stopped.");
			if(S!=null)S.println("Web server "+webServerThread.getName()+" stopped");
		}
		webServers.clear();
		
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down...unloading macros");
		CMLib.lang().clear();
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutting down" + (keepItDown? "..." : " and restarting..."));

		try{Thread.sleep(500);}catch(Exception i){}
		Log.sysOut(Thread.currentThread().getName(),"CoffeeMud shutdown complete.");
		if(S!=null)S.println("CoffeeMud shutdown complete.");
		bringDown=keepItDown;
        CMLib.threads().resumeAll();
		if(!keepItDown)
			if(S!=null)S.println("Restarting...");
        if(S!=null)S.logoff(false);
		try{Thread.sleep(500);}catch(Exception i){}
		System.gc();
		System.runFinalization();
		try{Thread.sleep(500);}catch(Exception i){}

		execExternalCommand=externalCommand;
		CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Shutdown: you are the special lucky chosen one!");
		for(int m=CMLib.hosts().size()-1;m>=0;m--)
            if(CMLib.hosts().elementAt(m) instanceof Thread)
    			CMLib.killThread((Thread)CMLib.hosts().elementAt(m),100,1);
		if(!keepItDown)
			CMProps.setBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN,false);
	}


	public void interrupt()
	{
		if(servsock!=null)
		{
			try
			{
				servsock.close();
				servsock = null;
			}
			catch(IOException e)
			{
			}
		}
		super.interrupt();
	}

	public static int activeThreadCount(ThreadGroup tGroup)
	{
		int realAC=0;
		int ac = tGroup.activeCount();
		Thread tArray[] = new Thread [ac+1];
		tGroup.enumerate(tArray);
		for (int i = 0; i<ac; ++i)
		{
			if (tArray[i] != null && tArray[i].isAlive())
				realAC++;
		}
		return realAC;
	}

	public static int killCount(ThreadGroup tGroup, Thread thisOne)
	{
		int killed=0;

		int ac = tGroup.activeCount();
		Thread tArray[] = new Thread [ac+1];
		tGroup.enumerate(tArray);
		for (int i = 0; i<ac; ++i)
		{
			if (tArray[i] != null && tArray[i].isAlive() && (tArray[i] != thisOne))
			{
				CMLib.killThread(tArray[i],500,1);
				killed++;
			}
		}
		return killed;
	}

	public static void threadList(ThreadGroup tGroup)
	{
		int ac = tGroup.activeCount();
		Thread tArray[] = new Thread [ac+1];
		tGroup.enumerate(tArray);
		for (int i = 0; i<ac; ++i)
		{
			if (tArray[i] != null && tArray[i].isAlive())
			{
			    if(tArray[i] instanceof Session)
			    {
			        Session S=(Session)tArray[i];
					Log.sysOut(Thread.currentThread().getName(), "-->Thread: Session status "+S.getStatus()+"-"+CMParms.combine(S.previousCMD(),0) + "\n\r");
			    }
			    else
			    if(tArray[i] instanceof Tickable)
			    {
			        Tickable T=(Tickable)tArray[i];
					Log.sysOut(Thread.currentThread().getName(), "-->Thread: "+T.ID()+"-"+T.name()+"-"+T.getTickStatus() + "\n\r");
			    }
			    else
                if((tArray[i] instanceof Tick)
                &&(((Tick)tArray[i]).lastClient!=null)
                &&(((Tick)tArray[i]).lastClient.clientObject!=null))
                    Log.sysOut(Thread.currentThread().getName(), "-->Thread: "+tArray[i].getName()+" "+((Tick)tArray[i]).lastClient.clientObject.ID()+"-"+((Tick)tArray[i]).lastClient.clientObject.name()+"-"+((Tick)tArray[i]).lastClient.clientObject.getTickStatus() + "\n\r");
                else
					Log.sysOut(Thread.currentThread().getName(), "-->Thread: "+tArray[i].getName() + "\n\r");
			}
		}
	}

	public String getHost()
	{
		return host;
	}
	public int getPort()
	{
		return port;
	}

	private static class HostGroup extends Thread
	{
		private static int grpid=0;
		private String name=null;
		private String iniFile=null;
		private String logName=null;
		public HostGroup(ThreadGroup G, String mudName, String iniFileName)
		{
			super(G,"HOST"+grpid);
			logName="mud"+((grpid>0)?("."+grpid):"");
			grpid++;
			iniFile=iniFileName;
			name=mudName;
			setDaemon(true);
		}
		
		public void run()
		{
			CMProps page=CMProps.loadPropPage("//"+iniFile);
			CMProps.setBoolVar(CMProps.SYSTEMB_MUDSTARTED,false);
			CMProps.setVar(CMProps.SYSTEM_MUDVER,HOST_VERSION_MAJOR + "." + HOST_VERSION_MINOR);
			DBConnector currentDBconnector=new DBConnector();
	        CMLib.registerLibrary(new DBInterface(currentDBconnector));
	        CMLib.registerLibrary(new ServiceEngine());
	        CMLib.registerLibrary(new IMudClient());
			CMLib.registerLibrary(new ProcessHTTPrequest(null,null,null,true));
			if ((page==null)||(!page.loaded))
			{
				Log.errOut(Thread.currentThread().getName(),"ERROR: Unable to read ini file: '"+iniFile+"'.");
				System.out.println("MUD/ERROR: Unable to read ini file: '"+iniFile+"'.");
				CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"A terminal error has occured!");
				return;
			}
			
			if(!logName.equals("mud"))
			{
				Log.startLogFiles(logName,page.getInt("NUMLOGS"));
				Log.setLogOutput(page.getStr("SYSMSGS"),page.getStr("ERRMSGS"),page.getStr("WRNMSGS"),page.getStr("DBGMSGS"),page.getStr("HLPMSGS"),page.getStr("KILMSGS"),page.getStr("CBTMSGS"));
			}
			CMProps.setVar(CMProps.SYSTEM_INIPATH,iniFile,false);
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDNAME,name);
			try
			{
				isOK = true;
				CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting");
				CMProps.setVar(CMProps.SYSTEM_MUDBINDADDRESS,page.getStr("BIND"));
				CMProps.setIntVar(CMProps.SYSTEMI_MUDBACKLOG,page.getInt("BACKLOG"));

				if(MUD.isOK)
				{
					CMLib.hosts().clear();
					String ports=page.getProperty("PORT");
					int pdex=ports.indexOf(",");
					while(pdex>0)
					{
						MUD mud=new MUD("MUD@"+ports.substring(0,pdex));
						mud.acceptConnections=false;
						mud.port=CMath.s_int(ports.substring(0,pdex));
						ports=ports.substring(pdex+1);
						mud.start();
                        CMLib.hosts().addElement(mud);
						pdex=ports.indexOf(",");
					}
					MUD mud=new MUD("MUD@"+ports);
					mud.acceptConnections=false;
					mud.port=CMath.s_int(ports);
					mud.start();
                    CMLib.hosts().addElement(mud);
				}

				StringBuffer str=new StringBuffer("");
				for(int m=0;m<CMLib.hosts().size();m++)
				{
					MudHost mud=(MudHost)CMLib.hosts().elementAt(m);
					str.append(" "+mud.getPort());
				}
				CMProps.setVar(CMProps.SYSTEM_MUDPORTS,str.toString());
				
		        Runtime.getRuntime().addShutdownHook(new Thread() {
		            public void run() {
		            	if(!CMProps.getBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN))
		            		MUD.globalShutdown(null,true,null);
		            }
		        });
		        
				if(initHost(Thread.currentThread(),page))
                {
                    Thread joinable=null;
                    for(int i=0;i<CMLib.hosts().size();i++)
                        if(CMLib.hosts().elementAt(i) instanceof Thread)
                        {
                            joinable=(Thread)CMLib.hosts().elementAt(i);
                            break;
                        }
                    if(joinable!=null)
                        joinable.join();
                    else
                        System.exit(-1);
                }
			}
			catch(InterruptedException e)
			{
				Log.errOut(Thread.currentThread().getName(),e);
			}
		}
	}
	
	public static void main(String a[])
	{
		String nameID="";
		Vector iniFiles=CMParms.makeVector();
		if(a.length>0)
		{
			for(int i=0;i<a.length;i++)
				nameID+=" "+a[i];
			nameID=nameID.trim();
			Vector V=CMParms.paramParse(nameID);
			for(int v=0;v<V.size();v++)
			{
				String s=(String)V.elementAt(v);
				if(s.toUpperCase().startsWith("BOOT=")&&(s.length()>5))
				{
					iniFiles.addElement(s.substring(5));
					V.removeElementAt(v);
					v--;
				}
			}
			nameID=CMParms.combine(V,0);
		}
		if(iniFiles.size()==0) iniFiles.addElement("coffeemud.ini");
		if(nameID.length()==0) nameID="Unnamed CoffeeMud";
		String iniFile=(String)iniFiles.firstElement();
		CMProps page=CMProps.loadPropPage("//"+iniFile);
		if ((page==null)||(!page.loaded))
		{
			Log.startLogFiles("mud",1);
			Log.setLogOutput("BOTH","BOTH","BOTH","BOTH","BOTH","BOTH","BOTH");
			Log.errOut(Thread.currentThread().getName(),"ERROR: Unable to read ini file: '"+iniFile+"'.");
			System.out.println("MUD/ERROR: Unable to read ini file: '"+iniFile+"'.");
			CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"A terminal error has occured!");
			System.exit(-1);
		}
		Log.startLogFiles("mud",page.getInt("NUMLOGS"));
		Log.setLogOutput(page.getStr("SYSMSGS"),page.getStr("ERRMSGS"),page.getStr("WRNMSGS"),page.getStr("DBGMSGS"),page.getStr("HLPMSGS"),page.getStr("KILMSGS"),page.getStr("CBTMSGS"));
		while(!bringDown)
		{
			System.out.println();
			Log.sysOut(Thread.currentThread().getName(),"CoffeeMud v"+HOST_VERSION_MAJOR + "." + HOST_VERSION_MINOR);
			Log.sysOut(Thread.currentThread().getName(),"(C) 2000-2007 Bo Zimmerman");
			Log.sysOut(Thread.currentThread().getName(),"http://coffeemud.zimmers.net");
			HostGroup joinable=null;
			for(int i=0;i<iniFiles.size();i++)
			{
				iniFile=(String)iniFiles.elementAt(i);
				ThreadGroup G=new ThreadGroup(i+"-MUD");
				HostGroup H=new HostGroup(G,nameID,iniFile);
				H.start();
				if(joinable==null) joinable=H;
			}
			try{joinable.join();}catch(Exception e){e.printStackTrace(); Log.errOut(Thread.currentThread().getName(),e); }
			System.gc();
	        try{Thread.sleep(1000);}catch(Exception e){}
			System.runFinalization();
	        try{Thread.sleep(1000);}catch(Exception e){}
	        
			if(activeThreadCount(Thread.currentThread().getThreadGroup())>1)
			{
				try{ Thread.sleep(1000);}catch(Exception e){}
				killCount(Thread.currentThread().getThreadGroup(),Thread.currentThread());
				if(activeThreadCount(Thread.currentThread().getThreadGroup())>1)
				{
					Log.sysOut(Thread.currentThread().getName(),"WARNING: " + activeThreadCount(Thread.currentThread().getThreadGroup()) +" other thread(s) are still active!");
					threadList(Thread.currentThread().getThreadGroup());
				}
			}
			if(!bringDown)
			{
				if(execExternalCommand!=null)
				{
					//Runtime r=Runtime.getRuntime();
					//Process p=r.exec(external);
					Log.sysOut("Attempted to execute '"+execExternalCommand+"'.");
					execExternalCommand=null;
					bringDown=true;
				}
			}
		}
	}
    
    public void setAcceptConnections(boolean truefalse){ acceptConnections=truefalse;}
    
    public String executeCommand(String cmd)
        throws Exception
    {
        Vector V=CMParms.parse(cmd);
        if(V.size()==0) throw new CMException("Unknown command!");
        String word=(String)V.firstElement();
        if(word.equalsIgnoreCase("FORCE"))
        {
            String rest=CMParms.combine(V,1);
            if(rest.equalsIgnoreCase("SAVETHREAD"))
                saveThread.forceTick();
            else
            if(rest.equalsIgnoreCase("UTILITHREAD"))
                utiliThread.forceTick();
            else
                throw new CMException("Unknown parm: "+rest);
        }
        else
            throw new CMException("Unknown command: "+word);
        return "OK";
    }
}
