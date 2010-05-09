package com.planet_ink.coffee_mud.core.intermud.cm1;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
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
import com.planet_ink.coffee_mud.core.intermud.cm1.commands.*;
import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.io.*;
import java.util.concurrent.atomic.*;

/* 
   Copyright 2000-2010 Bo Zimmerman

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
public class CommandHandler implements Runnable
{
	private String cmd;
	private String rest;
	private RequestHandler req; 
	private static final Map<String,CM1Command> commandList=new Hashtable<String,CM1Command>();
	private static final void AddCommand(CM1Command c) { commandList.put(c.CommandWord(),c); }
	static
	{
		AddCommand(new CM1Quit());
		AddCommand(new CM1Block());
	}
	
	public CommandHandler(RequestHandler req, String command)
	{
		this.req=req;
		int x=command.indexOf(' ');
		if(x<0)
		{
			cmd=command;
			rest="";
		}
		else
		{
			cmd=command.substring(0,x);
			rest=command.substring(x+1);
		}
	}

	public void run() 
	{
		if(cmd.length()>0)
		{
			try
			{
				CM1Command command = commandList.get(cmd.toUpperCase().trim());
				if(command == null)
					req.sendMsg("[UNKNOWN: "+cmd.toUpperCase().trim()+"]");
				else
					command.newInstance(req, rest).run();
			}
			catch(java.io.IOException ioe)
			{
				req.close();
			}
		}
		
	}
}