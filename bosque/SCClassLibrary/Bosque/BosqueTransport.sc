/**
 *	(C)opyright 2007-2008 by Hanns Holger Rutz. All rights reserved.
 *
 *	@version	0.17, 19-Jul-08
 */
BosqueTransport : Object {
	var <doc;
	var running	= false;
	var paused	= false;
	var clpseStop;
	var playRate, playRate2, playStartPos, playStartTime;
	var loop;
	var loopUsed	= false;
	
	*new { arg doc;
		^super.new.prInit( doc );
	}
	
	prInit { arg argDoc;
		doc = argDoc;
		UpdateListener.newFor( doc.timelineView, { arg upd, timelineView, what, what2;
			var pos, wasPaused;
			
			wasPaused = paused;
			if( running, {
				switch( what,
				\positioned, {
					pos = timelineView.cursor.position;
					this.stop( setPosition: false );
					this.play( pos );
					if( wasPaused, { this.pause });
				},
				\changed, {
					what2.postln;
					switch( what2,
					\span, { this.prMakeCollapse },
					\rate, {
						this.stop( setPosition: true );
						this.play( pos );
						if( wasPaused, { this.pause });
					});
				});
			});
		});
	}
	
	loop_ { arg span;
		loop = span;
		if( running, { this.prMakeCollapse });
	}
	
	play { arg pos, rate = 1;
		if( running, { ^this });
		
		pos			= pos ?? { doc.timelineView.cursor.position };
		if( pos >= doc.timeline.span.stop, { pos = doc.timeline.span.start });
		playStartTime	= thisThread.seconds;
		playStartPos	= pos;
		playRate		= rate;
		playRate2		= playRate * doc.timeline.rate;
		running		= true;
		paused		= false;
		this.prMakeCollapse;
		this.changed( \play, pos, rate );
	}

	prMakeCollapse {
		var f, pos, dur;
		if( clpseStop.notNil, { clpseStop.cancel; clpseStop = nil });
		pos		= this.currentFrame;
		loopUsed	= loop.notNil and: { pos < loop.stop };
		if( loopUsed, {
			f	= { this.stop; this.play( loop.start, playRate )};
			dur	= (loop.stop - pos) / playRate2;
		}, {
			f = { this.stop };
			dur	= (doc.timeline.span.stop - pos) / playRate2;
		});
		clpseStop = Collapse( f, dur, SystemClock ).defer;
	}
	
	stop { arg setPosition = true;
		var pos;

		if( running.not, { ^this });

		if( clpseStop.notNil, { clpseStop.cancel; clpseStop = nil });
		pos 			= this.currentFrame;
		running		= false;
		paused		= false;
		this.changed( \stop );
		if( setPosition, { doc.editPosition( this, pos )});
	}
	
	pause {
		if( running.not || paused, { ^this });
		
		if( clpseStop.notNil, { clpseStop.cancel; clpseStop = nil });
		playStartPos 	= this.currentFrame;
		playStartTime	= thisThread.seconds;
		paused		= true;
		this.changed( \pause );
	}
	
	resume { arg pos;
		if( running.not || paused.not, { ^this });
		
		paused		= false;
		pos			= pos ? playStartPos;
		playStartPos 	= pos;
		playStartTime	= thisThread.seconds;
		this.prMakeCollapse;
		this.changed( \resume, pos, playRate );
	}
	
	isRunning { ^running }
	isPaused  { ^paused }
	
	currentFrame {
		if( running, {
			^(playStartPos + ((thisThread.seconds - playStartTime) * playRate2).asInteger).min( doc.timeline.span.stop );
		}, {
			^doc.timelineView.cursor.position;
		});
	}
}
