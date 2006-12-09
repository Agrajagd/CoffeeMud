package com.planet_ink.coffee_mud.Common;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
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
import com.jcraft.jzlib.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.net.*;



/*
   Copyright 2000-2006 Bo Zimmerman

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
public class DefaultSession extends Thread implements Session
{
	protected int status=0;
    protected Socket sock;
    protected BufferedReader in;
    protected PrintWriter out;
    protected OutputStream rawout;
	protected MOB mob;
	protected boolean killFlag=false;
	protected boolean needPrompt=false;
	protected boolean afkFlag=false;
    protected String afkMessage=null;
    protected StringBuffer input=new StringBuffer("");
    private StringBuffer preliminaryInput=new StringBuffer("");
    private StringBuffer fakeInput=null;
	protected boolean waiting=false;
    protected static final int SOTIMEOUT=300;
	protected Vector previousCmd=new Vector();
    protected String[] clookup=null;
	protected String lastColorStr="";
	protected String lastStr=null;
	protected int spamStack=0;
	protected Vector snoops=new Vector();
	protected Vector prevMsgs=new Vector();
	protected StringBuffer curPrevMsg=null;
	
	protected boolean lastWasCR=false;
	protected boolean lastWasLF=false;
	protected boolean suspendCommandLine=false;

    private long lastStart=System.currentTimeMillis();
    private long lastStop=System.currentTimeMillis();
    private long lastLoopTop=System.currentTimeMillis();
    private long onlineTime=System.currentTimeMillis();
    private long lastPKFight=0;
    private long lastNPCFight=0;
    private long lastBlahCheck=0;
    private long milliTotal=0;
    private long tickTotal=0;
    private long lastKeystroke=0;
    private long promptLastShown=0;

    private boolean[] serverTelnetCodes=new boolean[256];
    private boolean[] clientTelnetCodes=new boolean[256];
    protected String terminalType="UNKNOWN";
    protected int terminalWidth = 80;
    protected int terminalHeight = 25;
    protected long writeStartTime=0;

    private final HashSet telnetSupportSet=new HashSet();
    private static final HashSet mxpSupportSet=new HashSet();
    private static final Hashtable mxpVersionInfo=new Hashtable();
    private boolean bNextByteIs255=false;
    private boolean connectionComplete=false;
    
    private int currentColor='N';
    private int lastColor=-1;
    protected static int sessionCounter=0;
    
    public String ID(){return "DefaultSession";}
    public CMObject newInstance(){try{return (CMObject)getClass().newInstance();}catch(Exception e){return new DefaultSession();}}
    public void initializeClass(){}
    public CMObject copyOf(){ try{ Object O=this.clone(); return (CMObject)O;}catch(Exception e){return newInstance();} }
    public int compareTo(Object o){ return CMClass.classID(this).compareToIgnoreCase(CMClass.classID(o));}

    public DefaultSession()
    {
        super("DefaultSession."+sessionCounter);
        ++sessionCounter;
    }
    
	public void initializeSession(Socket s, String introTextStr)
	{
		sock=s;
		try
		{
			sock.setSoTimeout(SOTIMEOUT);
            rawout=sock.getOutputStream();
            InputStream rawin=sock.getInputStream();
			setServerTelnetMode(TELNET_ANSI,true);
			setClientTelnetMode(TELNET_ANSI,true);
			setClientTelnetMode(TELNET_TERMTYPE,true);
			requestSubOption(rawout,TELNET_TERMTYPE);
			if(!CMSecurity.isDisabled("MCCP"))
			    changeTelnetMode(rawout,TELNET_COMPRESS2,true);
			
			if(!CMSecurity.isDisabled("MXP"))
			    changeTelnetMode(rawout,TELNET_MXP,true);
			if(!CMSecurity.isDisabled("MSP"))
			    changeTelnetMode(rawout,TELNET_MSP,true);
			//changeTelnetMode(rawout,TELNET_SUPRESS_GO_AHEAD,true);
			changeTelnetMode(rawout,TELNET_NAWS,true);
			//changeTelnetMode(rawout,TELNET_BINARY,true);
			preliminaryRead(250);
			if((!terminalType.equalsIgnoreCase("ANSI"))&&(clientTelnetMode(TELNET_ECHO)))
			    changeTelnetModeBackwards(TELNET_ECHO,false);
			rawout.flush();
			preliminaryRead(250);
			
            //out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(rawout, "UTF-8")));
            //in = new BufferedReader(new InputStreamReader(rawin, "UTF-8"));
			out = new PrintWriter(new OutputStreamWriter(rawout,"iso-8859-1"));
			in = new BufferedReader(new InputStreamReader(rawin,"iso-8859-1"));
			
			preliminaryRead(250);
			if(clientTelnetMode(TELNET_COMPRESS2))
			{
			    out.flush();
			    rawout.flush();
				try{Thread.sleep(50);}catch(Exception e){}
			    requestSubOption(rawout,TELNET_COMPRESS2);
			    ZOutputStream zOut=new ZOutputStream(rawout, JZlib.Z_DEFAULT_COMPRESSION);
			    zOut.setFlushMode(JZlib.Z_SYNC_FLUSH);
				out = new PrintWriter(new OutputStreamWriter(zOut,"iso-8859-1"));
				try{Thread.sleep(50);}catch(Exception e){}
			}
			if(clientTelnetMode(Session.TELNET_MXP))
			    print("\n\033[6z\n<SUPPORT IMAGE IMAGE.URL>\n");
			preliminaryRead(500);
			if(introTextStr!=null) 
                print(introTextStr);
            if((clientTelnetMode(Session.TELNET_MXP))
            &&((mxpSupportSet.contains("+IMAGE.URL"))
                ||((mxpSupportSet.contains("+IMAGE"))&&(!mxpSupportSet.contains("-IMAGE.URL")))))
            { 
                String path=CMProps.mxpImagePath("intro.jpg");
                if(path.length()>0)
                    println("\n\r\n\r\n\r<IMAGE 'intro.jpg' URL='"+path+"' H=400 W=400>\n\r\n\r");
            }
            preliminaryRead(100);
            connectionComplete=true;
		}
		catch(SocketException e)
		{
		}
		catch(Exception e)
		{
		}
        if(preliminaryInput.length()>0)
            fakeInput=preliminaryInput;
        preliminaryInput=null;
	}

    private void preliminaryRead(long timeToWait)
    {
        try{ 
            long time=System.currentTimeMillis();
            while((System.currentTimeMillis()-time)<timeToWait)
            {
                String s=blockingIn(timeToWait);
                if((s!=null)&&(s.length()>0))
                    preliminaryInput.append(s+"\n");
            }
        }catch(Exception e){}
    }
    
    private void requestSubOption(OutputStream out, int optionCode)
    throws IOException
    {
        if(CMSecurity.isDebugging("TELNET"))
            Log.debugOut("Session","Sent sub-option: "+Session.TELNET_DESCS[optionCode]);
        if(optionCode!=TELNET_COMPRESS2)
        {
	        byte[] stream={(byte)TELNET_IAC,(byte)TELNET_SB,(byte)optionCode,(byte)1,(byte)TELNET_IAC,(byte)TELNET_SE};
	        out.write(stream);
        }
        else
        {
	        byte[] stream={(byte)TELNET_IAC,(byte)TELNET_SB,(byte)optionCode,(byte)TELNET_IAC,(byte)TELNET_SE};
	        out.write(stream);
        }
        out.flush();
    }
    
    private boolean mightSupportTelnetMode(int telnetCode)
    {
        if(telnetSupportSet.size()==0)
        {
            telnetSupportSet.add(new Integer(Session.TELNET_MXP));
            telnetSupportSet.add(new Integer(Session.TELNET_MSP));
            telnetSupportSet.add(new Integer(Session.TELNET_TERMTYPE));
            telnetSupportSet.add(new Integer(Session.TELNET_BINARY));
            telnetSupportSet.add(new Integer(Session.TELNET_ECHO));
            telnetSupportSet.add(new Integer(Session.TELNET_SUPRESS_GO_AHEAD));
            telnetSupportSet.add(new Integer(Session.TELNET_TERMTYPE));
            telnetSupportSet.add(new Integer(Session.TELNET_NAWS));
            //telnetSupportSet.add(new Integer(Session.TELNET_COMPRESS2));
        }
        return telnetSupportSet.contains(new Integer(telnetCode));
    }
    
    public void setServerTelnetMode(int telnetCode, boolean onOff)
    { serverTelnetCodes[telnetCode]=onOff; }
    public boolean serverTelnetMode(int telnetCode)
    { return serverTelnetCodes[telnetCode]; }
    public void setClientTelnetMode(int telnetCode, boolean onOff)
    { clientTelnetCodes[telnetCode]=onOff; }
    public boolean clientTelnetMode(int telnetCode)
    { return clientTelnetCodes[telnetCode]; }
    private void changeTelnetMode(OutputStream out, int telnetCode, boolean onOff)
    throws IOException
    {
    	byte[] command={(byte)TELNET_IAC,onOff?(byte)TELNET_WILL:(byte)TELNET_WONT,(byte)telnetCode};
    	out.write(command);
        out.flush();
        if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Sent: "+(onOff?"Will":"Won't")+" "+Session.TELNET_DESCS[telnetCode]);
        setServerTelnetMode(telnetCode,onOff);
    }
    public void changeTelnetMode(int telnetCode, boolean onOff)
    {
    	char[] command={(char)TELNET_IAC,onOff?(char)TELNET_WILL:(char)TELNET_WONT,(char)telnetCode};
    	out.write(command);
        out.flush();
        if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Sent: "+(onOff?"Will":"Won't")+" "+Session.TELNET_DESCS[telnetCode]);
        setServerTelnetMode(telnetCode,onOff);
    }
    private void changeTelnetModeBackwards(int telnetCode, boolean onOff)
    {
    	char[] command={(char)TELNET_IAC,onOff?(char)TELNET_DO:(char)TELNET_DONT,(char)telnetCode};
    	out.write(command);
        out.flush();
        if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Back-Sent: "+(onOff?"Do":"Don't")+" "+Session.TELNET_DESCS[telnetCode]);
        setServerTelnetMode(telnetCode,onOff);
    }
    
    public void initTelnetMode(int mobbitmap)
    {
        setServerTelnetMode(TELNET_ANSI,CMath.bset(mobbitmap,MOB.ATT_ANSI));
        setClientTelnetMode(TELNET_ANSI,CMath.bset(mobbitmap,MOB.ATT_ANSI));
        boolean changedSomething=false;
        boolean mxpSet=(!CMSecurity.isDisabled("MXP"))&&CMath.bset(mobbitmap,MOB.ATT_MXP);
        if(mxpSet!=clientTelnetMode(TELNET_MXP))
        { changeTelnetMode(TELNET_MXP,!clientTelnetMode(TELNET_MXP)); changedSomething=true;}
        boolean mspSet=(!CMSecurity.isDisabled("MSP"))&&CMath.bset(mobbitmap,MOB.ATT_SOUND);
        if(mspSet!=clientTelnetMode(TELNET_MSP))
        { changeTelnetMode(TELNET_MSP,!clientTelnetMode(TELNET_MSP)); changedSomething=true;}
        try{if(changedSomething) blockingIn(500);}catch(Exception e){}
    }

	public int currentColor(){return currentColor;}
    public int lastColor(){return lastColor;}
	public long getTotalMillis(){ return milliTotal;}
	public long getIdleMillis(){ return System.currentTimeMillis()-lastKeystroke;}
	public long getTotalTicks(){ return tickTotal;}
    public long getMillisOnline(){ return System.currentTimeMillis()-onlineTime;}
    
	public long lastLoopTime(){ return lastLoopTop;}
    public long getLastPKFight(){return lastPKFight;}
    public void setLastPKFight(){lastPKFight=System.currentTimeMillis();}
    public long getLastNPCFight(){return lastNPCFight;}
    public void setLastNPCFight(){lastNPCFight=System.currentTimeMillis();}
    public Vector getLastMsgs(){return (Vector)prevMsgs.clone();}

	public MOB mob(){return mob;}
	public void setMob(MOB newmob)
	{ mob=newmob;}
	public int getWrap(){return ((mob!=null)&&(mob.playerStats()!=null))?mob.playerStats().getWrap():78;}
	public boolean killFlag(){return killFlag;}
	public void setKillFlag(boolean truefalse){killFlag=truefalse;}
	public Vector previousCMD(){return previousCmd;}
	public void startBeingSnoopedBy(Session S)
	{
		if(!snoops.contains(S))
			snoops.addElement(S);
	}
	public void stopBeingSnoopedBy(Session S)
	{
		while(snoops.contains(S))
			snoops.removeElement(S);
	}
	public boolean amBeingSnoopedBy(Session S)
	{
		if(S==null) return snoops.size()==0;
		return(snoops.contains(S));
	}

	public void setPreviousCmd(Vector cmds)
	{
		if(cmds==null) return;
		if(cmds.size()==0) return;
		if((cmds.size()>0)&&(((String)cmds.elementAt(0)).trim().startsWith("!")))
			return;

		previousCmd.removeAllElements();
		for(int i=0;i<cmds.size();i++)
			previousCmd.addElement(((String)cmds.elementAt(i)).toString());
	}

	public boolean afkFlag(){return afkFlag;}
	public void setAfkFlag(boolean truefalse)
	{
		if(afkFlag==truefalse) return;
		afkFlag=truefalse;
		if(afkFlag)
			println("\n\rYou are now listed as AFK.");
		else
        {
            afkMessage=null;
			println("\n\rYou are no longer AFK.");
        }
	}
    public String afkMessage()
    {
        if(mob==null) return "";
        if((afkMessage==null)||(CMStrings.removeColors(afkMessage).trim().length()==0))
            return mob.name()+" is AFK at the moment.";
        return afkMessage;
    }
    public void setAFKMessage(String str){afkMessage=str;}

    protected void errorOut(Exception t)
	{
		Log.errOut("Session",t);
		CMLib.sessions().removeElement(this);
		killFlag=true;
	}

	public long getWriteStartTime(){return writeStartTime;}
	public boolean isLockedUpWriting(){
		long time=writeStartTime;
		if(time==0) return false;
		return ((System.currentTimeMillis()-time)>10000);
	}
    
	public void out(char[] c){
		try{
			if((out!=null)&&(c!=null)&&(c.length>0))
			{
                if(isLockedUpWriting())
                {
                    String name=(mob!=null)?mob.Name():getAddress();
                    Log.errOut("DefaultSession","Kicked out "+name+" due to write-lock ("+out.getClass().getName()+".");
                    logoff(true);
                    logoff(true);
            		CMLib.killThread(this,500,1);
                }
                else
                {
    				writeStartTime=System.currentTimeMillis()+c.length;
    				out.write(c);
    				if(out.checkError()) 
    					logoff(true);
                }
			}
		}
        catch(Exception ioe){ killFlag=true;}
		finally{writeStartTime=0;}
	}
    public void out(String c){ if(c!=null) out(c.toCharArray());}
    public void out(char c){ char[] cs={c}; out(cs);}
	public void onlyPrint(String msg){onlyPrint(msg,-1,false);}
	public void onlyPrint(String msg, int pageBreak, boolean noCache)
	{
		if((out==null)||(msg==null)) return;
		try
		{
			try{
				if(snoops.size()>0)
                {
                    String msgColored=CMStrings.replaceAll(msg,"\n\r",CMLib.coffeeFilter().colorOnlyFilter("\n\r^Z"+((mob==null)?"?":mob.Name())+":^N ",this));
					for(int s=0;s<snoops.size();s++)
						((Session)snoops.elementAt(s)).onlyPrint(msgColored,0,noCache);
                }
			}catch(IndexOutOfBoundsException x){}

			if(msg.endsWith("\n\r")
			&&(msg.equals(lastStr))
			&&(msg.length()>2)
			&&(msg.indexOf("\n")==(msg.length()-2)))
			{ spamStack++; return; }
			else
			if(spamStack>0)
			{
				if(spamStack>1)
					lastStr=lastStr.substring(0,lastStr.length()-2)+"("+spamStack+")"+lastStr.substring(lastStr.length()-2);
				out(lastStr.toCharArray());
			}

			spamStack=0;
			if(msg.startsWith("\n\r")&&(msg.length()>2))
				lastStr=msg.substring(2);
			else
				lastStr=msg;
			
			if(pageBreak<0)	pageBreak=CMProps.getIntVar(CMProps.SYSTEMI_PAGEBREAK);
			if((pageBreak>0)&&(this==Thread.currentThread()))
			{
				int lines=0;
				for(int i=0;i<msg.length();i++)
				{
					if(msg.charAt(i)=='\n')
					{
						lines++;
						if(lines>=pageBreak)
						{
							lines=0;
							if((i<(msg.length()-1)&&(msg.charAt(i+1)=='\r')))
								i++;
							out(msg.substring(0,i).toCharArray());
							msg=msg.substring(i+1);
							out("<pause - enter>".toCharArray());
							try{ blockingIn(); }catch(Exception e){return;}
						}
					}
				}
			}
			
			// handle line cache -- 
			if(!noCache)
			for(int i=0;i<msg.length();i++)
			{
				if(curPrevMsg==null) curPrevMsg=new StringBuffer("");
				if(msg.charAt(i)=='\r') continue;
				if(msg.charAt(i)=='\n')
				{
					if(curPrevMsg.toString().trim().length()>0)
					{
						synchronized(prevMsgs)
						{
							while(prevMsgs.size()>=MAX_PREVMSGS)
								prevMsgs.removeElementAt(0);
							prevMsgs.addElement(curPrevMsg.toString());
							curPrevMsg.setLength(0);
						}
					}
					continue;
				}
				curPrevMsg.append(msg.charAt(i));
			}
			out(msg.toCharArray());
		}
		catch(java.lang.NullPointerException e){}
	}

    public void rawOut(String msg){out(msg);}
	public void rawPrint(String msg){rawPrint(msg,-1);}
	public void rawPrint(String msg, int pageBreak)
	{ if(msg==null)return;
	  onlyPrint((needPrompt?"":"\n\r")+msg,pageBreak,false);
	  needPrompt=true;
	}

	public void print(String msg)
	{ onlyPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,mob,mob,null,msg,false),-1,false); }

	public void rawPrintln(String msg){rawPrintln(msg,-1);}
	public void rawPrintln(String msg, int pageBreak)
	{ if(msg==null)return; rawPrint(msg+"\n\r",pageBreak);}

	public void stdPrint(String msg)
	{ rawPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,mob,mob,null,msg,false)); }

	public void print(Environmental src, Environmental trg, Environmental tol, String msg)
	{ onlyPrint((CMLib.coffeeFilter().fullOutFilter(this,mob,src,trg,tol,msg,false)),-1,false);}

	public void stdPrint(Environmental src, Environmental trg, Environmental tol, String msg)
	{ rawPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,src,trg,trg,msg,false)); }

	public void println(String msg)
	{ if(msg==null)return; print(msg+"\n\r");}

	public void wraplessPrintln(String msg)
	{ if(msg==null)return;
	  onlyPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,mob,mob,null,msg,true)+"\n\r",-1,false);
	  needPrompt=true;
	}

	public void wraplessPrint(String msg)
	{ onlyPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,mob,mob,null,msg,true),-1,false);
	  needPrompt=true;
	}

	public void colorOnlyPrintln(String msg)
	{ colorOnlyPrint(msg,-1,false);}
	public void colorOnlyPrintln(String msg, int pageBreak, boolean noCache)
	{ if(msg==null)return;
	  onlyPrint(CMLib.coffeeFilter().colorOnlyFilter(msg,this)+"\n\r",pageBreak,noCache);
	  needPrompt=true;
	}

	public void colorOnlyPrint(String msg)
	{ colorOnlyPrint(msg,-1,false);}
	public void colorOnlyPrint(String msg, int pageBreak, boolean noCache)
	{ onlyPrint(CMLib.coffeeFilter().colorOnlyFilter(msg,this),pageBreak,noCache);
	  needPrompt=true;
	}

	public void stdPrintln(String msg)
	{ if(msg==null)return;
	  rawPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,mob,mob,null,msg,false)+"\n\r");
	}

	public void println(Environmental src, Environmental trg, Environmental tol, String msg)
	{ if(msg==null)return;
	  onlyPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,src,trg,tol,msg,false)+"\n\r",-1,false);
	}

	public void stdPrintln(Environmental src,Environmental trg, Environmental tol, String msg)
	{ if(msg==null)return;
	  rawPrint(CMLib.coffeeFilter().fullOutFilter(this,mob,src,trg,tol,msg,false)+"\n\r");
	}

	public void setPromptFlag(boolean truefalse)
	{
		needPrompt=truefalse;
	}

	public String prompt(String Message, String Default, long maxTime)
		throws IOException
	{
		String Msg=prompt(Message,maxTime).trim();
		if(Msg.equals("")) 
            return Default;
        return Msg;
	}

	public String prompt(String Message, String Default)
		throws IOException
	{
		String Msg=prompt(Message,-1).trim();
		if(Msg.equals("")) 
            return Default;
		return Msg;
	}

	public String prompt(String Message, long maxTime)
		throws IOException
	{
		print(Message);
		String input=blockingIn(maxTime);
		if(input==null) return "";
		if((input.length()>0)&&(input.charAt(input.length()-1)=='\\'))
			return input.substring(0,input.length()-1);
		return input;
	}

	public String prompt(String Message)
		throws IOException
	{
		print(Message);
		String input=blockingIn(-1);
		if(input==null) return "";
		if((input.length()>0)&&(input.charAt(input.length()-1)=='\\'))
			return input.substring(0,input.length()-1);
		return input;
	}

	public void cmdExit(MOB mob, Vector commands)
		throws Exception
	{
		if (confirm("\n\rQuit -- are you sure (y/N)?","N"))
		{
            CMMsg msg=CMClass.getMsg(mob,null,CMMsg.MSG_QUIT,null);
            Room R=mob.location();
            try{
                if((R!=null)&&(R.okMessage(mob,msg)))
                    killFlag=true;
            }
            catch(Exception e){}
		}
	}

	public int getColor(char c)
	{
		// warning do not nest!
		if (c == '?') return lastColor;
		if (c>255) return -1;
		return c;
	}

	public String[] clookup(){
		if(clookup==null)
			clookup=CMLib.color().standardColorLookups();

		if(mob()==null) return clookup;
		PlayerStats pstats=mob().playerStats();
		if((mob.soulMate()!=null)&&(mob.soulMate().playerStats()!=null))
		    pstats=mob.soulMate().playerStats();
		if(pstats==null) return clookup;

		if(!pstats.getColorStr().equals(lastColorStr))
		{
			if(pstats.getColorStr().length()==0)
				clookup=CMLib.color().standardColorLookups();
			else
			{
				String changes=pstats.getColorStr();
				lastColorStr=changes;
				clookup=(String[])CMLib.color().standardColorLookups().clone();
				int x=changes.indexOf("#");
				while(x>0)
				{
					String sub=changes.substring(0,x);
					changes=changes.substring(x+1);
					clookup[sub.charAt(0)]=CMLib.color().translateCMCodeToANSI(sub.substring(1));
					x=changes.indexOf("#");
				}
				for(int i=0;i<clookup.length;i++)
				{
					String s=clookup[i];
					if((s!=null)&&(s.startsWith("^"))&&(s.length()>1))
						clookup[i]=clookup[s.charAt(1)];
				}
			}
		}
		return clookup;
	}

	public String makeEscape(int c)
	{
	    switch(c)
	    {
	    	case '>':
	    	    if(currentColor>0)
	    	    {
	    	    	if(clookup()[c]==null)
	    	    		return clookup()[currentColor];
	    	    	if(clookup()[currentColor]==null)
	    	    		return clookup[c];
					return clookup()[c]+clookup()[currentColor];
	    	    }
    	        return clookup()[c];
	    	case '<':
	    	case '&':
	    	case '"':
				return clookup()[c];
		    case '0': case '1': case '2': case '3': case '4': case '5':
		    case '6': case '7': case '8': case '9':
			{
                if(clientTelnetMode(Session.TELNET_MSP))
					return CMProps.getVar(CMProps.SYSTEM_ESC0+(c-('0')));
				return "";
			}
		    default:
				break;
	    }
		if (clientTelnetMode(Session.TELNET_ANSI) && (c != -1))
		{
			if ((c != currentColor)||(c=='^'))
			{
                if((c!='.')&&(c!='<')&&(c!='>')&&(c!='^'))
                {
					lastColor = currentColor;
					currentColor = c;
			    }
				return clookup()[c];
			}
		}
		else
		{
			lastColor = currentColor;
			currentColor = 0;
		}
		return null;
	}

    public void handleSubOption(int optionCode, char[] suboptionData, int dataSize)
    	throws IOException
    {
        switch(optionCode)
        {
        case TELNET_TERMTYPE:
            if(dataSize >= 1)
            {
                if(CMSecurity.isDebugging("TELNET"))
                    Log.debugOut("Session","For suboption "+Session.TELNET_DESCS[optionCode]+", got code "+((int)suboptionData[0])+": "+new String(suboptionData, 1, dataSize - 1));
                if(suboptionData[0] == 0)
                {
                    terminalType = new String(suboptionData, 1, dataSize - 1);
                    if((terminalType.equalsIgnoreCase("ZMUD"))
        			||(terminalType.equalsIgnoreCase("XTERM")))
        			{
        				if(mightSupportTelnetMode(TELNET_ECHO))
        					telnetSupportSet.remove(new Integer(TELNET_ECHO));
        			    changeTelnetMode(rawout,TELNET_ECHO,false);
        			}
        			else
    				if(terminalType.equalsIgnoreCase("ANSI"))
        			    changeTelnetMode(rawout,TELNET_ECHO,true);
                }
                else 
                if (suboptionData[0] == 1) // Request for data.
                {/* No idea how to handle this, ignore it for now.*/}
            }
            break;
        case TELNET_NAWS:
            if (dataSize == 4)  // It should always be 4.
            {
                terminalWidth = (suboptionData[0] << 8) | suboptionData[1];
                terminalHeight = (suboptionData[2] << 8) | suboptionData[3];
                if(CMSecurity.isDebugging("TELNET"))
                    Log.debugOut("Session","For suboption "+Session.TELNET_DESCS[optionCode]+", got: "+terminalWidth+"x"+terminalHeight);
            }
            break;
        default:
            // Ignore it.
            break;
        }
    }
    
    public void handleEscape()
    throws IOException, InterruptedIOException
    {
        if((in==null)||(out==null)) return;
        int c=read();
        if((char)c!='[') return;
        
        boolean quote=false;
        StringBuffer escapeStr=new StringBuffer("");
        while(((c=read())!=-1)
        &&(!killFlag)
        &&((quote)||(!Character.isLetter((char)c))))
        {
            escapeStr.append((char)c);
            if(c=='"') quote=!quote;
        }
        if(c==-1) return;
        escapeStr.append((char)c);
        String esc=escapeStr.toString().trim();
        // at the moment, we only handle mxp escapes
        // everything else is effectively EATEN
        if(!esc.endsWith("z")) return;
        esc=esc.substring(0,esc.length()-1);
        if(!CMath.isNumber(esc)) return;
        int escNum=CMath.s_int(esc);
        // only LINE-based mxp escape sequences are respected
        if(escNum>3) return;
        sock.setSoTimeout(30000);
        StringBuffer line=new StringBuffer("");
        while(((c=read())!=-1)&&(!killFlag))
        {
            if(c=='\n') break;
            line.append((char)c);
        }
        sock.setSoTimeout(SOTIMEOUT);
        String l=line.toString().toUpperCase().trim();
        // now we have the line, so parse out tags -- only tags matter!
        while(l.length()>0)
        {
            int tagStart=l.indexOf("<");
            if(tagStart<0) return;
            int tagEnd=l.indexOf(">");
            if(tagEnd<0) return;
            String tag=l.substring(tagStart+1,tagEnd).trim();
            l=l.substring(tagEnd+1).trim();
            // now we have a tag, and its parameters (space delimited)
            Vector parts=CMParms.parseSpaces(tag,true);
            if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Got secure MXP tag: "+tag);
            if(parts.size()>1)
            {
                tag=(String)parts.firstElement();
                if(tag.equals("VERSION"))
                {
                    for(int p=1;p<parts.size();p++)
                    {
                        String pp=(String)parts.elementAt(p);
                        int x=pp.indexOf("=");
                        if(x<0) continue;
                        mxpVersionInfo.remove(pp.substring(0,x).trim());
                        mxpVersionInfo.put(pp.substring(0,x).trim(),pp.substring(x+1).trim());
                    }
                }
                else
                if(tag.equals("SUPPORTS"))
                    for(int p=1;p<parts.size();p++)
                        mxpSupportSet.add(parts.elementAt(p));
                else
                if(tag.equals("SHUTDOWN"))
                {
                    MOB M=CMLib.map().getLoadPlayer((String)parts.elementAt(1));
                    if((M!=null)&&(M.playerStats().password().equalsIgnoreCase((String)parts.elementAt(2)))&&(CMSecurity.isASysOp(M)))
                    {
                        boolean keepDown=parts.size()>3?CMath.s_bool((String)parts.elementAt(3)):true;
                        String externalCmd=(parts.size()>4)?CMParms.combine(parts,4):null;
                        Vector cmd=CMParms.makeVector("SHUTDOWN","NOPROMPT");
                        if(!keepDown)
                        {
                            cmd.add("RESTART");
                            if(externalCmd.length()>0)
                                cmd.add(externalCmd);
                        }
                        Command C=CMClass.getCommand("Shutdown");
                        l="";
                        killFlag=true;
                        out.write("\n\n\033[1z<Executing Shutdown...\n\n");
                        M.setSession(this);
                        if(C!=null) C.execute(M,cmd);
                    }
                }
            }
        }
    }
    
	public void handleIAC()
		throws IOException, InterruptedIOException
	{
		if((in==null)||(out==null))
			return;
		int c=read();
		switch(c)
		{
        case TELNET_IAC:
            bNextByteIs255=true;
            break;
        case TELNET_SB:
        {
            char[] subOptionData = new char[1024];
            int subOptionCode = read();
            int numBytes = 0;
            int last = 0;
            if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Reading sub-option "+subOptionCode);
            while(((last = read()) != -1)
            &&(numBytes<subOptionData.length)
            &&(!killFlag))
            {
                if (last == TELNET_IAC)
                {
                    last = read();
                    if (last == TELNET_IAC)
                        subOptionData[numBytes++] = TELNET_IAC;
                    else 
                    if (last == TELNET_SE)
                        break;
                }
                else
                    subOptionData[numBytes++] = (char)last;
            }
            handleSubOption(subOptionCode, subOptionData, numBytes);
            break;
        }
		case TELNET_DO:
		{
		    int last=read();
            setClientTelnetMode(last,true);
            if((terminalType.equalsIgnoreCase("zmud"))&&(last==Session.TELNET_ECHO))
                setClientTelnetMode(Session.TELNET_ECHO,false);
            if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Got DO "+Session.TELNET_DESCS[last]);
            if((last==TELNET_COMPRESS2)&&(serverTelnetMode(last)))
            {
            	setClientTelnetMode(last,true);
            	if(connectionComplete)
            	{
	                requestSubOption(rawout,TELNET_COMPRESS2);
	                out.flush();
	                ZOutputStream zOut=new ZOutputStream(rawout, JZlib.Z_DEFAULT_COMPRESSION);
	                zOut.setFlushMode(JZlib.Z_SYNC_FLUSH);
					out = new PrintWriter(new OutputStreamWriter(zOut,"iso-8859-1"));
					try{Thread.sleep(250);}catch(Exception e){}
            	}
            }
            else
            if(!mightSupportTelnetMode(last))
                changeTelnetMode(last,false);
            else
            if(!serverTelnetMode(last))
                changeTelnetMode(last,true);
		    break;
		}
		case TELNET_DONT:
		{
		    int last=read();
            if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Got DONT "+Session.TELNET_DESCS[last]);
            setClientTelnetMode(last,false);
            if((last==TELNET_COMPRESS2)&&(serverTelnetMode(last)))
            {
            	setClientTelnetMode(last,false);
				out = new PrintWriter(new OutputStreamWriter(rawout,"iso-8859-1"));
            }
            if((mightSupportTelnetMode(last)&&(serverTelnetMode(last))))
                changeTelnetMode(last,false);
		    break;
		}
        case TELNET_WILL:
        {
            int last=read();
            if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Got WILL "+Session.TELNET_DESCS[last]);
            setClientTelnetMode(last,true);
            if((terminalType.equalsIgnoreCase("zmud"))&&(last==Session.TELNET_ECHO))
                setClientTelnetMode(Session.TELNET_ECHO,false);
            if(!mightSupportTelnetMode(last))
                changeTelnetModeBackwards(last,false);
            else
            if(!serverTelnetMode(last))
                changeTelnetModeBackwards(last,true);
            break;
        }
        case TELNET_WONT:
        {
            int last=read();
            if(CMSecurity.isDebugging("TELNET")) Log.debugOut("Session","Got WONT "+Session.TELNET_DESCS[last]);
            setClientTelnetMode(last,false);
            if((mightSupportTelnetMode(last))&&(serverTelnetMode(last)))
                changeTelnetModeBackwards(last,false);
            break;
        }
        case TELNET_AYT:
            out(" \b");
            break;
		default:
			return;
		}
	}

    public int read() throws IOException
    {
        if(bNextByteIs255) return 255;
        bNextByteIs255 = false;
        if(fakeInput!=null)
        {
            if(fakeInput.length()>0)
            {
                int c=fakeInput.charAt(0);
                fakeInput=new StringBuffer(fakeInput.substring(1));
                return c;
            }
            fakeInput=null;
        }
        return in.read();
    }
    
    public char hotkey(long maxWait)
    {
        if((in==null)||(out==null)) return '\0';
        input=new StringBuffer("");
        long start=System.currentTimeMillis();
        try
        {
            suspendCommandLine=true;
            char c='\0';
            while((!killFlag)
            &&((maxWait<=0)||((System.currentTimeMillis()-start)<maxWait)))
            {
                c=(char)nonBlockingIn(false);
                if((c==(char)0)||(c==(char)1)||(c==(char)-1))
                    continue;
                return c;
            }
            suspendCommandLine=false;
            if((maxWait>0)&&((System.currentTimeMillis()-start)>=maxWait))
                throw new java.io.InterruptedIOException("Timed Out.");
        }
        catch(java.io.IOException e) { }
        return '\0';
    }
    
	public int nonBlockingIn(boolean appendInputFlag)
	throws IOException
	{
		try
		{
            int c=read();
			if(c<0)
				throw new IOException("reset by peer");
			else
			if(c==TELNET_IAC)
			    handleIAC();
			else
            if(c=='\033')
                handleEscape();
            else
            {
                boolean rv = false;
                switch (c)
                {
                    case 0:
                    {
                        c=-1;
                        lastWasCR = false;
                        lastWasLF = false;
                    }
                    break;
                    case 10:
                    {
                        c=-1;
                        if(!lastWasCR)
                        {
                            lastWasLF = true; 
                            rv = true;
                        }
                        else
                            lastWasLF = false; 
                        lastWasCR = false;
                        if (clientTelnetMode(TELNET_ECHO))
                            out(""+(char)13+(char)10);  // CR
                        break;
                    }
                    case 13:
                    {
                        c=-1;
                        if(!lastWasLF)
                        {
                            lastWasCR = true;
                            rv = true;
                        }
                        else
                            lastWasCR = false;
                        lastWasLF = false;
                        if (clientTelnetMode(TELNET_ECHO))
                            out(""+(char)13+(char)10);  // CR
                        break;
                    }
                    case 26:
                    {
                        lastWasCR = false;
                        lastWasLF = false;
                        // don't let them enter ANSI escape sequences...
                        c = -1;
                        break;
                    }
                    case 255:
                    case 241:
                    case 242:
                    case 243:
                    case 244:
                    case 245:
                    case 246:
                    case 247:
                    case 248:
                    case 249:
                    case 250:
                    case 251:
                    case 252:
                    case 253:
                    case 254:
                    {
                        lastWasCR = false;
                        lastWasLF = false;
                        // don't let them enter telnet codes, except IAC, which is handled...
                        c = -1;
                        break;
                    }
                    default:
                    {
                        lastWasCR = false;
                        lastWasLF = false;
                        break;
                    }
                }

                if(c>0)
                {
                    lastKeystroke=System.currentTimeMillis();
                    if(appendInputFlag) input.append((char)c);
                    if (clientTelnetMode(TELNET_ECHO))
                        out((char)c);
                    if(!appendInputFlag) return c;
                }
                if(rv) return 0;
            }
		}
		catch(InterruptedIOException e)
		{
		    return -1;
		}
		return 1;
	}
	
	public String blockingIn(long maxTime)
		throws IOException
	{
		if((in==null)||(out==null)) return "";
		input=new StringBuffer("");
		long start=System.currentTimeMillis();
		try
		{
			suspendCommandLine=true;
			while((!killFlag)
			&&((maxTime<=0)||((System.currentTimeMillis()-start)<maxTime)))
			    if(nonBlockingIn(true)==0) 
			        break;
			suspendCommandLine=false;
			if((maxTime>0)&&((System.currentTimeMillis()-start)>=maxTime))
				throw new java.io.InterruptedIOException("Timed Out.");
		
			StringBuffer inStr=CMLib.coffeeFilter().simpleInFilter(input,CMSecurity.isAllowed(mob,(mob!=null)?mob.location():null,"MXPTAGS"));
			input=new StringBuffer("");
			if(inStr==null) return null;
			return inStr.toString();
		}
		finally
		{
			suspendCommandLine=false;
		}
	}
	
	public String blockingIn()
		throws IOException
	{
		return blockingIn(-1);
	}

	public String readlineContinue()
		throws IOException, SocketException
	{

		if((in==null)||(out==null)) return "";
		int code=-1;
		while(!killFlag)
		{
		    code=nonBlockingIn(true);
		    if(code==1)
		        continue;
		    if(code==0) 
		        break;
		    if(code==-1) 
		        return null;
		}

		StringBuffer inStr=CMLib.coffeeFilter().simpleInFilter(input,CMSecurity.isAllowed(mob,(mob!=null)?mob.location():null,"MXPTAGS"));
		input=new StringBuffer("");
		if(inStr==null) return null;
		return inStr.toString();
	}

	public boolean confirm(String Message, String Default, long maxTime)
	throws IOException
	{
		String YN=choose(Message,"YN",Default,maxTime);
		if(YN.equals("Y"))
			return true;
		return false;
	}
	public boolean confirm(String Message, String Default)
	throws IOException
	{
		String YN=choose(Message,"YN",Default,-1);
		if(YN.equals("Y"))
			return true;
		return false;
	}

	public String choose(String Message, String Choices, String Default)
	throws IOException
	{ return choose(Message,Choices,Default,-1);}
	
	public String choose(String Message, String Choices, String Default, long maxTime)
	throws IOException
	{
		String YN="";
		while((YN.equals(""))||(Choices.indexOf(YN)<0)&&(!killFlag))
		{
			print(Message);
			YN=blockingIn(maxTime);
			if(YN==null){ return Default.toUpperCase(); }
			YN=YN.trim().toUpperCase();
			if(YN.equals("")){ return Default.toUpperCase(); }
			if(YN.length()>1) YN=YN.substring(0,1);
		}
		return YN;
	}

	public void logoff(boolean killThread)
	{
		killFlag=true;
        if(mob!=null)
        {
            if(mob.playerStats()!=null)
            {
                mob.playerStats().setLastDateTime(System.currentTimeMillis());
            }
            if(CMLib.flags().isInTheGame(mob,true))
	            CMLib.database().DBUpdateFollowers(mob);
            mob.removeFromGame(true);
        }
        status=Session.STATUS_LOGOUT3;
        CMLib.sessions().removeElement(this);
        status=Session.STATUS_LOGOUT4;
        closeSocks();
        status=Session.STATUS_LOGOUT5;
		if(killThread) CMLib.killThread(this,1000,1);
	}

	public void showPrompt()
	{
	    promptLastShown=System.currentTimeMillis();
		if(mob()==null) return;
		if(mob().playerStats()==null) return;
		StringBuffer buf=new StringBuffer("\n\r");
		String prompt=mob().playerStats().getPrompt();
		int c=0;
		if(clientTelnetMode(Session.TELNET_MXP))
		    buf.append("^<!EN Hp '"+mob().curState().getHitPoints()
					+"'^>^<!EN MaxHp '"+mob().maxState().getHitPoints()
					+"'^>^<!EN Mana '"+mob().curState().getMana()
					+"'^>^<!EN MaxMana '"+mob().maxState().getMana()
					+"'^>^<!EN Move '"+mob().curState().getMovement()
					+"'^>^<!EN MaxMove '"+mob().maxState().getMovement()
					+"'^>^<!EN Exp '"+mob().getExperience()
					+"'^>^<!EN ExpNeed '"+mob().getExpNeededLevel()
					+"'^>^\n\r\n\r");
		while(c<prompt.length())
			if((prompt.charAt(c)=='%')&&(c<(prompt.length()-1)))
			{
				switch(prompt.charAt(++c))
				{
                case 'a': { buf.append(CMLib.factions().getRangePercent(CMLib.factions().AlignID(),mob.fetchFaction(CMLib.factions().AlignID()))+"%"); c++; break; }
                case 'A': { Faction.FactionRange FR=CMLib.factions().getRange(CMLib.factions().AlignID(),mob.fetchFaction(CMLib.factions().AlignID()));buf.append((FR!=null)?FR.name():""+mob.fetchFaction(CMLib.factions().AlignID())); c++; break;}
                case 'B': { buf.append("\n\r"); c++; break;}
                case 'c': { buf.append(mob().inventorySize()); c++; break;}
                case 'C': { buf.append(mob().maxItems()); c++; break;}
                case 'd': {   MOB victim=mob().getVictim();
                              if((mob().isInCombat())&&(victim!=null))
                                  buf.append(""+mob().rangeToTarget());
                              c++; break; }
                case 'e': {   MOB victim=mob().getVictim();
                              if((mob().isInCombat())&&(victim!=null)&&(CMLib.flags().canBeSeenBy(victim,mob)))
                                  buf.append(victim.name());
                              c++; break; }
                case 'E': {   MOB victim=mob().getVictim();
                              if((mob().isInCombat())&&(victim!=null)&&(!victim.amDead())&&(CMLib.flags().canBeSeenBy(victim,mob)))
                                  buf.append(victim.charStats().getMyRace().healthText(mob(),victim)+"\n\r");
                              c++; break; }
                case 'g': { buf.append((int)Math.round(Math.floor(CMLib.beanCounter().getTotalAbsoluteNativeValue(mob())/CMLib.beanCounter().getLowestDenomination(CMLib.beanCounter().getCurrency(mob()))))); c++; break;}
                case 'G': { buf.append(CMLib.beanCounter().nameCurrencyShort(mob(),CMLib.beanCounter().getTotalAbsoluteNativeValue(mob()))); c++; break;}
				case 'h': { buf.append("^<Hp^>"+mob().curState().getHitPoints()+"^</Hp^>"); c++; break;}
				case 'H': { buf.append("^<MaxHp^>"+mob().maxState().getHitPoints()+"^</MaxHp^>"); c++; break;}
                case 'I': {   if((CMLib.flags().isCloaked(mob()))&&(!CMLib.flags().isSeen(mob())))
                                  buf.append("Wizinvisible");
                              else
                              if(CMLib.flags().isCloaked(mob()))
                                  buf.append("Cloaked");
                              else
                              if(!CMLib.flags().isSeen(mob()))
                                  buf.append("Undetectable");
                              else
                              if(CMLib.flags().isInvisible(mob())&&CMLib.flags().isHidden(mob()))
                                  buf.append("Hidden/Invisible");
                              else
                              if(CMLib.flags().isInvisible(mob()))
                                  buf.append("Invisible");
                              else
                              if(CMLib.flags().isHidden(mob()))
                                  buf.append("Hidden");
                              c++; break;}
                case 'K':
                case 'k': { MOB tank=mob();
                            if((tank.getVictim()!=null)
                            &&(tank.getVictim().getVictim()!=null)
                            &&(tank.getVictim().getVictim()!=mob()))
                                tank=tank.getVictim().getVictim();
                            if(((c+1)<prompt.length())&&(tank!=null))
                                switch(prompt.charAt(c+1))
                                {
                                    case 'h': { buf.append(tank.curState().getHitPoints()); c++; break;}
                                    case 'H': { buf.append(tank.maxState().getHitPoints()); c++; break;}
                                    case 'm': { buf.append(tank.curState().getMana()); c++; break;}
                                    case 'M': { buf.append(tank.maxState().getMana()); c++; break;}
                                    case 'v': { buf.append(tank.curState().getMovement()); c++; break;}
                                    case 'V': { buf.append(tank.maxState().getMovement()); c++; break;}
                                }
                            c++;
                            break;
                          }
				case 'm': { buf.append("^<Mana^>"+mob().curState().getMana()+"^</Mana^>"); c++; break;}
				case 'M': { buf.append("^<MaxMana^>"+mob().maxState().getMana()+"^</MaxMana^>"); c++; break;}
                case 'r': {   if(mob().location()!=null)
                              buf.append(mob().location().displayText());
                              c++; break; }
                case 'R': {   if((mob().location()!=null)&&CMSecurity.isAllowed(mob(),mob().location(),"SYSMSGS"))
                              buf.append(mob().location().roomID());
                              c++; break; }
				case 'v': { buf.append("^<Move^>"+mob().curState().getMovement()+"^</Move^>"); c++; break;}
				case 'V': { buf.append("^<MaxMove^>"+mob().maxState().getMovement()+"^</MaxMove^>"); c++; break;}
                case 'w': { buf.append(mob().envStats().weight()); c++; break;}
                case 'W': { buf.append(mob().maxCarry()); c++; break;}
				case 'x': { buf.append(mob().getExperience()); c++; break;}
				case 'X': {
							  if(mob().getExpNeededLevel()==Integer.MAX_VALUE)
								buf.append("N/A");
							  else
								buf.append(mob().getExpNeededLevel());
							  c++; break;
						  }
				case 'z': {      if((mob().location()!=null)&&(CMSecurity.isAllowed(mob(),mob().location(),"SYSMSGS")))
								  buf.append(mob().location().getArea().name());
							  c++; break; }
				case 't': {	  if(mob().location()!=null)
								  buf.append(CMStrings.capitalizeAndLower(TimeClock.TOD_DESC[mob().location().getArea().getTimeObj().getTODCode()].toLowerCase()));
							  c++; break;
						  }
				case 'T': {	  if(mob().location()!=null)
								  buf.append(mob().location().getArea().getTimeObj().getTimeOfDay());
							  c++; break;
						  }
				case '@': {	  if(mob().location()!=null)
								  buf.append(mob().location().getArea().getClimateObj().weatherDescription(mob().location()));
							  c++; break;
						  }
				default:{ buf.append("%"+prompt.charAt(c)); c++; break;}
				}
			}
			else
				buf.append(prompt.charAt(c++));
		print("^<Prompt^>"+buf.toString()+"^</Prompt^>^.^N");
	}

    protected void closeSocks()
	{
		try
		{
			if(sock!=null)
			{
				status=Session.STATUS_LOGOUT6;
				if(out!=null) out.flush();
				status=Session.STATUS_LOGOUT7;
                if(sock!=null) sock.shutdownInput();
				status=Session.STATUS_LOGOUT8;
				if(sock!=null) sock.shutdownOutput();
				status=Session.STATUS_LOGOUT9;
				if(out!=null) out.close();
				status=Session.STATUS_LOGOUT10;
                if(sock!=null) sock.close();
				status=Session.STATUS_LOGOUT11;
			}
			in=null;
			out=null;
			sock=null;
		}
		catch(IOException e)
		{
		}
	}

	public String getAddress()
	{
		try
		{
			return sock.getInetAddress().getHostAddress();
		}
		catch (Exception e)
		{
			return "Unknown (Excpt "+e.getMessage() + ")";
		}
	}

	public int getStatus(){return status;}

	public void run()
	{
		status=Session.STATUS_LOGIN;
		try
		{
			int tries=0;
			while((!killFlag)&&((++tries)<5))
			{
				MOB newMob=CMClass.getMOB("StdMOB");
				newMob.setSession(this);
				mob=newMob;
				status=Session.STATUS_LOGIN;
				String input=null;
                int loginAttempt=CMLib.login().login(mob,tries);
				if(loginAttempt>=1)
				{
					status=Session.STATUS_LOGIN2;
					if((!killFlag)&&(mob!=null))
                    {
						Log.sysOut("Session",getAddress()+" login: "+mob.Name());
                        if(loginAttempt>0)
                            if(!CMLib.map().sendGlobalMessage(mob,CMMsg.TYP_LOGIN,CMClass.getMsg(mob,null,CMMsg.MSG_LOGIN,null)))
                                killFlag=true;
                    }
					needPrompt=true;
					Vector CMDS=null;
					while((!killFlag)&&(mob!=null))
					{
                        while((!killFlag)
                        &&(mob!=null)
                        &&(CMLib.threads().isAllSuspended())
                        &&(!CMSecurity.isASysOp(mob)))
                            try{Thread.sleep(2000);}catch(Exception e){}
						status=Session.STATUS_OK;
						lastLoopTop=System.currentTimeMillis();
						waiting=true;
						if(suspendCommandLine)
						{
							input=null;
							try{Thread.sleep(100);}catch(Exception e){}
						}
						else
							input=readlineContinue();
						if(input!=null)
						{
							lastKeystroke=System.currentTimeMillis();
							setAfkFlag(false);
							CMDS=CMParms.parse(input);
							if(CMDS.size()>0)
							{
								waiting=false;
                                String firstWord=(String)CMDS.firstElement();
                                PlayerStats pstats=mob.playerStats();
                                String alias=(pstats!=null)?pstats.getAlias(firstWord):"";
                                Vector ALL_CMDS=new Vector();
                                boolean echoOn=false;
                                if(alias.length()>0)
                                {
                                    CMDS.removeElementAt(0);
                                    Vector all_stuff=CMParms.parseSquiggleDelimited(alias,true);
                                    for(int a=0;a<all_stuff.size();a++)
                                    {
                                        Vector THIS_CMDS=(Vector)CMDS.clone();
                                        ALL_CMDS.addElement(THIS_CMDS);
                                        Vector preCommands=CMParms.parse((String)all_stuff.elementAt(a));
                                        for(int v=preCommands.size()-1;v>=0;v--)
                                            THIS_CMDS.insertElementAt(preCommands.elementAt(v),0);
                                    }
                                    echoOn=true;
                                }
                                else
                                    ALL_CMDS.addElement(CMDS);
                                for(int v=0;v<ALL_CMDS.size();v++)
                                {
                                    CMDS=(Vector)ALL_CMDS.elementAt(v);
    								setPreviousCmd(CMDS);
    								milliTotal+=(lastStop-lastStart);
    								
    								if(snoops.size()>0)
                                    {
                                        String msgColored=CMStrings.replaceAll(input,"\n\r",CMLib.coffeeFilter().colorOnlyFilter("\n\r^Z"+((mob==null)?"?":mob.Name())+":^N ",this));
    									for(int s=0;s<snoops.size();s++)
    										((Session)snoops.elementAt(s)).rawPrintln(msgColored);
                                    }
    								
    								lastStart=System.currentTimeMillis();
                                    if(echoOn) rawPrintln(CMParms.combineWithQuotes(CMDS,0));
                                    Vector MORE_CMDS=CMLib.english().preCommandParser(CMDS);
                                    for(int m=0;m<MORE_CMDS.size();m++)
                                    	if(mob!=null)
		    								mob.enqueCommand((Vector)MORE_CMDS.elementAt(m),0);
    								lastStop=System.currentTimeMillis();
                                }
							}
							needPrompt=true;
						}
						if(mob==null) break;
                        while((!killFlag)&&(mob!=null)&&(mob.dequeCommand()));

						if(((System.currentTimeMillis()-lastBlahCheck)>=60000)
						&&(mob()!=null))
						{
							lastBlahCheck=System.currentTimeMillis();
							Vector V=CMParms.parse(CMProps.getVar(CMProps.SYSTEM_IDLETIMERS));
							if((V.size()>0)
							&&(!CMSecurity.isAllowed(mob(),mob().location(),"IDLEOK"))
							&&(CMath.s_int((String)V.firstElement())>0))
							{
								int minsIdle=(int)(getIdleMillis()/60000);
								if(minsIdle>=CMath.s_int((String)V.firstElement()))
								{
									println("\n\r^ZYou are being logged out!^?");
									setKillFlag(true);
								}
								else
								if(minsIdle>=CMath.s_int((String)V.lastElement()))
								{
									int remain=CMath.s_int((String)V.firstElement())-minsIdle;
									println(mob(),null,null,"\n\r^ZIf you don't do something, you will be logged out in "+remain+" minute(s)!^?");
								}
							}
							
							if(!afkFlag())
							{
								if(getIdleMillis()>=600000)
									setAfkFlag(true);
							}
							else
							if((getIdleMillis()>=10800000)&&(!killFlag()))
							{
								if((!CMLib.flags().isSleeping(mob))
								&&(mob().fetchEffect("Disease_Blahs")==null))
								{
									Ability A=CMClass.getAbility("Disease_Blahs");
									if(A!=null) A.invoke(mob,mob,true,0);
								}
								else
								if((CMLib.flags().isSleeping(mob))
								&&(mob().fetchEffect("Disease_Narcolepsy")==null))
								{
									Ability A=CMClass.getAbility("Disease_Narcolepsy");
									if(A!=null) A.invoke(mob,mob,true,0);
								}
							}
						}
						if((needPrompt)&&(waiting))
						{
						    if((mob!=null)&&(mob.isInCombat()))
						    {
						        if(((System.currentTimeMillis()-promptLastShown)>=Tickable.TIME_TICK)
						        ||(input!=null))
						        {
									showPrompt();
									needPrompt=false;
						        }
						    }
						    else
						    {
								showPrompt();
								needPrompt=false;
						    }
						}
					}
					status=Session.STATUS_LOGOUT2;
				}
				else
				{
					mob=null;
					newMob.setSession(null);
				}
				status=Session.STATUS_LOGOUT;
			}
			status=Session.STATUS_LOGOUT3;
		}
		catch(SocketException e)
		{
			if(!Log.isMaskedErrMsg(e.getMessage()))
				errorOut(e);
		}
		catch(Exception t)
		{
			if(!Log.isMaskedErrMsg(t.getMessage()))
				errorOut(t);
		}
		status=Session.STATUS_LOGOUT3;

        if(mob!=null)
        {
            while((getLastPKFight()>0)
            &&((System.currentTimeMillis()-getLastPKFight())<(2*60*1000))
            &&(mob!=null))
            { try{Thread.sleep(1000);}catch(Exception e){}}
        }
		if(mob!=null)
		{
			String name=mob.Name();
			if(name.trim().length()==0) name="Unknown";
            if((mob.isInCombat())&&(mob.location()!=null))
            {
                CMLib.commands().postFlee(mob,"NOWHERE");
                mob.makePeace();
            }
            Vector channels=CMLib.channels().getFlaggedChannelNames("LOGOFFS");
            if(!CMLib.flags().isCloaked(mob))
            for(int i=0;i<channels.size();i++)
                CMLib.commands().postChannel((String)channels.elementAt(i),mob.getClanID(),name+" has logged out",true);
			// the player quit message!
            loginLogoutThread LT=new loginLogoutThread(mob,CMMsg.MSG_QUIT);
            LT.initialize();
            LT.start();
			if(mob.playerStats()!=null)
            {
				mob.playerStats().setLastDateTime(System.currentTimeMillis());
            }
			Log.sysOut("Session",getAddress()+" logout: "+name);
			if(mob!=null) CMLib.database().DBUpdateFollowers(mob);
			if(mob!=null) mob.removeFromGame(true);
			if(mob!=null) mob.setSession(null);
			mob=null;
		}
		
		status=Session.STATUS_LOGOUT4;
		killFlag=true;
		waiting=false;
		needPrompt=false;
		snoops.clear();
		
		closeSocks();
		
		
		status=Session.STATUS_LOGOUT5;
		CMLib.sessions().removeElement(this);

		//finally
		//{
		//}
		status=Session.STATUS_LOGOUTFINAL;
	}
    
    private static class loginLogoutThread extends Thread implements Tickable
    {
        public String name(){return (theMOB==null)?"Dead LLThread":"LLThread for "+theMOB.Name();}
        public boolean tick(Tickable ticking, int tickID){return false;}
        public String ID(){return name();}
        public CMObject newInstance(){try{return (CMObject)getClass().newInstance();}catch(Exception e){return new loginLogoutThread();}}
        public void initializeClass(){}
        public CMObject copyOf(){try{return (CMObject)this.clone();}catch(Exception e){return newInstance();}}
        public int compareTo(Object o){ return CMClass.classID(this).compareToIgnoreCase(CMClass.classID(o));}
        public long getTickStatus(){return 0;}
        private MOB theMOB=null;
        private int msgCode=-1;
        private HashSet skipRooms=new HashSet();
        private loginLogoutThread(){}
        public loginLogoutThread(MOB mob, int msgC)
        {
            theMOB=mob;
            msgCode=msgC;
        }

        public void initialize()
        {
            HashSet group=theMOB.getGroupMembers(new HashSet());
            skipRooms.clear();
            for(Iterator i=group.iterator();i.hasNext();)
            {
                MOB M=(MOB)i.next();
                if((M.location()!=null)&&(!skipRooms.contains(M.location())))
                    skipRooms.add(M.location());
            }
            if((!CMProps.getBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN))
            &&(CMProps.getBoolVar(CMProps.SYSTEMB_MUDSTARTED)))
            {
                CMMsg msg=CMClass.getMsg(theMOB,null,msgCode,null);
                Room R=theMOB.location();
                if(R!=null) skipRooms.remove(R);
                try{
                    if((R!=null)&&(theMOB.location()!=null))
                        R.send(theMOB,msg);
                    for(Iterator i=skipRooms.iterator();i.hasNext();)
                    {
                        R=(Room)i.next();
                        if(theMOB.location()!=null)
                            R.sendOthers(theMOB,msg);
                    }
                    if(R!=null) skipRooms.add(R);
                }catch(Exception e){}
            }
        }
        
        public void run()
        {
            if((!CMProps.getBoolVar(CMProps.SYSTEMB_MUDSHUTTINGDOWN))
            &&(CMProps.getBoolVar(CMProps.SYSTEMB_MUDSTARTED)))
            {
                CMMsg msg=CMClass.getMsg(theMOB,null,msgCode,null);
                Room R=null;
                try{
                    for(Enumeration e=CMLib.map().rooms();e.hasMoreElements();)
                    {
                        R=(Room)e.nextElement();
                        if((!skipRooms.contains(R))&&(theMOB.location()!=null))
                            R.sendOthers(theMOB,msg);
                    }
                }catch(Exception e){}
                theMOB=null;
            }
        }
    }
}
