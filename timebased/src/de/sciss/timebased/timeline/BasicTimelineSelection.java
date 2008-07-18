/*
 *  BasicTimelineSelection.java
 *  TimeBased
 *
 *  Copyright (c) 2004-2008 Hanns Holger Rutz. All rights reserved.
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
 *  Changelog:
 *		17-Jul-08	created
 */

package de.sciss.timebased.timeline;

import java.awt.EventQueue;

import de.sciss.app.BasicEvent;
import de.sciss.app.EventManager;
import de.sciss.io.Span;

public class BasicTimelineSelection
implements TimelineSelection, EventManager.Processor
{
	private final Timeline			tl;
	protected Span					span;
	private final Timeline.Listener	tll;
	
	// --- event handling ---

	protected final EventManager	elm		= new EventManager( this );
	
	public BasicTimelineSelection( Timeline tl )
	{
		this( tl, new Span( tl.getSpan().start, tl.getSpan().start ));
	}
	
	public BasicTimelineSelection( Timeline tl, Span span )
	{
		this.span	= span;
		this.tl		= tl;
		tll = new Timeline.Listener() {
			public void timelineChanged( Timeline.Event e )
			{
				final Span tlSpan = e.getTimeline().getSpan();
				if( !tlSpan.contains( BasicTimelineSelection.this.span )) {
					BasicTimelineSelection.this.span = BasicTimelineSelection.this.span.intersection( tlSpan );
					elm.dispatchEvent( new Event( e.getSource(), Event.CHANGED, e.getWhen(), BasicTimelineSelection.this ));
				}
			}
		};
		tl.addListener( tll );
	}
	
	public void dispose()
	{
		tl.removeListener( tll );
		elm.dispose();
	}

	public Timeline getTimeline()
	{
		return tl;
	}
	
	public Span getSpan()
	{
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException();
		return span;
	}

	public void setSpan( Object source, Span newSpan )
	{
		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException();
		if( !span.equals( newSpan )) {
			span = newSpan;
			if( source != null ) dispatchChange( source );
		}
	}
	
	/**
	 *  Register a <code>TimelineListener</code>
	 *  which will be informed about changes of
	 *  the timeline (i.e. changes in rate and length,
	 *  scrolling in the timeline frame and selection
	 *  of timeline portions by the user).
	 *
	 *  @param  listener	the <code>TimelineListener</code> to register
	 *  @see	de.sciss.app.EventManager#addListener( Object )
	 */
	public void addListener( Listener listener )
	{
		elm.addListener( listener );
	}

	/**
	 *  Unregister a <code>TimelineListener</code>
	 *  from receiving timeline events.
	 *
	 *  @param  listener	the <code>TimelineListener</code> to unregister
	 *  @see	de.sciss.app.EventManager#removeListener( Object )
	 */
	public void removeListener( Listener listener )
	{
		elm.removeListener( listener );
	}
	
	/**
	 *  This is called by the EventManager
	 *  if new events are to be processed
	 */
	public void processEvent( BasicEvent e )
	{
		for( int i = 0; i < elm.countListeners(); i++ ) {
			final Listener l = (Listener) elm.getListener( i );
			switch( e.getID() ) {
			case Event.CHANGED:
				l.timelineSelected( (Event) e );
				break;
			default:
				assert false : e.getID();
			}
		}
	}
	
	// utility function to create and dispatch a TimelineEvent
	protected void dispatchChange( Object source )
	{
		elm.dispatchEvent( new Event( source, Event.CHANGED,
		                              System.currentTimeMillis(), this ));
	}
}