/*
 *	BosqueTimeline
 *	(Bosque)
 *
 *	Copyright (c) 2007-2008 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *	Changelog:
 */

/**
 *	@author	Hanns Holger Rutz
 *	@version	0.12, 18-Jul-08
 */
BosqueTimeline : Object {
	var <span;
	var <rate;
	var <java;

	*new { arg rate, span;
		^super.new.prInit( rate, span );
	}
	
	prInit { arg argRate, argSpan;
		var swing;
		
		rate			= argRate ? 1000.0;
		span			= argSpan ??  {�Span.new };
		swing		= Bosque.default.swing;
		java			= JavaObject( "de.sciss.timebased.timeline.BasicTimeline", swing, rate, span );
	}
	
	storeModifiersOn { arg stream;
		stream << ".rate_(";
		rate.storeOn( stream );
		stream << ")";
		stream << ".span_(";
		span.storeOn( stream );
		stream << ")";
	}
	
	span_ { arg newSpan;
		if( newSpan.equals( span ).not, {
			span = newSpan;
			this.changed( \span, span );
			java.setSpan( java, span );
		});
	}

	rate_ { arg newRate;
		if( newRate != rate, {
			rate = newRate;
			this.changed( \rate, rate );
			java.setRate( java, rate );
		});
	}

	dispose {
		java.dispose; java.destroy; java = nil;
	}

	asSwingArg {
		^java.asSwingArg;
	}
}